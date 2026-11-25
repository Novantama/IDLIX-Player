package com.idlix.mobile.ui.screens.settings

import android.app.Application
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import coil.imageLoader
import com.idlix.data.api.ApiClient
import com.idlix.data.local.IdlixAccountStore
import com.idlix.data.model.IdlixAccount
import com.idlix.data.repository.SubdlRepository
import com.idlix.mobile.R
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext

data class SettingsUiState(
    val activeBackendUrl: String = "",
    val activeSubdlKey: String = "",
    val backendUrl: String = "",
    val subdlKey: String = "",
    val activeDnsProvider: String = ApiClient.DEFAULT_DNS_PROVIDER,
    val selectedDnsProvider: String = ApiClient.DEFAULT_DNS_PROVIDER,
    val isTestingBackend: Boolean = false,
    val backendTestResult: String? = null,
    val isBackendTestSuccess: Boolean = false,
    val isTestingSubdl: Boolean = false,
    val subdlTestResult: String? = null,
    val isSubdlTestSuccess: Boolean = false,
    val isTestingDns: Boolean = false,
    val dnsTestResult: String? = null,
    val isDnsTestSuccess: Boolean = false,
    val cacheCleared: Boolean = false,
    val isLoggingInAccount: Boolean = false,
    val loginAccountError: String? = null,
    val isRefreshingAccount: Boolean = false
)

