package com.hush.todo.core

import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import org.json.JSONArray
import org.json.JSONObject
import java.util.TimeZone

class CloudLlm(private val settings: SecureSettings) : LlmClient {
    override suspend fun parseAmbiguous(text: String, nowMs: Long, tz: String): ParseResult? = withContext(Dispatchers.IO) {
        val key = settings.geminiKey
        if (key.isBlank()) return@withContext null
        val prompt = """You extract a student deadline from short English, Hindi, or Hinglish.
Now is ${java.time.Instant.ofEpochMilli(nowMs)} timezone $tz.
Return ONLY JSON: {"intent":"add"|"query_week"|"complete","title":string,"due":string|null ISO,"completeQuery":string|null,"confidence":0-1}
Text: $text"""
        val body = JSONObject()
            .put("contents", JSONArray().put(JSONObject().put("parts", JSONArray().put(JSONObject().put("text", prompt)))))
            .toString()
        val raw = httpPostJson(
            "https://generativelanguage.googleapis.com/v1beta/models/gemini-2.0-flash-lite:generateContent?key=${java.net.URLEncoder.encode(key, "UTF-8")}",
            body,
            timeoutMs = 8000,
        ) ?: return@withContext DateParser.parse(text, nowMs)
        val textOut = geminiText(raw) ?: return@withContext DateParser.parse(text, nowMs)
        val match = Regex("\\{[\\s\\S]*\\}").find(textOut)?.value ?: return@withContext DateParser.parse(text, nowMs)
        try {
            val o = JSONObject(match)
            val dueStr = o.optString("due", "")
            val dueMs = dueStr.takeIf { it.isNotBlank() && it != "null" }?.let { parseIso(it) }
            val conf = o.optDouble("confidence", 0.7).toFloat()
            ParseResult(
                intent = o.optString("intent", "add"),
                title = o.optString("title", text),
                dueMs = dueMs,
                confidence = conf,
                needsReview = conf < 0.65f,
                completeQuery = o.optString("completeQuery", "").ifBlank { null },
                raw = text,
            )
        } catch (_: Exception) {
            DateParser.parse(text, nowMs)
        }
    }

    override suspend fun transcribe(audio: ByteArray, lang: String): String? = withContext(Dispatchers.IO) {
        val key = settings.groqKey
        if (key.isBlank() || audio.isEmpty()) return@withContext null
        val fields = mutableMapOf("model" to "whisper-large-v3-turbo")
        if (lang == "en") fields["language"] = "en"
        if (lang == "hi") fields["language"] = "hi"
        val raw = httpPostMultipart(
            "https://api.groq.com/openai/v1/audio/transcriptions",
            mapOf("Authorization" to "Bearer $key"),
            fields,
            "file",
            "clip.m4a",
            audio,
            "audio/mp4",
            timeoutMs = 8000,
        ) ?: return@withContext null
        try {
            JSONObject(raw).optString("text").ifBlank { null }
        } catch (_: Exception) {
            null
        }
    }

    override suspend fun generate(prompt: String): String? = withContext(Dispatchers.IO) {
        val key = settings.geminiKey
        if (key.isBlank()) return@withContext null
        val body = JSONObject()
            .put("contents", JSONArray().put(JSONObject().put("parts", JSONArray().put(JSONObject().put("text", prompt)))))
            .toString()
        val raw = httpPostJson(
            "https://generativelanguage.googleapis.com/v1beta/models/gemini-2.0-flash-lite:generateContent?key=${java.net.URLEncoder.encode(key, "UTF-8")}",
            body,
            timeoutMs = 12_000,
        ) ?: return@withContext null
        geminiText(raw)?.trim()?.ifBlank { null }
    }
}

private fun geminiText(raw: String): String? = try {
    JSONObject(raw)
        .getJSONArray("candidates")
        .getJSONObject(0)
        .getJSONObject("content")
        .getJSONArray("parts")
        .getJSONObject(0)
        .optString("text")
        .ifBlank { null }
} catch (_: Exception) {
    null
}

fun deviceTz(): String = TimeZone.getDefault().id

private fun parseIso(raw: String): Long? {
    return runCatching { java.time.Instant.parse(raw).toEpochMilli() }.getOrNull()
        ?: runCatching { java.time.OffsetDateTime.parse(raw).toInstant().toEpochMilli() }.getOrNull()
        ?: runCatching {
            java.time.LocalDateTime.parse(raw).atZone(java.time.ZoneId.systemDefault()).toInstant().toEpochMilli()
        }.getOrNull()
        ?: runCatching {
            java.time.LocalDate.parse(raw.take(10)).atTime(17, 0)
                .atZone(java.time.ZoneId.systemDefault()).toInstant().toEpochMilli()
        }.getOrNull()
}
