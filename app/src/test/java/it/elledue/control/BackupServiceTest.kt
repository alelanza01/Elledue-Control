package it.elledue.control

import android.content.Context
import androidx.room.Room
import androidx.test.core.app.ApplicationProvider
import it.elledue.control.data.*
import it.elledue.control.domain.*
import it.elledue.control.reports.*
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.runBlocking
import org.json.JSONObject
import org.junit.*
import org.junit.Assert.*
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config

@RunWith(RobolectricTestRunner::class)
@Config(sdk = [34], application = android.app.Application::class)
class BackupServiceTest {
    private lateinit var db: AppDatabase; private lateinit var service: BackupService; private lateinit var context: Context
    @Before fun setup() { context = ApplicationProvider.getApplicationContext(); db = Room.inMemoryDatabaseBuilder(context, AppDatabase::class.java).build(); service = BackupService(db, context) }
    @After fun close() { db.close() }
    private suspend fun reject(action: suspend () -> Unit) { try { action(); fail("Expected rejection") } catch (_: Exception) { } }
    @Test fun exportRestoreIncludesConfirmedSnapshotsLedgerAuditsAndIds() = runBlocking {
        val snapshot = reportFixture(db, context)
        val reports = ReportRepository(db, context); val revision = reports.beginRevision(snapshot.id, "Rettifica dimostrativa")
        val booking = db.reservationDao().get("synthetic-booking")!!
        ReservationRepository(db).save(BookingDraft.from(booking).copy(actualOta = "150", correctionReason = "OTA verificate"))
        reports.refresh(revision); reports.confirm(revision, ReportCodec.hash(ReportCodec.encode(reports.snapshot(revision))), true)
        val exported = service.export(); val verified = service.prepare(exported)
        val target = Room.inMemoryDatabaseBuilder(context, AppDatabase::class.java).build()
        try {
            PropertyRepository(target).save(PropertyDraft(name = "Da sostituire", address = "Test", ownerName = "Test"))
            BackupService(target, context).restore(verified, confirmed = true)
            assertEquals(1, PropertyRepository(target).properties.first().size)
            assertEquals(3, target.accountingDao().observeExpenses().first().size)
            assertEquals(2, target.reportDao().observe().first().size)
            assertEquals(2, target.accountingDao().movements().size)
            assertEquals(1, target.accountingDao().observeFinancialChanges().first().size)
            assertEquals(17000L, ReportRepository(target, context).snapshot(snapshot.id).summary().balance)
            assertEquals(17900L, ReportRepository(target, context).snapshot(revision).summary().balance)
            assertEquals(5, target.reportDao().allocations().size)
            target.openHelper.readableDatabase.query("PRAGMA foreign_key_check").use { assertFalse(it.moveToFirst()) }
        } finally { target.close() }
    }
    @Test fun restoreOverPopulatedLedgerIsAtomicAndRequiresExplicitConfirmation() = runBlocking {
        val snapshot = reportFixture(db, context); val bytes = service.export(); val prepared = service.prepare(bytes)
        reject { service.restore(prepared) }; assertEquals(17000L, ReportRepository(db, context).snapshot(snapshot.id).summary().balance)
        service.restore(prepared, confirmed = true)
        assertEquals(2, db.accountingDao().movements().size); assertEquals(1, db.reportDao().observe().first().size)
    }
    @Test fun malformedChecksumUnsupportedVersionAndMissingTablesDoNotAlterCurrentData() = runBlocking {
        val snapshot = reportFixture(db, context); val original = service.export()
        reject { service.prepare("not json".toByteArray()) }
        val root = JSONObject(original.toString(Charsets.UTF_8)); root.put("sha256", "wrong"); reject { service.prepare(root.toString().toByteArray()) }
        root.put("sha256", JSONObject(original.toString(Charsets.UTF_8)).getString("sha256")); root.put("databaseVersion", 999); reject { service.prepare(root.toString().toByteArray()) }
        val altered = JSONObject(original.toString(Charsets.UTF_8)); val payload = JSONObject(altered.getString("payload")); payload.remove("reports"); altered.put("payload", payload.toString()).put("sha256", ReportCodec.hash(payload.toString()))
        reject { service.prepare(altered.toString().toByteArray()) }
        assertEquals(17000L, ReportRepository(db, context).snapshot(snapshot.id).summary().balance)
    }
    @Test fun checksummedButBrokenRelationshipsAndInvalidMoneyAreRejected() = runBlocking {
        reportFixture(db, context); val bytes = service.export()
        val root = JSONObject(bytes.toString(Charsets.UTF_8)); val payload = JSONObject(root.getString("payload"))
        payload.getJSONArray("reservations").getJSONObject(0).put("propertyId", "missing-property")
        root.put("payload", payload.toString()).put("sha256", ReportCodec.hash(payload.toString())); reject { service.prepare(root.toString().toByteArray()) }
        val second = JSONObject(bytes.toString(Charsets.UTF_8)); val p = JSONObject(second.getString("payload")); p.getJSONArray("money_movements").getJSONObject(0).put("amountCents", -1)
        second.put("payload", p.toString()).put("sha256", ReportCodec.hash(p.toString())); reject { service.prepare(second.toString().toByteArray()) }
        assertEquals(1, db.reservationDao().observeAll().first().size)
    }
    @Test fun restoredDataPersistsAfterFileDatabaseReopening() = runBlocking {
        val snapshot = reportFixture(db, context); val prepared = service.prepare(service.export()); val name = "m4-restored.db"; context.deleteDatabase(name)
        try {
            Room.databaseBuilder(context, AppDatabase::class.java, name).build().withClosing { target -> BackupService(target, context).restore(prepared, true) }
            Room.databaseBuilder(context, AppDatabase::class.java, name).build().withClosing { reopened -> assertEquals(17000L, ReportRepository(reopened, context).snapshot(snapshot.id).summary().balance); assertEquals(3, reopened.accountingDao().observeExpenses().first().size) }
        } finally { context.deleteDatabase(name) }
    }
    @Test fun missingReportAllocationsAreRejectedEvenWithCorrectChecksum() = runBlocking {
        val snapshot = reportFixture(db, context)
        val root = JSONObject(service.export().toString(Charsets.UTF_8)); val payload = JSONObject(root.getString("payload"))
        payload.put("report_allocations", org.json.JSONArray())
        root.put("payload", payload.toString()).put("sha256", ReportCodec.hash(payload.toString()))
        reject { service.prepare(root.toString().toByteArray()) }
        assertEquals(17000L, ReportRepository(db, context).snapshot(snapshot.id).summary().balance)
        assertEquals(5, db.reportDao().allocations().size)
    }

}
