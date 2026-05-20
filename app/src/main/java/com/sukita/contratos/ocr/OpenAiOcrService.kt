package com.sukita.contratos.ocr

import android.content.Context
import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.util.Base64
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import okhttp3.MediaType.Companion.toMediaType
import okhttp3.OkHttpClient
import okhttp3.Request
import okhttp3.RequestBody.Companion.toRequestBody
import org.json.JSONArray
import org.json.JSONObject
import java.io.ByteArrayOutputStream
import java.io.File
import java.util.concurrent.TimeUnit

/**
 * Serviço de OCR via OpenAI Vision API (GPT-4o).
 *
 * ─── PRIVACIDADE ────────────────────────────────────────────────────────────
 * Ao usar este serviço, a imagem do documento é enviada para os servidores da
 * OpenAI (EUA). O usuário deve ser informado antes do uso.
 * Nunca são enviados dados já processados — apenas a imagem original.
 * A chave de API é armazenada localmente (SharedPreferences) e nunca sai do
 * dispositivo nem é incluída em logs.
 *
 * ─── CONFIGURAÇÃO ───────────────────────────────────────────────────────────
 * 1. No app, toque no ícone ⚙ na tela inicial.
 * 2. Cole sua chave de API OpenAI (começa com "sk-...").
 * 3. (Opcional) Edite o prompt em Configurações → "Prompt de Leitura".
 *    O prompt padrão fica em:  app/src/main/assets/ocr_prompt.txt
 *
 * ─── FLUXO DE FALLBACK ──────────────────────────────────────────────────────
 * DocumentScanScreen tenta primeiro ML Kit (local/offline).
 * Se a chave estiver configurada E o usuário confirmar, usa esta API.
 */
object OpenAiOcrService {

    private const val PREFS_NAME         = "sukita_prefs"
    /** Chave SharedPreferences onde a API key do usuário é salva (nome do campo, não o valor). */
    const val PREF_API_KEY               = "openai_api_key"
    private const val PREF_CUSTOM_PROMPT = "openai_ocr_prompt"
    private const val OPENAI_URL        = "https://api.openai.com/v1/chat/completions"
    private const val MODEL             = "gpt-4o"

    private val client by lazy {
        OkHttpClient.Builder()
            .connectTimeout(30, TimeUnit.SECONDS)
            .readTimeout(60, TimeUnit.SECONDS)
            .build()
    }

    // ── Gestão de chave e prompt ──────────────────────────────────────────────

    /**
     * Retorna a chave de API ativa.
     * Retorna apenas a chave salva pelo usuario em Configuracoes.
     */
    fun getApiKey(context: Context): String? {
        val stored = context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
            .getString(PREF_API_KEY, null)
            ?.takeIf { it.isNotBlank() }
        return stored
    }

    /** Salva a chave de API localmente (nunca logada). */
    fun saveApiKey(context: Context, key: String) {
        context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
            .edit().putString(PREF_API_KEY, key.trim()).apply()
    }

    /** Remove a chave de API. */
    fun clearApiKey(context: Context) {
        context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
            .edit().remove(PREF_API_KEY).apply()
    }

    /**
     * Retorna o prompt ativo.
     * Prioridade: 1º prompt salvo pelo usuário em Configurações,
     *             2º arquivo padrão em assets/ocr_prompt.txt.
     */
    fun getActivePrompt(context: Context): String {
        val custom = context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
            .getString(PREF_CUSTOM_PROMPT, null)
        if (!custom.isNullOrBlank()) return custom
        return context.assets.open("ocr_prompt.txt").bufferedReader().readText()
    }

    /** Salva um prompt customizado. */
    fun saveCustomPrompt(context: Context, prompt: String) {
        context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
            .edit().putString(PREF_CUSTOM_PROMPT, prompt.trim()).apply()
    }

    /** Restaura o prompt padrão do arquivo asset. */
    fun resetPrompt(context: Context) {
        context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
            .edit().remove(PREF_CUSTOM_PROMPT).apply()
    }

    // ── Chamada à API ─────────────────────────────────────────────────────────

