package com.example.core.update

import android.content.Context
import android.content.Intent
import android.content.pm.PackageInfo
import android.content.pm.PackageManager
import android.net.Uri
import android.os.Build
import android.provider.Settings
import androidx.core.content.FileProvider
import com.example.BuildConfig
import com.example.core.safety.DatabaseSafety
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import org.json.JSONException
import org.json.JSONObject
import java.io.File
import java.net.ConnectException
import java.net.HttpURLConnection
import java.net.SocketTimeoutException
import java.net.URL
import java.net.UnknownHostException
import java.security.MessageDigest
import javax.net.ssl.SSLException

data class UpdateInfo(
    val versionName: String,
    val notes: String,
    val required: Boolean,
    val apkUrl: String,
    val apkSizeBytes: Long,
    val sha256: String?,
    val releasePageUrl: String
)

sealed class UpdateCheckResult {
    data class Available(val info: UpdateInfo) : UpdateCheckResult()
    object UpToDate : UpdateCheckResult()
    data class Failed(val reason: String) : UpdateCheckResult()
}

sealed class DownloadResult {
    data class Ready(val file: File) : DownloadResult()
    data class Failed(val reason: String) : DownloadResult()
}

/**
 * Checks GitHub Releases (no account, no token) for a newer Flowly build, downloads it,
 * verifies it, and hands it to the Android package installer.
 *
 * Release conventions (see RELEASING.md):
 *  - tag is `v<versionName>`, e.g. `v1.2.0`
 *  - one `.apk` asset is attached; GitHub records its SHA-256 as the asset `digest`
 *  - the release notes contain `[required]` for an emergency update the user cannot skip
 */
object AppUpdateManager {

    private const val PREFS = "flowly_updates"
    private const val KEY_LAST_CHECK = "last_check_millis"
    private const val KEY_PROMPTED = "prompted_version"
    private const val KEY_SKIPPED = "skipped_version"
    private const val CHECK_INTERVAL_MS = 24L * 60 * 60 * 1000

    // ---------------------------------------------------------------- scheduling / prompt memory

    private fun prefs(context: Context) = context.getSharedPreferences(PREFS, Context.MODE_PRIVATE)

    fun isDailyCheckDue(context: Context): Boolean =
        System.currentTimeMillis() - prefs(context).getLong(KEY_LAST_CHECK, 0L) >= CHECK_INTERVAL_MS

    fun markPrompted(context: Context, version: String) {
        prefs(context).edit().putString(KEY_PROMPTED, version).apply()
    }

    fun skipVersion(context: Context, version: String) {
        prefs(context).edit().putString(KEY_SKIPPED, version).putString(KEY_PROMPTED, version).apply()
    }

    /** Pop-up shows once per version, never for a skipped one, unless the update is required. */
    fun shouldPrompt(context: Context, info: UpdateInfo): Boolean {
        if (info.required) return true
        val p = prefs(context)
        return p.getString(KEY_PROMPTED, null) != info.versionName &&
            p.getString(KEY_SKIPPED, null) != info.versionName
    }

    // ---------------------------------------------------------------- check

    private const val GITHUB_API = "https://api.github.com"
    private val REPO_PATTERN = Regex("[A-Za-z0-9_.-]+/[A-Za-z0-9_.-]+")
    private val VERSION_PATTERN = Regex("\\d+(\\.\\d+)*")

    internal const val MSG_BAD_REPO =
        "This build doesn't know which GitHub repository to check. Build it with the release workflow, or set flowly.updateRepo=owner/repo in gradle.properties"

    /** `owner/repo`, and not the unconfigured `OWNER/...` placeholder. */
    internal fun isValidRepo(repo: String): Boolean =
        REPO_PATTERN.matches(repo) && !repo.startsWith("OWNER/", ignoreCase = true)

