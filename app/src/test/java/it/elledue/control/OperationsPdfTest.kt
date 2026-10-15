package it.elledue.control

import android.content.Context
import androidx.room.Room
import androidx.test.core.app.ApplicationProvider
import com.tom_roush.pdfbox.pdmodel.PDDocument
import com.tom_roush.pdfbox.text.PDFTextStripper
import it.elledue.control.data.AppDatabase
import it.elledue.control.reports.*
import kotlinx.coroutines.runBlocking
import org.junit.Test
import org.junit.Assert.*
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config
import org.robolectric.annotation.GraphicsMode
import java.io.ByteArrayOutputStream
import java.io.File

@RunWith(RobolectricTestRunner::class)
@Config(sdk=[34],application=android.app.Application::class)
@GraphicsMode(GraphicsMode.Mode.NATIVE)
class OperationsPdfTest {
    @Test fun compactPdfSeparatesActualReimbursementsWithoutCountingOtherCleaningCosts()=runBlocking<Unit> {
        val context=ApplicationProvider.getApplicationContext<Context>();val db=Room.inMemoryDatabaseBuilder(context,AppDatabase::class.java).build()
        try {
            val original=reportFixture(db,context)
            val snapshot=original.copy(expenses=original.expenses+original.expenses.first{it.kind=="CLEANING"}.copy(id="non-reimbursable",actualCents=90000,reimbursable=false))
            assertEquals(17000L,snapshot.summary().balance)
            val bytes=ByteArrayOutputStream().also{ReportPdf(context).write(snapshot,it,ReportLayout.SUMMARY)}.toByteArray()
            val text=PDDocument.load(bytes).use {assertEquals(1,it.numberOfPages);PDFTextStripper().getText(it)}
            listOf("Pulizie","Lavanderia","Altre spese","24,00","10,00","5,00","39,00","170,00").forEach{assertTrue("Missing $it",text.contains(it))}
            assertFalse(text.contains("900,00"));assertEquals(17000L,original.summary().balance)
            File("build/reports/047/rendiconto-sintetico-test.pdf").apply{parentFile!!.mkdirs();writeBytes(bytes)}
        }finally{db.close()}
    }
}
