package it.elledue.control

import android.content.Context
import android.net.Uri
import androidx.room.Room
import androidx.test.core.app.ApplicationProvider
import it.elledue.control.data.*
import it.elledue.control.domain.*
import it.elledue.control.reports.*
import kotlinx.coroutines.runBlocking
import org.junit.*
import org.junit.Assert.*
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config
import java.io.File

@RunWith(RobolectricTestRunner::class)
@Config(sdk=[34],application=android.app.Application::class)
class FinalSafetyTest {
 private val context=ApplicationProvider.getApplicationContext<Context>();private lateinit var db:AppDatabase
 @Before fun setup(){db=Room.inMemoryDatabaseBuilder(context,AppDatabase::class.java).build()}
 @After fun close(){db.close()}
 private suspend fun seed(){PropertyRepository(db).save(PropertyDraft(id="p",ownerId="o",name="Nome immobile lungo",address="Test",ownerName="Test",maxGuests="4"));AccountingRepository(db).initialize()}
 private fun draft()=BookingDraft(id="b",propertyId="p",firstName="Ospite",lastName="Sintetico",gross="500",taxableGuestCount="2",guests="2",checkIn="02/11/2026",checkOut="06/11/2026")
 private suspend fun reject(action:suspend()->Unit){try{action();fail("Operazione da rifiutare")}catch(_:IllegalArgumentException){}}
 @Test fun deletionAcceptsOnlySimpleWordNormalizedAndStillVerifiesBackupAndDependencies()=runBlocking<Unit>{
  seed();ReservationRepository(db).save(draft());val s=DeletionRepository(db,context);val p=s.prepare("property","p");val r=s.saveVerifiedBackup(p,Uri.fromFile(File(context.cacheDir,"deletion-normalized.json")))
  reject{s.delete(p,r,"Nome immobile lungo",true)};s.delete(p,r,"  eLiMiNa \n",true)
  assertNull(db.propertyDao().get("p"));assertTrue(db.reportDao().bookings().isEmpty());assertTrue(db.reportDao().expenses().isEmpty())
 }
 @Test fun failedDeletionRollsBackAllRowsEvenWithValidSimpleConfirmation()=runBlocking<Unit>{
  seed();ReservationRepository(db).save(draft());val s=DeletionRepository(db,context){throw IllegalArgumentException("Interruzione simulata")};val p=s.prepare("property","p");val r=s.saveVerifiedBackup(p,Uri.fromFile(File(context.cacheDir,"rollback.json")))
  reject{s.delete(p,r,"ELIMINA",true)};assertNotNull(db.propertyDao().get("p"));assertNotNull(db.reservationDao().get("b"))
 }
 @Test fun protectedPropertyHasArchiveAlternativeAndFrozenReportIsUnchanged()=runBlocking<Unit>{
  val frozen=reportFixture(db,context);val before=db.reportDao().get(frozen.id)!!.snapshotJson;val s=DeletionRepository(db,context)
  reject{s.prepare("property",frozen.property.id)};s.archive(frozen.property.id)
  assertTrue(db.propertyDao().get(frozen.property.id)!!.property.archived);assertEquals(before,db.reportDao().get(frozen.id)!!.snapshotJson)
 }
 @Test fun localDiscardAllNeverDeletesActualRecordsOrDatabaseDraftReports()=runBlocking<Unit>{
  seed();ReservationRepository(db).save(draft());val r=ReportRepository(db,context);val id=r.generate("p","2026-11");val store=DraftStore.get(context)
  store.save("booking/b/edit",draft().copy(firstName="Non salvato"));store.save("expense/new/OTHER",ExpenseDraft(amount="99"));store.save("activity/new#title","Non salvata")
  store.discardAll();assertTrue(store.snapshot().isEmpty());assertEquals("Ospite",db.reservationDao().get("b")!!.guestFirstName);assertEquals("DRAFT",db.reportDao().get(id)!!.status)
 }
 @Test fun generatedActivityIdAloneDoesNotPretendToBeAnEditedLocalDraft(){val store=DraftStore.get(context);store.discardAll();store.save("activity/new#id","generated");assertFalse(store.has("activity/new"));store.save("activity/new#title","Da fare");assertTrue(store.has("activity/new"));store.clear("activity/new");assertTrue(store.snapshot().isEmpty())}
 @Test fun abandoningARevisionPreservesOriginalConfirmedSnapshotAllocationsAndExpenses()=runBlocking<Unit>{
  val frozen=reportFixture(db,context);val repo=ReportRepository(db,context);val before=db.reportDao().get(frozen.id)!!.snapshotJson;val allocations=db.reportDao().allocations();val expenses=db.reportDao().expenses()
  val revision=repo.beginRevision(frozen.id,"Prova non confermata");repo.abandonDraft(revision)
  assertNull(db.reportDao().get(revision));assertEquals(before,db.reportDao().get(frozen.id)!!.snapshotJson);assertEquals(allocations,db.reportDao().allocations());assertEquals(expenses,db.reportDao().expenses());reject{repo.abandonDraft(frozen.id)}
 }
 @Test fun unreportedExpenseVoidIsIdempotentAndConfirmedOneNeedsExplicitRevision()=runBlocking<Unit>{
  val frozen=reportFixture(db,context);val repo=ReportRepository(db,context);val a=AccountingRepository(db);val cost=frozen.expenses.first();val before=db.reportDao().get(frozen.id)!!.snapshotJson
  reject{a.voidExpense(cost.id,"Inserita per errore")};val revision=repo.beginRevision(frozen.id,"Correzione spesa")
  a.voidExpense(cost.id,"Inserita per errore");val history=a.expenseHistory(cost.id);a.voidExpense(cost.id,"Secondo tocco");assertEquals(history,a.expenseHistory(cost.id));assertTrue(db.accountingDao().expense(cost.id)!!.voided);assertEquals(before,db.reportDao().get(frozen.id)!!.snapshotJson);assertNotNull(db.reportDao().get(revision))
 }
 @Test fun proposedForecastCanBeExcludedWithoutPostingAnExpenseAndRestoredOnce()=runBlocking<Unit>{
  seed();ReservationRepository(db).save(draft());val repo=ReportRepository(db,context);val id=repo.generate("p","2026-11");val f=db.stayCostDao().get("b").first{it.kind=="CLEANING"}
  assertEquals(2,repo.snapshot(id).proposedExpenses!!.size);StayCostRepository(db).save(f.copy(overrideCents=0));assertEquals(1,repo.snapshot(id).proposedExpenses!!.size);assertTrue(db.reportDao().expenses().isEmpty());assertTrue(db.accountingDao().movements().isEmpty())
  StayCostRepository(db).save(f.copy(overrideCents=null));assertEquals(2,repo.snapshot(id).proposedExpenses!!.size)
 }
}
