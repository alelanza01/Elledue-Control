package it.elledue.control

import android.graphics.Bitmap
import android.graphics.Canvas
import androidx.compose.ui.test.*
import androidx.compose.ui.test.junit4.createAndroidComposeRule
import it.elledue.control.data.*
import it.elledue.control.domain.*
import kotlinx.coroutines.runBlocking
import org.junit.*
import org.junit.Assert.*
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config
import org.robolectric.annotation.GraphicsMode
import java.io.File
import java.time.YearMonth

@RunWith(RobolectricTestRunner::class)
@Config(sdk=[34],qualifiers="w411dp-h891dp-night-xxhdpi")
@GraphicsMode(GraphicsMode.Mode.NATIVE)
class OwnershipUiTest {
    @get:Rule val compose=createAndroidComposeRule<MainActivity>()
    private val app get()=compose.activity.application as ElledueApplication
    private fun waitTag(tag:String){compose.waitUntil(20000){compose.onAllNodes(hasTestTag(tag)).fetchSemanticsNodes().isNotEmpty()}}
    @Before fun seed()=runBlocking<Unit>{
        val managed=PropertyDraft(id="m",ownerId="mo",name="Gestito sintetico",address="Test",ownerName="Test",maxGuests="4")
        val own=managed.copy(id="o",ownerId="oo",name="Proprio sintetico",businessType="OWNED")
        val archived=managed.copy(id="a",ownerId="ao",name="Archiviato sintetico")
        listOf(managed,own,archived).forEach{app.propertyRepository.save(it)}
        app.reservationRepository.save(BookingDraft(id="bm",propertyId="m",firstName="Ospite",lastName="Gestione",gross="1000",actualOta="195"))
        app.reservationRepository.save(BookingDraft(id="bo",propertyId="o",firstName="Ospite",lastName="Proprio",gross="500",actualOta="75").withPropertyDefaults(app.propertyRepository.get("o")!!))
        app.reservationRepository.save(BookingDraft(id="ba",propertyId="a",firstName="Ospite",lastName="Archivio",gross="9999",actualOta="0"));app.propertyRepository.archive("a",true)
        app.accountingRepository.initialize();app.accountingRepository.saveExpense(ExpenseDraft(id="own-cost",propertyId="o",categoryId="cleaning",quantity="3",unit="8",month=YearMonth.from(BookingDates.today()).toString(),advanced=false,reimbursable=false))
    }
    @Test fun explicitBusinessScopesKeepOwnProfitSeparateAndExternalReportsDisabled(){
        waitTag("home-scroll");compose.onNodeWithTag("business-OWNED").performClick();waitTag("home-scroll")
        compose.onNodeWithTag("nav-economy").performClick();waitTag("economy-scroll")
        compose.onNodeWithTag("open-reports").assertIsNotEnabled()
        compose.onNodeWithTag("economy-scroll").performScrollToNode(hasText("Risultato operativo complessivo"))
        compose.onNodeWithText(Amounts.euro(40100)).assertExists();compose.onNodeWithText(Amounts.euro(16100)).assertDoesNotExist()
        compose.onNodeWithTag("business-ALL").performClick();waitTag("economy-scroll")
        compose.onNodeWithTag("economy-scroll").performScrollToNode(hasText("Compenso di gestione Elledue"))
        compose.onAllNodesWithText(Amounts.euro(16100)).assertCountEquals(2) // fee and managed balance, both 161; own profit is separate
        compose.onNodeWithTag("economy-scroll").performScrollToNode(hasText("Risultato operativo complessivo"))
        compose.onNodeWithText(Amounts.euro(40100)).assertExists();compose.onNodeWithText(Amounts.euro(999900)).assertDoesNotExist()
        compose.mainClock.advanceTimeBy(700);compose.waitForIdle();compose.runOnIdle{
            val view=compose.activity.window.decorView;val bitmap=Bitmap.createBitmap(view.width,view.height,Bitmap.Config.ARGB_8888);view.draw(Canvas(bitmap))
            File("build/reports/screenshots/048-owned-result.png").apply{parentFile!!.mkdirs()}.outputStream().use{bitmap.compress(Bitmap.CompressFormat.PNG,100,it)};bitmap.recycle()
        }
    }
}
