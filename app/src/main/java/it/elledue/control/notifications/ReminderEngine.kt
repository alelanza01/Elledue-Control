package it.elledue.control.notifications

import android.Manifest
import android.app.*
import android.content.*
import android.content.pm.PackageManager
import android.net.Uri
import android.os.Build
import androidx.core.app.NotificationCompat
import androidx.core.app.NotificationManagerCompat
import androidx.core.content.ContextCompat
import com.google.gson.Gson
import it.elledue.control.ElledueApplication
import it.elledue.control.MainActivity
import it.elledue.control.R
import kotlinx.coroutines.*
import kotlinx.coroutines.flow.first
import org.json.JSONObject
import java.time.Instant

class ReminderStore(context:Context) {
    private val prefs=context.getSharedPreferences("reminders-device-v1",Context.MODE_PRIVATE)
    val state=kotlinx.coroutines.flow.MutableStateFlow(load())
    val diagnostics=kotlinx.coroutines.flow.MutableStateFlow(0L)
    fun load():ReminderSettings=runCatching{Gson().fromJson(prefs.getString("settings",null),ReminderSettings::class.java) ?: ReminderSettings()}.getOrDefault(ReminderSettings())
    fun save(value:ReminderSettings){ReminderPlanner.validate(value);check(prefs.edit().putString("settings",Gson().toJson(value)).commit());state.value=value}
    fun scheduled():Map<String,Reminder> {
        val obj=JSONObject(prefs.getString("scheduled","{}")!!)
        return obj.keys().asSequence().associateWith{Gson().fromJson(obj.getString(it),Reminder::class.java)}
    }
    fun scheduled(value:Map<String,Reminder>){val json=JSONObject();value.forEach{(k,v)->json.put(k,Gson().toJson(v))};if(prefs.getString("scheduled","")!=json.toString()){check(prefs.edit().putString("scheduled",json.toString()).commit());diagnostics.value++}}
    fun diagnosticJob():Reminder?=runCatching{Gson().fromJson(prefs.getString("diagnostic-job",null),Reminder::class.java)}.getOrNull()
    fun diagnosticJob(job:Reminder){check(prefs.edit().putString("diagnostic-job",Gson().toJson(job)).commit());diagnostics.value++}
    fun status(key:String):String?=prefs.getString("diagnostic:$key",null)
    fun status(key:String,value:String){check(prefs.edit().putString("diagnostic:$key",value).commit());diagnostics.value++}
    fun handled(key:String)=prefs.getBoolean("handled:$key",false)
    fun handle(key:String){check(prefs.edit().putBoolean("handled:$key",true).commit())}
    fun delivered(key:String)=prefs.getString("sent:$key",null)
    fun history():List<Reminder> = prefs.all.keys.filter{it.startsWith("delivered-job:")}.mapNotNull{key->runCatching{Gson().fromJson(prefs.getString(key,null),Reminder::class.java)}.getOrNull()}
    fun mark(job:Reminder){check(prefs.edit().putString("sent:${job.key}",job.version).putString("delivered-job:${job.key}",Gson().toJson(job)).commit())}
}
class ReminderEngine(private val app:ElledueApplication) {
    private val store get()=app.reminderStore
    private val mutex=kotlinx.coroutines.sync.Mutex()
    fun permissionGranted()=Build.VERSION.SDK_INT<33 || ContextCompat.checkSelfPermission(app,Manifest.permission.POST_NOTIFICATIONS)==PackageManager.PERMISSION_GRANTED
    fun appNotificationsEnabled()=NotificationManagerCompat.from(app).areNotificationsEnabled()
    fun channelEnabled()=app.getSystemService(NotificationManager::class.java).getNotificationChannel(CHANNEL)?.importance!=NotificationManager.IMPORTANCE_NONE
    fun allowed():Boolean=permissionGranted() && appNotificationsEnabled() && channelEnabled()
    fun blockedReason():String?=when {
        !permissionGranted()->"Permesso Android notifiche negato: concedilo con il pulsante Permesso Android."
        !appNotificationsEnabled()->"Notifiche dell’app disabilitate nelle impostazioni Android."
        !channelEnabled()->"Canale Promemoria operativi disattivato: riattivalo nelle impostazioni del canale."
        else->null
    }
    fun channel(){app.getSystemService(NotificationManager::class.java).createNotificationChannel(NotificationChannel(CHANNEL,"Promemoria operativi",NotificationManager.IMPORTANCE_DEFAULT).apply{description="Prenotazioni, attività e rendiconti locali";lockscreenVisibility=Notification.VISIBILITY_PRIVATE})}
    private fun pending(job:Reminder):PendingIntent=PendingIntent.getBroadcast(app,0,Intent(app,ReminderReceiver::class.java).setAction(ACTION).setData(Uri.parse("elledue-reminder://local/${Uri.encode(job.key)}")).putExtra("key",job.key).putExtra("version",job.version),PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE)
    private suspend fun jobs(now:Instant):List<Reminder> {
        val tasks=app.activityRepository.activities.first()
        val issues=app.issueRepository.issues.first().filter{issue->it.elledue.control.data.IssueRepository.open(issue)}
        val issueTasks=issues.mapNotNull{i->i.scheduledAt?.let{at->it.elledue.control.data.ActivityEntity("issue-${i.id}",i.propertyId,i.title,at,kind="MAINTENANCE",hasTime=i.hasTime)}}
        val planned=ReminderPlanner.plan(app.propertyRepository.properties.first(),app.reservationRepository.reservations.first(),tasks+issueTasks,store.load(),now).map{job->if(job.key.startsWith("task:issue-"))job.copy(route="issue/"+job.key.removePrefix("task:issue-")) else job}
        val test=store.diagnosticJob()?.takeIf{store.load().enabled && it.at>=now.minusSeconds(48*3600).toEpochMilli()}
        return planned+listOfNotNull(test)
    }
    fun exactAllowed():Boolean=Build.VERSION.SDK_INT<31 || app.getSystemService(AlarmManager::class.java).canScheduleExactAlarms()
    private fun schedule(alarm:AlarmManager,job:Reminder):String {
        if(exactAllowed())try{alarm.setExactAndAllowWhileIdle(AlarmManager.RTC_WAKEUP,job.at,pending(job));return "EXACT"}catch(_:SecurityException){}
        alarm.setAndAllowWhileIdle(AlarmManager.RTC_WAKEUP,job.at,pending(job));return "INEXACT"
    }
    suspend fun reconcile(now:Instant=Instant.now(),force:Boolean=false) {
        mutex.lock();try {
            channel();val alarm=app.getSystemService(AlarmManager::class.java);val old=store.scheduled()
            val all=if(allowed())jobs(now) else emptyList()
            all.filter{it.at<=now.toEpochMilli() && old[it.key]?.version==it.version && store.delivered(it.key)!=it.version && !store.handled(it.key)}.forEach{if(show(it))store.mark(it)}
            val planned=all.filter{it.at>=now.toEpochMilli() && store.delivered(it.key)!=it.version && !store.handled(it.key)}.associateBy{it.key}
            old.forEach{(key,value)->if(planned[key]?.version!=value.version){alarm.cancel(pending(value));if(all.none{it.key==key && it.version==value.version})NotificationManagerCompat.from(app).cancel(key.hashCode())}}
            val accepted=mutableMapOf<String,Reminder>();val errors=mutableListOf<String>();var mode=if(exactAllowed())"EXACT" else "INEXACT"
            planned.forEach{(key,value)->
                try {
                    if(force || old[key]?.version!=value.version){val result=schedule(alarm,value);if(result=="INEXACT")mode=result}
                    accepted[key]=value
                }catch(e:Exception){errors.add("Programmazione rifiutata da Android (${e.javaClass.simpleName}). Verifica allarmi e restrizioni dell’app, poi riprogramma.")}
            }
            store.scheduled(accepted)
            store.status("last-schedule",now.toEpochMilli().toString());store.status("mode",mode)
            store.status("error",errors.distinct().joinToString(" ").ifBlank{blockedReason() ?: ""})
        }catch(e:CancellationException){throw e}catch(e:Exception){store.status("error","Aggiornamento promemoria non riuscito (${e.javaClass.simpleName}). Controlla gli orari e riprogramma.");throw e}finally{mutex.unlock()}
    }
    suspend fun deliver(key:String,version:String,now:Instant=Instant.now()) {
        mutex.lock();try {
            store.status("last-receiver",now.toEpochMilli().toString())
            if(!allowed() || !store.load().enabled){store.status("error",blockedReason() ?: "Promemoria disattivati nell’app.");return}
            val job=jobs(now).firstOrNull{it.key==key && it.version==version} ?: return
            if(job.at>now.plusSeconds(60).toEpochMilli() || now.toEpochMilli()-job.at>48*3600*1000L || store.delivered(key)==version || store.handled(key))return
            if(show(job))store.mark(job)
        }finally{mutex.unlock()}
    }
    fun test(){channel();check(allowed()){blockedReason() ?: "Notifiche bloccate da Android."};check(show(Reminder("test",System.currentTimeMillis(),"Elledue Control","Notifica locale di prova immediata. Nessun dato contabile modificato.","notifications","test"))){"Android non ha accettato la notifica: verifica la diagnostica."}}
    suspend fun scheduleTest(now:Instant=Instant.now()):Reminder {
        channel();check(allowed()){blockedReason() ?: "Notifiche bloccate da Android."};check(store.load().enabled){"Attiva prima i promemoria nell’app."}
        val id="diagnostic:"+java.util.UUID.randomUUID().toString()
        val job=Reminder(id,now.plusSeconds(120).toEpochMilli(),"Elledue · promemoria di prova","Il percorso allarme → ricezione → notifica è stato eseguito. Verifica sul telefono se lo hai visto.","notifications",id)
        store.diagnosticJob(job);reconcile(now,force=true)
        check(store.scheduled()[id]?.version==job.version){store.status("error")?.ifBlank{null} ?: "Programmazione non confermata da Android."};return job
    }
    private fun show(job:Reminder):Boolean {
        if(!allowed()){store.status("error",blockedReason() ?: "Notifiche bloccate.");return false}
        val intent=Intent(app,MainActivity::class.java).addFlags(Intent.FLAG_ACTIVITY_CLEAR_TOP or Intent.FLAG_ACTIVITY_SINGLE_TOP).setData(Uri.parse("elledue-open://local/${Uri.encode(job.key)}")).putExtra("route",job.route)
        val open=PendingIntent.getActivity(app,0,intent,PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE)
        return try {
            NotificationManagerCompat.from(app).notify(job.key.hashCode(),NotificationCompat.Builder(app,CHANNEL).setSmallIcon(R.drawable.elledue_notification).setContentTitle(if(app.appLock.settings.value.enabled)"Elledue Control" else job.title).setContentText(if(app.appLock.settings.value.enabled)"Promemoria operativo · sblocca per visualizzare" else job.text).setStyle(NotificationCompat.BigTextStyle().bigText(if(app.appLock.settings.value.enabled)"Promemoria operativo · sblocca per visualizzare" else job.text)).setVisibility(NotificationCompat.VISIBILITY_PRIVATE).setAutoCancel(true).setContentIntent(open).build())
            store.status("last-posted",System.currentTimeMillis().toString());if(job.key.startsWith("diagnostic:"))store.status("test-posted",System.currentTimeMillis().toString());true
        }catch(e:SecurityException){store.status("error","Android ha revocato il permesso durante l’invio. Riabilita le notifiche.");false}
    }
    companion object {const val CHANNEL="elledue_operations_v1";const val ACTION="it.elledue.control.REMINDER"}
}
class ReminderReceiver:BroadcastReceiver() {
    override fun onReceive(context:Context,intent:Intent) {
        val result=goAsync();val app=context.applicationContext as ElledueApplication
        app.background.launch{try{app.reminderStore.status("last-broadcast",intent.action ?: "Senza azione");if(intent.action==ReminderEngine.ACTION)app.reminders.deliver(intent.getStringExtra("key") ?: "",intent.getStringExtra("version") ?: "");app.reminders.reconcile(force=intent.action!=ReminderEngine.ACTION)}catch(e:CancellationException){throw e}catch(e:Exception){app.maintenanceError.value="Promemoria non aggiornato: apri la diagnostica notifiche.";app.reminderStore.status("error","Ricezione interrotta (${e.javaClass.simpleName}). Riapri l’app e riprogramma.")}finally{result.finish()}}
    }
}
