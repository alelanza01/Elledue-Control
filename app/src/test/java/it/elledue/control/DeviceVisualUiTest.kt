package it.elledue.control

import android.app.Activity
import android.content.Intent
import android.net.Uri
import androidx.compose.ui.test.*
import androidx.compose.ui.test.junit4.createAndroidComposeRule
import it.elledue.control.data.*
import it.elledue.control.domain.*
import kotlinx.coroutines.runBlocking
import org.junit.*
import org.junit.Assert.*
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.Shadows
import org.robolectric.annotation.Config
import org.robolectric.annotation.GraphicsMode
import java.io.File

@RunWith(RobolectricTestRunner::class)
@Config(sdk=[34],qualifiers="w411dp-h891dp-night-xxhdpi")
@GraphicsMode(GraphicsMode.Mode.NATIVE)
class DeviceVisualUiTest {
    @get:Rule val compose=createAndroidComposeRule<MainActivity>()
    private val app get()=compose.activity.application as ElledueApplication
    private fun waitTag(tag:String){compose.waitUntil(20000){compose.onAllNodes(hasTestTag(tag)).fetchSemanticsNodes().isNotEmpty()}}
    @Test fun nativeNotificationSettingsOwnedPropertyAndVerifiedSafDeletionAreUsable() {
        runBlocking {
            app.propertyRepository.save(PropertyDraft(id="owned",ownerId="own-ref",name="Proprio di test",address="Indirizzo sintetico",businessType="OWNED",maxGuests="4"))
            app.reservationRepository.save(BookingDraft(id="stay",propertyId="owned",firstName="Ospite",lastName="Sintetico",gross="500",actualOta="75").withPropertyDefaults(app.propertyRepository.get("owned")!!))
        }
        waitTag("home-scroll");compose.onNodeWithContentDescription("Centro notifiche").performClick();waitTag("notification-settings")
        nativeCapture(compose,"notification-settings")
        compose.onNodeWithTag("save-notifications").performScrollTo().performClick()
        compose.waitUntil(20000){compose.onAllNodes(hasText("Impostazioni salvate e promemoria aggiornati")).fetchSemanticsNodes().isNotEmpty()}
        compose.onNodeWithText("Indietro").performScrollTo().performClick();waitTag("home-scroll")
        compose.onNodeWithTag("nav-properties").performClick();waitTag("new-property")
        compose.onNodeWithText("Proprio di test").performClick();waitTag("property-menu")
        compose.onNodeWithText("Immobile proprio / in comproprietà").performScrollTo();nativeCapture(compose,"owned-property")
        compose.onNodeWithTag("property-menu").performClick();compose.onNodeWithText("Elimina definitivamente immobile").performClick();waitTag("delete-permanently")
        compose.onNodeWithTag("deletion-confirmation").performScrollTo().performTextReplacement("ELIMINA Proprio di test")
        compose.onNodeWithTag("delete-permanently").assertIsNotEnabled();nativeCapture(compose,"deletion-before-backup")
        compose.onNodeWithTag("deletion-backup").performScrollTo().performClick()
        val activity=compose.activity;val started=Shadows.shadowOf(activity).nextStartedActivityForResult
        assertEquals(Intent.ACTION_CREATE_DOCUMENT,started.intent.action)
        val file=File(activity.cacheDir,"synthetic-safety.json")
        compose.runOnUiThread{Shadows.shadowOf(activity).receiveResult(started.intent,Activity.RESULT_OK,Intent().setData(Uri.fromFile(file)))}
        compose.waitUntil(20000){compose.onAllNodes(hasText("Backup verificato e accessibile nel documento scelto.")).fetchSemanticsNodes().isNotEmpty()}
        assertTrue(file.length()>100);compose.onNodeWithTag("delete-permanently").performScrollTo().assertIsEnabled();nativeCapture(compose,"deletion-verified")
        compose.onNodeWithTag("delete-permanently").performClick();try{waitTag("new-property")}catch(e:Throwable){throw AssertionError(compose.onRoot().printToString(),e)}
        runBlocking {assertNull(app.propertyRepository.get("owned"));assertNull(app.reservationRepository.get("stay"))}
    }
}
