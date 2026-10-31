package com.hush.todo.google

import com.hush.todo.core.RawItem
import com.hush.todo.core.SecureSettings
import com.hush.todo.core.httpGet
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import org.json.JSONObject
import java.net.URLEncoder
import java.time.Instant
import java.time.OffsetDateTime
import java.time.ZoneOffset

suspend fun fetchGoogleDeadlines(settings: SecureSettings): List<RawItem> = withContext(Dispatchers.IO) {
    val token = settings.googleToken
    if (token.isBlank()) return@withContext emptyList()
    val headers = mapOf("Authorization" to "Bearer $token")
    val items = mutableListOf<RawItem>()
    val now = Instant.now()
    val timeMin = now.toString()
    val timeMax = now.plusSeconds(60L * 86400).toString()
    val cal = httpGet(
        "https://www.googleapis.com/calendar/v3/calendars/primary/events?singleEvents=true&orderBy=startTime&timeMin=${enc(timeMin)}&timeMax=${enc(timeMax)}&maxResults=40",
        headers,
    )
    if (cal != null) {
        val json = JSONObject(cal)
        val arr = json.optJSONArray("items")
        if (arr != null) {
            for (i in 0 until arr.length()) {
                val ev = arr.getJSONObject(i)
                val start = ev.optJSONObject("start")
                val whenMs = parseWhen(start?.optString("dateTime"), start?.optString("date"))
                items += RawItem(
                    id = "cal:${ev.optString("id")}",
                    source = "calendar",
                    title = ev.optString("summary", "Event"),
                    body = ev.optString("description", ""),
                    whenMs = whenMs,
                )
            }
        }
    }
    val domain = settings.senderDomain.removePrefix("@").trim()
    val q = if (domain.isNotBlank()) "from:@$domain newer_than:21d" else "newer_than:7d"
    val list = httpGet(
        "https://gmail.googleapis.com/gmail/v1/users/me/messages?q=${enc(q)}&maxResults=20",
        headers,
    )
    if (list != null) {
        val json = JSONObject(list)
        val arr = json.optJSONArray("messages")
        if (arr != null) {
            for (i in 0 until arr.length()) {
                val id = arr.getJSONObject(i).optString("id")
                val full = httpGet(
                    "https://gmail.googleapis.com/gmail/v1/users/me/messages/$id?format=metadata&metadataHeaders=Subject&metadataHeaders=From",
                    headers,
                ) ?: continue
                val msg = JSONObject(full)
                val headersList = msg.optJSONObject("payload")?.optJSONArray("headers")
                var subject = "(no subject)"
                var from = ""
                if (headersList != null) {
                    for (h in 0 until headersList.length()) {
                        val row = headersList.getJSONObject(h)
                        when (row.optString("name")) {
                            "Subject" -> subject = row.optString("value")
                            "From" -> from = row.optString("value")
                        }
                    }
                }
                items += RawItem(
                    id = "mail:$id:$from",
                    source = "email",
                    title = subject,
                    body = "$from ${msg.optString("snippet")}",
                    whenMs = null,
                )
            }
        }
    }
    items
}

private fun enc(s: String) = URLEncoder.encode(s, "UTF-8")

private fun parseWhen(dateTime: String?, date: String?): Long? {
    if (!dateTime.isNullOrBlank()) {
        return runCatching { Instant.parse(dateTime) }.getOrNull()?.toEpochMilli()
            ?: runCatching { OffsetDateTime.parse(dateTime).toInstant().toEpochMilli() }.getOrNull()
    }
    if (!date.isNullOrBlank()) {
        return runCatching {
            java.time.LocalDate.parse(date.take(10)).atTime(17, 0).toInstant(ZoneOffset.UTC).toEpochMilli()
        }.getOrNull()
    }
    return null
}
