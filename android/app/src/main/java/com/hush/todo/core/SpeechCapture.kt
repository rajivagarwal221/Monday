package com.hush.todo.core

import android.content.Context
import android.content.Intent
import android.media.MediaRecorder
import android.os.Build
import android.os.Bundle
import android.speech.RecognitionListener
import android.speech.RecognizerIntent
import android.speech.SpeechRecognizer
import java.io.File

class SpeechCapture(private val ctx: Context) : SpeechInput {
    private var rec: SpeechRecognizer? = null
    private var recorder: MediaRecorder? = null
    private var audioFile: File? = null
    private var lastText = ""
    private var lastConf = 0f
    private var callback: ((String, Float, ByteArray?) -> Unit)? = null

    override fun start(lang: String) {
        lastText = ""; lastConf = 0f
        rec?.destroy()
        startRecorder()
        rec = SpeechRecognizer.createSpeechRecognizer(ctx)
        rec?.setRecognitionListener(object : RecognitionListener {
            override fun onReadyForSpeech(params: Bundle?) {}
            override fun onBeginningOfSpeech() {}
            override fun onRmsChanged(rmsdB: Float) {}
            override fun onBufferReceived(buffer: ByteArray?) {}
            override fun onEndOfSpeech() {}
            override fun onError(error: Int) { finish() }
            override fun onResults(results: Bundle) {
                val texts = results.getStringArrayList(SpeechRecognizer.RESULTS_RECOGNITION)
                val confs = results.getFloatArray(SpeechRecognizer.CONFIDENCE_SCORES)
                lastText = texts?.firstOrNull().orEmpty()
                lastConf = confs?.firstOrNull() ?: 0f
                finish()
            }
            override fun onPartialResults(partialResults: Bundle) {
                lastText = partialResults.getStringArrayList(SpeechRecognizer.RESULTS_RECOGNITION)?.firstOrNull().orEmpty()
            }
            override fun onEvent(eventType: Int, params: Bundle?) {}
        })
        val locale = when (lang) {
            "hi" -> "hi-IN"
            "en" -> "en-IN"
            else -> "en-IN"
        }
        val intent = Intent(RecognizerIntent.ACTION_RECOGNIZE_SPEECH).apply {
            putExtra(RecognizerIntent.EXTRA_LANGUAGE_MODEL, RecognizerIntent.LANGUAGE_MODEL_FREE_FORM)
            putExtra(RecognizerIntent.EXTRA_LANGUAGE, locale)
            putExtra(RecognizerIntent.EXTRA_PARTIAL_RESULTS, true)
        }
        rec?.startListening(intent)
    }

    override fun stop(onResult: (transcript: String, confidence: Float, audio: ByteArray?) -> Unit) {
        callback = onResult
        rec?.stopListening()
    }

    override fun abort() {
        callback = null
        rec?.cancel()
        rec?.destroy()
        rec = null
        stopRecorder()
        audioFile?.delete()
        audioFile = null
    }

    private fun finish() {
        val cb = callback
        callback = null
        rec?.destroy()
        rec = null
        val audio = stopRecorder()
        cb?.invoke(lastText, lastConf, audio)
    }

    private fun startRecorder() {
        stopRecorder()
        val file = File(ctx.cacheDir, "hush-voice.m4a")
        if (file.exists()) file.delete()
        audioFile = file
        recorder = if (Build.VERSION.SDK_INT >= 31) MediaRecorder(ctx) else @Suppress("DEPRECATION") MediaRecorder()
        try {
            recorder?.apply {
                setAudioSource(MediaRecorder.AudioSource.MIC)
                setOutputFormat(MediaRecorder.OutputFormat.MPEG_4)
                setAudioEncoder(MediaRecorder.AudioEncoder.AAC)
                setOutputFile(file.absolutePath)
                prepare()
                start()
            }
        } catch (_: Exception) {
            recorder?.release()
            recorder = null
        }
    }

    private fun stopRecorder(): ByteArray? {
        try {
            recorder?.stop()
        } catch (_: Exception) { }
        recorder?.release()
        recorder = null
        val bytes = audioFile?.takeIf { it.exists() }?.readBytes()
        audioFile?.delete()
        audioFile = null
        return bytes?.takeIf { it.isNotEmpty() }
    }
}
