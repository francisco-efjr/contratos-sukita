package com.sukita.contratos.util

/**
 * Formata e valida CPF no padrão brasileiro (000.000.000-00).
 */
object CpfFormatter {

    /** Remove tudo que não for dígito */
    fun digitsOnly(cpf: String): String = cpf.filter { it.isDigit() }

    /** Aplica máscara: 078.440.702-93 */
    fun format(cpf: String): String {
        val d = digitsOnly(cpf)
        return when {
            d.length <= 3  -> d
            d.length <= 6  -> "${d.substring(0,3)}.${d.substring(3)}"
            d.length <= 9  -> "${d.substring(0,3)}.${d.substring(3,6)}.${d.substring(6)}"
            else           -> "${d.substring(0,3)}.${d.substring(3,6)}.${d.substring(6,9)}-${d.substring(9, minOf(11,d.length))}"
        }
    }

    /**
     * Valida CPF completo (11 dígitos + dígitos verificadores).
     * Retorna true se válido.
     */
    fun isValid(cpf: String): Boolean {
        val d = digitsOnly(cpf)
        if (d.length != 11) return false
        if (d.all { it == d[0] }) return false  // Ex: 111.111.111-11

        fun calcDigit(slice: List<Int>, weights: IntRange): Int {
            val sum = slice.zip(weights.toList()).sumOf { (v, w) -> v * w }
            val rem = sum % 11
            return if (rem < 2) 0 else 11 - rem
        }

        val digits = d.map { it.digitToInt() }
        val d1 = calcDigit(digits.take(9), 10 downTo 2)
        val d2 = calcDigit(digits.take(10), 11 downTo 2)
        return digits[9] == d1 && digits[10] == d2
    }
}
