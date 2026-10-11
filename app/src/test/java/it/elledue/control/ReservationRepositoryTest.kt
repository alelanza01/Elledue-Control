package it.elledue.control

import android.content.Context
import androidx.room.Room
import androidx.test.core.app.ApplicationProvider
import it.elledue.control.data.*
import it.elledue.control.domain.*
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.runBlocking
import org.junit.*
import org.junit.Assert.*
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config

@RunWith(RobolectricTestRunner::class)
@Config(sdk = [34], application = android.app.Application::class)
class ReservationRepositoryTest {
    private lateinit var db: AppDatabase
    private lateinit var properties: PropertyRepository
    private lateinit var bookings: ReservationRepository
    private val property = PropertyDraft(name = "Casa Uno", address = "Via Test 1", ownerName = "Proprietario Uno", maxGuests = "4")
    private val other = property.copy(id = "property-two", ownerId = "owner-two", name = "Casa Due")
    private fun draft() = BookingDraft(propertyId = property.id, firstName = "Ospite", lastName = "Test", guests = "2", checkIn = "10/06/2026", checkOut = "12/06/2026", gross = "1000")
    @Before fun setup() = runBlocking {
        db = Room.inMemoryDatabaseBuilder(ApplicationProvider.getApplicationContext(), AppDatabase::class.java).build()
        properties = PropertyRepository(db); bookings = ReservationRepository(db)
        properties.save(property); properties.save(other)
    }
    @After fun close() { db.close() }

    @Test fun createEditAndCancelKeepIdAndEconomicHistory() = runBlocking {
        val draft = draft(); bookings.save(draft)
        val first = bookings.get(draft.id)!!
        assertEquals(16100L, first.managementCents)
        bookings.save(BookingDraft.from(first).copy(lastName = "Modificato", actualOta = "150", correctionReason = "OTA effettiva verificata", actualArrival = "16:30", notes = "Nota"))
        val updated = bookings.get(draft.id)!!
        assertEquals(17000L, updated.managementCents)
        assertEquals(first.createdAt, updated.createdAt)
        assertTrue(updated.updatedAt > first.updatedAt)
        bookings.cancel(draft.id)
        val cancelled = bookings.get(draft.id)!!
        assertEquals("CANCELLED", cancelled.status)
        assertEquals("16:30", cancelled.actualArrivalTime)
        assertEquals(17000L, cancelled.managementCents)
        assertEquals(1, bookings.reservations.first().size)
    }
    @Test fun savedBookingAndRatesPersistAfterDatabaseReopening() = runBlocking {
        val context = ApplicationProvider.getApplicationContext<Context>(); val file = "booking-persistence.db"
        context.deleteDatabase(file)
        val input = draft().copy(actualOta = "123,45", notes = "Persistente")
        try {
            Room.databaseBuilder(context, AppDatabase::class.java, file).build().withClosing { database ->
                PropertyRepository(database).save(property)
                ReservationRepository(database).save(input)
            }
            Room.databaseBuilder(context, AppDatabase::class.java, file).build().withClosing { database ->
                val item = ReservationRepository(database).get(input.id)!!
                assertEquals(12345L, item.actualOtaCents)
                assertEquals(87655L, item.netCents)
                assertEquals(17531L, item.managementCents)
                assertEquals("Persistente", item.notes)
                assertEquals(property.id, item.propertyId)
            }
        } finally { context.deleteDatabase(file) }
    }
    @Test fun defaultsUsePlatformRateButHistoricalBookingAndEditingKeepSnapshots() = runBlocking {
        properties.save(property.copy(platformRates = "Booking.com=15,00"))
        val configured = properties.get(property.id)!!
        val input = draft().withPropertyDefaults(configured)
        assertEquals("15,00", input.otaPercent)
        bookings.save(input)
        properties.save(property.copy(otaPercent = "30", managementPercent = "50", platformRates = "Booking.com=25"))
        val original = bookings.get(input.id)!!
        bookings.save(BookingDraft.from(original).copy(notes = "Solo nota"))
        val edited = bookings.get(input.id)!!
        assertEquals(1500, edited.appliedOtaBasisPoints)
        assertEquals(2000, edited.appliedManagementBasisPoints)
        assertEquals(17000L, edited.managementCents)
    }
    @Test fun differentPropertiesCanHaveSameDates() = runBlocking {
        bookings.save(draft()); bookings.save(draft().copy(propertyId = other.id))
        assertEquals(2, bookings.reservations.first().size)
    }
    @Test fun checkOutAndNextCheckInOnSameDayAreAllowed() = runBlocking {
        bookings.save(draft()); bookings.save(draft().copy(checkIn = "12/06/2026", checkOut = "14/06/2026"))
        assertEquals(2, bookings.reservations.first().size)
    }
    @Test fun conflictNeedsExplicitCurrentApprovalAndCancellationReleasesDates() {
        runBlocking { bookings.save(draft()) }
        val candidate = draft().copy(checkIn = "11/06/2026", checkOut = "14/06/2026")
        val conflict = assertThrows(OverlapException::class.java) { runBlocking { bookings.save(candidate) } }
        runBlocking {
            assertNull(bookings.get(candidate.id))
            bookings.save(candidate, conflict.conflicts.map { ConflictToken(it.id, it.updatedAt) }.toSet())
            assertTrue(bookings.get(candidate.id)!!.overlapAcknowledged)
            conflict.conflicts.forEach { bookings.cancel(it.id) }; bookings.cancel(candidate.id)
            bookings.save(draft())
            assertEquals(3, bookings.reservations.first().size)
        }
    }
    @Test fun changingAConflictInvalidatesAnOldApproval() {
        val original = draft(); runBlocking { bookings.save(original) }
        val candidate = draft().copy(checkIn = "11/06/2026", checkOut = "14/06/2026")
        val conflict = assertThrows(OverlapException::class.java) { runBlocking { bookings.save(candidate) } }
        runBlocking { bookings.save(original.copy(notes = "Aggiornato")) }
        assertThrows(OverlapException::class.java) { runBlocking { bookings.save(candidate, conflict.conflicts.map { ConflictToken(it.id, it.updatedAt) }.toSet()) } }
    }
    @Test fun invalidDatesGuestCountsTimesAndUnknownPropertiesAreRejected() {
        listOf(draft().copy(checkOut = "10/06/2026"), draft().copy(checkOut = "09/06/2026"), draft().copy(checkIn = "31/06/2026"),
            draft().copy(guests = "0"), draft().copy(guests = "5"), draft().copy(expectedArrival = "25:00"),
            draft().copy(firstName = ""), draft().copy(propertyId = "missing"), draft().copy(actualOta = "1001"),
        ).forEach { item -> assertThrows(IllegalArgumentException::class.java) { runBlocking { bookings.save(item) } } }
        runBlocking { assertTrue(bookings.reservations.first().isEmpty()) }
    }
    @Test fun archivedPropertyKeepsExistingBookingsButRejectsNewOnes() {
        val input = draft(); runBlocking { bookings.save(input); properties.archive(property.id, true) }
        assertThrows(IllegalArgumentException::class.java) { runBlocking { bookings.save(draft()) } }
        runBlocking { bookings.save(input.copy(notes = "Nota storica")); assertEquals("Nota storica", bookings.get(input.id)!!.notes) }
    }
    @Test fun oneNightStayIsPossibleWithoutStructuralTwoNightRestriction() = runBlocking {
        val input = draft().copy(checkOut = "11/06/2026"); bookings.save(input)
        assertEquals(1L, bookings.get(input.id)!!.let { it.checkOutEpochDay - it.checkInEpochDay })
    }
}
