package com.sukita.contratos.util

/**
 * Utilitário de formatação de valores monetários em Real (BRL).
 */
object CurrencyFormatter {

    /**
     * Converte centavos para string formatada: "650,00"
     * (sem o prefixo R$ — o template HTML já inclui)
     */
    fun format(cents: Long): String {
        val reais = cents / 100
        val centavos = cents % 100
        return "%,d,%02d".format(reais, centavos)
            .replace(",", "X")
            .replace(".", ",")
            .replace("X", ".")
    }

    /**
     * Converte string digitada pelo usuário (ex: "1500,00" ou "1500.00")
     * em centavos. Retorna null se inválido.
     */
    fun parseToCents(input: String): Long? {
        val cleaned = input.trim()
            .replace("R$", "")
            .replace(" ", "")
            .replace(".", "")   // separador de milhar
            .replace(",", ".")  // separador decimal → ponto
        return try {
            val value = cleaned.toDouble()
            if (value < 0) null else (value * 100).toLong()
        } catch (e: NumberFormatException) {
            null
        }
    }
}
