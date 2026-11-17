package com.example.ui.screens

import androidx.compose.foundation.clickable
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
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.rounded.ArrowBack
import androidx.compose.material.icons.rounded.CheckCircle
import androidx.compose.material.icons.rounded.Notifications
import androidx.compose.material.icons.rounded.Verified
import androidx.compose.material.icons.rounded.WorkspacePremium
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FilterChip
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Surface
import androidx.compose.material3.Switch
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.TopAppBar
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.example.core.AppLanguage
import com.example.core.LocalStrings
import com.example.core.ThemeMode
import com.example.core.UiState
import com.example.models.AppNotification
import com.example.models.CityPost
import com.example.models.LocalBusiness
import com.example.models.ModerationReport
import com.example.models.UserPrivateSettings
import com.example.models.UserProfile
import com.example.ui.components.EmptyStateCard
import com.example.ui.components.formatRelativeTime

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun NotificationsScreen(
    notifications: List<AppNotification>,
    onMarkRead: (String) -> Unit,
    onBack: () -> Unit
) {
    val strings = LocalStrings.current
    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text(strings.notificationsTitle) },
                navigationIcon = {
                    IconButton(onClick = onBack) { Icon(Icons.AutoMirrored.Rounded.ArrowBack, contentDescription = "Back") }
                }
            )
        }
    ) { padding ->
        if (notifications.isEmpty()) {
            Box(Modifier.fillMaxSize().padding(padding).padding(16.dp)) {
                EmptyStateCard(Icons.Rounded.Notifications, "All caught up", "Likes, replies, new followers, and local alerts appear here.")
            }
        } else {
            LazyColumn(
                modifier = Modifier.fillMaxSize().padding(padding),
                contentPadding = PaddingValues(16.dp),
                verticalArrangement = Arrangement.spacedBy(10.dp)
            ) {
                items(notifications, key = { it.id }) { notif ->
                    Surface(
                        color = if (!notif.readFlag) MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.35f) else MaterialTheme.colorScheme.surface,
                        shape = MaterialTheme.shapes.medium,
                        modifier = Modifier.fillMaxWidth().clickable { if (!notif.readFlag) onMarkRead(notif.id) }
                    ) {
                        Column(modifier = Modifier.padding(14.dp), verticalArrangement = Arrangement.spacedBy(4.dp)) {
                            Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                                Text(notif.title, style = MaterialTheme.typography.titleSmall)
                                Text(formatRelativeTime(notif.createdAt), style = MaterialTheme.typography.labelSmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
                            }
                            Text(notif.body, style = MaterialTheme.typography.bodyMedium)
                        }
                    }
                }
            }
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun SettingsScreen(
    profile: UserProfile,
    privateSettings: UserPrivateSettings?,
    currentLanguage: AppLanguage,
    currentTheme: ThemeMode,
    onLanguageChange: (AppLanguage) -> Unit,
    onThemeChange: (ThemeMode) -> Unit,
    onPrivacyChange: (Boolean, Boolean) -> Unit,
    onUnblockUser: (String) -> Unit,
    onSignOut: () -> Unit,
    onDeleteAccount: () -> Unit,
    onBack: () -> Unit
) {
    val strings = LocalStrings.current
    var showDeleteConfirm by remember { mutableStateOf(false) }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text(strings.settingsTitle) },
                navigationIcon = { IconButton(onClick = onBack) { Icon(Icons.AutoMirrored.Rounded.ArrowBack, contentDescription = "Back") } }
            )
        }
    ) { padding ->
        LazyColumn(
            modifier = Modifier.fillMaxSize().padding(padding),
            contentPadding = PaddingValues(16.dp),
            verticalArrangement = Arrangement.spacedBy(14.dp)
        ) {
            item {
                Text(strings.languageLabel, style = MaterialTheme.typography.titleMedium)
                Spacer(Modifier.height(6.dp))
                Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    AppLanguage.entries.forEach { lang ->
                        FilterChip(
                            selected = currentLanguage == lang,
                            onClick = { onLanguageChange(lang) },
                            label = { Text(lang.label) },
                            modifier = Modifier.testTag("settings_lang_${lang.code}")
                        )
                    }
                }
            }

            item {
                Text(strings.themeLabel, style = MaterialTheme.typography.titleMedium)
                Spacer(Modifier.height(6.dp))
                Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    ThemeMode.entries.forEach { mode ->
                        FilterChip(
                            selected = currentTheme == mode,
                            onClick = { onThemeChange(mode) },
                            label = { Text(mode.name.lowercase().replaceFirstChar { it.uppercase() }) },
                            modifier = Modifier.testTag("settings_theme_${mode.key}")
                        )
                    }
                }
            }

            item {
                val push = privateSettings?.pushEnabled ?: true
                val priv = privateSettings?.privateProfile ?: false
                Card(colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface)) {
                    Column(modifier = Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(10.dp)) {
                        Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween, verticalAlignment = Alignment.CenterVertically) {
                            Column(Modifier.weight(1f)) {
                                Text("Community Alerts & Notifications", style = MaterialTheme.typography.titleSmall)
                                Text("Receive replies, message pings, and event updates", style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
                            }
                            Switch(checked = push, onCheckedChange = { onPrivacyChange(it, priv) })
                        }
                        Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween, verticalAlignment = Alignment.CenterVertically) {
                            Column(Modifier.weight(1f)) {
                                Text("Private Account Mode", style = MaterialTheme.typography.titleSmall)
                                Text("Hide profile from public neighborhood search", style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
                            }
                            Switch(checked = priv, onCheckedChange = { onPrivacyChange(push, it) })
                        }
                    }
                }
            }

            if (profile.blockedUserIds.isNotEmpty()) {
                item {
                    Text("Blocked Users (${profile.blockedUserIds.size})", style = MaterialTheme.typography.titleMedium)
                }
                items(profile.blockedUserIds) { blockedId ->
                    Surface(shape = MaterialTheme.shapes.medium, color = MaterialTheme.colorScheme.surface) {
                        Row(Modifier.fillMaxWidth().padding(12.dp), horizontalArrangement = Arrangement.SpaceBetween, verticalAlignment = Alignment.CenterVertically) {
                            Text("User ${blockedId.take(8)}...", style = MaterialTheme.typography.bodyMedium)
                            TextButton(onClick = { onUnblockUser(blockedId) }) { Text("Unblock") }
                        }
                    }
                }
            }

            item {
                OutlinedButton(
                    onClick = onSignOut,
                    modifier = Modifier.fillMaxWidth().testTag("sign_out_button")
                ) {
                    Text(strings.signOut)
                }
            }

            item {
                Button(
                    onClick = { showDeleteConfirm = true },
                    colors = ButtonDefaults.buttonColors(containerColor = MaterialTheme.colorScheme.error),
                    modifier = Modifier.fillMaxWidth().testTag("delete_account_button")
                ) {
                    Text("Delete Account & Data")
                }
            }
        }
    }

    if (showDeleteConfirm) {
        AlertDialog(
            onDismissRequest = { showDeleteConfirm = false },
            title = { Text("Delete CITYLOOP Account?") },
            text = { Text("This permanently deletes your CITYLOOP profile, private settings, and authentication account.") },
            confirmButton = {
                Button(
                    onClick = {
                        showDeleteConfirm = false
                        onDeleteAccount()
                    },
                    colors = ButtonDefaults.buttonColors(containerColor = MaterialTheme.colorScheme.error)
                ) { Text("Delete Permanently") }
            },
            dismissButton = {
                TextButton(onClick = { showDeleteConfirm = false }) { Text("Cancel") }
            }
        )
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun AdminModerationScreen(
    reportsState: UiState<List<ModerationReport>>,
    postsState: UiState<List<CityPost>>,
    businessesState: UiState<List<LocalBusiness>>,
    onResolveReport: (String, String) -> Unit,
    onModeratePost: (String, String, String) -> Unit,
    onVerifyBusiness: (String, Boolean) -> Unit,
    onBack: () -> Unit
) {
    val strings = LocalStrings.current
    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text(strings.adminTitle) },
                navigationIcon = { IconButton(onClick = onBack) { Icon(Icons.AutoMirrored.Rounded.ArrowBack, contentDescription = "Back") } }
            )
        }
    ) { padding ->
        LazyColumn(
            modifier = Modifier.fillMaxSize().padding(padding),
            contentPadding = PaddingValues(16.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            item { Text("Moderation Reports", style = MaterialTheme.typography.titleLarge) }
            when (reportsState) {
                is UiState.Loading -> item { Text("Loading reports...") }
                is UiState.Error -> item { Text(reportsState.message, color = MaterialTheme.colorScheme.error) }
                is UiState.Success -> {
                    if (reportsState.data.isEmpty()) {
                        item { Text("No open reports.", color = MaterialTheme.colorScheme.onSurfaceVariant) }
                    } else {
                        items(reportsState.data, key = { it.id }) { rep ->
                            Card(colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface)) {
                                Column(Modifier.padding(14.dp), verticalArrangement = Arrangement.spacedBy(6.dp)) {
                                    Text("${rep.targetType.uppercase()} • ${rep.status.uppercase()} • ${rep.city}", style = MaterialTheme.typography.labelLarge, color = MaterialTheme.colorScheme.primary)
                                    Text(rep.reason, style = MaterialTheme.typography.bodyMedium)
                                    if (rep.status == "open") {
                                        Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                                            Button(onClick = { onResolveReport(rep.id, "resolved") }) { Text("Resolve") }
                                            OutlinedButton(onClick = { onResolveReport(rep.id, "dismissed") }) { Text("Dismiss") }
                                        }
                                    }
                                }
                            }
                        }
                    }
                }
            }

            item { Text("Business Verification Queue", style = MaterialTheme.typography.titleLarge) }
            if (businessesState is UiState.Success) {
                items(businessesState.data, key = { it.id }) { biz ->
                    Card(colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface)) {
                        Row(Modifier.fillMaxWidth().padding(14.dp), horizontalArrangement = Arrangement.SpaceBetween, verticalAlignment = Alignment.CenterVertically) {
                            Column(Modifier.weight(1f)) {
                                Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(4.dp)) {
                                    Text(biz.name, style = MaterialTheme.typography.titleSmall)
                                    if (biz.verified) Icon(Icons.Rounded.Verified, contentDescription = null, tint = MaterialTheme.colorScheme.primary, modifier = Modifier.size(16.dp))
                                }
                                Text("${biz.city} • ${biz.category}", style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
                            }
                            OutlinedButton(onClick = { onVerifyBusiness(biz.id, !biz.verified) }) {
                                Text(if (biz.verified) "Unverify" else "Verify")
                            }
                        }
                    }
                }
            }

            item { Text("City Posts Quick Moderation", style = MaterialTheme.typography.titleLarge) }
            if (postsState is UiState.Success) {
                items(postsState.data.take(10), key = { it.id }) { post ->
                    Card(colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface)) {
                        Row(Modifier.fillMaxWidth().padding(14.dp), horizontalArrangement = Arrangement.SpaceBetween, verticalAlignment = Alignment.CenterVertically) {
                            Column(Modifier.weight(1f)) {
                                Text("${post.authorName}: ${post.caption.take(65)}", style = MaterialTheme.typography.bodyMedium)
                                Text("Status: ${post.status}", style = MaterialTheme.typography.labelSmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
                            }
                            OutlinedButton(onClick = { onModeratePost(post.id, "removed", "Removed by moderator") }) {
                                Text("Hide")
                            }
                        }
                    }
                }
            }
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun SubscriptionScreen(
    currentPlanTier: String,
    onSelectPlan: (String) -> Unit,
    onBack: () -> Unit
) {
    val strings = LocalStrings.current
    val isPlus = currentPlanTier == "plus"
    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text(strings.plusTitle) },
                navigationIcon = { IconButton(onClick = onBack) { Icon(Icons.AutoMirrored.Rounded.ArrowBack, contentDescription = "Back") } }
            )
        }
    ) { padding ->
        Column(
            modifier = Modifier.fillMaxSize().padding(padding).padding(20.dp),
            verticalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            Card(
                colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.secondaryContainer),
                shape = MaterialTheme.shapes.extraLarge
            ) {
                Column(Modifier.padding(20.dp), verticalArrangement = Arrangement.spacedBy(10.dp)) {
                    Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                        Icon(Icons.Rounded.WorkspacePremium, contentDescription = null, tint = MaterialTheme.colorScheme.secondary)
                        Text("CITYLOOP Plus • ₹149 / month", style = MaterialTheme.typography.headlineSmall, fontWeight = FontWeight.Bold)
                    }
                    Text("Monetization Foundation for local creators, verified sellers, and neighborhood business owners.", style = MaterialTheme.typography.bodyMedium)
                    val perks = listOf(
                        "Priority boost on Marketplace listings & Local Offers",
                        "Unlimited Gemini AI caption & Hindi/English translation assists",
                        "Plus member badge across city feeds & communities",
                        "Zero-commission local event hosting tools"
                    )
                    perks.forEach { perk ->
                        Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                            Icon(Icons.Rounded.CheckCircle, contentDescription = null, modifier = Modifier.size(18.dp), tint = MaterialTheme.colorScheme.primary)
                            Text(perk, style = MaterialTheme.typography.bodyMedium)
                        }
                    }
                }
            }

            Button(
                onClick = { onSelectPlan(if (isPlus) "free" else "plus") },
                modifier = Modifier.fillMaxWidth().height(52.dp).testTag("toggle_plus_plan_button")
            ) {
                Text(if (isPlus) "Current Plan: Plus (Switch to Free)" else "Activate CityLoop Plus Foundation")
            }
        }
    }
}
