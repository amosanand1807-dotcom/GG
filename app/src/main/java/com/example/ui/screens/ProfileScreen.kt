package com.example.ui.screens

import android.content.Intent
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.PickVisualMediaRequest
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ExperimentalLayoutApi
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.rounded.Chat
import androidx.compose.material.icons.rounded.AdminPanelSettings
import androidx.compose.material.icons.rounded.Bookmark
import androidx.compose.material.icons.rounded.DynamicFeed
import androidx.compose.material.icons.rounded.Edit
import androidx.compose.material.icons.rounded.Notifications
import androidx.compose.material.icons.rounded.Settings
import androidx.compose.material.icons.rounded.WorkspacePremium
import androidx.compose.material3.Badge
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FilterChip
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.example.core.AppLanguage
import com.example.core.LocalAuthController
import com.example.core.LocalStrings
import com.example.core.UiState
import com.example.models.CityPost
import com.example.models.FollowEdge
import com.example.models.SavedItem
import com.example.models.UserProfile
import com.example.ui.components.EditProfileSheet
import com.example.ui.components.EmptyStateCard
import com.example.ui.components.ProfileAvatar
import com.example.ui.components.ProfileRecentPostCard
import com.example.ui.components.SkeletonPostCard

@OptIn(ExperimentalMaterial3Api::class, ExperimentalLayoutApi::class)
@Composable
fun ProfileScreen(
    profile: UserProfile,
    isAdminUser: Boolean,
    unreadNotificationsCount: Int,
    myPostsState: UiState<List<CityPost>>,
    savedItems: List<SavedItem>,
    following: List<FollowEdge>,
    customAvatarUri: String? = null,
    currentLanguage: AppLanguage = AppLanguage.ENGLISH,
    onUpdateProfile: (String, String, String, String, String) -> Unit,
    onUpdateBio: (String) -> Unit = { newBio ->
        onUpdateProfile(profile.displayName, newBio, profile.city, profile.neighborhood, profile.avatarEmoji)
    },
    onUpdateAvatarUri: (String?) -> Unit = {},
    onDeletePost: (String) -> Unit = {},
    onCreatePost: () -> Unit = {},
    onOpenMessages: () -> Unit,
    onOpenNotifications: () -> Unit,
    onOpenSettings: () -> Unit,
    onOpenPlus: () -> Unit,
    onOpenAdmin: () -> Unit
) {
    val strings = LocalStrings.current
    val context = LocalContext.current
    val authUser = LocalAuthController.current.state.currentUser
    var activeTab by remember { mutableStateOf("posts") }
    var showEditSheet by remember { mutableStateOf(false) }

    var isEditingBio by remember { mutableStateOf(false) }
    var bioDraft by remember(profile.bio) { mutableStateOf(profile.bio) }
    LaunchedEffect(profile.bio) {
        if (!isEditingBio) bioDraft = profile.bio
    }

    val effectivePhotoUrl = customAvatarUri?.takeIf { it.isNotBlank() } ?: authUser?.photoUrl
    val avatarPickerLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.PickVisualMedia()
    ) { uri ->
        if (uri != null) {
            runCatching {
                context.contentResolver.takePersistableUriPermission(
                    uri,
                    Intent.FLAG_GRANT_READ_URI_PERMISSION
                )
            }
            onUpdateAvatarUri(uri.toString())
        }
    }
    val launchAvatarPicker = {
        avatarPickerLauncher.launch(PickVisualMediaRequest(ActivityResultContracts.PickVisualMedia.ImageOnly))
    }

    val resolvedPostsCount = when (myPostsState) {
        is UiState.Success -> myPostsState.data.size.toLong()
        else -> profile.postsCount
    }

    LazyColumn(
        modifier = Modifier
            .fillMaxSize()
            .testTag("profile_screen_list"),
        contentPadding = PaddingValues(start = 16.dp, end = 16.dp, top = 16.dp, bottom = 88.dp),
        verticalArrangement = Arrangement.spacedBy(14.dp)
    ) {
        item {
            Card(
                modifier = Modifier.fillMaxWidth(),
                shape = MaterialTheme.shapes.extraLarge,
                colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
                elevation = CardDefaults.cardElevation(defaultElevation = 1.dp)
            ) {
                Column(modifier = Modifier.padding(20.dp), verticalArrangement = Arrangement.spacedBy(14.dp)) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.Top
                    ) {
                        Row(
                            horizontalArrangement = Arrangement.spacedBy(14.dp),
                            verticalAlignment = Alignment.CenterVertically,
                            modifier = Modifier.weight(1f)
                        ) {
                            ProfileAvatar(
                                photoUrl = effectivePhotoUrl,
                                fallbackEmoji = profile.avatarEmoji,
                                size = 76.dp,
                                onEditPhotoClick = launchAvatarPicker
                            )
                            Column(verticalArrangement = Arrangement.spacedBy(2.dp)) {
                                Row(
                                    verticalAlignment = Alignment.CenterVertically,
                                    horizontalArrangement = Arrangement.spacedBy(6.dp)
                                ) {
                                    Text(
                                        text = profile.displayName.ifBlank { authUser?.displayName ?: "Neighbor" },
                                        style = MaterialTheme.typography.headlineSmall,
                                        fontWeight = FontWeight.Bold
                                    )
                                    if (profile.planTier == "plus") {
                                        Surface(
                                            color = MaterialTheme.colorScheme.secondaryContainer,
                                            shape = RoundedCornerShape(50)
                                        ) {
                                            Text(
                                                "PLUS",
                                                style = MaterialTheme.typography.labelSmall,
                                                color = MaterialTheme.colorScheme.onSecondaryContainer,
                                                modifier = Modifier.padding(horizontal = 8.dp, vertical = 2.dp)
                                            )
                                        }
                                    }
                                }
                                Text(
                                    text = "@${profile.handle} • ${profile.neighborhood.ifBlank { profile.city }}",
                                    style = MaterialTheme.typography.bodyMedium,
                                    color = MaterialTheme.colorScheme.onSurfaceVariant
                                )
                                if (!authUser?.email.isNullOrBlank()) {
                                    Text(
                                        text = authUser?.email.orEmpty(),
                                        style = MaterialTheme.typography.labelSmall,
                                        color = MaterialTheme.colorScheme.primary
                                    )
                                }
                            }
                        }
                        IconButton(
                            onClick = { showEditSheet = true },
                            modifier = Modifier.testTag("edit_profile_button")
                        ) {
                            Icon(Icons.Rounded.Edit, contentDescription = "Edit profile")
                        }
                    }

                    // Short Editable Bio Section
                    Surface(
                        color = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.42f),
                        shape = MaterialTheme.shapes.medium,
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Column(
                            modifier = Modifier.padding(12.dp),
                            verticalArrangement = Arrangement.spacedBy(8.dp)
                        ) {
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.SpaceBetween,
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Text(
                                    text = if (currentLanguage == AppLanguage.HINDI) "परिचय" else "Bio",
                                    style = MaterialTheme.typography.labelMedium,
                                    fontWeight = FontWeight.SemiBold,
                                    color = MaterialTheme.colorScheme.primary
                                )
                                if (!isEditingBio) {
                                    TextButton(
                                        onClick = {
                                            bioDraft = profile.bio
                                            isEditingBio = true
                                        },
                                        contentPadding = PaddingValues(horizontal = 8.dp, vertical = 0.dp),
                                        modifier = Modifier.testTag("profile_inline_edit_bio_button")
                                    ) {
                                        Icon(
                                            imageVector = Icons.Rounded.Edit,
                                            contentDescription = null,
                                            modifier = Modifier.size(14.dp)
                                        )
                                        Text(
                                            text = if (currentLanguage == AppLanguage.HINDI) " संपादित करें" else " Edit Bio",
                                            style = MaterialTheme.typography.labelMedium
                                        )
                                    }
                                }
                            }

                            if (isEditingBio) {
                                OutlinedTextField(
                                    value = bioDraft,
                                    onValueChange = { if (it.length <= 240) bioDraft = it },
                                    placeholder = {
                                        Text(
                                            if (currentLanguage == AppLanguage.HINDI)
                                                "अपने शहर और रुचियों के बारे में लिखें..."
                                            else
                                                "Write a short bio about yourself..."
                                        )
                                    },
                                    supportingText = { Text("${bioDraft.length}/240") },
                                    maxLines = 3,
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .testTag("profile_inline_bio_input")
                                )
                                Row(
                                    modifier = Modifier.fillMaxWidth(),
                                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                                ) {
                                    OutlinedButton(
                                        onClick = {
                                            bioDraft = profile.bio
                                            isEditingBio = false
                                        },
                                        modifier = Modifier
                                            .weight(1f)
                                            .testTag("profile_inline_bio_cancel")
                                    ) {
                                        Text(if (currentLanguage == AppLanguage.HINDI) "रद्द करें" else "Cancel")
                                    }
                                    Button(
                                        onClick = {
                                            onUpdateBio(bioDraft)
                                            isEditingBio = false
                                        },
                                        modifier = Modifier
                                            .weight(1f)
                                            .testTag("profile_inline_bio_save")
                                    ) {
                                        Text(if (currentLanguage == AppLanguage.HINDI) "सहेजें" else "Save Bio")
                                    }
                                }
                            } else {
                                Text(
                                    text = profile.bio.ifBlank {
                                        if (currentLanguage == AppLanguage.HINDI)
                                            "अपने मोहल्ले के लोगों से जुड़ने के लिए एक छोटा परिचय जोड़ें।"
                                        else
                                            "Add a short bio so neighbors in ${profile.city} know what you're into."
                                    },
                                    style = MaterialTheme.typography.bodyMedium,
                                    color = if (profile.bio.isBlank()) {
                                        MaterialTheme.colorScheme.onSurfaceVariant
                                    } else {
                                        MaterialTheme.colorScheme.onSurface
                                    },
                                    modifier = Modifier.testTag("profile_bio_text")
                                )
                            }
                        }
                    }

                    if (profile.interests.isNotEmpty()) {
                        FlowRow(
                            horizontalArrangement = Arrangement.spacedBy(6.dp),
                            verticalArrangement = Arrangement.spacedBy(6.dp)
                        ) {
                            profile.interests.forEach { tag ->
                                Surface(color = MaterialTheme.colorScheme.surfaceVariant, shape = RoundedCornerShape(50)) {
                                    Text(
                                        text = tag,
                                        style = MaterialTheme.typography.labelSmall,
                                        modifier = Modifier.padding(horizontal = 10.dp, vertical = 4.dp)
                                    )
                                }
                            }
                        }
                    }

                    HorizontalDivider(color = MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.45f))

                    Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceEvenly) {
                        StatBlock(
                            label = if (currentLanguage == AppLanguage.HINDI) "पोस्ट" else "Posts",
                            value = resolvedPostsCount.toString()
                        )
                        StatBlock(
                            label = if (currentLanguage == AppLanguage.HINDI) "फ़ॉलोअर्स" else "Followers",
                            value = profile.followersCount.toString()
                        )
                        StatBlock(
                            label = if (currentLanguage == AppLanguage.HINDI) "फ़ॉलोइंग" else "Following",
                            value = following.size.toString()
                        )
                    }
                }
            }
        }

        // Quick Module Hub
        item {
            Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                Row(horizontalArrangement = Arrangement.spacedBy(8.dp), modifier = Modifier.fillMaxWidth()) {
                    HubTile(
                        icon = Icons.AutoMirrored.Rounded.Chat,
                        title = strings.messagesTitle,
                        onClick = onOpenMessages,
                        modifier = Modifier
                            .weight(1f)
                            .testTag("open_messages_tile")
                    )
                    HubTile(
                        icon = Icons.Rounded.Notifications,
                        title = strings.notificationsTitle,
                        badgeCount = unreadNotificationsCount,
                        onClick = onOpenNotifications,
                        modifier = Modifier
                            .weight(1f)
                            .testTag("open_notifications_tile")
                    )
                }
                Row(horizontalArrangement = Arrangement.spacedBy(8.dp), modifier = Modifier.fillMaxWidth()) {
                    HubTile(
                        icon = Icons.Rounded.WorkspacePremium,
                        title = strings.plusTitle,
                        onClick = onOpenPlus,
                        modifier = Modifier
                            .weight(1f)
                            .testTag("open_plus_tile")
                    )
                    HubTile(
                        icon = Icons.Rounded.Settings,
                        title = strings.settingsTitle,
                        onClick = onOpenSettings,
                        modifier = Modifier
                            .weight(1f)
                            .testTag("open_settings_tile")
                    )
                }
                if (isAdminUser) {
                    HubTile(
                        icon = Icons.Rounded.AdminPanelSettings,
                        title = strings.adminTitle,
                        onClick = onOpenAdmin,
                        modifier = Modifier
                            .fillMaxWidth()
                            .testTag("open_admin_tile")
                    )
                }
            }
        }

        item {
            Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                FilterChip(
                    selected = activeTab == "posts",
                    onClick = { activeTab = "posts" },
                    label = {
                        Text(if (currentLanguage == AppLanguage.HINDI) "मेरी पोस्ट" else "Recent Posts")
                    },
                    modifier = Modifier.testTag("profile_tab_posts")
                )
                FilterChip(
                    selected = activeTab == "saved",
                    onClick = { activeTab = "saved" },
                    label = {
                        Text(
                            if (currentLanguage == AppLanguage.HINDI) "सहेजे गए (${savedItems.size})"
                            else "Saved (${savedItems.size})"
                        )
                    },
                    modifier = Modifier.testTag("profile_tab_saved")
                )
                FilterChip(
                    selected = activeTab == "following",
                    onClick = { activeTab = "following" },
                    label = {
                        Text(
                            if (currentLanguage == AppLanguage.HINDI) "फ़ॉलोइंग (${following.size})"
                            else "Following (${following.size})"
                        )
                    },
                    modifier = Modifier.testTag("profile_tab_following")
                )
            }
        }

        when (activeTab) {
            "posts" -> {
                when (myPostsState) {
                    is UiState.Loading -> items(2) { SkeletonPostCard() }
                    is UiState.Error -> item {
                        EmptyStateCard(
                            icon = Icons.Rounded.DynamicFeed,
                            title = "Could not load your posts",
                            subtitle = myPostsState.message
                        )
                    }
                    is UiState.Success -> {
                        if (myPostsState.data.isEmpty()) {
                            item {
                                EmptyStateCard(
                                    icon = Icons.Rounded.DynamicFeed,
                                    title = if (currentLanguage == AppLanguage.HINDI) "अभी कोई पोस्ट नहीं" else "No recent posts yet",
                                    subtitle = if (currentLanguage == AppLanguage.HINDI)
                                        "अपने शहर के अपडेट और शॉर्ट रील्स यहाँ दिखेंगे।"
                                    else
                                        "Your city updates, photos, and short reels will appear here.",
                                    ctaLabel = strings.createPostCta,
                                    onCtaClick = onCreatePost
                                )
                            }
                        } else {
                            items(myPostsState.data, key = { it.id }) { post ->
                                ProfileRecentPostCard(
                                    post = post,
                                    onDeleteClick = { onDeletePost(post.id) }
                                )
                            }
                        }
                    }
                }
            }

            "saved" -> {
                if (savedItems.isEmpty()) {
                    item {
                        EmptyStateCard(
                            icon = Icons.Rounded.Bookmark,
                            title = "No saved items",
                            subtitle = "Bookmark posts, marketplace deals, or events to revisit them anytime."
                        )
                    }
                } else {
                    items(savedItems, key = { it.id }) { item ->
                        Surface(
                            shape = MaterialTheme.shapes.medium,
                            color = MaterialTheme.colorScheme.surface,
                            modifier = Modifier.fillMaxWidth()
                        ) {
                            Column(modifier = Modifier.padding(14.dp), verticalArrangement = Arrangement.spacedBy(4.dp)) {
                                Text(
                                    "${item.itemType.uppercase()} • ${item.city}",
                                    style = MaterialTheme.typography.labelSmall,
                                    color = MaterialTheme.colorScheme.primary
                                )
                                Text(item.title, style = MaterialTheme.typography.titleSmall)
                                if (item.subtitle.isNotBlank()) {
                                    Text(
                                        item.subtitle,
                                        style = MaterialTheme.typography.bodySmall,
                                        color = MaterialTheme.colorScheme.onSurfaceVariant
                                    )
                                }
                            }
                        }
                    }
                }
            }

            "following" -> {
                if (following.isEmpty()) {
                    item {
                        EmptyStateCard(
                            icon = Icons.Rounded.Bookmark,
                            title = "Not following anyone yet",
                            subtitle = "Explore neighbors in your city to follow their updates."
                        )
                    }
                } else {
                    items(following, key = { it.id }) { edge ->
                        Surface(
                            shape = MaterialTheme.shapes.medium,
                            color = MaterialTheme.colorScheme.surface,
                            modifier = Modifier.fillMaxWidth()
                        ) {
                            Row(
                                modifier = Modifier.padding(14.dp),
                                verticalAlignment = Alignment.CenterVertically,
                                horizontalArrangement = Arrangement.SpaceBetween
                            ) {
                                Column {
                                    Text(edge.targetName, style = MaterialTheme.typography.titleSmall)
                                    Text(
                                        "@${edge.targetHandle}",
                                        style = MaterialTheme.typography.bodySmall,
                                        color = MaterialTheme.colorScheme.onSurfaceVariant
                                    )
                                }
                                Text(
                                    "Following",
                                    style = MaterialTheme.typography.labelMedium,
                                    color = MaterialTheme.colorScheme.primary
                                )
                            }
                        }
                    }
                }
            }
        }
    }

    if (showEditSheet) {
        EditProfileSheet(
            profile = profile,
            currentPhotoUrl = effectivePhotoUrl,
            hasCustomPhoto = !customAvatarUri.isNullOrBlank(),
            currentLanguage = currentLanguage,
            onPickPhoto = launchAvatarPicker,
            onResetPhoto = { onUpdateAvatarUri(null) },
            onSave = { name, bio, city, area, emoji ->
                onUpdateProfile(name, bio, city, area, emoji)
                showEditSheet = false
            },
            onDismiss = { showEditSheet = false }
        )
    }
}

@Composable
private fun StatBlock(label: String, value: String) {
    Column(horizontalAlignment = Alignment.CenterHorizontally) {
        Text(value, style = MaterialTheme.typography.titleLarge, fontWeight = FontWeight.Bold)
        Text(label, style = MaterialTheme.typography.labelMedium, color = MaterialTheme.colorScheme.onSurfaceVariant)
    }
}

@Composable
private fun HubTile(
    icon: ImageVector,
    title: String,
    badgeCount: Int = 0,
    onClick: () -> Unit,
    modifier: Modifier = Modifier
) {
    Surface(
        modifier = modifier
            .clip(MaterialTheme.shapes.medium)
            .clickable(onClick = onClick),
        color = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.55f),
        shape = MaterialTheme.shapes.medium
    ) {
        Row(
            modifier = Modifier.padding(horizontal = 14.dp, vertical = 14.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.SpaceBetween
        ) {
            Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                Icon(icon, contentDescription = null, tint = MaterialTheme.colorScheme.primary)
                Text(title, style = MaterialTheme.typography.titleSmall, maxLines = 1)
            }
            if (badgeCount > 0) {
                Badge { Text("$badgeCount") }
            }
        }
    }
}
