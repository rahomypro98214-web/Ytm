package com.example.ui.components

import android.Manifest
import android.app.Activity
import android.content.Context
import android.content.Intent
import android.content.pm.PackageManager
import android.os.Bundle
import android.speech.RecognitionListener
import android.speech.RecognizerIntent
import android.speech.SpeechRecognizer
import android.util.Log
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.Stable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberUpdatedState
import androidx.compose.runtime.setValue
import androidx.compose.ui.platform.LocalContext
import androidx.core.content.ContextCompat
import com.example.R

private const val STT_TAG = "SpeechToTextController"

fun mergeDictatedText(existingText: String, dictatedText: String): String {
    val cleanDictation = dictatedText.trim()
    if (cleanDictation.isEmpty()) return existingText
    val cleanExisting = existingText.trimEnd()
    return if (cleanExisting.isEmpty()) {
        cleanDictation
    } else {
        "$cleanExisting $cleanDictation"
    }
}

@Stable
class SpeechToTextController internal constructor(
    private val isListeningProvider: () -> Boolean,
    private val errorMessageProvider: () -> String?,
    private val onToggleAction: () -> Unit,
    private val onStopAction: () -> Unit,
    private val onDismissErrorAction: () -> Unit
) {
    val isListening: Boolean
        get() = isListeningProvider()

    val errorMessage: String?
        get() = errorMessageProvider()

    fun toggleListening() = onToggleAction()

    fun stopListening() = onStopAction()

    fun dismissError() = onDismissErrorAction()
}

@Composable
fun rememberSpeechToTextController(
    onPartialResult: (String) -> Unit,
    onFinalResult: (String) -> Unit
): SpeechToTextController {
    val context = LocalContext.current
    val latestOnPartial by rememberUpdatedState(onPartialResult)
    val latestOnFinal by rememberUpdatedState(onFinalResult)

    var isListening by remember { mutableStateOf(false) }
    var errorMessage by remember { mutableStateOf<String?>(null) }
    var ignoreNextCancelError by remember { mutableStateOf(false) }

    val speechRecognizer = remember(context) {
        if (SpeechRecognizer.isRecognitionAvailable(context)) {
            runCatching { SpeechRecognizer.createSpeechRecognizer(context) }.getOrNull()
        } else {
            null
        }
    }

    DisposableEffect(speechRecognizer) {
        if (speechRecognizer != null) {
            speechRecognizer.setRecognitionListener(object : RecognitionListener {
                override fun onReadyForSpeech(params: Bundle?) {
                    ignoreNextCancelError = false
                    isListening = true
                    errorMessage = null
                }

                override fun onBeginningOfSpeech() {}

                override fun onRmsChanged(rmsdB: Float) {}

                override fun onBufferReceived(buffer: ByteArray?) {}

                override fun onEndOfSpeech() {
                    isListening = false
                }

                override fun onError(error: Int) {
                    isListening = false
                    if (ignoreNextCancelError &&
                        (error == SpeechRecognizer.ERROR_CLIENT || error == SpeechRecognizer.ERROR_NO_MATCH)
                    ) {
                        ignoreNextCancelError = false
                        return
                    }
                    ignoreNextCancelError = false
                    errorMessage = mapSpeechErrorToArabic(error)
                    Log.w(STT_TAG, "SpeechRecognizer error code: $error")
                }

                override fun onResults(results: Bundle?) {
                    isListening = false
                    ignoreNextCancelError = false
                    val matches = results?.getStringArrayList(SpeechRecognizer.RESULTS_RECOGNITION)
                    val transcript = matches?.firstOrNull().orEmpty().trim()
                    if (transcript.isNotEmpty()) {
                        latestOnFinal(transcript)
                    }
                }

                override fun onPartialResults(partialResults: Bundle?) {
                    val matches = partialResults?.getStringArrayList(SpeechRecognizer.RESULTS_RECOGNITION)
                    val partial = matches?.firstOrNull().orEmpty().trim()
                    if (partial.isNotEmpty()) {
                        latestOnPartial(partial)
                    }
                }

                override fun onEvent(eventType: Int, params: Bundle?) {}
            })
        }

        onDispose {
            runCatching {
                speechRecognizer?.cancel()
                speechRecognizer?.destroy()
            }
        }
    }

    val fallbackActivityLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.StartActivityForResult()
    ) { result ->
        isListening = false
        if (result.resultCode == Activity.RESULT_OK) {
            val matches = result.data?.getStringArrayListExtra(RecognizerIntent.EXTRA_RESULTS)
            val transcript = matches?.firstOrNull().orEmpty().trim()
            if (transcript.isNotEmpty()) {
                latestOnFinal(transcript)
            }
        }
    }

    fun startLiveRecognition() {
        if (speechRecognizer == null) {
            val intent = buildSpeechIntent(context)
            if (intent.resolveActivity(context.packageManager) != null) {
                errorMessage = null
                fallbackActivityLauncher.launch(intent)
            } else {
                errorMessage = context.getString(R.string.speech_not_available)
            }
            return
        }

        try {
            ignoreNextCancelError = false
            errorMessage = null
            isListening = true
            speechRecognizer.startListening(buildSpeechIntent(context))
        } catch (e: Exception) {
            isListening = false
            Log.e(STT_TAG, "Failed to start SpeechRecognizer", e)
            errorMessage = e.localizedMessage ?: context.getString(R.string.speech_not_available)
        }
    }

    val permissionLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.RequestPermission()
    ) { granted ->
        if (granted) {
            startLiveRecognition()
        } else {
            isListening = false
            errorMessage = context.getString(R.string.speech_permission_denied)
        }
    }

    fun toggleListening() {
        if (isListening) {
            ignoreNextCancelError = true
            isListening = false
            runCatching { speechRecognizer?.stopListening() }
            return
        }

        errorMessage = null
        val hasAudioPermission = ContextCompat.checkSelfPermission(
            context,
            Manifest.permission.RECORD_AUDIO
        ) == PackageManager.PERMISSION_GRANTED

        if (speechRecognizer != null) {
            if (hasAudioPermission) {
                startLiveRecognition()
            } else {
                permissionLauncher.launch(Manifest.permission.RECORD_AUDIO)
            }
        } else {
            val intent = buildSpeechIntent(context)
            if (intent.resolveActivity(context.packageManager) != null) {
                fallbackActivityLauncher.launch(intent)
            } else {
                errorMessage = context.getString(R.string.speech_not_available)
            }
        }
    }

    fun stopListening() {
        if (isListening) {
            ignoreNextCancelError = true
            isListening = false
            runCatching { speechRecognizer?.stopListening() }
        }
    }

    return remember(speechRecognizer) {
        SpeechToTextController(
            isListeningProvider = { isListening },
            errorMessageProvider = { errorMessage },
            onToggleAction = { toggleListening() },
            onStopAction = { stopListening() },
            onDismissErrorAction = { errorMessage = null }
        )
    }
}

