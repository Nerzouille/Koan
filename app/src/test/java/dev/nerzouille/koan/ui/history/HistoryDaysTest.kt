package dev.nerzouille.koan.ui.history

import dev.nerzouille.koan.data.VisitEntity
import org.junit.Assert.assertEquals
import org.junit.Test
import java.time.LocalDate
import java.time.ZoneId

class HistoryDaysTest {
    private val today = LocalDate.of(2026, 10, 19)
    private fun at(date: LocalDate, hour: Int) = date.atTime(hour, 0).atZone(ZoneId.systemDefault()).toInstant().toEpochMilli()

    @Test fun `labels today, yesterday, then dates`() {
        assertEquals("Today", dayLabel(today, today))
        assertEquals("Yesterday", dayLabel(today.minusDays(1), today))
        assertEquals("Monday 12 October", dayLabel(LocalDate.of(2026, 10, 12), today))
        assertEquals("24 December 2025", dayLabel(LocalDate.of(2025, 12, 24), today))
    }

    @Test fun `groups by day, newest day first`() {
        val visits = listOf(
            VisitEntity("https://example.com/a", "A", at(today, 9), 1),
            VisitEntity("https://example.org/b", "B", at(today.minusDays(3), 20), 1),
            VisitEntity("https://example.net/c", "C", at(today, 8), 1, fromSync = true),
        )
        val days = byDay(visits, today)
        assertEquals(listOf("Today", "Friday 16 October"), days.map { it.first })
        assertEquals(listOf("A", "C"), days.first().second.map { it.title })
    }
}
