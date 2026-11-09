package com.example.core.util

import android.app.Activity
import android.app.KeyguardManager
import android.content.Context
import android.content.Intent
import android.os.Build
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow

object AppLockManager {

    private val _isLocked = MutableStateFlow(false)
    val isLocked: StateFlow<Boolean> = _isLocked.asStateFlow()

    private var lastBackgroundTimestamp: Long = 0L
    private var isAppInForeground: Boolean = true

    /**
     * Checks if the device has a screen lock (PIN, Pattern, Password, or Biometrics) configured.
     */
    fun isDeviceSecure(context: Context): Boolean {
        val keyguardManager = context.getSystemService(Context.KEYGUARD_SERVICE) as? KeyguardManager
        return keyguardManager?.isDeviceSecure == true
    }

    fun isBiometricOrDeviceLockSupported(context: Context): Boolean {
        return isDeviceSecure(context)
    }

    fun onAppForegrounded(appLockEnabled: Boolean, timeoutMinutes: Int) {
        isAppInForeground = true
        if (!appLockEnabled) {
            _isLocked.value = false
            return
        }

        if (lastBackgroundTimestamp == 0L) {
            // First launch with lock enabled
            _isLocked.value = true
            return
        }

        val elapsedMillis = System.currentTimeMillis() - lastBackgroundTimestamp
        val timeoutMillis = timeoutMinutes * 60 * 1000L

        if (elapsedMillis >= timeoutMillis) {
            _isLocked.value = true
        }
    }

    fun onAppBackgrounded() {
        isAppInForeground = false
        lastBackgroundTimestamp = System.currentTimeMillis()
    }

    fun unlock() {
        _isLocked.value = false
        lastBackgroundTimestamp = System.currentTimeMillis()
    }

    fun lockNow() {
        _isLocked.value = true
    }

    fun setInitialLockState(appLockEnabled: Boolean) {
        if (appLockEnabled) {
            _isLocked.value = true
        }
    }

    fun createDeviceCredentialIntent(context: Context, title: String = "Unlock Flowly", description: String = "Verify your identity to access Flowly"): Intent? {
        val keyguardManager = context.getSystemService(Context.KEYGUARD_SERVICE) as? KeyguardManager ?: return null
        return if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            keyguardManager.createConfirmDeviceCredentialIntent(title, description)
        } else {
            keyguardManager.createConfirmDeviceCredentialIntent(title, description)
        }
    }
}
