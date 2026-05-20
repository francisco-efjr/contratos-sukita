package com.sukita.contratos.viewmodel

import android.app.Application
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.sukita.contratos.ContratosApp
import com.sukita.contratos.data.ApartmentRepository
import com.sukita.contratos.model.Apartment
import com.sukita.contratos.model.ContractData
import com.sukita.contratos.ocr.BrazilianDocumentParser
import com.sukita.contratos.util.CpfFormatter
import com.sukita.contratos.util.CurrencyFormatter
import com.sukita.contratos.util.DateCalculator
import com.sukita.contratos.util.NameFormatter
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch

/**
 * ViewModel compartilhado entre todas as telas do fluxo de geração de contrato.
 *
 * Armazenamento de campos:
 *  - cpf           → dígitos brutos, máx. 11  (ex: "07844070293")
 *  - startDate     → dígitos brutos, máx. 8   (ex: "07052026")
 *  - signatureDate → dígitos brutos, máx. 8
 * As máscaras visuais (CpfVisualTransformation / DateVisualTransformation) são
 * aplicadas apenas na camada de UI; o ViewModel nunca armazena strings formatadas.
 */
class ContractViewModel(application: Application) : AndroidViewModel(application) {

    private val repository: ApartmentRepository by lazy {
        val db = (application as ContratosApp).database
        ApartmentRepository(db.apartmentDao())
    }

    // ─── Lista de apartamentos ────────────────────────────────────────────────
    private val _apartments = MutableStateFlow<List<Apartment>>(emptyList())
    val apartments: StateFlow<List<Apartment>> = _apartments.asStateFlow()

    init {
        viewModelScope.launch {
            repository.getAllApartments().collect { _apartments.value = it }
        }
    }

    // ─── Apartamento selecionado ──────────────────────────────────────────────
    private val _selectedApartment = MutableStateFlow<Apartment?>(null)
    val selectedApartment: StateFlow<Apartment?> = _selectedApartment.asStateFlow()

    fun selectApartment(apartment: Apartment) {
        _selectedApartment.value = apartment
    }

    // ─── Campos do formulário ─────────────────────────────────────────────────
    private val _tenantName    = MutableStateFlow("")
    private val _cpf           = MutableStateFlow("")   // dígitos brutos, máx. 11
    private val _rg            = MutableStateFlow("")
    private val _rentInput     = MutableStateFlow("")   // Ex: "650,00"
    private val _termMonths    = MutableStateFlow("")   // Ex: "12"
    private val _startDate     = MutableStateFlow("")   // dígitos brutos DD/MM/YYYY → "07052026"
    private val _paymentDay    = MutableStateFlow("")   // 1-31
    private val _signatureDate = MutableStateFlow("")   // dígitos brutos, máx. 8

    val tenantName:    StateFlow<String> = _tenantName.asStateFlow()
    val cpf:           StateFlow<String> = _cpf.asStateFlow()
    val rg:            StateFlow<String> = _rg.asStateFlow()
    val rentInput:     StateFlow<String> = _rentInput.asStateFlow()
    val termMonths:    StateFlow<String> = _termMonths.asStateFlow()
    val startDate:     StateFlow<String> = _startDate.asStateFlow()
    val paymentDay:    StateFlow<String> = _paymentDay.asStateFlow()
    val signatureDate: StateFlow<String> = _signatureDate.asStateFlow()

    // ─── Setters ──────────────────────────────────────────────────────────────

    fun setTenantName(v: String)    { _tenantName.value    = v }

    /** Armazena apenas dígitos do CPF (máx. 11) — sem máscara. */
    fun setCpf(v: String)           { _cpf.value           = v.filter { it.isDigit() }.take(11) }

    /** Mantem numero, digito verificador e orgao emissor do RG quando existirem. */
    fun setRg(v: String)            { _rg.value            = BrazilianDocumentParser.normalizeRg(v) }
    fun setRentInput(v: String)     { _rentInput.value     = v }
    fun setTermMonths(v: String)    { _termMonths.value    = v.filter { it.isDigit() } }

    /** Armazena apenas dígitos da data de início (máx. 8) — sem máscara. */
    fun setStartDate(v: String)     { _startDate.value     = v.filter { it.isDigit() }.take(8) }

    fun setPaymentDay(v: String)    { _paymentDay.value    = v.filter { it.isDigit() }.take(2) }

    /** Armazena apenas dígitos da data de assinatura (máx. 8) — sem máscara. */
    fun setSignatureDate(v: String) { _signatureDate.value = v.filter { it.isDigit() }.take(8) }

    /** Preenche nome, CPF e RG a partir da leitura OCR de um documento */
    fun fillFromOcr(name: String?, cpf: String?, rg: String?) {
        if (!name.isNullOrBlank()) _tenantName.value = NameFormatter.capitalize(name)
        // Armazena apenas dígitos brutos do CPF retornado pelo OCR
        if (!cpf.isNullOrBlank())  _cpf.value        = CpfFormatter.digitsOnly(cpf).take(11)
        if (!rg.isNullOrBlank())   _rg.value         = BrazilianDocumentParser.normalizeRg(rg)
    }

