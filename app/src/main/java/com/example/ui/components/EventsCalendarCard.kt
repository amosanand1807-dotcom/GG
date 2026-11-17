package com.example.ui.components

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.rounded.KeyboardArrowLeft
import androidx.compose.material.icons.automirrored.rounded.KeyboardArrowRight
import androidx.compose.material.icons.rounded.Today
import androidx.compose.material3.AssistChip
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.ElevatedCard
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import com.example.core.AppLanguage
import com.example.core.CalendarDay
import com.example.core.CalendarMonth
import com.example.models.CityEvent

@Composable
fun EventsCalendarCard(
    displayedMonth: CalendarMonth,
    selectedDay: CalendarDay?,
    today: CalendarDay,
    eventsByDay: Map<CalendarDay, List<CityEvent>>,
    rsvpedEventIds: Set<String>,
    currentLanguage: AppLanguage,
    onPreviousMonth: () -> Unit,
    onNextMonth: () -> Unit,
    onJumpToToday: () -> Unit,
    onSelectDay: (CalendarDay) -> Unit,
    modifier: Modifier = Modifier
) {
    val cells = remember(displayedMonth) { displayedMonth.calendarGridCells() }
    val weeks = remember(cells) { cells.chunked(7) }
    val monthEventCount = remember(displayedMonth, eventsByDay) {
        eventsByDay.entries
            .filter { (day, _) -> day.toMonth() == displayedMonth }
            .sumOf { it.value.size }
    }
    val weekdayLabels = remember(currentLanguage) {
        if (currentLanguage == AppLanguage.HINDI) {
            listOf("रवि", "सोम", "मंगल", "बुध", "गुरु", "शुक्र", "शनि")
        } else {
            listOf("Sun", "Mon", "Tue", "Wed", "Thu", "Fri", "Sat")
        }
    }
    val showTodayJump = displayedMonth != today.toMonth() || selectedDay != today

    ElevatedCard(
        modifier = modifier
            .fillMaxWidth()
            .testTag("events_calendar_card"),
        shape = MaterialTheme.shapes.large,
        colors = CardDefaults.elevatedCardColors(
            containerColor = MaterialTheme.colorScheme.surface
        ),
        elevation = CardDefaults.elevatedCardElevation(defaultElevation = 2.dp)
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 12.dp, vertical = 14.dp),
            verticalArrangement = Arrangement.spacedBy(10.dp)
        ) {
            // Month navigation header
            Row(
                modifier = Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                IconButton(
                    onClick = onPreviousMonth,
                    modifier = Modifier.testTag("calendar_prev_month")
                ) {
                    Icon(
                        imageVector = Icons.AutoMirrored.Rounded.KeyboardArrowLeft,
                        contentDescription = if (currentLanguage == AppLanguage.HINDI) "पिछला महीना" else "Previous month"
                    )
                }

                Column(
                    horizontalAlignment = Alignment.CenterHorizontally,
                    verticalArrangement = Arrangement.spacedBy(2.dp)
                ) {
                    Text(
                        text = displayedMonth.formatTitle(currentLanguage),
                        style = MaterialTheme.typography.titleMedium,
                        fontWeight = FontWeight.Bold,
                        color = MaterialTheme.colorScheme.onSurface,
                        modifier = Modifier.testTag("calendar_month_title")
                    )
                    Text(
                        text = if (currentLanguage == AppLanguage.HINDI) {
                            "$monthEventCount कार्यक्रम इस महीने"
                        } else {
                            if (monthEventCount == 1) "1 event this month" else "$monthEventCount events this month"
                        },
                        style = MaterialTheme.typography.labelSmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }

                IconButton(
                    onClick = onNextMonth,
                    modifier = Modifier.testTag("calendar_next_month")
                ) {
                    Icon(
                        imageVector = Icons.AutoMirrored.Rounded.KeyboardArrowRight,
                        contentDescription = if (currentLanguage == AppLanguage.HINDI) "अगला महीना" else "Next month"
                    )
                }
            }

            AnimatedVisibility(visible = showTodayJump) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.Center
                ) {
                    AssistChip(
                        onClick = onJumpToToday,
                        leadingIcon = {
                            Icon(
                                imageVector = Icons.Rounded.Today,
                                contentDescription = null,
                                modifier = Modifier.size(16.dp)
                            )
                        },
                        label = {
                            Text(if (currentLanguage == AppLanguage.HINDI) "आज पर जाएँ" else "Jump to Today")
                        },
                        modifier = Modifier.testTag("calendar_today_button")
                    )
                }
            }

            // Weekday header row
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(4.dp)
            ) {
                weekdayLabels.forEach { dayLabel ->
                    Text(
                        text = dayLabel,
                        style = MaterialTheme.typography.labelSmall,
                        fontWeight = FontWeight.SemiBold,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                        textAlign = TextAlign.Center,
                        modifier = Modifier.weight(1f)
                    )
                }
            }

            // Month day grid
            Column(verticalArrangement = Arrangement.spacedBy(4.dp)) {
                weeks.forEach { week ->
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(4.dp)
                    ) {
                        week.forEach { day ->
                            if (day == null) {
                                Spacer(
                                    modifier = Modifier
                                        .weight(1f)
                                        .height(48.dp)
                                )
                            } else {
                                val dayEvents = eventsByDay[day].orEmpty()
                                val hasGoingEvent = dayEvents.any { it.id in rsvpedEventIds }
                                CalendarDayCell(
                                    day = day,
                                    isToday = day == today,
                                    isSelected = day == selectedDay,
                                    eventCount = dayEvents.size,
                                    hasGoingEvent = hasGoingEvent,
                                    onClick = { onSelectDay(day) },
                                    modifier = Modifier.weight(1f)
                                )
                            }
                        }
                    }
                }
            }

            HorizontalDivider(color = MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.45f))

            // Legend row
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 4.dp),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Row(
                    horizontalArrangement = Arrangement.spacedBy(14.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    LegendDotItem(
                        color = MaterialTheme.colorScheme.primary,
                        label = if (currentLanguage == AppLanguage.HINDI) "कार्यक्रम" else "Events"
                    )
                    LegendDotItem(
                        color = MaterialTheme.colorScheme.tertiary,
                        label = if (currentLanguage == AppLanguage.HINDI) "जा रहे हैं" else "Going"
                    )
                }
                Text(
                    text = if (currentLanguage == AppLanguage.HINDI) "तारीख़ चुनें" else "Tap a date",
                    style = MaterialTheme.typography.labelSmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
        }
    }
}

