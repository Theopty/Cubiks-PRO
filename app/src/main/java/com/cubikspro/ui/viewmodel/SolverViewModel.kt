package com.cubikspro.ui.viewmodel

import android.app.Application
import android.content.Context
import androidx.datastore.core.DataStore
import androidx.datastore.preferences.core.Preferences
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.core.stringPreferencesKey
import androidx.datastore.preferences.preferencesDataStore
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.cubikspro.BuildConfig
import com.cubikspro.api.AnthropicApiClient
import com.cubikspro.api.CubiksResult
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.launch

private val Context.dataStore: DataStore<Preferences> by preferencesDataStore(name = "settings")

class SolverViewModel(application: Application) : AndroidViewModel(application) {

    companion object {
        private val API_KEY_PREF = stringPreferencesKey("anthropic_api_key")
    }

    private val _uiState = MutableStateFlow(SolverUiState())
    val uiState: StateFlow<SolverUiState> = _uiState

    private val _apiKey = MutableStateFlow("")
    val apiKey: StateFlow<String> = _apiKey

    init {
        viewModelScope.launch {
            val savedKey = getApplication<Application>().dataStore.data
                .map { prefs -> prefs[API_KEY_PREF] ?: "" }
                .first()
            // Prefer saved key, then build config
            val key = savedKey.ifEmpty { BuildConfig.ANTHROPIC_API_KEY }
            _apiKey.value = key
        }
    }

    fun setApiKey(key: String) {
        _apiKey.value = key
        viewModelScope.launch {
            getApplication<Application>().dataStore.edit { prefs ->
                prefs[API_KEY_PREF] = key
            }
        }
    }

    fun solveFromImage(imageBytes: ByteArray, questionType: String? = null) {
        val key = _apiKey.value
        if (key.isBlank()) {
            _uiState.value = _uiState.value.copy(
                error = "Please set your Anthropic API key in Settings first."
            )
            return
        }

        _uiState.value = _uiState.value.copy(
            isLoading = true,
            error = null,
            result = null,
            capturedImageBytes = imageBytes
        )

        viewModelScope.launch {
            try {
                val client = AnthropicApiClient(key)
                val result = client.solveCubiksQuestion(imageBytes, questionType)
                _uiState.value = _uiState.value.copy(
                    isLoading = false,
                    result = result,
                    error = null
                )
            } catch (e: Exception) {
                _uiState.value = _uiState.value.copy(
                    isLoading = false,
                    error = e.message ?: "Unknown error occurred"
                )
            }
        }
    }

    fun clearResult() {
        _uiState.value = SolverUiState()
    }

    fun clearError() {
        _uiState.value = _uiState.value.copy(error = null)
    }
}

data class SolverUiState(
    val isLoading: Boolean = false,
    val result: CubiksResult? = null,
    val error: String? = null,
    val capturedImageBytes: ByteArray? = null,
) {
    override fun equals(other: Any?): Boolean {
        if (this === other) return true
        if (other !is SolverUiState) return false
        return isLoading == other.isLoading &&
                result == other.result &&
                error == other.error &&
                capturedImageBytes.contentEquals(other.capturedImageBytes)
    }

    override fun hashCode(): Int {
        var hash = isLoading.hashCode()
        hash = 31 * hash + (result?.hashCode() ?: 0)
        hash = 31 * hash + (error?.hashCode() ?: 0)
        hash = 31 * hash + (capturedImageBytes?.contentHashCode() ?: 0)
        return hash
    }
}
