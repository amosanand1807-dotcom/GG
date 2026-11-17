package com.example.viewmodel

import android.util.Log
import com.example.core.AppLanguage
import com.example.core.CityData
import com.example.core.PreferencesStore
import com.example.core.ThemeMode
import com.example.core.UiState
import com.example.core.unwrapFirestoreMessage
import com.example.data.UserRepository
import com.example.models.AppNotification
import com.example.models.FollowEdge
import com.example.models.SavedItem
import com.example.models.UserPrivateSettings
import com.example.models.UserProfile
import com.google.firebase.Firebase
import com.google.firebase.auth.auth
import com.google.firebase.firestore.FirebaseFirestoreException
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.catch
import kotlinx.coroutines.flow.flatMapLatest
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope

@OptIn(ExperimentalCoroutinesApi::class)
class SessionViewModel(
    private val userRepo: UserRepository,
    private val prefs: PreferencesStore,
    val currentUserId: String
) : ViewModel() {

    private val _selectedCity = MutableStateFlow(CityData.defaultCity)
    val selectedCity: StateFlow<String> = _selectedCity.asStateFlow()

    private val _actionStatus = MutableStateFlow<String?>(null)
    val actionStatus: StateFlow<String?> = _actionStatus.asStateFlow()

    private val _isBusy = MutableStateFlow(false)
    val isBusy: StateFlow<Boolean> = _isBusy.asStateFlow()

    val language: StateFlow<AppLanguage> = prefs.languageFlow
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000L), AppLanguage.ENGLISH)

    val themeMode: StateFlow<ThemeMode> = prefs.themeModeFlow
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000L), ThemeMode.SYSTEM)

    val customAvatarUri: StateFlow<String?> = prefs.customAvatarUriFlow(currentUserId)
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000L), null)

    val profileState: StateFlow<UiState<UserProfile?>> = userRepo.observeProfile(currentUserId)
        .map<UserProfile?, UiState<UserProfile?>> { profile ->
            if (profile != null && profile.city.isNotBlank()) {
                _selectedCity.value = profile.city
            }
            UiState.Success(profile)
        }
        .catch { error ->
            val fs = generateSequence(error) { it.cause }.filterIsInstance<FirebaseFirestoreException>().firstOrNull()
            if (fs == null && error is CancellationException) throw error
            Log.w(TAG, "Profile observe error", fs ?: error)
            emit(UiState.Error(unwrapFirestoreMessage(error, "Failed to load profile")))
        }
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000L), UiState.Loading)

    val privateSettings: StateFlow<UserPrivateSettings?> = userRepo.observePrivateSettings(currentUserId)
        .catch { error ->
            val fs = generateSequence(error) { it.cause }.filterIsInstance<FirebaseFirestoreException>().firstOrNull()
            if (fs == null && error is CancellationException) throw error
            Log.w(TAG, "Private settings error", fs ?: error)
            emit(null)
        }
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000L), null)

    val following: StateFlow<List<FollowEdge>> = userRepo.observeFollowing(currentUserId)
        .catch { error ->
            val fs = generateSequence(error) { it.cause }.filterIsInstance<FirebaseFirestoreException>().firstOrNull()
            if (fs == null && error is CancellationException) throw error
            emit(emptyList())
        }
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000L), emptyList())

    val savedItems: StateFlow<List<SavedItem>> = userRepo.observeSavedItems(currentUserId)
        .catch { error ->
            val fs = generateSequence(error) { it.cause }.filterIsInstance<FirebaseFirestoreException>().firstOrNull()
            if (fs == null && error is CancellationException) throw error
            emit(emptyList())
        }
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000L), emptyList())

    val notifications: StateFlow<List<AppNotification>> = userRepo.observeNotifications(currentUserId)
        .catch { error ->
            val fs = generateSequence(error) { it.cause }.filterIsInstance<FirebaseFirestoreException>().firstOrNull()
            if (fs == null && error is CancellationException) throw error
            emit(emptyList())
        }
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000L), emptyList())

    val cityNeighbors: StateFlow<List<UserProfile>> = _selectedCity
        .flatMapLatest { city -> userRepo.observeCityNeighbors(city) }
        .catch { error ->
            val fs = generateSequence(error) { it.cause }.filterIsInstance<FirebaseFirestoreException>().firstOrNull()
            if (fs == null && error is CancellationException) throw error
            emit(emptyList())
        }
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000L), emptyList())

    val isAdminUser: Boolean
        get() {
            val email = Firebase.auth.currentUser?.email
            val verified = Firebase.auth.currentUser?.isEmailVerified == true
            val profile = (profileState.value as? UiState.Success)?.data
            return (email == CityData.ADMIN_EMAIL && verified) || profile?.role == "admin" || profile?.role == "moderator"
        }

    init {
        viewModelScope.launch {
            prefs.selectedCityFlow.collect { savedCity ->
                if ((profileState.value as? UiState.Success)?.data == null) {
                    _selectedCity.value = savedCity
                }
            }
        }
    }

    fun clearStatus() {
        _actionStatus.value = null
    }

    fun selectCity(city: String) {
        _selectedCity.value = city
        viewModelScope.launch { prefs.setSelectedCity(city) }
    }

    fun setLanguage(lang: AppLanguage) {
        viewModelScope.launch {
            prefs.setLanguage(lang)
            userRepo.updatePrivateSettings(languageCode = lang.code)
        }
    }

    fun setThemeMode(mode: ThemeMode) {
        viewModelScope.launch {
            prefs.setThemeMode(mode)
            userRepo.updatePrivateSettings(darkThemeMode = mode.key)
        }
    }

    fun completeOnboarding(
        displayName: String,
        handle: String,
        city: String,
        neighborhood: String,
        bio: String,
        avatarEmoji: String,
        interests: List<String>
    ) {
        viewModelScope.launch {
            _isBusy.value = true
            _selectedCity.value = city
            prefs.setSelectedCity(city)
            val result = userRepo.saveOnboardingProfile(
                displayName = displayName,
                handle = handle,
                city = city,
                neighborhood = neighborhood,
                bio = bio,
                avatarEmoji = avatarEmoji,
                interests = interests,
                languageCode = language.value.code
            )
            _isBusy.value = false
            result.onFailure { _actionStatus.value = it.localizedMessage ?: "Failed to save onboarding." }
        }
    }

    fun updateProfile(
        displayName: String,
        bio: String,
        city: String,
        neighborhood: String,
        avatarEmoji: String
    ) {
        viewModelScope.launch {
            _isBusy.value = true
            _selectedCity.value = city
            prefs.setSelectedCity(city)
            val result = userRepo.updateProfile(displayName, bio, city, neighborhood, avatarEmoji)
            _isBusy.value = false
            _actionStatus.value = result.fold(
                onSuccess = { "Profile updated" },
                onFailure = { it.localizedMessage ?: "Update failed" }
            )
        }
    }

    fun updateBio(newBio: String, currentProfile: UserProfile) {
        viewModelScope.launch {
            _isBusy.value = true
            val result = userRepo.updateProfile(
                displayName = currentProfile.displayName,
                bio = newBio,
                city = currentProfile.city,
                neighborhood = currentProfile.neighborhood,
                avatarEmoji = currentProfile.avatarEmoji
            )
            _isBusy.value = false
            _actionStatus.value = result.fold(
                onSuccess = { "Bio updated" },
                onFailure = { it.localizedMessage ?: "Failed to update bio" }
            )
        }
    }

    fun updateCustomAvatarUri(uri: String?) {
        viewModelScope.launch {
            prefs.setCustomAvatarUri(currentUserId, uri)
            _actionStatus.value = if (uri.isNullOrBlank()) "Profile photo reset" else "Profile photo updated"
        }
    }

    fun updatePrivacy(pushEnabled: Boolean, privateProfile: Boolean) {
        viewModelScope.launch {
            val res = userRepo.updatePrivateSettings(pushEnabled = pushEnabled, privateProfile = privateProfile)
            res.onFailure { _actionStatus.value = it.localizedMessage ?: "Settings update failed" }
        }
    }

    fun toggleFollow(target: UserProfile, myName: String) {
        val isFollowing = following.value.any { it.targetUserId == target.userId }
        viewModelScope.launch {
            val res = userRepo.toggleFollow(target, isFollowing, myName)
            _actionStatus.value = res.fold(
                onSuccess = { if (isFollowing) "Unfollowed ${target.displayName}" else "Following ${target.displayName}" },
                onFailure = { it.localizedMessage ?: "Follow action failed" }
            )
        }
    }

    fun toggleSave(itemType: String, targetId: String, title: String, subtitle: String, city: String) {
        val isSaved = savedItems.value.any { it.itemType == itemType && it.targetId == targetId }
        viewModelScope.launch {
            val res = userRepo.toggleSaveItem(itemType, targetId, title, subtitle, city, isSaved)
            _actionStatus.value = res.fold(
                onSuccess = { if (isSaved) "Removed from Saved" else "Saved to bookmarks" },
                onFailure = { it.localizedMessage ?: "Could not update bookmark" }
            )
        }
    }

    fun markNotificationRead(notifId: String) {
        viewModelScope.launch { userRepo.markNotificationRead(notifId) }
    }

    fun toggleBlockUser(targetUserId: String, currentBlocked: List<String>) {
        viewModelScope.launch {
            val res = userRepo.toggleBlockUser(targetUserId, currentBlocked)
            _actionStatus.value = res.fold(
                onSuccess = { "Updated blocked users list" },
                onFailure = { it.localizedMessage ?: "Could not block user" }
            )
        }
    }

    fun setPlanTier(tier: String) {
        viewModelScope.launch {
            val res = userRepo.updatePlanTier(tier)
            _actionStatus.value = res.fold(
                onSuccess = { if (tier == "plus") "Welcome to CityLoop Plus!" else "Switched to Free plan" },
                onFailure = { it.localizedMessage ?: "Plan update failed" }
            )
        }
    }

    fun deleteAccount(onDeleted: () -> Unit) {
        viewModelScope.launch {
            _isBusy.value = true
            val res = userRepo.deleteAccountData()
            _isBusy.value = false
            res.fold(
                onSuccess = { onDeleted() },
                onFailure = { _actionStatus.value = it.localizedMessage ?: "Re-authenticate with Google before deleting account." }
            )
        }
    }

    private companion object {
        const val TAG = "SessionVM"
    }
}
