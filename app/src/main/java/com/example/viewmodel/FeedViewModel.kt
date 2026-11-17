package com.example.viewmodel

import android.util.Log
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.core.UiState
import com.example.core.unwrapFirestoreMessage
import com.example.data.SocialRepository
import com.example.models.CityCommunity
import com.example.models.CityPost
import com.example.models.ModerationReport
import com.example.models.PostComment
import com.example.models.UserProfile
import com.google.firebase.firestore.FirebaseFirestoreException
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.FlowPreview
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.catch
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.debounce
import kotlinx.coroutines.flow.distinctUntilChanged
import kotlinx.coroutines.flow.flatMapLatest
import kotlinx.coroutines.flow.flowOf
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.onEach
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch

@OptIn(ExperimentalCoroutinesApi::class, FlowPreview::class)
class FeedViewModel(
    private val socialRepo: SocialRepository,
    private val cityFlow: StateFlow<String>,
    private val currentUserId: String,
    private val isAdmin: Boolean
) : ViewModel() {

    private val _activePostId = MutableStateFlow<String?>(null)
    private val _refreshNonce = MutableStateFlow(0)
    private val _isRefreshing = MutableStateFlow(false)
    val isRefreshing: StateFlow<Boolean> = _isRefreshing.asStateFlow()

    private val _feedback = MutableStateFlow<String?>(null)
    val feedback: StateFlow<String?> = _feedback.asStateFlow()

    private val _likedPostIds = MutableStateFlow<Set<String>>(emptySet())
    val likedPostIds: StateFlow<Set<String>> = _likedPostIds.asStateFlow()
    private val checkedLikePostIds = mutableSetOf<String>()

    private val _joinedCommunityIds = MutableStateFlow<Set<String>>(emptySet())
    val joinedCommunityIds: StateFlow<Set<String>> = _joinedCommunityIds.asStateFlow()

    private val _searchQuery = MutableStateFlow("")
    val searchQuery: StateFlow<String> = _searchQuery.asStateFlow()

    private val debouncedSearchQuery = _searchQuery
        .map { it.trim() }
        .debounce { query -> if (query.isEmpty()) 0L else 250L }
        .distinctUntilChanged()

    val postsState: StateFlow<UiState<List<CityPost>>> = combine(
        cityFlow,
        _refreshNonce,
        debouncedSearchQuery
    ) { city, _, query -> city to query }
        .flatMapLatest { (city, query) ->
            if (query.isBlank()) socialRepo.observeCityPosts(city)
            else socialRepo.searchCityPosts(city, query)
        }
        .onEach { posts ->
            val uncheckedIds = posts.map { it.id }.filter { it.isNotBlank() && checkedLikePostIds.add(it) }
            if (uncheckedIds.isNotEmpty()) {
                viewModelScope.launch {
                    val liked = socialRepo.fetchLikedPostIds(uncheckedIds)
                    if (liked.isNotEmpty()) {
                        _likedPostIds.value = _likedPostIds.value + liked
                    }
                }
            }
        }
        .map<List<CityPost>, UiState<List<CityPost>>> { UiState.Success(it) }
        .catch { error ->
            val fs = generateSequence(error) { it.cause }.filterIsInstance<FirebaseFirestoreException>().firstOrNull()
            if (fs == null && error is CancellationException) throw error
            Log.w(TAG, "Error observing city posts", fs ?: error)
            emit(UiState.Error(unwrapFirestoreMessage(error, "Failed to load city feed")))
        }
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000L), UiState.Loading)

    val myPostsState: StateFlow<UiState<List<CityPost>>> = socialRepo.observeUserPosts(currentUserId)
        .map<List<CityPost>, UiState<List<CityPost>>> { UiState.Success(it) }
        .catch { error ->
            val fs = generateSequence(error) { it.cause }.filterIsInstance<FirebaseFirestoreException>().firstOrNull()
            if (fs == null && error is CancellationException) throw error
            emit(UiState.Error(unwrapFirestoreMessage(error, "Failed to load your posts")))
        }
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000L), UiState.Loading)

    val communitiesState: StateFlow<UiState<List<CityCommunity>>> = cityFlow
        .flatMapLatest { city -> socialRepo.observeCityCommunities(city) }
        .map<List<CityCommunity>, UiState<List<CityCommunity>>> { UiState.Success(it) }
        .catch { error ->
            val fs = generateSequence(error) { it.cause }.filterIsInstance<FirebaseFirestoreException>().firstOrNull()
            if (fs == null && error is CancellationException) throw error
            emit(UiState.Error(unwrapFirestoreMessage(error, "Failed to load communities")))
        }
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000L), UiState.Loading)

    val activeCommentsState: StateFlow<UiState<List<PostComment>>> = _activePostId
        .flatMapLatest { postId ->
            if (postId.isNullOrBlank()) flowOf(emptyList())
            else socialRepo.observePostComments(postId)
        }
        .map<List<PostComment>, UiState<List<PostComment>>> { UiState.Success(it) }
        .catch { error ->
            val fs = generateSequence(error) { it.cause }.filterIsInstance<FirebaseFirestoreException>().firstOrNull()
            if (fs == null && error is CancellationException) throw error
            emit(UiState.Error(unwrapFirestoreMessage(error, "Failed to load comments")))
        }
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000L), UiState.Loading)

    val reportsState: StateFlow<UiState<List<ModerationReport>>> = socialRepo.observeReports(isAdmin, currentUserId)
        .map<List<ModerationReport>, UiState<List<ModerationReport>>> { UiState.Success(it) }
        .catch { error ->
            val fs = generateSequence(error) { it.cause }.filterIsInstance<FirebaseFirestoreException>().firstOrNull()
            if (fs == null && error is CancellationException) throw error
            emit(UiState.Error(unwrapFirestoreMessage(error, "Failed to load reports")))
        }
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000L), UiState.Loading)

    fun clearFeedback() {
        _feedback.value = null
    }

    fun updateSearchQuery(query: String) {
        _searchQuery.value = query.take(80)
    }

    fun triggerRefresh() {
        if (_isRefreshing.value) return
        viewModelScope.launch {
            _isRefreshing.value = true
            try {
                val city = cityFlow.value
                val res = socialRepo.refreshCityPosts(city, _searchQuery.value)
                res.onSuccess { freshPosts ->
                    val postIds = freshPosts.map { it.id }.filter { it.isNotBlank() }
                    checkedLikePostIds.clear()
                    checkedLikePostIds.addAll(postIds)
                    _likedPostIds.value = socialRepo.fetchLikedPostIds(postIds)
                }.onFailure { err ->
                    _feedback.value = unwrapFirestoreMessage(err, "Could not refresh feed")
                }
                _refreshNonce.value += 1
            } finally {
                _isRefreshing.value = false
            }
        }
    }

    fun openComments(postId: String?) {
        _activePostId.value = postId
    }

    fun createPost(
        profile: UserProfile,
        postType: String,
        caption: String,
        neighborhood: String,
        mediaUri: String?,
        videoDurationSec: Long,
        tagsRaw: String,
        communityId: String = "",
        communityName: String = "",
        onSuccess: () -> Unit = {},
        onError: (String) -> Unit = {}
    ) {
        viewModelScope.launch {
            val tags = tagsRaw.split(",", " ").map { it.trim().removePrefix("#") }.filter { it.isNotEmpty() }
            val res = socialRepo.createPost(
                profile = profile,
                postType = postType,
                caption = caption,
                neighborhood = neighborhood,
                mediaUri = mediaUri,
                videoDurationSec = videoDurationSec,
                tags = tags,
                communityId = communityId,
                communityName = communityName
            )
            res.fold(
                onSuccess = {
                    _feedback.value = "Published to ${profile.city}!"
                    onSuccess()
                },
                onFailure = { err ->
                    val msg = err.localizedMessage ?: "Failed to publish post"
                    _feedback.value = msg
                    onError(msg)
                }
            )
        }
    }

    fun toggleLike(post: CityPost, myName: String) {
        viewModelScope.launch {
            val res = socialRepo.toggleLike(post, myName)
            res.onSuccess { liked ->
                _likedPostIds.value = if (liked) _likedPostIds.value + post.id else _likedPostIds.value - post.id
            }.onFailure {
                _feedback.value = it.localizedMessage ?: "Could not update like"
            }
        }
    }

    fun addComment(post: CityPost, profile: UserProfile, body: String, parentCommentId: String = "") {
        viewModelScope.launch {
            val res = socialRepo.addComment(post, profile, body, parentCommentId)
            res.onFailure { _feedback.value = it.localizedMessage ?: "Failed to add comment" }
        }
    }

    fun deletePost(postId: String) {
        viewModelScope.launch {
            val res = socialRepo.deletePost(postId)
            _feedback.value = res.fold(
                onSuccess = { "Post deleted" },
                onFailure = { it.localizedMessage ?: "Failed to delete post" }
            )
        }
    }

    fun createCommunity(profile: UserProfile, name: String, description: String, category: String, emoji: String, onSuccess: () -> Unit = {}) {
        viewModelScope.launch {
            val res = socialRepo.createCommunity(profile, name, description, category, emoji)
            res.fold(
                onSuccess = {
                    _feedback.value = "Community created in ${profile.city}!"
                    onSuccess()
                },
                onFailure = { _feedback.value = it.localizedMessage ?: "Failed to create community" }
            )
        }
    }

    fun toggleJoinCommunity(community: CityCommunity, profile: UserProfile) {
        viewModelScope.launch {
            val res = socialRepo.toggleJoinCommunity(community, profile)
            res.onSuccess { joined ->
                _joinedCommunityIds.value = if (joined) _joinedCommunityIds.value + community.id else _joinedCommunityIds.value - community.id
                _feedback.value = if (joined) "Joined ${community.name}" else "Left ${community.name}"
            }.onFailure {
                _feedback.value = it.localizedMessage ?: "Could not update membership"
            }
        }
    }

    fun reportContent(targetType: String, targetId: String, reason: String, city: String) {
        viewModelScope.launch {
            val res = socialRepo.submitReport(targetType, targetId, reason, city)
            _feedback.value = res.fold(
                onSuccess = { "Report submitted to moderators" },
                onFailure = { it.localizedMessage ?: "Failed to submit report" }
            )
        }
    }

    fun moderatePost(postId: String, status: String, note: String) {
        viewModelScope.launch {
            val res = socialRepo.adminModeratePost(postId, status, note)
            _feedback.value = res.fold(
                onSuccess = { "Post marked as $status" },
                onFailure = { it.localizedMessage ?: "Moderation failed" }
            )
        }
    }

    fun resolveReport(reportId: String, status: String) {
        viewModelScope.launch {
            val res = socialRepo.adminResolveReport(reportId, status)
            _feedback.value = res.fold(
                onSuccess = { "Report $status" },
                onFailure = { it.localizedMessage ?: "Could not update report" }
            )
        }
    }

    private companion object {
        const val TAG = "FeedVM"
    }
}