@Composable
private fun CalendarDayCell(
    day: CalendarDay,
    isToday: Boolean,
    isSelected: Boolean,
    eventCount: Int,
    hasGoingEvent: Boolean,
    onClick: () -> Unit,
    modifier: Modifier = Modifier
) {
    val containerColor = when {
        isSelected -> MaterialTheme.colorScheme.primary
        isToday -> MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.45f)
        eventCount > 0 -> MaterialTheme.colorScheme.secondaryContainer.copy(alpha = 0.35f)
        else -> Color.Transparent
    }
    val textColor = when {
        isSelected -> MaterialTheme.colorScheme.onPrimary
        isToday -> MaterialTheme.colorScheme.primary
        else -> MaterialTheme.colorScheme.onSurface
    }
    val borderModifier = if (isToday && !isSelected) {
        Modifier.border(
            BorderStroke(1.5.dp, MaterialTheme.colorScheme.primary),
            RoundedCornerShape(12.dp)
        )
    } else {
        Modifier
    }

    val accessibilityLabel = buildString {
        append("${day.dayOfMonth}")
        if (isToday) append(", Today")
        if (eventCount > 0) append(", $eventCount event${if (eventCount > 1) "s" else ""}")
        if (hasGoingEvent) append(", RSVP confirmed")
    }

    Box(
        modifier = modifier
            .heightIn(min = 48.dp)
            .clip(RoundedCornerShape(12.dp))
            .background(containerColor)
            .then(borderModifier)
            .clickable(onClick = onClick)
            .semantics { contentDescription = accessibilityLabel }
            .testTag("calendar_day_${day.year}_${day.month}_${day.dayOfMonth}"),
        contentAlignment = Alignment.Center
    ) {
        Column(
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.spacedBy(3.dp),
            modifier = Modifier.padding(vertical = 4.dp)
        ) {
            Text(
                text = day.dayOfMonth.toString(),
                style = MaterialTheme.typography.bodyMedium,
                fontWeight = if (isSelected || isToday || eventCount > 0) FontWeight.Bold else FontWeight.Normal,
                color = textColor
            )
            if (eventCount > 0) {
                Row(
                    horizontalArrangement = Arrangement.spacedBy(2.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    val dotCount = eventCount.coerceAtMost(3)
                    repeat(dotCount) { idx ->
                        val dotColor = when {
                            isSelected -> MaterialTheme.colorScheme.onPrimary
                            idx == 0 && hasGoingEvent -> MaterialTheme.colorScheme.tertiary
                            else -> MaterialTheme.colorScheme.primary
                        }
                        Box(
                            modifier = Modifier
                                .size(5.dp)
                                .clip(CircleShape)
                                .background(dotColor)
                        )
                    }
                }
            } else {
                Spacer(modifier = Modifier.height(5.dp))
            }
        }
    }
}

@Composable
private fun LegendDotItem(color: Color, label: String) {
    Row(
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(5.dp)
    ) {
        Surface(
            color = color,
            shape = CircleShape,
            modifier = Modifier.size(8.dp)
        ) {}
        Text(
            text = label,
            style = MaterialTheme.typography.labelSmall,
            color = MaterialTheme.colorScheme.onSurfaceVariant
        )
    }
}
