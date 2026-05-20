package com.sukita.contratos.ui.screens

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.CalendarMonth
import androidx.compose.material.icons.filled.DocumentScanner
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.TextRange
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.input.KeyboardCapitalization
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.input.TextFieldValue
import androidx.compose.ui.text.input.VisualTransformation
import androidx.compose.ui.unit.dp
import com.sukita.contratos.ui.CpfVisualTransformation
import com.sukita.contratos.ui.DateVisualTransformation
import com.sukita.contratos.viewmodel.ContractViewModel
import java.util.Calendar
import java.util.TimeZone

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ContractFormScreen(
    vm: ContractViewModel,
    onScanDocument: () -> Unit,
    onNext: () -> Unit,
    onBack: () -> Unit
) {
    val apt          by vm.selectedApartment.collectAsState()
    val tenantName   by vm.tenantName.collectAsState()
    val cpf          by vm.cpf.collectAsState()
    val rg           by vm.rg.collectAsState()
    val rentInput    by vm.rentInput.collectAsState()
    val termMonths   by vm.termMonths.collectAsState()
    val startDate    by vm.startDate.collectAsState()   // dígitos brutos
    val paymentDay   by vm.paymentDay.collectAsState()

    var errors     by remember { mutableStateOf(ContractViewModel.FormErrors()) }
    var showErrors by remember { mutableStateOf(false) }

    val endDate = remember(startDate, termMonths) { vm.computedEndDate() }

    // TextFieldValue para CPF → cursor explícito (evita saltar para posição errada com VisualTransformation)
    var cpfTfv by remember {
        mutableStateOf(TextFieldValue(text = cpf, selection = TextRange(cpf.length)))
    }
    LaunchedEffect(cpf) {
        if (cpfTfv.text != cpf) {
            cpfTfv = TextFieldValue(text = cpf, selection = TextRange(cpf.length))
        }
    }

    // TextFieldValue para data de início → cursor no fim após DatePicker
    var startDateTfv by remember {
        mutableStateOf(TextFieldValue(text = startDate, selection = TextRange(startDate.length)))
    }
    LaunchedEffect(startDate) {
        if (startDateTfv.text != startDate) {
            startDateTfv = TextFieldValue(
                text      = startDate,
                selection = TextRange(startDate.length)
            )
        }
    }

    // DatePicker para data de início
    var showStartDatePicker by remember { mutableStateOf(false) }
    val startDatePickerState = rememberDatePickerState()

    if (showStartDatePicker) {
        DatePickerDialog(
            onDismissRequest = { showStartDatePicker = false },
            confirmButton = {
                TextButton(
                    onClick = {
                        startDatePickerState.selectedDateMillis?.let { millis ->
                            val cal = Calendar.getInstance(TimeZone.getTimeZone("UTC"))
                            cal.timeInMillis = millis
                            val raw = "%02d%02d%04d".format(
                                cal.get(Calendar.DAY_OF_MONTH),
                                cal.get(Calendar.MONTH) + 1,
                                cal.get(Calendar.YEAR)
                            )
                            vm.setStartDate(raw)
                            // LaunchedEffect(startDate) sincronizará o cursor para o fim
                        }
                        showStartDatePicker = false
                    }
                ) { Text("OK") }
            },
            dismissButton = {
                TextButton(onClick = { showStartDatePicker = false }) { Text("Cancelar") }
            }
        ) {
            DatePicker(state = startDatePickerState)
        }
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text(apt?.name ?: "Dados do Contrato") },
                navigationIcon = {
                    IconButton(onClick = onBack) {
                        Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "Voltar")
                    }
                },
                actions = {
                    IconButton(onClick = onScanDocument) {
                        Icon(Icons.Filled.DocumentScanner, contentDescription = "Ler Documento")
                    }
                }
            )
        }
    ) { padding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(padding)
                .padding(horizontal = 16.dp)
                .imePadding()
                .verticalScroll(rememberScrollState()),
            verticalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            Spacer(modifier = Modifier.height(8.dp))

            SectionHeader("Locatário")

            ContractTextField(
                label  = "Nome completo",
                value  = tenantName,
                onValueChange = vm::setTenantName,
                error  = if (showErrors) errors.tenantName else null,
                keyboardOptions = KeyboardOptions(
                    capitalization = KeyboardCapitalization.Words,
                    imeAction = ImeAction.Next
                )
            )

            // CPF com TextFieldValue para controle preciso do cursor
            OutlinedTextField(
                value = cpfTfv,
                onValueChange = { newTfv ->
                    val filtered      = newTfv.text.filter { it.isDigit() }.take(11)
                    val clampedCursor = newTfv.selection.end.coerceAtMost(filtered.length)
                    cpfTfv = TextFieldValue(text = filtered, selection = TextRange(clampedCursor))
                    vm.setCpf(filtered)
                },
                label   = { Text("CPF") },
                placeholder = {
                    Text("000.000.000-00",
                        color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.38f))
                },
                isError       = if (showErrors) errors.cpf != null else false,
                supportingText = { if (showErrors && errors.cpf != null) Text(errors.cpf!!) },
                keyboardOptions = KeyboardOptions(
                    keyboardType = KeyboardType.Number,
                    imeAction    = ImeAction.Next
                ),
                visualTransformation = CpfVisualTransformation(),
                modifier   = Modifier.fillMaxWidth(),
                singleLine = true
            )

            ContractTextField(
                label = "RG",
                value = rg,
                onValueChange = vm::setRg,
                error = if (showErrors) errors.rg else null,
                keyboardOptions = KeyboardOptions(
                    capitalization = KeyboardCapitalization.Characters,
                    keyboardType = KeyboardType.Text,
                    imeAction    = ImeAction.Next
                ),
                placeholder = "Ex: 12.345.678-9 SSP/AM"
            )

            HorizontalDivider(modifier = Modifier.padding(vertical = 4.dp))
            SectionHeader("Contrato")

            ContractTextField(
                label = "Valor do aluguel (R$)",
                value = rentInput,
                onValueChange = vm::setRentInput,
                error = if (showErrors) errors.rentValue else null,
                keyboardOptions = KeyboardOptions(
                    keyboardType = KeyboardType.Decimal,
                    imeAction = ImeAction.Next
                ),
                placeholder = "Ex: 650,00"
            )

            ContractTextField(
                label = "Prazo (meses)",
                value = termMonths,
                onValueChange = vm::setTermMonths,
                error = if (showErrors) errors.termMonths else null,
                keyboardOptions = KeyboardOptions(
                    keyboardType = KeyboardType.Number,
                    imeAction = ImeAction.Next
                ),
                placeholder = "Ex: 12"
            )

            // Campo de data com TextFieldValue (cursor no fim após DatePicker)
            OutlinedTextField(
                value = startDateTfv,
                onValueChange = { newTfv ->
                    // Filtra para dígitos, max 8 — mantém posição de cursor correta
                    val filteredText   = newTfv.text.filter { it.isDigit() }.take(8)
                    val clampedCursor  = newTfv.selection.end.coerceAtMost(filteredText.length)
                    val adjusted       = TextFieldValue(
                        text      = filteredText,
                        selection = TextRange(clampedCursor)
                    )
                    startDateTfv = adjusted
                    vm.setStartDate(filteredText)
                },
                label   = { Text("Data de início") },
                placeholder = {
                    Text("DD/MM/AAAA",
                        color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.38f))
                },
                isError = if (showErrors) errors.startDate != null else false,
                supportingText = { if (showErrors && errors.startDate != null) Text(errors.startDate!!) },
                keyboardOptions = KeyboardOptions(
                    keyboardType = KeyboardType.Number,
                    imeAction = ImeAction.Next
                ),
                visualTransformation = DateVisualTransformation(),
                trailingIcon = {
                    IconButton(onClick = { showStartDatePicker = true }) {
                        Icon(Icons.Filled.CalendarMonth, contentDescription = "Escolher data")
                    }
                },
                modifier  = Modifier.fillMaxWidth(),
                singleLine = true
            )

            if (endDate.isNotEmpty()) {
                Text(
                    text  = "Término calculado: $endDate",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.6f)
                )
            }

            ContractTextField(
                label = "Dia de pagamento",
                value = paymentDay,
                onValueChange = vm::setPaymentDay,
                error = if (showErrors) errors.paymentDay else null,
                keyboardOptions = KeyboardOptions(
                    keyboardType = KeyboardType.Number,
                    imeAction = ImeAction.Done
                ),
                placeholder = "Ex: 7"
            )

            Spacer(modifier = Modifier.height(8.dp))

            Button(
                onClick = {
                    errors = vm.validatePage1()
                    showErrors = true
                    if (!errors.hasErrors) onNext()
                },
                modifier = Modifier.fillMaxWidth()
            ) {
                Text("Próximo")
            }

            Spacer(modifier = Modifier.height(16.dp))
        }
    }
}

// ── Componentes compartilhados ────────────────────────────────────────────────

@Composable
fun SectionHeader(title: String) {
    Text(
        text  = title,
        style = MaterialTheme.typography.titleSmall,
        color = MaterialTheme.colorScheme.primary
    )
}

@Composable
fun ContractTextField(
    label: String,
    value: String,
    onValueChange: (String) -> Unit,
    error: String? = null,
    placeholder: String = "",
    keyboardOptions: KeyboardOptions = KeyboardOptions.Default,
    visualTransformation: VisualTransformation = VisualTransformation.None,
    trailingIcon: (@Composable () -> Unit)? = null,
    enabled: Boolean = true
) {
    OutlinedTextField(
        value                = value,
        onValueChange        = onValueChange,
        label                = { Text(label) },
        placeholder          = {
            Text(placeholder, color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.38f))
        },
        isError              = error != null,
        supportingText       = { if (error != null) Text(error) },
        keyboardOptions      = keyboardOptions,
        visualTransformation = visualTransformation,
        trailingIcon         = trailingIcon,
        modifier             = Modifier.fillMaxWidth(),
        singleLine           = true,
        enabled              = enabled
    )
}
