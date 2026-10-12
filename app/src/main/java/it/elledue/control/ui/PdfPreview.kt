package it.elledue.control.ui

import android.content.Context
import android.graphics.Bitmap
import android.graphics.pdf.PdfRenderer
import android.os.ParcelFileDescriptor
import androidx.compose.foundation.*
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.ui.Modifier
import androidx.compose.ui.Alignment
import androidx.compose.ui.graphics.asImageBitmap
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.unit.dp
import androidx.compose.ui.window.Dialog
import androidx.compose.ui.window.DialogProperties
import it.elledue.control.reports.*
import kotlinx.coroutines.*
import java.io.*
import java.util.UUID

/** PDFBox parses the exported bytes and paints them into actual Android bitmaps.
 * Same renderer on phone and Robolectric; no fake pages or second layout engine. */
class PdfPreviewDocument(context: Context, bytes: ByteArray) : Closeable {
    private val document: com.tom_roush.pdfbox.pdmodel.PDDocument
    private val renderer: com.tom_roush.pdfbox.rendering.PDFRenderer
    init {
        com.tom_roush.pdfbox.android.PDFBoxResourceLoader.init(context)
        document = com.tom_roush.pdfbox.pdmodel.PDDocument.load(bytes)
        renderer = object : com.tom_roush.pdfbox.rendering.PDFRenderer(document) {
            override fun createPageDrawer(parameters: com.tom_roush.pdfbox.rendering.PageDrawerParameters): com.tom_roush.pdfbox.rendering.PageDrawer =
                object : com.tom_roush.pdfbox.rendering.PageDrawer(parameters) {
                    override fun drawImage(image: com.tom_roush.pdfbox.pdmodel.graphics.image.PDImage) {
                        // PDFBox's RGB fast path copies raw bytes using platform pixel byte order.
                        // Our opaque DeviceRGB brand images use explicit Color.rgb conversion,
                        // keeping the official colors consistent on Android and native host graphics.
                        if (!image.isStencil && image.colorSpace is com.tom_roush.pdfbox.pdmodel.graphics.color.PDDeviceRGB && image.bitsPerComponent == 8 && image.width <= 1200 && image.height <= 1200) {
                            val bytes = image.createInputStream().use { it.readBytes() }
                            require(bytes.size == image.width * image.height * 3)
                            val colors = IntArray(image.width * image.height) { i -> android.graphics.Color.rgb(bytes[i*3].toInt() and 255, bytes[i*3+1].toInt() and 255, bytes[i*3+2].toInt() and 255) }
                            val bitmap = Bitmap.createBitmap(colors, image.width, image.height, Bitmap.Config.ARGB_8888)
                            try { super.drawImage(object : com.tom_roush.pdfbox.pdmodel.graphics.image.PDImage by image {
                                override fun getImage(): Bitmap = bitmap
                                override fun getImage(region: android.graphics.Rect?, subsampling: Int): Bitmap = bitmap
                            }) } finally { bitmap.recycle() }
                        } else super.drawImage(image)
                    }
                }
        }
    }
    private var closed = false
    val pages = document.numberOfPages
    @Synchronized fun render(index: Int, width: Int): Bitmap {
        if (closed) throw CancellationException("Anteprima precedente chiusa.")
        return renderer.renderImage(index, width / document.getPage(index).mediaBox.width, com.tom_roush.pdfbox.rendering.ImageType.RGB)
    }
    @Synchronized override fun close() { if (!closed) { closed = true; document.close() } }
}

private data class PreviewState(val document: PdfPreviewDocument? = null, val failed: Boolean = false)
@Composable
private fun rememberDocument(snapshot: ReportSnapshot, layout: ReportLayout): State<PreviewState> {
    val context = LocalContext.current
    return produceState(PreviewState(), snapshot, layout) {
        value = PreviewState()
        try {
            val created = withContext(Dispatchers.IO) { val bytes = ByteArrayOutputStream().also { ReportPdf(context).write(snapshot, it, layout) }.toByteArray(); PdfPreviewDocument(context, bytes) }
            value = PreviewState(created)
            awaitDispose { created.close() }
        } catch (e: CancellationException) { throw e }
        catch (_: Exception) { value = PreviewState(failed = true) }
    }
}

