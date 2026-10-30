package it.elledue.control.ui

import androidx.compose.foundation.*
import androidx.compose.foundation.layout.*
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.unit.dp
import it.elledue.control.R

val portals=listOf("Booking.com" to R.drawable.portal_booking,"Airbnb" to R.drawable.portal_airbnb,"Vrbo" to R.drawable.portal_vrbo,"Prenotazione diretta" to R.drawable.portal_prenotazione_diretta)
@Composable fun PortalSelector(selected:String, enabled:Boolean, change:(String)->Unit) {
    Text("Portale di provenienza",style=MaterialTheme.typography.titleMedium)
    Column(verticalArrangement=Arrangement.spacedBy(6.dp)) { portals.forEach { (name,res) ->
        Row(Modifier.fillMaxWidth().clickable(enabled=enabled){change(name)}.testTag("portal-$name").background(if(name.equals(selected,true)) MaterialTheme.colorScheme.primaryContainer else ConceptCard,MaterialTheme.shapes.small).padding(8.dp),verticalAlignment=androidx.compose.ui.Alignment.CenterVertically) {
            Image(painterResource(res),null,Modifier.size(38.dp),contentScale=ContentScale.Fit)
            Text(name,Modifier.weight(1f).padding(start=12.dp));RadioButton(name.equals(selected,true),{change(name)},enabled=enabled)
        }
    } }
    if(portals.none {it.first.equals(selected,true)}) Text("Portale storico: $selected · mantenuto",style=MaterialTheme.typography.bodySmall)
}
