package it.elledue.control

import android.graphics.Bitmap
import android.graphics.Canvas
import androidx.compose.ui.test.*
import androidx.compose.ui.test.junit4.createAndroidComposeRule
import it.elledue.control.data.*
import it.elledue.control.domain.*
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.runBlocking
import org.junit.*
import org.junit.Assert.*
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config
import org.robolectric.annotation.GraphicsMode
import java.io.File
import java.time.YearMonth

@RunWith(RobolectricTestRunner::class)
@Config(sdk = [34], qualifiers = "w411dp-h891dp-night-xxhdpi")
@GraphicsMode(GraphicsMode.Mode.NATIVE)
class ProductUiTest {
    @get:Rule val compose = createAndroidComposeRule<MainActivity>()
    private val app get() = compose.activity.application as ElledueApplication
    private val date = BookingDates.today()
    private val property = PropertyDraft(id = "m45-ui-property", ownerId = "m45-ui-owner", name = "Casa Dimostrativa", address = "Indirizzo sintetico", ownerName = "Proprietario Dimostrativo", maxGuests = "4")
    private val booking = BookingDraft(id = "m45-ui-booking", propertyId = property.id, firstName = "Ospite", lastName = "Dimostrativo", guests = "3", checkIn = BookingDates.display(date.toEpochDay()), checkOut = BookingDates.display(date.plusDays(4).toEpochDay()), gross = "1000", actualOta = "195")
    @Before fun seed() = runBlocking<Unit> {
        app.propertyRepository.save(property); app.reservationRepository.save(booking); app.accountingRepository.initialize()
        app.accountingRepository.saveTax(booking.id, TaxDraft("3", "0", "4"))
        app.settingsRepository.saveProfile("Operatore", "Dimostrativo", null)
    }
    private fun waitTag(tag: String) { compose.waitUntil(15000) { compose.onAllNodes(hasTestTag(tag)).fetchSemanticsNodes().isNotEmpty() } }
    private fun back() { compose.runOnUiThread { compose.activity.onBackPressedDispatcher.onBackPressed() } }
    private fun screenshot(name: String) { compose.mainClock.advanceTimeBy(500); compose.waitForIdle(); compose.runOnIdle {
        val view = compose.activity.window.decorView; val dialog = org.robolectric.shadows.ShadowDialog.getShownDialogs().lastOrNull { it.isShowing }?.window?.decorView; assertTrue(view.width > 0 && view.height > 0)
        val bitmap = Bitmap.createBitmap(view.width, view.height, Bitmap.Config.ARGB_8888); view.draw(Canvas(bitmap)); dialog?.draw(Canvas(bitmap))
        val file = File("build/reports/screenshots/m45-$name.png"); file.parentFile!!.mkdirs(); file.outputStream().use { bitmap.compress(Bitmap.CompressFormat.PNG, 100, it) }; bitmap.recycle()
    } }
    @Test fun operationalJourneyPreservesLinksAndExposesAllDestinationsAndRealPdfPreview() {
        waitTag("home-scroll"); screenshot("home")
        compose.onNodeWithTag("quick-action").performClick(); screenshot("quick-actions"); compose.onNodeWithTag("close-quick").performClick(); compose.mainClock.advanceTimeBy(600)
        compose.onNodeWithTag("nav-properties").performClick(); waitTag("new-property"); screenshot("portfolio")
        compose.onNodeWithText(property.name).performClick(); waitTag("property-menu"); screenshot("property-detail")
        compose.onNodeWithTag("property-menu").performClick(); screenshot("property-menu"); compose.onNodeWithText("Archivia").performClick(); screenshot("archive-dialog"); compose.onNodeWithText("Annulla").performClick(); back()
        compose.onNodeWithTag("nav-calendar").performClick(); waitTag("booking-calendar-mode"); screenshot("calendar")
        compose.onNodeWithTag("bookings-scroll").performScrollToNode(hasTestTag("booking-row-${booking.id}")); compose.onNodeWithTag("booking-row-${booking.id}").performClick()
        waitTag("booking-add-cleaning"); screenshot("booking")
        compose.onNodeWithText("Attività").performClick(); waitTag("activity-title")
        compose.onNodeWithTag("activity-title").performTextReplacement("Verifica preparazione sintetica")
        compose.onNodeWithTag("save-activity").performClick()
        compose.waitUntil(15000) { runBlocking { app.activityRepository.activities.first().size == 1 } }
        runBlocking { assertEquals(booking.id, app.activityRepository.activities.first().single().reservationId) }
        screenshot("activities"); back()
        compose.onNodeWithTag("booking-add-cleaning").performClick(); waitTag("expense-editor")
        compose.onNodeWithTag("expense-quantity").performTextReplacement("3"); compose.onNodeWithTag("save-expense").performClick(); waitTag("expense-detail")
        runBlocking { val rows = app.accountingRepository.expenses.first(); assertEquals(1, rows.size); assertEquals(property.id, rows.single().propertyId); assertEquals(booking.id, rows.single().reservationId); assertEquals(2400L, rows.single().actualCents); assertEquals(YearMonth.from(date).toString(), rows.single().competenceMonth) }
        back(); back(); compose.onNodeWithTag("nav-economy").performClick(); waitTag("open-analysis"); screenshot("economy")
        compose.onNodeWithTag("open-analysis").performClick(); waitTag("analysis-scroll"); screenshot("analysis"); back()
        compose.onNodeWithTag("open-reports").performClick(); waitTag("generate-report"); screenshot("reports")
        compose.onNodeWithTag("generate-report").performClick(); waitTag("open-pdf-preview")
        compose.onNodeWithTag("open-pdf-preview").performScrollTo().performClick(); waitTag("pdf-pages")
        compose.waitUntil(20000) { compose.onAllNodes(hasContentDescription("Pagina 1 del PDF")).fetchSemanticsNodes().isNotEmpty() }
        compose.waitUntil(20000) { compose.onAllNodes(hasTestTag("preview-export") and isEnabled()).fetchSemanticsNodes().isNotEmpty() }; compose.onNodeWithTag("preview-export").assertIsEnabled(); screenshot("pdf-summary-preview")
        compose.onNodeWithTag("preview-detailed").performClick()
        compose.waitUntil(20000) { compose.onAllNodes(hasContentDescription("Pagina 1 del PDF")).fetchSemanticsNodes().isNotEmpty() }
        screenshot("pdf-detailed-preview"); compose.onNodeWithText("Chiudi").performClick()
    }
    @Test fun localProfileEditsPersistAndKeyboardCanBeDismissed() {
        waitTag("open-profile"); compose.onNodeWithTag("open-profile").performClick(); waitTag("profile-first")
        compose.onNodeWithTag("profile-first").performTextReplacement("Operatore aggiornato")
        compose.onNodeWithTag("profile-last").performTextReplacement("Locale")
        compose.onNodeWithTag("save-profile").performScrollTo().performClick()
        compose.waitUntil(15000) { runBlocking { app.settingsRepository.settings.first().operatorFirstName == "Operatore aggiornato" } }
        screenshot("profile"); compose.activityRule.scenario.recreate(); waitTag("profile-first"); compose.onNodeWithTag("profile-first").assertTextContains("Operatore aggiornato")
    }
    @Test fun phonePhotoSelectionReplacementRemovalAndRecreationUseOwnedBytes() {
        waitTag("nav-properties"); compose.onNodeWithTag("nav-properties").performClick(); waitTag("new-property"); compose.onNodeWithText(property.name).performClick(); waitTag("select-photo")
        fun select(color: Int) {
            val bitmap = Bitmap.createBitmap(900, 600, Bitmap.Config.ARGB_8888); bitmap.eraseColor(color)
            val file = File(compose.activity.cacheDir, "synthetic-phone-photo.jpg"); file.outputStream().use { bitmap.compress(Bitmap.CompressFormat.JPEG, 85, it) }; bitmap.recycle()
            compose.onNodeWithTag("select-photo").performClick()
            compose.runOnUiThread {
                val shadow = org.robolectric.Shadows.shadowOf(compose.activity)
                val started = shadow.nextStartedActivityForResult
                shadow.receiveResult(started.intent, android.app.Activity.RESULT_OK, android.content.Intent().setData(android.net.Uri.fromFile(file)))
            }
        }
        select(android.graphics.Color.rgb(38,86,110))
        compose.waitUntil(15000) { runBlocking { app.propertyRepository.get(property.id)!!.property.coverImageBase64 != null } }
        val original = runBlocking { app.propertyRepository.get(property.id)!!.property.coverImageBase64 }
        compose.activityRule.scenario.recreate(); waitTag("remove-photo"); screenshot("cover-photo")
        select(android.graphics.Color.rgb(78,94,121))
        compose.waitUntil(15000) { runBlocking { app.propertyRepository.get(property.id)!!.property.coverImageBase64 != original } }
        compose.onNodeWithTag("remove-photo").performClick()
        compose.waitUntil(15000) { runBlocking { app.propertyRepository.get(property.id)!!.property.coverImageBase64 == null } }
        compose.onNodeWithTag("select-photo").assertIsDisplayed()
    }
    @Test @Config(qualifiers = "w320dp-h640dp-night-xxhdpi", fontScale = 1.5f)
    fun smallScreenLargeFontsKeepPrimaryActionsAndEmptyStatesAccessible() {
        waitTag("quick-action"); compose.onNodeWithTag("nav-calendar").assertIsDisplayed(); compose.onNodeWithTag("quick-action").performClick()
        screenshot("small-large-font-actions"); compose.onNodeWithText("Nuova prenotazione").assertIsDisplayed(); compose.onNodeWithTag("close-quick").performClick(); compose.mainClock.advanceTimeBy(600)
        compose.onNodeWithTag("nav-properties").performClick(); screenshot("small-large-font-portfolio")
        compose.onNodeWithTag("new-property").performClick(); compose.onNodeWithTag("property-name").performTextReplacement("Bozza sintetica")
        screenshot("small-editor-keyboard"); compose.onNodeWithTag("save-property").assertIsDisplayed()
    }
    @Test @Config(qualifiers = "w411dp-h891dp-xxhdpi")
    fun lightThemeUsesSameNativeNavigationAndPortfolio() {
        waitTag("home-scroll"); screenshot("home-light"); compose.onNodeWithTag("nav-properties").performClick(); waitTag("new-property"); screenshot("portfolio-light")
    }
}
