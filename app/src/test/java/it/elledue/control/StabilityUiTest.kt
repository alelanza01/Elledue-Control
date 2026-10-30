package it.elledue.control

import android.content.Intent
import androidx.compose.ui.test.*
import androidx.compose.ui.test.junit4.createAndroidComposeRule
import androidx.test.core.app.ActivityScenario
import it.elledue.control.data.*
import it.elledue.control.domain.*
import kotlinx.coroutines.runBlocking
import kotlinx.coroutines.flow.first
import org.junit.*
import org.junit.Assert.*
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config
import org.robolectric.annotation.GraphicsMode

@RunWith(RobolectricTestRunner::class)
@Config(sdk=[34],qualifiers="w411dp-h891dp-night-xxhdpi")
@GraphicsMode(GraphicsMode.Mode.NATIVE)
class StabilityUiTest {
    @get:Rule val compose=createAndroidComposeRule<MainActivity>()
    private lateinit var app:ElledueApplication
    @Before fun seed()=runBlocking<Unit>{
        app=compose.activity.application as ElledueApplication
        app.propertyRepository.save(PropertyDraft(id="p",ownerId="o",name="Casa sintetica",address="Test",ownerName="Proprietario",maxGuests="6"))
        app.reservationRepository.save(BookingDraft(id="b",propertyId="p",firstName="Originale",lastName="Test",gross="1000",actualOta="195"))
    }
    private fun waitTag(tag:String){compose.waitUntil(20000){compose.onAllNodes(hasTestTag(tag)).fetchSemanticsNodes().isNotEmpty()}}
    private fun editStay(){waitTag("home-scroll");compose.onNodeWithTag("nav-calendar").performClick();waitTag("booking-list-mode");compose.onNodeWithTag("booking-list-mode").performClick();compose.onNodeWithTag("bookings-scroll").performScrollToNode(hasTestTag("booking-row-b"));compose.onNodeWithTag("booking-row-b").performClick();waitTag("booking-status");compose.onNodeWithText("Modifica").performClick();waitTag("booking-first-name")}
    @Test fun fivePlusEditedFieldsAndFinancialReasonSurviveClosingAllActivitiesAndResumingADurableDraft(){
        editStay()
        compose.onNodeWithTag("booking-first-name").performTextReplacement("Nuovo")
        compose.onNodeWithTag("booking-last-name").performTextReplacement("Persistente")
        compose.onNodeWithTag("booking-guests").performTextReplacement("4")
        compose.onNodeWithTag("booking-exempt-guests").performTextReplacement("1")
        compose.onNodeWithTag("booking-actual-ota").performScrollTo().performTextReplacement("150")
        compose.onNodeWithText("Arrivo previsto").performScrollTo().performTextReplacement("17:30")
        compose.onNodeWithText("Note").performScrollTo().performTextReplacement("Nota dopo riavvio")
        compose.onNodeWithTag("booking-correction-reason").performScrollTo().performTextReplacement("OTA effettiva verificata")
        compose.activityRule.scenario.close()
        ActivityScenario.launch<MainActivity>(Intent(app,MainActivity::class.java)).use { fresh ->
            waitTag("home-scroll");compose.onNodeWithTag("home-scroll").performScrollToNode(hasTestTag("open-drafts"));compose.onNodeWithTag("open-drafts").performClick();waitTag("resume-draft-booking/b/edit")
            compose.onNodeWithTag("resume-draft-booking/b/edit").performClick();waitTag("booking-first-name")
            compose.onNodeWithTag("booking-first-name").assertTextContains("Nuovo")
            compose.onNodeWithTag("booking-last-name").assertTextContains("Persistente")
            compose.onNodeWithTag("save-booking").performClick();waitTag("booking-status")
            runBlocking {
                val saved=app.reservationRepository.get("b")!!;assertEquals("Nuovo",saved.guestFirstName);assertEquals("Persistente",saved.guestLastName);assertEquals(4,saved.guestCount);assertEquals(3,saved.taxableGuests)
                assertEquals("17:30",saved.expectedArrivalTime);assertEquals("Nota dopo riavvio",saved.notes);assertEquals(15000L,saved.actualOtaCents);assertEquals(17000L,saved.managementCents)
                assertEquals(1800L,app.accountingRepository.assessments.first().single().expectedCents)
            }
            assertFalse(app.drafts.has("booking/b/edit"));fresh.recreate();waitTag("booking-status")
            compose.onNodeWithText("Nuovo Persistente").assertExists()
        }
    }
    @Test fun financialChangesAskForVisibleReasonAndConfirmOnlyAfterTheActualUpdate(){
        editStay();compose.onNodeWithTag("booking-actual-ota").performScrollTo().performTextReplacement("150")
        compose.onNodeWithTag("save-booking").performClick();waitTag("booking-correction-dialog")
        assertEquals(19500L,runBlocking{app.reservationRepository.get("b")!!.actualOtaCents})
        compose.onNodeWithTag("booking-correction-dialog").performTextReplacement("Importo OTA verificato")
        compose.onNodeWithText("Salva rettifica").performClick();waitTag("booking-status")
        assertEquals(15000L,runBlocking{app.reservationRepository.get("b")!!.actualOtaCents});assertFalse(app.drafts.has("booking/b/edit"))
    }

}
