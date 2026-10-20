package it.elledue.control
import android.graphics.*
import androidx.compose.ui.test.*
import androidx.compose.ui.test.junit4.createAndroidComposeRule
import it.elledue.control.data.*
import it.elledue.control.domain.*
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
class ConsolidatedUiTest {
 @get:Rule val compose=createAndroidComposeRule<MainActivity>()
 private val app get()=compose.activity.application as ElledueApplication
 private val today=BookingDates.today()
 private val day=today.toEpochDay()
 @Before fun seed()=runBlocking<Unit>{
  app.propertyRepository.save(PropertyDraft(id="049-p",ownerId="049-o",name="Casa sintetica",address="Indirizzo di prova",ownerName="Proprietario sintetico",maxGuests="6",cleaningRate="10"))
  app.reservationRepository.save(BookingDraft(id="049-b",propertyId="049-p",firstName="Ospite",lastName="Sintetico",guests="4",taxableGuestCount="3",gross="500",checkIn=BookingDates.display(day-5),checkOut=BookingDates.display(day),expectedDeparture="11:00"))
  app.reservationRepository.save(BookingDraft(id="049-next",propertyId="049-p",firstName="Secondo",lastName="Ospite",guests="2",gross="300",checkIn=BookingDates.display(day),checkOut=BookingDates.display(day+3)))
  val at=today.atTime(14,30).atZone(BookingDates.zone).toInstant().toEpochMilli()
  app.activityRepository.save(ActivityEntity("049-clean","049-p","Pulizia programmata",today.atTime(11,30).atZone(BookingDates.zone).toInstant().toEpochMilli(),reservationId="049-b",kind="CLEANING"))
  app.issueRepository.save(PropertyIssue(id="049-issue",propertyId="049-p",title="Verifica serratura",reservationId="049-b",priority="HIGH",status="SCHEDULED",scheduledAt=at,hasTime=true))
  app.messageRepository.initialize();app.messageRepository.save(GuestMessageEntity("049-out","049-p","out","Partenza","Buongiorno {nome_ospite}, confermi la partenza del {check_out} alle {ora_check_out}?","Hello {nome_ospite}, please confirm check-out on {check_out} at {ora_check_out}."))
 }
 private fun waitTag(tag:String){compose.waitUntil(15000){compose.onAllNodes(hasTestTag(tag)).fetchSemanticsNodes().isNotEmpty()}}
 private fun route(value:String){compose.runOnIdle{compose.activity.notificationRoute.value=value};compose.waitForIdle()}
 private fun capture(name:String){compose.mainClock.advanceTimeBy(600);compose.waitForIdle();compose.runOnIdle{
  val v=compose.activity.window.decorView;val bitmap=Bitmap.createBitmap(v.width,v.height,Bitmap.Config.ARGB_8888);v.draw(Canvas(bitmap))
  org.robolectric.shadows.ShadowDialog.getShownDialogs().lastOrNull{it.isShowing}?.window?.decorView?.draw(Canvas(bitmap))
  File("build/reports/screenshots/049-$name.png").apply{parentFile!!.mkdirs()}.outputStream().use{bitmap.compress(Bitmap.CompressFormat.PNG,100,it)};bitmap.recycle()
 }}
 @Test fun newReservationHasNoPropertyAndOneRangePickerPreservesBothDates(){
  route("booking/new");waitTag("booking-property-selector");compose.onNodeWithTag("booking-property-selector").assertTextContains("Seleziona appartamento")
  capture("new-reservation")
  compose.onNodeWithTag("booking-property-selector").performClick();compose.onNodeWithTag("select-property-049-p").performClick()
  compose.onNodeWithTag("booking-date-range").performScrollTo().performClick();waitTag("stay-range-picker");capture("date-range-picker")
  compose.onNodeWithText("Conferma soggiorno").performClick();compose.onNodeWithTag("booking-check-in").assertTextContains(BookingDates.display(day))
 }
 @Test fun calendarTapOpensChronologicalOperationalDayAndIssueDetail(){
  route("calendar");waitTag("calendar-day-$day");capture("monthly-calendar")
  compose.onNodeWithTag("calendar-day-$day").performClick();waitTag("operational-day");capture("operational-day")
  compose.onNodeWithTag("day-issue:049-issue").performScrollTo().performClick();waitTag("issue-editor");capture("issue-detail")
  compose.onNodeWithTag("issue-title").assertTextContains("Verifica serratura")
 }
 @Test fun issuesStaySummaryExpensesAndAnalyticsAreNavigableUsingRealPersistedFixtures(){
  route("issues");waitTag("issues-screen");capture("issues-registry")
  route("booking/049-b");waitTag("booking-status");capture("stay-summary")
  compose.onNodeWithTag("booking-add-cleaning").performScrollTo().performClick();waitTag("expense-editor");capture("cleaning-registration")
  compose.onNodeWithTag("expense-quantity").assertTextContains("2")
  route("expense/new/OTHER?booking=049-b");waitTag("expense-editor");capture("expense-registration")
  route("analysis");waitTag("analysis-scroll");capture("analytics-operations")
  compose.onNodeWithText("Economia",useUnmergedTree=true).performClick();capture("analytics-economy")
 }
 @Test fun notificationOpensContextualGuestTemplateWithEditableTextAndManualHandledState(){
  route("guest-message/049-b/out");waitTag("guest-reminder-message")
  compose.waitUntil(15000){compose.onAllNodes(hasText("Buongiorno Ospite",substring=true)).fetchSemanticsNodes().isNotEmpty()}
  capture("notification-guest-message")
  compose.onNodeWithTag("reminder-message-text").performTextReplacement("Messaggio verificato per l’ospite")
  compose.onNodeWithText("Copia messaggio").performScrollTo().performClick()
  assertFalse(app.reminderStore.handled("departure:049-b"))
  compose.onNodeWithText("Segna come gestito").performScrollTo().performClick();assertTrue(app.reminderStore.handled("departure:049-b"))
 }
 @Test @Config(qualifiers="w320dp-h740dp-hdpi") fun compactIssueAndDayRemainScrollable(){
  route("day/$day");waitTag("operational-day");capture("compact-day")
  compose.onNodeWithTag("day-issue:049-issue").performScrollTo().performClick();waitTag("issue-editor");capture("compact-issue")
  compose.onNodeWithTag("save-issue").performScrollTo().assertIsDisplayed()
 }
}
