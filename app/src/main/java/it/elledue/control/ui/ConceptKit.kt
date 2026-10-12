package it.elledue.control.ui

import androidx.compose.foundation.*
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.*
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.semantics.*
import androidx.compose.ui.unit.dp
import androidx.compose.material.icons.outlined.*

val ConceptBackground = Color(0xFF0B1424)
val ConceptCard = Color(0xFF12203A)
val ConceptBorder = Color(0xFF24344F)
val ConceptMuted = Color(0xFFB9C3D1)
val ArrivalColor = Color(0xFF4CB7B0)
val DepartureColor = Color(0xFFD96B6B)
val StayColor = Color(0xFF5D8DFF)
val TaskColor = Color(0xFF8A73D6)

@Composable
fun ConceptPill(selected: Boolean, onClick: () -> Unit, label: @Composable () -> Unit, modifier: Modifier = Modifier) {
    Box(modifier.clip(RoundedCornerShape(9.dp)).background(if (selected) Brush.horizontalGradient(listOf(ElledueGold, Color(0xFFEBD2A1))) else Brush.horizontalGradient(listOf(Color(0xFF15253B),Color(0xFF101D30)))).border(1.dp, if(selected) ElledueGold.copy(.65f) else ConceptBorder, RoundedCornerShape(9.dp)).clickable(role = Role.Tab, onClick = onClick).semantics { this.selected = selected }.padding(horizontal = 14.dp, vertical = 8.dp), contentAlignment = Alignment.Center) {
        CompositionLocalProvider(LocalContentColor provides if (selected) ElledueNight else ConceptMuted) { ProvideTextStyle(MaterialTheme.typography.labelMedium, content = label) }
    }
}

@Composable
fun ConceptHeading(title: String, subtitle: String, action: (@Composable () -> Unit)? = null) {
    Row(Modifier.fillMaxWidth(), verticalAlignment = Alignment.CenterVertically) {
        Column(Modifier.weight(1f), verticalArrangement = Arrangement.spacedBy(3.dp)) {
            Text(title, style = MaterialTheme.typography.headlineMedium)
            Text(subtitle, style = MaterialTheme.typography.bodySmall, color = ConceptMuted)
        }
        action?.invoke()
    }
}

@Composable
fun ActionTile(label: String, icon: ImageVector, modifier: Modifier = Modifier, onClick: () -> Unit) {
    Column(modifier.clip(RoundedCornerShape(12.dp)).background(Brush.verticalGradient(listOf(Color(0xFF182B42),Color(0xFF101D30)))).border(1.dp, ElledueGold.copy(.24f), RoundedCornerShape(12.dp)).clickable(role = Role.Button, onClick = onClick).padding(vertical = 13.dp, horizontal = 4.dp), horizontalAlignment = Alignment.CenterHorizontally, verticalArrangement = Arrangement.spacedBy(8.dp)) {
        Icon(icon, label, tint = ElledueGold, modifier = Modifier.size(24.dp))
        Text(label, style = MaterialTheme.typography.labelSmall, maxLines = 2)
    }
}

@Composable
fun StatusBadge(label: String, color: Color) {
    Text(label, color = color, style = MaterialTheme.typography.labelSmall, modifier = Modifier.background(color.copy(.19f), RoundedCornerShape(20.dp)).border(1.dp,color.copy(.3f),RoundedCornerShape(20.dp)).padding(horizontal = 10.dp, vertical = 4.dp))
}

@Composable
fun ConceptTimelineRow(time: String, title: String, subtitle: String, icon: ImageVector, color: Color, modifier: Modifier = Modifier, onClick: () -> Unit = {}) {
    Row(modifier.fillMaxWidth().clickable(onClick = onClick).padding(vertical = 10.dp), verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(10.dp)) {
        Box(Modifier.width(3.dp).height(42.dp).background(color, CircleShape))
        Text(time, style = MaterialTheme.typography.labelSmall, modifier = Modifier.width(45.dp), color = ConceptMuted)
        Box(Modifier.size(32.dp).background(color.copy(.18f), CircleShape), contentAlignment = Alignment.Center) { Icon(icon, null, tint = color, modifier = Modifier.size(18.dp)) }
        Column(Modifier.weight(1f), verticalArrangement = Arrangement.spacedBy(3.dp)) { Text(title, style = MaterialTheme.typography.titleSmall); Text(subtitle, style = MaterialTheme.typography.bodySmall, color = ConceptMuted, maxLines = 2) }
    }
}