    // ─── Validação ────────────────────────────────────────────────────────────
    data class FormErrors(
        val tenantName: String?    = null,
        val cpf: String?           = null,
        val rg: String?            = null,
        val rentValue: String?     = null,
        val termMonths: String?    = null,
        val startDate: String?     = null,
        val paymentDay: String?    = null,
        val signatureDate: String? = null
    ) {
        val hasErrors: Boolean get() = listOf(
            tenantName, cpf, rg, rentValue, termMonths,
            startDate, paymentDay, signatureDate
        ).any { it != null }
    }

    /**
     * Valida apenas os campos da página 1 (formulário de dados do contrato).
     * NÃO valida signatureDate, que é preenchida na página 2.
     */
    fun validatePage1(): FormErrors {
        val months         = _termMonths.value.toIntOrNull()
        val day            = _paymentDay.value.toIntOrNull()
        val startFormatted = DateCalculator.applyMask(_startDate.value)
        return FormErrors(
            tenantName = if (_tenantName.value.isBlank()) "Nome obrigatório" else null,
            cpf        = when {
                _cpf.value.isBlank()              -> "CPF obrigatório"
                !CpfFormatter.isValid(_cpf.value) -> "CPF inválido"
                else                              -> null
            },
            rg        = if (_rg.value.isBlank()) "RG obrigatório" else null,
            rentValue = if (CurrencyFormatter.parseToCents(_rentInput.value) == null) "Valor inválido" else null,
            termMonths = when {
                months == null -> "Prazo obrigatório"
                months <= 0    -> "Prazo deve ser maior que zero"
                else           -> null
            },
            startDate  = DateCalculator.validate(startFormatted),
            paymentDay = when {
                day == null   -> "Dia obrigatório"
                day !in 1..31 -> "Dia deve ser entre 1 e 31"
                else          -> null
            },
            signatureDate = null   // página 1 não valida data de assinatura
        )
    }

    /** Valida todos os campos (página 1 + página 2). */
    fun validate(): FormErrors {
        val months         = _termMonths.value.toIntOrNull()
        val day            = _paymentDay.value.toIntOrNull()
        val startFormatted = DateCalculator.applyMask(_startDate.value)
        val sigFormatted   = DateCalculator.applyMask(_signatureDate.value)
        return FormErrors(
            tenantName = if (_tenantName.value.isBlank()) "Nome obrigatório" else null,
            cpf        = when {
                _cpf.value.isBlank()              -> "CPF obrigatório"
                !CpfFormatter.isValid(_cpf.value) -> "CPF inválido"
                else                              -> null
            },
            rg        = if (_rg.value.isBlank()) "RG obrigatório" else null,
            rentValue = if (CurrencyFormatter.parseToCents(_rentInput.value) == null) "Valor inválido" else null,
            termMonths = when {
                months == null -> "Prazo obrigatório"
                months <= 0    -> "Prazo deve ser maior que zero"
                else           -> null
            },
            startDate     = DateCalculator.validate(startFormatted),
            paymentDay    = when {
                day == null   -> "Dia obrigatório"
                day !in 1..31 -> "Dia deve ser entre 1 e 31"
                else          -> null
            },
            signatureDate = DateCalculator.validate(sigFormatted)
        )
    }

    // ─── Monta o ContractData final ───────────────────────────────────────────
    fun buildContractData(): ContractData? {
        val apt    = _selectedApartment.value ?: return null
        val cents  = CurrencyFormatter.parseToCents(_rentInput.value) ?: return null
        val months = _termMonths.value.toIntOrNull() ?: return null
        // Converte dígitos brutos para DD/MM/YYYY antes de usar
        val startFormatted = DateCalculator.applyMask(_startDate.value)
        val sigFormatted   = DateCalculator.applyMask(_signatureDate.value)
        val endDt  = DateCalculator.calcEndDate(startFormatted, months) ?: return null
        return ContractData(
            apartment      = apt,
            tenantName     = NameFormatter.capitalize(_tenantName.value),
            cpf            = CpfFormatter.format(_cpf.value),   // formata apenas para o PDF
            rg             = _rg.value,
            rentValueCents = cents,
            termMonths     = months,
            startDate      = startFormatted,
            paymentDay     = _paymentDay.value.toInt(),
            signatureDate  = sigFormatted
        )
    }

    /** Calcula e expõe a data fim como string (para exibição em tempo real) */
    fun computedEndDate(): String {
        val months = _termMonths.value.toIntOrNull() ?: return ""
        val startFormatted = DateCalculator.applyMask(_startDate.value)
        if (startFormatted.length < 10) return ""
        return DateCalculator.calcEndDate(startFormatted, months) ?: ""
    }

    fun resetForm() {
        _tenantName.value    = ""
        _cpf.value           = ""
        _rg.value            = ""
        _rentInput.value     = ""
        _termMonths.value    = ""
        _startDate.value     = ""
        _paymentDay.value    = ""
        _signatureDate.value = ""
        _selectedApartment.value = null
    }
}
