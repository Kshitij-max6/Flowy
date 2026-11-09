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
import org.json.JSONObject
import java.io.File
import java.net.HttpURLConnection
import java.net.URL
import java.security.MessageDigest

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

    suspend fun checkForUpdate(context: Context, force: Boolean = false): UpdateCheckResult =
        withContext(Dispatchers.IO) {
            val repo = BuildConfig.UPDATE_REPO
            if (repo.isBlank() || repo.startsWith("OWNER/")) {
                return@withContext UpdateCheckResult.Failed("Update source is not configured yet")
            }
            if (!force && !isDailyCheckDue(context)) return@withContext UpdateCheckResult.UpToDate
            try {
                val conn = (URL("https://api.github.com/repos/$repo/releases/latest").openConnection() as HttpURLConnection).apply {
                    connectTimeout = 10_000
                    readTimeout = 15_000
                    setRequestProperty("Accept", "application/vnd.github+json")
                    setRequestProperty("User-Agent", "Flowly-Android/${BuildConfig.VERSION_NAME}")
                }
                if (conn.responseCode == 404) {
                    prefs(context).edit().putLong(KEY_LAST_CHECK, System.currentTimeMillis()).apply()
                    return@withContext UpdateCheckResult.UpToDate
                }
                if (conn.responseCode != 200) {
                    return@withContext UpdateCheckResult.Failed("GitHub answered ${conn.responseCode}")
                }
                val json = JSONObject(conn.inputStream.bufferedReader().use { it.readText() })
                prefs(context).edit().putLong(KEY_LAST_CHECK, System.currentTimeMillis()).apply()

                if (json.optBoolean("draft") || json.optBoolean("prerelease")) return@withContext UpdateCheckResult.UpToDate

                val tag = json.optString("tag_name").removePrefix("v").removePrefix("V")
                if (!isNewer(tag, BuildConfig.VERSION_NAME)) return@withContext UpdateCheckResult.UpToDate

                val assets = json.optJSONArray("assets")
                var apk: JSONObject? = null
                var shaAsset: String? = null
                if (assets != null) {
                    for (i in 0 until assets.length()) {
                        val a = assets.getJSONObject(i)
                        val name = a.optString("name")
                        if (name.endsWith(".apk", true) && apk == null) apk = a
                        if (name.endsWith(".sha256", true)) shaAsset = a.optString("browser_download_url")
                    }
                }
                val apkAsset = apk ?: return@withContext UpdateCheckResult.Failed("Release has no APK attached")

                var sha = apkAsset.optString("digest").removePrefix("sha256:").takeIf { it.length == 64 }
                if (sha == null && shaAsset != null) sha = fetchSha(shaAsset)

                val notes = json.optString("body")
                UpdateCheckResult.Available(
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
            } catch (e: Exception) {
                UpdateCheckResult.Failed(e.localizedMessage ?: "Could not reach GitHub")
            }
        }

    private fun fetchSha(url: String): String? = try {
        val conn = URL(url).openConnection() as HttpURLConnection
        conn.connectTimeout = 10_000; conn.readTimeout = 15_000
        conn.inputStream.bufferedReader().use { it.readText() }.trim().split(Regex("\\s+")).firstOrNull()
            ?.takeIf { it.length == 64 }
    } catch (_: Exception) { null }

    /** Numeric semantic comparison, ignores suffixes like -beta. */
    internal fun isNewer(candidate: String, current: String): Boolean {
        fun parts(v: String) = v.substringBefore('-').split('.').map { it.filter(Char::isDigit).toIntOrNull() ?: 0 }
        val a = parts(candidate); val b = parts(current)
        for (i in 0 until maxOf(a.size, b.size)) {
            val x = a.getOrElse(i) { 0 }; val y = b.getOrElse(i) { 0 }
            if (x != y) return x > y
        }
        return false
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
        DatabaseSafety.createPreUpdateBackup(context)
        val uri = FileProvider.getUriForFile(context, "${context.packageName}.fileprovider", apk)
        context.startActivity(
            Intent(Intent.ACTION_VIEW).apply {
                setDataAndType(uri, "application/vnd.android.package-archive")
                addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION or Intent.FLAG_ACTIVITY_NEW_TASK)
            }
        )
    }
}
