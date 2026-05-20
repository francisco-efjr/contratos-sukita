package com.sukita.contratos.ui.screens

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.CalendarMonth
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.TextRange
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.input.TextFieldValue
import androidx.compose.ui.unit.dp
import com.sukita.contratos.pdf.ContractPdfGenerator
import com.sukita.contratos.ui.DateVisualTransformation
import com.sukita.contratos.util.CpfFormatter
import com.sukita.contratos.util.DateCalculator
import com.sukita.contratos.util.NameFormatter
import com.sukita.contratos.viewmodel.ContractViewModel
import kotlinx.coroutines.launch
import java.util.Calendar
import java.util.TimeZone

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun FinalPageScreen(
    vm: ContractViewModel,
    onGenerate: (String) -> Unit,
    onBack: () -> Unit
) {
    val context       = LocalContext.current
    val scope         = rememberCoroutineScope()
    val tenantName    by vm.tenantName.collectAsState()
    val cpf           by vm.cpf.collectAsState()
    val signatureDate by vm.signatureDate.collectAsState()   // dígitos brutos

    var sigDateError by remember { mutableStateOf<String?>(null) }
    var isGenerating by remember { mutableStateOf(false) }
    var showError    by remember { mutableStateOf<String?>(null) }

    // Fix 4: TextFieldValue para data de assinatura → cursor no fim após DatePicker
    var sigDateTfv by remember {
        mutableStateOf(TextFieldValue(text = signatureDate, selection = TextRange(signatureDate.length)))
    }
    LaunchedEffect(signatureDate) {
        if (sigDateTfv.text != signatureDate) {
            sigDateTfv = TextFieldValue(
                text      = signatureDate,
                selection = TextRange(signatureDate.length)
            )
        }
    }

    var showSigDatePicker  by remember { mutableStateOf(false) }
    val sigDatePickerState = rememberDatePickerState()

    if (showSigDatePicker) {
        DatePickerDialog(
            onDismissRequest = { showSigDatePicker = false },
            confirmButton = {
                TextButton(
                    onClick = {
                        sigDatePickerState.selectedDateMillis?.let { millis ->
                            val cal = Calendar.getInstance(TimeZone.getTimeZone("UTC"))
                            cal.timeInMillis = millis
                            val raw = "%02d%02d%04d".format(
                                cal.get(Calendar.DAY_OF_MONTH),
                                cal.get(Calendar.MONTH) + 1,
                                cal.get(Calendar.YEAR)
                            )
                            vm.setSignatureDate(raw)
                            sigDateError = null
                        }
                        showSigDatePicker = false
                    }
                ) { Text("OK") }
            },
            dismissButton = {
                TextButton(onClick = { showSigDatePicker = false }) { Text("Cancelar") }
            }
        ) {
            DatePicker(state = sigDatePickerState)
        }
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("Página Final") },
                navigationIcon = {
                    IconButton(onClick = onBack) {
                        Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "Voltar")
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

            SectionHeader("Assinatura")

            OutlinedTextField(
                value = sigDateTfv,
                onValueChange = { newTfv ->
                    val filteredText  = newTfv.text.filter { it.isDigit() }.take(8)
                    val clampedCursor = newTfv.selection.end.coerceAtMost(filteredText.length)
                    val adjusted      = TextFieldValue(
                        text      = filteredText,
                        selection = TextRange(clampedCursor)
                    )
                    sigDateTfv   = adjusted
                    sigDateError = null
                    vm.setSignatureDate(filteredText)
                },
                label       = { Text("Data de assinatura") },
                placeholder = {
                    Text("DD/MM/AAAA",
                        color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.38f))
                },
                isError       = sigDateError != null,
                supportingText = { if (sigDateError != null) Text(sigDateError!!) },
                keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                visualTransformation = DateVisualTransformation(),
                trailingIcon = {
                    IconButton(onClick = { showSigDatePicker = true }) {
                        Icon(Icons.Filled.CalendarMonth, contentDescription = "Escolher data")
                    }
                },
                modifier   = Modifier.fillMaxWidth(),
                singleLine = true
            )

            HorizontalDivider(modifier = Modifier.padding(vertical = 4.dp))

            SectionHeader("Confirmação do Locatário")

            Text("Nome (MAIÚSCULAS):", style = MaterialTheme.typography.labelMedium)
            Text(
                text  = NameFormatter.toUpper(tenantName).ifEmpty { "—" },
                style = MaterialTheme.typography.bodyLarge
            )

            Spacer(modifier = Modifier.height(4.dp))

            Text("CPF:", style = MaterialTheme.typography.labelMedium)
            Text(
                text  = CpfFormatter.format(cpf).ifEmpty { "—" },
                style = MaterialTheme.typography.bodyLarge
            )

            Spacer(modifier = Modifier.height(24.dp))

            showError?.let {
                Text(
                    text  = it,
                    color = MaterialTheme.colorScheme.error,
                    style = MaterialTheme.typography.bodySmall
                )
            }

            Button(
                onClick = {
                    // Valida data de assinatura (converte dígitos brutos → DD/MM/YYYY)
                    val err = DateCalculator.validate(DateCalculator.applyMask(signatureDate))
                    if (err != null) {
                        sigDateError = err
                        return@Button
                    }

                    isGenerating = true
                    showError    = null
                    scope.launch {
                        val data = vm.buildContractData()
                        if (data == null) {
                            isGenerating = false
                            showError = "Erro: dados do contrato incompletos. Volte e revise."
                            return@launch
                        }
                        var errorDetail = ""
                        val file = ContractPdfGenerator.generate(context, data) { errorDetail = it }
                        isGenerating = false
                        if (file != null && file.exists() && file.length() > 0) {
                            onGenerate(file.absolutePath)
                        } else {
                            showError = "Falha ao gerar PDF: $errorDetail"
                        }
                    }
                },
                enabled  = !isGenerating,
                modifier = Modifier.fillMaxWidth()
            ) {
                if (isGenerating) {
                    CircularProgressIndicator(
                        modifier    = Modifier.size(18.dp),
                        strokeWidth = 2.dp,
                        color       = MaterialTheme.colorScheme.onPrimary
                    )
                    Spacer(modifier = Modifier.width(8.dp))
                    Text("Gerando PDF…")
                } else {
                    Text("Gerar PDF")
                }
            }

            Spacer(modifier = Modifier.height(16.dp))
        }
    }
}
