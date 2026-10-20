package it.elledue.control

import android.os.Bundle
import android.content.Context
import android.content.res.Configuration
import java.util.Locale
import androidx.fragment.app.FragmentActivity
import androidx.biometric.BiometricPrompt
import androidx.biometric.BiometricManager
import androidx.core.content.ContextCompat
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.runtime.*
import androidx.lifecycle.lifecycleScope
import kotlinx.coroutines.launch
import android.content.Intent
import android.app.KeyguardManager
import android.os.Build
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.core.splashscreen.SplashScreen.Companion.installSplashScreen
import it.elledue.control.ui.PropertyViewModel
import it.elledue.control.ui.ReservationViewModel
import androidx.lifecycle.viewmodel.compose.viewModel
import it.elledue.control.ui.ElledueApp
import it.elledue.control.ui.ElledueTheme
import it.elledue.control.ui.SettingsViewModel

class MainActivity : FragmentActivity() {
    val notificationRoute = kotlinx.coroutines.flow.MutableStateFlow<String?>(null)
    private var credentialCallback: (() -> Unit)? = null
    private var credentialError: ((String) -> Unit)? = null
    private val credentials = registerForActivityResult(ActivityResultContracts.StartActivityForResult()) { result ->
        val success=credentialCallback;val failure=credentialError;credentialCallback=null;credentialError=null
        if(result.resultCode==RESULT_OK)success?.invoke() else failure?.invoke("Autenticazione annullata. I dati sono conservati.")
    }
    private fun credential(success:()->Unit,error:(String)->Unit) {
        val keyguard=getSystemService(KeyguardManager::class.java)
        val intent=keyguard.createConfirmDeviceCredentialIntent("Elledue Control","Conferma la credenziale del dispositivo")
        if(!keyguard.isDeviceSecure || intent==null){error("Configura un blocco schermo sicuro nelle impostazioni Android. I dati restano sul dispositivo.");return}
        credentialCallback=success;credentialError=error;credentials.launch(intent)
    }
    fun authenticate(devicePreferred:Boolean,success:()->Unit,error:(String)->Unit) {
        val keyguard=getSystemService(KeyguardManager::class.java)
        if(!keyguard.isDeviceSecure){error("Configura prima un blocco schermo sicuro nelle impostazioni Android.");return}
        val auth=BiometricManager.Authenticators.BIOMETRIC_WEAK
        if(devicePreferred || BiometricManager.from(this).canAuthenticate(auth)!=BiometricManager.BIOMETRIC_SUCCESS){credential(success,error);return}
        val callback=object:BiometricPrompt.AuthenticationCallback(){
            override fun onAuthenticationSucceeded(result:BiometricPrompt.AuthenticationResult){success()}
            override fun onAuthenticationError(code:Int,message:CharSequence){if(code==BiometricPrompt.ERROR_NEGATIVE_BUTTON)credential(success,error) else error("${message}. Puoi riprovare o usare la credenziale del dispositivo.")}
            override fun onAuthenticationFailed(){error("Riconoscimento non riuscito. Riprova o usa la credenziale del dispositivo.")}
        }
        val info=BiometricPrompt.PromptInfo.Builder().setTitle("Sblocca Elledue Control").setSubtitle("Accedi alla tua gestione")
        if(Build.VERSION.SDK_INT>=30)info.setAllowedAuthenticators(auth or BiometricManager.Authenticators.DEVICE_CREDENTIAL)
        else info.setAllowedAuthenticators(auth).setNegativeButtonText("Usa credenziale dispositivo")
        BiometricPrompt(this,ContextCompat.getMainExecutor(this),callback).authenticate(info.build())
    }
    private fun acceptNotification(intent:Intent?) {
        val route=intent?.getStringExtra("route") ?: return
        if(Regex("guest-message/[A-Za-z0-9_-]{1,100}/(pre|out)").matches(route) || route in listOf("reports","notifications") || Regex("(booking|activity|tax|issue)/[A-Za-z0-9_-]{1,100}").matches(route))notificationRoute.value=route
        intent.removeExtra("route")
    }
    override fun onNewIntent(intent:Intent){super.onNewIntent(intent);setIntent(intent);acceptNotification(intent)}
    override fun onStart(){super.onStart();(application as ElledueApplication).appLock.start()}
    override fun onStop(){(application as ElledueApplication).appLock.stop();super.onStop()}
    override fun onResume(){super.onResume();lifecycleScope.launch{runCatching{(application as ElledueApplication).reminders.reconcile()}}}

    override fun attachBaseContext(newBase: Context) {
        val configuration = Configuration(newBase.resources.configuration)
        configuration.setLocale(Locale.ITALIAN)
        super.attachBaseContext(newBase.createConfigurationContext(configuration))
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        installSplashScreen()
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        acceptNotification(intent)
        val repository = (application as ElledueApplication).settingsRepository
        setContent {
            ElledueTheme {
                val model: SettingsViewModel = viewModel(factory = SettingsViewModel.Factory(repository))
                val properties: PropertyViewModel = viewModel(factory = PropertyViewModel.Factory((application as ElledueApplication).propertyRepository))
                val bookings: ReservationViewModel = viewModel(factory = ReservationViewModel.Factory((application as ElledueApplication).reservationRepository))
                val accounting: it.elledue.control.ui.AccountingViewModel = viewModel(factory = it.elledue.control.ui.AccountingViewModel.Factory((application as ElledueApplication).accountingRepository))
                val app = application as ElledueApplication
                val reports: it.elledue.control.ui.ReportViewModel = viewModel(factory = it.elledue.control.ui.ReportViewModel.Factory(app.reportRepository))
                val unlocked by app.appLock.unlocked.collectAsState()
                val lock by app.appLock.settings.collectAsState()
                DisposableEffect(lock.enabled){
                    if(lock.enabled)window.addFlags(android.view.WindowManager.LayoutParams.FLAG_SECURE) else window.clearFlags(android.view.WindowManager.LayoutParams.FLAG_SECURE)
                    onDispose{}
                }
                if(unlocked)ElledueApp(model, properties, bookings, accounting, reports, app.backupService, this@MainActivity)
                else it.elledue.control.ui.LockedScreen(this@MainActivity)
            }
        }
    }
}
