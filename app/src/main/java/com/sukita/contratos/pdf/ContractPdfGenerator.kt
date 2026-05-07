package com.sukita.contratos.pdf

import android.content.Context
import android.graphics.Bitmap
import android.graphics.Canvas
import android.graphics.pdf.PdfDocument
import android.os.Build
import android.print.PrintAttributes
import android.print.pdf.PrintedPdfDocument
import android.webkit.WebView
import android.webkit.WebViewClient
import com.sukita.contratos.model.ContractData
import com.sukita.contratos.util.CurrencyFormatter
import com.sukita.contratos.util.DateCalculator
import com.sukita.contratos.util.NameFormatter
import com.sukita.contratos.util.NumberToWords
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.suspendCancellableCoroutine
import kotlinx.coroutines.withContext
import java.io.File
import java.io.FileOutputStream
import kotlin.coroutines.resume

/**
 * Gera o PDF do contrato a partir do template HTML, substituindo os
 * placeholders pelos dados do ContractData.
 *
 * Estratégia:
 * 1. Carrega o template HTML de assets
 * 2. Substitui os placeholders
 * 3. Renderiza via WebView
 * 4. Exporta para PDF usando PrintedPdfDocument (A4)
 */
object ContractPdfGenerator {

    /**
     * Gera o PDF e salva no diretório de cache do app.
     * @return File do PDF gerado, ou null em caso de erro.
     */
    suspend fun generate(context: Context, data: ContractData): File? {
        return withContext(Dispatchers.IO) {
            try {
                val html = buildHtml(context, data)
                val pdfFile = outputFile(context, data)
                renderToPdf(context, html, pdfFile)
                pdfFile
            } catch (e: Exception) {
                e.printStackTrace()
                null
            }
        }
    }

    // ── Substituição de placeholders ─────────────────────────────────────────

    private fun buildHtml(context: Context, data: ContractData): String {
        val template = context.assets
            .open(data.apartment.templateHtmlAsset)
            .bufferedReader()
            .readText()

        val months     = data.termMonths
        val endDate    = DateCalculator.calcEndDate(data.startDate, months) ?: ""
        val rentCents  = data.rentValueCents
        val rentFormatted = CurrencyFormatter.format(rentCents)
        val rentWords  = NumberToWords.fromCents(rentCents)
        val termWords  = NumberToWords.monthsToWords(months)
        val sigDate    = DateCalculator.toWrittenDate(data.signatureDate)

        return template
            .replace("{{TENANT_NAME}}",       data.tenantName)
            .replace("{{TENANT_NAME_UPPER}}", NameFormatter.toUpper(data.tenantName))
            .replace("{{CPF}}",               data.cpf)
            .replace("{{RG}}",                data.rg)
            .replace("{{PROPERTY_ADDRESS}}", data.apartment.address)
            .replace("{{UC_CODE}}",           data.apartment.ucCode)
            .replace("{{RENT_VALUE}}",        rentFormatted)
            .replace("{{RENT_VALUE_WORDS}}",  rentWords)
            .replace("{{TERM_MONTHS}}",       months.toString())
            .replace("{{TERM_MONTHS_WORDS}}", termWords)
            .replace("{{START_DATE}}",        data.startDate)
            .replace("{{END_DATE}}",          endDate)
            .replace("{{PAYMENT_DAY}}",       "%02d".format(data.paymentDay))
            .replace("{{SIGNATURE_DATE}}",    sigDate)
    }

    // ── Renderização WebView → PDF ───────────────────────────────────────────

    private suspend fun renderToPdf(
        context: Context,
        html: String,
        outputFile: File
    ) = suspendCancellableCoroutine<Unit> { cont ->
        // WebView precisa rodar na Main thread
        android.os.Handler(android.os.Looper.getMainLooper()).post {
            val webView = WebView(context)
            webView.settings.apply {
                javaScriptEnabled    = false
                allowFileAccess      = false
                allowContentAccess   = false
                blockNetworkLoads    = true
            }

            webView.webViewClient = object : WebViewClient() {
                override fun onPageFinished(view: WebView?, url: String?) {
                    // Página carregada → converte para PDF
                    val attrs = PrintAttributes.Builder()
                        .setMediaSize(PrintAttributes.MediaSize.ISO_A4)
                        .setResolution(PrintAttributes.Resolution("pdf", "pdf", 300, 300))
                        .setMinMargins(PrintAttributes.Margins.NO_MARGINS)
                        .build()

                    val pdfDoc = PrintedPdfDocument(context, attrs)
                    val pageCount = estimatePages(webView)

                    for (pageNum in 1..pageCount) {
                        val pageInfo = PdfDocument.PageInfo.Builder(
                            mmToPixels(210), mmToPixels(297), pageNum
                        ).create()
                        val page = pdfDoc.startPage(pageInfo)
                        val canvas: Canvas = page.canvas
                        val scaleX = canvas.width.toFloat() / webView.width
                        canvas.scale(scaleX, scaleX)
                        canvas.translate(
                            0f,
                            -mmToPixels(297).toFloat() * (pageNum - 1) / scaleX
                        )
                        webView.draw(canvas)
                        pdfDoc.finishPage(page)
                    }

                    FileOutputStream(outputFile).use { pdfDoc.writeTo(it) }
                    pdfDoc.close()

                    if (cont.isActive) cont.resume(Unit)
                }
            }

            // Define tamanho para renderização
            val widthPx = mmToPixels(210)
            webView.measure(
                android.view.View.MeasureSpec.makeMeasureSpec(widthPx, android.view.View.MeasureSpec.EXACTLY),
                android.view.View.MeasureSpec.makeMeasureSpec(0, android.view.View.MeasureSpec.UNSPECIFIED)
            )
            webView.layout(0, 0, widthPx, webView.measuredHeight)
            webView.loadDataWithBaseURL(null, html, "text/html", "UTF-8", null)
        }
    }

    private fun estimatePages(webView: WebView): Int {
        val pageHeightPx = mmToPixels(297)
        return maxOf(1, (webView.measuredHeight + pageHeightPx - 1) / pageHeightPx)
    }

    /** Converte milímetros para pixels @ 96 dpi (padrão WebView) */
    private fun mmToPixels(mm: Int): Int = (mm * 96 / 25.4).toInt()

    // ── Naming do arquivo de saída ──────────────────────────────────────────

    private fun outputFile(context: Context, data: ContractData): File {
        val dir = File(context.cacheDir, "contracts").also { it.mkdirs() }
        val aptSlug   = data.apartment.name.replace(Regex("[^\\w]"), "_")
        val nameSlug  = data.tenantName.split(" ").first().lowercase()
        val dateSlug  = data.startDate.replace("/", "")
        val fileName  = "contrato_${aptSlug}_${nameSlug}_${dateSlug}.pdf"
        return File(dir, fileName)
    }
}
