package com.sukita.contratos

import com.sukita.contratos.util.*
import org.junit.Assert.*
import org.junit.Test

class CpfFormatterTest {

    @Test fun `formats 11 digit CPF correctly`() {
        assertEquals("078.440.702-93", CpfFormatter.format("07844070293"))
    }

    @Test fun `formats partial CPF without crashing`() {
        assertEquals("078.440", CpfFormatter.format("078440"))
    }

    @Test fun `validates correct CPF`() {
        assertTrue(CpfFormatter.isValid("078.440.702-93"))
        assertTrue(CpfFormatter.isValid("07844070293"))
    }

    @Test fun `rejects all-same-digit CPF`() {
        assertFalse(CpfFormatter.isValid("111.111.111-11"))
    }

    @Test fun `rejects CPF with wrong check digits`() {
        assertFalse(CpfFormatter.isValid("078.440.702-00"))
    }

    @Test fun `rejects CPF with wrong length`() {
        assertFalse(CpfFormatter.isValid("0784407029"))
    }
}

class NumberToWordsTest {

    @Test fun `converts R$650,00 correctly`() {
        assertEquals("SEISCENTOS E CINQUENTA REAIS", NumberToWords.fromCents(65000L))
    }

    @Test fun `converts R$1500,00 correctly`() {
        assertEquals("MIL E QUINHENTOS REAIS", NumberToWords.fromCents(150000L))
    }

    @Test fun `converts R$1,00 correctly`() {
        assertEquals("UM REAL", NumberToWords.fromCents(100L))
    }

    @Test fun `converts R$0,50 correctly`() {
        assertEquals("CINQUENTA CENTAVOS", NumberToWords.fromCents(50L))
    }

    @Test fun `converts R$1500,50 correctly`() {
        assertEquals("MIL E QUINHENTOS REAIS E CINQUENTA CENTAVOS", NumberToWords.fromCents(150050L))
    }

    @Test fun `converts 12 months`() {
        assertEquals("DOZE MESES", NumberToWords.monthsToWords(12))
    }

    @Test fun `converts 1 month`() {
        assertEquals("UM MÊS", NumberToWords.monthsToWords(1))
    }
}

class DateCalculatorTest {

    @Test fun `adds 12 months correctly`() {
        assertEquals("07/05/2027", DateCalculator.calcEndDate("07/05/2026", 12))
    }

    @Test fun `adds 6 months correctly`() {
        assertEquals("07/11/2026", DateCalculator.calcEndDate("07/05/2026", 6))
    }

    @Test fun `crossing year boundary`() {
        assertEquals("07/01/2027", DateCalculator.calcEndDate("07/07/2026", 6))
    }

    @Test fun `returns null for invalid date`() {
        assertNull(DateCalculator.calcEndDate("99/99/9999", 12))
    }

    @Test fun `toWrittenDate formats correctly`() {
        assertEquals("07 de Maio de 2026", DateCalculator.toWrittenDate("07/05/2026"))
    }

    @Test fun `applyMask inserts slashes`() {
        assertEquals("07/05/2026", DateCalculator.applyMask("07052026"))
        assertEquals("07/0", DateCalculator.applyMask("0705"))
    }
}

class CurrencyFormatterTest {

    @Test fun `formats 65000 cents as 650,00`() {
        assertEquals("650,00", CurrencyFormatter.format(65000L))
    }

    @Test fun `formats 150000 cents as 1.500,00`() {
        assertEquals("1.500,00", CurrencyFormatter.format(150000L))
    }

    @Test fun `parses 650,00 to 65000`() {
        assertEquals(65000L, CurrencyFormatter.parseToCents("650,00"))
    }

    @Test fun `parses 1500.00 dot decimal to 150000`() {
        assertEquals(150000L, CurrencyFormatter.parseToCents("1500.00"))
    }

    @Test fun `parses R$ prefixed value`() {
        assertEquals(65000L, CurrencyFormatter.parseToCents("R$ 650,00"))
    }

    @Test fun `returns null for invalid input`() {
        assertNull(CurrencyFormatter.parseToCents("abc"))
        assertNull(CurrencyFormatter.parseToCents(""))
    }
}

class NameFormatterTest {

    @Test fun `capitalizes name correctly`() {
        assertEquals("Karina da Costa Mendonca", NameFormatter.capitalize("KARINA DA COSTA MENDONCA"))
    }

    @Test fun `uppercases name for signature`() {
        assertEquals("KARINA DA COSTA MENDONCA", NameFormatter.toUpper("Karina da Costa Mendonca"))
    }

    @Test fun `handles single word`() {
        assertEquals("Karina", NameFormatter.capitalize("karina"))
    }
}
