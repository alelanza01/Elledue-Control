package it.elledue.control

import android.content.Context
import android.graphics.Bitmap
import android.util.Base64
import androidx.room.Room
import androidx.test.core.app.ApplicationProvider
import it.elledue.control.data.*
import it.elledue.control.domain.*
import it.elledue.control.reports.*
import it.elledue.control.ui.PdfPreviewDocument
import kotlinx.coroutines.runBlocking
import org.json.JSONObject
import org.junit.*
import org.junit.Assert.*
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config
import org.robolectric.annotation.GraphicsMode
import java.io.ByteArrayOutputStream
import java.time.YearMonth

@RunWith(RobolectricTestRunner::class)
@Config(sdk = [34], application = android.app.Application::class)
@GraphicsMode(GraphicsMode.Mode.NATIVE)
class ProductArchiveTest {
    private lateinit var context: Context
    private lateinit var db: AppDatabase
    @Before fun setup() { context = ApplicationProvider.getApplicationContext(); db = Room.inMemoryDatabaseBuilder(context, AppDatabase::class.java).build() }
    @After fun close() { db.close() }
    private fun photo(): String { val bitmap = Bitmap.createBitmap(160, 100, Bitmap.Config.ARGB_8888); bitmap.eraseColor(android.graphics.Color.rgb(46,87,107)); val bytes = ByteArrayOutputStream(); bitmap.compress(Bitmap.CompressFormat.JPEG, 80, bytes); bitmap.recycle(); return Base64.encodeToString(bytes.toByteArray(), Base64.NO_WRAP) }
    @Test fun coverAndProfileSurviveBackupRestoreReopenAndPropertyEdits() = runBlocking<Unit> {
        val report = reportFixture(db, context); val image = photo(); val properties = PropertyRepository(db)
        properties.setCover(report.property.id, image)
        properties.save(PropertyDraft.from(properties.get(report.property.id)!!).copy(name = "Nome aggiornato"))
        SettingsRepository(db.settingsDao()).saveProfile("Operatore", "Dimostrativo", image)
        SettingsRepository(db.settingsDao()).saveOrganizationName("Elledue test")
        assertEquals(image, db.settingsDao().get()!!.operatorPhotoBase64)
        val backup = BackupService(db, context); val prepared = backup.prepare(backup.export())
        val name = "m45-photo-restored.db"; context.deleteDatabase(name)
        try {
            Room.databaseBuilder(context, AppDatabase::class.java, name).build().withClosing { target -> BackupService(target, context).restore(prepared, true) }
            Room.databaseBuilder(context, AppDatabase::class.java, name).build().withClosing { target ->
                assertEquals(image, target.propertyDao().get(report.property.id)!!.property.coverImageBase64)
                assertEquals("Operatore", target.settingsDao().get()!!.operatorFirstName)
                assertEquals(image, target.settingsDao().get()!!.operatorPhotoBase64)
                assertEquals(report, ReportRepository(target, context).snapshot(report.id))
            }
        } finally { context.deleteDatabase(name) }
        properties.setCover(report.property.id, null); assertNull(properties.get(report.property.id)!!.property.coverImageBase64)
    }
    @Test fun publishedV4BackupImportsWithEmptyNewFieldsAndSameFrozenSnapshot() = runBlocking<Unit> {
        val report = reportFixture(db, context); db.settingsDao().save(AppSettings())
        val service = BackupService(db, context); val root = JSONObject(service.export().toString(Charsets.UTF_8)); val payload = JSONObject(root.getString("payload"))
        payload.getJSONArray("properties").getJSONObject(0).remove("coverImageBase64")
        val settings = payload.getJSONArray("app_settings").getJSONObject(0); listOf("operatorFirstName", "operatorLastName", "operatorPhotoBase64").forEach(settings::remove)
        root.put("databaseVersion", 4).put("payload", payload.toString()).put("sha256", ReportCodec.hash(payload.toString()))
        service.restore(service.prepare(root.toString().toByteArray()), true)
        assertEquals(report, ReportRepository(db, context).snapshot(report.id)); assertEquals("", db.settingsDao().get()!!.operatorFirstName)
    }
    @Test fun destructiveDeleteIsBlockedForHistoryButEmptyPropertyCanBeDeletedAndArchivedRecovered() = runBlocking<Unit> {
        val report = reportFixture(db, context); val repo = PropertyRepository(db)
        try { repo.deleteEmpty(report.property.id); fail("Historical deletion should be blocked") } catch (_: IllegalArgumentException) { }
        repo.archive(report.property.id, true); assertTrue(repo.get(report.property.id)!!.property.archived)
        repo.archive(report.property.id, false); assertFalse(repo.get(report.property.id)!!.property.archived)
        val empty = PropertyDraft(name = "Vuoto", address = "Test", ownerName = "Test"); repo.save(empty); repo.deleteEmpty(empty.id); assertNull(repo.get(empty.id))
        assertEquals(17000L, ReportRepository(db, context).snapshot(report.id).summary().balance)
    }
    @Test fun bothLayoutsRenderAllPagesUsingRealPdfRendererOnAndroidBitmapsAndKeepSameBalance() = runBlocking<Unit> {
        val snapshot = reportFixture(db, context)
        val directory = java.io.File("build/reports/m45").apply { mkdirs() }
        ReportLayout.entries.forEach { layout ->
            val bytes = ByteArrayOutputStream().also { ReportPdf(context).write(snapshot, it, layout) }.toByteArray()
            java.io.File(directory, "Elledue-${layout.name.lowercase()}-dimostrativo.pdf").writeBytes(bytes)
            PdfPreviewDocument(context, bytes).use { doc ->
                assertTrue(doc.pages >= 1)
                for (i in 0 until doc.pages) {
                    val image = doc.render(i, 600); val pixels = IntArray(image.width * image.height); image.getPixels(pixels, 0, image.width, 0, 0, image.width, image.height)
                    assertTrue("Real page must contain rendered ink", pixels.count { it != android.graphics.Color.WHITE } > 1000)
                    assertTrue("Official logo gold must render without RGB channel swapping", pixels.count { it == android.graphics.Color.rgb(219,191,139) } > 20)
                    java.io.File(directory, "${layout.name.lowercase()}-page-${i+1}.png").outputStream().use { image.compress(Bitmap.CompressFormat.PNG, 100, it) }; image.recycle()
                }
            }
            assertEquals(17000L, snapshot.summary().balance)
        }
    }
    @Test fun invalidPhotoCannotReplaceArchiveEvenWithRecalculatedChecksum() = runBlocking<Unit> {
        val report = reportFixture(db, context); val service = BackupService(db, context)
        val root = JSONObject(service.export().toString(Charsets.UTF_8)); val payload = JSONObject(root.getString("payload"))
        payload.getJSONArray("properties").getJSONObject(0).put("coverImageBase64", "aW52YWxpZA==")
        root.put("payload", payload.toString()).put("sha256", ReportCodec.hash(payload.toString()))
        try { service.prepare(root.toString().toByteArray()); fail("Invalid image accepted") } catch (_: IllegalArgumentException) { }
        assertEquals(17000L, ReportRepository(db, context).snapshot(report.id).summary().balance)
    }
    @Test fun analyticsSeparateCheckinCompetenceFromCalendarOccupancyAndDeduplicateOverlaps() = runBlocking<Unit> {
        val base = reportFixture(db, context).bookings.first().copy(checkInEpochDay = java.time.LocalDate.parse("2026-11-29").toEpochDay(), checkOutEpochDay = java.time.LocalDate.parse("2026-12-03").toEpochDay(), competenceMonth = "2026-11")
        val november = PortfolioAnalytics.calculate(YearMonth.of(2026,11), base.propertyId, listOf(base, base.copy(id = "overlap", status = "CANCELLED")))
        val december = PortfolioAnalytics.calculate(YearMonth.of(2026,12), base.propertyId, listOf(base))
        assertEquals(1, november.bookings); assertEquals(2, november.nights); assertEquals(2, november.calendarOccupiedDays)
        assertEquals(0L, december.gross); assertEquals(0, december.bookings); assertEquals(2, december.calendarOccupiedDays)
        assertEquals(base.grossCents, november.gross)
    }
}
