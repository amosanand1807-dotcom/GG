package com.example

import com.example.core.CalendarDay
import com.example.core.CalendarMonth
import com.example.core.resolveEventCalendarDay
import com.example.models.CityEvent
import com.google.firebase.Timestamp
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test
import java.util.Calendar
import java.util.Date

class EventDateUtilsTest {

    private fun dateOf(year: Int, month: Int, day: Int): Date {
        return Calendar.getInstance().apply {
            clear()
            set(Calendar.YEAR, year)
            set(Calendar.MONTH, month - 1)
            set(Calendar.DAY_OF_MONTH, day)
            set(Calendar.HOUR_OF_DAY, 10)
        }.time
    }

    @Test
    fun calendarMonthNavigationAndLeapYearWork() {
        val jan = CalendarMonth(2026, 1)
        assertEquals(CalendarMonth(2025, 12), jan.previousMonth())
        assertEquals(CalendarMonth(2026, 2), jan.nextMonth())

        val dec = CalendarMonth(2026, 12)
        assertEquals(CalendarMonth(2027, 1), dec.nextMonth())

        assertEquals(28, CalendarMonth(2026, 2).daysInMonth())
        assertEquals(29, CalendarMonth(2024, 2).daysInMonth())
        assertEquals(30, CalendarMonth(2026, 11).daysInMonth())
    }

    @Test
    fun calendarGridCellsAlignNovember2026ToSundayStart() {
        // Nov 1, 2026 is a Sunday -> offset 0, 30 days -> 35 cells (5 rows)
        val nov2026 = CalendarMonth(2026, 11)
        val cells = nov2026.calendarGridCells()
        assertEquals(0, nov2026.firstDayOfWeekOffset())
        assertEquals(35, cells.size)
        assertEquals(CalendarDay(2026, 11, 1), cells.first())
        assertEquals(CalendarDay(2026, 11, 30), cells[29])
        assertEquals(null, cells.last())
    }

    @Test
    fun resolvesExplicitIsoAndNamedDates() {
        val ref = dateOf(2026, 11, 17) // Tuesday

        val isoEvent = CityEvent(id = "1", dateLabel = "2026-12-05 • 7:00 PM")
        val isoRes = resolveEventCalendarDay(isoEvent, ref)
        assertEquals(CalendarDay(2026, 12, 5), isoRes.day)
        assertTrue(isoRes.isExactDate)

        val namedEvent = CityEvent(id = "2", dateLabel = "Sat, 24 Nov • 6:30 PM")
        val namedRes = resolveEventCalendarDay(namedEvent, ref)
        assertEquals(CalendarDay(2026, 11, 24), namedRes.day)
        assertTrue(namedRes.isExactDate)

        val monthFirstEvent = CityEvent(id = "3", dateLabel = "December 12, 2026")
        val monthFirstRes = resolveEventCalendarDay(monthFirstEvent, ref)
        assertEquals(CalendarDay(2026, 12, 12), monthFirstRes.day)
        assertTrue(monthFirstRes.isExactDate)

        val dmyEvent = CityEvent(id = "4", dateLabel = "28/11/2026 at 5 PM")
        val dmyRes = resolveEventCalendarDay(dmyEvent, ref)
        assertEquals(CalendarDay(2026, 11, 28), dmyRes.day)
        assertTrue(dmyRes.isExactDate)
    }

    @Test
    fun resolvesWeekdayAndRelativeDatesFromAnchor() {
        // Nov 17, 2026 is Tuesday
        val tuesdayAnchor = Timestamp(dateOf(2026, 11, 17))

        val saturdayEvent = CityEvent(id = "sat", dateLabel = "Sat, 6:00 PM", createdAt = tuesdayAnchor)
        assertEquals(CalendarDay(2026, 11, 21), resolveEventCalendarDay(saturdayEvent).day)

        val sundayEvent = CityEvent(id = "sun", dateLabel = "Sun, 8:30 AM", createdAt = tuesdayAnchor)
        assertEquals(CalendarDay(2026, 11, 22), resolveEventCalendarDay(sundayEvent).day)

        val tomorrowEvent = CityEvent(id = "tmw", dateLabel = "Tomorrow • 5 PM", createdAt = tuesdayAnchor)
        assertEquals(CalendarDay(2026, 11, 18), resolveEventCalendarDay(tomorrowEvent).day)

        val weekendEvent = CityEvent(id = "wkd", dateLabel = "This Weekend", createdAt = tuesdayAnchor)
        val wkdRes = resolveEventCalendarDay(weekendEvent)
        assertEquals(CalendarDay(2026, 11, 21), wkdRes.day)
        assertFalse(wkdRes.isExactDate)

        val tbaEvent = CityEvent(id = "tba", dateLabel = "Date & time TBA", createdAt = tuesdayAnchor)
        val tbaRes = resolveEventCalendarDay(tbaEvent)
        assertEquals(CalendarDay(2026, 11, 17), tbaRes.day)
        assertFalse(tbaRes.isExactDate)
    }
}
