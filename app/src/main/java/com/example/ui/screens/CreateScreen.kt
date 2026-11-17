package com.example.ui.screens

import android.net.Uri
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.PickVisualMediaRequest
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ExperimentalLayoutApi
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.AutoAwesome
import androidx.compose.material.icons.rounded.PhotoLibrary
import androidx.compose.material.icons.rounded.Videocam
import androidx.compose.material3.AssistChip
import androidx.compose.material3.Button
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.FilterChip
import androidx.compose.material3.Icon
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
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
import coil.compose.AsyncImage
import com.example.core.CityData
import com.example.core.LocalStrings
import com.example.models.CityCommunity
import com.example.models.UserProfile

@OptIn(ExperimentalLayoutApi::class)
@Composable
fun CreateScreen(
    profile: UserProfile,
    communities: List<CityCommunity>,
    aiBusy: Boolean,
    onPolishWithAi: (String, String, (String) -> Unit) -> Unit,
    onCreatePost: (
        postType: String,
        caption: String,
        neighborhood: String,
        mediaUri: String?,
        videoDurationSec: Long,
        tagsRaw: String,
        communityId: String,
        communityName: String
    ) -> Unit,
    onCreateListing: (
        title: String,
        description: String,
        priceInr: Long,
        category: String,
        condition: String,
        neighborhood: String,
        mediaUri: String?
    ) -> Unit,
    onCreateCommunity: (name: String, desc: String, category: String, emoji: String) -> Unit,
    onCreateEvent: (title: String, desc: String, venue: String, dateLabel: String, category: String, priceInr: Long) -> Unit,
    onCreateBusiness: (name: String, category: String, desc: String, address: String, offerTitle: String, offerCode: String, discountLabel: String) -> Unit
) {
    val strings = LocalStrings.current
    var mode by remember { mutableStateOf("post") } // post, listing, community, event, business

    // Shared Photo Picker (Play-compliant zero-permission)
    var pickedPhotoUri by remember { mutableStateOf<Uri?>(null) }
    val photoPicker = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.PickVisualMedia()
    ) { uri ->
        pickedPhotoUri = uri
    }

    // Post state
    var postType by remember { mutableStateOf("text") }
    var caption by remember { mutableStateOf("") }
    var tagsRaw by remember { mutableStateOf("") }
    var selectedCommId by remember { mutableStateOf("") }
    var selectedCommName by remember { mutableStateOf("") }

    // Listing state
    var listingTitle by remember { mutableStateOf("") }
    var listingDesc by remember { mutableStateOf("") }
    var listingPrice by remember { mutableStateOf("") }
    var listingCategory by remember { mutableStateOf(CityData.marketplaceCategories[1]) }
    var listingCondition by remember { mutableStateOf("Like New") }

    // Community state
    var commName by remember { mutableStateOf("") }
    var commDesc by remember { mutableStateOf("") }
    var commCategory by remember { mutableStateOf(CityData.communityCategories.first()) }
    var commEmoji by remember { mutableStateOf("🏘️") }

    // Event state
    var eventTitle by remember { mutableStateOf("") }
    var eventDesc by remember { mutableStateOf("") }
    var eventVenue by remember { mutableStateOf("") }
    var eventDate by remember { mutableStateOf("Sat, 6:00 PM") }
    var eventCategory by remember { mutableStateOf(CityData.eventCategories.first()) }
    var eventPrice by remember { mutableStateOf("0") }

    // Business state
    var bizName by remember { mutableStateOf("") }
    var bizCategory by remember { mutableStateOf(CityData.businessCategories.first()) }
    var bizDesc by remember { mutableStateOf("") }
    var bizAddress by remember { mutableStateOf("") }
    var bizOfferTitle by remember { mutableStateOf("") }
    var bizOfferCode by remember { mutableStateOf("") }
    var bizDiscountLabel by remember { mutableStateOf("") }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .verticalScroll(rememberScrollState())
            .padding(16.dp),
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        Column(
            modifier = Modifier.widthIn(max = 600.dp).fillMaxWidth(),
            verticalArrangement = Arrangement.spacedBy(14.dp)
        ) {
            Text("Create in ${profile.city}", style = MaterialTheme.typography.headlineSmall)

            FlowRow(horizontalArrangement = Arrangement.spacedBy(8.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
                listOf(
                    "post" to "Post / Reel",
                    "listing" to "Sell Item",
                    "community" to "Community",
                    "event" to "Event",
                    "business" to "Business"
                ).forEach { (key, label) ->
                    FilterChip(
                        selected = mode == key,
                        onClick = { mode = key },
                        label = { Text(label) },
                        modifier = Modifier.testTag("create_mode_$key")
                    )
                }
            }

            when (mode) {
                "post" -> {
                    FlowRow(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                        FilterChip(selected = postType == "text", onClick = { postType = "text" }, label = { Text("Text Update") })
                        FilterChip(
                            selected = postType == "photo",
                            onClick = { postType = "photo" },
                            label = { Text("Photo") },
                            leadingIcon = { Icon(Icons.Rounded.PhotoLibrary, contentDescription = null, modifier = Modifier.size(16.dp)) }
                        )
                        FilterChip(
                            selected = postType == "reel",
                            onClick = { postType = "reel" },
                            label = { Text("Short Reel") },
                            leadingIcon = { Icon(Icons.Rounded.Videocam, contentDescription = null, modifier = Modifier.size(16.dp)) }
                        )
                    }

                    if (postType != "text") {
                        OutlinedButton(
                            onClick = {
                                val type = if (postType == "reel") {
                                    ActivityResultContracts.PickVisualMedia.ImageAndVideo
                                } else {
                                    ActivityResultContracts.PickVisualMedia.ImageOnly
                                }
                                photoPicker.launch(PickVisualMediaRequest(type))
                            },
                            modifier = Modifier.fillMaxWidth().testTag("pick_media_button")
                        ) {
                            Icon(Icons.Rounded.PhotoLibrary, contentDescription = null)
                            Spacer(Modifier.size(8.dp))
                            Text(if (pickedPhotoUri == null) "Pick from Gallery (Zero-Permission)" else "Change Selected Media")
                        }
                        if (pickedPhotoUri != null) {
                            AsyncImage(
                                model = pickedPhotoUri,
                                contentDescription = "Selected media",
                                contentScale = ContentScale.Crop,
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .height(170.dp)
                                    .clip(MaterialTheme.shapes.medium)
                            )
                        }
                    }

                    OutlinedTextField(
                        value = caption,
                        onValueChange = { if (it.length <= 1000) caption = it },
                        label = { Text("What's happening in ${profile.neighborhood.ifBlank { profile.city }}?") },
                        minLines = 3,
                        maxLines = 6,
                        modifier = Modifier.fillMaxWidth().testTag("create_post_caption_input")
                    )

                    Row(horizontalArrangement = Arrangement.spacedBy(8.dp), verticalAlignment = Alignment.CenterVertically) {
                        AssistChip(
                            onClick = {
                                onPolishWithAi(caption, "community post") { polished -> caption = polished }
                            },
                            enabled = caption.isNotBlank() && !aiBusy,
                            label = {
                                if (aiBusy) CircularProgressIndicator(modifier = Modifier.size(14.dp), strokeWidth = 2.dp)
                                else Text("Polish with Gemini AI")
                            },
                            leadingIcon = { Icon(Icons.Rounded.AutoAwesome, contentDescription = null, modifier = Modifier.size(16.dp)) },
                            modifier = Modifier.testTag("ai_polish_post_button")
                        )
                    }

                    OutlinedTextField(
                        value = tagsRaw,
                        onValueChange = { tagsRaw = it },
                        label = { Text("Tags (comma separated, e.g. coffee, meetup)") },
                        singleLine = true,
                        modifier = Modifier.fillMaxWidth()
                    )

                    if (communities.isNotEmpty()) {
                        Text("Link to Community (Optional)", style = MaterialTheme.typography.labelLarge)
                        FlowRow(horizontalArrangement = Arrangement.spacedBy(8.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
                            communities.take(5).forEach { comm ->
                                FilterChip(
                                    selected = selectedCommId == comm.id,
                                    onClick = {
                                        if (selectedCommId == comm.id) {
                                            selectedCommId = ""
                                            selectedCommName = ""
                                        } else {
                                            selectedCommId = comm.id
                                            selectedCommName = comm.name
                                        }
                                    },
                                    label = { Text("${comm.emoji} ${comm.name}") }
                                )
                            }
                        }
                    }

                    Button(
                        onClick = {
                            onCreatePost(
                                postType,
                                caption,
                                profile.neighborhood,
                                pickedPhotoUri?.toString(),
                                if (postType == "reel") 30L else 0L,
                                tagsRaw,
                                selectedCommId,
                                selectedCommName
                            )
                            caption = ""
                            tagsRaw = ""
                            pickedPhotoUri = null
                        },
                        enabled = caption.trim().isNotEmpty(),
                        modifier = Modifier.fillMaxWidth().height(50.dp).testTag("publish_post_button")
                    ) {
                        Text(strings.publishBtn)
                    }
                }

                "listing" -> {
                    OutlinedTextField(
                        value = listingTitle,
                        onValueChange = { if (it.length <= 100) listingTitle = it },
                        label = { Text("Item Title (e.g., Hero Hybrid Cycle)") },
                        singleLine = true,
                        modifier = Modifier.fillMaxWidth().testTag("listing_title_input")
                    )
                    OutlinedTextField(
                        value = listingPrice,
                        onValueChange = { if (it.length <= 8) listingPrice = it.filter { ch -> ch.isDigit() } },
                        label = { Text("Price (₹ INR)") },
                        prefix = { Text("₹ ") },
                        keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                        singleLine = true,
                        modifier = Modifier.fillMaxWidth().testTag("listing_price_input")
                    )
                    OutlinedTextField(
                        value = listingDesc,
                        onValueChange = { if (it.length <= 600) listingDesc = it },
                        label = { Text("Description & Pickup Details") },
                        minLines = 3,
                        modifier = Modifier.fillMaxWidth().testTag("listing_desc_input")
                    )
                    AssistChip(
                        onClick = {
                            onPolishWithAi(listingDesc, "marketplace listing") { polished -> listingDesc = polished }
                        },
                        enabled = listingDesc.isNotBlank() && !aiBusy,
                        label = { Text("Enhance with Gemini AI") },
                        leadingIcon = { Icon(Icons.Rounded.AutoAwesome, contentDescription = null, modifier = Modifier.size(16.dp)) }
                    )
                    Text("Category", style = MaterialTheme.typography.labelLarge)
                    FlowRow(horizontalArrangement = Arrangement.spacedBy(8.dp), verticalArrangement = Arrangement.spacedBy(6.dp)) {
                        CityData.marketplaceCategories.drop(1).forEach { cat ->
                            FilterChip(selected = listingCategory == cat, onClick = { listingCategory = cat }, label = { Text(cat) })
                        }
                    }
                    Text("Condition", style = MaterialTheme.typography.labelLarge)
                    FlowRow(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                        listOf("New", "Like New", "Good", "Fair").forEach { cond ->
                            FilterChip(selected = listingCondition == cond, onClick = { listingCondition = cond }, label = { Text(cond) })
                        }
                    }
                    OutlinedButton(
                        onClick = { photoPicker.launch(PickVisualMediaRequest(ActivityResultContracts.PickVisualMedia.ImageOnly)) },
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Text(if (pickedPhotoUri == null) "Attach Product Photo (Optional)" else "Photo Attached ✓")
                    }
                    Button(
                        onClick = {
                            val price = listingPrice.toLongOrNull() ?: 0L
                            onCreateListing(listingTitle, listingDesc, price, listingCategory, listingCondition, profile.neighborhood, pickedPhotoUri?.toString())
                            listingTitle = ""
                            listingDesc = ""
                            listingPrice = ""
                            pickedPhotoUri = null
                        },
                        enabled = listingTitle.trim().length >= 2 && listingDesc.trim().isNotEmpty(),
                        modifier = Modifier.fillMaxWidth().height(50.dp).testTag("publish_listing_button")
                    ) {
                        Text("Post Listing")
                    }
                }

                "community" -> {
                    OutlinedTextField(
                        value = commName,
                        onValueChange = { if (it.length <= 80) commName = it },
                        label = { Text("Community Name") },
                        singleLine = true,
                        modifier = Modifier.fillMaxWidth().testTag("community_name_input")
                    )
                    OutlinedTextField(
                        value = commDesc,
                        onValueChange = { if (it.length <= 400) commDesc = it },
                        label = { Text("About this Community") },
                        minLines = 3,
                        modifier = Modifier.fillMaxWidth().testTag("community_desc_input")
                    )
                    FlowRow(horizontalArrangement = Arrangement.spacedBy(8.dp), verticalArrangement = Arrangement.spacedBy(6.dp)) {
                        CityData.communityCategories.forEach { cat ->
                            FilterChip(selected = commCategory == cat, onClick = { commCategory = cat }, label = { Text(cat) })
                        }
                    }
                    FlowRow(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                        listOf("🏘️", "☕", "🏃🏽", "💻", "📚", "🐾", "🎨").forEach { e ->
                            FilterChip(selected = commEmoji == e, onClick = { commEmoji = e }, label = { Text(e) })
                        }
                    }
                    Button(
                        onClick = {
                            onCreateCommunity(commName, commDesc, commCategory, commEmoji)
                            commName = ""
                            commDesc = ""
                        },
                        enabled = commName.trim().length >= 2 && commDesc.trim().isNotEmpty(),
                        modifier = Modifier.fillMaxWidth().height(50.dp).testTag("publish_community_button")
                    ) {
                        Text("Launch Community")
                    }
                }

                "event" -> {
                    OutlinedTextField(
                        value = eventTitle,
                        onValueChange = { if (it.length <= 100) eventTitle = it },
                        label = { Text("Event Title") },
                        singleLine = true,
                        modifier = Modifier.fillMaxWidth().testTag("event_title_input")
                    )
                    OutlinedTextField(
                        value = eventVenue,
                        onValueChange = { if (it.length <= 120) eventVenue = it },
                        label = { Text("Venue / Area in ${profile.city}") },
                        singleLine = true,
                        modifier = Modifier.fillMaxWidth()
                    )
                    OutlinedTextField(
                        value = eventDate,
                        onValueChange = { if (it.length <= 60) eventDate = it },
                        label = { Text("Date & Time (e.g., Sun, 8:30 AM)") },
                        singleLine = true,
                        modifier = Modifier.fillMaxWidth()
                    )
                    OutlinedTextField(
                        value = eventPrice,
                        onValueChange = { if (it.length <= 6) eventPrice = it.filter { c -> c.isDigit() } },
                        label = { Text("Ticket Price in ₹ (0 for Free)") },
                        keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                        singleLine = true,
                        modifier = Modifier.fillMaxWidth()
                    )
                    OutlinedTextField(
                        value = eventDesc,
                        onValueChange = { if (it.length <= 600) eventDesc = it },
                        label = { Text("Event Details") },
                        minLines = 3,
                        modifier = Modifier.fillMaxWidth()
                    )
                    FlowRow(horizontalArrangement = Arrangement.spacedBy(8.dp), verticalArrangement = Arrangement.spacedBy(6.dp)) {
                        CityData.eventCategories.forEach { cat ->
                            FilterChip(selected = eventCategory == cat, onClick = { eventCategory = cat }, label = { Text(cat) })
                        }
                    }
                    Button(
                        onClick = {
                            onCreateEvent(eventTitle, eventDesc, eventVenue, eventDate, eventCategory, eventPrice.toLongOrNull() ?: 0L)
                            eventTitle = ""
                            eventDesc = ""
                            eventVenue = ""
                        },
                        enabled = eventTitle.trim().length >= 2 && eventDesc.trim().isNotEmpty(),
                        modifier = Modifier.fillMaxWidth().height(50.dp).testTag("publish_event_button")
                    ) {
                        Text("Publish Event")
                    }
                }

                "business" -> {
                    OutlinedTextField(
                        value = bizName,
                        onValueChange = { if (it.length <= 80) bizName = it },
                        label = { Text("Business Name") },
                        singleLine = true,
                        modifier = Modifier.fillMaxWidth().testTag("biz_name_input")
                    )
                    OutlinedTextField(
                        value = bizAddress,
                        onValueChange = { if (it.length <= 160) bizAddress = it },
                        label = { Text("Street Address / Neighborhood") },
                        singleLine = true,
                        modifier = Modifier.fillMaxWidth()
                    )
                    OutlinedTextField(
                        value = bizDesc,
                        onValueChange = { if (it.length <= 400) bizDesc = it },
                        label = { Text("What do you offer?") },
                        minLines = 2,
                        modifier = Modifier.fillMaxWidth()
                    )
                    Surface(
                        color = MaterialTheme.colorScheme.secondaryContainer.copy(alpha = 0.4f),
                        shape = MaterialTheme.shapes.medium,
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Column(modifier = Modifier.padding(12.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
                            Text("Neighborhood Promo Offer (Optional)", style = MaterialTheme.typography.titleSmall)
                            OutlinedTextField(
                                value = bizOfferTitle,
                                onValueChange = { if (it.length <= 100) bizOfferTitle = it },
                                label = { Text("Offer Headline (e.g. 20% Off Filter Coffee)") },
                                singleLine = true,
                                modifier = Modifier.fillMaxWidth()
                            )
                            Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                                OutlinedTextField(
                                    value = bizOfferCode,
                                    onValueChange = { if (it.length <= 20) bizOfferCode = it.uppercase() },
                                    label = { Text("Code (LOOP20)") },
                                    singleLine = true,
                                    modifier = Modifier.weight(1f)
                                )
                                OutlinedTextField(
                                    value = bizDiscountLabel,
                                    onValueChange = { if (it.length <= 30) bizDiscountLabel = it },
                                    label = { Text("Badge (20% OFF)") },
                                    singleLine = true,
                                    modifier = Modifier.weight(1f)
                                )
                            }
                        }
                    }
                    FlowRow(horizontalArrangement = Arrangement.spacedBy(8.dp), verticalArrangement = Arrangement.spacedBy(6.dp)) {
                        CityData.businessCategories.forEach { cat ->
                            FilterChip(selected = bizCategory == cat, onClick = { bizCategory = cat }, label = { Text(cat) })
                        }
                    }
                    Button(
                        onClick = {
                            onCreateBusiness(bizName, bizCategory, bizDesc, bizAddress, bizOfferTitle, bizOfferCode, bizDiscountLabel)
                            bizName = ""
                            bizDesc = ""
                            bizAddress = ""
                            bizOfferTitle = ""
                            bizOfferCode = ""
                            bizDiscountLabel = ""
                        },
                        enabled = bizName.trim().length >= 2 && bizDesc.trim().isNotEmpty(),
                        modifier = Modifier.fillMaxWidth().height(50.dp).testTag("publish_business_button")
                    ) {
                        Text("Register Business")
                    }
                }
            }

            Spacer(modifier = Modifier.height(20.dp))
        }
    }
}
