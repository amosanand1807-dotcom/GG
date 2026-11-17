package com.example.ui.components

import androidx.compose.animation.core.RepeatMode
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.tween
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.CheckCircle
import androidx.compose.material.icons.rounded.LocationCity
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.ModalBottomSheet
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
import androidx.compose.ui.geometry.CornerRadius
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import com.example.core.CityData
import com.google.firebase.Timestamp
import java.text.NumberFormat
import java.util.Locale

fun formatInr(amount: Long): String {
    if (amount <= 0L) return "Free"
    val formatter = NumberFormat.getNumberInstance(Locale.Builder().setLanguage("en").setRegion("IN").build())
    return "₹${formatter.format(amount)}"
}

fun formatRelativeTime(ts: Timestamp?): String {
    val millis = ts?.toDate()?.time ?: return "Just now"
    val diffSec = ((System.currentTimeMillis() - millis) / 1000L).coerceAtLeast(0L)
    return when {
        diffSec < 60 -> "Just now"
        diffSec < 3600 -> "${diffSec / 60}m ago"
        diffSec < 86400 -> "${diffSec / 3600}h ago"
        else -> "${diffSec / 86400}d ago"
    }
}

@Composable
fun CitySkylineArtwork(
    modifier: Modifier = Modifier,
    cityLabel: String = "CITYLOOP",
    subtitle: String = ""
) {
    val primary = MaterialTheme.colorScheme.primary
    val secondary = MaterialTheme.colorScheme.secondary
    val tertiary = MaterialTheme.colorScheme.tertiary

    Box(
        modifier = modifier
            .clip(RoundedCornerShape(24.dp))
            .background(
                Brush.linearGradient(
                    colors = listOf(
                        Color(0xFF0B3A35),
                        Color(0xFF134E4A),
                        Color(0xFF1E3A5F)
                    )
                )
            )
    ) {
        Canvas(modifier = Modifier.fillMaxSize()) {
            val w = size.width
            val h = size.height

            // Warm twilight glow
            drawCircle(
                brush = Brush.radialGradient(
                    colors = listOf(
                        Color(0x66FF9F1C),
                        Color(0x11FF9F1C),
                        Color.Transparent
                    ),
                    center = Offset(w * 0.78f, h * 0.28f),
                    radius = w * 0.45f
                ),
                radius = w * 0.45f,
                center = Offset(w * 0.78f, h * 0.28f)
            )

            // Stylized interlocking loop arc
            drawArc(
                color = Color(0x55FF9F1C),
                startAngle = 190f,
                sweepAngle = 160f,
                useCenter = false,
                topLeft = Offset(w * 0.12f, h * 0.12f),
                size = Size(w * 0.76f, h * 0.9f),
                style = Stroke(width = 6f)
            )

            // City buildings silhouette
            val buildingColor = Color(0xFF072623)
            val windowColor = Color(0xFFFFD166)
            val blocks = listOf(
                Triple(0.05f, 0.10f, 0.42f),
                Triple(0.17f, 0.11f, 0.58f),
                Triple(0.30f, 0.09f, 0.36f),
                Triple(0.41f, 0.13f, 0.68f),
                Triple(0.56f, 0.10f, 0.48f),
                Triple(0.68f, 0.12f, 0.60f),
                Triple(0.82f, 0.11f, 0.40f)
            )
            blocks.forEach { (xFrac, wFrac, hFrac) ->
                val bx = w * xFrac
                val bw = w * wFrac
                val bh = h * hFrac
                val by = h - bh
                drawRoundRect(
                    color = buildingColor,
                    topLeft = Offset(bx, by),
                    size = Size(bw, bh + 12f),
                    cornerRadius = CornerRadius(10f, 10f)
                )
                // Windows
                val cols = 2
                val rows = (hFrac * 7).toInt().coerceIn(2, 5)
                for (r in 0 until rows) {
                    for (c in 0 until cols) {
                        val wx = bx + bw * (0.24f + c * 0.36f)
                        val wy = by + 16f + r * 22f
                        if (wy < h - 18f) {
                            drawRect(
                                color = if ((r + c) % 2 == 0) windowColor.copy(alpha = 0.78f) else Color.White.copy(alpha = 0.25f),
                                topLeft = Offset(wx, wy),
                                size = Size(bw * 0.16f, 10f)
                            )
                        }
                    }
                }
            }

            // Rolling foreground wave
            val wavePath = Path().apply {
                moveTo(0f, h * 0.88f)
                quadraticTo(w * 0.35f, h * 0.78f, w * 0.65f, h * 0.87f)
                quadraticTo(w * 0.85f, h * 0.93f, w, h * 0.84f)
                lineTo(w, h)
                lineTo(0f, h)
                close()
            }
            drawPath(wavePath, color = primary.copy(alpha = 0.65f))
            // Keep secondary/tertiary referenced cleanly
            drawCircle(
                color = secondary.copy(alpha = 0.25f),
                radius = 18f,
                center = Offset(w * 0.14f, h * 0.22f)
            )
            drawCircle(
                color = tertiary.copy(alpha = 0.20f),
                radius = 12f,
                center = Offset(w * 0.88f, h * 0.18f)
            )
        }

        Column(
            modifier = Modifier
                .align(Alignment.BottomStart)
                .padding(18.dp)
        ) {
            Text(
                text = cityLabel,
                style = MaterialTheme.typography.headlineSmall,
                color = Color(0xFFFFF8F0),
                fontWeight = FontWeight.Bold
            )
            if (subtitle.isNotBlank()) {
                Spacer(modifier = Modifier.height(4.dp))
                Text(
                    text = subtitle,
                    style = MaterialTheme.typography.bodyMedium,
                    color = Color(0xFFE2F1EE)
                )
            }
        }
    }
}