@Composable
fun ConceptNavItem(icon: ImageVector, label: String, selected: Boolean, showLabel: Boolean, modifier: Modifier, onClick: () -> Unit) {
    Column(modifier.clickable(role=Role.Tab,onClick=onClick).semantics { this.selected=selected }.padding(vertical=10.dp),horizontalAlignment=Alignment.CenterHorizontally,verticalArrangement=Arrangement.spacedBy(5.dp)) {
        androidx.compose.material3.Icon(icon,label,tint=if(selected) ElledueGold else ConceptMuted,modifier=Modifier.size(21.dp))
        if(showLabel) Text(label,style=MaterialTheme.typography.labelSmall,color=if(selected) ElledueGold else ConceptMuted,maxLines=1)
    }
}

@Composable
fun ConceptPropertySelector(properties: List<it.elledue.control.data.PropertyWithOwner>, selected: String, allOption: Boolean = false, enabled: Boolean = true, change: (String) -> Unit) {
    var menu by remember { mutableStateOf(false) }
    Box(Modifier.fillMaxWidth()) {
        Row(Modifier.fillMaxWidth().clip(MaterialTheme.shapes.small).background(ConceptCard).border(1.dp,ConceptBorder,MaterialTheme.shapes.small).clickable(enabled=enabled) { menu=true }.testTag("booking-property-selector").padding(horizontal=12.dp,vertical=12.dp),verticalAlignment=Alignment.CenterVertically) {
            androidx.compose.material3.Icon(androidx.compose.material.icons.Icons.Outlined.Apartment,null,Modifier.size(17.dp),tint=ElledueGold)
            Text(properties.firstOrNull { it.property.id==selected }?.property?.name ?: if(allOption) "Tutti gli immobili" else "Seleziona immobile",style=MaterialTheme.typography.bodyMedium,modifier=Modifier.weight(1f).padding(horizontal=9.dp))
            androidx.compose.material3.Icon(androidx.compose.material.icons.Icons.Outlined.ExpandMore,"Seleziona immobile",Modifier.size(17.dp),tint=ConceptMuted)
        }
        DropdownMenu(menu,{menu=false}) {
            if(allOption) DropdownMenuItem(text={Text("Tutti gli immobili")},onClick={menu=false;change("")})
            properties.forEach { property -> DropdownMenuItem(modifier=Modifier.testTag("select-property-${property.property.id}"),text={Text(property.property.name)},onClick={menu=false;change(property.property.id)}) }
        }
    }
}

@Composable
fun ConceptSearch(query: String, change: (String) -> Unit) {
    Row(Modifier.fillMaxWidth().clip(MaterialTheme.shapes.small).background(ConceptCard).border(1.dp,ConceptBorder,MaterialTheme.shapes.small).padding(horizontal=12.dp,vertical=12.dp),verticalAlignment=Alignment.CenterVertically,horizontalArrangement=Arrangement.spacedBy(8.dp)) {
        androidx.compose.material3.Icon(androidx.compose.material.icons.Icons.Outlined.Search,null,Modifier.size(17.dp),tint=ConceptMuted)
        androidx.compose.foundation.text.BasicTextField(query,change,singleLine=true,textStyle=MaterialTheme.typography.bodyMedium.copy(color=androidx.compose.ui.graphics.Color(0xFFF6F2EA)),cursorBrush=androidx.compose.ui.graphics.SolidColor(ElledueGold),modifier=Modifier.weight(1f).semantics { contentDescription="Cerca immobile o proprietario" },decorationBox={field->if(query.isEmpty()) Text("Cerca per nome, località o proprietario…",style=MaterialTheme.typography.bodySmall,color=ConceptMuted);field()})
    }
}

/** Branded modal shell; confirmation actions and validation remain the existing ones. */
@Composable
fun AlertDialog(onDismissRequest: () -> Unit, confirmButton: @Composable () -> Unit, dismissButton: (@Composable () -> Unit)? = null, title: (@Composable () -> Unit)? = null, text: (@Composable () -> Unit)? = null) {
    androidx.compose.ui.window.Dialog(onDismissRequest) {
        Column(Modifier.fillMaxWidth().clip(RoundedCornerShape(18.dp)).background(Brush.verticalGradient(listOf(Color(0xFF1A2D46),ConceptBackground))).border(1.dp,ElledueGold.copy(.42f),RoundedCornerShape(18.dp)).padding(22.dp),verticalArrangement=Arrangement.spacedBy(16.dp)) {
            Box(Modifier.width(35.dp).height(2.dp).background(ElledueGold))
            title?.let { ProvideTextStyle(MaterialTheme.typography.titleLarge) { it() } }
            text?.let { CompositionLocalProvider(LocalContentColor provides ConceptMuted) { ProvideTextStyle(MaterialTheme.typography.bodyMedium) { it() } } }
            Column(Modifier.fillMaxWidth(),horizontalAlignment=Alignment.End,verticalArrangement=Arrangement.spacedBy(3.dp)) { confirmButton();dismissButton?.invoke() }
        }
    }
}
