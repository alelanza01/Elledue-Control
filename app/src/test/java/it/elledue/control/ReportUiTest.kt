package it.elledue.control

import android.graphics.Bitmap
import android.graphics.Canvas
import androidx.compose.ui.test.*
import androidx.compose.ui.test.junit4.createAndroidComposeRule
import it.elledue.control.data.*
import it.elledue.control.domain.*
import it.elledue.control.reports.*
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
@Config(sdk = [34], qualifiers = "w411dp-h891dp-xxhdpi")
@GraphicsMode(GraphicsMode.Mode.NATIVE)
class ReportUiTest {
    @get:Rule val compose = createAndroidComposeRule<MainActivity>()
    private val app get() = compose.activity.application as ElledueApplication
    private val today = BookingDates.today(); private val month = YearMonth.from(today).toString()
    private val property = PropertyDraft(id = "ui-reports-property", ownerId = "ui-reports-owner", name = "Casa Dimostrativa", address = "Via Esempio", ownerName = "Proprietario Dimostrativo", maxGuests = "4")
    private val booking = BookingDraft(id = "ui-reports-booking", propertyId = property.id, firstName = "Ospite", lastName = "Dimostrativo", guests = "3", checkIn = BookingDates.display(today.toEpochDay()), checkOut = BookingDates.display(today.plusDays(4).toEpochDay()), gross = "1000", actualOta = "195")
    @Before fun seed() = runBlocking<Unit> {
        app.propertyRepository.save(property); app.reservationRepository.save(booking); app.accountingRepository.initialize()
        app.accountingRepository.saveTax(booking.id, TaxDraft("3", "0", "4"))
        listOf(ExpenseDraft(propertyId = property.id, date = booking.checkIn, month = month, categoryId = "cleaning", quantity = "3", unit = "8"),
            ExpenseDraft(propertyId = property.id, date = booking.checkIn, month = month, categoryId = "laundry", quantity = "2", unit = "5"),
            ExpenseDraft(propertyId = property.id, date = booking.checkIn, month = month, categoryId = "detergents", amount = "5")).forEach { app.accountingRepository.saveExpense(it) }
        val cash = MoneyMovementEntity("ui-cash", property.id, booking.id, "COLLECTION", 3000, today.toEpochDay(), month, "Riscossione manuale")
        app.accountingRepository.post(cash); app.accountingRepository.post(cash.copy(id = "ui-comp", kind = "COMPENSATION", sourceId = cash.id))
    }
    private fun waitTag(tag: String) { compose.waitUntil(15000) { compose.onAllNodes(hasTestTag(tag)).fetchSemanticsNodes().isNotEmpty() } }
    private fun screenshot(name: String) { compose.mainClock.advanceTimeBy(500); compose.waitForIdle(); compose.runOnIdle {
        val view = compose.activity.window.decorView; val bitmap = Bitmap.createBitmap(view.width, view.height, Bitmap.Config.ARGB_8888); view.draw(Canvas(bitmap))
        val file = File("build/reports/screenshots/$name.png"); file.parentFile!!.mkdirs(); file.outputStream().use { bitmap.compress(Bitmap.CompressFormat.PNG, 100, it) }; bitmap.recycle()
    } }
    private fun generate() { compose.onNodeWithTag("nav-economy").performClick(); waitTag("open-reports"); compose.onNodeWithTag("open-reports").performScrollTo().performClick(); waitTag("generate-report"); screenshot("m4-reports-selection"); compose.onNodeWithTag("generate-report").performClick(); waitTag("report-balance") }
    private fun confirm() { compose.onNodeWithTag("report-detail-scroll").performScrollToNode(hasTestTag("confirm-report")); compose.onNodeWithTag("confirm-report").performClick(); compose.onNodeWithText("Conferma definitiva").performClick(); waitTag("export-report-pdf") }
    @Test fun userGeneratesChecksAndConfirmsThenRevisesSourceWithoutOverwritingHistory() {
        generate(); compose.onNodeWithTag("report-balance").assertTextEquals(Amounts.euro(17000)); screenshot("m4-report-draft")
        compose.onNodeWithTag("report-tab-1").performClick(); screenshot("m4-report-details")
        compose.onNodeWithTag("report-tab-2").performClick(); compose.onNodeWithTag("report-notes").performScrollTo().performTextReplacement("Nota dimostrativa verificata")
        compose.onNodeWithTag("report-detail-scroll").performScrollToNode(hasTestTag("refresh-report")); compose.onNodeWithTag("refresh-report").performClick()
        compose.waitUntil(15000) { runBlocking { app.reportRepository.reports.first().any { it.notes == "Nota dimostrativa verificata" } } }
        confirm()
        val original = runBlocking { app.reportRepository.reports.first().single() }; assertEquals("CONFIRMED", original.status)
        compose.onNodeWithTag("report-balance").performScrollTo(); screenshot("m4-report-confirmed")
        compose.onNodeWithTag("report-detail-scroll").performScrollToNode(hasTestTag("revise-report")); compose.onNodeWithTag("revise-report").performClick(); compose.onNodeWithTag("report-revision-reason").performScrollTo().performTextReplacement("Lordo verificato")
        compose.onNodeWithText("Apri nuova bozza rettificativa").performScrollTo().performClick(); compose.waitUntil(15000) { compose.onAllNodes(hasTestTag("report-detail-scroll") and hasStateDescription("DRAFT")).fetchSemanticsNodes().isNotEmpty() }
        compose.onNodeWithTag("report-tab-1").performClick(); compose.onNodeWithTag("report-detail-scroll").performScrollToNode(hasContentDescription("Modifica prenotazione")); compose.onNodeWithContentDescription("Modifica prenotazione").performClick()
        compose.onNodeWithTag("booking-gross").performScrollTo().performTextReplacement("1200")
        compose.onNodeWithTag("booking-correction-reason").performScrollTo().performTextReplacement("Lordo del portale verificato")
        compose.onNodeWithTag("save-booking").performClick(); waitTag("report-balance")
        compose.waitUntil(15000) { compose.onAllNodes(hasTestTag("report-balance") and hasText(Amounts.euro(21000))).fetchSemanticsNodes().isNotEmpty() }
        confirm()
        runBlocking { val versions = app.reportRepository.reports.first(); assertEquals(2, versions.size); assertEquals(original.snapshotJson, versions.first { it.id == original.id }.snapshotJson); assertEquals(17000L, app.reportRepository.snapshot(original.id).summary().balance) }
    }
    @Test
    @Config(qualifiers = "w411dp-h891dp-night-xxhdpi")
    fun settingsExposeManualBackupAndOriginalLogoIsDisplayed() {
        generate(); compose.onNodeWithContentDescription("Logo ufficiale Elledue").assertExists(); screenshot("m4-report-dark")
        compose.onNodeWithContentDescription("Indietro documento").performClick(); compose.onNodeWithContentDescription("Indietro rendiconti").performClick()
        compose.onNodeWithTag("nav-settings").performClick(); compose.onNodeWithTag("export-backup").performScrollTo().assertIsEnabled(); compose.onNodeWithTag("import-backup").assertIsEnabled()
        screenshot("m4-backup-settings")
    }
}
