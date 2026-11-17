package com.example.ui.screens

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ExperimentalLayoutApi
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.imePadding
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardActions
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.rounded.ArrowBack
import androidx.compose.material.icons.automirrored.rounded.Chat
import androidx.compose.material.icons.automirrored.rounded.Send
import androidx.compose.material.icons.rounded.AddComment
import androidx.compose.material.icons.rounded.Close
import androidx.compose.material.icons.rounded.Search
import androidx.compose.material3.AssistChip
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FilledIconButton
import androidx.compose.material3.FloatingActionButton
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.ModalBottomSheet
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import com.example.core.AppLanguage
import com.example.core.LocalStrings
import com.example.core.UiState
import com.example.models.ChatMessage
import com.example.models.Conversation
import com.example.models.UserProfile
import com.example.ui.components.EmptyStateCard
import com.example.ui.components.formatRelativeTime

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun MessagesInboxScreen(
    currentUserId: String,
    activeCity: String,
    blockedUserIds: List<String>,
    neighbors: List<UserProfile>,
    conversationsState: UiState<List<Conversation>>,
    currentLanguage: AppLanguage,
    onBack: () -> Unit,
    onSelectConversation: (Conversation) -> Unit,
    onStartNewChat: (UserProfile) -> Unit
) {
    val strings = LocalStrings.current
    var searchQuery by remember { mutableStateOf("") }
    var showNewChatSheet by remember { mutableStateOf(false) }

    val availableNeighbors = remember(neighbors, currentUserId, blockedUserIds) {
        neighbors.filter { it.userId != currentUserId && it.userId !in blockedUserIds }
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = {
                    Column {
                        Text(strings.messagesTitle, fontWeight = FontWeight.Bold)
                        Text(
                            text = if (currentLanguage == AppLanguage.HINDI)
                                "$activeCity • रीयल-टाइम चैट"
                            else
                                "$activeCity • Real-time 1-on-1 chat",
                            style = MaterialTheme.typography.labelSmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                },
                navigationIcon = {
                    IconButton(
                        onClick = onBack,
                        modifier = Modifier.testTag("messages_back_button")
                    ) {
                        Icon(Icons.AutoMirrored.Rounded.ArrowBack, contentDescription = "Back")
                    }
                }
            )
        },
        floatingActionButton = {
            FloatingActionButton(
                onClick = { showNewChatSheet = true },
                containerColor = MaterialTheme.colorScheme.primary,
                contentColor = MaterialTheme.colorScheme.onPrimary,
                modifier = Modifier.testTag("messages_new_chat_fab")
            ) {
                Icon(
                    imageVector = Icons.Rounded.AddComment,
                    contentDescription = if (currentLanguage == AppLanguage.HINDI) "नई चैट शुरू करें" else "Start new chat"
                )
            }
        }
    ) { padding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(padding)
        ) {
            OutlinedTextField(
                value = searchQuery,
                onValueChange = { searchQuery = it },
                placeholder = {
                    Text(
                        if (currentLanguage == AppLanguage.HINDI)
                            "चैट या पड़ोसी खोजें..."
                        else
                            "Search conversations..."
                    )
                },
                leadingIcon = { Icon(Icons.Rounded.Search, contentDescription = null) },
                trailingIcon = {
                    if (searchQuery.isNotEmpty()) {
                        IconButton(onClick = { searchQuery = "" }) {
                            Icon(Icons.Rounded.Close, contentDescription = "Clear search")
                        }
                    }
                },
                singleLine = true,
                shape = RoundedCornerShape(24.dp),
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 16.dp, vertical = 8.dp)
                    .testTag("messages_search_input")
            )

            when (conversationsState) {
                is UiState.Loading -> Box(
                    modifier = Modifier.fillMaxSize(),
                    contentAlignment = Alignment.Center
                ) {
                    CircularProgressIndicator()
                }

                is UiState.Error -> Box(
                    modifier = Modifier
                        .fillMaxSize()
                        .padding(16.dp)
                ) {
                    EmptyStateCard(
                        icon = Icons.AutoMirrored.Rounded.Chat,
                        title = "Inbox error",
                        subtitle = conversationsState.message
                    )
                }

                is UiState.Success -> {
                    val filtered = conversationsState.data.filter { conv ->
                        val otherIndex = conv.participantIds.indexOfFirst { it != currentUserId }.takeIf { it >= 0 } ?: 0
                        val otherName = conv.participantNames.getOrNull(otherIndex).orEmpty()
                        searchQuery.isBlank() ||
                            otherName.contains(searchQuery, ignoreCase = true) ||
                            conv.contextTitle.contains(searchQuery, ignoreCase = true) ||
                            conv.lastMessage.contains(searchQuery, ignoreCase = true)
                    }

                    if (filtered.isEmpty()) {
                        Box(
                            modifier = Modifier
                                .fillMaxSize()
                                .padding(16.dp)
                        ) {
                            EmptyStateCard(
                                icon = Icons.AutoMirrored.Rounded.Chat,
                                title = if (searchQuery.isNotBlank()) {
                                    "No matching chats"
                                } else if (currentLanguage == AppLanguage.HINDI) {
                                    "अभी कोई बातचीत नहीं"
                                } else {
                                    "No conversations yet"
                                },
                                subtitle = if (searchQuery.isNotBlank()) {
                                    "Try another neighbor name or topic."
                                } else {
                                    "Start a real-time chat with a neighbor in $activeCity, or message someone from Feed, Events, or Marketplace."
                                },
                                ctaLabel = if (searchQuery.isBlank() && availableNeighbors.isNotEmpty()) {
                                    if (currentLanguage == AppLanguage.HINDI) "नई चैट शुरू करें" else "Start a Chat"
                                } else null,
                                onCtaClick = if (searchQuery.isBlank() && availableNeighbors.isNotEmpty()) {
                                    { showNewChatSheet = true }
                                } else null
                            )
                        }
                    } else {
                        LazyColumn(
                            modifier = Modifier
                                .fillMaxSize()
                                .testTag("messages_conversations_list"),
                            contentPadding = PaddingValues(start = 16.dp, end = 16.dp, top = 6.dp, bottom = 88.dp),
                            verticalArrangement = Arrangement.spacedBy(10.dp)
                        ) {
                            items(filtered, key = { it.id }) { conv ->
                                ConversationCard(
                                    conv = conv,
                                    currentUserId = currentUserId,
                                    onClick = { onSelectConversation(conv) }
                                )
                            }
                        }
                    }
                }
            }
        }
    }

    if (showNewChatSheet) {
        NewChatNeighborSheet(
            activeCity = activeCity,
            neighbors = availableNeighbors,
            currentLanguage = currentLanguage,
            onSelectNeighbor = { neighbor ->
                showNewChatSheet = false
                onStartNewChat(neighbor)
            },
            onDismiss = { showNewChatSheet = false }
        )
    }
}

