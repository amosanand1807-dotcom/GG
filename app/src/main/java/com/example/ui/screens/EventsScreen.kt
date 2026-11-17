package com.example.ui.screens

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.rounded.ViewList
import androidx.compose.material.icons.rounded.Add
import androidx.compose.material.icons.rounded.CalendarMonth
import androidx.compose.material.icons.rounded.Event
import androidx.compose.material.icons.rounded.Search
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FilledTonalButton
import androidx.compose.material3.FilterChip
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.SegmentedButton
import androidx.compose.material3.SegmentedButtonDefaults
import androidx.compose.material3.SingleChoiceSegmentedButtonRow
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.pulltorefresh.PullToRefreshBox
import androidx.compose.material3.pulltorefresh.PullToRefreshDefaults
import androidx.compose.material3.pulltorefresh.rememberPullToRefreshState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.example.core.AppLanguage
import com.example.core.CalendarDay
import com.example.core.CalendarMonth
import com.example.core.UiState
import com.example.core.currentCalendarDay
import com.example.core.formatSelectedDayHeader
import com.example.core.formatShortDayLabel
import com.example.core.resolveEventCalendarDay
import com.example.models.CityEvent
import com.example.models.UserProfile
import com.example.ui.components.EmptyStateCard
import com.example.ui.components.EventCard
import com.example.ui.components.EventsCalendarCard
import com.example.ui.components.SkeletonPostCard

