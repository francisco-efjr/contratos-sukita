package com.sukita.contratos.ui

import androidx.compose.ui.text.AnnotatedString
import androidx.compose.ui.text.input.OffsetMapping
import androidx.compose.ui.text.input.TransformedText
import androidx.compose.ui.text.input.VisualTransformation

/**
 * Aplica a máscara visual de CPF (000.000.000-00) sem alterar o valor armazenado.
 * O TextField deve armazenar apenas dígitos (máx. 11).
 */
class CpfVisualTransformation : VisualTransformation {

    override fun filter(text: AnnotatedString): TransformedText {
        val raw = text.text.take(11)

        // Monta o string mascarado: 078.440.702-93
        val masked = buildString {
            raw.forEachIndexed { i, c ->
                if (i == 3 || i == 6) append('.')
                if (i == 9) append('-')
                append(c)
            }
        }

        val offsetMapping = object : OffsetMapping {
            // Cursor no espaço raw → posição no espaço transformado (após o separador, não sobre ele)
            // Ex: raw cursor 3 (entre '8' e '4') → transformed 4 (após o '.' em "078.|440")
            override fun originalToTransformed(offset: Int): Int {
                val o = offset.coerceIn(0, raw.length)
                return when {
                    o < 3  -> o           // 0,1,2 → sem separadores antes
                    o < 6  -> o + 1       // 3,4,5 → após 1º ponto
                    o < 9  -> o + 2       // 6,7,8 → após 2 pontos
                    else   -> o + 3       // 9,10,11 → após 2 pontos + traço
                }.coerceAtMost(masked.length)
            }

            // Cursor no espaço transformado → posição raw (separador e posição seguinte ambos mapeiam para o mesmo raw)
            override fun transformedToOriginal(offset: Int): Int {
                val t = offset.coerceIn(0, masked.length)
                return when {
                    t <= 3  -> t          // antes/sobre 1º ponto → raw 0..3
                    t <= 7  -> t - 1      // após 1º ponto → raw 3..6
                    t <= 11 -> t - 2      // após 2º ponto → raw 6..9
                    else    -> t - 3      // após traço → raw 9..11
                }.coerceIn(0, raw.length)
            }
        }

        return TransformedText(AnnotatedString(masked), offsetMapping)
    }
}

/**
 * Aplica a máscara visual de data (DD/MM/AAAA) sem alterar o valor armazenado.
 * O TextField deve armazenar apenas dígitos (máx. 8).
 */
class DateVisualTransformation : VisualTransformation {

    override fun filter(text: AnnotatedString): TransformedText {
        val raw = text.text.take(8)

        // Monta o string mascarado: 07/05/2026
        val masked = buildString {
            raw.forEachIndexed { i, c ->
                if (i == 2 || i == 4) append('/')
                append(c)
            }
        }

        val offsetMapping = object : OffsetMapping {
            // Cursor após separador, não sobre ele (igual à lógica do CPF)
            // Ex: raw cursor 2 (entre '7' e '0') → transformed 3 (após '/' em "07/|05")
            override fun originalToTransformed(offset: Int): Int {
                val o = offset.coerceIn(0, raw.length)
                return when {
                    o < 2  -> o           // 0,1 → antes da 1ª barra
                    o < 4  -> o + 1       // 2,3 → após 1ª barra
                    else   -> o + 2       // 4..8 → após 2 barras
                }.coerceAtMost(masked.length)
            }

            override fun transformedToOriginal(offset: Int): Int {
                val t = offset.coerceIn(0, masked.length)
                return when {
                    t <= 2 -> t
                    t <= 5 -> t - 1
                    else   -> t - 2
                }.coerceIn(0, raw.length)
            }
        }

        return TransformedText(AnnotatedString(masked), offsetMapping)
    }
}