@Composable
private fun ConversationCard(
    conv: Conversation,
    currentUserId: String,
    onClick: () -> Unit
) {
    val otherIndex = conv.participantIds.indexOfFirst { it != currentUserId }.takeIf { it >= 0 } ?: 0
    val otherName = conv.participantNames.getOrNull(otherIndex)?.ifBlank { "Neighbor" } ?: "Neighbor"
    val isLastMine = conv.lastSenderId == currentUserId

    Card(
        shape = MaterialTheme.shapes.large,
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
        elevation = CardDefaults.cardElevation(defaultElevation = 1.dp),
        modifier = Modifier
            .fillMaxWidth()
            .clip(MaterialTheme.shapes.large)
            .clickable(onClick = onClick)
            .testTag("conversation_${conv.id}")
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(14.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            Surface(
                shape = CircleShape,
                color = MaterialTheme.colorScheme.primaryContainer,
                modifier = Modifier.size(46.dp)
            ) {
                Box(contentAlignment = Alignment.Center) {
                    Text(
                        text = otherName.firstOrNull()?.uppercase() ?: "N",
                        style = MaterialTheme.typography.titleMedium,
                        fontWeight = FontWeight.Bold,
                        color = MaterialTheme.colorScheme.onPrimaryContainer
                    )
                }
            }

            Column(modifier = Modifier.weight(1f), verticalArrangement = Arrangement.spacedBy(3.dp)) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text(
                        text = otherName,
                        style = MaterialTheme.typography.titleMedium,
                        fontWeight = FontWeight.Bold,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis,
                        modifier = Modifier.weight(1f)
                    )
                    Text(
                        text = formatRelativeTime(conv.updatedAt ?: conv.createdAt),
                        style = MaterialTheme.typography.labelSmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }

                if (conv.contextTitle.isNotBlank()) {
                    Text(
                        text = conv.contextTitle,
                        style = MaterialTheme.typography.labelMedium,
                        color = MaterialTheme.colorScheme.primary,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis
                    )
                }

                Text(
                    text = if (isLastMine) "You: ${conv.lastMessage}" else conv.lastMessage,
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis
                )
            }
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun NewChatNeighborSheet(
    activeCity: String,
    neighbors: List<UserProfile>,
    currentLanguage: AppLanguage,
    onSelectNeighbor: (UserProfile) -> Unit,
    onDismiss: () -> Unit
) {
    var query by remember { mutableStateOf("") }
    val filtered = remember(neighbors, query) {
        neighbors.filter {
            query.isBlank() ||
                it.displayName.contains(query, ignoreCase = true) ||
                it.handle.contains(query, ignoreCase = true) ||
                it.neighborhood.contains(query, ignoreCase = true)
        }
    }

    ModalBottomSheet(
        onDismissRequest = onDismiss,
        modifier = Modifier.testTag("new_chat_sheet")
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 16.dp)
                .padding(bottom = 24.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            Text(
                text = if (currentLanguage == AppLanguage.HINDI)
                    "$activeCity में नई बातचीत"
                else
                    "Message a Neighbor in $activeCity",
                style = MaterialTheme.typography.titleLarge,
                fontWeight = FontWeight.Bold
            )

            OutlinedTextField(
                value = query,
                onValueChange = { query = it },
                placeholder = {
                    Text(
                        if (currentLanguage == AppLanguage.HINDI)
                            "नाम या मोहल्ला खोजें..."
                        else
                            "Search neighbor name or area..."
                    )
                },
                leadingIcon = { Icon(Icons.Rounded.Search, contentDescription = null) },
                singleLine = true,
                shape = RoundedCornerShape(16.dp),
                modifier = Modifier
                    .fillMaxWidth()
                    .testTag("new_chat_neighbor_search")
            )

            if (filtered.isEmpty()) {
                Text(
                    text = if (currentLanguage == AppLanguage.HINDI)
                        "इस शहर में अभी कोई अन्य सदस्य नहीं मिला।"
                    else
                        "No other active neighbors found in $activeCity yet.",
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    modifier = Modifier.padding(vertical = 16.dp)
                )
            } else {
                LazyColumn(
                    modifier = Modifier.heightIn(max = 320.dp),
                    verticalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    items(filtered, key = { it.userId }) { neighbor ->
                        Surface(
                            color = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.45f),
                            shape = MaterialTheme.shapes.medium,
                            modifier = Modifier
                                .fillMaxWidth()
                                .clip(MaterialTheme.shapes.medium)
                                .clickable { onSelectNeighbor(neighbor) }
                                .testTag("new_chat_neighbor_${neighbor.userId}")
                        ) {
                            Row(
                                modifier = Modifier.padding(12.dp),
                                verticalAlignment = Alignment.CenterVertically,
                                horizontalArrangement = Arrangement.spacedBy(12.dp)
                            ) {
                                Surface(
                                    shape = CircleShape,
                                    color = MaterialTheme.colorScheme.primaryContainer,
                                    modifier = Modifier.size(40.dp)
                                ) {
                                    Box(contentAlignment = Alignment.Center) {
                                        Text(
                                            text = neighbor.avatarEmoji.ifBlank { "🧑🏽‍💻" },
                                            style = MaterialTheme.typography.titleMedium
                                        )
                                    }
                                }
                                Column(modifier = Modifier.weight(1f)) {
                                    Text(
                                        text = neighbor.displayName.ifBlank { "Neighbor" },
                                        style = MaterialTheme.typography.titleSmall,
                                        fontWeight = FontWeight.Bold
                                    )
                                    val subtitle = buildString {
                                        if (neighbor.handle.isNotBlank()) append("@${neighbor.handle}")
                                        if (neighbor.neighborhood.isNotBlank()) {
                                            if (isNotEmpty()) append(" • ")
                                            append(neighbor.neighborhood)
                                        }
                                    }
                                    if (subtitle.isNotBlank()) {
                                        Text(
                                            text = subtitle,
                                            style = MaterialTheme.typography.bodySmall,
                                            color = MaterialTheme.colorScheme.onSurfaceVariant
                                        )
                                    }
                                }
                                Icon(
                                    imageVector = Icons.AutoMirrored.Rounded.Chat,
                                    contentDescription = "Start chat",
                                    tint = MaterialTheme.colorScheme.primary
                                )
                            }
                        }
                    }
                }
            }
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class, ExperimentalLayoutApi::class)
@Composable
fun ChatThreadScreen(
    currentUserId: String,
    conversation: Conversation,
    messagesState: UiState<List<ChatMessage>>,
    isSending: Boolean = false,
    currentLanguage: AppLanguage = AppLanguage.ENGLISH,
    onBack: () -> Unit,
    onSendMessage: (String, () -> Unit) -> Unit
) {
    val otherIndex = conversation.participantIds.indexOfFirst { it != currentUserId }.takeIf { it >= 0 } ?: 0
    val otherName = conversation.participantNames.getOrNull(otherIndex)?.ifBlank { "Neighbor" } ?: "Neighbor"
    var draft by remember { mutableStateOf("") }
    val listState = rememberLazyListState()

    val messages = (messagesState as? UiState.Success)?.data.orEmpty()
    LaunchedEffect(messages.size) {
        if (messages.isNotEmpty()) {
            listState.animateScrollToItem(messages.lastIndex)
        }
    }

    val submitDraft: () -> Unit = {
        val text = draft.trim()
        if (text.isNotEmpty() && !isSending) {
            onSendMessage(text) { draft = "" }
        }
    }

    Scaffold(
        contentWindowInsets = WindowInsets(0, 0, 0, 0),
        topBar = {
            TopAppBar(
                title = {
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(10.dp)
                    ) {
                        Surface(
                            shape = CircleShape,
                            color = MaterialTheme.colorScheme.primaryContainer,
                            modifier = Modifier.size(36.dp)
                        ) {
                            Box(contentAlignment = Alignment.Center) {
                                Text(
                                    text = otherName.firstOrNull()?.uppercase() ?: "N",
                                    style = MaterialTheme.typography.titleSmall,
                                    fontWeight = FontWeight.Bold,
                                    color = MaterialTheme.colorScheme.onPrimaryContainer
                                )
                            }
                        }
                        Column {
                            Text(
                                text = otherName,
                                style = MaterialTheme.typography.titleMedium,
                                fontWeight = FontWeight.Bold,
                                maxLines = 1,
                                overflow = TextOverflow.Ellipsis
                            )
                            if (conversation.contextTitle.isNotBlank()) {
                                Text(
                                    text = conversation.contextTitle,
                                    style = MaterialTheme.typography.labelSmall,
                                    color = MaterialTheme.colorScheme.primary,
                                    maxLines = 1,
                                    overflow = TextOverflow.Ellipsis
                                )
                            }
                        }
                    }
                },
                navigationIcon = {
                    IconButton(
                        onClick = onBack,
                        modifier = Modifier.testTag("chat_back_button")
                    ) {
                        Icon(Icons.AutoMirrored.Rounded.ArrowBack, contentDescription = "Back")
                    }
                }
            )
        }
    ) { padding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(padding)
                .navigationBarsPadding()
                .imePadding()
        ) {
            Box(
                modifier = Modifier
                    .weight(1f)
                    .fillMaxWidth()
            ) {
                when (messagesState) {
                    is UiState.Loading -> {
                        Box(Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                            CircularProgressIndicator()
                        }
                    }

                    is UiState.Error -> {
                        Box(Modifier.fillMaxSize().padding(16.dp)) {
                            EmptyStateCard(
                                icon = Icons.AutoMirrored.Rounded.Chat,
                                title = "Could not load messages",
                                subtitle = messagesState.message
                            )
                        }
                    }

                    is UiState.Success -> {
                        if (messages.isEmpty()) {
                            Column(
                                modifier = Modifier
                                    .fillMaxSize()
                                    .padding(24.dp),
                                verticalArrangement = Arrangement.Center,
                                horizontalAlignment = Alignment.CenterHorizontally
                            ) {
                                EmptyStateCard(
                                    icon = Icons.AutoMirrored.Rounded.Chat,
                                    title = if (currentLanguage == AppLanguage.HINDI)
                                        "$otherName से बातचीत शुरू करें"
                                    else
                                        "Say hello to $otherName",
                                    subtitle = if (currentLanguage == AppLanguage.HINDI)
                                        "आपके संदेश Firestore पर रीयल-टाइम में सिंक होते हैं।"
                                    else
                                        "Messages sync in real time over Firestore."
                                )
                                Spacer(modifier = Modifier.size(12.dp))
                                FlowRow(
                                    horizontalArrangement = Arrangement.spacedBy(8.dp),
                                    verticalArrangement = Arrangement.spacedBy(8.dp)
                                ) {
                                    val starters = if (currentLanguage == AppLanguage.HINDI) {
                                        listOf("नमस्ते! 👋", "क्या यह अभी उपलब्ध है?")
                                    } else {
                                        listOf("Hi $otherName! 👋", "Hey, quick question!")
                                    }
                                    starters.forEach { suggestion ->
                                        AssistChip(
                                            onClick = { draft = suggestion },
                                            label = { Text(suggestion) }
                                        )
                                    }
                                }
                            }
                        } else {
                            LazyColumn(
                                state = listState,
                                modifier = Modifier
                                    .fillMaxSize()
                                    .testTag("chat_messages_list"),
                                contentPadding = PaddingValues(horizontal = 16.dp, vertical = 12.dp),
                                verticalArrangement = Arrangement.spacedBy(8.dp)
                            ) {
                                items(messages, key = { it.id }) { msg ->
                                    ChatMessageBubble(
                                        message = msg,
                                        isMine = msg.senderId == currentUserId
                                    )
                                }
                            }
                        }
                    }
                }
            }

            HorizontalDivider(color = MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.4f))

            Surface(
                color = MaterialTheme.colorScheme.surface,
                tonalElevation = 2.dp,
                modifier = Modifier.fillMaxWidth()
            ) {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 12.dp, vertical = 10.dp),
                    verticalAlignment = Alignment.Bottom,
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    OutlinedTextField(
                        value = draft,
                        onValueChange = { if (it.length <= 1000) draft = it },
                        placeholder = {
                            Text(
                                if (currentLanguage == AppLanguage.HINDI)
                                    "$otherName को संदेश लिखें..."
                                else
                                    "Message $otherName..."
                            )
                        },
                        shape = RoundedCornerShape(24.dp),
                        maxLines = 4,
                        keyboardOptions = KeyboardOptions(imeAction = ImeAction.Send),
                        keyboardActions = KeyboardActions(onSend = { submitDraft() }),
                        modifier = Modifier
                            .weight(1f)
                            .testTag("chat_message_input")
                    )
                    FilledIconButton(
                        onClick = submitDraft,
                        enabled = draft.trim().isNotEmpty() && !isSending,
                        modifier = Modifier
                            .size(48.dp)
                            .testTag("chat_send_button")
                    ) {
                        if (isSending) {
                            CircularProgressIndicator(
                                modifier = Modifier.size(18.dp),
                                color = MaterialTheme.colorScheme.onPrimary,
                                strokeWidth = 2.dp
                            )
                        } else {
                            Icon(
                                imageVector = Icons.AutoMirrored.Rounded.Send,
                                contentDescription = if (currentLanguage == AppLanguage.HINDI) "भेजें" else "Send"
                            )
                        }
                    }
                }
            }
        }
    }
}

