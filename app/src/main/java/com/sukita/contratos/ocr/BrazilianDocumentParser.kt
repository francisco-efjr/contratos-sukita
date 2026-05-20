package com.sukita.contratos.ocr

import com.sukita.contratos.util.CpfFormatter
import java.text.Normalizer
import java.util.Locale

enum class BrazilianDocumentType(val displayName: String) {
    CNH("CNH"),
    IDENTITY_CARD("Carteira de Identidade"),
    NATIONAL_ID_CARD("Carteira de Identidade Nacional"),
    CPF("CPF"),
    PASSPORT("Passaporte"),
    UNKNOWN("Documento")
}

data class BrazilianDocumentResult(
    val type: BrazilianDocumentType,
    val name: String = "",
    val cpf: String = "",
    val rg: String = "",
    val documentNumber: String = "",
    val rawText: String = ""
) {
    val hasAnyData: Boolean
        get() = name.isNotBlank() || cpf.isNotBlank() || rg.isNotBlank() || documentNumber.isNotBlank()

    val typeLabel: String
        get() = type.displayName
}

object BrazilianDocumentParser {

    private val localePtBr = Locale("pt", "BR")

    fun parse(rawText: String, lines: List<String> = emptyList()): BrazilianDocumentResult {
        val cleanLines = normalizeLines(lines.ifEmpty { rawText.lines() })
        val text = cleanLines.joinToString("\n").ifBlank { rawText }
        val foldedText = fold(text)
        val type = detectType(foldedText)
        val cpf = extractCpf(text)
        val rg = extractRg(cleanLines, text, cpf, type)
        val name = extractName(cleanLines, type)
        val documentNumber = when (type) {
            BrazilianDocumentType.CNH -> extractCnhNumber(cleanLines, text, cpf)
            BrazilianDocumentType.IDENTITY_CARD -> rg
            BrazilianDocumentType.NATIONAL_ID_CARD -> CpfFormatter.digitsOnly(cpf)
            BrazilianDocumentType.CPF -> CpfFormatter.digitsOnly(cpf)
            BrazilianDocumentType.PASSPORT -> extractPassportNumber(cleanLines)
            BrazilianDocumentType.UNKNOWN -> rg.ifBlank { CpfFormatter.digitsOnly(cpf) }
        }

        return BrazilianDocumentResult(
            type = type,
            name = name,
            cpf = cpf,
            rg = rg,
            documentNumber = documentNumber,
            rawText = rawText
        )
    }

    fun normalizeRg(value: String): String {
        return value
            .uppercase(localePtBr)
            .replace(Regex("""[^0-9A-Z./\- ]"""), " ")
            .replace(Regex("""\s+"""), " ")
            .trim(' ', '.', '-', '/')
            .take(40)
    }

    private fun normalizeLines(lines: List<String>): List<String> {
        return lines
            .map { it.replace(Regex("""\s+"""), " ").trim() }
            .filter { it.isNotBlank() }
    }

    private fun detectType(foldedText: String): BrazilianDocumentType {
        return when {
            foldedText.contains("CARTEIRA NACIONAL DE HABILITACAO") ||
                foldedText.contains("PERMISSAO PARA DIRIGIR") ||
                (foldedText.contains("CNH") && foldedText.contains("REGISTRO")) ||
                (foldedText.contains("DETRAN") && foldedText.contains("CAT")) ->
                BrazilianDocumentType.CNH

            foldedText.contains("CARTEIRA DE IDENTIDADE NACIONAL") ||
                foldedText.contains("IDENTIDADE NACIONAL") ||
                Regex("""\bCIN\b""").containsMatchIn(foldedText) ->
                BrazilianDocumentType.NATIONAL_ID_CARD

            foldedText.contains("PASSAPORTE") || foldedText.contains("PASSPORT") ->
                BrazilianDocumentType.PASSPORT

            foldedText.contains("CARTEIRA DE IDENTIDADE") ||
                foldedText.contains("REGISTRO GERAL") ||
                foldedText.contains("INSTITUTO DE IDENTIFICACAO") ||
                foldedText.contains("SECRETARIA DE SEGURANCA PUBLICA") ->
                BrazilianDocumentType.IDENTITY_CARD

            foldedText.contains("CADASTRO DE PESSOAS FISICAS") ||
                foldedText.contains("COMPROVANTE DE INSCRICAO") ||
                (foldedText.contains("RECEITA FEDERAL") && foldedText.contains("CPF")) ->
                BrazilianDocumentType.CPF

            else -> BrazilianDocumentType.UNKNOWN
        }
    }

