package dev.nerzouille.koan.ui.kit

import org.junit.Assert.assertEquals
import org.junit.Test
import java.time.LocalDate
import java.time.ZoneId

class DaysTest {
    private val today = LocalDate.of(2026, 10, 19)
    private fun at(date: LocalDate, hour: Int) = date.atTime(hour, 0).atZone(ZoneId.systemDefault()).toInstant().toEpochMilli()

    @Test fun `labels today, yesterday, then dates`() {
        assertEquals("Today", dayLabel(today, today))
        assertEquals("Yesterday", dayLabel(today.minusDays(1), today))
        assertEquals("Monday 12 October", dayLabel(LocalDate.of(2026, 10, 12), today))
        assertEquals("24 December 2025", dayLabel(LocalDate.of(2025, 12, 24), today))
    }

    @Test fun `groups by day, newest day and newest item first`() {
        val items = listOf("a" to at(today, 8), "b" to at(today.minusDays(3), 20), "c" to at(today, 9))
        val days = byDay(items, { it.second }, today)
        assertEquals(listOf("Today", "Friday 16 October"), days.map { it.first })
        assertEquals(listOf("c", "a"), days.first().second.map { it.first })
    }
}
