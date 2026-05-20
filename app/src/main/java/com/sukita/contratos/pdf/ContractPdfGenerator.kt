package com.sukita.contratos.pdf

import android.content.Context
import android.graphics.pdf.PdfDocument
import android.view.View
import android.webkit.WebView
import android.webkit.WebViewClient
import com.sukita.contratos.model.ContractData
import com.sukita.contratos.util.CurrencyFormatter
import com.sukita.contratos.util.DateCalculator
import com.sukita.contratos.util.NameFormatter
import com.sukita.contratos.util.NumberToWords
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.TimeoutCancellationException
import kotlinx.coroutines.suspendCancellableCoroutine
import kotlinx.coroutines.withContext
import kotlinx.coroutines.withTimeout
import java.io.File
import java.io.FileOutputStream
import java.util.concurrent.atomic.AtomicBoolean
import kotlin.coroutines.resume
import kotlin.coroutines.resumeWithException

/**
 * Gera o PDF do contrato a partir do template HTML, substituindo os
 * placeholders pelos dados do ContractData.
 *
 * Estratégia:
 * 1. Carrega o template HTML de assets e substitui todos os {{PLACEHOLDERS}}
 * 2. Renderiza via WebView (100% offline — sem rede) na Main thread
 * 3. Após o conteúdo estar totalmente carregado (onPageFinished + 400 ms),
 *    re-mede o WebView com a altura real do conteúdo HTML
 * 4. Exporta página a página para PdfDocument (A4 @ 96 dpi)
 * 5. Verifica que o arquivo foi criado antes de retornar
 *
 * Nenhum dado pessoal é logado nem enviado para a nuvem.
 */
object ContractPdfGenerator {

    // A4 @ 96 dpi (referência usada pelo WebView para renderização HTML)
    private const val DPI           = 96
    private const val A4_WIDTH_MM   = 210
    private const val A4_HEIGHT_MM  = 297

    private val widthPx     get() = mmToPx(A4_WIDTH_MM)
    private val pageHeightPx get() = mmToPx(A4_HEIGHT_MM)

    private fun mmToPx(mm: Int): Int = (mm * DPI / 25.4).toInt()

    /**
     * Gera o PDF e salva no diretório de cache do app.
     * @return File do PDF gerado, ou null em caso de erro.
     *
     * Executa na Main thread (WebView exige) com timeout de 20 s para evitar
     * carregamento infinito. O arquivo de saída é verificado antes de retornar.
     */
    /**
     * Gera o PDF. Retorna o arquivo em caso de sucesso.
     * Em caso de erro, chama [onError] com a descrição completa e retorna null —
     * o chamador pode exibir essa string diretamente na UI para facilitar o diagnóstico.
     */
    suspend fun generate(
        context: Context,
        data: ContractData,
        onError: (String) -> Unit = {}
    ): File? {
        return try {
            val html    = buildHtml(context, data)
            val pdfFile = outputFile(context, data)
            // WebView DEVE rodar na Main thread; withTimeout previne hang infinito
            withContext(Dispatchers.Main) {
                withTimeout(20_000L) {
                    renderToPdf(context, html, pdfFile)
                }
            }
            if (pdfFile.exists() && pdfFile.length() > 0) pdfFile else {
                val msg = "PDF criado mas vazio (${pdfFile.length()} bytes)"
                android.util.Log.e("PdfGenerator", msg)
                onError(msg)
                null
            }
        } catch (e: TimeoutCancellationException) {
            val msg = "Timeout: WebView não terminou em 20 s"
            android.util.Log.e("PdfGenerator", msg)
            onError(msg)
            null
        } catch (e: Exception) {
            val msg = "${e.javaClass.simpleName}: ${e.message}"
            android.util.Log.e("PdfGenerator", "Erro ao gerar PDF: $msg", e)
            onError(msg)
            null
        }
    }

    // ── Substituição de placeholders ─────────────────────────────────────────

    private fun buildHtml(context: Context, data: ContractData): String {
        val template = context.assets
            .open(data.apartment.templateHtmlAsset)
            .bufferedReader()
            .readText()
            // WebView ignora @page CSS (directiva de impressão). Injeta padding equivalente
            // a 2,5 cm (topo/base ≈ 95 px) e 2 cm (laterais ≈ 76 px) @ 96 DPI.
            .replace(
                "</head>",
                "<style>body{padding:95px 76px;box-sizing:border-box}</style></head>"
            )

        val months         = data.termMonths
        val endDate        = DateCalculator.calcEndDate(data.startDate, months) ?: ""
        val rentFormatted  = CurrencyFormatter.format(data.rentValueCents)
        val rentWords      = NumberToWords.fromCents(data.rentValueCents)
        val termWords      = NumberToWords.monthsToWords(months)
        val sigDateWritten = DateCalculator.toWrittenDate(data.signatureDate)

        return template
            .replace("{{TENANT_NAME}}",        data.tenantName)
            .replace("{{TENANT_NAME_UPPER}}",  NameFormatter.toUpper(data.tenantName))
            .replace("{{CPF}}",                data.cpf)
            .replace("{{RG}}",                 data.rg)
            .replace("{{PROPERTY_ADDRESS}}",   data.apartment.address)
            .replace("{{UC_CODE}}",            data.apartment.ucCode)
            .replace("{{RENT_VALUE}}",         rentFormatted)
            .replace("{{RENT_VALUE_WORDS}}",   rentWords)
            .replace("{{TERM_MONTHS}}",        months.toString())
            .replace("{{TERM_MONTHS_WORDS}}",  termWords)
            .replace("{{START_DATE}}",         data.startDate)
            .replace("{{END_DATE}}",           endDate)
            .replace("{{PAYMENT_DAY}}",        "%02d".format(data.paymentDay))
            .replace("{{SIGNATURE_DATE}}",     sigDateWritten)
    }

