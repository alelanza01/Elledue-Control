package it.elledue.control.security

import android.content.Context
import android.os.SystemClock
import kotlinx.coroutines.flow.MutableStateFlow

data class LockSettings(val enabled:Boolean=false,val timeoutSeconds:Long=60,val deviceCredentialPreferred:Boolean=false)
/** Access control only. The OS owns credentials; this file contains no PIN or biometric material. */
class AppLock(context:Context) {
    private val prefs=context.getSharedPreferences("app-access-v1",Context.MODE_PRIVATE)
    val settings=MutableStateFlow(LockSettings(prefs.getBoolean("enabled",false),prefs.getLong("timeout",60),prefs.getBoolean("device",false)))
    val unlocked=MutableStateFlow(!settings.value.enabled)
    private var stoppedAt:Long?=null
    fun stop(now:Long=SystemClock.elapsedRealtime()){if(settings.value.enabled)stoppedAt=now}
    fun start(now:Long=SystemClock.elapsedRealtime()) {
        val previous=stoppedAt;stoppedAt=null
        if(settings.value.enabled && previous!=null && now-previous>=settings.value.timeoutSeconds*1000)unlocked.value=false
    }
    fun grant(){unlocked.value=true;stoppedAt=null}
    /** Call only from an authenticated platform callback. */
    internal fun configureAuthenticated(value:LockSettings){
        require(value.timeoutSeconds in listOf(0L,30L,60L,300L,900L))
        check(prefs.edit().putBoolean("enabled",value.enabled).putLong("timeout",value.timeoutSeconds).putBoolean("device",value.deviceCredentialPreferred).commit())
        settings.value=value;grant()
    }
    fun lock(){if(settings.value.enabled)unlocked.value=false}
}
