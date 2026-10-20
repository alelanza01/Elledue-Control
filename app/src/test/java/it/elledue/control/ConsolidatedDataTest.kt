package it.elledue.control
import android.content.Context
import android.net.Uri
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
import java.time.*
import java.io.File

@RunWith(RobolectricTestRunner::class)
@Config(sdk=[34],application=android.app.Application::class)
class ConsolidatedDataTest {
 private val context=ApplicationProvider.getApplicationContext<Context>()
 private lateinit var db:AppDatabase
 @Before fun setup(){db=Room.inMemoryDatabaseBuilder(context,AppDatabase::class.java).build()}
 @After fun close(){db.close()}
 private suspend fun seed():BookingDraft {
  PropertyRepository(db).save(PropertyDraft(id="p",ownerId="o",name="Casa sintetica",address="Test",ownerName="Test",maxGuests="8",cleaningRate="10"))
  val draft=BookingDraft(id="b",propertyId="p",firstName="Ospite",lastName="Sintetico",guests="4",taxableGuestCount="3",checkIn="30/09/2026",checkOut="05/10/2026",gross="500")
  ReservationRepository(db).save(draft);AccountingRepository(db).initialize();return draft
 }
 private suspend fun reject(block:suspend()->Unit){try{block();fail("Operazione non valida accettata")}catch(_:IllegalArgumentException){}}
 @Test fun portalDefaultsAndManualOverrideUseExactCentsAndPropertyTakesPrecedence()=runBlocking<Unit>{
  seed();val p=db.propertyDao().get("p")!!
  listOf("Booking.com","Airbnb","Vrbo").forEach{assertEquals("19,50",BookingDraft.otaDefault(p,it))}
  assertEquals("0,00",BookingDraft.otaDefault(p,"Prenotazione diretta"))
  assertEquals(9750L,db.reservationDao().get("b")!!.otaCents)
  val configured=p.copy(platformCommissions=listOf(PlatformCommissionEntity("p","Airbnb",1500)))
  assertEquals("15,00",BookingDraft.otaDefault(configured,"Airbnb"))
  assertEquals("9",BookingDraft(otaPercent="9",otaManual=true).withPropertyDefaults(configured).otaPercent)
 }
 @Test fun forecastsAreNotCostsAndCleaningConfirmationIsOneExpenseWithActualOverride()=runBlocking<Unit>{
  seed();val repo=StayCostRepository(db);val forecasts=db.stayCostDao().get("b")
  assertEquals(2,forecasts.size);assertEquals("2",forecasts.first{it.kind=="LAUNDRY"}.quantity)
  assertEquals(1000L,OperationalMoney.cost(forecasts.first{it.kind=="LAUNDRY"}.quantity,500))
  assertTrue(db.reportDao().expenses().isEmpty());val cleaning=forecasts.first{it.kind=="CLEANING"}
  assertEquals(2000L,OperationalMoney.cost(cleaning.quantity,cleaning.unitCents))
  val id=repo.confirm(cleaning,"3",2500,date=BookingDates.parse("05/10/2026").toEpochDay())
  assertEquals(2500L,db.accountingDao().expense(id)!!.actualCents);assertEquals(3000L,db.accountingDao().expense(id)!!.calculatedCents)
  reject{repo.confirm(cleaning,"2",null,date=BookingDates.parse("05/10/2026").toEpochDay())}
  assertEquals(1,db.reportDao().expenses().size)
 }
 @Test fun guestChangesRecalculateLaundryUntilOverrideWithoutChangingConfirmedCosts()=runBlocking<Unit>{
  seed();val reservations=ReservationRepository(db);val costs=StayCostRepository(db)
  reservations.save(BookingDraft.from(reservations.get("b")!!).copy(guests="5"))
  assertEquals("3",db.stayCostDao().get("b").first{it.kind=="LAUNDRY"}.quantity)
  costs.save(db.stayCostDao().get("b").first{it.kind=="LAUNDRY"}.copy(quantity="4",overrideCents=1800))
  reservations.save(BookingDraft.from(reservations.get("b")!!).copy(guests="6"))
  assertEquals("4",db.stayCostDao().get("b").first{it.kind=="LAUNDRY"}.quantity)
 }
 @Test fun taxThirtySixIsForecastNotCollectionAndPartialCollectionKeepsCorrectResidual()=runBlocking<Unit>{
  seed();val tax=db.accountingDao().assessment("b")!!;assertEquals(3600L,tax.expectedCents);assertEquals(4,tax.taxableNights)
  assertEquals(0L,db.reservationDao().get("b")!!.collectedTaxCents)
  val a=AccountingRepository(db);a.post(MoneyMovementEntity("collection","p","b","COLLECTION",1000,BookingDates.parse("05/10/2026").toEpochDay(),"2026-10","Conferma manuale"))
  assertEquals(2600L,Ledger.residual(tax.expectedCents,Ledger.total(a.movements.first(),"COLLECTION")))
  assertEquals(0L,Ledger.total(a.movements.first(),"REMITTANCE"));assertEquals(0L,Ledger.total(a.movements.first(),"COMPENSATION"))
 }
 @Test fun issueSurvivesEndedBookingReopensAndKeepsStatusHistoryAndSingleLinkedCost()=runBlocking<Unit>{
  seed();val r=IssueRepository(db);val issue=PropertyIssue(id="i",propertyId="p",title="Serratura",reservationId="b",priority="URGENT")
  r.save(issue);val saved=db.issueDao().get("i")!!
  val a=AccountingRepository(db);a.saveExpense(ExpenseDraft(id="cost",propertyId="p",reservationId="b",categoryId="maintenance",amount="45"))
  r.save(saved.copy(status="RESOLVED",expenseId="cost"));val resolved=db.issueDao().get("i")!!
  assertNotNull(resolved.resolvedAt);r.save(resolved.copy(status="OPEN"));assertNull(db.issueDao().get("i")!!.resolvedAt)
  assertEquals(3,r.history("i").size);assertEquals(1,db.reportDao().expenses().size);assertEquals(4500L,db.reportDao().expenses().single().actualCents)
  assertEquals("Conclusa",StayStatus.label(db.reservationDao().get("b")!!,Instant.parse("2026-10-20T10:00:00Z")))
  assertTrue(IssueRepository.open(db.issueDao().get("i")!!))
 }
 @Test fun scheduledInterventionAndTurnoverAreSortedWithTimelessActivitiesLast()=runBlocking<Unit>{
  seed();val day=BookingDates.parse("05/10/2026").toEpochDay()
  val at=LocalDate.ofEpochDay(day).atTime(14,30).atZone(BookingDates.zone).toInstant().toEpochMilli()
  val issue=PropertyIssue(id="i",propertyId="p",title="Serratura",scheduledAt=at,hasTime=true,status="SCHEDULED")
  val new=BookingDraft(id="new",propertyId="p",firstName="Altro",lastName="Ospite",checkIn="05/10/2026",checkOut="08/10/2026",gross="100").toRecord(null,false)
  val task=ActivityEntity("a","p","Senza ora",at,hasTime=false)
  val rows=OperationalDay.events(day,db.reportDao().bookings()+new,listOf(task),listOf(issue))
  assertEquals(listOf("DEPARTURE","ISSUE","ARRIVAL","GENERAL"),rows.map{it.kind});assertEquals("issue/i",rows[1].route);assertNull(rows.last().time)
 }
 @Test fun occupancyClipsMonthsYearsLeapDaysAndDeduplicatesOverlapsWithoutChangingCompetence()=runBlocking<Unit>{
  seed();val b=db.reservationDao().get("b")!!.copy(checkOutEpochDay=BookingDates.parse("07/10/2026").toEpochDay())
  val sept=PortfolioAnalytics.calculate(YearMonth.of(2026,9),"p",listOf(b,b.copy(id="duplicate")))
  assertEquals(1,sept.nights);assertEquals(1,sept.calendarOccupiedDays);assertEquals(6,PortfolioAnalytics.calculate(YearMonth.of(2026,10),"p",listOf(b)).nights)
  val leap=b.copy(checkInEpochDay=LocalDate.of(2024,2,28).toEpochDay(),checkOutEpochDay=LocalDate.of(2024,3,2).toEpochDay())
  assertEquals(2,PortfolioAnalytics.calculate(YearMonth.of(2024,2),"p",listOf(leap)).nights)
  assertEquals("2026-09",b.competenceMonth);assertEquals(0,PortfolioAnalytics.calculate(YearMonth.of(2026,9),"p",listOf(b.copy(status="CANCELLED"))).nights)
 }
 @Test fun copiedCalendarIncludesDeparturesTodayAndAllFutureWithoutChangingDates()=runBlocking<Unit>{
  seed();val b=db.reservationDao().get("b")!!.copy(checkInEpochDay=LocalDate.of(2026,10,7).toEpochDay(),checkOutEpochDay=LocalDate.of(2026,10,10).toEpochDay())
  assertEquals(listOf(b),CalendarText.upcoming("p",LocalDate.of(2026,10,10),listOf(b)))
  assertTrue(CalendarText.upcoming("p",LocalDate.of(2026,10,11),listOf(b)).isEmpty())
  val future=b.copy(id="far",checkInEpochDay=LocalDate.of(2035,1,1).toEpochDay(),checkOutEpochDay=LocalDate.of(2035,1,3).toEpochDay())
  assertEquals(2,CalendarText.upcoming("p",LocalDate.of(2026,10,10),listOf(future,b)).size)
  assertTrue(CalendarText.render("Casa",listOf(b)).contains("07/10/2026"))
 }
 @Test fun ownerExclusionRequiresReasonKeepsInternalOccupancyAndFrozenOriginalUnchanged()=runBlocking<Unit>{
  seed();val repo=ReportRepository(db,context);val id=repo.generate("p","2026-09")
  reject{repo.setBookingOption(id,"b",false,true)}
  repo.setBookingOption(id,"b",false,true,reason="Accordo specifico documentato")
  assertEquals(0L,repo.snapshot(id).gross());assertEquals(1,db.reportDao().bookings().size)
  assertTrue(PortfolioAnalytics.calculate(YearMonth.of(2026,9),"p",db.reportDao().bookings()).nights>0)
  val option=db.reportDao().options(id).single();assertTrue(option.reason.isNotBlank());assertEquals(1,org.json.JSONArray(option.auditJson).length())
  repo.confirm(id,ReportCodec.hash(ReportCodec.encode(repo.snapshot(id))),true)
  val json=db.reportDao().get(id)!!.snapshotJson;reject{repo.setBookingOption(id,"b",false,false)};assertEquals(json,db.reportDao().get(id)!!.snapshotJson)
 }
 @Test fun bookingDeletionDetachesIssueInsteadOfRemovingItAndBackupRoundTripsAllNewData()=runBlocking<Unit>{
  seed();val r=IssueRepository(db);r.save(PropertyIssue(id="i",propertyId="p",title="Aperto",reservationId="b"))
  val backup=BackupService(db,context);val original=backup.export();val service=DeletionRepository(db,context)
  val plan=service.prepare("booking","b");val receipt=service.saveVerifiedBackup(plan,Uri.fromFile(File(context.cacheDir,"049-safety.json")))
  service.delete(plan,receipt,"ELIMINA ${plan.label}",true)
  assertNull(db.issueDao().get("i")!!.reservationId);assertTrue(IssueRepository.open(db.issueDao().get("i")!!));assertTrue(db.stayCostDao().get("b").isEmpty())
  backup.restore(backup.prepare(original),true);assertEquals("b",db.issueDao().get("i")!!.reservationId);assertEquals(2,db.stayCostDao().get("b").size)
 }
 @Test fun collectedTaxRemovesOnlyTaxPromptFromContextualTemplate(){
  val body="Partenza alle {ora_check_out}\nRicorda la tassa {tassa_residua}\nGrazie"
  assertEquals("Partenza alle {ora_check_out}\nGrazie",GuestText.forResidual(body,0))
  assertEquals(body,GuestText.forResidual(body,300))
 }
 @Test fun versionSevenBackupRestoresAndPreservesHistoricalReportBytes()=runBlocking<Unit>{
  val frozen=reportFixture(db,context);val report=db.reportDao().get(frozen.id)!!
  val legacy=org.json.JSONObject(report.snapshotJson!!).apply{remove("unconfirmedCosts");getJSONObject("property").apply{remove("laundryGuestsPerSet");remove("habitualTransportCents")}}.toString()
  db.reportDao().save(report.copy(snapshotJson=legacy))
  val service=BackupService(db,context);val root=org.json.JSONObject(service.export().toString(Charsets.UTF_8));val tables=org.json.JSONObject(root.getString("payload"))
  listOf("property_issues","issue_history","stay_cost_forecasts").forEach{tables.remove(it)}
  val props=tables.getJSONArray("properties");for(i in 0 until props.length())props.getJSONObject(i).apply{remove("laundryGuestsPerSet");remove("habitualTransportCents")}
  val opts=tables.getJSONArray("report_booking_options");for(i in 0 until opts.length())opts.getJSONObject(i).apply{remove("reason");remove("auditJson")}
  root.put("databaseVersion",7).put("payload",tables.toString()).put("sha256",ReportCodec.hash(tables.toString()))
  service.restore(service.prepare(root.toString().toByteArray()),true)
  assertEquals(legacy,db.reportDao().get(frozen.id)!!.snapshotJson);assertEquals(17000L,ReportCodec.decode(legacy).summary().balance)
  assertTrue(db.issueDao().all().isEmpty());assertEquals(2,db.propertyDao().get(frozen.property.id)!!.property.laundryGuestsPerSet)
 }
 @Test fun ownPropertyHasTransportCategoryAndNoAutomaticManagementOrCostForecasts()=runBlocking<Unit>{
  seed();PropertyRepository(db).save(PropertyDraft(id="own",ownerId="own-o",name="Propria",address="Test",businessType="OWNED",transportRate="7,50"))
  val own=BookingDraft(id="owned-stay",firstName="Ospite",lastName="Proprio",gross="500").withPropertyDefaults(db.propertyDao().get("own")!!)
  ReservationRepository(db).save(own)
  assertEquals(0L,db.reservationDao().get(own.id)!!.managementCents);assertTrue(db.stayCostDao().get(own.id).isEmpty())
  assertEquals(750L,db.propertyDao().get("own")!!.property.habitualTransportCents)
  assertEquals("Trasporti",db.accountingDao().category("transport")!!.label)
 }
}
