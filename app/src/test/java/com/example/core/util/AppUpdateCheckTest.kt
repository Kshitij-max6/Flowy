package com.example.core.util

import com.example.BuildConfig
import com.example.core.update.AppUpdateManager
import com.example.core.update.UpdateCheckResult
import com.sun.net.httpserver.HttpServer
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config
import java.net.InetAddress
import java.net.InetSocketAddress

/** Update-check behaviour: release parsing, GitHub failure modes, and the repository the build baked in. */
@RunWith(RobolectricTestRunner::class)
@Config(sdk = [34])
class AppUpdateCheckTest {

    private val repo = "Kshitij-max6/Flowy"
    private val installed = "1.1.0"
    private val sha = "a".repeat(64)

    private val apk = """{"name":"Flowly-v1.2.0.apk","size":1234,"browser_download_url":"https://example.test/Flowly-v1.2.0.apk","digest":"sha256:$sha"}"""

    private fun release(
        tag: String = "v1.2.0",
        draft: Boolean = false,
        prerelease: Boolean = false,
        notes: String = "Fixes",
        assets: String = "[$apk]"
    ) = """{"tag_name":"$tag","draft":$draft,"prerelease":$prerelease,"body":"$notes","html_url":"https://example.test/r/$tag","assets":$assets}"""

    private fun parse(body: String, shaFetcher: (String) -> String? = { null }) =
        AppUpdateManager.parseRelease(body, installed, shaFetcher)

    // ------------------------------------------------------------ the repository the build baked in

    @Test
    fun `the generated BuildConfig carries a real owner-slash-repo, not the placeholder`() {
        assertTrue("BuildConfig.UPDATE_REPO was '${BuildConfig.UPDATE_REPO}'", AppUpdateManager.isValidRepo(BuildConfig.UPDATE_REPO))
        assertFalse(BuildConfig.UPDATE_REPO.startsWith("OWNER/", ignoreCase = true))
    }

    // ------------------------------------------------------------ parsing a release

    @Test
    fun `a newer release is offered with its details`() {
        val r = parse(release(notes = "Fixes")) as UpdateCheckResult.Available
        assertEquals("1.2.0", r.info.versionName)
        assertEquals("Fixes", r.info.notes)
        assertFalse(r.info.required)
        assertEquals("https://example.test/Flowly-v1.2.0.apk", r.info.apkUrl)
        assertEquals(1234L, r.info.apkSizeBytes)
        assertEquals(sha, r.info.sha256)
        assertEquals("https://example.test/r/v1.2.0", r.info.releasePageUrl)
    }

    @Test
    fun `required marker makes the update mandatory and is removed from the notes`() {
        val r = parse(release(notes = "[required] Security fix")) as UpdateCheckResult.Available
        assertTrue(r.info.required)
        assertEquals("Security fix", r.info.notes)
    }

    @Test
    fun `the installed version and older releases are not updates`() {
        assertEquals(UpdateCheckResult.UpToDate, parse(release(tag = "v1.1.0")))
        assertEquals(UpdateCheckResult.UpToDate, parse(release(tag = "1.1.0")))
        assertEquals(UpdateCheckResult.UpToDate, parse(release(tag = "v1.0.9")))
    }

    @Test
    fun `drafts and pre-releases are ignored even when newer`() {
        assertEquals(UpdateCheckResult.UpToDate, parse(release(tag = "v9.0.0", draft = true)))
        assertEquals(UpdateCheckResult.UpToDate, parse(release(tag = "v9.0.0", prerelease = true)))
    }

    @Test
    fun `a tag that is not a version never produces an update`() {
        assertEquals(UpdateCheckResult.UpToDate, parse(release(tag = "nightly")))
        assertEquals(UpdateCheckResult.UpToDate, parse(release(tag = "release-2024")))
    }

