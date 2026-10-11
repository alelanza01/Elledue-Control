package it.elledue.control

import it.elledue.control.domain.PropertyDraft
import org.junit.Assert.*
import org.junit.Test

class PropertyDraftTest {
    private val valid = PropertyDraft(name = "  Casa  ", address = "Via Test 1", ownerName = "Proprietario Test")
    @Test fun defaultsMatchOperationalBrief() {
        val p = valid.toRecord().property
        assertEquals("Casa", p.name)
        assertEquals(2000, p.managementBasisPoints)
        assertEquals(1950, p.otaBasisPoints)
        assertEquals(800L, p.cleaningHourlyCents)
        assertEquals(500L, p.laundrySetCents)
        assertEquals(300L, p.touristTaxNightCents)
        assertEquals(16, p.touristTaxMinAge)
        assertNull(p.touristTaxMaxNights)
    }
    @Test fun requiredAndInvalidFieldsAreRejected() {
        listOf(valid.copy(name = " "), valid.copy(address = ""), valid.copy(ownerName = ""),
            valid.copy(beds = "0"), valid.copy(maxGuests = "-1"), valid.copy(checkInTime = "25:00"),
            valid.copy(taxMaxNights = "zero"), valid.copy(taxMinAge = "-1"), valid.copy(managementPercent = "101"),
            valid.copy(platformRates = "Airbnb=15\nairbnb=12"), valid.copy(cleaningRate = "-8"),
        ).forEach { draft -> assertThrows(IllegalArgumentException::class.java) { draft.toRecord() } }
    }
    @Test fun completeDraftRoundTripsWithoutLosingRatesOrTemplates() {
        val draft = valid.copy(platformRates = "Booking.com=19,50\nAirbnb=15,00", accessItalian = "Istruzioni reali", accessEnglish = "Real instructions", taxMaxNights = "4")
        val record = draft.toRecord()
        val copy = PropertyDraft.from(record).toRecord(record.property)
        assertEquals(record.owner, copy.owner)
        assertEquals(record.property.copy(updatedAt = copy.property.updatedAt), copy.property)
        assertEquals(record.platformCommissions, copy.platformCommissions)
        assertEquals(record.templates, copy.templates)
    }
}
