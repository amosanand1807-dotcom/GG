package com.example.core

import com.example.models.CityEvent
import java.text.SimpleDateFormat
import java.util.Calendar
import java.util.Date
import java.util.Locale

data class CalendarDay(
    val year: Int,
    val month: Int, // 1..12
    val dayOfMonth: Int
) : Comparable<CalendarDay> {
    override fun compareTo(other: CalendarDay): Int {
        if (year != other.year) return year.compareTo(other.year)
        if (month != other.month) return month.compareTo(other.month)
        return dayOfMonth.compareTo(other.dayOfMonth)
    }

    fun toMonth(): CalendarMonth = CalendarMonth(year, month)

    fun toCalendar(): Calendar = Calendar.getInstance().apply {
        clear()
        set(Calendar.YEAR, year)
        set(Calendar.MONTH, month - 1)
        set(Calendar.DAY_OF_MONTH, dayOfMonth)
    }
}

data class CalendarMonth(
    val year: Int,
    val month: Int // 1..12
) : Comparable<CalendarMonth> {
    override fun compareTo(other: CalendarMonth): Int {
        if (year != other.year) return year.compareTo(other.year)
        return month.compareTo(other.month)
    }

    fun previousMonth(): CalendarMonth =
        if (month == 1) CalendarMonth(year - 1, 12) else CalendarMonth(year, month - 1)

    fun nextMonth(): CalendarMonth =
        if (month == 12) CalendarMonth(year + 1, 1) else CalendarMonth(year, month + 1)

    fun daysInMonth(): Int {
        val cal = Calendar.getInstance().apply {
            clear()
            set(Calendar.YEAR, year)
            set(Calendar.MONTH, month - 1)
            set(Calendar.DAY_OF_MONTH, 1)
        }
        return cal.getActualMaximum(Calendar.DAY_OF_MONTH)
    }

    /** 0 = Sunday .. 6 = Saturday */
    fun firstDayOfWeekOffset(): Int {
        val cal = Calendar.getInstance().apply {
            clear()
            set(Calendar.YEAR, year)
            set(Calendar.MONTH, month - 1)
            set(Calendar.DAY_OF_MONTH, 1)
        }
        return cal.get(Calendar.DAY_OF_WEEK) - Calendar.SUNDAY
    }

    fun calendarGridCells(): List<CalendarDay?> {
        val leadingEmpty = firstDayOfWeekOffset()
        val totalDays = daysInMonth()
        val totalCells = ((leadingEmpty + totalDays + 6) / 7) * 7
        return List(totalCells) { index ->
            val dayNum = index - leadingEmpty + 1
            if (dayNum in 1..totalDays) CalendarDay(year, month, dayNum) else null
        }
    }

    fun formatTitle(language: AppLanguage): String {
        val locale = appLocale(language)
        val cal = Calendar.getInstance().apply {
            clear()
            set(Calendar.YEAR, year)
            set(Calendar.MONTH, month - 1)
            set(Calendar.DAY_OF_MONTH, 1)
        }
        return SimpleDateFormat("MMMM yyyy", locale).format(cal.time)
    }
}

data class ResolvedEventDate(
    val day: CalendarDay,
    val isExactDate: Boolean
)

private fun appLocale(language: AppLanguage): Locale =
    if (language == AppLanguage.HINDI) Locale.forLanguageTag("hi-IN") else Locale.forLanguageTag("en-IN")

fun currentCalendarDay(now: Date = Date()): CalendarDay {
    val cal = Calendar.getInstance().apply { time = now }
    return CalendarDay(
        year = cal.get(Calendar.YEAR),
        month = cal.get(Calendar.MONTH) + 1,
        dayOfMonth = cal.get(Calendar.DAY_OF_MONTH)
    )
}

fun formatSelectedDayHeader(day: CalendarDay, language: AppLanguage): String {
    return SimpleDateFormat("EEE, d MMM yyyy", appLocale(language)).format(day.toCalendar().time)
}

fun formatShortDayLabel(day: CalendarDay, language: AppLanguage): String {
    return SimpleDateFormat("d MMM", appLocale(language)).format(day.toCalendar().time)
}