    @Test
    fun `a newer release without a usable APK is a failure, not an update`() {
        assertTrue(parse(release(assets = "[]")) is UpdateCheckResult.Failed)
        assertTrue(parse(release(assets = """[{"name":"notes.txt","browser_download_url":"https://example.test/n.txt"}]""")) is UpdateCheckResult.Failed)
        assertTrue(parse(release(assets = """[{"name":"Flowly.apk","size":5}]""")) is UpdateCheckResult.Failed) // no download URL
        assertTrue(parse(release(assets = """[{"name":"Flowly-v9.0.0.apk","browser_download_url":"https://example.test/wrong.apk"}]""")) is UpdateCheckResult.Failed)
        assertTrue(parse(release(assets = """[$apk,$apk]""")) is UpdateCheckResult.Failed)
        assertTrue(parse(release(assets = """[1,"x",null]""")) is UpdateCheckResult.Failed)
    }

    @Test
    fun `checksum comes from the sha256 asset when GitHub gave no digest`() {
        val noDigest = """{"name":"Flowly-v1.2.0.apk","size":9,"browser_download_url":"https://example.test/a.apk"}"""
        val shaFile = """{"name":"Flowly-v1.2.0.apk.sha256","browser_download_url":"https://example.test/a.sha256"}"""
        val r = parse(release(assets = "[$noDigest,$shaFile]")) { url ->
            assertEquals("https://example.test/a.sha256", url)
            "B".repeat(64)
        } as UpdateCheckResult.Available
        assertEquals("b".repeat(64), r.info.sha256)

        val none = parse(release(assets = "[$noDigest]")) as UpdateCheckResult.Available
        assertNull(none.info.sha256) // the downloader refuses to install without a checksum
    }

    @Test
    fun `a checksum that is not a real sha256 is never trusted`() {
        val noDigest = """{"name":"Flowly-v1.2.0.apk","size":9,"browser_download_url":"https://example.test/a.apk"}"""
        val shaFile = """{"name":"Flowly-v1.2.0.apk.sha256","browser_download_url":"https://example.test/a.sha256"}"""
        val r = parse(release(assets = "[$noDigest,$shaFile]")) { "z".repeat(64) } as UpdateCheckResult.Available
        assertNull(r.info.sha256) // 64 characters but not hex: the downloader will refuse to install
        val badDigest = """{"name":"Flowly-v1.2.0.apk","browser_download_url":"https://example.test/a.apk","digest":"sha256:${"z".repeat(64)}"}"""
        assertNull((parse(release(assets = "[$badDigest]")) as UpdateCheckResult.Available).info.sha256)
    }

    @Test
    fun `a checksum asset that belongs to another APK is ignored`() {
        val noDigest = """{"name":"Flowly-v1.2.0.apk","size":9,"browser_download_url":"https://example.test/a.apk"}"""
        val foreign = """{"name":"Flowly-v1.1.0.apk.sha256","browser_download_url":"https://example.test/old.sha256"}"""
        var fetched = false
        val r = parse(release(assets = "[$noDigest,$foreign]")) { fetched = true; "b".repeat(64) } as UpdateCheckResult.Available
        assertFalse("must not even download a foreign checksum", fetched)
        assertNull(r.info.sha256)
    }

    @Test
    fun `malformed responses fail instead of reporting an update`() {
        listOf("", "   ", "<html>rate limited</html>", "[]", "null", "{", """{"message":"Not Found"}""", """{"assets":[]}""").forEach { body ->
            assertTrue("'$body' should fail", parse(body) is UpdateCheckResult.Failed)
        }
        // wrong types inside otherwise valid JSON
        assertTrue(parse("""{"tag_name":"v9.9.9","assets":"oops"}""") is UpdateCheckResult.Failed)
    }

    // ------------------------------------------------------------ talking to GitHub (local fake server)

