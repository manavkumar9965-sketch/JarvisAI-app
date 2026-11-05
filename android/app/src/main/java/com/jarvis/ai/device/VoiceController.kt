package com.jarvis.ai.device

import android.content.Context
import android.content.Intent
import android.os.Bundle
import android.speech.RecognitionListener
import android.speech.RecognizerIntent
import android.speech.SpeechRecognizer
import android.speech.tts.TextToSpeech
import android.speech.tts.UtteranceProgressListener
import java.util.Locale

/** Speech-to-text aur text-to-speech. Ye class main thread se use karo. */
class VoiceController(private val context: Context) : TextToSpeech.OnInitListener {

    private var tts: TextToSpeech? = TextToSpeech(context, this)
    private var ready = false
    private var recognizer: SpeechRecognizer? = null

    var onSpeakingChanged: ((Boolean) -> Unit)? = null

    override fun onInit(status: Int) {
        if (status != TextToSpeech.SUCCESS) return
        val engine = tts ?: return
        engine.language = Locale("en", "IN")
        engine.voices
            ?.firstOrNull { it.name.contains("female", ignoreCase = true) && it.locale.language == "en" }
            ?.let { engine.voice = it }
        engine.setSpeechRate(1.0f)
        engine.setPitch(1.15f)
        engine.setOnUtteranceProgressListener(object : UtteranceProgressListener() {
            override fun onStart(utteranceId: String?) {
                onSpeakingChanged?.invoke(true)
            }

            override fun onDone(utteranceId: String?) {
                onSpeakingChanged?.invoke(false)
            }

            override fun onError(utteranceId: String?) {
                onSpeakingChanged?.invoke(false)
            }
        })
        ready = true
    }

    fun speak(text: String) {
        if (!ready) return
        tts?.speak(text, TextToSpeech.QUEUE_FLUSH, null, "jarvis-${System.nanoTime()}")
    }

    fun stop() {
        if (ready) tts?.stop()
        onSpeakingChanged?.invoke(false)
    }

    fun listen(
        onResult: (String) -> Unit,
        onListeningChanged: (Boolean) -> Unit,
        onFail: (String) -> Unit,
    ) {
        if (!SpeechRecognizer.isRecognitionAvailable(context)) {
            onFail("Is phone par speech recognition available nahi hai.")
            return
        }
        recognizer?.destroy()
        val rec = SpeechRecognizer.createSpeechRecognizer(context)
        recognizer = rec
        rec.setRecognitionListener(object : RecognitionListener {
            override fun onReadyForSpeech(params: Bundle?) {
                onListeningChanged(true)
            }

            override fun onResults(results: Bundle?) {
                onListeningChanged(false)
                val spoken = results?.getStringArrayList(SpeechRecognizer.RESULTS_RECOGNITION)?.firstOrNull()
                if (spoken != null) onResult(spoken)
            }

            override fun onError(error: Int) {
                onListeningChanged(false)
                onFail("Mujhe sunai nahi diya, phir se bolo. (code $error)")
            }

            override fun onBeginningOfSpeech() {}
            override fun onRmsChanged(rmsdB: Float) {}
            override fun onBufferReceived(buffer: ByteArray?) {}
            override fun onEndOfSpeech() {}
            override fun onPartialResults(partialResults: Bundle?) {}
            override fun onEvent(eventType: Int, params: Bundle?) {}
        })
        val intent = Intent(RecognizerIntent.ACTION_RECOGNIZE_SPEECH).apply {
            putExtra(RecognizerIntent.EXTRA_LANGUAGE_MODEL, RecognizerIntent.LANGUAGE_MODEL_FREE_FORM)
            putExtra(RecognizerIntent.EXTRA_LANGUAGE, "en-IN")
            putExtra(RecognizerIntent.EXTRA_PARTIAL_RESULTS, false)
        }
        rec.startListening(intent)
    }

    fun stopListening() {
        recognizer?.stopListening()
    }

    fun shutdown() {
        tts?.shutdown()
        tts = null
        recognizer?.destroy()
        recognizer = null
    }
}