enum class EventsViewMode {
    LIST,
    CALENDAR
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun EventsScreen(
    activeCity: String,
    profile: UserProfile,
    eventsState: UiState<List<CityEvent>>,
    rsvpedEventIds: Set<String>,
    pendingRsvpIds: Set<String> = emptySet(),
    savedTargetIds: Set<String>,
    currentLanguage: AppLanguage,
    isRefreshing: Boolean,
    onRefresh: () -> Unit,
    onToggleEventRsvp: (CityEvent) -> Unit,
    onSaveEvent: (CityEvent) -> Unit,
    onMessageOrganizer: (String, String, String) -> Unit,
    onNavigateToCreate: () -> Unit
) {
    val pullToRefreshState = rememberPullToRefreshState()
    var searchQuery by remember { mutableStateOf("") }
    var filterMode by remember { mutableStateOf("all") } // all, going, free, paid
    var viewMode by rememberSaveable { mutableStateOf(EventsViewMode.LIST) }

    val today = remember { currentCalendarDay() }
    var displayedYear by rememberSaveable { mutableStateOf(today.year) }
    var displayedMonthNumber by rememberSaveable { mutableStateOf(today.month) }
    var selectedYear by rememberSaveable { mutableStateOf<Int?>(today.year) }
    var selectedMonthNumber by rememberSaveable { mutableStateOf<Int?>(today.month) }
    var selectedDayNumber by rememberSaveable { mutableStateOf<Int?>(today.dayOfMonth) }

    val displayedMonth = CalendarMonth(displayedYear, displayedMonthNumber)
    val selectedDay = if (selectedYear != null && selectedMonthNumber != null && selectedDayNumber != null) {
        CalendarDay(selectedYear!!, selectedMonthNumber!!, selectedDayNumber!!)
    } else {
        null
    }

    val setSelectedDay: (CalendarDay?) -> Unit = { day ->
        selectedYear = day?.year
        selectedMonthNumber = day?.month
        selectedDayNumber = day?.dayOfMonth
    }
    val setDisplayedMonth: (CalendarMonth) -> Unit = { month ->
        displayedYear = month.year
        displayedMonthNumber = month.month
    }

    PullToRefreshBox(
        isRefreshing = isRefreshing,
        onRefresh = onRefresh,
        state = pullToRefreshState,
        indicator = {
            PullToRefreshDefaults.Indicator(
                state = pullToRefreshState,
                isRefreshing = isRefreshing,
                containerColor = MaterialTheme.colorScheme.primaryContainer,
                color = MaterialTheme.colorScheme.primary,
                modifier = Modifier.align(Alignment.TopCenter)
            )
        },
        modifier = Modifier
            .fillMaxSize()
            .testTag("events_pull_to_refresh")
    ) {
        LazyColumn(
            modifier = Modifier
                .fillMaxSize()
                .testTag("events_list"),
            contentPadding = PaddingValues(start = 16.dp, end = 16.dp, top = 16.dp, bottom = 88.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            item {
                Card(
                    modifier = Modifier.fillMaxWidth(),
                    shape = MaterialTheme.shapes.large,
                    colors = CardDefaults.cardColors(
                        containerColor = MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.55f)
                    )
                ) {
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(16.dp),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Column(modifier = Modifier.weight(1f), verticalArrangement = Arrangement.spacedBy(4.dp)) {
                            Text(
                                text = if (currentLanguage == AppLanguage.HINDI)
                                    "$activeCity के आगामी कार्यक्रम"
                                else
                                    "Upcoming Events in $activeCity",
                                style = MaterialTheme.typography.titleMedium,
                                fontWeight = FontWeight.Bold,
                                color = MaterialTheme.colorScheme.onPrimaryContainer
                            )
                            Text(
                                text = if (currentLanguage == AppLanguage.HINDI)
                                    "स्थानीय मीटअप, वर्कशॉप और सप्ताहांत कार्यक्रम खोजें"
                                else
                                    "Discover neighborhood meetups, book clubs, workshops & weekend jams",
                                style = MaterialTheme.typography.bodySmall,
                                color = MaterialTheme.colorScheme.onPrimaryContainer.copy(alpha = 0.85f)
                            )
                        }
                        FilledTonalButton(
                            onClick = onNavigateToCreate,
                            modifier = Modifier.testTag("events_host_button")
                        ) {
                            Icon(Icons.Rounded.Add, contentDescription = null, modifier = Modifier.size(16.dp))
                            Text(if (currentLanguage == AppLanguage.HINDI) " होस्ट करें" else " Host")
                        }
                    }
                }
            }

            item {
                SingleChoiceSegmentedButtonRow(
                    modifier = Modifier
                        .fillMaxWidth()
                        .testTag("events_view_mode_toggle")
                ) {
                    SegmentedButton(
                        selected = viewMode == EventsViewMode.LIST,
                        onClick = { viewMode = EventsViewMode.LIST },
                        shape = SegmentedButtonDefaults.itemShape(index = 0, count = 2),
                        icon = {
                            Icon(
                                imageVector = Icons.AutoMirrored.Rounded.ViewList,
                                contentDescription = null,
                                modifier = Modifier.size(18.dp)
                            )
                        },
                        modifier = Modifier.testTag("events_view_toggle_list")
                    ) {
                        Text(if (currentLanguage == AppLanguage.HINDI) "सूची दृश्य" else "List View")
                    }
                    SegmentedButton(
                        selected = viewMode == EventsViewMode.CALENDAR,
                        onClick = { viewMode = EventsViewMode.CALENDAR },
                        shape = SegmentedButtonDefaults.itemShape(index = 1, count = 2),
                        icon = {
                            Icon(
                                imageVector = Icons.Rounded.CalendarMonth,
                                contentDescription = null,
                                modifier = Modifier.size(18.dp)
                            )
                        },
                        modifier = Modifier.testTag("events_view_toggle_calendar")
                    ) {
                        Text(if (currentLanguage == AppLanguage.HINDI) "कैलेंडर दृश्य" else "Calendar View")
                    }
                }
            }

            item {
                OutlinedTextField(
                    value = searchQuery,
                    onValueChange = { searchQuery = it },
                    placeholder = {
                        Text(
                            if (currentLanguage == AppLanguage.HINDI)
                                "कार्यक्रम, स्थान या आयोजक खोजें..."
                            else
                                "Search events, venues, or organizers..."
                        )
                    },
                    leadingIcon = { Icon(Icons.Rounded.Search, contentDescription = null) },
                    singleLine = true,
                    shape = RoundedCornerShape(16.dp),
                    modifier = Modifier
                        .fillMaxWidth()
                        .testTag("events_search_input")
                )
            }

            item {
                val filters = listOf(
                    "all" to if (currentLanguage == AppLanguage.HINDI) "सभी" else "All",
                    "going" to if (currentLanguage == AppLanguage.HINDI) "मेरे RSVP" else "Going",
                    "free" to if (currentLanguage == AppLanguage.HINDI) "निःशुल्क" else "Free",
                    "paid" to if (currentLanguage == AppLanguage.HINDI) "सशुल्क" else "Ticketed"
                )
                LazyRow(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    items(filters) { (key, label) ->
                        FilterChip(
                            selected = filterMode == key,
                            onClick = { filterMode = key },
                            label = { Text(label) },
                            modifier = Modifier.testTag("events_filter_$key")
                        )
                    }
                }
            }

            when (eventsState) {
                is UiState.Loading -> items(3) { SkeletonPostCard() }
                is UiState.Error -> item {
                    EmptyStateCard(
                        icon = Icons.Rounded.Event,
                        title = "Events unavailable",
                        subtitle = eventsState.message,
                        ctaLabel = "Retry",
                        onCtaClick = onRefresh
                    )
                }
                is UiState.Success -> {
                    val filtered = eventsState.data.filter { evt ->
                        val matchesSearch = searchQuery.isBlank() ||
                            evt.title.contains(searchQuery, ignoreCase = true) ||
                            evt.venue.contains(searchQuery, ignoreCase = true) ||
                            evt.category.contains(searchQuery, ignoreCase = true) ||
                            evt.organizerName.contains(searchQuery, ignoreCase = true)
                        val matchesFilter = when (filterMode) {
                            "going" -> evt.id in rsvpedEventIds
                            "free" -> evt.priceInr <= 0L
                            "paid" -> evt.priceInr > 0L
                            else -> true
                        }
                        matchesSearch && matchesFilter
                    }

                    if (viewMode == EventsViewMode.CALENDAR) {
                        val eventsByDay = filtered.groupBy { evt ->
                            resolveEventCalendarDay(evt).day
                        }
                        val monthEvents = eventsByDay.entries
                            .filter { (day, _) -> day.toMonth() == displayedMonth }
                            .sortedBy { it.key }
                            .flatMap { it.value }
                        val agendaEvents = if (selectedDay != null) {
                            eventsByDay[selectedDay].orEmpty()
                        } else {
                            monthEvents
                        }

                        item {
                            EventsCalendarCard(
                                displayedMonth = displayedMonth,
                                selectedDay = selectedDay,
                                today = today,
                                eventsByDay = eventsByDay,
                                rsvpedEventIds = rsvpedEventIds,
                                currentLanguage = currentLanguage,
                                onPreviousMonth = {
                                    val prev = displayedMonth.previousMonth()
                                    setDisplayedMonth(prev)
                                    setSelectedDay(null)
                                },
                                onNextMonth = {
                                    val next = displayedMonth.nextMonth()
                                    setDisplayedMonth(next)
                                    setSelectedDay(null)
                                },
                                onJumpToToday = {
                                    setDisplayedMonth(today.toMonth())
                                    setSelectedDay(today)
                                },
                                onSelectDay = { tappedDay ->
                                    if (selectedDay == tappedDay) {
                                        setSelectedDay(null)
                                    } else {
                                        setSelectedDay(tappedDay)
                                    }
                                }
                            )
                        }

                        item {
                            Row(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .padding(top = 4.dp)
                                    .testTag("events_calendar_agenda_header"),
                                horizontalArrangement = Arrangement.SpaceBetween,
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                val headerText = if (selectedDay != null) {
                                    val dateStr = formatSelectedDayHeader(selectedDay, currentLanguage)
                                    if (currentLanguage == AppLanguage.HINDI) {
                                        "$dateStr • ${agendaEvents.size} कार्यक्रम"
                                    } else {
                                        val suffix = if (agendaEvents.size == 1) "event" else "events"
                                        "$dateStr • ${agendaEvents.size} $suffix"
                                    }
                                } else {
                                    val monthTitle = displayedMonth.formatTitle(currentLanguage)
                                    if (currentLanguage == AppLanguage.HINDI) {
                                        "$monthTitle के सभी कार्यक्रम (${agendaEvents.size})"
                                    } else {
                                        "All in $monthTitle (${agendaEvents.size})"
                                    }
                                }
                                Text(
                                    text = headerText,
                                    style = MaterialTheme.typography.titleSmall,
                                    fontWeight = FontWeight.Bold,
                                    color = MaterialTheme.colorScheme.onSurface,
                                    modifier = Modifier.weight(1f)
                                )
                                if (selectedDay != null) {
                                    TextButton(
                                        onClick = { setSelectedDay(null) },
                                        modifier = Modifier.testTag("calendar_show_all_month")
                                    ) {
                                        Text(if (currentLanguage == AppLanguage.HINDI) "पूरा महीना" else "Show all month")
                                    }
                                }
                            }
                        }

                        if (agendaEvents.isEmpty()) {
                            val nextEventDay = eventsByDay.keys
                                .filter { it.toMonth() == displayedMonth }
                                .sorted()
                                .firstOrNull { selectedDay == null || it != selectedDay }

                            item {
                                EmptyStateCard(
                                    icon = Icons.Rounded.Event,
                                    title = if (selectedDay != null) {
                                        if (currentLanguage == AppLanguage.HINDI)
                                            "${formatShortDayLabel(selectedDay, currentLanguage)} को कोई कार्यक्रम नहीं"
                                        else
                                            "No events on ${formatShortDayLabel(selectedDay, currentLanguage)}"
                                    } else {
                                        if (currentLanguage == AppLanguage.HINDI)
                                            "${displayedMonth.formatTitle(currentLanguage)} में कोई कार्यक्रम नहीं"
                                        else
                                            "No events in ${displayedMonth.formatTitle(currentLanguage)}"
                                    },
                                    subtitle = if (nextEventDay != null) {
                                        if (currentLanguage == AppLanguage.HINDI)
                                            "इस महीने ${formatShortDayLabel(nextEventDay, currentLanguage)} को कार्यक्रम उपलब्ध हैं।"
                                        else
                                            "There are events scheduled on ${formatShortDayLabel(nextEventDay, currentLanguage)}."
                                    } else {
                                        if (currentLanguage == AppLanguage.HINDI)
                                            "इस तारीख़ पर पहला स्थानीय मीटअप होस्ट करें।"
                                        else
                                            "Host a neighborhood meetup or pick another month."
                                    },
                                    ctaLabel = if (nextEventDay != null) {
                                        if (currentLanguage == AppLanguage.HINDI)
                                            "${formatShortDayLabel(nextEventDay, currentLanguage)} पर जाएँ"
                                        else
                                            "View ${formatShortDayLabel(nextEventDay, currentLanguage)}"
                                    } else {
                                        if (currentLanguage == AppLanguage.HINDI) "होस्ट करें" else "Host Event"
                                    },
                                    onCtaClick = {
                                        if (nextEventDay != null) {
                                            setSelectedDay(nextEventDay)
                                        } else {
                                            onNavigateToCreate()
                                        }
                                    }
                                )
                            }
                        } else {
                            items(agendaEvents, key = { it.id }) { evt ->
                                EventCard(
                                    evt = evt,
                                    isGoing = evt.id in rsvpedEventIds,
                                    isRsvpPending = evt.id in pendingRsvpIds,
                                    isSaved = evt.id in savedTargetIds,
                                    isOwnEvent = evt.organizerId == profile.userId,
                                    currentLanguage = currentLanguage,
                                    onToggleRsvp = { onToggleEventRsvp(evt) },
                                    onSave = { onSaveEvent(evt) },
                                    onMessageOrganizer = {
                                        onMessageOrganizer(
                                            evt.organizerId,
                                            evt.organizerName,
                                            "Event: ${evt.title}"
                                        )
                                    }
                                )
                            }
                        }
                    } else if (filtered.isEmpty()) {
                        item {
                            EmptyStateCard(
                                icon = Icons.Rounded.Event,
                                title = "No upcoming events in $activeCity",
                                subtitle = "Host a weekend chai meetup, book swap, or neighborhood run.",
                                ctaLabel = "Host Event",
                                onCtaClick = onNavigateToCreate
                            )
                        }
                    } else {
                        items(filtered, key = { it.id }) { evt ->
                            EventCard(
                                evt = evt,
                                isGoing = evt.id in rsvpedEventIds,
                                isRsvpPending = evt.id in pendingRsvpIds,
                                isSaved = evt.id in savedTargetIds,
                                isOwnEvent = evt.organizerId == profile.userId,
                                currentLanguage = currentLanguage,
                                onToggleRsvp = { onToggleEventRsvp(evt) },
                                onSave = { onSaveEvent(evt) },
                                onMessageOrganizer = {
                                    onMessageOrganizer(
                                        evt.organizerId,
                                        evt.organizerName,
                                        "Event: ${evt.title}"
                                    )
                                }
                            )
                        }
                    }
                }
            }
        }
    }
}