private fun buildSpeechIntent(context: Context): Intent =
    Intent(RecognizerIntent.ACTION_RECOGNIZE_SPEECH).apply {
        putExtra(RecognizerIntent.EXTRA_LANGUAGE_MODEL, RecognizerIntent.LANGUAGE_MODEL_FREE_FORM)
        putExtra(RecognizerIntent.EXTRA_LANGUAGE, "ar-SA")
        putExtra(RecognizerIntent.EXTRA_LANGUAGE_PREFERENCE, "ar-SA")
        putExtra(RecognizerIntent.EXTRA_PARTIAL_RESULTS, true)
        putExtra(RecognizerIntent.EXTRA_MAX_RESULTS, 1)
        putExtra(RecognizerIntent.EXTRA_CALLING_PACKAGE, context.packageName)
        putExtra(RecognizerIntent.EXTRA_PROMPT, context.getString(R.string.speech_listening))
    }

private fun mapSpeechErrorToArabic(errorCode: Int): String = when (errorCode) {
    SpeechRecognizer.ERROR_AUDIO -> "تعذر تسجيل الصوت من الميكروفون."
    SpeechRecognizer.ERROR_INSUFFICIENT_PERMISSIONS -> "يلزم منح إذن الميكروفون لاستخدام الإملاء الصوتي."
    SpeechRecognizer.ERROR_NETWORK,
    SpeechRecognizer.ERROR_NETWORK_TIMEOUT -> "تعذر الاتصال بخدمة التعرف على الصوت، تحقق من الشبكة."
    SpeechRecognizer.ERROR_NO_MATCH,
    SpeechRecognizer.ERROR_SPEECH_TIMEOUT -> "لم يتم التقاط كلام واضح، حاول التحدث مجددًا."
    SpeechRecognizer.ERROR_RECOGNIZER_BUSY -> "محرك التعرف على الصوت مشغول حاليًا، أعد المحاولة."
    SpeechRecognizer.ERROR_SERVER -> "حدث خطأ في خادم التعرف على الصوت."
    else -> "تعذر إتمام الإملاء الصوتي، حاول مرة أخرى."
}
