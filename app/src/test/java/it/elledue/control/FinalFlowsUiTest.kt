package it.elledue.control

import android.Manifest
import android.graphics.*
import androidx.compose.ui.test.*
import androidx.compose.ui.test.junit4.createAndroidComposeRule
import it.elledue.control.data.*
import it.elledue.control.domain.*
import it.elledue.control.notifications.*
import it.elledue.control.reports.*
import kotlinx.coroutines.runBlocking
import kotlinx.coroutines.flow.first
import org.junit.*
import org.junit.Assert.*
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.Shadows
import org.robolectric.annotation.Config
import org.robolectric.annotation.GraphicsMode
import java.io.File

@RunWith(RobolectricTestRunner::class)
@Config(sdk=[34],qualifiers="w411dp-h891dp-night-xxhdpi")
@GraphicsMode(GraphicsMode.Mode.NATIVE)
class FinalFlowsUiTest {
 @get:Rule val compose=createAndroidComposeRule<MainActivity>()
 private val app get()=compose.activity.application as ElledueApplication
 @Before fun seed()=runBlocking<Unit>{
  app.propertyRepository.save(PropertyDraft(id="p",ownerId="o",name="Casa di prova",address="Test",ownerName="Test",maxGuests="4"))
  val b=BookingDraft(id="b",firstName="Originale",lastName="Sintetico",guests="2",gross="500",checkIn="02/11/2026",checkOut="06/11/2026").withPropertyDefaults(app.propertyRepository.get("p")!!)
  app.reservationRepository.save(b);app.accountingRepository.initialize();app.accountingRepository.saveExpense(ExpenseDraft(id="e",propertyId="p",description="Spesa originale",amount="5",date="03/11/2026",month="2026-11"));app.activityRepository.save(ActivityEntity("t","p","Attività originale",null))
 }
 private fun tag(t:String){try{compose.waitUntil(15000){compose.onAllNodes(hasTestTag(t)).fetchSemanticsNodes().isNotEmpty()}}catch(e:Throwable){File("build/reports/final3-tag-$t.txt").writeText(compose.onAllNodes(isRoot())[0].printToString());throw e}}
 private fun route(value:String){compose.runOnIdle{compose.activity.notificationRoute.value=value};compose.waitForIdle()}
 private fun back(){compose.runOnUiThread{compose.activity.onBackPressedDispatcher.onBackPressed()};compose.waitForIdle()}
 private fun shot(name:String){compose.mainClock.advanceTimeBy(600);compose.waitForIdle();compose.runOnIdle{val v=compose.activity.window.decorView;val b=Bitmap.createBitmap(v.width,v.height,Bitmap.Config.ARGB_8888);v.draw(Canvas(b));File("build/reports/screenshots/0411-final-$name.png").apply{parentFile!!.mkdirs()}.outputStream().use{b.compress(Bitmap.CompressFormat.PNG,100,it)};b.recycle()}}
 private fun generate()=runBlocking{app.reportRepository.generate("p","2026-11")}
 private fun confirmed():String=runBlocking{val id=generate();val s=app.reportRepository.snapshot(id);app.reportRepository.confirm(id,ReportCodec.hash(ReportCodec.encode(s)),true,true);id}
 @Test fun protectedDeletionShowsArchiveAndPreservesTheConfirmedDocument(){
  val id=confirmed();val before=runBlocking{ReportCodec.encode(app.reportRepository.snapshot(id))}
  route("delete/property/p");tag("archive-protected-property");shot("protected-property-archive")
  compose.onNodeWithTag("delete-permanently").assertDoesNotExist();compose.onNodeWithTag("archive-protected-property").performScrollTo().performClick()
  compose.waitUntil(15000){runBlocking{app.propertyRepository.get("p")!!.property.archived}};assertEquals(before,runBlocking{ReportCodec.encode(app.reportRepository.snapshot(id))})
 }
 @Test fun bookingExitKeepsDiscardsOrSavesWithoutEverDeletingTheOriginalRecord(){
  route("booking/b/edit");tag("booking-first-name");compose.onNodeWithTag("booking-first-name").performTextReplacement("Bozza locale");back();tag("exit-keep");compose.onNodeWithTag("exit-keep").performClick()
  assertEquals("Originale",runBlocking{app.reservationRepository.get("b")!!.guestFirstName});assertTrue(app.drafts.has("booking/b/edit"))
  route("booking/b/edit");tag("booking-first-name");compose.onNodeWithTag("booking-first-name").assertTextContains("Bozza locale");back();tag("exit-discard");compose.onNodeWithTag("exit-discard").performClick();assertFalse(app.drafts.has("booking/b/edit"))
  route("booking/b/edit");tag("booking-first-name");compose.onNodeWithTag("booking-first-name").performTextReplacement("Salvato");back();tag("exit-save");compose.onNodeWithTag("exit-save").performClick()
  compose.waitUntil(15000){runBlocking{app.reservationRepository.get("b")!!.guestFirstName=="Salvato"}};assertFalse(app.drafts.has("booking/b/edit"))
  route("booking/b/edit");tag("booking-first-name");back();compose.onNodeWithTag("exit-keep").assertDoesNotExist()
 }
 @Test fun expenseAndActivityLocalEditsCanBeKeptDiscardedAndSaved(){
  route("expense/e/edit");tag("expense-description");compose.onNodeWithTag("expense-description").performScrollTo().performTextReplacement("Appunto locale");back();tag("exit-keep");compose.onNodeWithTag("exit-keep").performClick()
  assertEquals("Spesa originale",runBlocking{app.accountingRepository.expenses.first().single().description});assertTrue(app.drafts.has("expense/e/edit"))
  route("expense/e/edit");tag("expense-description");compose.onNodeWithTag("expense-description").assertTextContains("Appunto locale");back();tag("exit-discard");compose.onNodeWithTag("exit-discard").performClick();assertFalse(app.drafts.has("expense/e/edit"))
  route("activity/t");tag("activity-title");compose.onNodeWithTag("activity-title").performTextReplacement("Titolo modificato");compose.onNodeWithTag("activity-editor").assert(SemanticsMatcher.expectValue(androidx.compose.ui.semantics.SemanticsProperties.StateDescription,"Modifiche non salvate"));assertEquals("Attività originale",runBlocking{app.activityRepository.activities.first().single().title});back();tag("exit-keep");compose.onNodeWithTag("exit-keep").performClick();assertEquals("Attività originale",runBlocking{app.activityRepository.activities.first().single().title})
  route("activity/t");tag("activity-title");compose.onNodeWithTag("activity-title").assertTextContains("Titolo modificato");back();tag("exit-save");compose.onNodeWithTag("exit-save").performClick();compose.waitUntil(15000){runBlocking{app.activityRepository.activities.first().single().title=="Titolo modificato"} && !app.drafts.has("activity/t")}
 }
 @Test fun discardAllLocalChangesHasConfirmationAndDoesNotRemoveDatabaseReportDraft(){
  val id=generate();app.drafts.save("booking/b/edit",BookingDraft.from(runBlocking{app.reservationRepository.get("b")!!}).copy(firstName="Solo locale"));app.drafts.save("expense/e/edit",ExpenseDraft(id="e",description="Solo locale"))
  route("drafts");tag("discard-all-local-drafts");shot("local-drafts-separated")
  compose.onNodeWithTag("discard-all-local-drafts").performClick();tag("confirm-discard-all-local");compose.onNodeWithTag("confirm-discard-all-local").performClick();assertTrue(app.drafts.snapshot().isEmpty());assertEquals("DRAFT",runBlocking{app.reportRepository.snapshot(id).let{if(it.draft)"DRAFT" else "CONFIRMED"}});assertNotNull(runBlocking{app.reservationRepository.get("b")})
 }
 @Test fun unreportedExpenseCanBeVoidedWithoutAnExtraTechnicalReasonAndThenCannotRepeat(){
  route("expense/e");tag("void-expense");compose.onNodeWithTag("void-expense").performScrollTo().performClick();compose.onNodeWithText("Conferma annullamento").performScrollTo().performClick()
  compose.waitUntil(15000){runBlocking{app.accountingRepository.expenses.first().single().voided}};compose.onNodeWithTag("void-expense").assertDoesNotExist();shot("expense-voided")
 }
 @Test fun reportedExpenseExplainsItsProtectionAndOffersTheReportRevision(){
  val id=confirmed();route("expense/e");tag("expense-open-report");compose.onNodeWithTag("void-expense").assertDoesNotExist();shot("expense-protected")
  compose.onNodeWithTag("expense-open-report").performScrollTo().performClick();tag("report-detail-scroll");compose.onNodeWithTag("report-detail-scroll").performScrollToNode(hasTestTag("revise-report"));assertFalse(runBlocking{app.reportRepository.snapshot(id).draft})
 }
 @Test fun reportDraftCanBeAbandonedWithoutDeletingItsSources(){
  val id=generate();route("report/$id");tag("report-detail-scroll");compose.onNodeWithTag("report-detail-scroll").performScrollToNode(hasTestTag("abandon-report-draft"));compose.onNodeWithTag("abandon-report-draft").performClick();tag("confirm-abandon-report");compose.onNodeWithTag("confirm-abandon-report").performClick()
  compose.waitUntil(15000){runBlocking{app.reportRepository.reports.first().none{it.id==id}}};assertNotNull(runBlocking{app.reservationRepository.get("b")});assertEquals(1,runBlocking{app.accountingRepository.expenses.first().size})
 }
 @Test fun diagnosticsShowsActualSettingsAndBothNotificationTestsCanBeInvoked(){
  Shadows.shadowOf(app).grantPermissions(Manifest.permission.POST_NOTIFICATIONS);app.reminderStore.save(ReminderSettings(enabled=true,monthly=false))
  route("notifications");tag("notification-settings");shot("notification-diagnostics")
  compose.onNodeWithTag("immediate-notification-test").performScrollTo().performClick();assertNotNull(app.reminderStore.status("last-posted"))
  compose.onNodeWithTag("scheduled-notification-test").performScrollTo().performClick();compose.waitUntil(15000){app.reminderStore.scheduled().keys.any{it.startsWith("diagnostic:")}}
  compose.onNodeWithTag("notification-scheduled-count").performScrollTo().assertExists();shot("scheduled-notification-confirmed")
 }
}