    // ── Renderização WebView → PDF ───────────────────────────────────────────

    /**
     * Renderiza o HTML em um WebView off-screen e exporta para PdfDocument.
     *
     * Deve ser chamada dentro de [withContext(Dispatchers.Main)] pois o WebView
     * exige a Main thread. O [suspendCancellableCoroutine] suspende até que
     * [onPageFinished] dispare e o postDelayed de 400 ms conclua.
     *
     * Garantias contra hang:
     *  • [withTimeout] no chamador limita o total a 20 s.
     *  • Flag [done] impede dupla-resumção (onPageFinished pode disparar mais de uma vez).
     *  • [onReceivedError] retorna imediatamente com exceção.
     *  • [invokeOnCancellation] destrói o WebView se o timeout cancelar a coroutine.
     */
    private suspend fun renderToPdf(
        context: Context,
        html: String,
        outputFile: File
    ) = suspendCancellableCoroutine<Unit> { cont ->

        // Já estamos na Main thread (garantido pelo withContext do chamador)
        val wPx  = widthPx
        val hPx  = pageHeightPx
        val done = AtomicBoolean(false)   // previne dupla-resumção

        val webView = WebView(context).apply {
            // OBRIGATÓRIO para renderização off-screen:
            // Hardware layers não funcionam com Canvas do PdfDocument (software canvas) —
            // view.draw(canvas) fica em branco ou lança exceção. Software layer resolve.
            setLayerType(View.LAYER_TYPE_SOFTWARE, null)
            settings.apply {
                javaScriptEnabled  = false
                allowFileAccess    = false
                allowContentAccess = false
            }
        }

        // Destrói o WebView se a coroutine for cancelada (ex.: timeout de 20 s)
        cont.invokeOnCancellation { webView.destroy() }

        webView.webViewClient = object : WebViewClient() {

            override fun onPageFinished(view: WebView, url: String?) {
                // compareAndSet garante que apenas a primeira chamada processa
                if (!done.compareAndSet(false, true)) return

                // Aguarda 400 ms para que o WebView conclua o layout interno
                // antes de medir contentHeight (necessário em todos os níveis de API).
                view.postDelayed({
                    try {
                        val scale    = view.scale.takeIf { it > 0f } ?: 1f
                        val contentH = maxOf((view.contentHeight * scale).toInt(), hPx)

                        // Re-mede com a altura real do conteúdo HTML
                        view.measure(
                            View.MeasureSpec.makeMeasureSpec(wPx, View.MeasureSpec.EXACTLY),
                            View.MeasureSpec.makeMeasureSpec(contentH, View.MeasureSpec.EXACTLY)
                        )
                        view.layout(0, 0, wPx, contentH)

                        val pageCount = (contentH + hPx - 1) / hPx
                        android.util.Log.d(
                            "PdfGenerator",
                            "HTML renderizado: ${wPx}×${contentH}px → $pageCount página(s)"
                        )

                        val pdfDoc = PdfDocument()
                        for (pageNum in 1..pageCount) {
                            val pageInfo = PdfDocument.PageInfo.Builder(wPx, hPx, pageNum).create()
                            val page     = pdfDoc.startPage(pageInfo)
                            page.canvas.translate(0f, -hPx.toFloat() * (pageNum - 1))
                            view.draw(page.canvas)
                            pdfDoc.finishPage(page)
                        }

                        outputFile.parentFile?.mkdirs()
                        FileOutputStream(outputFile).use { pdfDoc.writeTo(it) }
                        pdfDoc.close()

                        android.util.Log.d(
                            "PdfGenerator",
                            "PDF salvo: ${outputFile.name} (${outputFile.length() / 1024} KB)"
                        )

                        if (cont.isActive) cont.resume(Unit)
                    } catch (e: Exception) {
                        android.util.Log.e("PdfGenerator", "Falha na renderização: ${e.javaClass.simpleName}")
                        if (cont.isActive) cont.resumeWithException(e)
                    }
                }, 400L)
            }

            @Deprecated("Deprecated in Java")
            override fun onReceivedError(
                view: WebView,
                errorCode: Int,
                description: String?,
                failingUrl: String?
            ) {
                if (done.compareAndSet(false, true)) {
                    android.util.Log.e("PdfGenerator", "WebView error $errorCode: $description")
                    if (cont.isActive) cont.resumeWithException(
                        Exception("WebView error $errorCode: $description")
                    )
                }
            }
        }

        // Layout inicial — a altura real será recalculada em onPageFinished
        webView.measure(
            View.MeasureSpec.makeMeasureSpec(wPx, View.MeasureSpec.EXACTLY),
            View.MeasureSpec.makeMeasureSpec(0,   View.MeasureSpec.UNSPECIFIED)
        )
        webView.layout(0, 0, wPx, maxOf(webView.measuredHeight, hPx))

        // Usa "file:///android_asset/" como base para que o WebView resolva
        // recursos relativos (CSS, imagens) referenciados no HTML do template.
        webView.loadDataWithBaseURL(
            "file:///android_asset/",
            html,
            "text/html",
            "UTF-8",
            null
        )
    }

    // ── Naming do arquivo de saída ───────────────────────────────────────────

    private fun outputFile(context: Context, data: ContractData): File {
        val dir      = File(context.cacheDir, "contracts").also { it.mkdirs() }
        val aptSlug  = data.apartment.name.replace(Regex("[^\\w]"), "_")
        val nameSlug = data.tenantName.split(" ").first().lowercase()
        val dateSlug = data.startDate.replace("/", "")
        return File(dir, "contrato_${aptSlug}_${nameSlug}_${dateSlug}.pdf")
    }
}
