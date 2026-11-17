package com.example.viewmodel

import android.util.Log
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.core.UiState
import com.example.data.AiRepository
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.TimeoutCancellationException
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch

class AiViewModel(
    private val aiRepo: AiRepository = AiRepository()
) : ViewModel() {

    private val _conciergeState = MutableStateFlow<UiState<String>>(UiState.Success(""))
    val conciergeState: StateFlow<UiState<String>> = _conciergeState.asStateFlow()

    private val _draftAssistLoading = MutableStateFlow(false)
    val draftAssistLoading: StateFlow<Boolean> = _draftAssistLoading.asStateFlow()

    private val _translatedMap = MutableStateFlow<Map<String, String>>(emptyMap())
    val translatedMap: StateFlow<Map<String, String>> = _translatedMap.asStateFlow()

    private val _translatingId = MutableStateFlow<String?>(null)
    val translatingId: StateFlow<String?> = _translatingId.asStateFlow()

    private val _aiError = MutableStateFlow<String?>(null)
    val aiError: StateFlow<String?> = _aiError.asStateFlow()

    fun clearAiError() {
        _aiError.value = null
    }

    fun polishDraft(
        draft: String,
        contextType: String,
        city: String,
        onResult: (String) -> Unit
    ) {
        if (draft.isBlank()) return
        viewModelScope.launch {
            _draftAssistLoading.value = true
            _aiError.value = null
            try {
                val polished = aiRepo.enhanceCopy(draft, contextType, city)
                onResult(polished)
            } catch (e: TimeoutCancellationException) {
                Log.e(TAG, "Draft polish timed out", e)
                _aiError.value = "Gemini AI timed out. Please try again."
            } catch (e: CancellationException) {
                throw e
            } catch (e: Exception) {
                Log.e(TAG, "Draft polish failed", e)
                _aiError.value = e.localizedMessage ?: "AI assist failed."
            } finally {
                _draftAssistLoading.value = false
            }
        }
    }

    fun translateItem(itemId: String, sourceText: String, targetLanguageLabel: String) {
        viewModelScope.launch {
            _translatingId.value = itemId
            _aiError.value = null
            try {
                val translated = aiRepo.translateText(sourceText, targetLanguageLabel)
                _translatedMap.value = _translatedMap.value + (itemId to translated)
            } catch (e: TimeoutCancellationException) {
                Log.e(TAG, "Translation timed out", e)
                _aiError.value = "Translation timed out. Please retry."
            } catch (e: CancellationException) {
                throw e
            } catch (e: Exception) {
                Log.e(TAG, "Translation failed", e)
                _aiError.value = e.localizedMessage ?: "Translation failed."
            } finally {
                _translatingId.value = null
            }
        }
    }

    fun askCityConcierge(city: String, neighborhood: String, question: String, languageLabel: String) {
        if (question.isBlank()) return
        viewModelScope.launch {
            _conciergeState.value = UiState.Loading
            try {
                aiRepo.askCityConciergeStream(city, neighborhood, question, languageLabel).collect { partial ->
                    _conciergeState.value = UiState.Success(partial)
                }
            } catch (e: TimeoutCancellationException) {
                Log.e(TAG, "Concierge timed out", e)
                _conciergeState.value = UiState.Error("Request timed out after 60s. Please retry.")
            } catch (e: CancellationException) {
                throw e
            } catch (e: Exception) {
                Log.e(TAG, "Concierge failed", e)
                _conciergeState.value = UiState.Error(e.localizedMessage ?: "CityLoop AI Guide failed.")
            }
        }
    }

    private companion object {
        const val TAG = "AiVM"
    }
}
