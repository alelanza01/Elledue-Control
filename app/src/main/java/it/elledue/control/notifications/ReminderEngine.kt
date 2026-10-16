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
    fun delivered(key:String)=prefs.getString("sent:$key",null)
    fun mark(job:Reminder){check(prefs.edit().putString("sent:${job.key}",job.version).commit())}
}
class ReminderEngine(private val app:ElledueApplication) {
    private val store get()=app.reminderStore
    private val mutex=kotlinx.coroutines.sync.Mutex()
    fun allowed():Boolean=(Build.VERSION.SDK_INT<33 || ContextCompat.checkSelfPermission(app,Manifest.permission.POST_NOTIFICATIONS)==PackageManager.PERMISSION_GRANTED) && NotificationManagerCompat.from(app).areNotificationsEnabled() && app.getSystemService(NotificationManager::class.java).getNotificationChannel(CHANNEL)?.importance != NotificationManager.IMPORTANCE_NONE
    fun channel(){val manager=app.getSystemService(NotificationManager::class.java);manager.createNotificationChannel(NotificationChannel(CHANNEL,"Promemoria operativi",NotificationManager.IMPORTANCE_DEFAULT).apply{description="Prenotazioni, attività e rendiconti locali";lockscreenVisibility=Notification.VISIBILITY_PRIVATE})}
    private fun pending(job:Reminder):PendingIntent=PendingIntent.getBroadcast(app,0,Intent(app,ReminderReceiver::class.java).setAction(ACTION).setData(Uri.parse("elledue-reminder://local/${Uri.encode(job.key)}")).putExtra("key",job.key).putExtra("version",job.version),PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE)
    private suspend fun jobs(now:Instant)=ReminderPlanner.plan(app.propertyRepository.properties.first(),app.reservationRepository.reservations.first(),app.activityRepository.activities.first(),store.load(),now)
    suspend fun reconcile(now:Instant=Instant.now(),force:Boolean=false) {
        mutex.lock();try {
            channel();val alarm=app.getSystemService(AlarmManager::class.java);val old=store.scheduled()
            val planned=if(allowed()) jobs(now).filter{it.at>=now.toEpochMilli() && store.delivered(it.key)!=it.version}.associateBy{it.key} else emptyMap()
            old.forEach{(key,value)->if(planned[key]?.version!=value.version){alarm.cancel(pending(value));NotificationManagerCompat.from(app).cancel(key.hashCode())}}
            planned.forEach{(key,value)->if(force || old[key]?.version!=value.version)alarm.setAndAllowWhileIdle(AlarmManager.RTC_WAKEUP,value.at,pending(value))}
            store.scheduled(planned)
        }finally{mutex.unlock()}
    }
    suspend fun deliver(key:String,version:String,now:Instant=Instant.now()) {
        mutex.lock();try {
            if(!allowed() || !store.load().enabled)return
            val job=jobs(now).firstOrNull{it.key==key && it.version==version} ?: return
            if(job.at>now.plusSeconds(60).toEpochMilli() || now.toEpochMilli()-job.at>48*3600*1000L || store.delivered(key)==version)return
            show(job);store.mark(job)
        }finally{mutex.unlock()}
    }
    fun test(){check(allowed()){ "Concedi il permesso notifiche nelle impostazioni Android." };channel();show(Reminder("test",System.currentTimeMillis(),"Elledue Control","Notifica locale di prova. Nessun dato contabile è stato modificato.","notifications","test"))}
    private fun show(job:Reminder){
        val intent=Intent(app,MainActivity::class.java).addFlags(Intent.FLAG_ACTIVITY_CLEAR_TOP or Intent.FLAG_ACTIVITY_SINGLE_TOP).setData(Uri.parse("elledue-open://local/${Uri.encode(job.key)}")).putExtra("route",job.route)
        val open=PendingIntent.getActivity(app,0,intent,PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE)
        NotificationManagerCompat.from(app).notify(job.key.hashCode(),NotificationCompat.Builder(app,CHANNEL).setSmallIcon(R.drawable.elledue_notification).setContentTitle(job.title).setContentText(job.text).setStyle(NotificationCompat.BigTextStyle().bigText(job.text)).setVisibility(NotificationCompat.VISIBILITY_PRIVATE).setAutoCancel(true).setContentIntent(open).build())
    }
    companion object {const val CHANNEL="elledue_operations_v1";const val ACTION="it.elledue.control.REMINDER"}
}
class ReminderReceiver:BroadcastReceiver() {
    override fun onReceive(context:Context,intent:Intent) {
        val pending=goAsync();val app=context.applicationContext as ElledueApplication
        app.background.launch{try{if(intent.action==ReminderEngine.ACTION)app.reminders.deliver(intent.getStringExtra("key") ?: "",intent.getStringExtra("version") ?: "");app.reminders.reconcile(force=intent.action!=ReminderEngine.ACTION)}catch(_:Exception){/* A denied permission never fabricates completion of a task or cash movement. */}finally{pending.finish()}}
    }
}
