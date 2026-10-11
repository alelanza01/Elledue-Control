package it.elledue.control

import android.os.Bundle
import android.content.Context
import android.content.res.Configuration
import java.util.Locale
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.core.splashscreen.SplashScreen.Companion.installSplashScreen
import it.elledue.control.ui.PropertyViewModel
import it.elledue.control.ui.ReservationViewModel
import androidx.lifecycle.viewmodel.compose.viewModel
import it.elledue.control.ui.ElledueApp
import it.elledue.control.ui.ElledueTheme
import it.elledue.control.ui.SettingsViewModel

class MainActivity : ComponentActivity() {
    override fun attachBaseContext(newBase: Context) {
        val configuration = Configuration(newBase.resources.configuration)
        configuration.setLocale(Locale.ITALIAN)
        super.attachBaseContext(newBase.createConfigurationContext(configuration))
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        installSplashScreen()
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        val repository = (application as ElledueApplication).settingsRepository
        setContent {
            ElledueTheme {
                val model: SettingsViewModel = viewModel(factory = SettingsViewModel.Factory(repository))
                val properties: PropertyViewModel = viewModel(factory = PropertyViewModel.Factory((application as ElledueApplication).propertyRepository))
                val bookings: ReservationViewModel = viewModel(factory = ReservationViewModel.Factory((application as ElledueApplication).reservationRepository))
                val accounting: it.elledue.control.ui.AccountingViewModel = viewModel(factory = it.elledue.control.ui.AccountingViewModel.Factory((application as ElledueApplication).accountingRepository))
                ElledueApp(model, properties, bookings, accounting)
            }
        }
    }
}