    suspend fun checkForUpdate(context: Context, force: Boolean = false): UpdateCheckResult =
        withContext(Dispatchers.IO) {
            val repo = BuildConfig.UPDATE_REPO
            if (!isValidRepo(repo)) return@withContext UpdateCheckResult.Failed(MSG_BAD_REPO)
            if (!force && !isDailyCheckDue(context)) return@withContext UpdateCheckResult.UpToDate
            fetchLatest(repo, BuildConfig.VERSION_NAME, force) {
                prefs(context).edit().putLong(KEY_LAST_CHECK, System.currentTimeMillis()).apply()
            }
        }

    /**
     * Asks GitHub for [repo]'s latest release and decides whether it is newer than [currentVersion].
     * [markChecked] runs once GitHub has given a usable answer (so a failed attempt is retried sooner).
     * [apiBase] exists so tests can point this at a local server.
     */
    internal fun fetchLatest(
        repo: String,
        currentVersion: String,
        force: Boolean,
        apiBase: String = GITHUB_API,
        markChecked: () -> Unit = {}
    ): UpdateCheckResult {
        if (!isValidRepo(repo)) return UpdateCheckResult.Failed(MSG_BAD_REPO)
        return try {
            val conn = (URL("$apiBase/repos/$repo/releases/latest").openConnection() as HttpURLConnection).apply {
                connectTimeout = 10_000
                readTimeout = 15_000
                setRequestProperty("Accept", "application/vnd.github+json")
                setRequestProperty("User-Agent", "Flowly-Android/$currentVersion")
            }
            val code = conn.responseCode
            when {
                code == 404 -> {
                    markChecked()
                    // 404 = no published release yet, a wrong repo name, or a private repository.
                    if (force) UpdateCheckResult.Failed(
                        "No published release found in $repo. The repository must be public and have at least one release."
                    ) else UpdateCheckResult.UpToDate
                }
                code != 200 -> UpdateCheckResult.Failed(
                    httpFailureMessage(code, conn.getHeaderField("X-RateLimit-Remaining"), conn.getHeaderField("Retry-After"))
                )
                else -> {
                    val body = conn.inputStream.bufferedReader().use { it.readText() }
                    markChecked()
                    parseRelease(body, currentVersion) { fetchSha(it) }
                }
            }
        } catch (e: Exception) {
            UpdateCheckResult.Failed(describeFailure(e))
        }
    }

    /** Turns GitHub's `releases/latest` JSON into a result; anything unexpected is a failure, never an update. */
    internal fun parseRelease(body: String, currentVersion: String, fetchSha: (String) -> String?): UpdateCheckResult {
        val json = try {
            JSONObject(body)
        } catch (_: JSONException) {
            return UpdateCheckResult.Failed("GitHub sent a reply the app couldn't understand. Please try again later.")
        }

        if (json.optBoolean("draft") || json.optBoolean("prerelease")) return UpdateCheckResult.UpToDate

        val tag = json.optString("tag_name").trim().removePrefix("v").removePrefix("V")
        if (tag.isEmpty()) return UpdateCheckResult.Failed("GitHub sent a release without a version. Please try again later.")
        if (!isNewer(tag, currentVersion)) return UpdateCheckResult.UpToDate

        val assets = json.optJSONArray("assets")
        var apk: JSONObject? = null
        var shaAsset: String? = null
        if (assets != null) {
            for (i in 0 until assets.length()) {
                val a = assets.optJSONObject(i) ?: continue
                val name = a.optString("name")
                if (name.endsWith(".apk", true) && apk == null) apk = a
                if (name.endsWith(".sha256", true)) shaAsset = a.optString("browser_download_url")
            }
        }
        val apkAsset = apk?.takeIf { it.optString("browser_download_url").isNotBlank() }
            ?: return UpdateCheckResult.Failed("Release has no APK attached")

        var sha = apkAsset.optString("digest").removePrefix("sha256:").takeIf { it.length == 64 }
        if (sha == null && !shaAsset.isNullOrBlank()) sha = fetchSha(shaAsset)

        val notes = json.optString("body")
        return UpdateCheckResult.Available(
            UpdateInfo(
                versionName = tag,
                notes = notes.replace("[required]", "", ignoreCase = true).trim(),
                required = notes.contains("[required]", ignoreCase = true),
                apkUrl = apkAsset.optString("browser_download_url"),
                apkSizeBytes = apkAsset.optLong("size"),
                sha256 = sha?.lowercase(),
                releasePageUrl = json.optString("html_url")
            )
        )
    }

