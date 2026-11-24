package com.example.core.util

import com.example.core.update.AppUpdateManager
import org.junit.Assert.assertFalse
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
}
