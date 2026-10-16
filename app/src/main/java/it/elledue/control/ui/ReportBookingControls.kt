package it.elledue.control.ui

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.unit.dp
import it.elledue.control.data.ReservationEntity
import it.elledue.control.reports.ReportSnapshot
import androidx.lifecycle.compose.collectAsStateWithLifecycle

@Composable fun ReportBookingControls(snapshot:ReportSnapshot,booking:ReservationEntity,model:ReportViewModel) {
    var dialog by remember{mutableStateOf(false)};var applying by remember{mutableStateOf(false)}
    val rule=snapshot.bookingOptions.orEmpty().firstOrNull{it.reservationId==booking.id}
    var hidden by remember(snapshot.id,rule){mutableStateOf(rule?.hideDetails ?: (booking.id in snapshot.hiddenBookingIds.orEmpty()))}
    var excluded by remember(snapshot.id,rule){mutableStateOf(rule?.excluded ?: snapshot.excludedBookings.orEmpty().any{it.id==booking.id})}
    val preview by model.optionPreview.collectAsStateWithLifecycle();val busy by model.busy.collectAsStateWithLifecycle();val error by model.error.collectAsStateWithLifecycle()
    LaunchedEffect(busy,preview,error){if(applying && !busy){if(error==null && preview==null)dialog=false;applying=false}}
    if(snapshot.draft)TextButton(onClick={model.clearPreview();dialog=true},modifier=Modifier.testTag("report-options-${booking.id}")){Text("Visibilità PDF / inclusione nei calcoli")}
    else Text("Snapshot confermato: apri una versione rettificativa per cambiare l’inclusione.",style=MaterialTheme.typography.bodySmall)
    if(dialog)AlertDialog(onDismissRequest={if(!busy){dialog=false;model.clearPreview()}},title={Text("Prenotazione nel rendiconto")},text={Column(Modifier.verticalScroll(rememberScrollState()),verticalArrangement=Arrangement.spacedBy(10.dp)){
        Text("${booking.guestFirstName} ${booking.guestLastName}")
        Row{Checkbox(hidden,{hidden=it;model.clearPreview()});Text("Non mostrare la riga nominativa nel PDF; importi inclusi",Modifier.weight(1f).padding(top=10.dp))}
        Row{Checkbox(excluded,{excluded=it;model.clearPreview()});Text("Escludi dai calcoli di questo rendiconto anche spese e riconoscimenti direttamente collegati",Modifier.weight(1f).padding(top=10.dp))}
        Text("La prenotazione rimane nel calendario. Incassi e riversamenti reali non vengono modificati.",style=MaterialTheme.typography.bodySmall)
        OutlinedButton(onClick={model.previewOption(snapshot.id,booking.id,hidden,excluded)},enabled=!busy,modifier=Modifier.testTag("preview-report-option")){Text("Verifica effetto sui totali")}
        preview?.takeIf{it.id==snapshot.id}?.let{p->AmountLine("Saldo prima",snapshot.summary().balance);AmountLine("Saldo dopo",p.summary().balance);AmountLine("Compenso di gestione Elledue",p.summary().fees);AmountLine("Rimborsi dopo",p.summary().reimbursable)}
        error?.let{Text(it,color=MaterialTheme.colorScheme.error)}
    }},confirmButton={TextButton(onClick={preview?.let{applying=true;model.applyOption(snapshot.id,booking.id,hidden,excluded,it)}},enabled=!busy && preview!=null,modifier=Modifier.testTag("apply-report-option")){Text("Applica alla bozza")}},dismissButton={TextButton(onClick={dialog=false;model.clearPreview()},enabled=!busy){Text("Continua senza applicare")}})
}
