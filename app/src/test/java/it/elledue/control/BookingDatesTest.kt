package it.elledue.control

import it.elledue.control.domain.*
import org.junit.Assert.*
import org.junit.Test
import java.time.*

class BookingDatesTest {
    @Test fun impossibleDatesAreNotNormalized() {
        listOf("31/06/2026", "29/02/2026", "2026-06-10", "").forEach { assertThrows(IllegalArgumentException::class.java) { BookingDates.parse(it) } }
        assertEquals(LocalDate.of(2028, 2, 29), BookingDates.parse("29/02/2028"))
    }
    @Test fun sameDayTurnoverDoesNotOverlapButActualIntersectionsDo() {
        assertFalse(BookingDates.overlaps(10, 12, 12, 15))
        assertTrue(BookingDates.overlaps(10, 12, 11, 15))
        assertTrue(BookingDates.overlaps(10, 15, 11, 12))
    }
    @Test fun datePickerUsesUtcDateWithoutRomeOffsetDriftAcrossDst() {
        listOf(LocalDate.of(2026, 3, 29), LocalDate.of(2026, 10, 25)).forEach { date -> assertEquals(date, BookingDates.fromPicker(BookingDates.pickerMillis(date))) }
    }
    @Test fun todayUsesRomeRatherThanHostUtc() {
        assertEquals(LocalDate.of(2026, 7, 1), BookingDates.today(Clock.fixed(Instant.parse("2026-06-30T22:30:00Z"), ZoneOffset.UTC)))
    }
}
