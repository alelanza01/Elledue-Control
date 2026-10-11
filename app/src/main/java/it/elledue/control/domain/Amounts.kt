package it.elledue.control.domain

import java.math.BigDecimal
import java.math.RoundingMode
import java.text.NumberFormat
import java.util.Locale

/** EUR cents and basis points (1% = 100 bp). HALF_UP for commission amounts. */
object Amounts {
    private val decimalPattern = Regex("\\d+(?:[.,]\\d{1,2})?")

    fun parseCents(input: String): Long {
        val value = input.trim()
        require(decimalPattern.matches(value)) { "Usa un importo positivo con al massimo due decimali." }
        return BigDecimal(value.replace(',', '.')).movePointRight(2).longValueExact()
    }

    fun parseBasisPoints(input: String): Int {
        val result = parseCents(input)
        require(result in 0..10_000) { "La percentuale deve essere tra 0 e 100." }
        return result.toInt()
    }

    fun percentage(cents: Long, basisPoints: Int): Long {
        require(cents >= 0 && basisPoints in 0..10_000)
        return BigDecimal.valueOf(cents).multiply(BigDecimal.valueOf(basisPoints.toLong()))
            .divide(BigDecimal.valueOf(10_000), 0, RoundingMode.HALF_UP).longValueExact()
    }

    fun editable(cents: Long): String = BigDecimal.valueOf(cents, 2).toPlainString().replace('.', ',')
    fun euro(cents: Long): String = NumberFormat.getCurrencyInstance(Locale.ITALY)
        .format(BigDecimal.valueOf(cents, 2))
}
