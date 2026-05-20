package com.sukita.contratos

import com.sukita.contratos.ocr.BrazilianDocumentParser
import com.sukita.contratos.ocr.BrazilianDocumentType
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

class BrazilianDocumentParserTest {

    @Test
    fun `detecta CNH e extrai nome CPF RG e registro`() {
        val lines = listOf(
            "REPUBLICA FEDERATIVA DO BRASIL",
            "CARTEIRA NACIONAL DE HABILITACAO",
            "NOME",
            "KARINA DA COSTA MENDONCA",
            "DOC. IDENTIDADE / ORG EMISSOR / UF",
            "403380-9 SSP AM",
            "CPF",
            "078.440.702-93",
            "N REGISTRO",
            "01234567890",
            "VALIDADE",
            "07/05/2030"
        )

        val result = BrazilianDocumentParser.parse(lines.joinToString("\n"), lines)

        assertEquals(BrazilianDocumentType.CNH, result.type)
        assertEquals("KARINA DA COSTA MENDONCA", result.name)
        assertEquals("078.440.702-93", result.cpf)
        assertEquals("403380-9 SSP AM", result.rg)
        assertEquals("01234567890", result.documentNumber)
    }

    @Test
    fun `detecta carteira de identidade e nao usa cabecalho como nome`() {
        val lines = listOf(
            "REPUBLICA FEDERATIVA DO BRASIL",
            "ESTADO DO AMAZONAS",
            "SECRETARIA DE SEGURANCA PUBLICA",
            "CARTEIRA DE IDENTIDADE",
            "REGISTRO GERAL 12.345.678-9 SSP/AM",
            "NOME",
            "JOAO DA SILVA",
            "CPF 529.982.247-25",
            "DATA DE NASCIMENTO 01/02/1990"
        )

        val result = BrazilianDocumentParser.parse(lines.joinToString("\n"), lines)

        assertEquals(BrazilianDocumentType.IDENTITY_CARD, result.type)
        assertEquals("JOAO DA SILVA", result.name)
        assertEquals("529.982.247-25", result.cpf)
        assertEquals("12.345.678-9 SSP/AM", result.rg)
    }

    @Test
    fun `detecta CIN usando CPF como numero principal`() {
        val lines = listOf(
            "CARTEIRA DE IDENTIDADE NACIONAL",
            "REPUBLICA FEDERATIVA DO BRASIL",
            "NOME CIVIL",
            "MARIA DE SOUZA",
            "CPF",
            "529.982.247-25",
            "NACIONALIDADE BRASILEIRA"
        )

        val result = BrazilianDocumentParser.parse(lines.joinToString("\n"), lines)

        assertEquals(BrazilianDocumentType.NATIONAL_ID_CARD, result.type)
        assertEquals("MARIA DE SOUZA", result.name)
        assertEquals("529.982.247-25", result.cpf)
        assertEquals("", result.rg)
        assertEquals("52998224725", result.documentNumber)
    }

    @Test
    fun `detecta comprovante de CPF e extrai nome do titular`() {
        val lines = listOf(
            "MINISTERIO DA FAZENDA",
            "RECEITA FEDERAL",
            "COMPROVANTE DE INSCRICAO NO CPF",
            "Nome: ANA CLARA PEREIRA",
            "CPF: 07844070293",
            "Codigo de controle"
        )

        val result = BrazilianDocumentParser.parse(lines.joinToString("\n"), lines)

        assertEquals(BrazilianDocumentType.CPF, result.type)
        assertEquals("ANA CLARA PEREIRA", result.name)
        assertEquals("078.440.702-93", result.cpf)
        assertEquals("", result.rg)
        assertEquals("07844070293", result.documentNumber)
        assertTrue(result.hasAnyData)
    }
}
