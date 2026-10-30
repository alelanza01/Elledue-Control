package it.elledue.control.ui
import androidx.compose.runtime.*
import androidx.compose.material3.*
import androidx.compose.foundation.layout.*
import androidx.compose.ui.Modifier
import it.elledue.control.domain.OtaTreatment
@Composable fun OtaTreatmentField(value:String?,enabled:Boolean=true,onChange:(String)->Unit) {
    var expanded by remember{mutableStateOf(false)}
    Box {OutlinedButton(onClick={expanded=true},enabled=enabled){Text("Costo OTA: "+OtaTreatment.label(value))}
        DropdownMenu(expanded,onDismissRequest={expanded=false}){OtaTreatment.values.forEach{v->DropdownMenuItem(text={Text(OtaTreatment.label(v))},onClick={expanded=false;onChange(v)})}}
    }
}
