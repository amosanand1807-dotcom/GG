package com.example.ui.screens

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
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
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.Button
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.FilterChip
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Scaffold
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
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.unit.dp
import com.example.core.CityData
import com.example.core.LocalStrings
import com.google.firebase.Firebase
import com.google.firebase.auth.auth

@OptIn(ExperimentalLayoutApi::class)
@Composable
fun OnboardingScreen(
    isBusy: Boolean,
    errorMessage: String?,
    onCompleteOnboarding: (
        displayName: String,
        handle: String,
        city: String,
        neighborhood: String,
        bio: String,
        avatarEmoji: String,
        interests: List<String>
    ) -> Unit
) {
    val strings = LocalStrings.current
    val firebaseUser = Firebase.auth.currentUser
    val defaultName = firebaseUser?.displayName ?: "Neighbor"
    val defaultHandle = defaultName.lowercase().replace(Regex("[^a-z0-9]"), "_").take(16).ifBlank { "city_neighbor" }

    var displayName by remember { mutableStateOf(defaultName) }
    var handle by remember { mutableStateOf(defaultHandle) }
    var selectedCity by remember { mutableStateOf(CityData.defaultCity) }
    val neighborhoods = remember(selectedCity) { CityData.neighborhoodsFor(selectedCity) }
    var selectedNeighborhood by remember(selectedCity) { mutableStateOf(neighborhoods.firstOrNull().orEmpty()) }
    var bio by remember { mutableStateOf("") }
    var selectedEmoji by remember { mutableStateOf(CityData.avatarEmojis.first()) }
    var selectedInterests by remember { mutableStateOf(setOf("Chai & Cafes", "Local Deals")) }

    Scaffold { innerPadding ->
        Box(
            modifier = Modifier
                .fillMaxSize()
                .padding(innerPadding),
            contentAlignment = Alignment.TopCenter
        ) {
            Column(
                modifier = Modifier
                    .widthIn(max = 600.dp)
                    .fillMaxSize()
                    .verticalScroll(rememberScrollState())
                    .padding(20.dp),
                verticalArrangement = Arrangement.spacedBy(18.dp)
            ) {
                Text(strings.onboardingTitle, style = MaterialTheme.typography.headlineMedium)
                Text(
                    strings.onboardingSubtitle,
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )

                Text("Pick Your Avatar", style = MaterialTheme.typography.titleMedium)
                FlowRow(
                    horizontalArrangement = Arrangement.spacedBy(10.dp),
                    verticalArrangement = Arrangement.spacedBy(10.dp)
                ) {
                    CityData.avatarEmojis.forEach { emoji ->
                        val chosen = emoji == selectedEmoji
                        Surface(
                            shape = CircleShape,
                            color = if (chosen) MaterialTheme.colorScheme.primaryContainer else MaterialTheme.colorScheme.surfaceVariant,
                            modifier = Modifier
                                .size(48.dp)
                                .clip(CircleShape)
                                .clickable { selectedEmoji = emoji }
                        ) {
                            Box(contentAlignment = Alignment.Center) {
                                Text(emoji, style = MaterialTheme.typography.titleLarge)
                            }
                        }
                    }
                }

                OutlinedTextField(
                    value = displayName,
                    onValueChange = { if (it.length <= 50) displayName = it },
                    label = { Text("Display Name") },
                    singleLine = true,
                    modifier = Modifier.fillMaxWidth().testTag("onboarding_name_input")
                )

                OutlinedTextField(
                    value = handle,
                    onValueChange = { if (it.length <= 30) handle = it },
                    label = { Text("Username Handle") },
                    prefix = { Text("@") },
                    singleLine = true,
                    modifier = Modifier.fillMaxWidth().testTag("onboarding_handle_input")
                )

                Text("Select Your City", style = MaterialTheme.typography.titleMedium)
                FlowRow(
                    horizontalArrangement = Arrangement.spacedBy(8.dp),
                    verticalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    CityData.indianCities.forEach { city ->
                        FilterChip(
                            selected = selectedCity == city.name,
                            onClick = { selectedCity = city.name },
                            label = { Text("${city.emoji} ${city.name}") },
                            modifier = Modifier.testTag("onboarding_city_${city.name}")
                        )
                    }
                }

                Text("Neighborhood", style = MaterialTheme.typography.titleMedium)
                FlowRow(
                    horizontalArrangement = Arrangement.spacedBy(8.dp),
                    verticalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    neighborhoods.forEach { area ->
                        FilterChip(
                            selected = selectedNeighborhood == area,
                            onClick = { selectedNeighborhood = area },
                            label = { Text(area) }
                        )
                    }
                }

                OutlinedTextField(
                    value = bio,
                    onValueChange = { if (it.length <= 240) bio = it },
                    label = { Text("Short Bio (Optional)") },
                    placeholder = { Text("Foodie, runner, startup builder in $selectedCity...") },
                    minLines = 2,
                    maxLines = 3,
                    modifier = Modifier.fillMaxWidth().testTag("onboarding_bio_input")
                )

                Text("Interests (Up to 8)", style = MaterialTheme.typography.titleMedium)
                FlowRow(
                    horizontalArrangement = Arrangement.spacedBy(8.dp),
                    verticalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    CityData.interestOptions.forEach { interest ->
                        val active = interest in selectedInterests
                        FilterChip(
                            selected = active,
                            onClick = {
                                selectedInterests = if (active) {
                                    selectedInterests - interest
                                } else if (selectedInterests.size < 8) {
                                    selectedInterests + interest
                                } else selectedInterests
                            },
                            label = { Text(interest) }
                        )
                    }
                }

                if (!errorMessage.isNullOrBlank()) {
                    Text(
                        text = errorMessage,
                        color = MaterialTheme.colorScheme.error,
                        style = MaterialTheme.typography.bodySmall
                    )
                }

                Button(
                    onClick = {
                        onCompleteOnboarding(
                            displayName,
                            handle,
                            selectedCity,
                            selectedNeighborhood,
                            bio,
                            selectedEmoji,
                            selectedInterests.toList()
                        )
                    },
                    enabled = !isBusy && displayName.trim().isNotEmpty(),
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(52.dp)
                        .testTag("onboarding_complete_button")
                ) {
                    if (isBusy) {
                        CircularProgressIndicator(modifier = Modifier.size(22.dp), color = MaterialTheme.colorScheme.onPrimary)
                    } else {
                        Text(strings.continueBtn, style = MaterialTheme.typography.titleMedium)
                    }
                }

                Spacer(modifier = Modifier.height(16.dp))
            }
        }
    }
}
