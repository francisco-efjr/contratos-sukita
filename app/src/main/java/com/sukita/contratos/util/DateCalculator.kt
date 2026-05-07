package com.sukita.contratos.util

import java.util.Calendar

/**
 * Utilitário para cálculo e formatação de datas no padrão brasileiro.
 */
object DateCalculator {

    private val MONTHS_PT = listOf(
        "Janeiro", "Fevereiro", "Março", "Abril", "Maio", "Junho",
        "Julho", "Agosto", "Setembro", "Outubro", "Novembro", "Dezembro"
    )

    /**
     * Calcula a data de término somando [months] meses à data de início.
     * @param startDate no formato DD/MM/YYYY
     * @param months    número de meses do prazo
     * @return data de término no formato DD/MM/YYYY, ou null se startDate inválido
     */
    fun calcEndDate(startDate: String, months: Int): String? {
        val parts = startDate.split("/")
        if (parts.size != 3) return null
        val day   = parts[0].toIntOrNull() ?: return null
        val month = parts[1].toIntOrNull() ?: return null
        val year  = parts[2].toIntOrNull() ?: return null

        val cal = Calendar.getInstance()
        cal.isLenient = false
        return try {
            cal.set(year, month - 1, day)
            cal.add(Calendar.MONTH, months)
            "%02d/%02d/%04d".format(
                cal.get(Calendar.DAY_OF_MONTH),
                cal.get(Calendar.MONTH) + 1,
                cal.get(Calendar.YEAR)
            )
        } catch (e: Exception) {
            null
        }
    }

    /**
     * Formata data DD/MM/YYYY para extenso localizado:
     * "07 de Maio de 2026"
     */
    fun toWrittenDate(date: String): String {
        val parts = date.split("/")
        if (parts.size != 3) return date
        val day   = parts[0].toIntOrNull() ?: return date
        val month = parts[1].toIntOrNull()?.takeIf { it in 1..12 } ?: return date
        val year  = parts[2]
        return "%02d de %s de %s".format(day, MONTHS_PT[month - 1], year)
    }

    /**
     * Valida data no formato DD/MM/YYYY.
     * Retorna mensagem de erro localizada, ou null se válida.
     */
    fun validate(date: String): String? {
        val parts = date.split("/")
        if (parts.size != 3) return "Use o formato DD/MM/AAAA"
        val day   = parts[0].toIntOrNull() ?: return "Dia inválido"
        val month = parts[1].toIntOrNull() ?: return "Mês inválido"
        val year  = parts[2].toIntOrNull() ?: return "Ano inválido"
        if (month !in 1..12) return "Mês deve ser entre 01 e 12"
        if (day !in 1..31)   return "Dia deve ser entre 01 e 31"
        if (year < 2000)     return "Ano parece inválido"
        return null
    }

    /** Aplica máscara de data enquanto o usuário digita: DD/MM/AAAA */
    fun applyMask(input: String): String {
        val digits = input.filter { it.isDigit() }.take(8)
        return buildString {
            for (i in digits.indices) {
                append(digits[i])
                if (i == 1 || i == 3) append('/')
            }
        }
    }
}