    private fun extractCpf(text: String): String {
        val candidates = mutableListOf<String>()
        val candidatePattern = Regex(
            """(?<![0-9A-Z])([0-9OQDISBLZT]{3}[.\s-]?[0-9OQDISBLZT]{3}[.\s-]?[0-9OQDISBLZT]{3}[-\s]?[0-9OQDISBLZT]{2})(?![0-9A-Z])""",
            RegexOption.IGNORE_CASE
        )

        candidatePattern.findAll(text).forEach { match ->
            val digits = toDigits(match.groupValues[1])
            if (digits.length == 11) candidates += digits
        }

        val best = candidates.distinct().firstOrNull { CpfFormatter.isValid(it) }
            ?: candidates.firstOrNull()
            ?: return ""

        return CpfFormatter.format(best)
    }

    private fun extractRg(
        lines: List<String>,
        fullText: String,
        cpf: String,
        type: BrazilianDocumentType
    ): String {
        if (type == BrazilianDocumentType.CPF || type == BrazilianDocumentType.NATIONAL_ID_CARD) return ""

        val labels = listOf(
            "DOC IDENTIDADE",
            "DOCUMENTO DE IDENTIDADE",
            "DOCUMENTO IDENTIDADE",
            "REGISTRO GERAL",
            "IDENTIDADE",
            "R G",
            "RG"
        )

        for (index in lines.indices) {
            val foldedLine = fold(lines[index])
            val matchingLabel = labels.firstOrNull { foldedLine.contains(it) } ?: continue
            candidateAfterLabel(lines[index], foldedLine, matchingLabel, cpf)?.let { return it }

            val end = (index + 3).coerceAtMost(lines.lastIndex)
            for (nextIndex in (index + 1)..end) {
                val next = lines[nextIndex]
                if (isOnlyStructuralLabel(next)) continue
                rgCandidateFrom(next, cpf)?.let { return it }
            }
        }

        if (type == BrazilianDocumentType.CNH || type == BrazilianDocumentType.IDENTITY_CARD) {
            return fallbackRgCandidate(fullText, cpf)
        }

        return ""
    }

    private fun candidateAfterLabel(
        rawLine: String,
        foldedLine: String,
        label: String,
        cpf: String
    ): String? {
        val start = foldedLine.indexOf(label).takeIf { it >= 0 } ?: return null
        val afterIndex = (start + label.length).coerceAtMost(rawLine.length)
        val after = rawLine.drop(afterIndex).trim(' ', ':', '-', '/', '|')
        return rgCandidateFrom(after, cpf)
    }

    private fun rgCandidateFrom(value: String, cpf: String): String? {
        val withoutCpf = removeCpfCandidates(value, cpf)
        val issuer = """(?:SSP|SSPDS|SSPC|SESP|SDS|IFP|PC|DGPC|DETRAN|DIC|IIRGD|II|SECC|MAER|MEX|MD|PM|CBM)"""
        val patterns = listOf(
            Regex("""\b\d{1,2}\.?\d{3}\.?\d{3}-?[0-9Xx]\b(?:\s*$issuer[-/ ]?[A-Za-z]{0,2})?"""),
            Regex("""\b\d{5,10}-?[0-9Xx]?\b(?:\s*$issuer[-/ ]?[A-Za-z]{0,2})?""")
        )

        for (pattern in patterns) {
            val match = pattern.find(withoutCpf) ?: continue
            val candidate = normalizeRg(match.value)
            val digits = candidate.filter { it.isDigit() }
            if (digits.length in 5..10 && !looksLikeDate(candidate)) return candidate
        }
        return null
    }

