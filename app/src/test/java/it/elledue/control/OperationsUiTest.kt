package it.elledue.control

import android.content.ClipboardManager
import android.content.Context
import android.graphics.Bitmap
import android.graphics.Canvas
import androidx.compose.ui.test.*
import androidx.compose.ui.test.junit4.createAndroidComposeRule
import it.elledue.control.data.*
import it.elledue.control.domain.*
import kotlinx.coroutines.runBlocking
import kotlinx.coroutines.flow.first
import org.junit.*
import org.junit.Assert.*
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config
import org.robolectric.annotation.GraphicsMode
import java.io.File
import java.time.YearMonth

@RunWith(RobolectricTestRunner::class)
@Config(sdk=[34],qualifiers="w411dp-h891dp-night-xxhdpi")
@GraphicsMode(GraphicsMode.Mode.NATIVE)
class OperationsUiTest {
    @get:Rule val compose=createAndroidComposeRule<MainActivity>()
    private val app get()=compose.activity.application as ElledueApplication
    private val today=BookingDates.today()
    @Before fun seed()=runBlocking {
        app.propertyRepository.save(PropertyDraft(id="op-p",ownerId="op-o",name="Casa dimostrativa",address="Indirizzo di test",ownerName="Proprietario test",maxGuests="4",platformRates="Booking.com=19,50\nAirbnb=15\nVrbo=8"))
        app.propertyRepository.save(PropertyDraft(id="op-q",ownerId="op-qo",name="Altro immobile",address="Test",ownerName="Test"))
        app.reservationRepository.save(BookingDraft(id="op-b",propertyId="op-p",firstName="Nicolò",lastName="D’Amico",guests="3",checkIn=BookingDates.display(today.toEpochDay()),checkOut=BookingDates.display(today.plusDays(4).toEpochDay()),gross="1000"))
    }
    private fun waitTag(tag:String){compose.waitUntil(20000){compose.onAllNodes(hasTestTag(tag)).fetchSemanticsNodes().isNotEmpty()}}
    private fun shot(name:String){compose.mainClock.advanceTimeBy(600);compose.waitForIdle();compose.runOnIdle {
        val view=compose.activity.window.decorView;val b=Bitmap.createBitmap(view.width,view.height,Bitmap.Config.ARGB_8888);view.draw(Canvas(b))
        File("build/reports/screenshots/047-$name.png").apply{parentFile!!.mkdirs()}.outputStream().use{b.compress(Bitmap.CompressFormat.PNG,100,it)};b.recycle()
    }}
    @Test fun directTaskFormDateCalendarEditCompleteReopenDeleteAndPersistence() {
        waitTag("home-scroll");shot("home")
        compose.onNodeWithTag("quick-action").performClick();compose.onNodeWithText("Nuova attività").performClick();waitTag("activity-editor")
        compose.onNodeWithTag("activity-title").performTextReplacement("Controllo operativo")
        compose.onNodeWithTag("activity-notes").performTextReplacement("Nota di prova")
        compose.onNodeWithTag("activity-date").performScrollTo().performTextReplacement(BookingDates.display(today.toEpochDay()))
        compose.onNodeWithTag("activity-time").performTextReplacement("17:30")
        compose.activityRule.scenario.recreate();waitTag("activity-editor");compose.onNodeWithTag("activity-title").assertTextContains("Controllo operativo")
        shot("task-editor");compose.onNodeWithTag("save-activity").performScrollTo().performClick();waitTag("home-scroll")
        val task=runBlocking{app.activityRepository.activities.first().single()};assertEquals("Nota di prova",task.notes)
        compose.onNodeWithTag("nav-calendar").performClick();waitTag("bookings-scroll")
        compose.onNodeWithTag("bookings-scroll").performScrollToNode(hasTestTag("calendar-task-${task.id}"));shot("task-calendar")
        compose.onNodeWithTag("calendar-task-${task.id}").performClick();waitTag("activity-editor")
        compose.onNodeWithTag("activity-title").performTextReplacement("Controllo modificato")
        compose.onNode(isToggleable()).performScrollTo().performClick();compose.onNodeWithTag("save-activity").performScrollTo().performClick();waitTag("bookings-scroll")
        assertTrue(runBlocking{app.activityRepository.activities.first().single().completed})
        compose.onNodeWithTag("bookings-scroll").performScrollToNode(hasTestTag("calendar-task-${task.id}"));compose.onNodeWithTag("calendar-task-${task.id}").performClick()
        compose.onNode(isToggleable()).performScrollTo().performClick();compose.onNodeWithTag("save-activity").performScrollTo().performClick();waitTag("bookings-scroll")
        assertFalse(runBlocking{app.activityRepository.activities.first().single().completed})
        compose.onNodeWithTag("bookings-scroll").performScrollToNode(hasTestTag("calendar-task-${task.id}"));compose.onNodeWithTag("calendar-task-${task.id}").performClick()
        compose.onNodeWithText("Elimina attività").performScrollTo().performClick();compose.onNodeWithText("Conferma eliminazione").performClick();waitTag("bookings-scroll")
        assertTrue(runBlocking{app.activityRepository.activities.first().isEmpty()});assertEquals(1,runBlocking{app.reservationRepository.reservations.first().size})
    }
    @Test fun calendarPreviewCopiesExactSinglePropertyTextToAndroidClipboard() {
        waitTag("home-scroll");compose.onNodeWithTag("nav-calendar").performClick();waitTag("open-copy-calendar")
        compose.onNodeWithTag("open-copy-calendar").performClick();waitTag("copy-calendar-screen");shot("copy-calendar")
        val b=runBlocking{app.reservationRepository.reservations.first().single()}
        val expected=CalendarText.render("Casa dimostrativa",listOf(b))
        compose.onNodeWithTag("calendar-text-op-p").assertTextEquals(expected)
        compose.onNodeWithTag("copy-calendar-op-q").performScrollTo().assertIsNotEnabled()
        compose.onNodeWithTag("copy-calendar-op-p").performScrollTo().performClick()
        compose.onNodeWithTag("copy-feedback").performScrollTo().assertTextContains("Messaggio copiato",substring=true)
        val clipboard=compose.activity.getSystemService(Context.CLIPBOARD_SERVICE) as ClipboardManager
        assertEquals(expected,clipboard.primaryClip!!.getItemAt(0).text.toString())
        compose.activityRule.scenario.recreate();waitTag("copy-calendar-screen");compose.onNodeWithTag("calendar-text-op-p").assertTextEquals(expected)
    }
    @Test fun bilingualMessageLibraryPreviewCopyAndAppearancePersist() {
        waitTag("home-scroll");compose.onNodeWithTag("nav-economy").performClick();waitTag("economy-scroll")
        compose.onNodeWithTag("economy-scroll").performScrollToNode(hasTestTag("open-messages"));compose.onNodeWithTag("open-messages").performClick();waitTag("new-message")
        compose.onNodeWithTag("new-message").performClick();compose.onNodeWithTag("message-title").performTextReplacement("Richiesta arrivo")
        compose.onNodeWithTag("message-italian").performScrollTo().performTextReplacement("Ciao {nome_ospite}, a che ora arrivi a {immobile}?")
        compose.onNodeWithTag("message-english").performScrollTo().performTextReplacement("Hello {nome_ospite}, when do you arrive at {immobile}?")
        compose.onNodeWithTag("save-message").performScrollTo().performClick();waitTag("new-message")
        compose.onNodeWithText("Anteprima").performScrollTo().performClick()
        compose.onNodeWithTag("copy-message").performScrollTo().assertIsNotEnabled()
        compose.onNodeWithText("Seleziona prenotazione (facoltativa)").performScrollTo().performClick();compose.onNodeWithText("Nicolò D’Amico").performClick()
        compose.onNodeWithText("English").performScrollTo().performClick();shot("message-preview")
        compose.onNodeWithTag("copy-message").performScrollTo().performClick()
        val clipboard=compose.activity.getSystemService(Context.CLIPBOARD_SERVICE) as ClipboardManager
        assertEquals("Hello Nicolò, when do you arrive at Casa dimostrativa?",clipboard.primaryClip!!.getItemAt(0).text.toString())
        compose.activityRule.scenario.recreate();waitTag("message-preview");compose.onNodeWithTag("message-preview").assertTextContains("Hello Nicolò",substring=true)
        compose.onNodeWithText("Indietro").performClick();compose.onNodeWithText("Indietro").performClick();waitTag("open-reports")
        compose.onNodeWithTag("nav-settings").performClick();compose.onNodeWithText("Aspetto · copertina Home").performScrollTo().performClick()
        compose.onNodeWithTag("cover-3").performScrollTo().performClick();compose.onNodeWithTag("apply-cover").performScrollTo().performClick()
        compose.waitUntil(15000){runBlocking{app.settingsRepository.settings.first().homeCover==3}}
        compose.activityRule.scenario.recreate();waitTag("apply-cover");shot("appearance")
        assertEquals(3,runBlocking{app.settingsRepository.settings.first().homeCover})
    }
}
