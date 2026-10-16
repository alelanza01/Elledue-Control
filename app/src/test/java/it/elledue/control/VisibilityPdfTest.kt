package it.elledue.control

import android.content.Context
import androidx.room.Room
import androidx.test.core.app.ApplicationProvider
import com.tom_roush.pdfbox.pdmodel.PDDocument
import com.tom_roush.pdfbox.text.PDFTextStripper
import it.elledue.control.data.*
import it.elledue.control.reports.*
import kotlinx.coroutines.runBlocking
import org.junit.Test
import org.junit.Assert.*
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config
import org.robolectric.annotation.GraphicsMode
import java.io.ByteArrayOutputStream

@RunWith(RobolectricTestRunner::class)
@Config(sdk=[34],application=android.app.Application::class)
@GraphicsMode(GraphicsMode.Mode.NATIVE)
class VisibilityPdfTest {
    @Test fun hiddenNominalBookingIsAbsentFromBothActualPdfLayoutsWhileTotalsAndFrozenOriginalRemain()=runBlocking<Unit>{
        val context=ApplicationProvider.getApplicationContext<Context>()
        Room.inMemoryDatabaseBuilder(context,AppDatabase::class.java).build().withClosing{db->
            val original=reportFixture(db,context);val repo=ReportRepository(db,context);val revised=repo.beginRevision(original.id,"Test visibilità")
            val booking=original.bookings.single();repo.setBookingOption(revised,booking.id,true,false)
            val hidden=repo.snapshot(revised);assertEquals(original.summary(),hidden.summary())
            for(layout in ReportLayout.entries){
                val output=ByteArrayOutputStream();ReportPdf(context).write(hidden,output,layout)
                val text=PDDocument.load(output.toByteArray()).use{PDFTextStripper().getText(it)}
                assertFalse(text.contains(booking.guestFirstName+" "+booking.guestLastName));assertTrue(text.contains("non dettagliat"));assertTrue(text.contains("170,00"))
            }
            repo.setBookingOption(revised,booking.id,false,true);val excluded=repo.snapshot(revised)
            assertEquals(1500L,excluded.summary().balance);assertFalse(excluded.expenses.any{it.reservationId==booking.id});assertTrue(db.accountingDao().movements().isNotEmpty())
            assertEquals(original,repo.snapshot(original.id))
        }
    }
}
