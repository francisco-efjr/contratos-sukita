package com.sukita.contratos.ui.screens

import android.Manifest
import android.content.pm.PackageManager
import android.graphics.BitmapFactory
import android.net.Uri
import android.widget.Toast
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.camera.core.*
import androidx.camera.lifecycle.ProcessCameraProvider
import androidx.camera.view.PreviewView
import androidx.compose.foundation.Image as BitmapImage
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.AutoAwesome
import androidx.compose.material.icons.filled.CameraAlt
import androidx.compose.material.icons.filled.Image
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.asImageBitmap
import androidx.compose.ui.layout.ContentScale
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
import com.sukita.contratos.ocr.BrazilianDocumentParser
import com.sukita.contratos.ocr.BrazilianDocumentResult
import com.sukita.contratos.ocr.OpenAiOcrService
import com.sukita.contratos.viewmodel.ContractViewModel
import java.io.File
import kotlinx.coroutines.launch
import java.util.concurrent.ExecutorService
import java.util.concurrent.Executors

/**
 * Tela de leitura de documento via câmera ou galeria.
 *
 * ─── MODOS DE OCR ───────────────────────────────────────────────────────────
 * 1. LOCAL (padrão) — Google ML Kit Text Recognition, 100% offline.
 *    Nenhum dado sai do dispositivo.
 *
 * 2. IA (ChatGPT Vision) — OpenAI GPT-4o Vision API.
 *    A imagem é enviada para servidores da OpenAI.
 *    Disponível apenas quando a chave de API estiver configurada (⚙ Configurações).
 *    O usuário vê um aviso de privacidade e confirma antes do envio.
 *
 * ─── FLUXO ──────────────────────────────────────────────────────────────────
 * 1. Captura foto ou seleciona da galeria
 * 2. Pré-visualização → confirma ou tira outra
 * 3. Lê com OCR local OU com IA (escolha do usuário)
 * 4. Dados extraídos aparecem editáveis
 * 5. Usuário confirma → campos do formulário são preenchidos
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun DocumentScanScreen(
    vm: ContractViewModel,
    onDone: () -> Unit
) {
    val context        = LocalContext.current
    val lifecycleOwner = LocalLifecycleOwner.current

    val hasApiKey = remember { OpenAiOcrService.getApiKey(context) != null }

    var captureState  by remember { mutableStateOf<CaptureState>(CaptureState.Idle) }
    var extractedName by remember { mutableStateOf("") }
    var extractedCpf  by remember { mutableStateOf("") }
    var extractedRg   by remember { mutableStateOf("") }
    var extractedType by remember { mutableStateOf("") }
    var extractedDocumentNumber by remember { mutableStateOf("") }
    var ocrRan        by remember { mutableStateOf(false) }
    var ocrError      by remember { mutableStateOf<String?>(null) }

    // Diálogo de aviso de privacidade para modo IA
    var showAiPrivacyDialog by remember { mutableStateOf(false) }
    var pendingAiFile       by remember { mutableStateOf<File?>(null) }

    var hasCamPerm by remember {
        mutableStateOf(
            ContextCompat.checkSelfPermission(context, Manifest.permission.CAMERA)
                == PackageManager.PERMISSION_GRANTED
        )
    }
    val cameraPermLauncher = rememberLauncherForActivityResult(
        ActivityResultContracts.RequestPermission()
    ) { granted -> hasCamPerm = granted }

    // Galeria
    val galleryLauncher = rememberLauncherForActivityResult(
        ActivityResultContracts.GetContent()
    ) { uri: Uri? ->
        uri ?: return@rememberLauncherForActivityResult
        // Copia URI para um arquivo temporário para pré-visualização e OCR
        val tmpFile = File(context.cacheDir, "gallery_${System.currentTimeMillis()}.jpg")
        context.contentResolver.openInputStream(uri)?.use { input ->
            tmpFile.outputStream().use { input.copyTo(it) }
        }
        captureState = CaptureState.Preview(tmpFile)
    }

    // Diálogo de aviso LGPD/privacidade antes de enviar para OpenAI
    if (showAiPrivacyDialog) {
        AlertDialog(
            onDismissRequest = { showAiPrivacyDialog = false },
            icon    = { Icon(Icons.Filled.AutoAwesome, contentDescription = null) },
            title   = { Text("Enviar foto para IA?") },
            text    = {
                Text(
                    "A imagem deste documento será enviada para os servidores da OpenAI " +
                    "(EUA) para leitura por inteligência artificial.\n\n" +
                    "Dados pessoais como CPF, RG e nome serão visíveis para o serviço.\n\n" +
                    "Deseja continuar?"
                )
            },
            confirmButton = {
                Button(onClick = {
                    showAiPrivacyDialog = false
                    val file = pendingAiFile ?: return@Button
                    captureState = CaptureState.Processing
                    runAiOcr(context, file,
                        onResult = { name, cpf, rg ->
                            extractedName = name
                            extractedCpf  = cpf
                            extractedRg   = rg
                            extractedType = "IA"
                            extractedDocumentNumber = ""
                            ocrRan        = true
                            ocrError      = null
                            captureState  = CaptureState.Idle
                        },
                        onError = { msg ->
                            ocrError     = msg
                            ocrRan       = true
                            captureState = CaptureState.Idle
                        }
                    )
                }) { Text("Enviar") }
            },
            dismissButton = {
                TextButton(onClick = { showAiPrivacyDialog = false }) { Text("Cancelar") }
            }
        )
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
                .padding(horizontal = 16.dp)
                .verticalScroll(rememberScrollState()),
            verticalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            Spacer(modifier = Modifier.height(4.dp))

            when (captureState) {

                // ── Idle: botões de captura + resultado ───────────────────────
                CaptureState.Idle -> {
                    Text(
                        text  = "Tire uma foto ou selecione uma imagem da CNH, RG, CIN, CPF ou passaporte.",
                        style = MaterialTheme.typography.bodyMedium
                    )

                    Row(horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                        OutlinedButton(
                            onClick = {
                                if (hasCamPerm) captureState = CaptureState.Camera
                                else cameraPermLauncher.launch(Manifest.permission.CAMERA)
                            },
                            modifier = Modifier.weight(1f)
                        ) {
                            Icon(Icons.Filled.CameraAlt, contentDescription = null)
                            Spacer(Modifier.width(6.dp))
                            Text("Câmera")
                        }
                        OutlinedButton(
                            onClick = { galleryLauncher.launch("image/*") },
                            modifier = Modifier.weight(1f)
                        ) {
                            Icon(Icons.Filled.Image, contentDescription = null)
                            Spacer(Modifier.width(6.dp))
                            Text("Galeria")
                        }
                    }

                    // Erro do OCR
                    ocrError?.let { err ->
                        Card(
                            colors = CardDefaults.cardColors(
                                containerColor = MaterialTheme.colorScheme.errorContainer
                            )
                        ) {
                            Text(
                                text     = err,
                                modifier = Modifier.padding(12.dp),
                                color    = MaterialTheme.colorScheme.onErrorContainer,
                                style    = MaterialTheme.typography.bodySmall
                            )
                        }
                    }

                    // Seção de edição — exibida sempre após OCR rodar
                    if (ocrRan) {
                        HorizontalDivider()

                        if (extractedName.isEmpty() && extractedCpf.isEmpty() && extractedRg.isEmpty()) {
                            Text(
                                text  = "Nenhum dado detectado automaticamente. Preencha manualmente:",
                                style = MaterialTheme.typography.labelMedium,
                                color = MaterialTheme.colorScheme.error
                            )
                        } else {
                            Text(
                                text  = "Dados extraídos — revise antes de confirmar:",
                                style = MaterialTheme.typography.labelMedium
                            )
                        }

                        if (extractedType.isNotBlank()) {
                            Text(
                                text = buildString {
                                    append("Tipo identificado: ")
                                    append(extractedType)
                                    if (extractedDocumentNumber.isNotBlank()) {
                                        append(" • Nº ")
                                        append(extractedDocumentNumber)
                                    }
                                },
                                style = MaterialTheme.typography.bodySmall,
                                color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.65f)
                            )
                        }

                        ContractTextField(
                            label  = "Nome",
                            value  = extractedName,
                            onValueChange = { extractedName = it },
                            keyboardOptions = KeyboardOptions(capitalization = KeyboardCapitalization.Words)
                        )
                        ContractTextField(
                            label  = "CPF",
                            value  = extractedCpf,
                            onValueChange = { extractedCpf = it },
                            keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                            placeholder = "000.000.000-00"
                        )
                        ContractTextField(
                            label  = "RG",
                            value  = extractedRg,
                            onValueChange = { extractedRg = BrazilianDocumentParser.normalizeRg(it) },
                            keyboardOptions = KeyboardOptions(
                                capitalization = KeyboardCapitalization.Characters,
                                keyboardType = KeyboardType.Text
                            ),
                            placeholder = "Ex: 12.345.678-9 SSP/AM"
                        )

                        Button(
                            onClick = {
                                vm.fillFromOcr(
                                    name = extractedName.takeIf { it.isNotBlank() },
                                    cpf  = extractedCpf.takeIf  { it.isNotBlank() },
                                    rg   = extractedRg.takeIf   { it.isNotBlank() }
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

                // ── Câmera ────────────────────────────────────────────────────
                CaptureState.Camera -> {
                    if (hasCamPerm) {
                        CameraCapture(
                            onImageCaptured = { file ->
                                captureState = CaptureState.Preview(file)
                            },
                            onCancel = { captureState = CaptureState.Idle }
                        )
                    } else {
                        LaunchedEffect(Unit) {
                            cameraPermLauncher.launch(Manifest.permission.CAMERA)
                            captureState = CaptureState.Idle
                        }
                    }
                }

                // ── Pré-visualização ──────────────────────────────────────────
                is CaptureState.Preview -> {
                    val file   = (captureState as CaptureState.Preview).file
                    val bitmap = remember(file) {
                        BitmapFactory.decodeFile(file.absolutePath)?.asImageBitmap()
                    }

                    Text(
                        text  = "Confira a foto — o documento deve estar legível:",
                        style = MaterialTheme.typography.bodyMedium
                    )

                    if (bitmap != null) {
                        BitmapImage(
                            bitmap             = bitmap,
                            contentDescription = "Pré-visualização",
                            modifier           = Modifier
                                .fillMaxWidth()
                                .heightIn(min = 180.dp, max = 360.dp),
                            contentScale       = ContentScale.Fit
                        )
                    }

                    // Botão: OCR local (sempre disponível)
                    Button(
                        onClick = {
                            captureState = CaptureState.Processing
                            val image = InputImage.fromFilePath(context, Uri.fromFile(file))
                            runLocalOcr(image,
                                onResult = { result ->
                                    extractedName = result.name
                                    extractedCpf  = result.cpf
                                    extractedRg   = result.rg
                                    extractedType = result.typeLabel
                                    extractedDocumentNumber = result.documentNumber
                                    ocrRan        = true
                                    ocrError      = null
                                    captureState  = CaptureState.Idle
                                }
                            )
                        },
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Text("Ler offline: CNH, RG, CIN ou CPF")
                    }

                    // Botão: IA (somente com chave configurada)
                    if (hasApiKey) {
                        OutlinedButton(
                            onClick = {
                                pendingAiFile       = file
                                showAiPrivacyDialog = true
                            },
                            modifier = Modifier.fillMaxWidth()
                        ) {
                            Icon(Icons.Filled.AutoAwesome, contentDescription = null)
                            Spacer(Modifier.width(8.dp))
                            Text("Ler com IA — ChatGPT Vision")
                        }
                    } else {
                        Text(
                            text  = "💡 Configure sua chave OpenAI em ⚙ Configurações para usar leitura por IA.",
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.6f)
                        )
                    }

                    OutlinedButton(
                        onClick  = { captureState = CaptureState.Camera },
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Icon(Icons.Filled.CameraAlt, contentDescription = null)
                        Spacer(Modifier.width(6.dp))
                        Text("Tirar outra foto")
                    }
                }

                // ── Processando ───────────────────────────────────────────────
                CaptureState.Processing -> {
                    Box(
                        modifier          = Modifier
                            .fillMaxWidth()
                            .padding(vertical = 40.dp),
                        contentAlignment  = Alignment.Center
                    ) {
                        Column(horizontalAlignment = Alignment.CenterHorizontally) {
                            CircularProgressIndicator()
                            Spacer(Modifier.height(12.dp))
                            Text("Lendo documento…", style = MaterialTheme.typography.bodySmall)
                        }
                    }
                }
            }

            Spacer(modifier = Modifier.height(8.dp))
        }
    }
}

// ── Estados ───────────────────────────────────────────────────────────────────

private sealed class CaptureState {
    object Idle       : CaptureState()
    object Camera     : CaptureState()
    data class Preview(val file: File) : CaptureState()
    object Processing : CaptureState()
}

// ── OCR LOCAL (ML Kit, offline) ───────────────────────────────────────────────

private fun runLocalOcr(
    image: InputImage,
    onResult: (BrazilianDocumentResult) -> Unit
) {
    TextRecognition.getClient(TextRecognizerOptions.DEFAULT_OPTIONS)
        .process(image)
        .addOnSuccessListener { result ->
            val lines = result.textBlocks
                .flatMap { block -> block.lines.map { line -> line.text } }
            onResult(BrazilianDocumentParser.parse(result.text, lines))
        }
        .addOnFailureListener {
            onResult(BrazilianDocumentParser.parse(""))
        }
}

// ── OCR VIA IA (OpenAI Vision) ────────────────────────────────────────────────

private fun runAiOcr(
    context: android.content.Context,
    file: File,
    onResult: (name: String, cpf: String, rg: String) -> Unit,
    onError: (String) -> Unit
) {
    kotlinx.coroutines.MainScope().launch {
        try {
            val (name, cpf, rg) = OpenAiOcrService.extractFromImage(context, file)
            onResult(name, cpf, rg)
        } catch (e: Exception) {
            onError("Erro ao ler com IA: ${e.message ?: "Falha desconhecida"}")
        }
    }
}

// ── Câmera ────────────────────────────────────────────────────────────────────

@Composable
private fun CameraCapture(onImageCaptured: (File) -> Unit, onCancel: () -> Unit) {
    val context        = LocalContext.current
    val lifecycleOwner = LocalLifecycleOwner.current
    val executor: ExecutorService = remember { Executors.newSingleThreadExecutor() }
    var imageCapture: ImageCapture? by remember { mutableStateOf(null) }

    Box(modifier = Modifier.fillMaxWidth().height(300.dp)) {
        AndroidView(
            factory = { ctx ->
                val previewView  = PreviewView(ctx)
                val cameraFuture = ProcessCameraProvider.getInstance(ctx)
                cameraFuture.addListener({
                    val provider = cameraFuture.get()
                    val preview  = Preview.Builder().build()
                        .also { it.setSurfaceProvider(previewView.surfaceProvider) }
                    val capture  = ImageCapture.Builder()
                        .setCaptureMode(ImageCapture.CAPTURE_MODE_MAXIMIZE_QUALITY).build()
                    imageCapture = capture
                    try {
                        provider.unbindAll()
                        provider.bindToLifecycle(
                            lifecycleOwner, CameraSelector.DEFAULT_BACK_CAMERA, preview, capture
                        )
                    } catch (e: Exception) { e.printStackTrace() }
                }, ContextCompat.getMainExecutor(ctx))
                previewView
            },
            modifier = Modifier.fillMaxSize()
        )
        Column(
            modifier              = Modifier.align(Alignment.BottomCenter).padding(bottom = 8.dp),
            horizontalAlignment   = Alignment.CenterHorizontally
        ) {
            Button(onClick = {
                val file   = File(context.cacheDir, "scan_${System.currentTimeMillis()}.jpg")
                val output = ImageCapture.OutputFileOptions.Builder(file).build()
                imageCapture?.takePicture(output, executor,
                    object : ImageCapture.OnImageSavedCallback {
                        override fun onImageSaved(out: ImageCapture.OutputFileResults) { onImageCaptured(file) }
                        override fun onError(exc: ImageCaptureException) { exc.printStackTrace() }
                    })
            }) { Text("Capturar") }
            TextButton(onClick = onCancel) { Text("Cancelar") }
        }
    }
}
