package it.elledue.control

import androidx.compose.ui.test.*
import androidx.compose.ui.test.junit4.createAndroidComposeRule
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config
import org.robolectric.annotation.GraphicsMode
import android.graphics.Bitmap
import android.graphics.Canvas
import java.io.File

@RunWith(RobolectricTestRunner::class)
@Config(sdk = [34], qualifiers = "w411dp-h891dp-xxhdpi")
@GraphicsMode(GraphicsMode.Mode.NATIVE)
class AppNavigationTest {
    @get:Rule val compose = createAndroidComposeRule<MainActivity>()
    private fun waitText(text: String) {
        compose.waitUntil(15_000) { compose.onAllNodes(hasText(text)).fetchSemanticsNodes().isNotEmpty() }
    }

    private fun screenshot(name: String, expectedBackground: Int? = null) {
        compose.runOnIdle {
            val view = compose.activity.window.decorView
            val bitmap = Bitmap.createBitmap(view.width, view.height, Bitmap.Config.ARGB_8888)
            view.draw(Canvas(bitmap))
            if (expectedBackground != null) org.junit.Assert.assertEquals(expectedBackground, bitmap.getPixel(view.width / 2, view.height * 3 / 4))
            val file = File("build/reports/screenshots/$name.png")
            file.parentFile!!.mkdirs()
            file.outputStream().use { bitmap.compress(Bitmap.CompressFormat.PNG, 100, it) }
            bitmap.recycle()
        }
    }

    @Test fun allMainSectionsAndSettingsAreAccessible() {
        compose.onNodeWithTag("nav-calendar").performClick()
        compose.onNodeWithText("Prenotazioni e soggiorni").assertIsDisplayed()
        compose.onNodeWithTag("nav-economy").performClick()
        compose.onNodeWithText("Rendiconti e contabilità").assertIsDisplayed()
        compose.onNodeWithTag("nav-activities").performClick()
        compose.onNodeWithText("La tua operatività").assertIsDisplayed()
        compose.onNodeWithTag("nav-settings").performClick()
        waitText("Nome organizzazione")
        compose.onNodeWithText("Nome organizzazione").performTextReplacement("Elledue Management")
        compose.onNodeWithText("Salva").performClick()
        waitText("Impostazioni salvate")
        compose.onNodeWithTag("nav-dashboard").performClick()
        compose.onNodeWithText("La tua gestione").assertIsDisplayed()
        screenshot("dashboard-empty")
    }

    @Test
    @Config(sdk = [34], qualifiers = "w411dp-h891dp-night-xxhdpi")
    fun darkThemeRendersWithOfficialNightBackground() {
        waitText("Aggiungi un immobile")
        screenshot("dashboard-dark", android.graphics.Color.rgb(17, 27, 50))
    }

    @Test fun propertyCanBeCreatedEditedArchivedAndReactivatedThroughUi() {
        compose.onNodeWithTag("nav-properties").performClick()
        compose.waitUntil(15_000) { compose.onAllNodes(hasTestTag("new-property")).fetchSemanticsNodes().isNotEmpty() }
        compose.onNodeWithTag("new-property").performClick()
        compose.onNodeWithTag("save-property").performClick()
        waitText("Inserisci il nome dell’immobile.")
        compose.onNodeWithTag("property-name").performTextReplacement("Casa UI")
        compose.onNodeWithTag("property-address").performTextReplacement("Via Test 1")
        compose.onNodeWithTag("property-owner").performTextReplacement("Proprietario UI")
        compose.activityRule.scenario.recreate()
        compose.onNodeWithTag("property-name").assertTextContains("Casa UI")
        compose.onNodeWithTag("save-property").performClick()
        waitText("Scheda immobile")
        compose.onNodeWithText("Casa UI").assertIsDisplayed()
        compose.onNodeWithText("Modifica").performClick()
        compose.onNodeWithTag("property-name").performTextReplacement("Casa Aggiornata")
        compose.onNodeWithTag("save-property").performClick()
        waitText("Casa Aggiornata")
        compose.onNodeWithTag("archive-property").performScrollTo().performClick()
        compose.onNodeWithText("Conferma archiviazione").performClick()
        waitText("Archiviato · storico conservato")
        compose.onNodeWithContentDescription("Indietro").performClick()
        compose.onNodeWithTag("archived-filter").performClick()
        compose.onNodeWithText("Casa Aggiornata").performClick()
        compose.onNodeWithTag("archive-property").performScrollTo().performClick()
        waitText("Archivia immobile")
        compose.activityRule.scenario.recreate()
        waitText("Scheda immobile")
        compose.onNodeWithText("Casa Aggiornata").performScrollTo().assertIsDisplayed()
        screenshot("property-detail")
    }
}
