package com.example.core.util

import com.example.core.update.AppUpdateManager
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test

class AppUpdateManagerTest {
    @Test fun newerVersionsAreDetected() {
        assertTrue(AppUpdateManager.isNewer("1.2.0", "1.1.0"))
        assertTrue(AppUpdateManager.isNewer("1.10.0", "1.9.9"))
        assertTrue(AppUpdateManager.isNewer("2.0", "1.9.9"))
    }

    @Test fun sameOrOlderIsNotAnUpdate() {
        assertFalse(AppUpdateManager.isNewer("1.1.0", "1.1.0"))
        assertFalse(AppUpdateManager.isNewer("1.0.9", "1.1.0"))
        assertFalse(AppUpdateManager.isNewer("1.1", "1.1.0"))
    }

    @Test fun suffixesAndPrefixesAreIgnored() {
        assertTrue(AppUpdateManager.isNewer("1.2.0-beta", "1.1.0"))
        assertTrue(AppUpdateManager.isNewer("v1.2.0", "1.1.0"))
        assertFalse(AppUpdateManager.isNewer("1.1.0-rc1", "1.1.0"))
        assertFalse(AppUpdateManager.isNewer("1.1.0+build5", "1.1.0"))
    }

    @Test fun tagsThatAreNotVersionsNeverTriggerAnUpdate() {
        assertFalse(AppUpdateManager.isNewer("", "1.1.0"))
        assertFalse(AppUpdateManager.isNewer("nightly", "1.1.0"))
        assertFalse(AppUpdateManager.isNewer("release-2024", "1.1.0"))
        assertFalse(AppUpdateManager.isNewer("1.2.x", "1.1.0"))
        assertFalse(AppUpdateManager.isNewer("1..2", "1.1.0"))
        assertFalse(AppUpdateManager.isNewer("99999999999.0", "1.1.0")) // overflows Int
    }

    @Test fun anUnreadableInstalledVersionNeverTriggersAnUpdate() {
        assertFalse(AppUpdateManager.isNewer("9.9.9", "dev"))
        assertFalse(AppUpdateManager.isNewer("9.9.9", ""))
    }

    @Test fun repositoryMustBeOwnerSlashNameAndNotThePlaceholder() {
        assertTrue(AppUpdateManager.isValidRepo("Kshitij-max6/Flowy"))
        assertTrue(AppUpdateManager.isValidRepo("some.user/my_repo-2"))
        assertFalse(AppUpdateManager.isValidRepo("OWNER/Final-Flowly"))
        assertFalse(AppUpdateManager.isValidRepo(""))
        assertFalse(AppUpdateManager.isValidRepo("   "))
        assertFalse(AppUpdateManager.isValidRepo("Kshitij-max6"))
        assertFalse(AppUpdateManager.isValidRepo("a/b/c"))
        assertFalse(AppUpdateManager.isValidRepo("https://github.com/a/b"))
        assertFalse(AppUpdateManager.isValidRepo("a b/c"))
        assertFalse(AppUpdateManager.isValidRepo("a/b?x=1"))
    }

    @Test fun httpFailuresGetPlainWording() {
        val limit = "limit on update checks"
        assertTrue(AppUpdateManager.httpFailureMessage(403, "0", null).contains(limit))
        assertTrue(AppUpdateManager.httpFailureMessage(403, null, "60").contains(limit))
        assertTrue(AppUpdateManager.httpFailureMessage(429, null, null).contains(limit))
        assertTrue(AppUpdateManager.httpFailureMessage(503, null, null).contains("trouble"))
        // A 403 with quota left is not a rate limit; don't claim it is.
        assertFalse(AppUpdateManager.httpFailureMessage(403, "42", null).contains(limit))
        assertTrue(AppUpdateManager.httpFailureMessage(418, null, null).contains("418"))
    }

    // ------------------------------------------------------------ checksum file contract (sha256sum format)

    private val apkName = "Flowly-v1.2.0.apk"
    private val hash = "0123456789abcdef".repeat(4)

    @Test fun aCorrectChecksumFileIsAccepted() {
        assertEquals(hash, AppUpdateManager.parseChecksum("$hash  $apkName\n", apkName))
        assertEquals(hash, AppUpdateManager.parseChecksum("$hash  $apkName", apkName))
        assertEquals(hash, AppUpdateManager.parseChecksum("$hash *$apkName", apkName)) // binary-mode marker
        assertEquals(hash, AppUpdateManager.parseChecksum("$hash\t$apkName", apkName))
    }

    @Test fun checksumFilesAreForgivingAboutWhitespaceAndCase() {
        assertEquals(hash, AppUpdateManager.parseChecksum("  \n$hash  $apkName\r\n\r\n", apkName)) // CRLF, blank lines
        assertEquals(hash, AppUpdateManager.parseChecksum("${hash.uppercase()}  $apkName", apkName))
        assertEquals(hash, AppUpdateManager.parseChecksum(hash, apkName)) // a bare hash
    }

    @Test fun aChecksumForAnotherApkIsRejected() {
        assertNull(AppUpdateManager.parseChecksum("$hash  Flowly-v1.1.0.apk", apkName))
        assertNull(AppUpdateManager.parseChecksum("$hash  other.apk", apkName))
        assertNull(AppUpdateManager.parseChecksum("$hash  Flowly-v1.2.0.apk.sha256", apkName))
    }

    @Test fun emptyMalformedOrAmbiguousChecksumFilesAreRejected() {
        assertNull(AppUpdateManager.parseChecksum("", apkName))
        assertNull(AppUpdateManager.parseChecksum("   \n  \n", apkName))
        assertNull(AppUpdateManager.parseChecksum(hash.dropLast(1) + "  $apkName", apkName))           // 63 digits
        assertNull(AppUpdateManager.parseChecksum(hash + "a  $apkName", apkName))                      // 65 digits
        assertNull(AppUpdateManager.parseChecksum("g".repeat(64) + "  $apkName", apkName))             // not hex
        assertNull(AppUpdateManager.parseChecksum("$hash$apkName", apkName))                           // no separator
        assertNull(AppUpdateManager.parseChecksum("<html>Not Found</html>", apkName))
        assertNull(AppUpdateManager.parseChecksum("$hash  $apkName\n${"b".repeat(64)}  $apkName", apkName)) // two lines
    }
}
