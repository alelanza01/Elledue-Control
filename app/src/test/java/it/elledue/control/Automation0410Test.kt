package it.elledue.control

import android.content.Context
import androidx.room.Room
import androidx.test.core.app.ApplicationProvider
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
import java.io.ByteArrayOutputStream
import com.tom_roush.pdfbox.pdmodel.PDDocument
import com.tom_roush.pdfbox.text.PDFTextStripper

@RunWith(RobolectricTestRunner::class)
@Config(sdk=[34],application=android.app.Application::class)
@GraphicsMode(GraphicsMode.Mode.NATIVE)
class Automation0410Test {
 private val context=ApplicationProvider.getApplicationContext<Context>()
 private lateinit var db:AppDatabase
 @Before fun setup(){db=Room.inMemoryDatabaseBuilder(context,AppDatabase::class.java).build()}
 @After fun close(){db.close()}
 private suspend fun property() {PropertyRepository(db).save(PropertyDraft(id="p",ownerId="o",name="Casa sintetica",address="Test",ownerName="Test",maxGuests="10"));AccountingRepository(db).initialize()}
 private fun input(id:String="b",guests:String="4")=BookingDraft(id=id,propertyId="p",firstName="Ospite",lastName="Sintetico",guests=guests,taxableGuestCount=guests,checkIn="29/11/2026",checkOut="03/12/2026",gross="500")
 private suspend fun reject(block:suspend()->Unit){try{block();fail("Operazione da rifiutare")}catch(_:IllegalArgumentException){}}
 @Test fun laundryOneThroughTenGuestsAndCustomGroupingUseExactIntegerCents()=runBlocking<Unit>{
  property();val p=db.propertyDao().get("p")!!.property
  for(n in 1..10){val f=StandardCostProjection.defaults(input(guests=n.toString()).toRecord(null,false),p).first{it.kind=="LAUNDRY"};assertEquals(((n+1)/2).toString(),f.quantity);assertEquals(((n+1)/2)*500L,OperationalMoney.cost(f.quantity,f.unitCents))}
  val custom=StandardCostProjection.defaults(input(guests="7").toRecord(null,false),p.copy(laundrySetCents=750,laundryGuestsPerSet=3)).first{it.kind=="LAUNDRY"}
  assertEquals(2250L,OperationalMoney.cost(custom.quantity,custom.unitCents))
 }
 @Test fun retroactiveBackfillIsIdempotentTransactionalAndCreatesNoExpensesOrCash()=runBlocking<Unit>{
  property();db.reservationDao().save(input().toRecord(null,false))
  val reconciler=EconomicReconciliation(db);reconciler.reconcile();val first=db.stayCostDao().get("b");val tax=db.accountingDao().assessment("b")!!
  assertEquals(2,first.size);assertEquals(4800L,tax.expectedCents)
  reconciler.reconcile();reconciler.reconcile(force=true)
  assertEquals(first,db.stayCostDao().get("b"));assertEquals(tax,db.accountingDao().assessment("b"))
  assertTrue(db.reportDao().expenses().isEmpty());assertTrue(db.accountingDao().movements().isEmpty())
 }
 @Test fun cancelledOwnedManualOverridesAndActualCostsSurviveRepeatedReconciliation()=runBlocking<Unit>{
  property();val b=input().toRecord(null,false);db.reservationDao().save(b)
  StayCostRepository(db).save(StayCostForecast(b.id,"LAUNDRY","4",900,3200,true))
  AccountingRepository(db).saveExpense(ExpenseDraft(id="manual",propertyId="p",reservationId=b.id,categoryId="cleaning",quantity="3",unit="12",amount="35",date="03/12/2026",month="2026-12"))
  db.reservationDao().save(b.copy(id="cancelled",status="CANCELLED"));db.reservationDao().save(b.copy(id="own",appliedBusinessType="OWNED",appliedManagementBasisPoints=0,managementCents=0))
  val cost=db.accountingDao().expense("manual")!!;EconomicReconciliation(db).reconcile(force=true);EconomicReconciliation(db).reconcile(force=true)
  assertEquals(cost,db.accountingDao().expense("manual"));assertEquals(3200L,db.stayCostDao().get(b.id).first{it.kind=="LAUNDRY"}.overrideCents)
  assertTrue(db.stayCostDao().get("cancelled").isEmpty());assertTrue(db.stayCostDao().get("own").isEmpty())
 }
 @Test fun unknownHistoricalExemptionsRemainUnknownAndConsolidatedTaxIsUntouched()=runBlocking<Unit>{
  property();db.reservationDao().save(input().toRecord(null,false).copy(taxableGuests=null,exemptGuests=null,expectedTaxCents=null))
  val result=EconomicReconciliation(db).reconcile();assertTrue(result.contains("\"taxExemptionsUnknown\":1"));assertNull(db.accountingDao().assessment("b"))
  AccountingRepository(db).saveTax("b",TaxDraft("3","1","4",actualExpected="30",reason="Eccezione documentata"))
  val original=db.accountingDao().assessment("b");EconomicReconciliation(db).reconcile(force=true);assertEquals(original,db.accountingDao().assessment("b"))
 }
 @Test fun taxMatrixCapsNightsPerGuestAndPreservesAllExemptionsAndCustomRates()=runBlocking<Unit>{
  property();val p=db.propertyDao().get("p")!!.property
  for(n in listOf(1,2,3,4,5,7,10))for(g in 1..5)for(e in 0..g){val b=input(guests=g.toString()).toRecord(null,false).copy(checkOutEpochDay=BookingDates.parse("29/11/2026").toEpochDay()+n);val tax=TaxDraft((g-e).toString(),e.toString(),minOf(n,4).toString()).record(b,p,null);assertEquals((g-e)*minOf(n,4)*300L,tax.expectedCents)}
  val b=input(guests="5").toRecord(null,false).copy(checkOutEpochDay=BookingDates.parse("09/12/2026").toEpochDay())
  assertEquals(3600L,TaxDraft("3","2","4").record(b,p,null).expectedCents)
  assertEquals(5400L,TaxDraft("3","2","4").record(b,p.copy(touristTaxNightCents=450),null).expectedCents)
 }
 @Test fun proposalsAreInBothDraftPdfsButNotPostedUntilExplicitBulkApproval()=runBlocking<Unit>{
  property();ReservationRepository(db).save(input());val r=ReportRepository(db,context);val id=r.generate("p","2026-11");val s=r.snapshot(id)
  assertEquals(2600L,s.summary().reimbursable);assertEquals(10650L,s.summary().balance);assertEquals(2,s.proposedExpenses!!.size);assertTrue(db.reportDao().expenses().isEmpty())
  for(layout in ReportLayout.entries){val bytes=ByteArrayOutputStream().also{ReportPdf(context).write(s,it,layout)}.toByteArray();PDDocument.load(bytes).use{pdf->val text=PDFTextStripper().getText(pdf);assertTrue(text.contains("STANDARD PROPOSTI"));assertTrue(text.contains("Pulizie"));assertTrue(text.contains("Lavanderia"));assertTrue(text.contains("106,50"))}}
  reject{r.confirm(id,ReportCodec.hash(ReportCodec.encode(s)),true)}
  val approved=r.confirm(id,ReportCodec.hash(ReportCodec.encode(r.snapshot(id))),true,approveStandards=true)
  assertEquals(10650L,approved.summary().balance);assertEquals(2,db.reportDao().expenses().size);assertNotNull(approved.approvalBatch)
  assertTrue(approved.expenses.all{it.recognition=="STANDARD_APPROVED" && !it.effectiveDateConfirmed});assertTrue(approved.proposedExpenses.orEmpty().isEmpty())
  reject{r.confirm(id,ReportCodec.hash(ReportCodec.encode(approved)),true,true)}
  assertEquals(2,db.reportDao().expenses().size)
 }
 @Test fun actualDecemberCostAppearsAsLinkedInformationInNovemberWithoutDoubleCounting()=runBlocking<Unit>{
  property();ReservationRepository(db).save(input());AccountingRepository(db).saveExpense(ExpenseDraft(id="clean",propertyId="p",reservationId="b",categoryId="cleaning",quantity="3",unit="8",date="03/12/2026",month="2026-12"))
  val r=ReportRepository(db,context);val nov=r.snapshot(r.generate("p","2026-11"));val dec=r.snapshot(r.generate("p","2026-12"))
  assertEquals(1,nov.relatedExpenses!!.size);assertEquals(2400L,dec.summary().expenses);assertEquals(1000L,nov.summary().expenses)
  assertEquals(0,nov.proposedExpenses!!.count{it.kind=="CLEANING"});assertEquals("2026-12",db.accountingDao().expense("clean")!!.competenceMonth)
 }
 @Test fun approvalUsesBookingCompetenceAndKeepsExpectedInterventionDateThenAllowsRevision()=runBlocking<Unit>{
  property();ReservationRepository(db).save(input());val r=ReportRepository(db,context);val id=r.generate("p","2026-11")
  val first=r.confirm(id,ReportCodec.hash(ReportCodec.encode(r.snapshot(id))),true,true);val json=db.reportDao().get(id)!!.snapshotJson
  assertTrue(first.expenses.all{it.competenceMonth=="2026-11" && it.dateEpochDay==BookingDates.parse("03/12/2026").toEpochDay()})
  val rev=r.beginRevision(id,"Consuntivo verificato");val cost=db.accountingDao().expense(StayCostRepository.expenseId("b","CLEANING"))!!
  AccountingRepository(db).saveExpense(ExpenseDraft.from(cost).copy(amount="24",quantity="3",reason="Ore effettive"))
  val second=r.confirm(rev,ReportCodec.hash(ReportCodec.encode(r.snapshot(rev))),true,true)
  assertEquals(3400L,second.summary().expenses);assertEquals(json,db.reportDao().get(id)!!.snapshotJson);assertEquals(2,db.reportDao().expenses().size)
 }
 @Test fun manualUnlinkedCostsAreFlaggedRatherThanGuessedOrDuplicated()=runBlocking<Unit>{
  property();ReservationRepository(db).save(input());AccountingRepository(db).saveExpense(ExpenseDraft(id="unlinked",propertyId="p",categoryId="laundry",quantity="2",unit="5",date="03/12/2026",month="2026-12"))
  val r=ReportRepository(db,context);val s=r.snapshot(r.generate("p","2026-11"));assertEquals(0,s.proposedExpenses!!.count{it.kind=="LAUNDRY"});assertTrue(s.automationWarnings!!.any{it.contains("senza soggiorno")})
 }
 @Test fun quickCollectionAndCompensationAreAtomicIdempotentAndReversible()=runBlocking<Unit>{
  property();ReservationRepository(db).save(input());val a=AccountingRepository(db)
  a.collectTax("b",1500,"ELLEDUE",true,"request");a.collectTax("b",1500,"ELLEDUE",true,"request")
  var all=a.movements.first();assertEquals(2,all.size);assertEquals(1500L,Ledger.total(all,"COLLECTION"));assertEquals(1500L,Ledger.total(all,"COMPENSATION"))
  reject{a.collectTax("b",99999,"ELLEDUE",true,"over")};assertEquals(2,a.movements.first().size)
  val comp=all.first{it.kind=="COMPENSATION"};val cash=all.first{it.kind=="COLLECTION"}
  a.post(comp.copy(id="reverse-comp",kind="REVERSAL",sourceId=null,reversesId=comp.id,notes="Rettifica"))
  a.post(cash.copy(id="reverse-cash",kind="REVERSAL",sourceId=null,reversesId=cash.id,notes="Rettifica"))
  all=a.movements.first();assertEquals(0L,Ledger.total(all,"COLLECTION"));assertEquals(0L,Ledger.total(all,"COMPENSATION"))
 }
 @Test fun ownerOrPortalCollectionCannotCreateElledueAdvance()=runBlocking<Unit>{
  property();ReservationRepository(db).save(input());val a=AccountingRepository(db)
  reject{a.collectTax("b",1000,"OWNER",true,"bad")};assertTrue(a.movements.first().isEmpty())
  a.collectTax("b",1000,"OWNER",false,"owner");a.collectTax("b",1000,"PORTAL",false,"portal")
  assertEquals(2000L,Ledger.total(a.movements.first(),"COLLECTION"));assertEquals(0L,Ledger.total(a.movements.first(),"COMPENSATION"))
  val source=a.movements.first().first();reject{a.post(source.copy(id="forbidden",kind="COMPENSATION",sourceId=source.id))}
 }
 @Test fun approvedBatchAndProposalsSurviveVerifiedBackupRestore()=runBlocking<Unit>{
  property();ReservationRepository(db).save(input());val r=ReportRepository(db,context);val id=r.generate("p","2026-11");val s=r.confirm(id,ReportCodec.hash(ReportCodec.encode(r.snapshot(id))),true,true)
  val json=db.reportDao().get(id)!!.snapshotJson;val b=BackupService(db,context);b.restore(b.prepare(b.export()),true)
  assertEquals(json,db.reportDao().get(id)!!.snapshotJson);assertEquals(s,r.snapshot(id));assertEquals(2,db.reportDao().expenses().size)
 }
 @Test fun oldConfirmedSourcesAreNotBackfilledOrRewritten()=runBlocking<Unit>{
  val s=reportFixture(db,context);val before=db.reportDao().get(s.id)!!.snapshotJson
  val costs=db.reportDao().expenses();EconomicReconciliation(db).reconcile(force=true)
  assertEquals(before,db.reportDao().get(s.id)!!.snapshotJson);assertEquals(costs,db.reportDao().expenses())
 }
}