@Composable
private fun PdfPage(document: PdfPreviewDocument, index: Int, zoom: Boolean = false) {
    var failed by remember(document, index, zoom) { mutableStateOf(false) }
    val bitmap by produceState<Bitmap?>(null, document, index, zoom) {
        value = null
        try {
            val created = withContext(Dispatchers.IO) { currentCoroutineContext().ensureActive(); document.render(index, if (zoom) 1200 else 600) }
            value = created
        } catch (e: CancellationException) { throw e }
        catch (_: Exception) { failed = true }

        // Compose may still draw the previous bitmap during disposal; let GC reclaim it safely.
    }
    if (failed) Text("Pagina non disponibile: riapri l’anteprima.", color = MaterialTheme.colorScheme.error)
    bitmap?.let { Image(it.asImageBitmap(), "Pagina ${index + 1} del PDF", Modifier.then(if (zoom) Modifier.width(720.dp) else Modifier.fillMaxWidth()).aspectRatio(it.width.toFloat() / it.height)) }
}

@Composable
fun PdfThumbnail(snapshot: ReportSnapshot, layout: ReportLayout, open: () -> Unit) {
    // Bound the preview work to the report screen lifecycle; no external PDF viewer required.
    val state by rememberDocument(snapshot, layout)
    val document = state.document
    OutlinedCard(onClick = open, modifier = Modifier.fillMaxWidth().testTag("open-pdf-preview")) {
        Column(Modifier.padding(12.dp), verticalArrangement = Arrangement.spacedBy(10.dp)) {
            Text("Anteprima reale · ${if (snapshot.draft) "BOZZA" else "Confermato"}", style = MaterialTheme.typography.labelLarge)
            document?.let { Box(Modifier.fillMaxWidth().height(300.dp),contentAlignment=Alignment.Center) { Box(Modifier.width(211.dp)) { PdfPage(it, 0) } }; Text("${it.pages} pagine · Tocca per ingrandire",style=MaterialTheme.typography.labelMedium,color=ElledueGold) } ?: if (state.failed) Text("Anteprima non disponibile. I dati sono conservati; riapri il documento.") else LinearProgressIndicator(Modifier.fillMaxWidth())
        }
    }
}

@Composable
fun PdfPreview(snapshot: ReportSnapshot, layout: ReportLayout, close: () -> Unit, export: () -> Unit, select: (ReportLayout) -> Unit) {
    val state by rememberDocument(snapshot, layout)
    val document = state.document
    var zoom by rememberSaveable { mutableStateOf(false) }
    Dialog(close, properties = DialogProperties(usePlatformDefaultWidth = false, decorFitsSystemWindows = false)) {
        Surface(Modifier.fillMaxSize().systemBarsPadding(), color = MaterialTheme.colorScheme.background) {
            Column(Modifier.fillMaxSize()) {
                Row(Modifier.fillMaxWidth().horizontalScroll(rememberScrollState()), horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                    TextButton(onClick = close) { Text("Chiudi") }
                    FilterChip(layout == ReportLayout.SUMMARY, { select(ReportLayout.SUMMARY) }, label = { Text("Sintetico") }, modifier = Modifier.testTag("preview-summary"))
                    FilterChip(layout == ReportLayout.DETAILED, { select(ReportLayout.DETAILED) }, label = { Text("Dettagliato") }, modifier = Modifier.testTag("preview-detailed"))
                    TextButton(onClick = { zoom = !zoom }) { Text(if (zoom) "Adatta" else "Ingrandisci") }
                }
                Text("${if (snapshot.draft) "BOZZA · " else ""}${snapshot.property.name} · ${document?.pages ?: "…"} pagine", Modifier.padding(12.dp))
                LazyColumn(Modifier.weight(1f).testTag("pdf-pages"), contentPadding = PaddingValues(12.dp), verticalArrangement = Arrangement.spacedBy(12.dp)) {
                    val loaded = document
                    if (loaded != null) items(loaded.pages) { index ->
                        Column(Modifier.then(if (zoom) Modifier.horizontalScroll(rememberScrollState()) else Modifier)) { PdfPage(loaded, index, zoom) }
                    } else item { if (state.failed) Text("Anteprima non disponibile: chiudi e riprova.") else CircularProgressIndicator() }
                }
                Button(onClick = export, enabled = document != null, modifier = Modifier.padding(12.dp).fillMaxWidth().testTag("preview-export")) { Text("Esporta questo layout") }
            }
        }
    }
}
