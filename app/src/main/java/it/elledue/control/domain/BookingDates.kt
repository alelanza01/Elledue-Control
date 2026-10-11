package it.elledue.control.domain

import java.time.*
import java.time.format.DateTimeFormatter
import java.time.format.ResolverStyle
import java.util.Locale

object BookingDates {
    val zone: ZoneId = ZoneId.of("Europe/Rome")
    val inputFormat: DateTimeFormatter = DateTimeFormatter.ofPattern("dd/MM/uuuu", Locale.ITALY).withResolverStyle(ResolverStyle.STRICT)
    fun parse(text: String): LocalDate = try { LocalDate.parse(text.trim(), inputFormat) }
        catch (_: Exception) { throw IllegalArgumentException("Inserisci una data valida nel formato gg/mm/aaaa.") }
    fun display(epochDay: Long): String = LocalDate.ofEpochDay(epochDay).format(inputFormat)
    fun today(clock: Clock = Clock.system(zone)): LocalDate = LocalDate.now(clock.withZone(zone))
    fun overlaps(aStart: Long, aEnd: Long, bStart: Long, bEnd: Long) = aStart < bEnd && bStart < aEnd
    fun pickerMillis(date: LocalDate): Long = date.atStartOfDay(ZoneOffset.UTC).toInstant().toEpochMilli()
    fun fromPicker(millis: Long): LocalDate = Instant.ofEpochMilli(millis).atZone(ZoneOffset.UTC).toLocalDate()
    fun time(value: String, optional: Boolean = false): String? {
        if (optional && value.isBlank()) return null
        require(Regex("[0-2][0-9]:[0-5][0-9]").matches(value)) { "Usa orari validi nel formato HH:mm." }
        try { LocalTime.parse(value) } catch (_: Exception) { throw IllegalArgumentException("Usa orari validi nel formato HH:mm.") }
        return value
    }
}

enum class BookingStatus(val label: String) {
    CONFIRMED("Confermata"), COMPLETED("Conclusa"), CANCELLED("Annullata");
}
