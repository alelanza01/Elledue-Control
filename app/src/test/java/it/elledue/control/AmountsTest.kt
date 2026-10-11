package it.elledue.control

import it.elledue.control.domain.Amounts
import org.junit.Assert.*
import org.junit.Test

class AmountsTest {
    @Test fun briefCommissionExampleIsExact() {
        val gross = Amounts.parseCents("1000")
        val ota = Amounts.percentage(gross, Amounts.parseBasisPoints("19,5"))
        val net = gross - ota
        val fee = Amounts.percentage(net, Amounts.parseBasisPoints("20"))
        assertEquals(19_500L, ota)
        assertEquals(80_500L, net)
        assertEquals(16_100L, fee)
    }
    @Test fun halfCentRoundsUp() { assertEquals(1L, Amounts.percentage(5, 1000)) }
    @Test fun decimalParsingKeepsCentsExact() {
        assertEquals(12345L, Amounts.parseCents("123,45"))
        assertEquals(12345L, Amounts.parseCents("123.45"))
        assertEquals("123,45", Amounts.editable(12345))
    }
    @Test fun invalidInputsAreRejected() {
        listOf("-1", "1,001", "1e3", "NaN", "1.000,00", "").forEach { input ->
            assertThrows(IllegalArgumentException::class.java) { Amounts.parseCents(input) }
        }
        assertThrows(IllegalArgumentException::class.java) { Amounts.parseBasisPoints("100,01") }
        assertThrows(ArithmeticException::class.java) { Amounts.parseCents("999999999999999999999999") }
    }
}
