package it.elledue.control

import android.graphics.Bitmap
import android.graphics.Canvas
import androidx.compose.ui.test.junit4.AndroidComposeTestRule
import java.io.File

/** Test fixture windows only; never runs inside the normal application or captures user data. */
internal fun nativeCapture(compose:AndroidComposeTestRule<*,MainActivity>,name:String){
    compose.mainClock.advanceTimeBy(700);compose.waitForIdle()
    compose.runOnIdle{
        val view=compose.activity.window.decorView
        val bitmap=Bitmap.createBitmap(view.width,view.height,Bitmap.Config.ARGB_8888)
        view.draw(Canvas(bitmap))
        org.robolectric.shadows.ShadowDialog.getShownDialogs().lastOrNull{it.isShowing}?.window?.decorView?.draw(Canvas(bitmap))
        File("build/reports/screenshots/048-$name.png").apply{parentFile!!.mkdirs()}.outputStream().use{bitmap.compress(Bitmap.CompressFormat.PNG,100,it)}
        bitmap.recycle()
    }
}