private val MONTH_NAME_MAP = mapOf(
    "jan" to 1, "january" to 1,
    "feb" to 2, "february" to 2,
    "mar" to 3, "march" to 3,
    "apr" to 4, "april" to 4,
    "may" to 5,
    "jun" to 6, "june" to 6,
    "jul" to 7, "july" to 7,
    "aug" to 8, "august" to 8,
    "sep" to 9, "sept" to 9, "september" to 9,
    "oct" to 10, "october" to 10,
    "nov" to 11, "november" to 11,
    "dec" to 12, "december" to 12
)

private val WEEKDAY_PATTERNS = listOf(
    Calendar.SUNDAY to Regex("\\b(sun|sunday)\\b", RegexOption.IGNORE_CASE),
    Calendar.MONDAY to Regex("\\b(mon|monday)\\b", RegexOption.IGNORE_CASE),
    Calendar.TUESDAY to Regex("\\b(tue|tues|tuesday)\\b", RegexOption.IGNORE_CASE),
    Calendar.WEDNESDAY to Regex("\\b(wed|wednesday)\\b", RegexOption.IGNORE_CASE),
    Calendar.THURSDAY to Regex("\\b(thu|thur|thurs|thursday)\\b", RegexOption.IGNORE_CASE),
    Calendar.FRIDAY to Regex("\\b(fri|friday)\\b", RegexOption.IGNORE_CASE),
    Calendar.SATURDAY to Regex("\\b(sat|saturday)\\b", RegexOption.IGNORE_CASE)
)

private val ISO_DATE_REGEX = Regex("\\b(20\\d{2})[-/](0?[1-9]|1[0-2])[-/](0?[1-9]|[12]\\d|3[01])\\b")
private val DMY_DATE_REGEX = Regex("\\b(0?[1-9]|[12]\\d|3[01])[-/](0?[1-9]|1[0-2])(?:[-/](20\\d{2}))?\\b")
private val DAY_MONTH_NAME_REGEX = Regex(
    "\\b(0?[1-9]|[12]\\d|3[01])(?:st|nd|rd|th)?\\s+(jan(?:uary)?|feb(?:ruary)?|mar(?:ch)?|apr(?:il)?|may|jun(?:e)?|jul(?:y)?|aug(?:ust)?|sep(?:t|tember)?|oct(?:ober)?|nov(?:ember)?|dec(?:ember)?)(?:,?\\s*(20\\d{2}))?\\b",
    RegexOption.IGNORE_CASE
)
private val MONTH_NAME_DAY_REGEX = Regex(
    "\\b(jan(?:uary)?|feb(?:ruary)?|mar(?:ch)?|apr(?:il)?|may|jun(?:e)?|jul(?:y)?|aug(?:ust)?|sep(?:t|tember)?|oct(?:ober)?|nov(?:ember)?|dec(?:ember)?)\\s+(0?[1-9]|[12]\\d|3[01])(?:st|nd|rd|th)?(?:,?\\s*(20\\d{2}))?\\b",
    RegexOption.IGNORE_CASE
)

