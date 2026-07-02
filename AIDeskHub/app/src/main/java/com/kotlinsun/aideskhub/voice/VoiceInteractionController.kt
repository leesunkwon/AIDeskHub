package com.kotlinsun.aideskhub.voice

import android.content.Context
import android.content.Intent
import android.os.Bundle
import android.speech.RecognitionListener
import android.speech.RecognizerIntent
import android.speech.SpeechRecognizer
import java.util.Locale

class VoiceInteractionController(
    private val context: Context,
) {
    interface Listener {
        fun onReady()
        fun onPartialText(text: String)
        fun onResult(text: String)
        fun onError(message: String)
    }

    private var speechRecognizer: SpeechRecognizer? = null

    fun isAvailable(): Boolean = SpeechRecognizer.isRecognitionAvailable(context)

    fun startListening(listener: Listener) {
        if (!isAvailable()) {
            listener.onError("이 기기에서 음성 인식을 사용할 수 없습니다.")
            return
        }

        stopListening()
        speechRecognizer = SpeechRecognizer.createSpeechRecognizer(context).apply {
            setRecognitionListener(createRecognitionListener(listener))
            startListening(createRecognizerIntent())
        }
    }

    fun stopListening() {
        speechRecognizer?.stopListening()
        speechRecognizer?.cancel()
        speechRecognizer?.destroy()
        speechRecognizer = null
    }

    fun destroy() {
        stopListening()
    }

    private fun createRecognizerIntent(): Intent {
        return Intent(RecognizerIntent.ACTION_RECOGNIZE_SPEECH).apply {
            putExtra(RecognizerIntent.EXTRA_LANGUAGE_MODEL, RecognizerIntent.LANGUAGE_MODEL_FREE_FORM)
            putExtra(RecognizerIntent.EXTRA_LANGUAGE, Locale.KOREA.toLanguageTag())
            putExtra(RecognizerIntent.EXTRA_LANGUAGE_PREFERENCE, Locale.KOREA.toLanguageTag())
            putExtra(RecognizerIntent.EXTRA_PARTIAL_RESULTS, true)
            putExtra(RecognizerIntent.EXTRA_MAX_RESULTS, 1)
            putExtra(RecognizerIntent.EXTRA_PROMPT, "질문을 말씀해 주세요.")
        }
    }

    private fun createRecognitionListener(listener: Listener): RecognitionListener {
        return object : RecognitionListener {
            override fun onReadyForSpeech(params: Bundle?) {
                listener.onReady()
            }

            override fun onBeginningOfSpeech() = Unit

            override fun onRmsChanged(rmsdB: Float) = Unit

            override fun onBufferReceived(buffer: ByteArray?) = Unit

            override fun onEndOfSpeech() = Unit

            override fun onError(error: Int) {
                listener.onError(error.toUserMessage())
                stopListening()
            }

            override fun onResults(results: Bundle?) {
                val text = results.bestText()
                if (text.isBlank()) {
                    listener.onError("질문을 인식하지 못했습니다. 다시 말씀해 주세요.")
                } else {
                    listener.onResult(text)
                }
                stopListening()
            }

            override fun onPartialResults(partialResults: Bundle?) {
                val text = partialResults.bestText()
                if (text.isNotBlank()) {
                    listener.onPartialText(text)
                }
            }

            override fun onEvent(eventType: Int, params: Bundle?) = Unit
        }
    }

    private fun Bundle?.bestText(): String {
        return this
            ?.getStringArrayList(SpeechRecognizer.RESULTS_RECOGNITION)
            ?.firstOrNull()
            .orEmpty()
            .trim()
    }

    private fun Int.toUserMessage(): String {
        return when (this) {
            SpeechRecognizer.ERROR_AUDIO -> "마이크 입력을 처리하지 못했습니다."
            SpeechRecognizer.ERROR_CLIENT -> "음성 인식 요청을 시작하지 못했습니다."
            SpeechRecognizer.ERROR_INSUFFICIENT_PERMISSIONS -> "마이크 권한이 필요합니다."
            SpeechRecognizer.ERROR_NETWORK,
            SpeechRecognizer.ERROR_NETWORK_TIMEOUT,
            -> "음성 인식 네트워크 연결을 확인해 주세요."
            SpeechRecognizer.ERROR_NO_MATCH -> "질문을 인식하지 못했습니다. 다시 말씀해 주세요."
            SpeechRecognizer.ERROR_RECOGNIZER_BUSY -> "음성 인식기가 사용 중입니다. 잠시 후 다시 시도해 주세요."
            SpeechRecognizer.ERROR_SERVER -> "음성 인식 서버에서 오류가 발생했습니다."
            SpeechRecognizer.ERROR_SPEECH_TIMEOUT -> "음성이 감지되지 않았습니다."
            else -> "음성 인식 중 오류가 발생했습니다."
        }
    }
}