    /**
     * Envia a imagem para a OpenAI Vision API e retorna os dados extraídos.
     *
     * @return Triple(nome, cpf, rg) — campos vazios se não encontrados no documento.
     * @throws IllegalStateException se a chave de API não estiver configurada.
     * @throws Exception se a API retornar erro ou a rede falhar.
     */
    suspend fun extractFromImage(
        context: Context,
        imageFile: File
    ): Triple<String, String, String> = withContext(Dispatchers.IO) {

        val apiKey = getApiKey(context)
            ?: throw IllegalStateException(
                "Chave de API não configurada. Acesse ⚙ Configurações e insira sua chave OpenAI."
            )

        val prompt      = getActivePrompt(context)
        val base64Image = imageToBase64(imageFile)

        val payload = JSONObject().apply {
            put("model", MODEL)
            put("max_tokens", 300)
            put("messages", JSONArray().apply {
                put(JSONObject().apply {
                    put("role", "user")
                    put("content", JSONArray().apply {
                        put(JSONObject().apply {
                            put("type", "text")
                            put("text", prompt)
                        })
                        put(JSONObject().apply {
                            put("type", "image_url")
                            put("image_url", JSONObject().apply {
                                put("url", "data:image/jpeg;base64,$base64Image")
                                put("detail", "high")
                            })
                        })
                    })
                })
            })
        }

        val request = Request.Builder()
            .url(OPENAI_URL)
            .addHeader("Authorization", "Bearer $apiKey")
            .post(payload.toString().toRequestBody("application/json".toMediaType()))
            .build()

        val response     = client.newCall(request).execute()
        val responseBody = response.body?.string()

        if (!response.isSuccessful) {
            val detail = runCatching {
                JSONObject(responseBody ?: "{}").getJSONObject("error").getString("message")
            }.getOrDefault("HTTP ${response.code}")
            throw Exception("Erro OpenAI: $detail")
        }

        val content = JSONObject(responseBody ?: "{}")
            .getJSONArray("choices")
            .getJSONObject(0)
            .getJSONObject("message")
            .getString("content")
            .trim()

        parseAiResponse(content)
    }

    // ── Helpers ───────────────────────────────────────────────────────────────

    /** Comprime a imagem para JPEG base64 (máx ~900 KB para economizar tokens). */
    private fun imageToBase64(file: File, maxBytes: Int = 900_000): String {
        val bmp = BitmapFactory.decodeFile(file.absolutePath)
            ?: throw Exception("Não foi possível ler a imagem: ${file.name}")
        var quality = 85
        var bytes: ByteArray
        do {
            val out = ByteArrayOutputStream()
            bmp.compress(Bitmap.CompressFormat.JPEG, quality, out)
            bytes = out.toByteArray()
            quality -= 10
        } while (bytes.size > maxBytes && quality > 20)
        return Base64.encodeToString(bytes, Base64.NO_WRAP)
    }

    /**
     * Faz o parse do JSON retornado pela IA.
     * Se o JSON vier mal-formado, usa regex como fallback.
     */
    private fun parseAiResponse(content: String): Triple<String, String, String> {
        // Remove markdown code fences que a IA às vezes adiciona por engano
        val clean = content
            .removePrefix("```json").removePrefix("```")
            .removeSuffix("```")
            .trim()

        return try {
            val json = JSONObject(clean)
            Triple(
                json.optString("nome", "").trim(),
                json.optString("cpf",  "").trim(),
                json.optString("rg",   "").trim()
            )
        } catch (_: Exception) {
            // Fallback via regex quando o JSON está ligeiramente corrompido
            Triple(
                Regex(""""nome"\s*:\s*"([^"]*)"""").find(clean)?.groupValues?.get(1)?.trim() ?: "",
                Regex(""""cpf"\s*:\s*"([^"]*)"""").find(clean)?.groupValues?.get(1)?.trim() ?: "",
                Regex(""""rg"\s*:\s*"([^"]*)"""").find(clean)?.groupValues?.get(1)?.trim() ?: ""
            )
        }
    }
}
