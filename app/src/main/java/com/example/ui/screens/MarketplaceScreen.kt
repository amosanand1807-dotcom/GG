package com.example.ui.screens

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.rounded.Chat
import androidx.compose.material.icons.outlined.BookmarkBorder
import androidx.compose.material.icons.rounded.Bookmark
import androidx.compose.material.icons.rounded.CheckCircle
import androidx.compose.material.icons.rounded.Flag
import androidx.compose.material.icons.rounded.Search
import androidx.compose.material.icons.rounded.Storefront
import androidx.compose.material.icons.rounded.Translate
import androidx.compose.material3.AssistChip
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.FilterChip
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
import androidx.compose.ui.draw.clip
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import coil.compose.AsyncImage
import com.example.core.AppLanguage
import com.example.core.CityData
import com.example.core.LocalStrings
import com.example.core.UiState
import com.example.models.MarketplaceListing
import com.example.models.UserProfile
import com.example.ui.components.EmptyStateCard
import com.example.ui.components.ReportContentDialog
import com.example.ui.components.SkeletonPostCard
import com.example.ui.components.formatInr
import com.example.ui.components.formatRelativeTime

@Composable
fun MarketplaceScreen(
    activeCity: String,
    profile: UserProfile,
    listingsState: UiState<List<MarketplaceListing>>,
    savedTargetIds: Set<String>,
    translatedMap: Map<String, String>,
    translatingId: String?,
    currentLanguage: AppLanguage,
    onNavigateToCreate: () -> Unit,
    onMessageSeller: (String, String, String) -> Unit,
    onMarkSold: (String) -> Unit,
    onToggleSave: (MarketplaceListing) -> Unit,
    onTranslateListing: (MarketplaceListing) -> Unit,
    onReportListing: (String, String) -> Unit
) {
    val strings = LocalStrings.current
    var searchQuery by remember { mutableStateOf("") }
    var selectedCategory by remember { mutableStateOf("All") }
    var reportListingId by remember { mutableStateOf<String?>(null) }

    LazyColumn(
        modifier = Modifier.fillMaxSize(),
        contentPadding = PaddingValues(16.dp),
        verticalArrangement = Arrangement.spacedBy(12.dp)
    ) {
        item {
            OutlinedTextField(
                value = searchQuery,
                onValueChange = { searchQuery = it },
                placeholder = { Text(strings.marketSearchPlaceholder) },
                leadingIcon = { Icon(Icons.Rounded.Search, contentDescription = null) },
                singleLine = true,
                shape = RoundedCornerShape(16.dp),
                modifier = Modifier.fillMaxWidth().testTag("marketplace_search_input")
            )
        }

        item {
            LazyRow(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                items(CityData.marketplaceCategories) { category ->
                    FilterChip(
                        selected = selectedCategory == category,
                        onClick = { selectedCategory = category },
                        label = { Text(category) },
                        modifier = Modifier.testTag("market_cat_$category")
                    )
                }
            }
        }

        when (listingsState) {
            is UiState.Loading -> items(3) { SkeletonPostCard() }
            is UiState.Error -> item {
                EmptyStateCard(Icons.Rounded.Storefront, "Marketplace unavailable", listingsState.message)
            }
            is UiState.Success -> {
                val filtered = listingsState.data
                    .filter { it.sellerId !in profile.blockedUserIds }
                    .filter { selectedCategory == "All" || it.category == selectedCategory }
                    .filter {
                        searchQuery.isBlank() ||
                            it.title.contains(searchQuery, ignoreCase = true) ||
                            it.description.contains(searchQuery, ignoreCase = true)
                    }

                if (filtered.isEmpty()) {
                    item {
                        EmptyStateCard(
                            icon = Icons.Rounded.Storefront,
                            title = "No active listings in $activeCity",
                            subtitle = "Declutter and sell pre-loved bikes, electronics, books, or furniture to verified neighbors.",
                            ctaLabel = "Post Item for Sale",
                            onCtaClick = onNavigateToCreate
                        )
                    }
                } else {
                    items(filtered, key = { it.id }) { item ->
                        val isOwner = item.sellerId == profile.userId
                        val isSaved = item.id in savedTargetIds
                        Card(
                            modifier = Modifier.fillMaxWidth().testTag("listing_card_${item.id}"),
                            shape = MaterialTheme.shapes.large,
                            colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface)
                        ) {
                            Column(modifier = Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(10.dp)) {
                                if (!item.mediaUri.isNullOrBlank()) {
                                    AsyncImage(
                                        model = item.mediaUri,
                                        contentDescription = item.title,
                                        contentScale = ContentScale.Crop,
                                        modifier = Modifier
                                            .fillMaxWidth()
                                            .height(175.dp)
                                            .clip(MaterialTheme.shapes.medium)
                                    )
                                }

                                Row(
                                    modifier = Modifier.fillMaxWidth(),
                                    horizontalArrangement = Arrangement.SpaceBetween,
                                    verticalAlignment = Alignment.CenterVertically
                                ) {
                                    Text(
                                        text = formatInr(item.priceInr),
                                        style = MaterialTheme.typography.headlineSmall,
                                        color = MaterialTheme.colorScheme.primary,
                                        fontWeight = FontWeight.Bold
                                    )
                                    Row(horizontalArrangement = Arrangement.spacedBy(6.dp), verticalAlignment = Alignment.CenterVertically) {
                                        Surface(
                                            color = MaterialTheme.colorScheme.secondaryContainer,
                                            shape = RoundedCornerShape(8.dp)
                                        ) {
                                            Text(
                                                text = item.condition,
                                                style = MaterialTheme.typography.labelMedium,
                                                color = MaterialTheme.colorScheme.onSecondaryContainer,
                                                modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp)
                                            )
                                        }
                                        IconButton(onClick = { onToggleSave(item) }) {
                                            Icon(
                                                imageVector = if (isSaved) Icons.Rounded.Bookmark else Icons.Outlined.BookmarkBorder,
                                                contentDescription = "Save listing",
                                                tint = if (isSaved) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.onSurfaceVariant
                                            )
                                        }
                                    }
                                }

                                Text(item.title, style = MaterialTheme.typography.titleLarge)
                                Text(item.description, style = MaterialTheme.typography.bodyMedium)

                                AnimatedVisibility(visible = !translatedMap[item.id].isNullOrBlank()) {
                                    Surface(
                                        color = MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.38f),
                                        shape = MaterialTheme.shapes.small
                                    ) {
                                        Text(
                                            text = translatedMap[item.id].orEmpty(),
                                            style = MaterialTheme.typography.bodyMedium,
                                            modifier = Modifier.padding(10.dp)
                                        )
                                    }
                                }

                                Text(
                                    text = "${item.category} • ${item.neighborhood.ifBlank { item.city }} • Seller: ${item.sellerName} • ${formatRelativeTime(item.createdAt)}",
                                    style = MaterialTheme.typography.bodySmall,
                                    color = MaterialTheme.colorScheme.onSurfaceVariant
                                )

                                Row(
                                    modifier = Modifier.fillMaxWidth(),
                                    horizontalArrangement = Arrangement.spacedBy(8.dp),
                                    verticalAlignment = Alignment.CenterVertically
                                ) {
                                    if (isOwner) {
                                        OutlinedButton(
                                            onClick = { onMarkSold(item.id) },
                                            modifier = Modifier.weight(1f)
                                        ) {
                                            Icon(Icons.Rounded.CheckCircle, contentDescription = null, modifier = Modifier.size(16.dp))
                                            Spacer(Modifier.size(6.dp))
                                            Text("Mark as Sold")
                                        }
                                    } else {
                                        Button(
                                            onClick = { onMessageSeller(item.sellerId, item.sellerName, "Listing: ${item.title}") },
                                            modifier = Modifier.weight(1f).testTag("message_seller_${item.id}")
                                        ) {
                                            Icon(Icons.AutoMirrored.Rounded.Chat, contentDescription = null, modifier = Modifier.size(16.dp))
                                            Spacer(Modifier.size(6.dp))
                                            Text("Message Seller")
                                        }
                                        IconButton(onClick = { reportListingId = item.id }) {
                                            Icon(Icons.Rounded.Flag, contentDescription = "Report listing")
                                        }
                                    }
                                    AssistChip(
                                        onClick = { onTranslateListing(item) },
                                        label = {
                                            if (translatingId == item.id) {
                                                CircularProgressIndicator(modifier = Modifier.size(14.dp), strokeWidth = 2.dp)
                                            } else {
                                                Text(if (currentLanguage == AppLanguage.HINDI) "EN" else "HI")
                                            }
                                        },
                                        leadingIcon = { Icon(Icons.Rounded.Translate, contentDescription = null, modifier = Modifier.size(16.dp)) }
                                    )
                                }
                            }
                        }
                    }
                }
            }
        }
    }

    reportListingId?.let { id ->
        ReportContentDialog(
            targetLabel = "Listing",
            onSubmit = { reason -> onReportListing(id, reason) },
            onDismiss = { reportListingId = null }
        )
    }
}
