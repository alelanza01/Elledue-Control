package it.elledue.control

import android.content.Context
import androidx.room.Room
import androidx.test.core.app.ApplicationProvider
import it.elledue.control.data.*
import it.elledue.control.domain.*
import it.elledue.control.reports.*
import kotlinx.coroutines.runBlocking
import org.junit.Assert.*
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config
import org.robolectric.annotation.GraphicsMode
import java.io.File
import java.io.ByteArrayOutputStream

@RunWith(RobolectricTestRunner::class)
@Config(sdk = [34], application = android.app.Application::class)
@GraphicsMode(GraphicsMode.Mode.NATIVE)
class ReportPdfTest {
    @Test fun realPdfWithSyntheticDataShows170AndHistoricalRegenerationUsesFrozenRows() = runBlocking {
        val context = ApplicationProvider.getApplicationContext<Context>(); val db = Room.inMemoryDatabaseBuilder(context, AppDatabase::class.java).build()
        try {
            val original = reportFixture(db, context); assertEquals(17000L, original.summary().balance)
            val reports = ReportRepository(db, context); val revision = reports.beginRevision(original.id, "OTA effettive rettificate")
            val current = db.reservationDao().get("synthetic-booking")!!
            ReservationRepository(db).save(BookingDraft.from(current).copy(actualOta = "150", correctionReason = "Commissione verificata"))
            reports.refresh(revision); reports.confirm(revision, ReportCodec.hash(ReportCodec.encode(reports.snapshot(revision))), false)
            val file = File("build/reports/m4/Elledue-rendiconto-sintetico.pdf"); file.parentFile!!.mkdirs()
            val pdf = ReportPdf(context); file.outputStream().use { pdf.write(reports.snapshot(original.id), it) }
            assertTrue(file.length() > 1000); assertTrue(file.readBytes().take(4).toByteArray().toString(Charsets.US_ASCII).startsWith("%PDF"))
            assertTrue(pdf.pageCount >= 2); assertEquals(17000L, reports.snapshot(original.id).summary().balance)
            val updated = File("build/reports/m4/Elledue-rendiconto-rettificato-sintetico.pdf"); updated.outputStream().use { ReportPdf(context).write(reports.snapshot(revision), it) }
            assertEquals(17900L, reports.snapshot(revision).summary().balance)
        } finally { db.close() }
    }
    @Test fun largeMultipageReportWrapsVeryLongGuestNamesSetDefinitionsAndNotes() = runBlocking {
        val context = ApplicationProvider.getApplicationContext<Context>(); val db = Room.inMemoryDatabaseBuilder(context, AppDatabase::class.java).build()
        try {
            val base = reportFixture(db, context)
            val longName = "NOME_LUNGO_" + "CaratteriSenzaSpazi".repeat(50)
            val longNote = "NOTA_INIZIO " + "Nota dimostrativa con testo esteso da impaginare senza perdita. ".repeat(220) + "NOTA_FINE"
            val many = (1..85).map { index -> base.bookings.first().copy(id = "row-$index", guestFirstName = "OspiteNumero$index", guestLastName = if (index == 42) longName else "Dimostrativo$index") }
            val snapshot = base.copy(id = "synthetic-large", bookings = many, taxes = emptyList(), movements = emptyList(), notes = longNote,
                expenses = base.expenses.map { if (it.kind == "LAUNDRY") it.copy(setDefinition = "Set personalizzato con asciugamani e biancheria ".repeat(90)) else it })
            val file = File("build/reports/m4/Elledue-multipagina-sintetico.pdf"); file.parentFile!!.mkdirs(); val pdf = ReportPdf(context)
            file.outputStream().use { pdf.write(snapshot, it) }; assertTrue(pdf.pageCount >= 8); assertTrue(file.length() > 10000)
        } finally { db.close() }
    }
}
