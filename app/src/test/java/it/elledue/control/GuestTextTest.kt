package it.elledue.control

import it.elledue.control.domain.*
import org.junit.Test
import org.junit.Assert.*
import java.time.LocalDate

class GuestTextTest {
    private fun booking(id:String="b",property:String="p",start:String="29/12/2026",end:String="03/01/2027")=BookingDraft(id=id,propertyId=property,firstName="Nicolò",lastName="D’Amico",guests="1",checkIn=start,checkOut=end,gross="1000").toRecord(null,false)
    private val jan=LocalDate.of(2027,1,1)
    @Test fun overlapIncludesPreviousMonthAndYearButNotCancelledOrOtherProperty() {
        val b=booking();val rows=CalendarText.bookings("p",jan,jan.plusDays(30),listOf(b,b.copy(id="cancelled",status="CANCELLED"),b.copy(id="other",propertyId="q")))
        assertEquals(listOf(b),rows)
        assertEquals("CASA DELL’ETNA\n\nNicolò D’Amico (1 ospite)\nCheck-in 29/12/2026 ore 15.00\nCheck-out 03/01/2027 ore 10.00",CalendarText.render("Casa dell’Etna",rows))
    }
    @Test fun emptyPeriodHasNoInventedBookingAndTextsNeverContainAccounting() {
        assertEquals("",CalendarText.render("Casa",emptyList()))
        val text=CalendarText.render("Casa",listOf(booking()))
        listOf("1000","195","161","commission","telefono","document").forEach {assertFalse(text.contains(it))}
    }
    @Test fun independentPropertiesNeverMixGuests() {
        val a=booking();val b=booking(property="q").copy(guestFirstName="Altro")
        val text=CalendarText.render("Casa",CalendarText.bookings("p",jan,jan.plusDays(2),listOf(a,b)))
        assertFalse(text.contains("Altro"))
    }
    @Test fun stableChronologicalOrderAndTurnoverIncludesBothStaysWhenTheyOverlapPeriod() {
        val a=booking("a",start="01/01/2027",end="03/01/2027");val b=booking("b",start="03/01/2027",end="05/01/2027")
        assertEquals(listOf(a,b),CalendarText.bookings("p",jan,jan.plusDays(4),listOf(b,a)))
        assertEquals(listOf(b),CalendarText.bookings("p",jan.plusDays(2),jan.plusDays(2),listOf(b,a)))
        assertEquals(listOf(a,a.copy(id="z")),CalendarText.bookings("p",jan,jan,listOf(a.copy(id="z"),a)))
    }
    @Test fun missingTimesAccentsAndMultipleGuests() {
        val text=CalendarText.render("Cà d’Oro",listOf(booking().copy(guestCount=4,expectedArrivalTime="",expectedDepartureTime="")))
        assertTrue(text.contains("CÀ D’ORO"));assertTrue(text.contains("(4 ospiti)"));assertTrue(text.contains("Check-in 29/12/2026 orario non indicato"))
    }
    @Test(expected=IllegalArgumentException::class) fun invalidCustomRangeIsRejected(){CalendarText.bookings("p",jan.plusDays(2),jan,emptyList())}
    @Test fun inclusiveCustomEndKeepsRealDatesWithoutTruncation(){val b=booking();assertEquals(listOf(b),CalendarText.bookings("p",LocalDate.of(2026,12,29),LocalDate.of(2026,12,29),listOf(b)))}
    @Test fun englishAndItalianVariablesResolveWithoutInventedValues(){
        val resolved=GuestText.resolve("Hi {nome_ospite}, {{immobile}}: {check_in}, {ora_check_out}; {tassa_residua}",booking(),"Casa Etna",1200,true)
        assertTrue(resolved.missing.isEmpty());assertTrue(resolved.text.contains("Nicolò"));assertTrue(resolved.text.contains("29/12/2026"));assertTrue(resolved.text.contains(Amounts.euro(1200)))
        val missing=GuestText.resolve("{nome_ospite} {tassa_residua} {codice_accesso}",null,null,null)
        assertEquals(setOf("nome_ospite","tassa_residua","codice_accesso"),missing.missing);assertTrue(missing.text.contains("[manca: codice_accesso]"))
    }
}
