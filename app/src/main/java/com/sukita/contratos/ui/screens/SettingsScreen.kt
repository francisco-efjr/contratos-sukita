package com.sukita.contratos.ui.screens

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.filled.Refresh
import androidx.compose.material.icons.filled.Visibility
import androidx.compose.material.icons.filled.VisibilityOff
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.input.KeyboardCapitalization
import androidx.compose.ui.text.input.PasswordVisualTransformation
import androidx.compose.ui.text.input.VisualTransformation
import androidx.compose.ui.unit.dp
import com.sukita.contratos.ocr.OpenAiOcrService

/**
 * Tela de configurações do app.
 *
 * Permite ao usuário:
 *  • Inserir / remover a chave de API OpenAI (armazenada em SharedPreferences).
 *  • Visualizar e editar o prompt de leitura que a IA usará.
 *    O prompt padrão fica em assets/ocr_prompt.txt.
 *    Qualquer edição aqui sobrescreve o padrão localmente.
 *  • Restaurar o prompt para o arquivo padrão.
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun SettingsScreen(onBack: () -> Unit) {
    val context = LocalContext.current

    // ── Estado da chave de API ────────────────────────────────────────────────
    var apiKey         by remember { mutableStateOf(OpenAiOcrService.getApiKey(context) ?: "") }
    var showKey        by remember { mutableStateOf(false) }
    var apiKeySaved    by remember { mutableStateOf(false) }

    // ── Estado do prompt ──────────────────────────────────────────────────────
    var promptText     by remember { mutableStateOf(OpenAiOcrService.getActivePrompt(context)) }
    var promptSaved    by remember { mutableStateOf(false) }
    var showResetDialog by remember { mutableStateOf(false) }

    // Diálogo de confirmação para resetar prompt
    if (showResetDialog) {
        AlertDialog(
            onDismissRequest = { showResetDialog = false },
            title   = { Text("Restaurar prompt padrão?") },
            text    = { Text("Suas alterações no prompt serão descartadas e o arquivo original (assets/ocr_prompt.txt) será usado novamente.") },
            confirmButton = {
                TextButton(onClick = {
                    OpenAiOcrService.resetPrompt(context)
                    promptText  = OpenAiOcrService.getActivePrompt(context)
                    promptSaved = false
                    showResetDialog = false
                }) { Text("Restaurar") }
            },
            dismissButton = {
                TextButton(onClick = { showResetDialog = false }) { Text("Cancelar") }
            }
        )
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("Configurações") },
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
                .verticalScroll(rememberScrollState()),
            verticalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            Spacer(modifier = Modifier.height(4.dp))

            // ── Seção: Chave de API ───────────────────────────────────────────
            SectionHeader("Chave de API OpenAI")

            Text(
                text  = "Necessária para usar a leitura por IA (GPT-4o Vision). " +
                        "Obtenha em platform.openai.com → API Keys.\n" +
                        "A chave fica salva apenas neste dispositivo.",
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.7f)
            )

            OutlinedTextField(
                value   = apiKey,
                onValueChange = { apiKey = it; apiKeySaved = false },
                label   = { Text("Chave OpenAI (sk-...)") },
                placeholder = { Text("sk-proj-...") },
                visualTransformation = if (showKey) VisualTransformation.None
                                       else PasswordVisualTransformation(),
                trailingIcon = {
                    IconButton(onClick = { showKey = !showKey }) {
                        Icon(
                            imageVector = if (showKey) Icons.Filled.VisibilityOff
                                          else Icons.Filled.Visibility,
                            contentDescription = if (showKey) "Ocultar" else "Mostrar"
                        )
                    }
                },
                modifier   = Modifier.fillMaxWidth(),
                singleLine = true,
                keyboardOptions = KeyboardOptions(
                    capitalization = KeyboardCapitalization.None
                )
            )

            Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                Button(
                    onClick = {
                        OpenAiOcrService.saveApiKey(context, apiKey)
                        apiKeySaved = true
                    },
                    modifier = Modifier.weight(1f)
                ) {
                    if (apiKeySaved) {
                        Icon(Icons.Filled.Check, contentDescription = null)
                        Spacer(Modifier.width(4.dp))
                        Text("Salvo!")
                    } else {
                        Text("Salvar chave")
                    }
                }

                if (apiKey.isNotBlank()) {
                    OutlinedButton(
                        onClick = {
                            OpenAiOcrService.clearApiKey(context)
                            apiKey      = ""
                            apiKeySaved = false
                        }
                    ) { Text("Remover") }
                }
            }

            HorizontalDivider()

            // ── Seção: Prompt de Leitura ──────────────────────────────────────
            SectionHeader("Prompt de Leitura (IA)")

            Text(
                text  = "Este texto é enviado ao ChatGPT junto com a foto do documento. " +
                        "Edite para ajustar o que a IA deve extrair.\n" +
                        "Arquivo padrão: assets/ocr_prompt.txt",
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.7f)
            )

            OutlinedTextField(
                value         = promptText,
                onValueChange = { promptText = it; promptSaved = false },
                label         = { Text("Prompt") },
                modifier      = Modifier
                    .fillMaxWidth()
                    .heightIn(min = 200.dp),
                maxLines      = 30,
                textStyle     = MaterialTheme.typography.bodySmall
            )

            Row(
                horizontalArrangement = Arrangement.spacedBy(8.dp),
                verticalAlignment     = Alignment.CenterVertically
            ) {
                Button(
                    onClick = {
                        OpenAiOcrService.saveCustomPrompt(context, promptText)
                        promptSaved = true
                    },
                    modifier = Modifier.weight(1f)
                ) {
                    if (promptSaved) {
                        Icon(Icons.Filled.Check, contentDescription = null)
                        Spacer(Modifier.width(4.dp))
                        Text("Salvo!")
                    } else {
                        Text("Salvar prompt")
                    }
                }

                IconButton(onClick = { showResetDialog = true }) {
                    Icon(
                        Icons.Filled.Refresh,
                        contentDescription = "Restaurar padrão",
                        tint = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.6f)
                    )
                }
            }

            Spacer(modifier = Modifier.height(16.dp))
        }
    }
}
