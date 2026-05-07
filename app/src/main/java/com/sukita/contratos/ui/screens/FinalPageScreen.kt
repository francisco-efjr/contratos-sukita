package com.sukita.contratos.ui.screens

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
import com.sukita.contratos.pdf.ContractPdfGenerator
import com.sukita.contratos.util.NameFormatter
import com.sukita.contratos.viewmodel.ContractViewModel
import kotlinx.coroutines.launch

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun FinalPageScreen(
    vm: ContractViewModel,
    onGenerate: (String) -> Unit,
    onBack: () -> Unit
) {
    val context        = LocalContext.current
    val scope          = rememberCoroutineScope()
    val tenantName     by vm.tenantName.collectAsState()
    val cpf            by vm.cpf.collectAsState()
    val signatureDate  by vm.signatureDate.collectAsState()

    var sigDateError   by remember { mutableStateOf<String?>(null) }
    var isGenerating   by remember { mutableStateOf(false) }
    var showError      by remember { mutableStateOf<String?>(null) }

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
                .padding(horizontal = 16.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            Spacer(modifier = Modifier.height(8.dp))

            SectionHeader("Assinatura")

            ContractTextField(
                label = "Data de assinatura",
                value = signatureDate,
                onValueChange = {
                    vm.setSignatureDate(it)
                    sigDateError = null
                },
                error = sigDateError,
                keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                placeholder = "DD/MM/AAAA"
            )

            Divider(modifier = Modifier.padding(vertical = 4.dp))

            SectionHeader("Confirmação do Locatário")

            Text("Nome (MAIÚSCULAS):", style = MaterialTheme.typography.labelMedium)
            Text(
                text = NameFormatter.toUpper(tenantName).ifEmpty { "—" },
                style = MaterialTheme.typography.bodyLarge
            )

            Spacer(modifier = Modifier.height(4.dp))

            Text("CPF:", style = MaterialTheme.typography.labelMedium)
            Text(
                text = cpf.ifEmpty { "—" },
                style = MaterialTheme.typography.bodyLarge
            )

            Spacer(modifier = Modifier.height(24.dp))

            showError?.let {
                Text(
                    text = it,
                    color = MaterialTheme.colorScheme.error,
                    style = MaterialTheme.typography.bodySmall
                )
            }

            Button(
                onClick = {
                    // Valida data de assinatura
                    val err = com.sukita.contratos.util.DateCalculator.validate(signatureDate)
                    if (err != null) {
                        sigDateError = err
                        return@Button
                    }
                    // Gera o PDF
                    isGenerating = true
                    showError = null
                    scope.launch {
                        val data = vm.buildContractData()
                        if (data == null) {
                            isGenerating = false
                            showError = "Erro: dados do contrato incompletos. Volte e revise."
                            return@launch
                        }
                        val file = ContractPdfGenerator.generate(context, data)
                        isGenerating = false
                        if (file != null) {
                            onGenerate(file.absolutePath)
                        } else {
                            showError = "Erro ao gerar PDF. Tente novamente."
                        }
                    }
                },
                enabled = !isGenerating,
                modifier = Modifier.fillMaxWidth()
            ) {
                if (isGenerating) {
                    CircularProgressIndicator(
                        modifier = Modifier.size(18.dp),
                        strokeWidth = 2.dp,
                        color = MaterialTheme.colorScheme.onPrimary
                    )
                    Spacer(modifier = Modifier.width(8.dp))
                    Text("Gerando PDF...")
                } else {
                    Text("Gerar PDF")
                }
            }
        }
    }
}
