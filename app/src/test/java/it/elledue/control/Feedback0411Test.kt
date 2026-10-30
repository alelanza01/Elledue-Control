package it.elledue.control

import android.content.Context
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
import org.robolectric.annotation.GraphicsMode
import com.tom_roush.pdfbox.pdmodel.PDDocument
import com.tom_roush.pdfbox.text.PDFTextStripper
import java.io.ByteArrayOutputStream
import java.io.File
import org.json.JSONObject

@RunWith(RobolectricTestRunner::class)
@Config(sdk=[34],application=android.app.Application::class)
@GraphicsMode(GraphicsMode.Mode.NATIVE)
class Feedback0411Test {
 private val context=ApplicationProvider.getApplicationContext<Context>()
 private lateinit var db:AppDatabase
 @Before fun setup(){db=Room.inMemoryDatabaseBuilder(context,AppDatabase::class.java).build()}
 @After fun close(){db.close()}
 private suspend fun property(mode:String="NONE",rate:String="20") {
  PropertyRepository(db).save(PropertyDraft(id="p",ownerId="o",name="Immobile di prova",address="Indirizzo sintetico",ownerName="PROPRIETARIO_DA_NON_STAMPARE",maxGuests="10",otaTreatment=mode,managementPercent=rate));AccountingRepository(db).initialize()
 }
 private suspend fun input(id:String="b",guests:String="4",exempt:String="1",start:String="29/11/2026",end:String="03/12/2026"):BookingDraft = BookingDraft(id=id,firstName="Ospite",lastName="Sintetico",guests=guests,gross="1000",checkIn=start,checkOut=end).withPropertyDefaults(db.propertyDao().get("p")!!).copy(exemptGuestCount=exempt)
 private suspend fun report():ReportSnapshot {val r=ReportRepository(db,context);return r.snapshot(r.generate("p","2026-11"))}
 private fun summary(b:ReservationEntity,tax:TaxAssessmentEntity?,money:List<MoneyMovementEntity> = emptyList())=operationalSummary("2026-11","p",listOf(b),emptyList(),listOfNotNull(tax),money)
 @Test fun otaBaseAndVatAreRoundedOnCommissionOnlyAndManagementUsesFullNet() {
  val t=BookingFinance.calculate(100000,1950,null,2000,"BASE_PLUS_VAT",2200)
  assertEquals(19500L,t.baseOtaCents);assertEquals(4290L,t.vatCents);assertEquals(23790L,t.otaCents);assertEquals(76210L,t.netCents);assertEquals(15242L,t.managementCents)
  assertEquals(19053L,BookingFinance.calculate(100000,1950,null,2500,"BASE_PLUS_VAT").managementCents)
  assertEquals(1L,BookingFinance.calculate(3,5000,null,2000,"BASE_PLUS_VAT").netCents)
 }
 @Test fun actualInclusiveIsNeverUpliftedAgainAndActualBaseOverridesPercent() {
  assertEquals(23790L,BookingFinance.calculate(100000,1950,23790,2000,"INCLUDED").otaCents)
  assertEquals(12200L,BookingFinance.calculate(100000,1950,10000,2000,"BASE_PLUS_VAT").otaCents)
  assertEquals(10000L,BookingFinance.calculate(100000,1950,10000,2000,"NONE").otaCents)
  assertEquals(19500L,BookingFinance.calculate(100000,1950,null,2000,"NONE").otaCents)
 }
 @Test fun portalOverridesAndDirectDefaultsAreAppliedAndSnapshotted()=runBlocking<Unit>{
  property("BASE_PLUS_VAT")
  val p=db.propertyDao().get("p")!!;db.propertyDao().save(p.copy(platformCommissions=listOf(PlatformCommissionEntity("p","Airbnb",1500,"NONE"),PlatformCommissionEntity("p","Prenotazione diretta",100,"BASE_PLUS_VAT"))))
  val d=input().copy(platform="Airbnb").withPropertyDefaults(db.propertyDao().get("p")!!)
  assertEquals("NONE",d.otaTreatment);assertEquals("15,00",d.otaPercent);assertEquals(15000L,d.totals().otaCents)
  val direct=BookingDraft(platform="Prenotazione diretta",gross="1000").withPropertyDefaults(p)
  assertEquals(0L,direct.totals().otaCents)
  val exception=direct.withPropertyDefaults(db.propertyDao().get("p")!!);assertEquals(1220L,exception.totals().otaCents)
  ReservationRepository(db).save(d);assertEquals("NONE",db.reservationDao().get("b")!!.appliedOtaTreatment)
 }
 @Test fun differentManagementRatesRemainPropertySpecificInReportsAndAnalytics()=runBlocking<Unit>{
  property("BASE_PLUS_VAT",rate="25");ReservationRepository(db).save(input())
  val s=report();assertEquals(19053L,s.summary().fees)
  val a=BusinessAnalytics.calculate(s.month,listOf(db.propertyDao().get("p")!!),s.bookings,s.effectiveExpenses(),s.taxes,s.movements)
  assertEquals(s.summary().fees,a.fees);assertEquals(76210L,a.managedNet)
 }
 @Test fun automaticTaxNeedsOnlyCountsAndDatesAndNeverPostsCash()=runBlocking<Unit>{
  property();for(e in 0..4){val d=input(id="e$e",exempt=e.toString());val b=d.toRecord(null,false);assertEquals(4-e,b.taxableGuests);assertEquals(e,b.exemptGuests)
   val tax=TaxDraft((4-e).toString(),e.toString(),"4").record(b,db.propertyDao().get("p")!!.property,null)
   assertEquals((4-e)*1200L,summary(b,tax).compensated)
  }
  ReservationRepository(db).save(input());val s=report();assertEquals(3600L,s.summary().automaticAdvance);assertEquals(0L,s.summary().collectedTax);assertTrue(db.accountingDao().movements().isEmpty())
  assertEquals(0L,operationalSummary("2026-12","p",s.bookings,emptyList(),s.taxes,emptyList()).compensated)
 }
 @Test fun partialAndExistingCompensationsAreRecognizedOnceIncludingOtherPeriods()=runBlocking<Unit>{
  property();ReservationRepository(db).save(input());val a=AccountingRepository(db)
  a.collectTax("b",1200,"ELLEDUE",true,"first")
  var s=report();assertEquals(3600L,s.summary().compensated);assertEquals(2400L,s.summary().automaticAdvance)
  a.collectTax("b",2400,"ELLEDUE",true,"second")
  s=report();assertEquals(3600L,s.summary().compensated);assertEquals(0L,s.summary().automaticAdvance)
  assertEquals(4,db.accountingDao().movements().size)
  val shifted=s.movements.map{if(it.kind=="COMPENSATION")it.copy(competenceMonth="2026-12") else it}
  assertEquals(0L,summary(s.bookings.first(),s.taxes.first(),shifted).compensated)
 }
 @Test fun ownerPortalAndExplicitExceptionsDoNotInventCashOrAutomaticCredit()=runBlocking<Unit>{
  property();ReservationRepository(db).save(input());val a=AccountingRepository(db);a.collectTax("b",1200,"PORTAL",false,"portal")
  assertEquals(2400L,report().summary().compensated)
  val old=db.reservationDao().get("b")!!
  ReservationRepository(db).save(BookingDraft.from(old).copy(taxAdvanceOverride="15",taxAdvanceManual=true))
  assertEquals(1500L,report().summary().compensated)
  val updated=db.reservationDao().get("b")!!;ReservationRepository(db).save(BookingDraft.from(updated).copy(automaticTaxAdvance=false,taxAdvanceManual=true))
  assertEquals(0L,report().summary().compensated);assertEquals(1,db.accountingDao().movements().size)
 }
 @Test fun manualTaxAndCancelledBookingsAndOwnPropertiesArePreserved()=runBlocking<Unit>{
  property();ReservationRepository(db).save(input());val old=db.accountingDao().assessment("b")!!
  AccountingRepository(db).saveTax("b",TaxDraft.from(old).copy(actualExpected="30",reason="Eccezione verificata"))
  FeedbackReconciliation(db).reconcile(force=true);assertEquals(3000L,report().summary().compensated)
  ReservationRepository(db).cancel("b");assertEquals(0L,report().summary().automaticAdvance)
  val b=db.reservationDao().get("b")!!;assertEquals(0L,TaxAdvance.remaining(b.copy(status="CONFIRMED",appliedBusinessType="OWNED"),old,emptyList()))
 }
 @Test fun requiredFinalBalanceAndPerStayLaundryAggregationAgreeInBothPdfs()=runBlocking<Unit>{
  property()
  // €150 fees + €26 costs − €36 automatic advance = €140.
  val d=input(guests="4").copy(gross="937,50",otaPercent="20",otaManual=true)
  ReservationRepository(db).save(d);val s=report();assertEquals(15000L,s.summary().fees);assertEquals(2600L,s.summary().reimbursable);assertEquals(14000L,s.summary().balance)
  checkPdfs(s,"quadratura-140")
  val b=s.bookings.first();val stays=listOf(b.copy(id="a",guestCount=2),b.copy(id="bb",guestCount=1),b.copy(id="c",guestCount=1))
  val p=db.propertyDao().get("p")!!.property
  val forecasts=stays.flatMap{StandardCostProjection.defaults(it,p)}
  assertEquals(3,forecasts.filter{it.kind=="LAUNDRY"}.sumOf{it.quantity.toInt()})
  assertEquals(1500L,OperationalMoney.total(forecasts.filter{it.kind=="LAUNDRY"}.map{OperationalMoney.cost(it.quantity,it.unitCents)}))
  assertEquals(6,forecasts.filter{it.kind=="CLEANING"}.sumOf{it.quantity.toInt()});assertEquals(4800L,OperationalMoney.total(forecasts.filter{it.kind=="CLEANING"}.map{OperationalMoney.cost(it.quantity,it.unitCents)}))
 }
 private fun checkPdfs(s:ReportSnapshot,label:String) {
  val totals=s.summary();val finalTexts=mutableListOf<String>()
  for(layout in ReportLayout.entries) {
   val bytes=ByteArrayOutputStream().also{ReportPdf(context).write(s,it,layout)}.toByteArray();val out=File("build/reports/0411/$label-${if(s.draft)"BOZZA" else "CONFERMATO"}-$layout.pdf");out.parentFile!!.mkdirs();out.writeBytes(bytes)
   PDDocument.load(bytes).use{pdf->val text=PDFTextStripper().getText(pdf);File(out.path+".txt").writeText(text)
    assertTrue(text.contains("RIEPILOGO FINALE"));assertTrue(text.contains("Compenso di gestione Elledue"));assertTrue(text.contains("Spese anticipate da Elledue"));assertTrue(text.contains("Già saldato / anticipi in compensazione"));assertTrue(text.contains("TOTALE ANCORA DOVUTO A ELLEDUE"))
    assertTrue(text.contains("Pulizie · ${s.quantity("CLEANING")} ore"));assertTrue(text.contains("Lavanderia · ${s.quantity("LAUNDRY")} set"))
    for(forbidden in listOf("PROPRIETARIO_DA_NON_STAMPARE","Percentuali di gestione","Stimata","Costi a carico Elledue / standard","Costi da riconoscere nel saldo","Tassa prevista","Tassa di soggiorno riscossa","Tassa ancora da riscuotere","Identificativo:","VERSIONE"))assertFalse(forbidden,text.contains(forbidden))
    assertEquals(s.draft,text.contains("BOZZA"));assertTrue(text.contains(Amounts.editable(totals.balance)))
    finalTexts.add(text.substringAfter("RIEPILOGO FINALE").substringBefore("Elledue Management"))
   }
  }
  assertEquals(finalTexts[0].trim(),finalTexts[1].trim())
 }
 @Test fun confirmedReportsAreImmutableWhileBothOfficialPdfLayoutsHaveNoDraftOrTechnicalLabels()=runBlocking<Unit>{
  property("BASE_PLUS_VAT");ReservationRepository(db).save(input());val r=ReportRepository(db,context);val s=report();val frozen=r.confirm(s.id,ReportCodec.hash(ReportCodec.encode(s)),true,true)
  checkPdfs(frozen,"iva-ufficiale");val json=db.reportDao().get(s.id)!!.snapshotJson
  PropertyRepository(db).save(PropertyDraft.from(db.propertyDao().get("p")!!).copy(otaTreatment="NONE",automaticTaxAdvance=false))
  FeedbackReconciliation(db).reconcile(force=true);assertEquals(json,db.reportDao().get(s.id)!!.snapshotJson);assertEquals(23790L,db.reservationDao().get("b")!!.otaCents)
 }
 @Test fun backfillIsIdempotentSkipsConfirmedAndManualOtaAndKeepsOverrides()=runBlocking<Unit>{
  property("BASE_PLUS_VAT");val d=input().copy(otaTreatment="NONE",automaticTaxAdvance=false)
  db.reservationDao().save(d.toRecord(null,false));db.reservationDao().save(d.copy(id="manual",actualOta="100",otaManual=true).toRecord(null,false))
  val f=FeedbackReconciliation(db);f.reconcile();val first=db.reservationDao().get("b")!!;assertEquals(23790L,first.otaCents);assertTrue(first.automaticTaxAdvance)
  val manual=db.reservationDao().get("manual")!!;assertEquals(10000L,manual.otaCents)
  f.reconcile();f.reconcile(force=true);assertEquals(first,db.reservationDao().get("b"));assertEquals(manual,db.reservationDao().get("manual"))
 }
 @Test fun backupTenAndLegacyNineRestoreWithoutLosingNewRulesOrFrozenSnapshots()=runBlocking<Unit>{
  property("BASE_PLUS_VAT");ReservationRepository(db).save(input());val service=BackupService(db,context);val bytes=service.export();service.restore(service.prepare(bytes),true)
  assertEquals("BASE_PLUS_VAT",db.reservationDao().get("b")!!.appliedOtaTreatment);assertEquals(3600L,report().summary().compensated)
  val env=JSONObject(service.export().toString(Charsets.UTF_8));val tables=JSONObject(env.getString("payload"))
  // Represent an actual version9 backup with its published columns and old calculated money.
  val rows=tables.getJSONArray("reservations");for(i in 0 until rows.length()){val row=rows.getJSONObject(i);listOf("appliedOtaTreatment","appliedOtaVatBasisPoints","automaticTaxAdvance","taxAdvanceOverrideCents","otaTreatmentManual","taxAdvanceManual").forEach(row::remove);row.put("otaCents",19500).put("netCents",80500).put("managementCents",16100)}
  tables.getJSONArray("properties").let{rows2->for(i in 0 until rows2.length())listOf("otaTreatment","otaVatBasisPoints","automaticTaxAdvance").forEach(rows2.getJSONObject(i)::remove)}
  tables.getJSONArray("platform_commissions").let{ps->for(i in 0 until ps.length())ps.getJSONObject(i).remove("otaTreatment")}
  tables.getJSONArray("maintenance_state").let{states->for(i in states.length()-1 downTo 0)if(states.getJSONObject(i).getString("id")=="economics-0411")states.remove(i)}
  env.put("databaseVersion",9).put("payload",tables.toString()).put("sha256",ReportCodec.hash(tables.toString()));service.restore(service.prepare(env.toString().toByteArray()),true)
  assertEquals(19500L,db.reservationDao().get("b")!!.otaCents);assertEquals(3600L,report().summary().compensated);assertTrue(db.accountingDao().movements().isEmpty())
 }
 @Test fun threeSeparateStaysAggregateThreeSetsAndSixHoursInBothLayouts()=runBlocking<Unit>{
  property()
  val rows=listOf(Triple("a","2","01/11/2026" to "03/11/2026"),Triple("bb","1","05/11/2026" to "07/11/2026"),Triple("c","1","09/11/2026" to "11/11/2026"))
  for((id,g,dates) in rows)ReservationRepository(db).save(input(id=id,guests=g,exempt="0",start=dates.first,end=dates.second))
  val s=report();assertEquals("3",s.quantity("LAUNDRY"));assertEquals("6",s.quantity("CLEANING"));assertEquals(1500L,s.summary().laundry);assertEquals(4800L,s.summary().cleaning)
  checkPdfs(s,"tre-soggiorni")
 }
 @Test fun previousFormDraftsGainOnlyMissingFieldsAndKeepEveryEditedValue() {
  val store=DraftStore.get(context);val raw=com.google.gson.Gson().toJsonTree(BookingDraft(firstName="Da preservare",actualOta="237,90")).asJsonObject
  listOf("otaTreatment","otaVatBasisPoints","automaticTaxAdvance","taxAdvanceOverride","exemptGuestCount","taxAdvanceManual").forEach(raw::remove)
  store.replace(mapOf("booking/new" to raw.toString()));val loaded=store.load("booking/new",BookingDraft::class.java)!!
  assertEquals("Da preservare",loaded.firstName);assertEquals("237,90",loaded.actualOta);assertEquals("NONE",loaded.otaTreatment);assertEquals(2200,loaded.otaVatBasisPoints);assertEquals("",loaded.taxAdvanceOverride);assertEquals("",loaded.exemptGuestCount)
  store.clear("booking/new")
 }

 @Test fun otherOwnerPaymentsJoinAlreadySettledAndNegativePdfShowsAnExplicitCredit()=runBlocking<Unit>{
  property();ReservationRepository(db).save(input());val a=AccountingRepository(db)
  a.post(MoneyMovementEntity("owner-payment","p",null,"OWNER_PAYMENT",20000,BookingDates.parse("30/11/2026").toEpochDay(),"2026-11",notes="Pagamento documentato"))
  val s=report();assertEquals(-11950L,s.summary().balance);assertEquals(23600L,Math.addExact(s.summary().compensated,s.summary().payments))
  for(layout in ReportLayout.entries){val bytes=ByteArrayOutputStream().also{ReportPdf(context).write(s,it,layout)}.toByteArray();PDDocument.load(bytes).use{pdf->val text=PDFTextStripper().getText(pdf);assertTrue(text.contains("CREDITO DA RICONCILIARE"));assertTrue(text.contains("119,50"));assertTrue(text.contains("236,00"));assertFalse(text.contains("-119,50"))}}
 }

}
