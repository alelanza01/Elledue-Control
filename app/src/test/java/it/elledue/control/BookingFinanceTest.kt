package it.elledue.control

import it.elledue.control.domain.*
import org.junit.Assert.*
import org.junit.Test

class BookingFinanceTest {
    @Test fun requiredExampleUsesNetAsFeeBase() { assertEquals(BookingTotals(19500, 80500, 16100), BookingFinance.calculate(100000, 1950, null, 2000)) }
    @Test fun actualOtaOverridesPercentageIncludingZero() {
        assertEquals(BookingTotals(15000, 85000, 17000), BookingFinance.calculate(100000, 1950, 15000, 2000))
        assertEquals(BookingTotals(0, 100000, 20000), BookingFinance.calculate(100000, 1950, 0, 2000))
    }
    @Test fun eachCommissionRoundsHalfUpToTheCent() { assertEquals(BookingTotals(1, 4, 1), BookingFinance.calculate(5, 1000, null, 1250)) }
    @Test fun zeroAndFullCommissionsAreValid() {
        assertEquals(BookingTotals(0, 0, 0), BookingFinance.calculate(0, 1950, null, 2000))
        assertEquals(BookingTotals(100, 0, 0), BookingFinance.calculate(100, 10000, null, 2000))
    }
    @Test fun outOfRangeAmountsAndPercentagesAreRejected() {
        listOf(-1L, 100001L).forEach { value -> assertThrows(IllegalArgumentException::class.java) { BookingFinance.calculate(100000, 1950, value, 2000) } }
        assertThrows(IllegalArgumentException::class.java) { BookingFinance.calculate(100, 10001, null, 2000) }
        assertThrows(IllegalArgumentException::class.java) { BookingFinance.calculate(100, 0, null, -1) }
    }
    @Test fun largestLongDoesNotOverflowDuringPercentageMultiplication() { assertEquals(Long.MAX_VALUE, BookingFinance.calculate(Long.MAX_VALUE, 0, null, 10000).managementCents) }
}
