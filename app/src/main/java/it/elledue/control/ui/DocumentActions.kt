package it.elledue.control.ui

import android.content.Intent
import android.net.Uri
import androidx.core.net.toUri
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.layout.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.unit.dp
import it.elledue.control.data.BackupService
import it.elledue.control.data.PreparedBackup
import it.elledue.control.reports.*
import kotlinx.coroutines.*

@Composable
fun rememberPdfExporter(snapshot: ReportSnapshot, layout: ReportLayout = ReportLayout.DETAILED): () -> Unit {
    val context = LocalContext.current; val scope = rememberCoroutineScope()
    var message by remember { mutableStateOf<String?>(null) }; var saved by rememberSaveable { mutableStateOf<String?>(null) }
    // Freeze the selected version at launch, so navigating or source edits cannot change the output.
    var pending by remember { mutableStateOf<ReportSnapshot?>(null) }; var pendingLayout by remember { mutableStateOf(layout) }
    val launcher = rememberLauncherForActivityResult(ActivityResultContracts.CreateDocument("application/pdf")) { uri ->
        val target = pending
        if (uri != null && target != null) scope.launch {
            try { withContext(Dispatchers.IO) { val bytes = java.io.ByteArrayOutputStream().also { ReportPdf(context).write(target, it, pendingLayout) }.toByteArray(); context.contentResolver.openOutputStream(uri, "wt")!!.use { it.write(bytes) } }; saved = uri.toString(); message = "PDF salvato." }
            catch (_: Exception) { message = "Esportazione non riuscita. La versione del rendiconto è conservata." }
        }
    }
    if (message != null) AlertDialog(onDismissRequest = { message = null }, title = { Text(message!!) }, text = { Text("Il documento non viene inviato automaticamente.") }, confirmButton = { TextButton(onClick = { message = null; saved?.let { value ->
        runCatching { context.startActivity(Intent(Intent.ACTION_VIEW).setDataAndType(value.toUri(), "application/pdf").addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION)) }.onFailure { message = "PDF salvato; nessun visualizzatore disponibile. Aprilo dal gestore file." }
    } }) { Text(if (saved != null) "Apri PDF" else "Chiudi") } }, dismissButton = { TextButton(onClick = { message = null }) { Text("Chiudi") } })
    return { pending = snapshot; pendingLayout = layout; saved = null; launcher.launch("Elledue-${layout.name.lowercase()}-${snapshot.month}-v${snapshot.version}${if (snapshot.draft) "-bozza" else ""}.pdf") }
}

@Composable
fun BackupControls(service: BackupService) {
    val context = LocalContext.current; val scope = rememberCoroutineScope()
    var busy by remember { mutableStateOf(false) }; var message by remember { mutableStateOf<String?>(null) }
    var prepared by remember { mutableStateOf<PreparedBackup?>(null) }; var acknowledge by remember { mutableStateOf(false) }
    var rollback by rememberSaveable { mutableStateOf(false) }
    val exporter = rememberLauncherForActivityResult(ActivityResultContracts.CreateDocument("application/vnd.elledue.backup+json")) { uri ->
        if (uri != null) { busy = true; scope.launch { try {
            withContext(Dispatchers.IO) { val bytes = service.export(); service.prepare(bytes); context.contentResolver.openOutputStream(uri, "wt")!!.use { it.write(bytes) } }
            message = if (rollback) "Copia di sicurezza esportata. Ora puoi confermare il ripristino." else "Backup esportato e validato. Conserva il file in una posizione privata."; rollback = false
        } catch (_: Exception) { message = "Backup non riuscito. I dati sul dispositivo sono conservati." } finally { busy = false } } }
    }
    val importer = rememberLauncherForActivityResult(ActivityResultContracts.OpenDocument()) { uri ->
        if (uri != null) { busy = true; scope.launch { try {
            val checked = withContext(Dispatchers.IO) { val bytes = context.contentResolver.openInputStream(uri)!!.use { readBackupBytes(it) }; service.prepare(bytes) }
            prepared = checked; acknowledge = false; message = null
        } catch (_: Exception) { message = "File non valido, corrotto o incompatibile. Nessun dato è stato sostituito." } finally { busy = false } } }
    }
    HorizontalDivider(); Text("Backup e ripristino", style = MaterialTheme.typography.titleMedium)
    Text("Include dati personali, contabilità e tutte le versioni dei rendiconti. Nessun caricamento automatico. Scegli una destinazione privata; il file non è cifrato dall’app.", style = MaterialTheme.typography.bodySmall)
    Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
        OutlinedButton(onClick = { rollback = false; exporter.launch("Elledue-backup.json") }, enabled = !busy, modifier = Modifier.testTag("export-backup")) { Text("Esporta backup") }
        OutlinedButton(onClick = { importer.launch(arrayOf("application/json", "application/octet-stream", "*/*")) }, enabled = !busy, modifier = Modifier.testTag("import-backup")) { Text("Importa") }
    }
    if (busy) CircularProgressIndicator()
    message?.let { Text(it, style = MaterialTheme.typography.bodySmall) }
    if (prepared != null) OutlinedCard(Modifier.fillMaxWidth()) { Column(Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(10.dp)) {
        Text("Backup verificato · ${prepared!!.rowCount} record", style = MaterialTheme.typography.titleMedium)
        Text("Il ripristino sostituisce tutti i dati attuali, senza unire contabilità diverse. Esporta prima una copia di sicurezza.")
        TextButton(onClick = { rollback = true; exporter.launch("Elledue-prima-del-ripristino.json") }, enabled = !busy) { Text("Esporta copia dei dati attuali") }
        Row { Checkbox(acknowledge, { acknowledge = it }, enabled = !busy); Text("Confermo la sostituzione completa", modifier = Modifier.weight(1f)) }
        Button(onClick = { val backup = prepared!!; busy = true; scope.launch { try { withContext(Dispatchers.IO) { service.restore(backup, confirmed = true) }; message = "Ripristino completato e verificato."; prepared = null } catch (_: Exception) { message = "Ripristino non riuscito: i dati precedenti sono conservati." } finally { busy = false } } }, enabled = acknowledge && !busy, modifier = Modifier.testTag("confirm-restore")) { Text("Ripristina dati") }
        TextButton(onClick = { prepared = null }, enabled = !busy) { Text("Annulla") }
    } }
}

private fun readBackupBytes(input: java.io.InputStream): ByteArray {
    val output = java.io.ByteArrayOutputStream(); val buffer = ByteArray(8192)
    while (true) { val count = input.read(buffer); if (count < 0) break; require(output.size() + count <= BackupService.MAX_BYTES) { "Backup troppo grande." }; output.write(buffer, 0, count) }
    return output.toByteArray()
}
