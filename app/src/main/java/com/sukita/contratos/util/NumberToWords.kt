package com.sukita.contratos.util

/**
 * Converte valores monetários em extenso no português brasileiro.
 * Ex: 650_00 centavos → "seiscentos e cinquenta reais"
 * Ex: 1500_00 centavos → "mil e quinhentos reais"
 * Ex: 1500_50 centavos → "mil e quinhentos reais e cinquenta centavos"
 */
object NumberToWords {

    private val units = listOf(
        "", "um", "dois", "três", "quatro", "cinco",
        "seis", "sete", "oito", "nove", "dez",
        "onze", "doze", "treze", "quatorze", "quinze",
        "dezesseis", "dezessete", "dezoito", "dezenove"
    )
    private val tens = listOf(
        "", "", "vinte", "trinta", "quarenta", "cinquenta",
        "sessenta", "setenta", "oitenta", "noventa"
    )
    private val hundreds = listOf(
        "", "cem", "duzentos", "trezentos", "quatrocentos", "quinhentos",
        "seiscentos", "setecentos", "oitocentos", "novecentos"
    )

    /** Converte número inteiro até 999.999.999 em extenso */
    private fun intToWords(n: Int): String {
        if (n == 0) return "zero"
        if (n == 100) return "cem"

        val parts = mutableListOf<String>()

        val millions = n / 1_000_000
        val thousands = (n % 1_000_000) / 1_000
        val rest = n % 1_000

        if (millions > 0) {
            val milStr = hundredsToWords(millions)
            parts.add(if (millions == 1) "$milStr milhão" else "$milStr milhões")
        }
        if (thousands > 0) {
            val milStr = hundredsToWords(thousands)
            parts.add(if (thousands == 1) "mil" else "$milStr mil")
        }
        if (rest > 0) {
            parts.add(hundredsToWords(rest))
        }

        return parts.joinToString(" e ")
    }

    private fun hundredsToWords(n: Int): String {
        if (n == 0) return ""
        val h = n / 100
        val t = (n % 100) / 10
        val u = n % 10
        val tm = n % 100  // tens + units combined

        val parts = mutableListOf<String>()
        if (h > 0) parts.add(if (n == 100) "cem" else hundreds[h])
        if (tm > 0) {
            if (tm < 20) {
                parts.add(units[tm])
            } else {
                val tenStr = tens[t]
                val unitStr = if (u > 0) units[u] else ""
                parts.add(if (unitStr.isEmpty()) tenStr else "$tenStr e $unitStr")
            }
        }
        return parts.joinToString(" e ")
    }

    /**
     * @param cents valor em centavos (ex: 65000 = R$650,00)
     * @return extenso em MAIÚSCULAS, ex: "SEISCENTOS E CINQUENTA REAIS"
     */
    fun fromCents(cents: Long): String {
        if (cents <= 0L) return "ZERO REAIS"

        val reais = (cents / 100).toInt()
        val centavos = (cents % 100).toInt()

        val reaisStr = if (reais > 0) {
            val ext = intToWords(reais)
            if (reais == 1) "$ext real" else "$ext reais"
        } else ""

        val centStr = if (centavos > 0) {
            val ext = intToWords(centavos)
            if (centavos == 1) "$ext centavo" else "$ext centavos"
        } else ""

        val result = when {
            reaisStr.isNotEmpty() && centStr.isNotEmpty() -> "$reaisStr e $centStr"
            reaisStr.isNotEmpty() -> reaisStr
            else -> centStr
        }
        return result.uppercase()
    }

    /**
     * Converte meses em extenso MAIÚSCULAS.
     * Ex: 12 → "DOZE MESES"
     */
    fun monthsToWords(months: Int): String {
        val ext = intToWords(months)
        return if (months == 1) "${ext.uppercase()} MÊS" else "${ext.uppercase()} MESES"
    }
}