    private fun fallbackRgCandidate(fullText: String, cpf: String): String {
        val withoutCpf = removeCpfCandidates(fullText, cpf)
            .replace(Regex("""\b\d{1,2}[/-]\d{1,2}[/-]\d{2,4}\b"""), " ")
            .replace(Regex("""\b\d{11}\b"""), " ")

        return rgCandidateFrom(withoutCpf, cpf) ?: ""
    }

    private fun extractCnhNumber(lines: List<String>, fullText: String, cpf: String): String {
        val labels = listOf("N REGISTRO", "NO REGISTRO", "NUMERO DO REGISTRO", "REGISTRO")
        for (index in lines.indices) {
            val foldedLine = fold(lines[index])
            val label = labels.firstOrNull { foldedLine.contains(it) } ?: continue
            val start = foldedLine.indexOf(label)
            val after = lines[index].drop((start + label.length).coerceAtMost(lines[index].length))
            cnhCandidateFrom(after, cpf)?.let { return it }

            if (index + 1 <= lines.lastIndex) {
                cnhCandidateFrom(lines[index + 1], cpf)?.let { return it }
            }
        }

        return cnhCandidateFrom(fullText, cpf) ?: ""
    }

    private fun cnhCandidateFrom(value: String, cpf: String): String? {
        val cpfDigits = CpfFormatter.digitsOnly(cpf)
        Regex("""(?<![0-9A-Z])([0-9OQDISBLZT]{9,11})(?![0-9A-Z])""", RegexOption.IGNORE_CASE)
            .findAll(value)
            .forEach { match ->
            val digits = toDigits(match.groupValues[1])
            if (digits != cpfDigits) return digits
        }
        return null
    }

    private fun extractPassportNumber(lines: List<String>): String {
        val passportPattern = Regex("""\b[A-Z]{2}\d{6}\b""", RegexOption.IGNORE_CASE)
        for (line in lines) {
            val match = passportPattern.find(line) ?: continue
            return match.value.uppercase(localePtBr)
        }
        return ""
    }

    private fun extractName(lines: List<String>, type: BrazilianDocumentType): String {
        val labels = when (type) {
            BrazilianDocumentType.CPF -> listOf("NOME", "NOME DO TITULAR", "NAME")
            else -> listOf("NOME CIVIL", "NOME SOCIAL", "NOME E SOBRENOME", "NOME DO TITULAR", "NOME", "NAME")
        }

        for (index in lines.indices) {
            val foldedLine = fold(lines[index])
            val label = labels.firstOrNull { foldedLine.contains(it) } ?: continue
            candidateNameAfterLabel(lines[index], foldedLine, label)?.let { return it }

            val end = (index + 4).coerceAtMost(lines.lastIndex)
            for (nextIndex in (index + 1)..end) {
                val candidate = cleanupName(lines[nextIndex])
                if (looksLikeName(candidate)) return candidate
            }
        }

        return lines
            .asSequence()
            .map { cleanupName(it) }
            .filter { looksLikeName(it) }
            .maxByOrNull { nameScore(it) }
            ?: ""
    }

    private fun candidateNameAfterLabel(rawLine: String, foldedLine: String, label: String): String? {
        val start = foldedLine.indexOf(label).takeIf { it >= 0 } ?: return null
        val afterIndex = (start + label.length).coerceAtMost(rawLine.length)
        val candidate = cleanupName(rawLine.drop(afterIndex).trim(' ', ':', '-', '/', '|'))
        return candidate.takeIf { looksLikeName(it) }
    }

