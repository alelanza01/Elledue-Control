package it.elledue.control

import android.graphics.*
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
class FeedbackUxUiTest {
 @get:Rule val compose=createAndroidComposeRule<MainActivity>()
 private val app get()=compose.activity.application as ElledueApplication
 private val today=BookingDates.today();private val month=YearMonth.from(today);private val start=month.atDay(1).toEpochDay()
 @Before fun properties()=runBlocking<Unit>{
  listOf("p1" to "Casa Aurora","p2" to "Casa Belvedere","p3" to "Casa Levante").forEach{(id,name)->app.propertyRepository.save(PropertyDraft(id=id,ownerId="o$id",name=name,address="Indirizzo sintetico",ownerName="Proprietario di prova",maxGuests="6"))}
 }
 private fun stay(id:String,p:String,from:Long,to:Long,first:String="Ospite",last:String=id,guests:String="2",cancel:Boolean=false)=runBlocking<Unit>{
  val property=app.propertyRepository.get(p)!!
  val d=BookingDraft(id=id,firstName=first,lastName=last,checkIn=BookingDates.display(from),checkOut=BookingDates.display(to),gross="500",guests=guests).withPropertyDefaults(property)
  app.reservationRepository.save(if(cancel)d.copy(status="CANCELLED") else d)
 }
 private fun tag(t:String){compose.waitUntil(15000){compose.onAllNodes(hasTestTag(t)).fetchSemanticsNodes().isNotEmpty()}}
 private fun route(value:String){compose.runOnIdle{compose.activity.notificationRoute.value=value};compose.waitForIdle()}
 private fun shot(name:String){compose.mainClock.advanceTimeBy(500);compose.waitForIdle();compose.runOnIdle{
  val v=compose.activity.window.decorView;val bitmap=Bitmap.createBitmap(v.width,v.height,Bitmap.Config.ARGB_8888);v.draw(Canvas(bitmap));File("build/reports/screenshots/0411-ux-$name.png").apply{parentFile!!.mkdirs()}.outputStream().use{bitmap.compress(Bitmap.CompressFormat.PNG,100,it)};bitmap.recycle()
 }}
 private fun calendar(){route("calendar");tag("compact-month-grid");compose.onNodeWithTag("bookings-scroll").performScrollToNode(hasTestTag("compact-month-grid"))}
 private fun multiple(){stay("Rossi","p1",start+1,start+4);stay("Bianchi","p1",start+4,start+9);stay("Verdi","p2",start+2,start+7);stay("Neri","p3",start+3,start+11)}
 @Test fun emptyMonthKeepsReadableDaysAndExplicitEmptyState(){calendar();compose.onNodeWithText("Mese libero · nessun soggiorno").performScrollTo().assertExists();shot("month-empty")}
 @Test fun onePropertyShowsConnectedIntervalsAndBarOpensTheStay(){
  stay("Rossi","p1",start+1,start+5);stay("Bianchi","p1",start+12,start+15)
  route("calendar");tag("booking-property-selector");compose.onNodeWithTag("booking-property-selector").performClick();compose.onNodeWithTag("select-property-p1").performClick()
  compose.onNodeWithTag("bookings-scroll").performScrollToNode(hasTestTag("compact-month-grid"));shot("month-one-property")
  val bar=compose.onAllNodes(hasTestTag("stay-bar-Rossi-0"));if(bar.fetchSemanticsNodes().isNotEmpty())bar[0].performScrollTo().performClick() else compose.onNodeWithTag("stay-bar-Rossi-1").performScrollTo().performClick()
  tag("booking-status");compose.onNodeWithText("Ospite Rossi").assertExists()
 }
 @Test fun threePropertiesAndSameDayTurnoverOpenCompleteDayWithCleaningAndGuestDetails(){
  multiple();val day=start+4
  runBlocking {app.activityRepository.save(ActivityEntity("clean","p1","Pulizia cambio ospiti",month.atDay(5).atTime(11,30).atZone(BookingDates.zone).toInstant().toEpochMilli(),reservationId="Rossi",kind="CLEANING"))}
  calendar();shot("month-three-properties-top")
  compose.onNodeWithTag("turnover-$day").performScrollTo().assertTextContains("Cambio ospiti",substring=true);shot("month-turnover")
  compose.onNodeWithTag("calendar-day-$day").performScrollTo().performClick();tag("operational-day");shot("turnover-day")
  compose.onNodeWithTag("operational-day").performScrollToNode(hasTestTag("day-task:clean"));compose.onNodeWithTag("day-task:clean").performScrollTo().assertExists();compose.onNodeWithTag("operational-day").performScrollToNode(hasTestTag("day-in:Bianchi"));compose.onNodeWithTag("day-in:Bianchi").performScrollTo().performClick();tag("booking-status")
 }
 @Test @Config(qualifiers="w320dp-h740dp-night-hdpi",fontScale=1.5f) fun multiPropertyMonthAndDayRemainScrollableAt320WithLargeType(){
  multiple();calendar();shot("month-320-large-top")
  compose.onNodeWithTag("turnover-${start+4}").performScrollTo();shot("month-320-large-turnover")
  compose.onNodeWithTag("calendar-day-${start+4}").performScrollTo().performClick();tag("operational-day");shot("day-320-large")
  compose.onNodeWithTag("operational-day").performScrollToNode(hasTestTag("day-stay:Verdi"));compose.onNodeWithTag("day-stay:Verdi").performScrollTo().performClick();tag("booking-status")
 }
 @Test fun homeOpensIndependentHistoricalSearchCancelledFilterAndDetail(){
  stay("old","p1",start-400,start-396,"Nicolò","D’Amico");stay("cancel","p2",start-30,start-26,"Nicolò","Annullato",cancel=true)
  stay("future","p3",today.toEpochDay()+10,today.toEpochDay()+13,"Nicolò","Futuro")
  stay("present","p2",today.toEpochDay()-2,today.toEpochDay()+2,"Nicolò","Presente")
  tag("open-reservations");compose.onNodeWithTag("open-reservations").performClick();tag("booking-search")
  compose.onNodeWithTag("booking-search").performTextReplacement("d’amico nicolo");compose.onNodeWithTag("booking-row-old").assertExists();shot("reservations-historical-search")
  compose.onNodeWithTag("booking-row-old").performClick();tag("booking-status");compose.activityRule.scenario.onActivity{it.onBackPressedDispatcher.onBackPressed()};tag("booking-search")
  compose.onNodeWithTag("booking-search").assertTextContains("d’amico nicolo");compose.onNodeWithTag("booking-search").performTextReplacement("Nicolò")
  compose.onNodeWithTag("browser-state-UPCOMING").performScrollTo().performClick();compose.onNodeWithTag("booking-row-future").assertExists();compose.onNodeWithTag("booking-row-old").assertDoesNotExist()
  compose.onNodeWithTag("browser-state-ACTIVE").performScrollTo().performClick();compose.onNodeWithTag("booking-row-present").assertExists()
  compose.onNodeWithTag("browser-state-PAST").performScrollTo().performClick();compose.onNodeWithTag("booking-row-old").assertExists()
  compose.onNodeWithTag("browser-state-CANCELLED").performScrollTo().performClick();compose.onNodeWithTag("booking-row-cancel").assertExists();compose.onNodeWithTag("booking-row-old").assertDoesNotExist();shot("reservations-cancelled")
 }
 @Test fun browserPropertyAndMonthFiltersAndCreationAreIndependentOfCalendar(){
  multiple();stay("older","p2",start-100,start-95);route("reservations");tag("browser-filters");compose.onNodeWithTag("browser-filters").performClick()
  compose.onNodeWithTag("booking-property-selector").performScrollTo().performClick();compose.onNodeWithTag("select-property-p2").performClick()
  compose.onNodeWithTag("Periodo").performScrollTo().performClick();compose.onNodeWithText("Mese",useUnmergedTree=true).performClick()
  compose.onNodeWithTag("booking-row-older").assertDoesNotExist()
  compose.onNodeWithTag("booking-row-Verdi").performScrollTo().assertExists();compose.onNodeWithTag("booking-row-Rossi").assertDoesNotExist()
  compose.onNodeWithTag("browser-new-booking").performScrollTo().performClick();tag("booking-first-name")
  compose.onNodeWithTag("booking-property-selector").performClick();compose.onNodeWithTag("select-property-p2").performClick()
  compose.onNodeWithTag("booking-first-name").performScrollTo().performTextReplacement("Nuova");compose.onNodeWithTag("booking-last-name").performTextReplacement("Prenotazione")
  compose.onNodeWithTag("booking-gross").performScrollTo().performTextReplacement("300");compose.onNodeWithTag("save-booking").performClick();tag("booking-status")
  assertTrue(runBlocking{app.reservationRepository.reservations.first().any{it.guestFirstName=="Nuova"}})
 }
 @Test fun economyHasPracticalExpenseAndPaymentRoutesAndAdvancedLedgerStillExists(){
  route("economy");tag("economy-expenses");shot("economy-overview")
  compose.onNodeWithTag("economy-expenses").performClick();tag("accounting-tab-1");compose.onNodeWithTag("new-expense").performScrollTo().performClick();tag("expense-editor")
  route("economy");tag("economy-payments");compose.onNodeWithTag("economy-payments").performClick();tag("owner-payment");shot("owner-payments")
  compose.onNodeWithTag("booking-property-selector").performClick();compose.onNodeWithTag("select-property-p1").performClick();compose.onNodeWithTag("owner-payment").assertIsEnabled()
  route("economy");tag("economy-scroll");compose.onNodeWithTag("economy-scroll").performScrollToNode(hasTestTag("open-accounting"));compose.onNodeWithTag("open-accounting").performClick();tag("accounting-scroll")
 }
 @Test fun maintenanceHasContextAndLinksAnExistingExpenseWithoutCreatingAnother(){
  runBlocking{app.accountingRepository.initialize();app.accountingRepository.saveExpense(ExpenseDraft(id="receipt",propertyId="p1",description="Scontrino manutenzione",amount="5"))}
  route("issues");tag("issues-screen");compose.onNodeWithText("Segnala manutenzione").performClick();tag("issue-title")
  compose.onNodeWithTag("booking-property-selector").performClick();compose.onNodeWithTag("select-property-p1").performClick();compose.onNodeWithTag("issue-title").performTextReplacement("Serratura da controllare")
  compose.onNodeWithTag("Spesa già registrata").performScrollTo().performClick();compose.onNodeWithText("Scontrino manutenzione",substring=true).performClick()
  compose.onNodeWithTag("save-issue").performScrollTo().performClick();compose.waitUntil(15000){runBlocking{app.issueRepository.issues.first().any{it.title=="Serratura da controllare"}}}
  assertEquals(1,runBlocking{app.accountingRepository.expenses.first().size});assertEquals("receipt",runBlocking{app.issueRepository.issues.first().single().expenseId});compose.waitUntil(15000){compose.onAllNodes(hasText("Scheda manutenzione")).fetchSemanticsNodes().isNotEmpty()};shot("maintenance-record")
 }
 @Test fun homeHierarchyAndTaxAlertsDistinguishAnAutomaticAdvanceFromARealException(){
  val day=today.toEpochDay();stay("auto","p1",day-1,day+3)
  tag("home-scroll");compose.onNodeWithTag("home-scroll").performScrollToNode(hasTestTag("home-attention-heading"));shot("home-attention")
  compose.onNodeWithTag("home-tax-exception-auto").assertDoesNotExist();compose.onNodeWithText("Tassa da riscuotere",substring=true).assertDoesNotExist()
  runBlocking{app.accountingRepository.collectTax("auto",300,"ELLEDUE",false,"real");val old=app.reservationRepository.get("auto")!!;app.reservationRepository.save(BookingDraft.from(old).copy(guests="3"))}
  compose.onNodeWithTag("home-scroll").performScrollToNode(hasTestTag("home-tax-exception-auto"));compose.onNodeWithTag("home-tax-exception-auto").performClick();tag("tax-screen")
 }
 @Test fun archivedPropertiesAreExcludedUntilHistoryIsRequestedExplicitly(){
  stay("archive","p3",start-400,start-397,"Ospite","Archivio");runBlocking{app.propertyRepository.archive("p3",true)}
  route("reservations");tag("booking-search");compose.onNodeWithTag("booking-search").performTextReplacement("Archivio");compose.onNodeWithTag("booking-row-archive").assertDoesNotExist()
  compose.onNodeWithTag("browser-filters").performClick();compose.onNodeWithTag("browser-archived").performScrollTo().performClick()
  compose.onNodeWithTag("reservations-browser").performScrollToNode(hasTestTag("booking-row-archive"));compose.onNodeWithTag("booking-row-archive").performClick();tag("booking-status")
 }

}
