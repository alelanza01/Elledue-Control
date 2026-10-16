package it.elledue.control.data

import android.content.Context
import com.google.gson.Gson
import kotlinx.coroutines.flow.MutableStateFlow

/** Private, synchronous atomic preference commits: a keystroke is durable before returning.
 * Drafts never enter the financial tables. No large saved-instance Bundle or in-memory-only state.
 */
class DraftStore(context:Context) {
    private val preferences=context.getSharedPreferences("local-form-drafts-v1",Context.MODE_PRIVATE)
    private val gson=Gson()
    val revision=MutableStateFlow(0L)
    val error=MutableStateFlow<String?>(null)
    @Synchronized fun <T> load(key:String,type:Class<T>):T? = preferences.getString(key,null)?.let {
        runCatching {gson.fromJson(it,type)}.getOrElse {error.value="Bozza non leggibile: conservala o scartala esplicitamente.";null}
    }
    @Synchronized fun save(key:String,value:Any):Boolean {
        val ok=preferences.edit().putString(key,gson.toJson(value)).commit()
        error.value=if(ok)null else "Impossibile rendere persistente la bozza. Non chiudere il modulo prima di salvare."
        revision.value++;return ok
    }
    @Synchronized fun clear(route:String) {
        val edit=preferences.edit();preferences.all.keys.filter {it==route || it.startsWith("$route#")}.forEach(edit::remove)
        if(!edit.commit()) error.value="La bozza non è stata rimossa: riprova.";revision.value++
    }
    fun snapshot():Map<String,String> = preferences.all.mapNotNull{(k,v)->(v as? String)?.let{k to it}}.toMap()
    @Synchronized fun replace(values:Map<String,String>) {
        val edit=preferences.edit().clear();values.forEach{(k,v)->edit.putString(k,v)}
        check(edit.commit()) {"Le bozze non sono state ripristinate: il backup originale le conserva, riprova senza eliminarlo."};revision.value++
    }
    fun routes():List<String> = preferences.all.keys.map{it.substringBefore('#')}.distinct().sorted()
    fun contains(key:String)=preferences.contains(key)
    fun has(route:String)=routes().contains(route)
    internal fun removeReferences(ref:DeletionReferences) {
        routes().filter {route->
            val id=route.substringAfter('/', "").substringBefore('/')
            when {
                route.startsWith("property/")->id in ref.properties
                route.startsWith("booking/")->id in ref.bookings || load(route,it.elledue.control.domain.BookingDraft::class.java)?.propertyId in ref.properties
                route.startsWith("tax/")->id in ref.bookings
                route.startsWith("expense/")->id in ref.expenses || load(route,it.elledue.control.domain.ExpenseDraft::class.java)?.let{it.propertyId in ref.properties || it.reservationId in ref.bookings}==true
                route.startsWith("activity/")->id in ref.activities || load("$route#property",String::class.java) in ref.properties || load("$route#booking",String::class.java) in ref.bookings
                route.startsWith("messages?")->load("$route#property",String::class.java) in ref.properties
                route.startsWith("activities?")->load("$route#property",String::class.java) in ref.properties || android.net.Uri.parse("elledue://local/$route").getQueryParameter("booking") in ref.bookings
                route.startsWith("movement/")->android.net.Uri.parse("elledue://local/$route").let{it.getQueryParameter("property") in ref.properties || it.getQueryParameter("booking") in ref.bookings}
                route.startsWith("report/")->id in ref.reports
                else->false
            }
        }.forEach(::clear)
    }
    companion object {
        private val stores=java.util.WeakHashMap<Context,DraftStore>()
        fun get(context:Context):DraftStore=synchronized(stores){val app=context.applicationContext;stores.getOrPut(app){DraftStore(app)}}
    }
}

internal data class DeletionReferences(val properties:Set<String>,val bookings:Set<String>,val expenses:Set<String>,val activities:Set<String>,val reports:Set<String>)
