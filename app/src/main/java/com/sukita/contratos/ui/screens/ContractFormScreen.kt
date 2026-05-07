package com.sukita.contratos.ui.screens

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.DocumentScanner
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.input.KeyboardCapitalization
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
import com.sukita.contratos.viewmodel.ContractViewModel

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
    val startDate    by vm.startDate.collectAsState()
    val paymentDay   by vm.paymentDay.collectAsState()

    var errors by remember { mutableStateOf(ContractViewModel.FormErrors()) }
    var showErrors by remember { mutableStateOf(false) }

    val endDate = remember(startDate, termMonths) { vm.computedEndDate() }

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

            ContractTextField(
                label = "CPF",
                value = cpf,
                onValueChange = vm::setCpf,
                error = if (showErrors) errors.cpf else null,
                keyboardOptions = KeyboardOptions(
                    keyboardType = KeyboardType.Number,
                    imeAction = ImeAction.Next
                ),
                placeholder = "000.000.000-00"
            )

            ContractTextField(
                label = "RG",
                value = rg,
                onValueChange = vm::setRg,
                error = if (showErrors) errors.rg else null,
                keyboardOptions = KeyboardOptions(imeAction = ImeAction.Next),
                placeholder = "Ex: 403380-9 SSP-AM"
            )

            Divider(modifier = Modifier.padding(vertical = 4.dp))
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

            ContractTextField(
                label = "Data de início",
                value = startDate,
                onValueChange = vm::setStartDate,
                error = if (showErrors) errors.startDate else null,
                keyboardOptions = KeyboardOptions(
                    keyboardType = KeyboardType.Number,
                    imeAction = ImeAction.Next
                ),
                placeholder = "DD/MM/AAAA"
            )

            if (endDate.isNotEmpty()) {
                Text(
                    text = "Término calculado: $endDate",
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
                    errors = vm.validate()
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

@Composable
fun SectionHeader(title: String) {
    Text(
        text = title,
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
    enabled: Boolean = true
) {
    OutlinedTextField(
        value = value,
        onValueChange = onValueChange,
        label = { Text(label) },
        placeholder = { Text(placeholder, color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.38f)) },
        isError = error != null,
        supportingText = { if (error != null) Text(error) },
        keyboardOptions = keyboardOptions,
        modifier = Modifier.fillMaxWidth(),
        singleLine = true,
        enabled = enabled
    )
}
