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
}