    /** Wording for a non-200/404 answer. GitHub signals rate limits with 429, or 403 plus a spent quota / Retry-After. */
    internal fun httpFailureMessage(code: Int, rateLimitRemaining: String?, retryAfter: String?): String = when {
        code == 429 || (code == 403 && (rateLimitRemaining == "0" || retryAfter != null)) ->
            "GitHub's limit on update checks was reached. Please try again in a little while."
        code in 500..599 -> "GitHub is having trouble right now ($code). Please try again later."
        else -> "GitHub answered $code"
    }

    internal fun describeFailure(e: Exception): String = when (e) {
        is UnknownHostException, is ConnectException, is SocketTimeoutException, is SSLException ->
            "Couldn't reach GitHub. Check your internet connection and try again."
        else -> e.localizedMessage ?: "Could not reach GitHub"
    }

    private fun fetchSha(url: String): String? = try {
        val conn = URL(url).openConnection() as HttpURLConnection
        conn.connectTimeout = 10_000; conn.readTimeout = 15_000
        conn.inputStream.bufferedReader().use { it.readText() }.trim().split(Regex("\\s+")).firstOrNull()
            ?.takeIf { it.length == 64 }
    } catch (_: Exception) { null }

    /**
     * Numeric semantic comparison, ignores suffixes like -beta. Anything that isn't a dotted number
     * (`nightly`, `release-2024`, an empty tag) is never "newer", so a stray tag can't trigger a false update.
     */
    internal fun isNewer(candidate: String, current: String): Boolean {
        val a = numericParts(candidate) ?: return false
        val b = numericParts(current) ?: return false
        for (i in 0 until maxOf(a.size, b.size)) {
            val x = a.getOrElse(i) { 0 }; val y = b.getOrElse(i) { 0 }
            if (x != y) return x > y
        }
        return false
    }

    private fun numericParts(version: String): List<Int>? {
        val core = version.trim().removePrefix("v").removePrefix("V").substringBefore('-').substringBefore('+')
        if (!VERSION_PATTERN.matches(core)) return null
        return core.split('.').map { it.toIntOrNull() ?: return null }
    }

    // ---------------------------------------------------------------- download + verify

    suspend fun downloadAndVerify(
        context: Context,
        info: UpdateInfo,
        onProgress: (Float) -> Unit
    ): DownloadResult = withContext(Dispatchers.IO) {
        val expected = info.sha256
            ?: return@withContext DownloadResult.Failed("This release has no checksum, so it can't be verified. Update was not installed.")
        val dir = File(context.cacheDir, "updates").apply { mkdirs() }
        dir.listFiles()?.forEach { it.delete() }
        val out = File(dir, "flowly-${info.versionName}.apk")
        try {
            val conn = (URL(info.apkUrl).openConnection() as HttpURLConnection).apply {
                connectTimeout = 15_000; readTimeout = 30_000
                instanceFollowRedirects = true
                setRequestProperty("User-Agent", "Flowly-Android/${BuildConfig.VERSION_NAME}")
            }
            if (conn.responseCode != 200) return@withContext DownloadResult.Failed("Download failed (${conn.responseCode})")
            val total = conn.contentLengthLong.takeIf { it > 0 } ?: info.apkSizeBytes
            val digest = MessageDigest.getInstance("SHA-256")
            var done = 0L
            conn.inputStream.use { input ->
                out.outputStream().use { output ->
                    val buf = ByteArray(64 * 1024)
                    while (true) {
                        val n = input.read(buf)
                        if (n < 0) break
                        output.write(buf, 0, n)
                        digest.update(buf, 0, n)
                        done += n
                        if (total > 0) onProgress((done.toFloat() / total).coerceIn(0f, 1f))
                    }
                }
            }
            val actual = digest.digest().joinToString("") { "%02x".format(it) }
            if (actual != expected) {
                out.delete()
                return@withContext DownloadResult.Failed("The download is corrupted (checksum mismatch). Please try again.")
            }
            verifyPackage(context, out)?.let { problem ->
                out.delete()
                return@withContext DownloadResult.Failed(problem)
            }
            DownloadResult.Ready(out)
        } catch (e: Exception) {
            out.delete()
            DownloadResult.Failed(e.localizedMessage ?: "Download failed")
        }
    }

