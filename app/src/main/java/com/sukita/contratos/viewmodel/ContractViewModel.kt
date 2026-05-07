package com.sukita.contratos.viewmodel

import android.app.Application
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.sukita.contratos.ContratosApp
import com.sukita.contratos.data.ApartmentRepository
import com.sukita.contratos.model.Apartment
import com.sukita.contratos.model.ContractData
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
    private val _cpf           = MutableStateFlow("")
    private val _rg            = MutableStateFlow("")
    private val _rentInput     = MutableStateFlow("")   // Ex: "650,00"
    private val _termMonths    = MutableStateFlow("")   // Ex: "12"
    private val _startDate     = MutableStateFlow("")   // DD/MM/YYYY
    private val _paymentDay    = MutableStateFlow("")   // 1-31
    private val _signatureDate = MutableStateFlow("")   // DD/MM/YYYY

    val tenantName:    StateFlow<String> = _tenantName.asStateFlow()
    val cpf:           StateFlow<String> = _cpf.asStateFlow()
    val rg:            StateFlow<String> = _rg.asStateFlow()
    val rentInput:     StateFlow<String> = _rentInput.asStateFlow()
    val termMonths:    StateFlow<String> = _termMonths.asStateFlow()
    val startDate:     StateFlow<String> = _startDate.asStateFlow()
    val paymentDay:    StateFlow<String> = _paymentDay.asStateFlow()
    val signatureDate: StateFlow<String> = _signatureDate.asStateFlow()

    // Calculado automaticamente
    val endDate: StateFlow<String>
        get() = MutableStateFlow(
            if (_startDate.value.length == 10 && (_termMonths.value.toIntOrNull() ?: 0) > 0)
                DateCalculator.calcEndDate(_startDate.value, _termMonths.value.toInt()) ?: ""
            else ""
        ).asStateFlow()

    fun setTenantName(v: String)    { _tenantName.value    = v }
    fun setCpf(v: String)           { _cpf.value           = CpfFormatter.format(v) }
    fun setRg(v: String)            { _rg.value            = v }
    fun setRentInput(v: String)     { _rentInput.value     = v }
    fun setTermMonths(v: String)    { _termMonths.value    = v.filter { it.isDigit() } }
    fun setStartDate(v: String)     { _startDate.value     = DateCalculator.applyMask(v) }
    fun setPaymentDay(v: String)    { _paymentDay.value    = v.filter { it.isDigit() }.take(2) }
    fun setSignatureDate(v: String) { _signatureDate.value = DateCalculator.applyMask(v) }

    /** Preenche nome, CPF e RG a partir da leitura OCR de um documento */
    fun fillFromOcr(name: String?, cpf: String?, rg: String?) {
        if (!name.isNullOrBlank()) _tenantName.value = NameFormatter.capitalize(name)
        if (!cpf.isNullOrBlank())  _cpf.value        = CpfFormatter.format(cpf)
        if (!rg.isNullOrBlank())   _rg.value         = rg.trim()
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

    fun validate(): FormErrors {
        val months = _termMonths.value.toIntOrNull()
        val day    = _paymentDay.value.toIntOrNull()
        return FormErrors(
            tenantName    = if (_tenantName.value.isBlank()) "Nome obrigatório" else null,
            cpf           = when {
                _cpf.value.isBlank()              -> "CPF obrigatório"
                !CpfFormatter.isValid(_cpf.value) -> "CPF inválido"
                else                              -> null
            },
            rg            = if (_rg.value.isBlank()) "RG obrigatório" else null,
            rentValue     = if (CurrencyFormatter.parseToCents(_rentInput.value) == null) "Valor inválido" else null,
            termMonths    = when {
                months == null  -> "Prazo obrigatório"
                months <= 0     -> "Prazo deve ser maior que zero"
                else            -> null
            },
            startDate     = DateCalculator.validate(_startDate.value),
            paymentDay    = when {
                day == null      -> "Dia obrigatório"
                day !in 1..31    -> "Dia deve ser entre 1 e 31"
                else             -> null
            },
            signatureDate = DateCalculator.validate(_signatureDate.value)
        )
    }

    // ─── Monta o ContractData final ───────────────────────────────────────────
    fun buildContractData(): ContractData? {
        val apt   = _selectedApartment.value ?: return null
        val cents = CurrencyFormatter.parseToCents(_rentInput.value) ?: return null
        val months = _termMonths.value.toIntOrNull() ?: return null
        val endDt  = DateCalculator.calcEndDate(_startDate.value, months) ?: return null
        return ContractData(
            apartment      = apt,
            tenantName     = NameFormatter.capitalize(_tenantName.value),
            cpf            = _cpf.value,
            rg             = _rg.value,
            rentValueCents = cents,
            termMonths     = months,
            startDate      = _startDate.value,
            paymentDay     = _paymentDay.value.toInt(),
            signatureDate  = _signatureDate.value
        )
    }

    /** Calcula e expõe a data fim como string (para exibição em tempo real) */
    fun computedEndDate(): String {
        val months = _termMonths.value.toIntOrNull() ?: return ""
        if (_startDate.value.length < 10) return ""
        return DateCalculator.calcEndDate(_startDate.value, months) ?: ""
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
