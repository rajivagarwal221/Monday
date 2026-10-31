package com.hush.todo.core

import java.io.ByteArrayOutputStream
import java.net.HttpURLConnection
import java.net.URL
import java.nio.charset.Charset

internal fun httpGet(url: String, headers: Map<String, String> = emptyMap(), timeoutMs: Int = 12_000): String? {
    return try {
        val conn = (URL(url).openConnection() as HttpURLConnection).apply {
            requestMethod = "GET"
            connectTimeout = timeoutMs
            readTimeout = timeoutMs
            headers.forEach { (k, v) -> setRequestProperty(k, v) }
        }
        val code = conn.responseCode
        val stream = if (code in 200..299) conn.inputStream else conn.errorStream
        val body = stream?.bufferedReader()?.readText()
        conn.disconnect()
        if (code in 200..299) body else null
    } catch (_: Exception) {
        null
    }
}

internal fun httpPostJson(url: String, json: String, headers: Map<String, String> = emptyMap(), timeoutMs: Int = 12_000): String? {
    return try {
        val bytes = json.toByteArray(Charset.forName("UTF-8"))
        val conn = (URL(url).openConnection() as HttpURLConnection).apply {
            requestMethod = "POST"
            doOutput = true
            connectTimeout = timeoutMs
            readTimeout = timeoutMs
            setRequestProperty("Content-Type", "application/json")
            headers.forEach { (k, v) -> setRequestProperty(k, v) }
        }
        conn.outputStream.use { it.write(bytes) }
        val code = conn.responseCode
        val stream = if (code in 200..299) conn.inputStream else conn.errorStream
        val body = stream?.bufferedReader()?.readText()
        conn.disconnect()
        if (code in 200..299) body else null
    } catch (_: Exception) {
        null
    }
}

internal fun httpPostMultipart(
    url: String,
    headers: Map<String, String>,
    fields: Map<String, String>,
    fileField: String,
    fileName: String,
    fileBytes: ByteArray,
    mime: String,
    timeoutMs: Int = 15_000,
): String? {
    val boundary = "----hush${System.currentTimeMillis()}"
    return try {
        val body = ByteArrayOutputStream()
        fun write(s: String) = body.write(s.toByteArray(Charset.forName("UTF-8")))
        fields.forEach { (k, v) ->
            write("--$boundary\r\n")
            write("Content-Disposition: form-data; name=\"$k\"\r\n\r\n$v\r\n")
        }
        write("--$boundary\r\n")
        write("Content-Disposition: form-data; name=\"$fileField\"; filename=\"$fileName\"\r\n")
        write("Content-Type: $mime\r\n\r\n")
        body.write(fileBytes)
        write("\r\n--$boundary--\r\n")
        val payload = body.toByteArray()
        val conn = (URL(url).openConnection() as HttpURLConnection).apply {
            requestMethod = "POST"
            doOutput = true
            connectTimeout = timeoutMs
            readTimeout = timeoutMs
            setRequestProperty("Content-Type", "multipart/form-data; boundary=$boundary")
            headers.forEach { (k, v) -> setRequestProperty(k, v) }
        }
        conn.outputStream.use { it.write(payload) }
        val code = conn.responseCode
        val stream = if (code in 200..299) conn.inputStream else conn.errorStream
        val text = stream?.bufferedReader()?.readText()
        conn.disconnect()
        if (code in 200..299) text else null
    } catch (_: Exception) {
        null
    }
}