@Composable
fun SkeletonPostCard(modifier: Modifier = Modifier) {
    val transition = rememberInfiniteTransition(label = "skeleton")
    val alpha by transition.animateFloat(
        initialValue = 0.35f,
        targetValue = 0.85f,
        animationSpec = infiniteRepeatable(
            animation = tween(850),
            repeatMode = RepeatMode.Reverse
        ),
        label = "skeleton_alpha"
    )
    val shimmer = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = alpha)

    Card(
        modifier = modifier.fillMaxWidth(),
        shape = MaterialTheme.shapes.large,
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
        elevation = CardDefaults.cardElevation(defaultElevation = 1.dp)
    ) {
        Column(modifier = Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(12.dp)) {
            Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                Box(modifier = Modifier.size(42.dp).clip(CircleShape).background(shimmer))
                Column(verticalArrangement = Arrangement.spacedBy(6.dp)) {
                    Box(modifier = Modifier.width(140.dp).height(14.dp).clip(RoundedCornerShape(6.dp)).background(shimmer))
                    Box(modifier = Modifier.width(90.dp).height(11.dp).clip(RoundedCornerShape(6.dp)).background(shimmer))
                }
            }
            Box(modifier = Modifier.fillMaxWidth().height(16.dp).clip(RoundedCornerShape(6.dp)).background(shimmer))
            Box(modifier = Modifier.fillMaxWidth(0.75f).height(16.dp).clip(RoundedCornerShape(6.dp)).background(shimmer))
            Box(modifier = Modifier.fillMaxWidth().height(120.dp).clip(RoundedCornerShape(14.dp)).background(shimmer))
        }
    }
}