    /** Returns null when the APK is the same app, newer, and signed with the same key; otherwise a reason. */
    @Suppress("DEPRECATION")
    private fun verifyPackage(context: Context, apk: File): String? {
        val pm = context.packageManager
        val flags = if (Build.VERSION.SDK_INT >= 28) PackageManager.GET_SIGNING_CERTIFICATES else PackageManager.GET_SIGNATURES
        val candidate = pm.getPackageArchiveInfo(apk.path, flags) ?: return "The downloaded file is not a valid app package."
        if (candidate.packageName != context.packageName) return "The downloaded file belongs to a different app."
        val installed = pm.getPackageInfo(context.packageName, flags)
        if (versionCodeOf(candidate) <= versionCodeOf(installed)) return "The downloaded build is not newer than the installed one."
        val a = signerHashes(candidate)
        val b = signerHashes(installed)
        // Same signing key (or a rotated key whose history includes the installed one).
        if (a.isEmpty() || a.intersect(b).isEmpty()) {
            return "The update is signed with a different key than the installed app, so Android would refuse it. " +
                "Back up your data, uninstall once, then install the new version."
        }
        return null
    }

    @Suppress("DEPRECATION")
    private fun versionCodeOf(p: PackageInfo): Long =
        if (Build.VERSION.SDK_INT >= 28) p.longVersionCode else p.versionCode.toLong()

    @Suppress("DEPRECATION")
    private fun signerHashes(p: PackageInfo): Set<String> {
        val sigs = if (Build.VERSION.SDK_INT >= 28) {
            val si = p.signingInfo ?: return emptySet()
            if (si.hasMultipleSigners()) si.apkContentsSigners else si.signingCertificateHistory
        } else p.signatures
        return sigs?.map { s ->
            MessageDigest.getInstance("SHA-256").digest(s.toByteArray()).joinToString("") { "%02x".format(it) }
        }?.toSet() ?: emptySet()
    }

    // ---------------------------------------------------------------- install

    fun canInstall(context: Context): Boolean =
        Build.VERSION.SDK_INT < 26 || context.packageManager.canRequestPackageInstalls()

    fun openInstallPermissionSettings(context: Context) {
        context.startActivity(
            Intent(Settings.ACTION_MANAGE_UNKNOWN_APP_SOURCES, Uri.parse("package:${context.packageName}"))
                .addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
        )
    }

    /** Saves a safety copy of the data, then opens the system installer. */
    fun install(context: Context, apk: File) {
        DatabaseSafety.createPreUpdateBackup(context, checkpointFirst = true)
        val uri = FileProvider.getUriForFile(context, "${context.packageName}.fileprovider", apk)
        context.startActivity(
            Intent(Intent.ACTION_VIEW).apply {
                setDataAndType(uri, "application/vnd.android.package-archive")
                addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION or Intent.FLAG_ACTIVITY_NEW_TASK)
            }
        )
    }
}
