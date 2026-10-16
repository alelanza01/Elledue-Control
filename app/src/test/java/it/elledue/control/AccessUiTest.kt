package it.elledue.control

import android.app.Activity
import android.app.KeyguardManager
import android.content.Intent
import androidx.compose.ui.test.*
import androidx.compose.ui.test.junit4.createAndroidComposeRule
import it.elledue.control.security.LockSettings
import org.junit.*
import org.junit.Assert.*
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.Shadows
import org.robolectric.annotation.Config
import org.robolectric.annotation.GraphicsMode

@RunWith(RobolectricTestRunner::class)
@Config(sdk=[34],qualifiers="w320dp-h640dp-night-xxhdpi",fontScale=1.3f)
@GraphicsMode(GraphicsMode.Mode.NATIVE)
class AccessUiTest {
    @get:Rule val compose=createAndroidComposeRule<MainActivity>()
    private fun waitTag(tag:String){compose.waitUntil(20000){compose.onAllNodes(hasTestTag(tag)).fetchSemanticsNodes().isNotEmpty()}}
    @Test fun unavailableCredentialAndCancelledAuthenticationNeverUnlockOrRemoveStoredData(){
        val activity=compose.activity;val app=activity.application as ElledueApplication
        waitTag("home-scroll");app.appLock.configureAuthenticated(LockSettings(true,0,true));app.appLock.lock();waitTag("app-locked")
        compose.onNodeWithTag("nav-calendar").assertDoesNotExist()
        var message:String?=null;var success=false
        compose.runOnUiThread{activity.authenticate(true,{success=true},{message=it})}
        assertFalse(success);assertNotNull(message);assertFalse(app.appLock.unlocked.value)
        val keyguard=app.getSystemService(KeyguardManager::class.java)
        Shadows.shadowOf(keyguard).setIsDeviceSecure(true)
        compose.runOnUiThread{activity.authenticate(true,{success=true;app.appLock.grant()},{message=it})}
        val started=Shadows.shadowOf(activity).nextStartedActivityForResult
        assertNotNull("A real Android credential intent must be launched",started)
        compose.runOnUiThread{Shadows.shadowOf(activity).receiveResult(started.intent,Activity.RESULT_CANCELED,null)}
        assertFalse(success);waitTag("app-locked");assertTrue(app.appLock.settings.value.enabled)
        compose.runOnUiThread{activity.authenticate(true,{success=true;app.appLock.grant()},{message=it})}
        val second=Shadows.shadowOf(activity).nextStartedActivityForResult
        compose.runOnUiThread{Shadows.shadowOf(activity).receiveResult(second.intent,Activity.RESULT_OK,Intent())}
        waitTag("home-scroll");assertTrue(success)
        app.appLock.configureAuthenticated(LockSettings(false))
    }
}