@Composable
private fun ChatMessageBubble(
    message: ChatMessage,
    isMine: Boolean
) {
    val bubbleShape = RoundedCornerShape(
        topStart = 18.dp,
        topEnd = 18.dp,
        bottomStart = if (isMine) 18.dp else 4.dp,
        bottomEnd = if (isMine) 4.dp else 18.dp
    )

    Row(
        modifier = Modifier
            .fillMaxWidth()
            .testTag("chat_bubble_${message.id}"),
        horizontalArrangement = if (isMine) Arrangement.End else Arrangement.Start
    ) {
        Surface(
            color = if (isMine) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.surfaceVariant,
            shape = bubbleShape,
            modifier = Modifier.widthIn(max = 296.dp)
        ) {
            Column(
                modifier = Modifier.padding(horizontal = 14.dp, vertical = 10.dp),
                verticalArrangement = Arrangement.spacedBy(3.dp)
            ) {
                if (!isMine && message.senderName.isNotBlank()) {
                    Text(
                        text = message.senderName,
                        style = MaterialTheme.typography.labelSmall,
                        fontWeight = FontWeight.SemiBold,
                        color = MaterialTheme.colorScheme.primary
                    )
                }
                Text(
                    text = message.text,
                    color = if (isMine) MaterialTheme.colorScheme.onPrimary else MaterialTheme.colorScheme.onSurfaceVariant,
                    style = MaterialTheme.typography.bodyMedium
                )
                Text(
                    text = formatRelativeTime(message.createdAt),
                    color = (if (isMine) MaterialTheme.colorScheme.onPrimary else MaterialTheme.colorScheme.onSurfaceVariant)
                        .copy(alpha = 0.72f),
                    style = MaterialTheme.typography.labelSmall,
                    modifier = Modifier.align(Alignment.End)
                )
            }
        }
    }
}
