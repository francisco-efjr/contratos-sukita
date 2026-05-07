package com.sukita.contratos.ui.screens

import android.Manifest
import android.content.pm.PackageManager
import android.net.Uri
import android.widget.Toast
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.camera.core.*
import androidx.camera.lifecycle.ProcessCameraProvider
import androidx.camera.view.PreviewView
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.CameraAlt
import androidx.compose.material.icons.filled.Image
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalLifecycleOwner
import androidx.compose.ui.text.input.KeyboardCapitalization
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
import androidx.compose.ui.viewinterop.AndroidView
import androidx.core.content.ContextCompat
import com.google.mlkit.vision.common.InputImage
import com.google.mlkit.vision.text.TextRecognition
import com.google.mlkit.vision.text.latin.TextRecognizerOptions
import com.sukita.contratos.viewmodel.ContractViewModel
import java.io.File
import java.util.concurrent.ExecutorService
import java.util.concurrent.Executors

/**
 * Tela de leitura de documento via câmera ou galeria.
 * Usa ML Kit Text Recognition (100% local, sem envio de dados).
 * Extrai nome, CPF e RG com regex e permite correção manual antes de confirmar.
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun DocumentScanScreen(
    vm: ContractViewModel,
    onDone: () -> Unit
) {
    val context       = LocalContext.current
    val lifecycleOwner = LocalLifecycleOwner.current

    // Campos extraídos (editáveis)
    var extractedName by remember { mutableStateOf("") }
    var extractedCpf  by remember { mutableStateOf("") }
    var extractedRg   by remember { mutableStateOf("") }
    var rawOcrText    by remember { mutableStateOf("") }
    var isProcessing  by remember { mutableStateOf(false) }
    var showCamera    by remember { mutableStateOf(false) }
    var hasCamPerm    by remember {
        mutableStateOf(
            ContextCompat.checkSelfPermission(context, Manifest.permission.CAMERA)
                == PackageManager.PERMISSION_GRANTED
        )
    }

    val cameraPermLauncher = rememberLauncherForActivityResult(
        ActivityResultContracts.RequestPermission()
    ) { granted -> hasCamPerm = granted }

    // Galeria: selecionar imagem
    val galleryLauncher = rememberLauncherForActivityResult(
        ActivityResultContracts.GetContent()
    ) { uri: Uri? ->
        uri ?: return@rememberLauncherForActivityResult
        isProcessing = true
        val image = InputImage.fromFilePath(context, uri)
        runOcr(image) { name, cpf, rg, raw ->
            extractedName = name
            extractedCpf  = cpf
            extractedRg   = rg
            rawOcrText    = raw
            isProcessing  = false
        }
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("Ler Documento") },
                navigationIcon = {
                    IconButton(onClick = onDone) {
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
            Spacer(modifier = Modifier.height(4.dp))

            Text(
                text = "Tire uma foto ou selecione uma imagem do documento de identidade (CNH, RG, passaporte).",
                style = MaterialTheme.typography.bodyMedium
            )

            Row(horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                // Botão câmera
                OutlinedButton(
                    onClick = {
                        if (hasCamPerm) showCamera = true
                        else cameraPermLauncher.launch(Manifest.permission.CAMERA)
                    },
                    modifier = Modifier.weight(1f)
                ) {
                    Icon(Icons.Filled.CameraAlt, contentDescription = null)
                    Spacer(Modifier.width(6.dp))
                    Text("Câmera")
                }
                // Botão galeria
                OutlinedButton(
                    onClick = { galleryLauncher.launch("image/*") },
                    modifier = Modifier.weight(1f)
                ) {
                    Icon(Icons.Filled.Image, contentDescription = null)
                    Spacer(Modifier.width(6.dp))
                    Text("Galeria")
                }
            }

            if (isProcessing) {
                Box(modifier = Modifier.fillMaxWidth(), contentAlignment = Alignment.Center) {
                    CircularProgressIndicator()
                }
            }

            if (showCamera && hasCamPerm) {
                CameraCapture(
                    onImageCaptured = { file ->
                        showCamera = false
                        isProcessing = true
                        val image = InputImage.fromFilePath(context, Uri.fromFile(file))
                        runOcr(image) { name, cpf, rg, raw ->
                            extractedName = name
                            extractedCpf  = cpf
                            extractedRg   = rg
                            rawOcrText    = raw
                            isProcessing  = false
                        }
                    },
                    onCancel = { showCamera = false }
                )
            }

            // Campos editáveis extraídos
            if (extractedName.isNotEmpty() || extractedCpf.isNotEmpty() || extractedRg.isNotEmpty()) {
                Divider()
                Text(
                    text = "Dados extraídos — revise antes de confirmar:",
                    style = MaterialTheme.typography.labelMedium
                )

                ContractTextField(
                    label = "Nome",
                    value = extractedName,
                    onValueChange = { extractedName = it },
                    keyboardOptions = KeyboardOptions(
                        capitalization = KeyboardCapitalization.Words
                    )
                )
                ContractTextField(
                    label = "CPF",
                    value = extractedCpf,
                    onValueChange = { extractedCpf = it },
                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                    placeholder = "000.000.000-00"
                )
                ContractTextField(
                    label = "RG",
                    value = extractedRg,
                    onValueChange = { extractedRg = it }
                )

                Button(
                    onClick = {
                        vm.fillFromOcr(
                            name = extractedName.takeIf { it.isNotBlank() },
                            cpf  = extractedCpf.takeIf { it.isNotBlank() },
                            rg   = extractedRg.takeIf { it.isNotBlank() }
                        )
                        Toast.makeText(context, "Dados preenchidos!", Toast.LENGTH_SHORT).show()
                        onDone()
                    },
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Text("Confirmar e usar estes dados")
                }
            }
        }
    }
}

// ── OCR com ML Kit ────────────────────────────────────────────────────────────

private fun runOcr(
    image: InputImage,
    onResult: (name: String, cpf: String, rg: String, raw: String) -> Unit
) {
    val recognizer = TextRecognition.getClient(TextRecognizerOptions.DEFAULT_OPTIONS)
    recognizer.process(image)
        .addOnSuccessListener { result ->
            val text = result.text
            onResult(
                extractName(text),
                extractCpf(text),
                extractRg(text),
                text
            )
        }
        .addOnFailureListener {
            onResult("", "", "", "Falha na leitura: ${it.message}")
        }
}

private fun extractCpf(text: String): String {
    val regex = Regex("""(\d{3})[.\s]?(\d{3})[.\s]?(\d{3})[-\s]?(\d{2})""")
    val match = regex.find(text) ?: return ""
    return "${match.groupValues[1]}.${match.groupValues[2]}.${match.groupValues[3]}-${match.groupValues[4]}"
}

private fun extractRg(text: String): String {
    // Tenta pegar número após "RG", "Identidade" ou "N°"
    val patterns = listOf(
        Regex("""(?:RG|R\.G\.|Identidade|Nº|N°)[:\s]*([0-9A-Z][-0-9A-Z]{4,})\s*(SSP-?\w{2})?""", RegexOption.IGNORE_CASE),
        Regex("""\b(\d{6,9}-?\d)\b""")
    )
    for (p in patterns) {
        val m = p.find(text) ?: continue
        val num = m.groupValues[1].trim()
        val org = m.groupValues.getOrNull(2)?.trim() ?: ""
        return if (org.isNotEmpty()) "$num $org" else num
    }
    return ""
}

private fun extractName(text: String): String {
    // Procura por linha após "Nome" ou primeira linha com mais de 2 palavras sem números
    val lines = text.lines().map { it.trim() }.filter { it.isNotBlank() }
    val nameAfterLabel = Regex("""(?:Nome|Name)[:\s]+(.+)""", RegexOption.IGNORE_CASE)
    for (line in lines) {
        val m = nameAfterLabel.find(line)
        if (m != null) return m.groupValues[1].trim()
    }
    // Fallback: primeira linha longa sem dígitos que pareça um nome
    return lines.firstOrNull { line ->
        line.split(" ").size >= 2 && !line.any { it.isDigit() } && line.length > 8
    } ?: ""
}

// ── Visualização de câmera ────────────────────────────────────────────────────

@Composable
private fun CameraCapture(
    onImageCaptured: (File) -> Unit,
    onCancel: () -> Unit
) {
    val context        = LocalContext.current
    val lifecycleOwner = LocalLifecycleOwner.current
    val executor: ExecutorService = remember { Executors.newSingleThreadExecutor() }
    var imageCapture: ImageCapture? by remember { mutableStateOf(null) }

    Box(modifier = Modifier
        .fillMaxWidth()
        .height(280.dp)) {
        AndroidView(
            factory = { ctx ->
                val previewView = PreviewView(ctx)
                val cameraFuture = ProcessCameraProvider.getInstance(ctx)
                cameraFuture.addListener({
                    val provider = cameraFuture.get()
                    val preview = Preview.Builder().build().also {
                        it.surfaceProvider = previewView.surfaceProvider
                    }
                    val capture = ImageCapture.Builder().build()
                    imageCapture = capture
                    try {
                        provider.unbindAll()
                        provider.bindToLifecycle(lifecycleOwner, CameraSelector.DEFAULT_BACK_CAMERA, preview, capture)
                    } catch (e: Exception) { e.printStackTrace() }
                }, ContextCompat.getMainExecutor(ctx))
                previewView
            },
            modifier = Modifier.fillMaxSize()
        )

        // Botões sobre o preview
        Column(
            modifier = Modifier.align(Alignment.BottomCenter).padding(bottom = 8.dp),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            Button(onClick = {
                val file = File(context.cacheDir, "scan_${System.currentTimeMillis()}.jpg")
                val output = ImageCapture.OutputFileOptions.Builder(file).build()
                imageCapture?.takePicture(output, executor,
                    object : ImageCapture.OnImageSavedCallback {
                        override fun onImageSaved(out: ImageCapture.OutputFileResults) {
                            onImageCaptured(file)
                        }
                        override fun onError(exc: ImageCaptureException) { exc.printStackTrace() }
                    })
            }) { Text("Capturar") }
            TextButton(onClick = onCancel) { Text("Cancelar") }
        }
    }
}
