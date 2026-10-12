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

@RunWith(RobolectricTestRunner::class)
@Config(sdk = [34], qualifiers = "w411dp-h891dp-xxhdpi")
@GraphicsMode(GraphicsMode.Mode.NATIVE)
class AccountingUiTest {
    @get:Rule val compose = createAndroidComposeRule<MainActivity>()
    private val app get() = compose.activity.application as ElledueApplication
    private val property = PropertyDraft(id = "ui-accounting-property", ownerId = "ui-accounting-owner", name = "Casa Contabilità", address = "Via Test", ownerName = "Proprietario", maxGuests = "4")
    private val booking = BookingDraft(id = "ui-accounting-booking", propertyId = property.id, firstName = "Ospite", lastName = "Contabilità", guests = "4", checkIn = BookingDates.display(BookingDates.today().toEpochDay()), checkOut = BookingDates.display(BookingDates.today().plusDays(4).toEpochDay()), gross = "1000")
    @Before fun seed() = runBlocking { app.propertyRepository.save(property); app.reservationRepository.save(booking) }
    private fun waitTag(tag: String) { compose.waitUntil(15000) { compose.onAllNodes(hasTestTag(tag)).fetchSemanticsNodes().isNotEmpty() } }
    private fun economy() { compose.onNodeWithTag("nav-economy").performClick(); compose.onNodeWithTag("open-accounting").performClick(); waitTag("accounting-tab-1") }
    private fun screenshot(name: String) { compose.runOnIdle {
        val view = compose.activity.window.decorView; val bitmap = Bitmap.createBitmap(view.width, view.height, Bitmap.Config.ARGB_8888); view.draw(Canvas(bitmap))
        val file = File("build/reports/screenshots/$name.png"); file.parentFile!!.mkdirs(); file.outputStream().use { bitmap.compress(Bitmap.CompressFormat.PNG, 100, it) }; bitmap.recycle()
    } }
    @Test fun cleaningIsSavedOnceRectifiedWithReasonAndVoidedWithAuditHistory() {
        economy(); compose.onNodeWithTag("accounting-tab-1").performClick()
        compose.onNodeWithText("Pulizia", substring = false).performClick(); waitTag("expense-editor")
        compose.onNodeWithTag("expense-quantity").performTextReplacement("3")
        compose.onNodeWithTag("expense-calculated").assertTextEquals("Costo calcolato ${Amounts.euro(2400)}")
        compose.onNodeWithTag("expense-amount").performScrollTo().performTextReplacement("25")
        screenshot("m3-cleaning")
        compose.activityRule.scenario.recreate()
        waitTag("expense-editor"); compose.onNodeWithTag("expense-amount").performScrollTo().assertTextContains("25")
        compose.onNodeWithTag("save-expense").performClick(); waitTag("expense-detail")
        compose.onNodeWithTag("expense-actual").assertTextEquals(Amounts.euro(2500))
        runBlocking { assertEquals(1, app.accountingRepository.expenses.first().size) }
        compose.onNodeWithTag("edit-expense").performClick(); waitTag("expense-editor")
        compose.onNodeWithTag("expense-amount").performScrollTo().performTextReplacement("24")
        compose.onNodeWithTag("expense-reason").performScrollTo().performTextReplacement("Costo effettivo verificato")
        compose.onNodeWithTag("save-expense").performClick(); waitTag("expense-detail")
        compose.waitUntil(15000) { compose.onAllNodes(hasTestTag("expense-actual") and hasText(Amounts.euro(2400))).fetchSemanticsNodes().isNotEmpty() }
        compose.onNodeWithTag("void-expense").performScrollTo().performClick()
        compose.onNodeWithTag("void-reason").performScrollTo().performTextReplacement("Intervento annullato")
        compose.onNodeWithText("Conferma annullamento").performClick()
        compose.waitUntil(15000) { compose.onAllNodes(hasText("Spesa annullata: esclusa dai totali, storico conservato.")).fetchSemanticsNodes().isNotEmpty() }
        runBlocking { val cost = app.accountingRepository.expenses.first().single(); assertTrue(cost.voided); assertEquals(3, app.accountingRepository.expenseHistory(cost.id).size) }
    }
    @Test fun taxIsConfiguredCollectedPartiallyAndCompensatedOnlyOnExplicitConfirmation() {
        economy(); compose.onNodeWithTag("accounting-tab-2").performClick(); waitTag("tax-row-${booking.id}")
        compose.onNodeWithTag("tax-row-${booking.id}").performClick(); waitTag("tax-screen")
        compose.onNodeWithTag("tax-subjects").performScrollTo().performTextReplacement("3")
        compose.onNodeWithTag("tax-exempt").performScrollTo().performTextReplacement("1")
        compose.onNodeWithTag("save-tax").performScrollTo().performClick()
        compose.waitUntil(15000) { runBlocking { app.accountingRepository.assessments.first().isNotEmpty() } }
        runBlocking { assertEquals(3600L, app.accountingRepository.assessments.first().single().expectedCents); assertTrue(app.accountingRepository.movements.first().isEmpty()) }
        compose.onNodeWithTag("collect-tax").performScrollTo().performClick()
        compose.onNodeWithTag("movement-amount").performScrollTo().performTextReplacement("10")
        compose.onNodeWithTag("confirm-movement").performScrollTo().performClick()
        compose.waitUntil(15000) { compose.onAllNodes(hasTestTag("confirm-movement")).fetchSemanticsNodes().isEmpty() }
        compose.onNodeWithTag("tax-status").performScrollTo().assertTextEquals("Parzialmente incassata")
        val cash = runBlocking { app.accountingRepository.movements.first().single() }
        assertEquals(1000L, cash.amountCents)
        compose.onNodeWithTag("compensate-${cash.id}").performScrollTo().performClick()
        compose.onNodeWithTag("movement-amount").performScrollTo().performTextReplacement("5")
        compose.onNodeWithTag("confirm-movement").performScrollTo().performClick()
        compose.waitUntil(15000) { compose.onAllNodes(hasTestTag("confirm-movement")).fetchSemanticsNodes().isEmpty() }
        runBlocking { val money = app.accountingRepository.movements.first(); assertEquals(1000L, Ledger.total(money, "COLLECTION")); assertEquals(500L, Ledger.total(money, "COMPENSATION")); assertEquals(2600L, Ledger.residual(3600, Ledger.total(money,"COLLECTION"))) }
        compose.onNodeWithTag("collect-tax").performScrollTo(); screenshot("m3-tourist-tax")
    }
    @Test fun customCategoryAndQuickExpenseAreUsable() {
        economy(); screenshot("m3-accounting-summary"); compose.onNodeWithContentDescription("Categorie spese").performClick(); waitTag("new-category")
        compose.onNodeWithTag("new-category").performClick(); compose.onNodeWithTag("category-name").performTextReplacement("Giardinaggio")
        compose.onNodeWithText("Salva categoria").performClick()
        compose.waitUntil(15000) { compose.onAllNodes(hasTestTag("category-name")).fetchSemanticsNodes().isEmpty() }
        compose.onNodeWithContentDescription("Indietro categorie").performClick(); waitTag("accounting-tab-1")
        compose.onNodeWithTag("accounting-tab-1").performClick(); compose.onNodeWithTag("new-expense").performClick(); waitTag("expense-editor")
        compose.onNodeWithTag("Categoria").performClick(); compose.onNodeWithText("Giardinaggio").performClick()
        compose.onNodeWithTag("expense-amount").performTextReplacement("5")
        compose.onNodeWithTag("save-expense").performClick(); waitTag("expense-detail")
        runBlocking { val expense = app.accountingRepository.expenses.first().single(); assertEquals("Giardinaggio", expense.category); assertEquals(500L, expense.actualCents) }
    }
}