fun resolveEventCalendarDay(
    event: CityEvent,
    defaultReferenceDate: Date = Date()
): ResolvedEventDate {
    val anchorDate = event.createdAt?.toDate() ?: defaultReferenceDate
    val anchorCal = Calendar.getInstance().apply { time = anchorDate }
    val anchorYear = anchorCal.get(Calendar.YEAR)
    val anchorMonth = anchorCal.get(Calendar.MONTH) + 1
    val label = event.dateLabel.trim()

    // 1. ISO yyyy-MM-dd
    ISO_DATE_REGEX.find(label)?.let { match ->
        val year = match.groupValues[1].toIntOrNull()
        val month = match.groupValues[2].toIntOrNull()
        val day = match.groupValues[3].toIntOrNull()
        if (year != null && month != null && day != null) {
            safeCalendarDay(year, month, day)?.let {
                return ResolvedEventDate(it, isExactDate = true)
            }
        }
    }

    // 2. Day + Month name (e.g., "24 Nov", "24 Nov 2026")
    DAY_MONTH_NAME_REGEX.find(label)?.let { match ->
        val day = match.groupValues[1].toIntOrNull()
        val month = MONTH_NAME_MAP[match.groupValues[2].lowercase(Locale.ENGLISH)]
        val explicitYear = match.groupValues[3].toIntOrNull()
        if (day != null && month != null) {
            val year = explicitYear ?: inferYearForMonth(anchorYear, anchorMonth, month)
            safeCalendarDay(year, month, day)?.let {
                return ResolvedEventDate(it, isExactDate = true)
            }
        }
    }

    // 3. Month name + Day (e.g., "Nov 24", "November 24, 2026")
    MONTH_NAME_DAY_REGEX.find(label)?.let { match ->
        val month = MONTH_NAME_MAP[match.groupValues[1].lowercase(Locale.ENGLISH)]
        val day = match.groupValues[2].toIntOrNull()
        val explicitYear = match.groupValues[3].toIntOrNull()
        if (day != null && month != null) {
            val year = explicitYear ?: inferYearForMonth(anchorYear, anchorMonth, month)
            safeCalendarDay(year, month, day)?.let {
                return ResolvedEventDate(it, isExactDate = true)
            }
        }
    }

    // 4. Numeric dd/MM/yyyy or dd/MM
    DMY_DATE_REGEX.find(label)?.let { match ->
        val day = match.groupValues[1].toIntOrNull()
        val month = match.groupValues[2].toIntOrNull()
        val explicitYear = match.groupValues[3].toIntOrNull()
        if (day != null && month != null) {
            val year = explicitYear ?: inferYearForMonth(anchorYear, anchorMonth, month)
            safeCalendarDay(year, month, day)?.let {
                return ResolvedEventDate(it, isExactDate = true)
            }
        }
    }

    // 5. Relative keywords ("today", "tomorrow", "weekend")
    val lower = label.lowercase(Locale.ENGLISH)
    if (lower.contains("today") || label.contains("आज")) {
        return ResolvedEventDate(currentCalendarDay(anchorDate), isExactDate = true)
    }
    if (lower.contains("tomorrow") || label.contains("कल")) {
        val nextCal = (anchorCal.clone() as Calendar).apply { add(Calendar.DAY_OF_YEAR, 1) }
        return ResolvedEventDate(currentCalendarDay(nextCal.time), isExactDate = true)
    }

    // 6. Weekday token (e.g., "Sat, 6:00 PM", "Sun, 8:30 AM")
    for ((targetDow, regex) in WEEKDAY_PATTERNS) {
        if (regex.containsMatchIn(label)) {
            val currentDow = anchorCal.get(Calendar.DAY_OF_WEEK)
            val delta = (targetDow - currentDow + 7) % 7
            val resolved = (anchorCal.clone() as Calendar).apply { add(Calendar.DAY_OF_YEAR, delta) }
            return ResolvedEventDate(currentCalendarDay(resolved.time), isExactDate = true)
        }
    }

    if (lower.contains("weekend") || label.contains("सप्ताहांत")) {
        val currentDow = anchorCal.get(Calendar.DAY_OF_WEEK)
        val delta = (Calendar.SATURDAY - currentDow + 7) % 7
        val resolved = (anchorCal.clone() as Calendar).apply { add(Calendar.DAY_OF_YEAR, delta) }
        return ResolvedEventDate(currentCalendarDay(resolved.time), isExactDate = false)
    }

    // 7. Fallback to creation/reference day so all events remain browsable on calendar
    return ResolvedEventDate(currentCalendarDay(anchorDate), isExactDate = false)
}

private fun inferYearForMonth(anchorYear: Int, anchorMonth: Int, targetMonth: Int): Int =
    if (targetMonth < anchorMonth - 6) anchorYear + 1 else anchorYear

private fun safeCalendarDay(year: Int, month: Int, dayOfMonth: Int): CalendarDay? {
    if (month !in 1..12) return null
    val maxDay = CalendarMonth(year, month).daysInMonth()
    if (dayOfMonth !in 1..maxDay) return null
    return CalendarDay(year, month, dayOfMonth)
}
