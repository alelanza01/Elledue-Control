package it.elledue.control

import android.graphics.Bitmap
import android.graphics.Canvas
import androidx.compose.ui.test.*
import androidx.compose.ui.test.junit4.createAndroidComposeRule
import it.elledue.control.domain.*
import kotlinx.coroutines.runBlocking
import kotlinx.coroutines.flow.first
import org.junit.*
import org.junit.Assert.assertEquals
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config
import org.robolectric.annotation.GraphicsMode
import java.io.File
import java.time.YearMonth

@RunWith(RobolectricTestRunner::class)
@Config(sdk = [34], qualifiers = "w411dp-h891dp-xxhdpi")
@GraphicsMode(GraphicsMode.Mode.NATIVE)
class ReservationUiTest {
    @get:Rule val compose = createAndroidComposeRule<MainActivity>()
    private val property = PropertyDraft(id = "ui-property-a", ownerId = "ui-owner-a", name = "Casa A", address = "Via Test A", ownerName = "Proprietario A", maxGuests = "4")
    private val other = property.copy(id = "ui-property-b", ownerId = "ui-owner-b", name = "Casa B")
    private val today = BookingDates.today()
    private val app get() = compose.activity.application as ElledueApplication
    @Before fun seedProperties() = runBlocking { app.propertyRepository.save(property); app.propertyRepository.save(other) }
    private fun waitTag(tag: String) { compose.waitUntil(15_000) { compose.onAllNodes(hasTestTag(tag)).fetchSemanticsNodes().isNotEmpty() } }
    private fun waitText(text: String) { compose.waitUntil(15_000) { compose.onAllNodes(hasText(text)).fetchSemanticsNodes().isNotEmpty() } }
    private fun calendar() { compose.onNodeWithTag("nav-calendar").performClick(); waitTag("new-booking") }
    private fun fillNew() {
        compose.onNodeWithTag("new-booking").performClick()
        compose.onNodeWithTag("booking-first-name").performTextReplacement("Ospite")
        compose.onNodeWithTag("booking-last-name").performTextReplacement("UI")
        compose.onNodeWithTag("booking-guests").performTextReplacement("2")
        compose.onNodeWithTag("booking-gross").performScrollTo().performTextReplacement("1000")
    }
    private fun screenshot(name: String) {
        compose.runOnIdle {
            val view = compose.activity.window.decorView
            val bitmap = Bitmap.createBitmap(view.width, view.height, Bitmap.Config.ARGB_8888)
            view.draw(Canvas(bitmap))
            val file = File("build/reports/screenshots/$name.png"); file.parentFile!!.mkdirs()
            file.outputStream().use { bitmap.compress(Bitmap.CompressFormat.PNG, 100, it) }; bitmap.recycle()
        }
    }
    @Test fun userCreatesWithPickerEditsActualOtaAndCancelsWithoutLosingHistory() {
        calendar(); fillNew()
        compose.onNodeWithTag("booking-fee").performScrollTo().assertTextEquals(Amounts.euro(16100))
        compose.onNodeWithTag("booking-check-in").performScrollTo()
        compose.onNodeWithContentDescription("Seleziona Check-in").performClick()
        compose.onNodeWithText("Conferma data").performClick()
        compose.activityRule.scenario.recreate()
        compose.onNodeWithTag("booking-first-name").assertTextContains("Ospite")
        compose.onNodeWithTag("save-booking").performClick()
        waitText("Scheda prenotazione")
        compose.onNodeWithTag("booking-fee").performScrollTo().assertTextEquals(Amounts.euro(16100))
        screenshot("m2-booking-finance")
        compose.onNodeWithText("Modifica").performClick()
        compose.onNodeWithTag("booking-actual-ota").performScrollTo().performTextReplacement("150")
        compose.onNodeWithTag("booking-fee").performScrollTo().assertTextEquals(Amounts.euro(17000))
        compose.onNodeWithTag("save-booking").performClick(); waitText("Scheda prenotazione")
        compose.onNodeWithTag("cancel-booking").performScrollTo().performClick()
        compose.onNodeWithText("Conferma annullamento").performClick()
        compose.waitUntil(15_000) { runBlocking { app.reservationRepository.reservations.firstValue().single().status == "CANCELLED" } }
        compose.onNodeWithTag("booking-status").performScrollTo().assertTextEquals("Annullata")
        compose.onNodeWithContentDescription("Indietro").performClick()
        compose.onNodeWithTag("booking-list-mode").performClick()
        compose.onNodeWithText("Ospite UI").assertDoesNotExist()
        compose.onNodeWithTag("booking-cancelled-filter").performClick()
        compose.onNodeWithTag("bookings-scroll").performScrollToNode(hasText("Ospite UI"))
        compose.onNodeWithText("Ospite UI").assertIsDisplayed()
    }
    @Test fun overlappingSaveRequiresUserConfirmation() {
        val original = BookingDraft(propertyId = property.id, firstName = "Ospite", lastName = "Esistente", gross = "1000")
        runBlocking { app.reservationRepository.save(original) }
        calendar(); fillNew(); compose.onNodeWithTag("save-booking").performClick()
        waitText("Sovrapposizione rilevata")
        assertEquals(1, runBlocking { app.reservationRepository.reservations.firstValue().size })
        compose.onNodeWithText("Correggi date").performClick()
        compose.onNodeWithTag("save-booking").performClick(); waitText("Sovrapposizione rilevata")
        compose.onNodeWithText("Conferma sovrapposizione").performClick(); waitText("Scheda prenotazione")
        assertEquals(2, runBlocking { app.reservationRepository.reservations.firstValue().size })
    }
    @Test fun calendarDistinguishesTurnoverAndPropertyFilterAndOpensBooking() {
        val a = BookingDraft(propertyId = property.id, firstName = "Calendario", lastName = "A", gross = "1000")
        val b = a.copy(id = "ui-booking-b", propertyId = other.id, lastName = "B")
        val turnover = a.copy(id = "ui-turnover", lastName = "Nuovo", checkIn = today.plusDays(2).format(BookingDates.inputFormat), checkOut = today.plusDays(4).format(BookingDates.inputFormat))
        runBlocking { app.reservationRepository.save(a); app.reservationRepository.save(b); app.reservationRepository.save(turnover) }
        calendar()
        val target = today.plusDays(2)
        if (YearMonth.from(target) != YearMonth.from(today)) compose.onNodeWithTag("next-month").performClick()
        compose.onNodeWithTag("calendar-day-${target.toEpochDay()}").assertContentDescriptionContains("1 arrivi, 2 partenze, 1 soggiorni", substring = true).performClick()
        screenshot("m2-calendar")
        compose.onNodeWithTag("booking-property-selector").performClick()
        compose.onNodeWithTag("select-property-${other.id}").performClick()
        compose.onNodeWithTag("calendar-day-${target.toEpochDay()}").assertContentDescriptionContains("0 arrivi, 1 partenze, 0 soggiorni", substring = true)
        compose.onNodeWithTag("bookings-scroll").performScrollToNode(hasTestTag("booking-row-${b.id}"))
        compose.onNodeWithTag("booking-row-${b.id}").performClick(); waitText("Scheda prenotazione")
        compose.onNodeWithText("Calendario B").assertIsDisplayed()
        compose.onNodeWithText("Casa B").assertIsDisplayed()
    }
}

private suspend fun <T> kotlinx.coroutines.flow.Flow<T>.firstValue(): T = first()