    private fun cleanupName(value: String): String {
        return value
            .replace(Regex("""(?i)\b(nome|name|nome civil|nome social|nome do titular)\b"""), " ")
            .replace(Regex("""\s+"""), " ")
            .trim(' ', ':', '-', '/', '|')
    }

    private fun looksLikeName(value: String): Boolean {
        val folded = fold(value)
        if (value.length !in 6..80) return false
        if (value.any { it.isDigit() }) return false
        if (folded.any { it in ".:/\\|" }) return false
        if (nameStopWords.any { folded.contains(it) }) return false

        val words = folded.split(Regex("""\s+""")).filter { it.isNotBlank() }
        if (words.size < 2) return false
        return words.all { word ->
            word.length >= 2 || word in setOf("A", "E")
        }
    }

    private fun nameScore(value: String): Int {
        val folded = fold(value)
        val words = folded.split(Regex("""\s+""")).filter { it.isNotBlank() }
        var score = words.size * 10
        if (words.any { it in setOf("DA", "DE", "DO", "DAS", "DOS") }) score += 5
        if (folded == value.uppercase(localePtBr)) score += 2
        return score
    }

    private fun isOnlyStructuralLabel(value: String): Boolean {
        val folded = fold(value)
        return structuralLabels.any { folded == it || folded.contains(it) } && !Regex("""\d""").containsMatchIn(value)
    }

    private fun removeCpfCandidates(value: String, cpf: String): String {
        val cpfDigits = CpfFormatter.digitsOnly(cpf)
        return value
            .replace(Regex("""\d{3}[.\s]?\d{3}[.\s]?\d{3}[-\s]?\d{2}"""), " ")
            .replace(cpfDigits, " ")
    }

    private fun looksLikeDate(value: String): Boolean {
        return Regex("""^\d{1,2}[/-]\d{1,2}[/-]\d{2,4}$""").matches(value)
    }

    private fun toDigits(value: String): String {
        return value.uppercase(localePtBr).mapNotNull { char ->
            when (char) {
                in '0'..'9' -> char
                'O', 'Q', 'D' -> '0'
                'I', 'L', 'T' -> '1'
                'S' -> '5'
                'B' -> '8'
                'Z' -> '2'
                else -> null
            }
        }.joinToString("")
    }

    private fun fold(value: String): String {
        val normalized = Normalizer.normalize(value, Normalizer.Form.NFD)
        return normalized
            .replace(Regex("""\p{Mn}+"""), "")
            .uppercase(localePtBr)
            .replace(Regex("""[^\w\s./-]"""), " ")
            .replace(Regex("""\s+"""), " ")
            .trim()
    }

    private val structuralLabels = listOf(
        "ORG EMISSOR",
        "ORGAO EMISSOR",
        "UF",
        "DATA NASCIMENTO",
        "DATA DE NASCIMENTO",
        "FILIACAO",
        "NATURALIDADE",
        "VALIDADE",
        "EMISSAO",
        "LOCAL",
        "ASSINATURA"
    )

    private val nameStopWords = listOf(
        "REPUBLICA",
        "FEDERATIVA",
        "BRASIL",
        "GOVERNO",
        "MINISTERIO",
        "SECRETARIA",
        "DEPARTAMENTO",
        "DETRAN",
        "INSTITUTO",
        "IDENTIFICACAO",
        "CARTEIRA",
        "IDENTIDADE",
        "HABILITACAO",
        "PASSAPORTE",
        "CADASTRO",
        "PESSOAS FISICAS",
        "RECEITA FEDERAL",
        "REGISTRO",
        "VALIDA",
        "VALIDADE",
        "NASCIMENTO",
        "FILIACAO",
        "NATURALIDADE",
        "EMISSOR",
        "ASSINATURA",
        "PERMISSAO",
        "CATEGORIA",
        "DOCUMENTO"
    )
}
