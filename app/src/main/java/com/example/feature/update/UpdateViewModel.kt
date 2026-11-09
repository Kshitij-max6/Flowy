package com.example.feature.update

import android.app.Application
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.example.core.update.AppUpdateManager
import com.example.core.update.DownloadResult
import com.example.core.update.UpdateCheckResult
import com.example.core.update.UpdateInfo
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import java.io.File

data class UpdateUiState(
    val checking: Boolean = false,
    val available: UpdateInfo? = null,
    /** True when the pop-up (not just the Settings row) should be showing. */
    val showPrompt: Boolean = false,
    val downloading: Boolean = false,
    val progress: Float = 0f,
    val readyFile: File? = null,
    val message: String? = null,
    /** True when the user asked from Settings, so a result message should be shown. */
    val manual: Boolean = false
)

class UpdateViewModel(app: Application) : AndroidViewModel(app) {

    private val _state = MutableStateFlow(UpdateUiState())
    val state: StateFlow<UpdateUiState> = _state.asStateFlow()

    private var autoChecked = false

    /** Once per app launch; the manager itself limits real network checks to one per day. */
    fun autoCheck() {
        if (autoChecked) return
        autoChecked = true
        viewModelScope.launch {
            val result = AppUpdateManager.checkForUpdate(getApplication(), force = false)
            if (result is UpdateCheckResult.Available) {
                val show = AppUpdateManager.shouldPrompt(getApplication(), result.info)
                _state.value = _state.value.copy(available = result.info, showPrompt = show)
                if (show) AppUpdateManager.markPrompted(getApplication(), result.info.versionName)
            }
        }
    }

    fun checkNow() {
        if (_state.value.checking) return
        _state.value = _state.value.copy(checking = true, message = null, manual = true)
        viewModelScope.launch {
            when (val r = AppUpdateManager.checkForUpdate(getApplication(), force = true)) {
                is UpdateCheckResult.Available ->
                    _state.value = _state.value.copy(checking = false, available = r.info, showPrompt = true)
                UpdateCheckResult.UpToDate ->
                    _state.value = _state.value.copy(checking = false, available = null, message = "You're on the latest version.")
                is UpdateCheckResult.Failed ->
                    _state.value = _state.value.copy(checking = false, message = "Couldn't check: ${r.reason}")
            }
        }
    }

    fun later() {
        _state.value = _state.value.copy(showPrompt = false, manual = false)
    }

    fun skip() {
        val info = _state.value.available ?: return
        AppUpdateManager.skipVersion(getApplication(), info.versionName)
        _state.value = _state.value.copy(showPrompt = false, available = null, manual = false)
    }

    fun dismissMessage() {
        _state.value = _state.value.copy(message = null, manual = false)
    }

    fun startDownload() {
        val info = _state.value.available ?: return
        if (_state.value.downloading) return
        _state.value = _state.value.copy(downloading = true, progress = 0f, message = null)
        viewModelScope.launch {
            val result = AppUpdateManager.downloadAndVerify(getApplication(), info) { p ->
                _state.value = _state.value.copy(progress = p)
            }
            _state.value = when (result) {
                is DownloadResult.Ready -> _state.value.copy(downloading = false, readyFile = result.file)
                is DownloadResult.Failed -> _state.value.copy(downloading = false, message = result.reason)
            }
        }
    }

    /** Returns false when the user first has to allow installs from this app. */
    fun install(): Boolean {
        val file = _state.value.readyFile ?: return true
        val app = getApplication<Application>()
        if (!AppUpdateManager.canInstall(app)) {
            AppUpdateManager.openInstallPermissionSettings(app)
            return false
        }
        AppUpdateManager.install(app, file)
        return true
    }
}
