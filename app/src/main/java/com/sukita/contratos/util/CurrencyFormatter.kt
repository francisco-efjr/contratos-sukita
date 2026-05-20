package com.sukita.contratos.util

import java.math.BigDecimal
import java.math.RoundingMode
import java.text.NumberFormat
import java.util.Locale

/**
 * Utilitário de formatação de valores monetários em Real (BRL).
 */
object CurrencyFormatter {

    private val ptBR = Locale("pt", "BR")

    /**
     * Converte centavos para string formatada: "650,00" / "1.500,00"
     * (sem o prefixo R$ — o template HTML já inclui)
     */
    fun format(cents: Long): String {
        val reais    = cents / 100
        val centavos = cents % 100
        // Usa o separador de milhar do pt-BR ('.')  + vírgula decimal
        val nf = NumberFormat.getIntegerInstance(ptBR)
        nf.isGroupingUsed = true
        return "${nf.format(reais)},${"%02d".format(centavos)}"
    }

    /**
     * Converte string digitada pelo usuário (ex: "1500,00" ou "1500.00")
     * em centavos. Retorna null se inválido.
     */
    fun parseToCents(input: String): Long? {
        val cleaned = input
            .trim()
            .replace("R$", "")
            .replace(" ", "")
        val normalized = when {
            cleaned.contains('.') && cleaned.contains(',') ->
                cleaned.replace(".", "").replace(",", ".")
            cleaned.contains(',') ->
                cleaned.replace(".", "").replace(",", ".")
            cleaned.count { it == '.' } == 1 && Regex("""\.\d{1,2}$""").containsMatchIn(cleaned) ->
                cleaned
            else ->
                cleaned.replace(".", "")
        }

        val value = normalized.toBigDecimalOrNull() ?: return null
        if (value < BigDecimal.ZERO) return null
        return value
            .movePointRight(2)
            .setScale(0, RoundingMode.HALF_UP)
            .toLong()
    }
}
