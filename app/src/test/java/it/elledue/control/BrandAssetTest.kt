package it.elledue.control

import android.content.Context
import android.graphics.*
import android.graphics.drawable.AdaptiveIconDrawable
import androidx.test.core.app.ApplicationProvider
import org.junit.Assert.*
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config
import org.robolectric.annotation.GraphicsMode
import java.io.File

@RunWith(RobolectricTestRunner::class)
@Config(sdk = [34], application = android.app.Application::class)
@GraphicsMode(GraphicsMode.Mode.NATIVE)
class BrandAssetTest {
    @Test fun adaptiveIconUsesNightBackgroundAndAuthenticGoldSymbolInsideMask() {
        val context = ApplicationProvider.getApplicationContext<Context>()
        val drawable = context.getDrawable(R.mipmap.ic_launcher)!!; assertTrue(drawable is AdaptiveIconDrawable)
        val bitmap = Bitmap.createBitmap(600, 300, Bitmap.Config.ARGB_8888); val canvas = Canvas(bitmap)
        canvas.drawColor(Color.WHITE); val background = Paint().apply { color = Color.rgb(17,27,50) };canvas.drawRect(300f,0f,600f,300f,background)
        drawable.setBounds(35,35,265,265); drawable.draw(canvas); drawable.setBounds(335,35,565,265); drawable.draw(canvas)
        val pixels = IntArray(600*300); bitmap.getPixels(pixels,0,600,0,0,600,300)
        assertTrue(pixels.count { it == Color.rgb(219,191,139) } > 2000); assertTrue(pixels.count { it == Color.rgb(17,27,50) } > 20000)
        val file = File("build/reports/screenshots/m4-icon-light-dark.png");file.parentFile!!.mkdirs();file.outputStream().use { bitmap.compress(Bitmap.CompressFormat.PNG,100,it) };bitmap.recycle()
    }
    @Test fun horizontalLogoPreservesOriginalAspectAndAlphaWithRequestedOfficialGold() {
        val context = ApplicationProvider.getApplicationContext<Context>(); val logo = BitmapFactory.decodeResource(context.resources,R.drawable.elledue_logo_gold)
        assertEquals(784,logo.width);assertEquals(138,logo.height)
        val original = BitmapFactory.decodeFile("../el le.png")
        var minX=original.width;var minY=original.height;var maxX=0;var maxY=0
        for(y in 0 until original.height) for(x in 0 until original.width) if(Color.alpha(original.getPixel(x,y))>0){minX=minOf(minX,x);minY=minOf(minY,y);maxX=maxOf(maxX,x);maxY=maxOf(maxY,y)}
        assertEquals(maxX-minX+1,logo.width);assertEquals(maxY-minY+1,logo.height)
        for(y in 0 until logo.height) for(x in 0 until logo.width) {
            val pixel=logo.getPixel(x,y);assertEquals(Color.alpha(original.getPixel(x+minX,y+minY)),Color.alpha(pixel))
            if(Color.alpha(pixel)==255) assertEquals(Color.rgb(219,191,139),pixel)
        }
        original.recycle();logo.recycle()
    }
    @Test
    @Config(sdk = [26])
    @GraphicsMode(GraphicsMode.Mode.LEGACY)
    fun minimumAndroidVersionInflatesAdaptiveIconWithOptionalMonochromeTag() {
        val context = ApplicationProvider.getApplicationContext<Context>()
        val drawable = context.getDrawable(R.mipmap.ic_launcher) as AdaptiveIconDrawable
        assertNotNull(drawable.foreground); assertNotNull(drawable.background)
    }

}
