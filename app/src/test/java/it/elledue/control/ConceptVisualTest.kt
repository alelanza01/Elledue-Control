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
class ConceptVisualTest {
    @get:Rule val compose=createAndroidComposeRule<MainActivity>()
    private val app get()=compose.activity.application as ElledueApplication
    private val today=BookingDates.today();private val month=YearMonth.from(today).toString()
    @Before fun seed()=runBlocking<Unit> {
        val photo=File(compose.activity.cacheDir,"test-coast.jpg").apply { writeBytes(File("src/test/resources/concept/coastal-scene.jpg").readBytes()) }
        val image=LocalImages.import(compose.activity,android.net.Uri.fromFile(photo))
        listOf("Casa Dimostrativa","Dimora di Test","Residenza Esempio").forEachIndexed { i,name ->
            val p=PropertyDraft(id="visual-p$i",ownerId="visual-o$i",name=name,address="Località dimostrativa",ownerName="Proprietario di test ${i+1}",maxGuests="4",beds="2")
            app.propertyRepository.save(p);app.propertyRepository.setCover(p.id,image)
            val start=if(i==1) today.minusDays(3) else today.plusDays(i.toLong())
            app.reservationRepository.save(BookingDraft(id="visual-b$i",propertyId=p.id,firstName="Ospite",lastName="Esempio ${i+1}",guests="3",checkIn=BookingDates.display(start.toEpochDay()),checkOut=BookingDates.display((if(i==1)today else start.plusDays(4)).toEpochDay()),gross="1000",actualOta="195"))
        }
        app.settingsRepository.saveProfile("Operatore","Dimostrativo",null)
        app.accountingRepository.initialize();app.accountingRepository.saveTax("visual-b0",TaxDraft("3","0","4"))
        app.accountingRepository.saveExpense(ExpenseDraft(id="visual-cleaning",propertyId="visual-p0",reservationId="visual-b0",date=BookingDates.display(today.toEpochDay()),month=month,categoryId="cleaning",quantity="3",unit="8"))
        app.activityRepository.save(ActivityEntity("visual-task","visual-p0","Verifica istruzioni di accesso",today.atTime(17,0).atZone(java.time.ZoneId.of("Europe/Rome")).toInstant().toEpochMilli(),reservationId="visual-b0"))
    }
    private fun waitTag(tag:String) {compose.waitUntil(20000){compose.onAllNodes(hasTestTag(tag)).fetchSemanticsNodes().isNotEmpty()}}
    private fun back(){compose.runOnUiThread{compose.activity.onBackPressedDispatcher.onBackPressed()}}
    private fun screenshot(name:String){
        if(!name.contains("pdf-")) {
            compose.activityRule.scenario.recreate()
            compose.waitUntil(20000) { compose.onAllNodes(hasContentDescription("Intestazione Elledue")).fetchSemanticsNodes().isNotEmpty() }
        }
        val tag=when {
            name.contains("home")->"home-scroll"
            name.contains("calendar")->"booking-calendar-mode"
            name.contains("portfolio")->"new-property"
            name=="activities"->"activities-scroll"
            name=="economy"->"open-reports"
            name=="reports"->"generate-report"
            name=="report-document"->"report-balance"
            else->"pdf-pages"
        }
        waitTag(tag)
        if(name.contains("pdf-")) compose.waitUntil(30000) { compose.onAllNodes(hasContentDescription("Pagina 1 del PDF") and hasAnyAncestor(hasTestTag("pdf-pages"))).fetchSemanticsNodes().isNotEmpty() }
        if(name=="report-document") compose.waitUntil(30000) { compose.onAllNodes(hasContentDescription("Pagina 1 del PDF")).fetchSemanticsNodes().isNotEmpty() }
        compose.mainClock.advanceTimeBy(600);compose.waitForIdle();compose.runOnIdle{
        val root=compose.activity.window.decorView;val bitmap=Bitmap.createBitmap(root.width,root.height,Bitmap.Config.ARGB_8888);root.draw(Canvas(bitmap))
        org.robolectric.shadows.ShadowDialog.getShownDialogs().lastOrNull{it.isShowing}?.window?.decorView?.draw(Canvas(bitmap))
        File("build/reports/screenshots/concept-$name.png").apply{parentFile!!.mkdirs()}.outputStream().use{bitmap.compress(Bitmap.CompressFormat.PNG,100,it)};bitmap.recycle()
    }}
    private fun assertWarmText(text:String) {
        val bounds=compose.onNodeWithText(text).fetchSemanticsNode().boundsInRoot
        compose.runOnIdle {
            val root=compose.activity.window.decorView; val bitmap=Bitmap.createBitmap(root.width,root.height,Bitmap.Config.ARGB_8888);root.draw(Canvas(bitmap))
            var ink=0
            for(y in bounds.top.toInt().coerceAtLeast(0) until bounds.bottom.toInt().coerceAtMost(bitmap.height)) for(x in bounds.left.toInt().coerceAtLeast(0) until bounds.right.toInt().coerceAtMost(bitmap.width)) if(bitmap.getPixel(x,y)==android.graphics.Color.rgb(246,242,234)) ink++
            bitmap.recycle();assertTrue("Heading must have visible warm-white ink on navy",ink>20)
        }
    }
    @Test fun richNativeScreensFollowConceptAndPdfPreviewRemainsReal() {
        waitTag("home-scroll");screenshot("home")
        compose.onNodeWithTag("nav-calendar").performClick();waitTag("booking-calendar-mode");screenshot("calendar")
        compose.onNodeWithContentDescription("Seleziona immobile").performClick()
        compose.onNode(hasText("Casa Dimostrativa") and hasAnyAncestor(isPopup())).performClick()
        screenshot("calendar-single-property")
        compose.onNodeWithTag("nav-properties").performClick();waitTag("new-property");screenshot("portfolio")
        compose.onNodeWithTag("nav-dashboard").performClick();compose.onNodeWithTag("home-scroll").performScrollToNode(hasTestTag("nav-activities"));compose.onNodeWithTag("nav-activities").performClick();waitTag("activities-scroll");compose.mainClock.advanceTimeBy(600);compose.waitForIdle();assertWarmText("Attività");screenshot("activities");back()
        compose.onNodeWithTag("nav-economy").performClick();waitTag("open-reports");screenshot("economy")
        compose.onNodeWithTag("open-reports").performClick();waitTag("generate-report");screenshot("reports")
        compose.onNodeWithTag("generate-report").performClick();waitTag("report-balance")
        compose.waitUntil(30000){compose.onAllNodes(hasContentDescription("Pagina 1 del PDF")).fetchSemanticsNodes().isNotEmpty()}
        screenshot("report-document")
        compose.onNodeWithTag("report-detail-scroll").performScrollToNode(hasTestTag("open-pdf-preview"));compose.onNodeWithTag("open-pdf-preview").performClick();waitTag("pdf-pages")
        compose.waitUntil(30000){compose.onAllNodes(hasTestTag("preview-export") and isEnabled()).fetchSemanticsNodes().isNotEmpty()};screenshot("pdf-preview")
        compose.onNodeWithTag("preview-detailed").performClick();compose.waitUntil(30000){compose.onAllNodes(hasTestTag("preview-export") and isEnabled()).fetchSemanticsNodes().isNotEmpty()};screenshot("pdf-detailed-preview")
        compose.onNodeWithTag("preview-export").assertIsEnabled()
    }
    @Test @Config(qualifiers="w320dp-h640dp-xxhdpi",fontScale=1.5f)
    fun darkPrimaryThemeAndAccessibleNavigationRemainOnSmallLightSystemPhone(){
        waitTag("home-scroll");compose.onNodeWithTag("quick-action").assertIsDisplayed();screenshot("small-home")
        compose.onNodeWithTag("nav-calendar").performClick();waitTag("booking-calendar-mode");screenshot("small-calendar")
        compose.onNodeWithTag("nav-properties").performClick();waitTag("new-property");screenshot("small-portfolio")
    }
}
