package com.dr.tech.puretube.features.subscriptions

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.dr.tech.puretube.core.data.repository.SubscriptionRepository
import com.dr.tech.puretube.core.database.entity.SubscriptionEntity
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch

/**
 * UI State for the Subscriptions management and portability hub.
 */
data class SubscriptionsUiState(
    val isLoading: Boolean = true,
    val isActionInProgress: Boolean = false,
    val isActivatingPack: Boolean = false,
    val isImporting: Boolean = false,
    val subscriptions: List<SubscriptionEntity> = emptyList(),
    val errorMessage: String? = null,
    val userNoticeMessage: String? = null,
    val channelPendingUnsubscribe: SubscriptionEntity? = null
)

/**
 * ViewModel managing channel discovery, addition, unsubscription,
 * starter pack activation, and data import/export.
 */
class SubscriptionsViewModel(
    private val subscriptionRepository: SubscriptionRepository,
    private val ioDispatcher: kotlinx.coroutines.CoroutineDispatcher = Dispatchers.IO
) : ViewModel() {

    private val _uiState = MutableStateFlow(SubscriptionsUiState())
    val uiState: StateFlow<SubscriptionsUiState> = _uiState.asStateFlow()

    init {
        observeSubscriptions()
    }

    private fun observeSubscriptions() {
        viewModelScope.launch {
            subscriptionRepository.getSubscriptionsFlow().collect { list ->
                _uiState.update {
                    it.copy(
                        subscriptions = list,
                        isLoading = false
                    )
                }
            }
        }
    }

    /**
     * Resolves and subscribes to a channel via URL or @handle.
     */
    fun addChannel(urlOrHandle: String) {
        val trimmed = urlOrHandle.trim()
        if (trimmed.isBlank() || _uiState.value.isActionInProgress) return

        _uiState.update {
            it.copy(
                isActionInProgress = true,
                errorMessage = null,
                userNoticeMessage = null
            )
        }

        viewModelScope.launch(ioDispatcher) {
            val result = subscriptionRepository.fetchChannelDetails(trimmed)

            result.fold(
                onSuccess = { entity ->
                    subscriptionRepository.subscribe(entity)
                    _uiState.update {
                        it.copy(
                            isActionInProgress = false,
                            userNoticeMessage = "تم الاشتراك في قناة '${entity.channelName}' بنجاح"
                        )
                    }
                },
                onFailure = { error ->
                    _uiState.update {
                        it.copy(
                            isActionInProgress = false,
                            errorMessage = error.localizedMessage ?: "تعذر العثور على القناة أو التحقق من الرابط"
                        )
                    }
                }
            )
        }
    }

    /**
     * Activates or restores the Curated Starter Pack channels.
     */
    fun activateStarterPack() {
        if (_uiState.value.isActivatingPack) return

        _uiState.update { it.copy(isActivatingPack = true, errorMessage = null) }
        viewModelScope.launch {
            try {
                subscriptionRepository.reactivateStarterPack()
                _uiState.update {
                    it.copy(
                        isActivatingPack = false,
                        userNoticeMessage = "تم تفعيل حزمة القنوات المختارة بنجاح"
                    )
                }
            } catch (e: Exception) {
                _uiState.update {
                    it.copy(
                        isActivatingPack = false,
                        errorMessage = e.localizedMessage ?: "فشل في تفعيل حزمة القنوات"
                    )
                }
            }
        }
    }

    /**
     * Prompts the confirmation dialog before unsubscription.
     */
    fun requestUnsubscribe(channel: SubscriptionEntity) {
        _uiState.update { it.copy(channelPendingUnsubscribe = channel) }
    }

    /**
     * Confirms and executes channel unsubscription.
     */
    fun confirmUnsubscribe() {
        val channel = _uiState.value.channelPendingUnsubscribe ?: return
        _uiState.update { it.copy(channelPendingUnsubscribe = null) }

        viewModelScope.launch {
            subscriptionRepository.unsubscribe(channel.channelId)
            _uiState.update {
                it.copy(userNoticeMessage = "تم إلغاء الاشتراك من '${channel.channelName}'")
            }
        }
    }

    /**
     * Dismisses the unsubscription confirmation dialog.
     */
    fun dismissUnsubscribeDialog() {
        _uiState.update { it.copy(channelPendingUnsubscribe = null) }
    }

    /**
     * Imports subscriptions from NewPipe JSON string.
     */
    fun importNewPipeJson(json: String) {
        if (_uiState.value.isImporting) return

        _uiState.update { it.copy(isImporting = true, errorMessage = null) }
        viewModelScope.launch {
            val result = subscriptionRepository.importFromNewPipeJson(json)
            result.fold(
                onSuccess = { count ->
                    _uiState.update {
                        it.copy(
                            isImporting = false,
                            userNoticeMessage = "تم استيراد $count قناة بنجاح من NewPipe"
                        )
                    }
                },
                onFailure = { error ->
                    _uiState.update {
                        it.copy(
                            isImporting = false,
                            errorMessage = error.localizedMessage ?: "فشل في معالجة ملف NewPipe JSON"
                        )
                    }
                }
            )
        }
    }

    /**
     * Imports subscriptions from Google Takeout CSV string.
     */
    fun importTakeoutCsv(csv: String) {
        if (_uiState.value.isImporting) return

        _uiState.update { it.copy(isImporting = true, errorMessage = null) }
        viewModelScope.launch {
            val result = subscriptionRepository.importFromGoogleTakeoutCsv(csv)
            result.fold(
                onSuccess = { count ->
                    _uiState.update {
                        it.copy(
                            isImporting = false,
                            userNoticeMessage = "تم استيراد $count قناة بنجاح من Google Takeout"
                        )
                    }
                },
                onFailure = { error ->
                    _uiState.update {
                        it.copy(
                            isImporting = false,
                            errorMessage = error.localizedMessage ?: "فشل في معالجة ملف Google Takeout CSV"
                        )
                    }
                }
            )
        }
    }

    /**
     * Exports subscriptions to NewPipe JSON string.
     */
    suspend fun exportNewPipeJson(): String? {
        return subscriptionRepository.exportToNewPipeJson().getOrNull()
    }

    /**
     * Sets a user-facing error message.
     */
    fun showErrorMessage(message: String) {
        _uiState.update { it.copy(errorMessage = message) }
    }

    /**
     * Clears user feedback messages.
     */
    fun clearMessages() {
        _uiState.update { it.copy(errorMessage = null, userNoticeMessage = null) }
    }
}
