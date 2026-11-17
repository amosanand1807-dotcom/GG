package com.example.viewmodel

import android.util.Log
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.core.UiState
import com.example.core.unwrapFirestoreMessage
import com.example.data.CommerceRepository
import com.example.models.ChatMessage
import com.example.models.CityEvent
import com.example.models.Conversation
import com.example.models.LocalBusiness
import com.example.models.MarketplaceListing
import com.example.models.UserProfile
import com.google.firebase.firestore.FirebaseFirestoreException
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.catch
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.flatMapLatest
import kotlinx.coroutines.flow.flowOf
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.onEach
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch

@OptIn(ExperimentalCoroutinesApi::class)
class CommerceViewModel(
    private val commerceRepo: CommerceRepository,
    private val cityFlow: StateFlow<String>,
    private val currentUserId: String
) : ViewModel() {

    private val _activeConversation = MutableStateFlow<Conversation?>(null)
    val activeConversation: StateFlow<Conversation?> = _activeConversation.asStateFlow()

    private val _rsvpedEventIds = MutableStateFlow<Set<String>>(emptySet())
    val rsvpedEventIds: StateFlow<Set<String>> = _rsvpedEventIds.asStateFlow()
    private val checkedRsvpEventIds = mutableSetOf<String>()

    private val _pendingRsvpIds = MutableStateFlow<Set<String>>(emptySet())
    val pendingRsvpIds: StateFlow<Set<String>> = _pendingRsvpIds.asStateFlow()

    private val _eventsRefreshNonce = MutableStateFlow(0)
    private val _isRefreshingEvents = MutableStateFlow(false)
    val isRefreshingEvents: StateFlow<Boolean> = _isRefreshingEvents.asStateFlow()

    private val _isSendingMessage = MutableStateFlow(false)
    val isSendingMessage: StateFlow<Boolean> = _isSendingMessage.asStateFlow()

    private val _feedback = MutableStateFlow<String?>(null)
    val feedback: StateFlow<String?> = _feedback.asStateFlow()

    val listingsState: StateFlow<UiState<List<MarketplaceListing>>> = cityFlow
        .flatMapLatest { city -> commerceRepo.observeCityListings(city) }
        .map<List<MarketplaceListing>, UiState<List<MarketplaceListing>>> { UiState.Success(it) }
        .catch { error ->
            val fs = generateSequence(error) { it.cause }.filterIsInstance<FirebaseFirestoreException>().firstOrNull()
            if (fs == null && error is CancellationException) throw error
            Log.w(TAG, "Error observing listings", fs ?: error)
            emit(UiState.Error(unwrapFirestoreMessage(error, "Failed to load marketplace")))
        }
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000L), UiState.Loading)

    val conversationsState: StateFlow<UiState<List<Conversation>>> = commerceRepo.observeConversations(currentUserId)
        .map<List<Conversation>, UiState<List<Conversation>>> { UiState.Success(it) }
        .catch { error ->
            val fs = generateSequence(error) { it.cause }.filterIsInstance<FirebaseFirestoreException>().firstOrNull()
            if (fs == null && error is CancellationException) throw error
            emit(UiState.Error(unwrapFirestoreMessage(error, "Failed to load inbox")))
        }
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000L), UiState.Loading)

    val activeMessagesState: StateFlow<UiState<List<ChatMessage>>> = _activeConversation
        .flatMapLatest { conv ->
            if (conv == null) flowOf(emptyList())
            else commerceRepo.observeMessages(conv.id, currentUserId)
        }
        .map<List<ChatMessage>, UiState<List<ChatMessage>>> { UiState.Success(it) }
        .catch { error ->
            val fs = generateSequence(error) { it.cause }.filterIsInstance<FirebaseFirestoreException>().firstOrNull()
            if (fs == null && error is CancellationException) throw error
            emit(UiState.Error(unwrapFirestoreMessage(error, "Failed to load messages")))
        }
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000L), UiState.Loading)

    val eventsState: StateFlow<UiState<List<CityEvent>>> = combine(cityFlow, _eventsRefreshNonce) { city, _ -> city }
        .flatMapLatest { city -> commerceRepo.observeCityEvents(city) }
        .onEach { events ->
            val uncheckedIds = events.map { it.id }.filter { it.isNotBlank() && checkedRsvpEventIds.add(it) }
            if (uncheckedIds.isNotEmpty()) {
                viewModelScope.launch {
                    val rsvped = commerceRepo.fetchRsvpedEventIds(uncheckedIds)
                    if (rsvped.isNotEmpty()) {
                        _rsvpedEventIds.value = _rsvpedEventIds.value + rsvped
                    }
                }
            }
        }
        .map<List<CityEvent>, UiState<List<CityEvent>>> { UiState.Success(it) }
        .catch { error ->
            val fs = generateSequence(error) { it.cause }.filterIsInstance<FirebaseFirestoreException>().firstOrNull()
            if (fs == null && error is CancellationException) throw error
            emit(UiState.Error(unwrapFirestoreMessage(error, "Failed to load events")))
        }
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000L), UiState.Loading)

    val businessesState: StateFlow<UiState<List<LocalBusiness>>> = cityFlow
        .flatMapLatest { city -> commerceRepo.observeCityBusinesses(city) }
        .map<List<LocalBusiness>, UiState<List<LocalBusiness>>> { UiState.Success(it) }
        .catch { error ->
            val fs = generateSequence(error) { it.cause }.filterIsInstance<FirebaseFirestoreException>().firstOrNull()
            if (fs == null && error is CancellationException) throw error
            emit(UiState.Error(unwrapFirestoreMessage(error, "Failed to load local businesses")))
        }
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000L), UiState.Loading)

    fun clearFeedback() {
        _feedback.value = null
    }

    fun triggerEventsRefresh() {
        if (_isRefreshingEvents.value) return
        viewModelScope.launch {
            _isRefreshingEvents.value = true
            try {
                val res = commerceRepo.refreshCityEvents(cityFlow.value)
                res.onSuccess { freshEvents ->
                    val ids = freshEvents.map { it.id }.filter { it.isNotBlank() }
                    checkedRsvpEventIds.clear()
                    checkedRsvpEventIds.addAll(ids)
                    _rsvpedEventIds.value = commerceRepo.fetchRsvpedEventIds(ids)
                }.onFailure { err ->
                    _feedback.value = unwrapFirestoreMessage(err, "Could not refresh events")
                }
                _eventsRefreshNonce.value += 1
            } finally {
                _isRefreshingEvents.value = false
            }
        }
    }

    fun selectConversation(conversation: Conversation?) {
        _activeConversation.value = conversation
    }

    fun createListing(
        profile: UserProfile,
        title: String,
        description: String,
        priceInr: Long,
        category: String,
        condition: String,
        neighborhood: String,
        mediaUri: String?,
        onSuccess: () -> Unit = {}
    ) {
        viewModelScope.launch {
            val res = commerceRepo.createListing(profile, title, description, priceInr, category, condition, neighborhood, mediaUri)
            res.fold(
                onSuccess = {
                    _feedback.value = "Listing posted in ${profile.city}!"
                    onSuccess()
                },
                onFailure = { _feedback.value = it.localizedMessage ?: "Could not publish listing" }
            )
        }
    }

    fun markListingSold(listingId: String) {
        viewModelScope.launch {
            val res = commerceRepo.markListingSold(listingId)
            _feedback.value = res.fold(
                onSuccess = { "Listing marked as Sold" },
                onFailure = { it.localizedMessage ?: "Failed to mark sold" }
            )
        }
    }

    fun startChatWith(
        myProfile: UserProfile,
        otherUserId: String,
        otherUserName: String,
        contextTitle: String,
        onOpened: (Conversation) -> Unit
    ) {
        viewModelScope.launch {
            val res = commerceRepo.startOrGetConversation(myProfile, otherUserId, otherUserName, contextTitle)
            res.fold(
                onSuccess = { conv ->
                    _activeConversation.value = conv
                    onOpened(conv)
                },
                onFailure = { _feedback.value = it.localizedMessage ?: "Could not open chat" }
            )
        }
    }

    fun sendMessage(
        conversation: Conversation,
        myName: String,
        text: String,
        onSent: () -> Unit = {}
    ) {
        val clean = text.trim()
        if (clean.isEmpty() || _isSendingMessage.value) return
        viewModelScope.launch {
            _isSendingMessage.value = true
            try {
                val res = commerceRepo.sendMessage(conversation, myName, clean)
                res.onSuccess {
                    onSent()
                }.onFailure {
                    _feedback.value = unwrapFirestoreMessage(it, "Failed to send message")
                }
            } finally {
                _isSendingMessage.value = false
            }
        }
    }

    fun createEvent(
        profile: UserProfile,
        title: String,
        description: String,
        venue: String,
        dateLabel: String,
        category: String,
        priceInr: Long,
        onSuccess: () -> Unit = {}
    ) {
        viewModelScope.launch {
            val res = commerceRepo.createEvent(profile, title, description, venue, dateLabel, category, priceInr)
            res.fold(
                onSuccess = {
                    _feedback.value = "Event published!"
                    onSuccess()
                },
                onFailure = { _feedback.value = it.localizedMessage ?: "Could not create event" }
            )
        }
    }

    fun toggleRsvp(event: CityEvent, profile: UserProfile) {
        if (event.id in _pendingRsvpIds.value) return
        viewModelScope.launch {
            _pendingRsvpIds.value = _pendingRsvpIds.value + event.id
            try {
                val res = commerceRepo.toggleEventRsvp(event, profile)
                res.onSuccess { joined ->
                    _rsvpedEventIds.value = if (joined) _rsvpedEventIds.value + event.id else _rsvpedEventIds.value - event.id
                    _feedback.value = if (joined) "RSVP confirmed for ${event.title}" else "RSVP cancelled"
                }.onFailure {
                    _feedback.value = it.localizedMessage ?: "RSVP failed"
                }
            } finally {
                _pendingRsvpIds.value = _pendingRsvpIds.value - event.id
            }
        }
    }

    fun createBusiness(
        profile: UserProfile,
        name: String,
        category: String,
        description: String,
        address: String,
        offerTitle: String,
        offerCode: String,
        discountLabel: String,
        onSuccess: () -> Unit = {}
    ) {
        viewModelScope.launch {
            val res = commerceRepo.createBusiness(profile, name, category, description, address, offerTitle, offerCode, discountLabel)
            res.fold(
                onSuccess = {
                    _feedback.value = "Business & offer published!"
                    onSuccess()
                },
                onFailure = { _feedback.value = it.localizedMessage ?: "Could not register business" }
            )
        }
    }

    fun verifyBusiness(businessId: String, verified: Boolean) {
        viewModelScope.launch {
            val res = commerceRepo.adminVerifyBusiness(businessId, verified)
            _feedback.value = res.fold(
                onSuccess = { if (verified) "Business verified" else "Verification removed" },
                onFailure = { it.localizedMessage ?: "Admin update failed" }
            )
        }
    }

    private companion object {
        const val TAG = "CommerceVM"
    }
}
