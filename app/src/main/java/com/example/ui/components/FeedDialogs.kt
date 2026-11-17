package com.example.ui.components

import android.net.Uri
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.PickVisualMediaRequest
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.rounded.Send
import androidx.compose.material.icons.rounded.AutoAwesome
import androidx.compose.material.icons.rounded.Close
import androidx.compose.material.icons.rounded.PhotoLibrary
import androidx.compose.material3.AssistChip
import androidx.compose.material3.Button
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FilterChip
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.ModalBottomSheet
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
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
import androidx.compose.ui.window.Dialog
import androidx.compose.ui.window.DialogProperties
import coil.compose.AsyncImage
import com.example.core.AppLanguage
import com.example.core.LocalStrings
import com.example.core.UiState
import com.example.models.PostComment

@Composable
fun ComposePostDialog(
    activeCity: String,
    defaultNeighborhood: String,
    currentLanguage: AppLanguage,
    aiBusy: Boolean,
    onPolishWithAi: (String, String, (String) -> Unit) -> Unit,
    onPublish: (
        postType: String,
        caption: String,
        neighborhood: String,
        mediaUri: String?,
        tagsRaw: String,
        onSuccess: () -> Unit,
        onError: (String) -> Unit
    ) -> Unit,
    onDismiss: () -> Unit
) {
    var postType by remember { mutableStateOf("text") }
    var caption by remember { mutableStateOf("") }
    var neighborhood by remember { mutableStateOf(defaultNeighborhood) }
    var tagsRaw by remember { mutableStateOf("") }
    var pickedPhotoUri by remember { mutableStateOf<Uri?>(null) }
    var isSubmitting by remember { mutableStateOf(false) }
    var errorMessage by remember { mutableStateOf<String?>(null) }

    val photoPicker = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.PickVisualMedia()
    ) { uri ->
        pickedPhotoUri = uri
    }

    Dialog(
        onDismissRequest = { if (!isSubmitting) onDismiss() },
        properties = DialogProperties(usePlatformDefaultWidth = false)
    ) {
        Surface(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 16.dp)
                .widthIn(max = 520.dp)
                .testTag("compose_post_dialog"),
            shape = MaterialTheme.shapes.extraLarge,
            color = MaterialTheme.colorScheme.surface,
            tonalElevation = 6.dp
        ) {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .verticalScroll(rememberScrollState())
                    .padding(20.dp),
                verticalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text(
                        text = if (currentLanguage == AppLanguage.HINDI)
                            "$activeCity में नया अपडेट"
                        else
                            "New Update in $activeCity",
                        style = MaterialTheme.typography.titleLarge,
                        fontWeight = FontWeight.Bold
                    )
                    IconButton(
                        onClick = { if (!isSubmitting) onDismiss() },
                        modifier = Modifier.testTag("compose_dialog_close")
                    ) {
                        Icon(Icons.Rounded.Close, contentDescription = "Close dialog")
                    }
                }

                Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    FilterChip(
                        selected = postType == "text",
                        onClick = { postType = "text" },
                        label = { Text(if (currentLanguage == AppLanguage.HINDI) "टेक्स्ट" else "Text Update") },
                        modifier = Modifier.testTag("compose_type_text")
                    )
                    FilterChip(
                        selected = postType == "photo",
                        onClick = { postType = "photo" },
                        label = { Text(if (currentLanguage == AppLanguage.HINDI) "फ़ोटो" else "Photo") },
                        leadingIcon = {
                            Icon(Icons.Rounded.PhotoLibrary, contentDescription = null, modifier = Modifier.size(16.dp))
                        },
                        modifier = Modifier.testTag("compose_type_photo")
                    )
                }

                OutlinedTextField(
                    value = caption,
                    onValueChange = {
                        if (it.length <= 1000) {
                            caption = it
                            errorMessage = null
                        }
                    },
                    label = {
                        Text(
                            if (currentLanguage == AppLanguage.HINDI)
                                "अपने मोहल्ले के साथ कुछ साझा करें..."
                            else
                                "What's happening in your neighborhood?"
                        )
                    },
                    supportingText = { Text("${caption.length}/1000") },
                    minLines = 3,
                    maxLines = 6,
                    modifier = Modifier
                        .fillMaxWidth()
                        .testTag("compose_dialog_caption")
                )

                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(8.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    AssistChip(
                        onClick = {
                            onPolishWithAi(caption, "CityLoop Post") { polished ->
                                caption = polished.take(1000)
                            }
                        },
                        enabled = caption.isNotBlank() && !aiBusy && !isSubmitting,
                        label = {
                            if (aiBusy) {
                                CircularProgressIndicator(modifier = Modifier.size(14.dp), strokeWidth = 2.dp)
                            } else {
                                Text(if (currentLanguage == AppLanguage.HINDI) "AI से निखारें" else "Polish with AI")
                            }
                        },
                        leadingIcon = {
                            Icon(Icons.Rounded.AutoAwesome, contentDescription = null, modifier = Modifier.size(16.dp))
                        },
                        modifier = Modifier.testTag("compose_dialog_ai_polish")
                    )

                    if (postType == "photo") {
                        OutlinedButton(
                            onClick = {
                                photoPicker.launch(PickVisualMediaRequest(ActivityResultContracts.PickVisualMedia.ImageOnly))
                            },
                            enabled = !isSubmitting,
                            modifier = Modifier.testTag("compose_dialog_pick_photo")
                        ) {
                            Icon(Icons.Rounded.PhotoLibrary, contentDescription = null, modifier = Modifier.size(16.dp))
                            Spacer(modifier = Modifier.width(6.dp))
                            Text(if (pickedPhotoUri == null) "Add Photo" else "Change Photo")
                        }
                    }
                }

                if (postType == "photo" && pickedPhotoUri != null) {
                    AsyncImage(
                        model = pickedPhotoUri,
                        contentDescription = "Selected post photo",
                        contentScale = ContentScale.Crop,
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(160.dp)
                            .clip(MaterialTheme.shapes.medium)
                    )
                }

                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    OutlinedTextField(
                        value = neighborhood,
                        onValueChange = { if (it.length <= 60) neighborhood = it },
                        label = { Text(if (currentLanguage == AppLanguage.HINDI) "मोहल्ला" else "Area") },
                        singleLine = true,
                        modifier = Modifier
                            .weight(1f)
                            .testTag("compose_dialog_area")
                    )
                    OutlinedTextField(
                        value = tagsRaw,
                        onValueChange = { tagsRaw = it },
                        label = { Text("#tags") },
                        placeholder = { Text("chai, traffic") },
                        singleLine = true,
                        modifier = Modifier
                            .weight(1f)
                            .testTag("compose_dialog_tags")
                    )
                }

                AnimatedVisibility(visible = !errorMessage.isNullOrBlank()) {
                    Surface(
                        color = MaterialTheme.colorScheme.errorContainer,
                        shape = MaterialTheme.shapes.medium,
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Text(
                            text = errorMessage.orEmpty(),
                            color = MaterialTheme.colorScheme.onErrorContainer,
                            style = MaterialTheme.typography.bodySmall,
                            modifier = Modifier.padding(12.dp)
                        )
                    }
                }

                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.End,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    TextButton(
                        onClick = onDismiss,
                        enabled = !isSubmitting,
                        modifier = Modifier.testTag("compose_dialog_cancel")
                    ) {
                        Text(if (currentLanguage == AppLanguage.HINDI) "रद्द करें" else "Cancel")
                    }
                    Spacer(modifier = Modifier.width(8.dp))
                    Button(
                        onClick = {
                            if (caption.trim().isEmpty()) {
                                errorMessage = "Please enter an update before posting."
                                return@Button
                            }
                            errorMessage = null
                            isSubmitting = true
                            onPublish(
                                postType,
                                caption,
                                neighborhood,
                                pickedPhotoUri?.toString(),
                                tagsRaw,
                                {
                                    isSubmitting = false
                                    onDismiss()
                                },
                                { msg ->
                                    isSubmitting = false
                                    errorMessage = msg
                                }
                            )
                        },
                        enabled = caption.trim().isNotEmpty() && !isSubmitting,
                        modifier = Modifier.testTag("compose_dialog_submit")
                    ) {
                        if (isSubmitting) {
                            CircularProgressIndicator(
                                modifier = Modifier.size(18.dp),
                                color = MaterialTheme.colorScheme.onPrimary,
                                strokeWidth = 2.dp
                            )
                            Spacer(modifier = Modifier.width(8.dp))
                        }
                        Text(if (currentLanguage == AppLanguage.HINDI) "पोस्ट करें" else "Post Update")
                    }
                }
            }
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun FeedCommentsBottomSheet(
    commentsState: UiState<List<PostComment>>,
    onAddComment: (String, String) -> Unit,
    onDismiss: () -> Unit
) {
    val strings = LocalStrings.current
    var draft by remember { mutableStateOf("") }
    var replyingTo by remember { mutableStateOf<PostComment?>(null) }

    ModalBottomSheet(onDismissRequest = onDismiss) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 16.dp)
                .padding(bottom = 24.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            Text(strings.commentsTitle, style = MaterialTheme.typography.titleLarge)

            when (commentsState) {
                is UiState.Loading -> CircularProgressIndicator(modifier = Modifier.align(Alignment.CenterHorizontally))
                is UiState.Error -> Text(commentsState.message, color = MaterialTheme.colorScheme.error)
                is UiState.Success -> {
                    if (commentsState.data.isEmpty()) {
                        Text(
                            "No comments yet. Start the neighborhood conversation!",
                            style = MaterialTheme.typography.bodyMedium,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    } else {
                        LazyColumn(
                            modifier = Modifier.height(250.dp),
                            verticalArrangement = Arrangement.spacedBy(10.dp)
                        ) {
                            items(commentsState.data, key = { it.id }) { comment ->
                                val isReply = comment.parentCommentId.isNotBlank()
                                Surface(
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .padding(start = if (isReply) 24.dp else 0.dp),
                                    color = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.45f),
                                    shape = MaterialTheme.shapes.medium
                                ) {
                                    Column(modifier = Modifier.padding(12.dp), verticalArrangement = Arrangement.spacedBy(4.dp)) {
                                        Row(
                                            modifier = Modifier.fillMaxWidth(),
                                            horizontalArrangement = Arrangement.SpaceBetween,
                                            verticalAlignment = Alignment.CenterVertically
                                        ) {
                                            Text(
                                                "${comment.authorEmoji} ${comment.authorName}",
                                                style = MaterialTheme.typography.labelLarge
                                            )
                                            Text(
                                                formatRelativeTime(comment.createdAt),
                                                style = MaterialTheme.typography.labelSmall,
                                                color = MaterialTheme.colorScheme.onSurfaceVariant
                                            )
                                        }
                                        Text(comment.body, style = MaterialTheme.typography.bodyMedium)
                                        TextButton(
                                            onClick = { replyingTo = comment },
                                            contentPadding = PaddingValues(0.dp)
                                        ) {
                                            Text("Reply", style = MaterialTheme.typography.labelMedium)
                                        }
                                    }
                                }
                            }
                        }
                    }
                }
            }

            if (replyingTo != null) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text(
                        "Replying to ${replyingTo?.authorName}",
                        style = MaterialTheme.typography.labelMedium,
                        color = MaterialTheme.colorScheme.primary
                    )
                    TextButton(onClick = { replyingTo = null }) { Text("Clear") }
                }
            }

            Row(
                modifier = Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                OutlinedTextField(
                    value = draft,
                    onValueChange = { if (it.length <= 500) draft = it },
                    placeholder = { Text(strings.addCommentHint) },
                    modifier = Modifier
                        .weight(1f)
                        .testTag("comment_input_field"),
                    maxLines = 3
                )
                IconButton(
                    onClick = {
                        if (draft.isNotBlank()) {
                            onAddComment(draft, replyingTo?.id.orEmpty())
                            draft = ""
                            replyingTo = null
                        }
                    },
                    enabled = draft.isNotBlank(),
                    modifier = Modifier.testTag("send_comment_button")
                ) {
                    Icon(Icons.AutoMirrored.Rounded.Send, contentDescription = "Send comment")
                }
            }
        }
    }
}
