package com.sukita.contratos.ui.screens

import android.content.Context
import android.content.Intent
import android.os.Environment
import androidx.compose.foundation.layout.*
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.Share
import androidx.compose.material.icons.filled.SaveAlt
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.dp
import androidx.core.content.FileProvider
import java.io.File

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun PdfPreviewScreen(
    pdfPath: String,
    onNewContract: () -> Unit,
    onBack: () -> Unit
) {
    val context = LocalContext.current
    val pdfFile = remember(pdfPath) { File(pdfPath) }
    var saveMessage by remember { mutableStateOf<String?>(null) }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("PDF Gerado") },
                navigationIcon = {
                    IconButton(onClick = onBack) {
                        Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "Voltar")
                    }
                },
                actions = {
                    IconButton(onClick = { sharePdf(context, pdfFile) }) {
                        Icon(Icons.Filled.Share, contentDescription = "Compartilhar")
                    }
                }
            )
        }
    ) { padding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(padding)
                .padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            // Resumo do arquivo
            Card(modifier = Modifier.fillMaxWidth()) {
                Column(modifier = Modifier.padding(16.dp)) {
                    Text(
                        text = "Contrato gerado com sucesso!",
                        style = MaterialTheme.typography.titleMedium
                    )
                    Spacer(modifier = Modifier.height(8.dp))
                    Text(
                        text = "Arquivo: ${pdfFile.name}",
                        style = MaterialTheme.typography.bodySmall
                    )
                    Text(
                        text = "Tamanho: ${pdfFile.length() / 1024} KB",
                        style = MaterialTheme.typography.bodySmall
                    )
                }
            }

            saveMessage?.let {
                Text(
                    text = it,
                    style = MaterialTheme.typography.bodySmall,
                    color = if (it.startsWith("Erro"))
                        MaterialTheme.colorScheme.error
                    else
                        MaterialTheme.colorScheme.primary
                )
            }

            // Compartilhar
            Button(
                onClick = { sharePdf(context, pdfFile) },
                modifier = Modifier.fillMaxWidth()
            ) {
                Icon(Icons.Filled.Share, contentDescription = null)
                Spacer(modifier = Modifier.width(8.dp))
                Text("Compartilhar / WhatsApp / E-mail")
            }

            // Salvar em Downloads
            OutlinedButton(
                onClick = {
                    saveMessage = savePdfToDownloads(context, pdfFile)
                },
                modifier = Modifier.fillMaxWidth()
            ) {
                Icon(Icons.Filled.SaveAlt, contentDescription = null)
                Spacer(modifier = Modifier.width(8.dp))
                Text("Salvar em Downloads")
            }

            HorizontalDivider()

            // Novo contrato
            TextButton(
                onClick = onNewContract,
                modifier = Modifier.align(Alignment.CenterHorizontally)
            ) {
                Text("Novo contrato")
            }
        }
    }
}

private fun sharePdf(context: Context, file: File) {
    if (!file.exists()) return
    val uri = FileProvider.getUriForFile(
        context,
        "${context.packageName}.fileprovider",
        file
    )
    val intent = Intent(Intent.ACTION_SEND).apply {
        type = "application/pdf"
        putExtra(Intent.EXTRA_STREAM, uri)
        addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION)
    }
    context.startActivity(Intent.createChooser(intent, "Compartilhar contrato via..."))
}

private fun savePdfToDownloads(context: Context, file: File): String {
    return try {
        val downloads = Environment.getExternalStoragePublicDirectory(Environment.DIRECTORY_DOWNLOADS)
        val dest = File(downloads, file.name)
        file.copyTo(dest, overwrite = true)
        "Salvo em Downloads/${file.name}"
    } catch (e: Exception) {
        "Erro ao salvar: ${e.message}"
    }
}
