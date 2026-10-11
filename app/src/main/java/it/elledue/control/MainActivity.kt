package it.elledue.control

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.core.splashscreen.SplashScreen.Companion.installSplashScreen
import it.elledue.control.ui.PropertyViewModel
import androidx.lifecycle.viewmodel.compose.viewModel
import it.elledue.control.ui.ElledueApp
import it.elledue.control.ui.ElledueTheme
import it.elledue.control.ui.SettingsViewModel

class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        installSplashScreen()
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        val repository = (application as ElledueApplication).settingsRepository
        setContent {
            ElledueTheme {
                val model: SettingsViewModel = viewModel(factory = SettingsViewModel.Factory(repository))
                val properties: PropertyViewModel = viewModel(factory = PropertyViewModel.Factory((application as ElledueApplication).propertyRepository))
                ElledueApp(model, properties)
            }
        }
    }
}