class SettingsViewModel(
    application: Application
) : AndroidViewModel(application) {

    private val _uiState = MutableStateFlow(
        SettingsUiState(
            activeBackendUrl = ApiClient.getBaseUrl(),
            activeSubdlKey = SubdlRepository.getApiKey(),
            backendUrl = ApiClient.getBaseUrl(),
            subdlKey = SubdlRepository.getApiKey(),
            activeDnsProvider = ApiClient.getDnsProvider(),
            selectedDnsProvider = ApiClient.getDnsProvider()
        )
    )
    val uiState: StateFlow<SettingsUiState> = _uiState.asStateFlow()

    fun updateBackendUrl(url: String) {
        _uiState.value = _uiState.value.copy(backendUrl = url, backendTestResult = null)
    }

    fun updateSubdlKey(key: String) {
        _uiState.value = _uiState.value.copy(subdlKey = key, subdlTestResult = null)
    }

    fun resetBackendUrl() {
        val defaultUrl = ApiClient.getDefaultUrl()
        _uiState.value = _uiState.value.copy(backendUrl = defaultUrl, backendTestResult = null)
    }

    fun resetSubdlKey() {
        val defaultKey = SubdlRepository.DEFAULT_API_KEY
        _uiState.value = _uiState.value.copy(subdlKey = defaultKey, subdlTestResult = null)
    }

    fun saveBackendUrl() {
        val app = getApplication<Application>()
        val targetUrl = _uiState.value.backendUrl.trim()
        if (targetUrl.isBlank()) {
            _uiState.value = _uiState.value.copy(
                backendTestResult = app.getString(R.string.url_empty_error),
                isBackendTestSuccess = false
            )
            return
        }
        ApiClient.setBaseUrl(targetUrl)
        val newActive = ApiClient.getBaseUrl()
        _uiState.value = _uiState.value.copy(
            activeBackendUrl = newActive,
            backendUrl = newActive,
            backendTestResult = app.getString(R.string.url_saved_success),
            isBackendTestSuccess = true
        )
    }

    fun saveSubdlKey() {
        val app = getApplication<Application>()
        val targetKey = _uiState.value.subdlKey.trim()
        SubdlRepository.setApiKey(targetKey)
        val newActive = SubdlRepository.getApiKey()
        _uiState.value = _uiState.value.copy(
            activeSubdlKey = newActive,
            subdlKey = newActive,
            subdlTestResult = app.getString(R.string.subdl_key_saved_success),
            isSubdlTestSuccess = true
        )
    }

    fun testBackend() {
        val app = getApplication<Application>()
        val targetUrl = _uiState.value.backendUrl.trim()
        if (targetUrl.isBlank()) {
            _uiState.value = _uiState.value.copy(
                isTestingBackend = false,
                backendTestResult = app.getString(R.string.url_empty_error),
                isBackendTestSuccess = false
            )
            return
        }
        if (!targetUrl.startsWith("http://", ignoreCase = true) && !targetUrl.startsWith("https://", ignoreCase = true)) {
            _uiState.value = _uiState.value.copy(
                isTestingBackend = false,
                backendTestResult = app.getString(R.string.url_invalid_protocol),
                isBackendTestSuccess = false
            )
            return
        }

        viewModelScope.launch {
            _uiState.value = _uiState.value.copy(isTestingBackend = true, backendTestResult = null, isBackendTestSuccess = false)
            val result = ApiClient.testBackendConnection(targetUrl)
            _uiState.value = _uiState.value.copy(
                isTestingBackend = false,
                isBackendTestSuccess = result.isSuccess,
                backendTestResult = result.fold(
                    onSuccess = { it },
                    onFailure = { "Failed: ${it.localizedMessage ?: "Unknown network error"}" }
                )
            )
        }
    }

    fun autoDetectServer() {
        viewModelScope.launch {
            _uiState.value = _uiState.value.copy(
                isTestingBackend = true,
                backendTestResult = "Mencari server IDLIX di jaringan lokal...",
                isBackendTestSuccess = true
            )
            val result = ApiClient.autoDetectServer()
            result.fold(
                onSuccess = { detectedUrl ->
                    ApiClient.setBaseUrl(detectedUrl)
                    _uiState.value = _uiState.value.copy(
                        isTestingBackend = false,
                        backendUrl = detectedUrl,
                        activeBackendUrl = detectedUrl,
                        backendTestResult = "Server ditemukan & disimpan: $detectedUrl",
                        isBackendTestSuccess = true
                    )
                },
                onFailure = { error ->
                    _uiState.value = _uiState.value.copy(
                        isTestingBackend = false,
                        backendTestResult = "Gagal: ${error.localizedMessage ?: "Server tidak ditemukan"}",
                        isBackendTestSuccess = false
                    )
                }
            )
        }
    }

    fun testSubdl() {
        val app = getApplication<Application>()
        val targetKey = _uiState.value.subdlKey.trim()
        if (targetKey.isBlank()) {
            _uiState.value = _uiState.value.copy(
                isTestingSubdl = false,
                subdlTestResult = app.getString(R.string.subdl_key_empty_error),
                isSubdlTestSuccess = false
            )
            return
        }

        viewModelScope.launch {
            _uiState.value = _uiState.value.copy(isTestingSubdl = true, subdlTestResult = null, isSubdlTestSuccess = false)
            val result = SubdlRepository.testApiKey(targetKey)
            _uiState.value = _uiState.value.copy(
                isTestingSubdl = false,
                isSubdlTestSuccess = result.isSuccess,
                subdlTestResult = result.fold(
                    onSuccess = { it },
                    onFailure = { "Failed: ${it.localizedMessage ?: "Unknown network error"}" }
                )
            )
        }
    }

    fun updateDnsProvider(provider: String) {
        _uiState.value = _uiState.value.copy(selectedDnsProvider = provider, dnsTestResult = null)
    }

    fun saveDnsProvider() {
        val app = getApplication<Application>()
        val target = _uiState.value.selectedDnsProvider
        ApiClient.setDnsProvider(target)
        val active = ApiClient.getDnsProvider()
        _uiState.value = _uiState.value.copy(
            activeDnsProvider = active,
            selectedDnsProvider = active,
            dnsTestResult = app.getString(R.string.dns_saved_success),
            isDnsTestSuccess = true
        )
    }

    fun resetDnsProvider() {
        val defaultProvider = ApiClient.DEFAULT_DNS_PROVIDER
        _uiState.value = _uiState.value.copy(selectedDnsProvider = defaultProvider, dnsTestResult = null)
    }

    fun testDns() {
        val target = _uiState.value.selectedDnsProvider
        viewModelScope.launch {
            _uiState.value = _uiState.value.copy(isTestingDns = true, dnsTestResult = null, isDnsTestSuccess = false)
            val result = ApiClient.testDns(target)
            _uiState.value = _uiState.value.copy(
                isTestingDns = false,
                isDnsTestSuccess = result.isSuccess,
                dnsTestResult = result.fold(
                    onSuccess = { it },
                    onFailure = { "Failed: ${it.localizedMessage ?: "DNS resolution failed"}" }
                )
            )
        }
    }

    fun clearImageCache() {
        viewModelScope.launch(Dispatchers.IO) {
            val context = getApplication<Application>()
            try {
                context.imageLoader.diskCache?.clear()
                withContext(Dispatchers.Main) {
                    context.imageLoader.memoryCache?.clear()
                    _uiState.value = _uiState.value.copy(cacheCleared = true)
                }
            } catch (_: Exception) {}
        }
    }

    val accountState: StateFlow<IdlixAccount> = IdlixAccountStore.account

    fun loginIdlix(ident: String, pass: String) {
        val cleanIdent = ident.trim()
        val cleanPass = pass.trim()
        if (cleanIdent.isBlank() || cleanPass.isBlank()) {
            _uiState.value = _uiState.value.copy(loginAccountError = "Email / Username dan Password wajib diisi")
            return
        }
        viewModelScope.launch {
            _uiState.value = _uiState.value.copy(isLoggingInAccount = true, loginAccountError = null)
            val res = ApiClient.loginIdlix(cleanIdent, cleanPass)
            _uiState.value = _uiState.value.copy(
                isLoggingInAccount = false,
                loginAccountError = res.exceptionOrNull()?.localizedMessage
            )
        }
    }

    fun refreshAccountStatus() {
        viewModelScope.launch {
            _uiState.value = _uiState.value.copy(isRefreshingAccount = true)
            ApiClient.refreshIdlixAccountStatus()
            _uiState.value = _uiState.value.copy(isRefreshingAccount = false)
        }
    }

    fun logoutIdlix() {
        ApiClient.logoutIdlix()
        _uiState.value = _uiState.value.copy(loginAccountError = null)
    }
}
