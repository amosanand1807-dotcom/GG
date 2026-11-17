package com.example.ui.screens

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardActions
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.Add
import androidx.compose.material.icons.rounded.Close
import androidx.compose.material.icons.rounded.DynamicFeed
import androidx.compose.material.icons.rounded.Search
import androidx.compose.material.icons.rounded.Tag
import androidx.compose.material.icons.rounded.Videocam
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FilterChip
import androidx.compose.material3.FloatingActionButton
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.OutlinedTextFieldDefaults
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.pulltorefresh.PullToRefreshBox
import androidx.compose.material3.pulltorefresh.PullToRefreshDefaults
import androidx.compose.material3.pulltorefresh.rememberPullToRefreshState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalFocusManager
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.unit.dp
import com.example.core.AppLanguage
import com.example.core.LocalStrings
import com.example.core.UiState
import com.example.core.extractTrendingHashtags
import com.example.core.matchesPostSearchQuery
import com.example.models.CityPost
import com.example.models.PostComment
import com.example.models.UserProfile
import com.example.ui.components.CitySkylineArtwork
import com.example.ui.components.ComposePostDialog
import com.example.ui.components.EmptyStateCard
import com.example.ui.components.FeedCommentsBottomSheet
import com.example.ui.components.FeedPostCard
import com.example.ui.components.ReportContentDialog
import com.example.ui.components.SkeletonPostCard

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun FeedScreen(
    activeCity: String,
    profile: UserProfile,
    postsState: UiState<List<CityPost>>,
    commentsState: UiState<List<PostComment>>,
    likedPostIds: Set<String>,
    savedTargetIds: Set<String>,
    translatedMap: Map<String, String>,
    translatingId: String?,
    currentLanguage: AppLanguage,
    searchQuery: String = "",
    onSearchQueryChange: (String) -> Unit = {},
    isRefreshing: Boolean,
    aiBusy: Boolean,
    onRefresh: () -> Unit,
    onPolishWithAi: (String, String, (String) -> Unit) -> Unit,
    onPublishPost: (
        postType: String,
        caption: String,
        neighborhood: String,
        mediaUri: String?,
        tagsRaw: String,
        onSuccess: () -> Unit,
        onError: (String) -> Unit
    ) -> Unit,
    onToggleLike: (CityPost) -> Unit,
    onOpenComments: (CityPost) -> Unit,
    onDismissComments: () -> Unit,
    onAddComment: (CityPost, String, String) -> Unit,
    onToggleSave: (CityPost) -> Unit,
    onTranslatePost: (CityPost) -> Unit,
    onDeletePost: (String) -> Unit,
    onMessageAuthor: (String, String, String) -> Unit,
    onBlockAuthor: (String) -> Unit,
    onReportPost: (String, String) -> Unit
) {
    val strings = LocalStrings.current
    val focusManager = LocalFocusManager.current
    val pullToRefreshState = rememberPullToRefreshState()
    var feedFilter by remember { mutableStateOf("all") }
    var activeCommentPost by remember { mutableStateOf<CityPost?>(null) }
    var reportPostId by remember { mutableStateOf<String?>(null) }
    var showComposeDialog by remember { mutableStateOf(false) }

    val trendingTags = remember(postsState) {
        if (postsState is UiState.Success) extractTrendingHashtags(postsState.data, maxCount = 6)
        else emptyList()
    }

    Box(modifier = Modifier.fillMaxSize()) {
        Column(modifier = Modifier.fillMaxSize()) {
            // Top Feed Search Bar (Hashtags & Keywords)
            Surface(
                color = MaterialTheme.colorScheme.surface,
                tonalElevation = 1.dp,
                modifier = Modifier.fillMaxWidth()
            ) {
                OutlinedTextField(
                    value = searchQuery,
                    onValueChange = onSearchQueryChange,
                    placeholder = {
                        Text(
                            if (currentLanguage == AppLanguage.HINDI)
                                "$activeCity में #हैशटैग या कीवर्ड खोजें..."
                            else
                                "Search #hashtags or keywords in $activeCity..."
                        )
                    },
                    leadingIcon = {
                        Icon(
                            imageVector = Icons.Rounded.Search,
                            contentDescription = if (currentLanguage == AppLanguage.HINDI) "फ़ीड खोजें" else "Search feed",
                            tint = MaterialTheme.colorScheme.primary
                        )
                    },
                    trailingIcon = {
                        if (searchQuery.isNotEmpty()) {
                            IconButton(
                                onClick = {
                                    onSearchQueryChange("")
                                    focusManager.clearFocus()
                                },
                                modifier = Modifier.testTag("feed_search_clear")
                            ) {
                                Icon(
                                    imageVector = Icons.Rounded.Close,
                                    contentDescription = if (currentLanguage == AppLanguage.HINDI) "खोज साफ़ करें" else "Clear search"
                                )
                            }
                        }
                    },
                    singleLine = true,
                    shape = RoundedCornerShape(24.dp),
                    colors = OutlinedTextFieldDefaults.colors(
                        focusedContainerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.4f),
                        unfocusedContainerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.35f),
                        focusedBorderColor = MaterialTheme.colorScheme.primary,
                        unfocusedBorderColor = MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.6f)
                    ),
                    keyboardOptions = KeyboardOptions(imeAction = ImeAction.Search),
                    keyboardActions = KeyboardActions(onSearch = { focusManager.clearFocus() }),
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 16.dp, vertical = 10.dp)
                        .testTag("feed_search_input")
                )
            }

            PullToRefreshBox(
                isRefreshing = isRefreshing,
                onRefresh = onRefresh,
                state = pullToRefreshState,
                indicator = {
                    PullToRefreshDefaults.Indicator(
                        state = pullToRefreshState,
                        isRefreshing = isRefreshing,
                        containerColor = MaterialTheme.colorScheme.primaryContainer,
                        color = MaterialTheme.colorScheme.primary,
                        modifier = Modifier.align(Alignment.TopCenter)
                    )
                },
                modifier = Modifier
                    .weight(1f)
                    .fillMaxWidth()
                    .testTag("feed_pull_to_refresh")
            ) {
                LazyColumn(
                    modifier = Modifier
                        .fillMaxSize()
                        .testTag("feed_list"),
                    contentPadding = PaddingValues(start = 16.dp, end = 16.dp, top = 8.dp, bottom = 88.dp),
                    verticalArrangement = Arrangement.spacedBy(14.dp)
                ) {
                    item {
                        androidx.compose.animation.AnimatedVisibility(visible = searchQuery.isBlank()) {
                            CitySkylineArtwork(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .height(144.dp),
                                cityLabel = "$activeCity Loop",
                                subtitle = strings.tagline
                            )
                        }
                    }

                    item {
                        LazyRow(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                            val filters = listOf(
                                "all" to strings.all,
                                "photo" to strings.photos,
                                "reel" to strings.reels
                            )
                            items(filters) { (key, label) ->
                                FilterChip(
                                    selected = feedFilter == key,
                                    onClick = { feedFilter = key },
                                    label = { Text(label) },
                                    leadingIcon = if (key == "reel") {
                                        { Icon(Icons.Rounded.Videocam, contentDescription = null, modifier = Modifier.size(16.dp)) }
                                    } else null,
                                    modifier = Modifier.testTag("feed_filter_$key")
                                )
                            }

                            items(trendingTags, key = { "tag_$it" }) { tag ->
                                val tagQuery = "#$tag"
                                val isTagSelected = searchQuery.trim().equals(tagQuery, ignoreCase = true)
                                FilterChip(
                                    selected = isTagSelected,
                                    onClick = {
                                        onSearchQueryChange(if (isTagSelected) "" else tagQuery)
                                    },
                                    label = { Text(tagQuery) },
                                    leadingIcon = {
                                        Icon(Icons.Rounded.Tag, contentDescription = null, modifier = Modifier.size(15.dp))
                                    },
                                    modifier = Modifier.testTag("feed_trending_tag_$tag")
                                )
                            }
                        }
                    }

                    when (postsState) {
                        is UiState.Loading -> {
                            items(3) { SkeletonPostCard() }
                        }

                        is UiState.Error -> {
                            item {
                                EmptyStateCard(
                                    icon = Icons.Rounded.DynamicFeed,
                                    title = "Could not load $activeCity feed",
                                    subtitle = postsState.message,
                                    ctaLabel = "Retry",
                                    onCtaClick = onRefresh
                                )
                            }
                        }

                        is UiState.Success -> {
                            val filtered = postsState.data
                                .filter { it.authorId !in profile.blockedUserIds }
                                .filter { feedFilter == "all" || it.postType == feedFilter }
                                .filter { matchesPostSearchQuery(it, searchQuery) }

                            if (searchQuery.isNotBlank()) {
                                item {
                                    Row(
                                        modifier = Modifier
                                            .fillMaxWidth()
                                            .testTag("feed_search_status"),
                                        horizontalArrangement = Arrangement.SpaceBetween,
                                        verticalAlignment = Alignment.CenterVertically
                                    ) {
                                        Text(
                                            text = if (currentLanguage == AppLanguage.HINDI) {
                                                "\"${searchQuery.trim()}\" के लिए ${filtered.size} परिणाम"
                                            } else {
                                                val suffix = if (filtered.size == 1) "post" else "posts"
                                                "${filtered.size} $suffix matching \"${searchQuery.trim()}\""
                                            },
                                            style = MaterialTheme.typography.labelLarge,
                                            fontWeight = FontWeight.SemiBold,
                                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                                            modifier = Modifier.weight(1f)
                                        )
                                        TextButton(onClick = { onSearchQueryChange("") }) {
                                            Text(if (currentLanguage == AppLanguage.HINDI) "साफ़ करें" else "Clear")
                                        }
                                    }
                                }
                            }

                            if (filtered.isEmpty()) {
                                item {
                                    if (searchQuery.isNotBlank()) {
                                        EmptyStateCard(
                                            icon = Icons.Rounded.Search,
                                            title = if (currentLanguage == AppLanguage.HINDI)
                                                "\"${searchQuery.trim()}\" से कोई पोस्ट नहीं मिला"
                                            else
                                                "No posts match \"${searchQuery.trim()}\"",
                                            subtitle = if (currentLanguage == AppLanguage.HINDI)
                                                "कोई दूसरा #हैशटैग या कीवर्ड आज़माएँ, या खोज साफ़ करें।"
                                            else
                                                "Try another #hashtag or keyword, or clear your filter.",
                                            ctaLabel = if (currentLanguage == AppLanguage.HINDI) "खोज साफ़ करें" else "Clear Search",
                                            onCtaClick = {
                                                onSearchQueryChange("")
                                                feedFilter = "all"
                                            }
                                        )
                                    } else {
                                        EmptyStateCard(
                                            icon = Icons.Rounded.DynamicFeed,
                                            title = strings.emptyFeedTitle,
                                            subtitle = strings.emptyFeedSubtitle,
                                            ctaLabel = strings.createPostCta,
                                            onCtaClick = { showComposeDialog = true }
                                        )
                                    }
                                }
                            } else {
                                items(filtered, key = { it.id }) { post ->
                                    FeedPostCard(
                                        post = post,
                                        isOwnPost = post.authorId == profile.userId,
                                        isLiked = post.id in likedPostIds,
                                        isSaved = post.id in savedTargetIds,
                                        translatedText = translatedMap[post.id],
                                        isTranslating = translatingId == post.id,
                                        translateLabel = if (currentLanguage == AppLanguage.HINDI) "English (AI)" else strings.translateToHindi,
                                        onLikeClick = { onToggleLike(post) },
                                        onCommentClick = {
                                            activeCommentPost = post
                                            onOpenComments(post)
                                        },
                                        onSaveClick = { onToggleSave(post) },
                                        onTranslateClick = { onTranslatePost(post) },
                                        onDeleteClick = { onDeletePost(post.id) },
                                        onMessageClick = {
                                            onMessageAuthor(post.authorId, post.authorName, "Post in ${post.city}")
                                        },
                                        onBlockClick = { onBlockAuthor(post.authorId) },
                                        onReportClick = { reportPostId = post.id },
                                        onTagClick = { tag ->
                                            onSearchQueryChange("#$tag")
                                        }
                                    )
                                }
                            }
                        }
                    }
                }
            }
        }

        FloatingActionButton(
            onClick = { showComposeDialog = true },
            containerColor = MaterialTheme.colorScheme.primary,
            contentColor = MaterialTheme.colorScheme.onPrimary,
            modifier = Modifier
                .align(Alignment.BottomEnd)
                .padding(16.dp)
                .testTag("feed_compose_fab")
        ) {
            Icon(Icons.Rounded.Add, contentDescription = strings.createPostCta)
        }
    }

    if (showComposeDialog) {
        ComposePostDialog(
            activeCity = activeCity,
            defaultNeighborhood = profile.neighborhood,
            currentLanguage = currentLanguage,
            aiBusy = aiBusy,
            onPolishWithAi = onPolishWithAi,
            onPublish = { postType, caption, area, mediaUri, tagsRaw, onSuccess, onError ->
                onPublishPost(postType, caption, area, mediaUri, tagsRaw, onSuccess, onError)
            },
            onDismiss = { showComposeDialog = false }
        )
    }

    activeCommentPost?.let { post ->
        FeedCommentsBottomSheet(
            commentsState = commentsState,
            onAddComment = { body, parentId -> onAddComment(post, body, parentId) },
            onDismiss = {
                activeCommentPost = null
                onDismissComments()
            }
        )
    }

    reportPostId?.let { targetId ->
        ReportContentDialog(
            targetLabel = "Post",
            onSubmit = { reason -> onReportPost(targetId, reason) },
            onDismiss = { reportPostId = null }
        )
    }
}
