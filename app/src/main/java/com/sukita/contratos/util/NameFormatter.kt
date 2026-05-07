package com.sukita.contratos.util

/**
 * Formata nomes de pessoas de acordo com as regras do contrato.
 */
object NameFormatter {

    private val PREPOSITIONS = setOf("da", "de", "do", "das", "dos", "e")

    /**
     * Capitaliza o nome corretamente:
     * - Primeira letra de cada palavra em maiúscula
     * - Preposições (da, de, do...) em minúscula
     * Ex: "KARINA DA COSTA MENDONCA" → "Karina da Costa Mendonca"
     */
    fun capitalize(name: String): String {
        return name.trim().split(" ")
            .filter { it.isNotEmpty() }
            .mapIndexed { index, word ->
                val lower = word.lowercase()
                if (index == 0 || lower !in PREPOSITIONS) {
                    lower.replaceFirstChar { it.uppercaseChar() }
                } else {
                    lower
                }
            }
            .joinToString(" ")
    }

    /** Retorna o nome em MAIÚSCULAS para o bloco de assinatura */
    fun toUpper(name: String): String = name.trim().uppercase()
}
