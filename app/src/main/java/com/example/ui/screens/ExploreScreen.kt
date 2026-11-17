package com.example.ui.screens

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.rounded.Chat
import androidx.compose.material.icons.automirrored.rounded.Send
import androidx.compose.material.icons.rounded.AutoAwesome
import androidx.compose.material.icons.rounded.Event
import androidx.compose.material.icons.rounded.Groups
import androidx.compose.material.icons.rounded.LocalOffer
import androidx.compose.material.icons.rounded.Search
import androidx.compose.material.icons.rounded.Storefront
import androidx.compose.material.icons.rounded.Verified
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.FilterChip
import androidx.compose.material3.FilledTonalButton
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalClipboardManager
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.AnnotatedString
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.example.core.AppLanguage
import com.example.core.LocalStrings
import com.example.core.UiState
import com.example.models.CityCommunity
import com.example.models.CityEvent
import com.example.models.LocalBusiness
import com.example.models.UserProfile
import com.example.ui.components.EmptyStateCard
import com.example.ui.components.SkeletonPostCard
import com.example.ui.components.formatInr

@Composable
fun ExploreScreen(
    activeCity: String,
    profile: UserProfile,
    neighbors: List<UserProfile>,
    followingIds: Set<String>,
    communitiesState: UiState<List<CityCommunity>>,
    joinedCommunityIds: Set<String>,
    eventsState: UiState<List<CityEvent>>,
    rsvpedEventIds: Set<String>,
    businessesState: UiState<List<LocalBusiness>>,
    conciergeState: UiState<String>,
    currentLanguage: AppLanguage,
    onToggleFollow: (UserProfile) -> Unit,
    onMessageUser: (String, String, String) -> Unit,
    onToggleJoinCommunity: (CityCommunity) -> Unit,
    onToggleEventRsvp: (CityEvent) -> Unit,
    onSaveEvent: (CityEvent) -> Unit,
    onAskConcierge: (String) -> Unit,
    onNavigateToCreate: () -> Unit,
    onShowMessage: (String) -> Unit
) {
    val strings = LocalStrings.current
    val clipboard = LocalClipboardManager.current
    var searchQuery by remember { mutableStateOf("") }
    var activeTab by remember { mutableStateOf("communities") }
    var aiQuestion by remember { mutableStateOf("") }

    LazyColumn(
        modifier = Modifier.fillMaxSize(),
        contentPadding = PaddingValues(16.dp),
        verticalArrangement = Arrangement.spacedBy(14.dp)
    ) {
        item {
            OutlinedTextField(
                value = searchQuery,
                onValueChange = { searchQuery = it },
                placeholder = { Text(strings.searchPlaceholder) },
                leadingIcon = { Icon(Icons.Rounded.Search, contentDescription = null) },
                singleLine = true,
                shape = RoundedCornerShape(16.dp),
                modifier = Modifier.fillMaxWidth().testTag("explore_search_input")
            )
        }

        // Neighbors in City strip
        val visibleNeighbors = neighbors.filter { it.userId != profile.userId && it.userId !in profile.blockedUserIds }
        if (visibleNeighbors.isNotEmpty()) {
            item {
                Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                    Text("Neighbors in $activeCity", style = MaterialTheme.typography.titleMedium)
                    LazyRow(horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                        items(visibleNeighbors, key = { it.userId }) { neighbor ->
                            val isFollowing = neighbor.userId in followingIds
                            Card(
                                modifier = Modifier.width(200.dp),
                                shape = MaterialTheme.shapes.medium,
                                colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface)
                            ) {
                                Column(
                                    modifier = Modifier.padding(12.dp),
                                    verticalArrangement = Arrangement.spacedBy(8.dp)
                                ) {
                                    Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                                        Surface(shape = CircleShape, color = MaterialTheme.colorScheme.primaryContainer, modifier = Modifier.size(36.dp)) {
                                            Box(contentAlignment = Alignment.Center) { Text(neighbor.avatarEmoji) }
                                        }
                                        Column {
                                            Text(neighbor.displayName, style = MaterialTheme.typography.labelLarge, maxLines = 1)
                                            Text("@${neighbor.handle}", style = MaterialTheme.typography.labelSmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
                                        }
                                    }
                                    Row(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                                        FilledTonalButton(
                                            onClick = { onToggleFollow(neighbor) },
                                            modifier = Modifier.weight(1f),
                                            contentPadding = PaddingValues(horizontal = 8.dp, vertical = 4.dp)
                                        ) {
                                            Text(if (isFollowing) "Following" else "Follow", style = MaterialTheme.typography.labelMedium)
                                        }
                                        IconButton(
                                            onClick = { onMessageUser(neighbor.userId, neighbor.displayName, "CityLoop Neighbor") }
                                        ) {
                                            Icon(Icons.AutoMirrored.Rounded.Chat, contentDescription = "Message")
                                        }
                                    }
                                }
                            }
                        }
                    }
                }
            }
        }

        item {
            LazyRow(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                val tabs = listOf(
                    Triple("communities", strings.communities, Icons.Rounded.Groups),
                    Triple("events", strings.events, Icons.Rounded.Event),
                    Triple("businesses", strings.businesses, Icons.Rounded.Storefront),
                    Triple("ai", strings.aiConcierge, Icons.Rounded.AutoAwesome)
                )
                items(tabs) { (key, label, icon) ->
                    FilterChip(
                        selected = activeTab == key,
                        onClick = { activeTab = key },
                        label = { Text(label) },
                        leadingIcon = { Icon(icon, contentDescription = null, modifier = Modifier.size(16.dp)) },
                        modifier = Modifier.testTag("explore_tab_$key")
                    )
                }
            }
        }

        when (activeTab) {
            "communities" -> {
                when (communitiesState) {
                    is UiState.Loading -> items(2) { SkeletonPostCard() }
                    is UiState.Error -> item {
                        EmptyStateCard(Icons.Rounded.Groups, "Communities unavailable", communitiesState.message)
                    }
                    is UiState.Success -> {
                        val list = communitiesState.data.filter {
                            searchQuery.isBlank() ||
                                it.name.contains(searchQuery, ignoreCase = true) ||
                                it.category.contains(searchQuery, ignoreCase = true)
                        }
                        if (list.isEmpty()) {
                            item {
                                EmptyStateCard(
                                    icon = Icons.Rounded.Groups,
                                    title = "No communities yet in $activeCity",
                                    subtitle = "Start a neighborhood club for chai meetups, morning runs, or tech builders.",
                                    ctaLabel = "Create Community",
                                    onCtaClick = onNavigateToCreate
                                )
                            }
                        } else {
                            items(list, key = { it.id }) { comm ->
                                val joined = comm.id in joinedCommunityIds
                                Card(
                                    modifier = Modifier.fillMaxWidth().testTag("community_card_${comm.id}"),
                                    shape = MaterialTheme.shapes.large,
                                    colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface)
                                ) {
                                    Column(modifier = Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
                                        Row(
                                            modifier = Modifier.fillMaxWidth(),
                                            horizontalArrangement = Arrangement.SpaceBetween,
                                            verticalAlignment = Alignment.CenterVertically
                                        ) {
                                            Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                                                Surface(
                                                    shape = RoundedCornerShape(14.dp),
                                                    color = MaterialTheme.colorScheme.primaryContainer,
                                                    modifier = Modifier.size(46.dp)
                                                ) {
                                                    Box(contentAlignment = Alignment.Center) { Text(comm.emoji.ifBlank { "🏘️" }, style = MaterialTheme.typography.titleLarge) }
                                                }
                                                Column {
                                                    Text(comm.name, style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold)
                                                    Text("${comm.category} • ${comm.memberCount} members", style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
                                                }
                                            }
                                            if (joined) {
                                                OutlinedButton(onClick = { onToggleJoinCommunity(comm) }) { Text("Joined") }
                                            } else {
                                                Button(onClick = { onToggleJoinCommunity(comm) }) { Text("Join") }
                                            }
                                        }
                                        Text(comm.description, style = MaterialTheme.typography.bodyMedium)
                                    }
                                }
                            }
                        }
                    }
                }
            }

            "events" -> {
                when (eventsState) {
                    is UiState.Loading -> items(2) { SkeletonPostCard() }
                    is UiState.Error -> item {
                        EmptyStateCard(Icons.Rounded.Event, "Events unavailable", eventsState.message)
                    }
                    is UiState.Success -> {
                        val list = eventsState.data.filter {
                            searchQuery.isBlank() ||
                                it.title.contains(searchQuery, ignoreCase = true) ||
                                it.venue.contains(searchQuery, ignoreCase = true)
                        }
                        if (list.isEmpty()) {
                            item {
                                EmptyStateCard(
                                    icon = Icons.Rounded.Event,
                                    title = "No upcoming events in $activeCity",
                                    subtitle = "Host a weekend book swap, rooftop jam, or local workshop.",
                                    ctaLabel = "Host Event",
                                    onCtaClick = onNavigateToCreate
                                )
                            }
                        } else {
                            items(list, key = { it.id }) { evt ->
                                val going = evt.id in rsvpedEventIds
                                Card(
                                    modifier = Modifier.fillMaxWidth().testTag("event_card_${evt.id}"),
                                    shape = MaterialTheme.shapes.large,
                                    colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface)
                                ) {
                                    Column(modifier = Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
                                        Row(
                                            modifier = Modifier.fillMaxWidth(),
                                            horizontalArrangement = Arrangement.SpaceBetween,
                                            verticalAlignment = Alignment.CenterVertically
                                        ) {
                                            Surface(
                                                color = MaterialTheme.colorScheme.secondaryContainer,
                                                shape = RoundedCornerShape(8.dp)
                                            ) {
                                                Text(
                                                    text = "${evt.dateLabel} • ${formatInr(evt.priceInr)}",
                                                    style = MaterialTheme.typography.labelMedium,
                                                    color = MaterialTheme.colorScheme.onSecondaryContainer,
                                                    modifier = Modifier.padding(horizontal = 10.dp, vertical = 4.dp)
                                                )
                                            }
                                            Text("${evt.rsvpCount} going", style = MaterialTheme.typography.labelLarge, color = MaterialTheme.colorScheme.primary)
                                        }
                                        Text(evt.title, style = MaterialTheme.typography.titleLarge)
                                        Text(evt.description, style = MaterialTheme.typography.bodyMedium)
                                        Text("📍 ${evt.venue}, ${evt.city} • Host: ${evt.organizerName}", style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
                                        Row(horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                                            Button(
                                                onClick = { onToggleEventRsvp(evt) },
                                                modifier = Modifier.weight(1f)
                                            ) {
                                                Text(if (going) "Going ✓" else "RSVP Now")
                                            }
                                            OutlinedButton(onClick = { onSaveEvent(evt) }) {
                                                Text("Save")
                                            }
                                        }
                                    }
                                }
                            }
                        }
                    }
                }
            }

            "businesses" -> {
                when (businessesState) {
                    is UiState.Loading -> items(2) { SkeletonPostCard() }
                    is UiState.Error -> item {
                        EmptyStateCard(Icons.Rounded.Storefront, "Businesses unavailable", businessesState.message)
                    }
                    is UiState.Success -> {
                        val list = businessesState.data.filter {
                            searchQuery.isBlank() ||
                                it.name.contains(searchQuery, ignoreCase = true) ||
                                it.category.contains(searchQuery, ignoreCase = true)
                        }
                        if (list.isEmpty()) {
                            item {
                                EmptyStateCard(
                                    icon = Icons.Rounded.Storefront,
                                    title = "No local businesses listed in $activeCity",
                                    subtitle = "Showcase your cafe, studio, or neighborhood shop with an exclusive promo code.",
                                    ctaLabel = "Register Business",
                                    onCtaClick = onNavigateToCreate
                                )
                            }
                        } else {
                            items(list, key = { it.id }) { biz ->
                                Card(
                                    modifier = Modifier.fillMaxWidth().testTag("business_card_${biz.id}"),
                                    shape = MaterialTheme.shapes.large,
                                    colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface)
                                ) {
                                    Column(modifier = Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
                                        Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                                            Text(biz.name, style = MaterialTheme.typography.titleLarge)
                                            if (biz.verified) {
                                                Icon(
                                                    Icons.Rounded.Verified,
                                                    contentDescription = "Verified business",
                                                    tint = MaterialTheme.colorScheme.primary,
                                                    modifier = Modifier.size(20.dp)
                                                )
                                            }
                                        }
                                        Text("${biz.category} • 📍 ${biz.address}", style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
                                        Text(biz.description, style = MaterialTheme.typography.bodyMedium)
                                        if (biz.offerTitle.isNotBlank()) {
                                            Surface(
                                                color = MaterialTheme.colorScheme.secondaryContainer.copy(alpha = 0.7f),
                                                shape = MaterialTheme.shapes.medium,
                                                modifier = Modifier.fillMaxWidth()
                                            ) {
                                                Row(
                                                    modifier = Modifier.padding(12.dp),
                                                    verticalAlignment = Alignment.CenterVertically,
                                                    horizontalArrangement = Arrangement.SpaceBetween
                                                ) {
                                                    Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(8.dp), modifier = Modifier.weight(1f)) {
                                                        Icon(Icons.Rounded.LocalOffer, contentDescription = null, tint = MaterialTheme.colorScheme.secondary)
                                                        Column {
                                                            Text(biz.offerTitle, style = MaterialTheme.typography.titleSmall)
                                                            if (biz.discountLabel.isNotBlank()) {
                                                                Text(biz.discountLabel, style = MaterialTheme.typography.labelMedium, color = MaterialTheme.colorScheme.tertiary)
                                                            }
                                                        }
                                                    }
                                                    if (biz.offerCode.isNotBlank()) {
                                                        FilledTonalButton(
                                                            onClick = {
                                                                clipboard.setText(AnnotatedString(biz.offerCode))
                                                                onShowMessage("Copied offer code ${biz.offerCode}")
                                                            }
                                                        ) {
                                                            Text(biz.offerCode)
                                                        }
                                                    }
                                                }
                                            }
                                        }
                                    }
                                }
                            }
                        }
                    }
                }
            }

            "ai" -> {
                item {
                    Card(
                        modifier = Modifier.fillMaxWidth(),
                        shape = MaterialTheme.shapes.large,
                        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface)
                    ) {
                        Column(modifier = Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(12.dp)) {
                            Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                                Icon(Icons.Rounded.AutoAwesome, contentDescription = null, tint = MaterialTheme.colorScheme.secondary)
                                Text("CITYLOOP AI Concierge ($activeCity)", style = MaterialTheme.typography.titleLarge)
                            }
                            Text(
                                if (currentLanguage == AppLanguage.HINDI)
                                    "अपने शहर के लिए टिप्स, इवेंट आइडिया या मोहल्ले की जानकारी पूछें।"
                                else
                                    "Ask Gemini for local weekend ideas, community event templates, or neighborhood safety tips in $activeCity.",
                                style = MaterialTheme.typography.bodyMedium,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                            Row(
                                verticalAlignment = Alignment.CenterVertically,
                                horizontalArrangement = Arrangement.spacedBy(8.dp)
                            ) {
                                OutlinedTextField(
                                    value = aiQuestion,
                                    onValueChange = { aiQuestion = it },
                                    placeholder = { Text("e.g., Weekend meetup ideas in $activeCity") },
                                    modifier = Modifier.weight(1f).testTag("ai_concierge_input"),
                                    maxLines = 3
                                )
                                IconButton(
                                    onClick = {
                                        if (aiQuestion.isNotBlank()) {
                                            onAskConcierge(aiQuestion)
                                        }
                                    },
                                    enabled = aiQuestion.isNotBlank(),
                                    modifier = Modifier.testTag("ai_concierge_send")
                                ) {
                                    Icon(Icons.AutoMirrored.Rounded.Send, contentDescription = "Ask AI")
                                }
                            }

                            when (conciergeState) {
                                is UiState.Loading -> {
                                    Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                                        CircularProgressIndicator(modifier = Modifier.size(20.dp), strokeWidth = 2.dp)
                                        Text("Thinking for $activeCity...", style = MaterialTheme.typography.bodyMedium)
                                    }
                                }
                                is UiState.Error -> {
                                    Surface(color = MaterialTheme.colorScheme.errorContainer, shape = MaterialTheme.shapes.medium) {
                                        Text(
                                            conciergeState.message,
                                            color = MaterialTheme.colorScheme.onErrorContainer,
                                            modifier = Modifier.padding(12.dp)
                                        )
                                    }
                                }
                                is UiState.Success -> {
                                    if (conciergeState.data.isNotBlank()) {
                                        Surface(
                                            color = MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.4f),
                                            shape = MaterialTheme.shapes.medium,
                                            modifier = Modifier.fillMaxWidth()
                                        ) {
                                            Text(
                                                text = conciergeState.data,
                                                style = MaterialTheme.typography.bodyMedium,
                                                modifier = Modifier.padding(14.dp)
                                            )
                                        }
                                    }
                                }
                            }
                        }
                    }
                }
            }
        }
    }
}
