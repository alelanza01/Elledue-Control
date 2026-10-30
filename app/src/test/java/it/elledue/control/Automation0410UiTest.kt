package it.elledue.control
import android.graphics.*
import androidx.compose.ui.test.*
import androidx.compose.ui.test.junit4.createAndroidComposeRule
import androidx.compose.ui.unit.dp
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
import java.time.*

@RunWith(RobolectricTestRunner::class)
@Config(sdk=[34],qualifiers="w411dp-h891dp-xxhdpi")
@GraphicsMode(GraphicsMode.Mode.NATIVE)
class Automation0410UiTest {
 @get:Rule val compose=createAndroidComposeRule<MainActivity>()
 private val app get()=compose.activity.application as ElledueApplication
 private val today=BookingDates.today()
 private val day=today.toEpochDay()
 @Before fun seed()=runBlocking<Unit>{
  app.propertyRepository.save(PropertyDraft(id="p",ownerId="o",name="Casa sintetica",address="Test",ownerName="Proprietario sintetico",maxGuests="10"))
  app.reservationRepository.save(BookingDraft(id="b",propertyId="p",firstName="Ospite",lastName="Sintetico",guests="4",taxableGuestCount="3",checkIn=BookingDates.display(day-3),checkOut=BookingDates.display(day+2),gross="500"))
 }
 private fun waitTag(tag:String){compose.waitUntil(15000){compose.onAllNodes(hasTestTag(tag)).fetchSemanticsNodes().isNotEmpty()}}
 private fun route(value:String){compose.runOnIdle{compose.activity.notificationRoute.value=value};compose.waitForIdle()}
 private fun capture(name:String){compose.mainClock.advanceTimeBy(600);compose.waitForIdle();compose.runOnIdle{
  val v=compose.activity.window.decorView;val b=Bitmap.createBitmap(v.width,v.height,Bitmap.Config.ARGB_8888);v.draw(Canvas(b))
  org.robolectric.shadows.ShadowDialog.getShownDialogs().lastOrNull{it.isShowing}?.window?.decorView?.draw(Canvas(b))
  File("build/reports/screenshots/0410-$name.png").apply{parentFile!!.mkdirs()}.outputStream().use{b.compress(Bitmap.CompressFormat.PNG,100,it)};b.recycle()
 }}
 @Test fun dateFieldsOpenTheSameRangePickerAndThereIsNoThirdButton(){
  route("booking/new");waitTag("booking-check-in");compose.onNodeWithTag("booking-date-range").assertDoesNotExist()
  capture("new-booking")
  compose.onNodeWithTag("booking-check-in").performScrollTo().performClick();waitTag("stay-range-picker");capture("range-picker")
  compose.onNodeWithText("Conferma soggiorno").performClick()
  compose.onNodeWithTag("booking-check-out").performScrollTo().performClick();waitTag("stay-range-picker")
  compose.onNodeWithText("Conferma soggiorno").performClick()
 }
 @Test @Config(qualifiers="w320dp-h740dp-hdpi",fontScale=1.5f) fun compactMonthIsBoundedWithLargeTextAndDayOpens(){
  route("calendar");waitTag("compact-month-grid");compose.onNodeWithTag("bookings-scroll").performScrollToNode(hasTestTag("compact-month-grid"))
  val bounds=compose.onNodeWithTag("compact-month-grid").getUnclippedBoundsInRoot();assertTrue("Griglia con barre e caratteri150%: ${bounds.bottom-bounds.top}",bounds.bottom-bounds.top<520.dp)
  capture("compact-month-320-large-type")
  compose.onNodeWithTag("calendar-day-$day").performScrollTo().performClick();waitTag("operational-day");capture("day-320-large-type")
  compose.onNodeWithTag("operational-day").performScrollToNode(hasTestTag("day-stay:b"));compose.onNodeWithTag("day-stay:b").performScrollTo().performClick();waitTag("booking-status")
 }
 @Test fun ordinaryExpenseUsesShortFormPrefilledStayAndExpandableDetails(){
  route("expense/new/OTHER?booking=b");waitTag("expense-editor")
  compose.onNodeWithTag("expense-month").assertDoesNotExist();compose.onNodeWithTag("expense-quantity").assertDoesNotExist()
  compose.onNodeWithTag("booking-property-selector").assertTextContains("Casa sintetica")
  compose.onNodeWithTag("expense-amount").performScrollTo().performTextReplacement("5")
  capture("quick-expense")
  compose.onNodeWithTag("expense-more-details").performScrollTo().performClick();compose.onNodeWithTag("expense-month").assertExists();capture("expense-details")
  compose.onNodeWithTag("save-expense").performClick();compose.waitUntil(15000){runBlocking{app.accountingRepository.expenses.first().any{it.actualCents==500L && it.reservationId=="b"}}}
 }
 @Test fun quickTaxRecordsPartialReceiptAndRecognitionTogetherWithVisibleFeedback(){
  route("booking/b");waitTag("quick-collect-tax");capture("stay-summary")
  compose.onNodeWithTag("quick-collect-tax").performScrollTo().performClick();waitTag("quick-tax-amount");compose.onNodeWithTag("quick-tax-amount").performScrollTo();capture("quick-tax")
  compose.onNodeWithTag("quick-tax-amount").performTextReplacement("12")
  compose.onNodeWithTag("confirm-quick-tax").performScrollTo().performClick()
  try{compose.waitUntil(15000){runBlocking{app.accountingRepository.movements.first().size==2}}}catch(e:Throwable){File("build/reports/0410-quick-tax-failure.txt").writeText(compose.onAllNodes(isRoot())[0].printToString()+"\nMovements="+runBlocking{app.accountingRepository.movements.first()});throw e}
  val all=runBlocking{app.accountingRepository.movements.first()};assertEquals(1200L,Ledger.total(all,"COLLECTION"));assertEquals(1200L,Ledger.total(all,"COMPENSATION"))
  compose.onNodeWithText("Riscossione registrata").assertExists()
 }
 @Test fun standardExceptionEditsQuantityRateAndAmountWithoutLosingIntentionalOverride(){
  route("booking/b");waitTag("edit-standard-LAUNDRY")
  compose.onNodeWithTag("edit-standard-LAUNDRY").performScrollTo().performClick();compose.onNodeWithTag("standard-unit-LAUNDRY").performScrollTo().performTextReplacement("6")
  compose.onNodeWithText("Salva previsione").performScrollTo().performClick()
  compose.waitUntil(15000){runBlocking{app.stayCosts.forecasts.first().any{it.kind=="LAUNDRY" && it.unitCents==600L && it.manual}}}
  capture("standard-exception")
 }
 @Test fun centralPlusKeepsAllCoreActionsAvailableFromEconomy(){
  route("economy");waitTag("quick-action");compose.onNodeWithTag("quick-action").performClick()
  listOf("Nuova prenotazione","Nuova spesa","Nuova attività","Nuova manutenzione").forEach{compose.onNodeWithText(it).assertExists()}
  compose.onNodeWithText("Nuova manutenzione").performScrollTo().performClick();waitTag("issue-editor")
 }
 @Test fun draftIncludesAutomaticCostsAndOneApprovalCreatesTheDefinitiveRows(){
  val month=YearMonth.from(LocalDate.ofEpochDay(day-3)).toString()
  val id=runBlocking{app.reportRepository.generate("p",month)}
  route("report/$id");waitTag("report-balance");capture("report-summary-proposals")
  compose.onNodeWithTag("pdf-detailed").performScrollTo().performClick();capture("report-detailed-proposals")
  compose.onNodeWithTag("report-detail-scroll").performScrollToNode(hasTestTag("report-tab-2"));compose.onNodeWithTag("report-tab-2").performClick()
  compose.onNodeWithTag("report-detail-scroll").performScrollToNode(hasTestTag("ack-report-warnings"));compose.onNodeWithTag("ack-report-warnings").performClick()
  compose.onNodeWithTag("report-detail-scroll").performScrollToNode(hasTestTag("confirm-report"));compose.onNodeWithTag("confirm-report").performClick();compose.onNodeWithText("Conferma definitiva").performClick()
  compose.waitUntil(15000){runBlocking{app.reportRepository.reports.first().single().status=="CONFIRMED"}}
  assertEquals(2,runBlocking{app.accountingRepository.expenses.first().size});capture("report-confirmed")
 }
}
