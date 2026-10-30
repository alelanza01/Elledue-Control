package it.elledue.control.reports

import android.graphics.*
import android.content.Context
import com.tom_roush.pdfbox.android.PDFBoxResourceLoader
import com.tom_roush.pdfbox.pdmodel.*
import com.tom_roush.pdfbox.pdmodel.font.PDType0Font
import com.tom_roush.pdfbox.pdmodel.graphics.image.PDImageXObject
import android.util.Base64
import it.elledue.control.domain.*
import java.io.OutputStream
import java.time.Instant
import java.time.ZoneId
import java.time.format.DateTimeFormatter

/** A4 vector text/tables. Layout floats only; every accounting value remains integer cents. */
enum class ReportLayout { SUMMARY, DETAILED }

class ReportPdf(private val context: Context) {
    private val night = Color.rgb(17, 27, 50); private val gold = Color.rgb(219, 191, 139)
    private lateinit var document: PDDocument; private lateinit var content: PDPageContentStream
    private lateinit var regular: PDType0Font; private lateinit var boldFont: PDType0Font
    private lateinit var snapshot: ReportSnapshot
    private var y = 0f; private var number = 0
    private var rowCounter = 0
    private var compact=false
    val pageCount get() = number
    private val left = 40f; private val right = 555f; private val bottom = 775f
    private fun width(value: String, size: Float, bold: Boolean = false): Float = (if (bold) boldFont else regular).getStringWidth(value) * size / 1000f
    private fun fill(color: Int) { content.setNonStrokingColor(Color.red(color) / 255f, Color.green(color) / 255f, Color.blue(color) / 255f) }
    private fun rect(x1: Float, top: Float, x2: Float, end: Float, color: Int) { fill(color); content.addRect(x1, 842f - end, x2 - x1, end - top); content.fill() }
    private fun text(value: String, x: Float, top: Float, size: Float = 9f, color: Int = night, bold: Boolean = false) {
        if (value.isEmpty()) return
        fill(color); content.beginText(); content.setFont(if (bold) boldFont else regular, size); content.newLineAtOffset(x, 842f - top - size); content.showText(value); content.endText()
    }
    private fun lines(value: String, available: Float, size: Float, bold: Boolean = false): List<String> {
        val result = mutableListOf<String>()
        value.split('\n').forEach { paragraph ->
            if (paragraph.isEmpty()) result.add("")
            else {
                var rest = paragraph
                while (rest.isNotEmpty()) {
                    var count = 0; var used = 0f
                    while (count < rest.length) { val next = count + Character.charCount(rest.codePointAt(count)); val w = width(rest.substring(count, next), size, bold); if (used + w > available && count > 0) break; used += w; count = next }
                    if (count < rest.length) { val space = rest.lastIndexOf(' ', count - 1); if (space > 0) count = space }
                    result.add(rest.take(count)); rest = rest.drop(count).trimStart()
                }
            }
        }; return result
    }
    private fun finish() {
        rect(left, 795f, right, 795.5f, Color.rgb(220, 224, 230))
        text("Elledue Management · ${snapshot.month}", left, 807f, 8f)
        text("Pagina $number", 505f, 807f, 8f); content.close()
    }
    private fun newPage() {
        if (number > 0) finish()
        number++; val page = PDPage(com.tom_roush.pdfbox.pdmodel.common.PDRectangle.A4); document.addPage(page); content = PDPageContentStream(document, page)
        rect(0f, 0f, 595f, 95f, night)
        val bytes = Base64.decode(snapshot.logoBase64, Base64.DEFAULT)
        val original = BitmapFactory.decodeByteArray(bytes, 0, bytes.size)
        val opaque = Bitmap.createBitmap(original.width, original.height, Bitmap.Config.ARGB_8888)
        Canvas(opaque).apply { drawColor(night); drawBitmap(original, 0f, 0f, null) }
        opaque.setHasAlpha(false)
        val logo = com.tom_roush.pdfbox.pdmodel.graphics.image.LosslessFactory.createFromImage(document, opaque)
        original.recycle(); opaque.recycle()
        val width = 155f; val height = width * logo.height / logo.width
        content.drawImage(logo, left, 842f - 28f - height, width, height)
        text("RENDICONTO MENSILE", 310f, 26f, 12f, Color.WHITE, true)
        text(snapshot.month, 310f, 48f, 9f, gold)
        text(if(snapshot.draft)"BOZZA · NON DEFINITIVO" else if(snapshot.previousId!=null)"RENDICONTO RETTIFICATIVO" else "",310f,67f,8f,Color.WHITE)
        y = if(compact)105f else 115f
    }
    private fun ensure(height: Float) { if (y + height > bottom) newPage() }
    private fun paragraph(value: String, size: Float = 9f, bold: Boolean = false) {
        val wrapped = lines(value, right - left, size, bold)
        if (wrapped.size * (size + 5f) + 5f <= bottom - 115f) ensure(wrapped.size * (size + 5f) + 5f)
        wrapped.forEach { line -> ensure(size + 7f); text(line, left, y, size, bold = bold); y += size + if(compact)3f else 5f }
        y += if(compact)3f else 5f
    }
    private fun section(title: String) { ensure(48f); y += if(compact)5f else 8f; text(title, left, y, 13f, night, true); y += if(compact)18f else 22f; rect(left, y, right, y + 1f, gold); y += if(compact)5f else 8f }
    private fun table(headers: List<String>, widths: List<Float>, rows: List<List<String>>) {
        fun header() {
            val height = headers.maxOf { lines(it, widths[headers.indexOf(it)] - 10f, 8f, true).size } * 12f + 14f
            ensure(height + 20f); rect(left, y, right, y + height, night)
            var x = left
            headers.forEachIndexed { i, value -> lines(value, widths[i] - 10f, 8f, true).forEachIndexed { j, line -> text(line, x + 5f, y + 5f + j * 12f, 8f, Color.WHITE, true) }; x += widths[i] }
            y += height
        }
        header()
        if (rows.isEmpty()) { paragraph("Nessuna voce nel periodo."); return }
        rows.forEach { values ->
            val cells = values.mapIndexed { i, value -> lines(value, widths[i] - 10f, 8f) }
            var offset = 0; val total = cells.maxOf { it.size }
            while (offset < total) {
                if (bottom - y < 28f) { newPage(); header() }
                val count = minOf(total - offset, ((bottom - y - 12f) / 11f).toInt().coerceAtLeast(1))
                val height = count * 11f + 12f
                rect(left, y, right, y + height, if (rowCounter % 2 == 0) Color.rgb(245, 247, 250) else Color.WHITE)
                var x = left
                cells.forEachIndexed { i, cell ->
                    cell.drop(offset).take(count).forEachIndexed { j, line ->
                        // Financial cells align to the right; text wraps without ellipses or clipping.
                        val position = if (headers[i] in listOf("Lordo", "OTA", "Netto", "Gestione", "Compenso", "Ore", "Quantità", "Tariffa", "Costo effettivo", "Importo")) x + widths[i] - 5f - width(line, 8f) else x + 5f
                        text(line, position, y + 5f + j * 11f, 8f)
                    }; x += widths[i]
                }
                y += height; offset += count
                if (offset < total) { newPage(); header() }
            }
            rowCounter++
        }
        y += 10f
    }
    private fun amount(label: String, cents: Long) { ensure(20f); text(label, left, y, 9f); val value = Amounts.euro(cents); text(value, right - width(value, 9f), y); y += if(compact)17f else 21f }
    fun write(value: ReportSnapshot, output: OutputStream, layout: ReportLayout = ReportLayout.DETAILED) {
        require(value.issues().none { it.blocking }) { "Snapshot incoerente." }
        compact=layout==ReportLayout.SUMMARY
        snapshot = value; PDFBoxResourceLoader.init(context); document = PDDocument(); number = 0; rowCounter = 0
        regular = context.assets.open("fonts/DejaVuSans.ttf").use { PDType0Font.load(document, it) }; boldFont = context.assets.open("fonts/DejaVuSans-Bold.ttf").use { PDType0Font.load(document, it) }
        try {
            newPage()
            paragraph(value.property.name,18f,true)
            paragraph("Periodo: ${value.month} · Emissione: ${Instant.ofEpochMilli(value.issuedAt).atZone(ZoneId.of("Europe/Rome")).format(DateTimeFormatter.ofPattern("dd/MM/yyyy"))}")
            section("01  Prenotazioni e ricavi")
            val visible=value.bookings.filter{it.status!="CANCELLED" && it.id !in value.hiddenBookingIds.orEmpty()}
            if(compact) {
                table(listOf("Ospite","Soggiorno / ospiti","Lordo","OTA"),listOf(175f,155f,90f,95f),visible.map {
                    listOf("${it.guestFirstName} ${it.guestLastName}","${BookingDates.display(it.checkInEpochDay)} → ${BookingDates.display(it.checkOutEpochDay)} · ${it.guestCount} ospiti",Amounts.euro(it.grossCents),Amounts.euro(it.otaCents))
                })
            } else {
                table(listOf("Ospite / portale","Soggiorno","Lordo","OTA","Netto","Compenso"),listOf(139f,101f,65f,65f,65f,80f),visible.map {
                    listOf("${it.guestFirstName} ${it.guestLastName}\n${it.guestCount} ospiti · ${it.platform}","${BookingDates.display(it.checkInEpochDay)}\n${BookingDates.display(it.checkOutEpochDay)}",Amounts.euro(it.grossCents),Amounts.euro(it.otaCents),Amounts.euro(it.netCents),Amounts.euro(it.managementCents))
                })
            }
            if(value.hiddenBookingIds.orEmpty().isNotEmpty())paragraph("Le prenotazioni non dettagliate sono incluse nei totali.",8f)
            val totals=value.summary()
            amount("Lordo prenotazioni",value.gross())
            amount("Commissioni OTA complessive (IVA ove prevista)",value.ota())
            amount("Netto dopo OTA",value.net())
            val costs=value.effectiveExpenses().filter{!it.voided && it.advancedByElledue && it.reimbursable}
            section("02  Pulizie e lavanderia")
            if(!compact) {
                fun bookingName(id:String?)=if(id in value.hiddenBookingIds.orEmpty())"Prenotazione non dettagliata" else (value.bookings+value.linkedBookings.orEmpty()).firstOrNull{it.id==id}?.let{"${it.guestFirstName} ${it.guestLastName}"} ?: ""
                table(listOf("Data / soggiorno","Intervento","Ore / set","Tariffa","Importo"),listOf(156f,146f,57f,73f,83f),costs.filter{it.kind in listOf("CLEANING","LAUNDRY")}.map {
                    listOf("${BookingDates.display(it.dateEpochDay)}${if(!it.effectiveDateConfirmed)" (prevista)" else ""}\n${bookingName(it.reservationId)}",if(it.kind=="CLEANING")it.description else "Lavanderia · ${it.setDefinition}",it.quantity ?: "—",it.unitCents?.let(Amounts::euro) ?: "—",Amounts.euro(it.actualCents))
                })
            }
            amount("Pulizie · ${value.quantity("CLEANING")} ore",OperationalMoney.total(costs.filter{it.kind=="CLEANING"}.map{it.actualCents}))
            amount("Lavanderia · ${value.quantity("LAUNDRY")} set",OperationalMoney.total(costs.filter{it.kind=="LAUNDRY"}.map{it.actualCents}))
            section("03  Altre spese anticipate")
            val other=costs.filter{it.kind !in listOf("CLEANING","LAUNDRY") && it.advancedByElledue && it.reimbursable}
            if(compact) {
                table(listOf("Categoria","Importo"),listOf(426f,89f),other.groupBy{it.category}.toSortedMap().map{(label,rows)->listOf(label,Amounts.euro(OperationalMoney.total(rows.map{it.actualCents})))})
            } else {
                table(listOf("Data / categoria","Descrizione","Importo"),listOf(142f,284f,89f),other.map{listOf("${BookingDates.display(it.dateEpochDay)}\n${it.category}",it.description+if(it.notes.isBlank())"" else "\n${it.notes}",Amounts.euro(it.actualCents))})
            }
            amount("Totale altre spese anticipate",OperationalMoney.total(other.map{it.actualCents}))
            if(value.relatedExpenses.orEmpty().isNotEmpty()) {
                section("Spese attribuite ad altro periodo")
                paragraph("Queste voci non sono sommate al presente rendiconto.",8f)
                table(listOf("Voce","Data","Periodo","Importo"),listOf(190f,120f,105f,100f),value.relatedExpenses.orEmpty().map{listOf(it.category,BookingDates.display(it.dateEpochDay),it.competenceMonth,Amounts.euro(it.actualCents))})
            }
            if(value.notes.isNotBlank()){section("Note");paragraph(value.notes)}
            ensure(190f)
            section("RIEPILOGO FINALE")
            amount("Compenso di gestione Elledue",totals.fees)
            amount("Spese anticipate da Elledue",totals.reimbursable)
            amount("Già saldato / anticipi in compensazione",OperationalMoney.total(listOf(totals.compensated,totals.payments)))
            ensure(76f);rect(left,y,right,y+76f,night)
            text(if(totals.balance>=0)"TOTALE ANCORA DOVUTO A ELLEDUE" else "CREDITO DA RICONCILIARE",left+15f,y+12f,10f,gold,true)
            val balance=java.math.BigDecimal.valueOf(totals.balance).abs().movePointLeft(2).toPlainString().replace('.',',')+" €"
            text(balance,left+15f,y+33f,23f,Color.WHITE,true);y+=91f
            finish(); document.save(output)
        } finally { document.close() }
    }
}
