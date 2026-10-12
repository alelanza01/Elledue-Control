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
        text("Elledue Management · ${snapshot.month} · v${snapshot.version}", left, 807f, 8f)
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
        text("${snapshot.month}  ·  VERSIONE ${snapshot.version}", 310f, 48f, 9f, gold)
        text("${if (snapshot.draft) "BOZZA · " else ""}${snapshot.id.take(8)}${if (snapshot.previousId != null) " · RETTIFICATIVO" else ""}", 310f, 67f, 8f, Color.WHITE)
        y = 115f
    }
    private fun ensure(height: Float) { if (y + height > bottom) newPage() }
    private fun paragraph(value: String, size: Float = 9f, bold: Boolean = false) {
        val wrapped = lines(value, right - left, size, bold)
        if (wrapped.size * (size + 5f) + 5f <= bottom - 115f) ensure(wrapped.size * (size + 5f) + 5f)
        wrapped.forEach { line -> ensure(size + 7f); text(line, left, y, size, bold = bold); y += size + 5f }
        y += 5f
    }
    private fun section(title: String) { ensure(48f); y += 8f; text(title, left, y, 13f, night, true); y += 22f; rect(left, y, right, y + 1f, gold); y += 8f }
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
    private fun amount(label: String, cents: Long) { ensure(20f); text(label, left, y, 9f); val value = Amounts.euro(cents); text(value, right - width(value, 9f), y); y += 21f }
    fun write(value: ReportSnapshot, output: OutputStream, layout: ReportLayout = ReportLayout.DETAILED) {
        require(value.issues().none { it.blocking }) { "Snapshot incoerente." }
        snapshot = value; PDFBoxResourceLoader.init(context); document = PDDocument(); number = 0; rowCounter = 0
        regular = context.assets.open("fonts/DejaVuSans.ttf").use { PDType0Font.load(document, it) }; boldFont = context.assets.open("fonts/DejaVuSans-Bold.ttf").use { PDType0Font.load(document, it) }
        try {
            newPage()
            paragraph(value.property.name, 18f, true); paragraph("Proprietario: ${value.owner.name}")
            paragraph("Periodo: ${value.month} · Emissione: ${Instant.ofEpochMilli(value.issuedAt).atZone(ZoneId.of("Europe/Rome")).format(DateTimeFormatter.ofPattern("dd/MM/yyyy"))}")
            if (layout == ReportLayout.DETAILED) paragraph("Identificativo: ${value.id.take(8)} · versione ${value.version}")
            if (value.previousId != null) paragraph("Rettifica della versione precedente: ${value.revisionReason}")
            section("01  Prenotazioni")
            if (layout == ReportLayout.SUMMARY) {
                table(listOf("Ospite", "Soggiorno / ospiti", "Lordo", "Compenso"), listOf(175f, 155f, 90f, 95f), value.bookings.filter { it.status != "CANCELLED" }.map {
                    listOf("${it.guestFirstName} ${it.guestLastName}", "${BookingDates.display(it.checkInEpochDay)} → ${BookingDates.display(it.checkOutEpochDay)} · ${it.guestCount} ospiti", Amounts.euro(it.grossCents), Amounts.euro(it.managementCents))
                })
            } else {
            table(listOf("Ospite / portale", "Soggiorno", "Lordo", "OTA", "Netto", "Gestione", "Compenso"), listOf(139f, 81f, 59f, 58f, 60f, 51f, 67f), value.bookings.filter { it.status != "CANCELLED" }.map {
                listOf("${it.guestFirstName} ${it.guestLastName}\n${it.guestCount} ospiti · ${it.platform}\n${it.checkOutEpochDay - it.checkInEpochDay} notti", "${BookingDates.display(it.checkInEpochDay)}\n${BookingDates.display(it.checkOutEpochDay)}", Amounts.euro(it.grossCents), "${Amounts.euro(it.otaCents)}\n${if (it.actualOtaCents == null) "Stimata" else "Effettiva"}", Amounts.euro(it.netCents), "${Amounts.editable(it.appliedManagementBasisPoints.toLong())}%", Amounts.euro(it.managementCents))
            })
            }
            val totals = value.summary()
            amount("Lordo prenotazioni (tassa esclusa)", value.gross()); if (layout == ReportLayout.DETAILED) { amount("Commissioni OTA", value.ota()); amount("Netto dopo OTA al proprietario", value.net()) }; if (layout == ReportLayout.DETAILED) amount("Compenso Elledue maturato", totals.fees)
            if (layout == ReportLayout.DETAILED) {
            section("02  Pulizie")
            fun bookingName(id: String?) = (value.bookings + value.linkedBookings.orEmpty()).firstOrNull { it.id == id }?.let { "${it.guestFirstName} ${it.guestLastName}" } ?: if (id == null) "Non collegata" else "Collegamento storico"
            table(listOf("Data / prenotazione", "Intervento", "Ore", "Tariffa", "Costo effettivo"), listOf(126f, 177f, 50f, 73f, 89f), value.expenses.filter { it.kind == "CLEANING" }.map { listOf("${BookingDates.display(it.dateEpochDay)}\n${bookingName(it.reservationId)}", it.description, it.quantity ?: "—", it.unitCents?.let(Amounts::euro) ?: "—", Amounts.euro(it.actualCents)) })
            amount("Totale pulizie", totals.cleaning)
            section("03  Lavanderia")
            table(listOf("Data", "Tipologia set", "Quantità", "Tariffa", "Costo effettivo"), listOf(80f, 223f, 50f, 73f, 89f), value.expenses.filter { it.kind == "LAUNDRY" }.map { listOf(BookingDates.display(it.dateEpochDay), it.setDefinition, it.quantity ?: "—", it.unitCents?.let(Amounts::euro) ?: "—", Amounts.euro(it.actualCents)) })
            amount("Totale lavanderia", totals.laundry)
            section("04  Altre spese")
            table(listOf("Data / categoria", "Descrizione / note", "Rimborso", "Importo"), listOf(127f, 223f, 76f, 89f), value.expenses.filter { it.kind == "OTHER" }.map { listOf("${BookingDates.display(it.dateEpochDay)}\n${it.category}", "${it.description}\n${it.notes}", if (it.advancedByElledue && it.reimbursable) "Sì" else "No", Amounts.euro(it.actualCents)) })
            amount("Spese complessive", totals.expenses); amount("Spese anticipate da Elledue", totals.advanced); amount("Spese anticipate e rimborsabili", totals.reimbursable)
            paragraph("Le tariffe e i costi effettivi degli interventi sono storici. Nel saldo entrano soltanto spese anticipate da Elledue e rimborsabili dal proprietario.", 8f)
            } else {
                section("02  Spese rimborsabili")
                table(listOf("Categoria", "Importo"), listOf(426f, 89f), value.expenses.filter { it.advancedByElledue && it.reimbursable }.groupBy { it.category }.map { (category, rows) -> listOf(category, Amounts.euro(OperationalMoney.total(rows.map { it.actualCents }))) })
            }
            section("Tassa di soggiorno e anticipi")
            amount("Tassa di soggiorno riscossa", totals.collectedTax)
            amount("Altri anticipi", totals.payments)
            if (totals.collectedTax != totals.compensated) paragraph("Nel saldo sono riconosciuti ${Amounts.euro(totals.compensated)} di tassa, a fronte di ${Amounts.euro(totals.collectedTax)} riscossi. Riscossione e compensazione sono registrazioni distinte; possono riferirsi a periodi differenti.", 8f)
            if (value.notes.isNotBlank()) { section("Note"); paragraph(value.notes) }
            ensure(if (layout == ReportLayout.SUMMARY) 180f else 210f)
            section("Saldo del periodo")
            amount("Compenso di gestione", totals.fees); amount("Rimborsi anticipati", totals.reimbursable); if (layout == ReportLayout.SUMMARY) amount("Meno anticipi riconosciuti (tassa e altri)", OperationalMoney.total(listOf(totals.compensated, totals.payments))) else { amount("Meno compensazioni riconosciute", totals.compensated); amount("Meno altri anticipi / pagamenti", totals.payments) }
            ensure(76f); rect(left, y, right, y + 76f, night)
            text(if (totals.balance >= 0) "BONIFICO ANCORA DOVUTO A ELLEDUE" else "CREDITO DA RICONCILIARE", left + 15f, y + 12f, 10f, gold, true)
            val balance = java.math.BigDecimal.valueOf(totals.balance).abs().movePointLeft(2).toPlainString().replace('.', ',') + " €"
            text(balance, left + 15f, y + 33f, 23f, Color.WHITE, true); y += 91f
            finish(); document.save(output)
        } finally { document.close() }
    }
}
