package com.hush.todo.core

import com.hush.todo.data.entities.TaskEntity

interface TaskStore {
    suspend fun add(task: TaskEntity)
    suspend fun update(task: TaskEntity)
    suspend fun remove(id: String)
    suspend fun get(id: String): TaskEntity?
    suspend fun complete(id: String)
    suspend fun snooze(id: String, untilMs: Long)
}

interface SpeechInput {
    fun start(lang: String)
    fun stop(onResult: (transcript: String, confidence: Float, audio: ByteArray?) -> Unit)
    fun abort()
}

interface LlmClient {
    suspend fun parseAmbiguous(text: String, nowMs: Long, tz: String): ParseResult?
    suspend fun transcribe(audio: ByteArray, lang: String): String?
    suspend fun generate(prompt: String): String?
}