    private class FakeGitHub(
        private val status: Int,
        private val body: String = "",
        private val headers: Map<String, String> = emptyMap()
    ) {
        @Volatile var path: String? = null
        @Volatile var accept: String? = null
        @Volatile var userAgent: String? = null
        @Volatile var requests = 0
        private val server: HttpServer = HttpServer.create(InetSocketAddress(InetAddress.getLoopbackAddress(), 0), 0)

        init {
            server.createContext("/") { ex ->
                requests++
                path = ex.requestURI.path
                accept = ex.requestHeaders.getFirst("Accept")
                userAgent = ex.requestHeaders.getFirst("User-Agent")
                headers.forEach { (k, v) -> ex.responseHeaders.add(k, v) }
                val bytes = body.toByteArray()
                ex.sendResponseHeaders(status, if (bytes.isEmpty()) -1 else bytes.size.toLong())
                if (bytes.isNotEmpty()) ex.responseBody.use { it.write(bytes) }
                ex.close()
            }
            server.start()
        }

        val baseUrl get() = "http://127.0.0.1:${server.address.port}"
        fun stop() = server.stop(0)
    }

    private val servers = mutableListOf<FakeGitHub>()
    private fun fake(status: Int, body: String = "", headers: Map<String, String> = emptyMap()) =
        FakeGitHub(status, body, headers).also { servers += it }

    @After fun tearDown() = servers.forEach { it.stop() }

    private fun check(server: FakeGitHub, force: Boolean = false, repoName: String = repo, onChecked: () -> Unit = {}) =
        AppUpdateManager.fetchLatest(repoName, installed, force, server.baseUrl, onChecked)

    @Test
    fun `asks GitHub about the configured repository and offers a newer release`() {
        val gh = fake(200, release())
        var checked = 0
        val r = check(gh) { checked++ }
        assertTrue(r is UpdateCheckResult.Available)
        assertEquals("/repos/Kshitij-max6/Flowy/releases/latest", gh.path)
        assertEquals("application/vnd.github+json", gh.accept)
        assertEquals("Flowly-Android/$installed", gh.userAgent)
        assertEquals(1, checked)
    }

    @Test
    fun `the installed version from GitHub is up to date`() {
        assertEquals(UpdateCheckResult.UpToDate, check(fake(200, release(tag = "v1.1.0"))))
    }

    @Test
    fun `no published release is quiet in the background and explained when asked`() {
        var checked = 0
        assertEquals(UpdateCheckResult.UpToDate, check(fake(404), force = false) { checked++ })
        val manual = check(fake(404), force = true) { checked++ } as UpdateCheckResult.Failed
        assertTrue(manual.reason, manual.reason.contains("No published release found in $repo"))
        assertEquals(2, checked)
    }

    @Test
    fun `rate limiting is reported as such and not counted as a completed check`() {
        var checked = 0
        for (gh in listOf(
            fake(403, """{"message":"API rate limit exceeded"}""", mapOf("X-RateLimit-Remaining" to "0")),
            fake(429, "", mapOf("Retry-After" to "60"))
        )) {
            val r = check(gh) { checked++ } as UpdateCheckResult.Failed
            assertTrue(r.reason, r.reason.contains("limit on update checks"))
        }
        assertEquals(0, checked)
    }

    @Test
    fun `GitHub errors are failures`() {
        val r = check(fake(500)) as UpdateCheckResult.Failed
        assertTrue(r.reason, r.reason.contains("500"))
    }

    @Test
    fun `an unreadable 200 reply is a failure, never an update`() {
        assertTrue(check(fake(200, "<html>captive portal</html>")) is UpdateCheckResult.Failed)
        assertTrue(check(fake(200, "")) is UpdateCheckResult.Failed)
    }

    @Test
    fun `being offline is a friendly failure`() {
        val gh = fake(200, release())
        gh.stop() // nothing listens on that port any more
        val r = check(gh) as UpdateCheckResult.Failed
        assertTrue(r.reason, r.reason.contains("Couldn't reach GitHub"))
    }

    @Test
    fun `an invalid repository configuration makes no request and says so`() {
        val gh = fake(200, release())
        for (bad in listOf("OWNER/Final-Flowly", "", "not-a-repo")) {
            val r = check(gh, repoName = bad) as UpdateCheckResult.Failed
            assertEquals(AppUpdateManager.MSG_BAD_REPO, r.reason)
        }
        assertEquals(0, gh.requests)
    }
}