@Composable
fun EmptyStateCard(
    icon: ImageVector,
    title: String,
    subtitle: String,
    ctaLabel: String? = null,
    onCtaClick: (() -> Unit)? = null,
    modifier: Modifier = Modifier
) {
    Surface(
        modifier = modifier.fillMaxWidth(),
        shape = MaterialTheme.shapes.large,
        color = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.45f)
    ) {
        Column(
            modifier = Modifier.padding(28.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.spacedBy(10.dp)
        ) {
            Surface(
                shape = CircleShape,
                color = MaterialTheme.colorScheme.primaryContainer,
                modifier = Modifier.size(64.dp)
            ) {
                Box(contentAlignment = Alignment.Center) {
                    Icon(
                        imageVector = icon,
                        contentDescription = title,
                        tint = MaterialTheme.colorScheme.onPrimaryContainer,
                        modifier = Modifier.size(32.dp)
                    )
                }
            }
            Text(
                text = title,
                style = MaterialTheme.typography.titleLarge,
                textAlign = TextAlign.Center
            )
            Text(
                text = subtitle,
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                textAlign = TextAlign.Center
            )
            if (ctaLabel != null && onCtaClick != null) {
                Spacer(modifier = Modifier.height(6.dp))
                Button(
                    onClick = onCtaClick,
                    modifier = Modifier.testTag("empty_state_cta")
                ) {
                    Text(ctaLabel)
                }
            }
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun CityPickerSheet(
    currentCity: String,
    onCitySelected: (String) -> Unit,
    onDismiss: () -> Unit
) {
    ModalBottomSheet(onDismissRequest = onDismiss) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 20.dp)
                .padding(bottom = 28.dp)
        ) {
            Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                Icon(Icons.Rounded.LocationCity, contentDescription = null, tint = MaterialTheme.colorScheme.primary)
                Text("Select Your City", style = MaterialTheme.typography.headlineSmall)
            }
            Spacer(modifier = Modifier.height(12.dp))
            LazyColumn(
                modifier = Modifier.height(360.dp),
                verticalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                items(CityData.indianCities) { city ->
                    val selected = city.name == currentCity
                    Surface(
                        modifier = Modifier
                            .fillMaxWidth()
                            .clip(MaterialTheme.shapes.medium)
                            .clickable {
                                onCitySelected(city.name)
                                onDismiss()
                            }
                            .testTag("city_option_${city.name}"),
                        color = if (selected) MaterialTheme.colorScheme.primaryContainer else MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f),
                        shape = MaterialTheme.shapes.medium
                    ) {
                        Row(
                            modifier = Modifier.padding(horizontal = 16.dp, vertical = 14.dp),
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.SpaceBetween
                        ) {
                            Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                                Text(city.emoji, style = MaterialTheme.typography.headlineSmall)
                                Column {
                                    Text(city.name, style = MaterialTheme.typography.titleMedium)
                                    Text(city.state, style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
                                }
                            }
                            if (selected) {
                                Icon(Icons.Rounded.CheckCircle, contentDescription = "Selected", tint = MaterialTheme.colorScheme.primary)
                            }
                        }
                    }
                }
            }
        }
    }
}

@Composable
fun ReportContentDialog(
    targetLabel: String,
    onSubmit: (String) -> Unit,
    onDismiss: () -> Unit
) {
    var reason by remember { mutableStateOf("") }
    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text("Report $targetLabel") },
        text = {
            Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                Text("Help keep CITYLOOP safe and neighborly. Tell moderators why this should be reviewed.")
                OutlinedTextField(
                    value = reason,
                    onValueChange = { if (it.length <= 300) reason = it },
                    label = { Text("Reason (min 3 characters)") },
                    modifier = Modifier.fillMaxWidth().testTag("report_reason_input"),
                    minLines = 2,
                    maxLines = 4
                )
            }
        },
        confirmButton = {
            Button(
                onClick = {
                    if (reason.trim().length >= 3) {
                        onSubmit(reason.trim())
                        onDismiss()
                    }
                },
                enabled = reason.trim().length >= 3,
                modifier = Modifier.testTag("submit_report_button")
            ) {
                Text("Submit Report")
            }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) { Text("Cancel") }
        }
    )
}
