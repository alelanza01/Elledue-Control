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
    fun load():ReminderSettings=runCatching{Gson().fromJson(prefs.getString("settings",null),ReminderSettings::class.java) ?: ReminderSettings()}.getOrDefault(ReminderSettings())
    fun save(value:ReminderSettings){ReminderPlanner.validate(value);check(prefs.edit().putString("settings",Gson().toJson(value)).commit());state.value=value}
    fun scheduled():Map<String,Reminder> {
        val obj=JSONObject(prefs.getString("scheduled","{}")!!)
        return obj.keys().asSequence().associateWith{Gson().fromJson(obj.getString(it),Reminder::class.java)}
    }
    fun scheduled(value:Map<String,Reminder>){val json=JSONObject();value.forEach{(k,v)->json.put(k,Gson().toJson(v))};check(prefs.edit().putString("scheduled",json.toString()).commit())}
    fun handled(key:String)=prefs.getBoolean("handled:$key",false)
    fun handle(key:String){check(prefs.edit().putBoolean("handled:$key",true).commit())}
    fun delivered(key:String)=prefs.getString("sent:$key",null)
    fun mark(job:Reminder){check(prefs.edit().putString("sent:${job.key}",job.version).commit())}
}
class ReminderEngine(private val app:ElledueApplication) {
    private val store get()=app.reminderStore
    private val mutex=kotlinx.coroutines.sync.Mutex()
    fun allowed():Boolean=(Build.VERSION.SDK_INT<33 || ContextCompat.checkSelfPermission(app,Manifest.permission.POST_NOTIFICATIONS)==PackageManager.PERMISSION_GRANTED) && NotificationManagerCompat.from(app).areNotificationsEnabled() && app.getSystemService(NotificationManager::class.java).getNotificationChannel(CHANNEL)?.importance != NotificationManager.IMPORTANCE_NONE
    fun channel(){val manager=app.getSystemService(NotificationManager::class.java);manager.createNotificationChannel(NotificationChannel(CHANNEL,"Promemoria operativi",NotificationManager.IMPORTANCE_DEFAULT).apply{description="Prenotazioni, attività e rendiconti locali";lockscreenVisibility=Notification.VISIBILITY_PRIVATE})}
    private fun pending(job:Reminder):PendingIntent=PendingIntent.getBroadcast(app,0,Intent(app,ReminderReceiver::class.java).setAction(ACTION).setData(Uri.parse("elledue-reminder://local/${Uri.encode(job.key)}")).putExtra("key",job.key).putExtra("version",job.version),PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE)
    private suspend fun jobs(now:Instant):List<Reminder> {
        val tasks=app.activityRepository.activities.first()
        val issues=app.issueRepository.issues.first().filter{issue->it.elledue.control.data.IssueRepository.open(issue)}
        val issueTasks=issues.mapNotNull{i->i.scheduledAt?.let{at->it.elledue.control.data.ActivityEntity("issue-${i.id}",i.propertyId,i.title,at,kind="MAINTENANCE",hasTime=i.hasTime)}}
        return ReminderPlanner.plan(app.propertyRepository.properties.first(),app.reservationRepository.reservations.first(),tasks+issueTasks,store.load(),now).map{job->
            if(job.key.startsWith("task:issue-"))job.copy(route="issue/"+job.key.removePrefix("task:issue-")) else job
        }
    }
    fun exactAllowed():Boolean=Build.VERSION.SDK_INT<31 || app.getSystemService(AlarmManager::class.java).canScheduleExactAlarms()
    private fun schedule(alarm:AlarmManager,job:Reminder) {
        if(exactAllowed())try{alarm.setExactAndAllowWhileIdle(AlarmManager.RTC_WAKEUP,job.at,pending(job));return}catch(_:SecurityException){}
        alarm.setAndAllowWhileIdle(AlarmManager.RTC_WAKEUP,job.at,pending(job))
    }
    suspend fun reconcile(now:Instant=Instant.now(),force:Boolean=false) {
        mutex.lock();try {
            channel();val alarm=app.getSystemService(AlarmManager::class.java);val old=store.scheduled()
            val all=if(allowed())jobs(now) else emptyList()
            // Android may defer inexact alarms or discard them after permissions change.
            // Recover only already scheduled, still current jobs; never invent old reminders.
            all.filter{it.at<=now.toEpochMilli() && old[it.key]?.version==it.version && store.delivered(it.key)!=it.version && !store.handled(it.key)}.forEach{if(show(it))store.mark(it)}
            val planned=if(allowed()) all.filter{it.at>=now.toEpochMilli() && store.delivered(it.key)!=it.version && !store.handled(it.key)}.associateBy{it.key} else emptyMap()
            old.forEach{(key,value)->if(planned[key]?.version!=value.version){alarm.cancel(pending(value));NotificationManagerCompat.from(app).cancel(key.hashCode())}}
            planned.forEach{(key,value)->if(force || old[key]?.version!=value.version)schedule(alarm,value)}
            store.scheduled(planned)
        }finally{mutex.unlock()}
    }
    suspend fun deliver(key:String,version:String,now:Instant=Instant.now()) {
        mutex.lock();try {
            if(!allowed() || !store.load().enabled)return
            val job=jobs(now).firstOrNull{it.key==key && it.version==version} ?: return
            if(job.at>now.plusSeconds(60).toEpochMilli() || now.toEpochMilli()-job.at>48*3600*1000L || store.delivered(key)==version || store.handled(key))return
            if(show(job))store.mark(job)
        }finally{mutex.unlock()}
    }
    fun test(){check(allowed()){ "Concedi il permesso notifiche nelle impostazioni Android." };channel();check(show(Reminder("test",System.currentTimeMillis(),"Elledue Control","Notifica locale di prova. Nessun dato contabile è stato modificato.","notifications","test"))){"Il permesso notifiche è stato revocato durante l’invio."}}
    private fun show(job:Reminder):Boolean {
        if(Build.VERSION.SDK_INT>=33 && ContextCompat.checkSelfPermission(app,Manifest.permission.POST_NOTIFICATIONS)!=PackageManager.PERMISSION_GRANTED)return false
        val intent=Intent(app,MainActivity::class.java).addFlags(Intent.FLAG_ACTIVITY_CLEAR_TOP or Intent.FLAG_ACTIVITY_SINGLE_TOP).setData(Uri.parse("elledue-open://local/${Uri.encode(job.key)}")).putExtra("route",job.route)
        val open=PendingIntent.getActivity(app,0,intent,PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE)
        return try { NotificationManagerCompat.from(app).notify(job.key.hashCode(),NotificationCompat.Builder(app,CHANNEL).setSmallIcon(R.drawable.elledue_notification).setContentTitle(if(app.appLock.settings.value.enabled)"Elledue Control" else job.title).setContentText(if(app.appLock.settings.value.enabled)"Promemoria operativo · sblocca per visualizzare" else job.text).setStyle(NotificationCompat.BigTextStyle().bigText(if(app.appLock.settings.value.enabled)"Promemoria operativo · sblocca per visualizzare" else job.text)).setVisibility(NotificationCompat.VISIBILITY_PRIVATE).setAutoCancel(true).setContentIntent(open).build());true } catch(_:SecurityException){false}
    }
    companion object {const val CHANNEL="elledue_operations_v1";const val ACTION="it.elledue.control.REMINDER"}
}
class ReminderReceiver:BroadcastReceiver() {
    override fun onReceive(context:Context,intent:Intent) {
        val pending=goAsync();val app=context.applicationContext as ElledueApplication
        app.background.launch{try{if(intent.action==ReminderEngine.ACTION)app.reminders.deliver(intent.getStringExtra("key") ?: "",intent.getStringExtra("version") ?: "");app.reminders.reconcile(force=intent.action!=ReminderEngine.ACTION)}catch(_:Exception){app.maintenanceError.value="Promemoria non aggiornato: verifica permessi e programmazione nelle impostazioni."}finally{pending.finish()}}
    }
}
