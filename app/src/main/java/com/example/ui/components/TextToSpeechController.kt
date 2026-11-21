package com.example.ui.components

import android.os.Handler
import android.os.Looper
import android.speech.tts.TextToSpeech
import android.speech.tts.UtteranceProgressListener
import android.util.Log
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.Stable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.platform.LocalContext
import com.example.R
import java.util.Locale

private const val TTS_TAG = "TextToSpeechController"
private val ARABIC_CHAR_REGEX = Regex("[\\u0600-\\u06FF]")
private val LINK_REGEX = Regex("\\[([^\\]]+)]\\([^)]+\\)")
private val CODE_FENCE_REGEX = Regex("```[a-zA-Z0-9_-]*")
private val HEADING_PREFIX_REGEX = Regex("(?m)^\\s{0,3}#{1,6}\\s+")
private val LIST_PREFIX_REGEX = Regex("(?m)^\\s*[-*•>]\\s+")

fun stripMarkdownForSpeech(raw: String): String {
    if (raw.isBlank()) return ""
    return raw
        .replace(LINK_REGEX, "$1")
        .replace(CODE_FENCE_REGEX, " ")
        .replace("`", "")
        .replace(HEADING_PREFIX_REGEX, "")
        .replace(LIST_PREFIX_REGEX, "")
        .replace("**", "")
        .replace("__", "")
        .replace("*", "")
        .replace(Regex("\\s+"), " ")
        .trim()
}

fun splitTextForTts(text: String, maxChunkChars: Int = 3500): List<String> {
    val clean = text.trim()
    if (clean.isEmpty()) return emptyList()
    if (clean.length <= maxChunkChars) return listOf(clean)

    val chunks = mutableListOf<String>()
    var start = 0
    while (start < clean.length) {
        val tentativeEnd = (start + maxChunkChars).coerceAtMost(clean.length)
        if (tentativeEnd == clean.length) {
            chunks.add(clean.substring(start).trim())
            break
        }
        val breakIndex = clean.lastIndexOf(' ', tentativeEnd)
        val cutAt = if (breakIndex > start) breakIndex else tentativeEnd
        chunks.add(clean.substring(start, cutAt).trim())
        start = cutAt
        while (start < clean.length && clean[start].isWhitespace()) {
            start++
        }
    }
    return chunks.filter { it.isNotEmpty() }
}

@Stable
class TextToSpeechController internal constructor(
    private val speakingMessageIdProvider: () -> String?,
    private val errorMessageProvider: () -> String?,
    private val onToggleSpeakAction: (messageId: String, text: String) -> Unit,
    private val onStopAction: () -> Unit,
    private val onDismissErrorAction: () -> Unit
) {
    val speakingMessageId: String?
        get() = speakingMessageIdProvider()

    val errorMessage: String?
        get() = errorMessageProvider()

    fun toggleSpeak(messageId: String, text: String) = onToggleSpeakAction(messageId, text)

    fun stop() = onStopAction()

    fun dismissError() = onDismissErrorAction()
}

@Composable
fun rememberTextToSpeechController(): TextToSpeechController {
    val context = LocalContext.current
    val appContext = context.applicationContext
    val mainHandler = remember { Handler(Looper.getMainLooper()) }

    var speakingMessageId by remember { mutableStateOf<String?>(null) }
    var errorMessage by remember { mutableStateOf<String?>(null) }
    var isInitialized by remember { mutableStateOf(false) }
    var initFailed by remember { mutableStateOf(false) }
    var pendingUtterance by remember { mutableStateOf<Pair<String, String>?>(null) }
    var ttsInstance by remember { mutableStateOf<TextToSpeech?>(null) }
    var isDisposed by remember { mutableStateOf(false) }

    fun speakInternal(tts: TextToSpeech, messageId: String, rawText: String) {
        val clean = stripMarkdownForSpeech(rawText)
        if (clean.isEmpty()) return

        val preferredLocale = if (ARABIC_CHAR_REGEX.containsMatchIn(clean)) {
            Locale.forLanguageTag("ar-SA")
        } else {
            Locale.ENGLISH
        }

        runCatching {
            val langResult = tts.setLanguage(preferredLocale)
            if (langResult == TextToSpeech.LANG_MISSING_DATA || langResult == TextToSpeech.LANG_NOT_SUPPORTED) {
                if (preferredLocale.language == "ar") {
                    val genericAr = tts.setLanguage(Locale.forLanguageTag("ar"))
                    if (genericAr == TextToSpeech.LANG_MISSING_DATA || genericAr == TextToSpeech.LANG_NOT_SUPPORTED) {
                        tts.language = Locale.getDefault()
                    }
                } else {
                    tts.language = Locale.getDefault()
                }
            }
        }

        val maxLen = runCatching { TextToSpeech.getMaxSpeechInputLength() }
            .getOrDefault(3500)
            .coerceIn(500, 3500)
        val chunks = splitTextForTts(clean, maxLen)
        if (chunks.isEmpty()) return

        errorMessage = null
        speakingMessageId = messageId

        chunks.forEachIndexed { index, chunk ->
            val queueMode = if (index == 0) TextToSpeech.QUEUE_FLUSH else TextToSpeech.QUEUE_ADD
            val utteranceId = if (index == chunks.lastIndex) messageId else "${messageId}_part_$index"
            val result = runCatching {
                tts.speak(chunk, queueMode, null, utteranceId)
            }.getOrElse { TextToSpeech.ERROR }
            if (result == TextToSpeech.ERROR) {
                speakingMessageId = null
                errorMessage = appContext.getString(R.string.tts_error_speaking)
                return
            }
        }
    }

    DisposableEffect(appContext) {
        isDisposed = false
        var engine: TextToSpeech? = null
        val created = runCatching {
            lateinit var localEngine: TextToSpeech
            localEngine = TextToSpeech(appContext) { status ->
                mainHandler.post {
                    if (isDisposed) return@post
                    if (status == TextToSpeech.SUCCESS) {
                        isInitialized = true
                        initFailed = false
                        localEngine.setOnUtteranceProgressListener(object : UtteranceProgressListener() {
                            override fun onStart(utteranceId: String?) {
                                if (utteranceId == null) return
                                val baseId = utteranceId.substringBefore("_part_")
                                mainHandler.post { speakingMessageId = baseId }
                            }

                            override fun onDone(utteranceId: String?) {
                                if (utteranceId == null || utteranceId.contains("_part_")) return
                                mainHandler.post {
                                    if (speakingMessageId == utteranceId) {
                                        speakingMessageId = null
                                    }
                                }
                            }

                            override fun onStop(utteranceId: String?, interrupted: Boolean) {
                                if (utteranceId == null) return
                                val baseId = utteranceId.substringBefore("_part_")
                                mainHandler.post {
                                    if (speakingMessageId == baseId) {
                                        speakingMessageId = null
                                    }
                                }
                            }

                            @Deprecated("Deprecated in Java")
                            override fun onError(utteranceId: String?) {
                                mainHandler.post {
                                    speakingMessageId = null
                                    errorMessage = appContext.getString(R.string.tts_error_speaking)
                                }
                            }

                            override fun onError(utteranceId: String?, errorCode: Int) {
                                Log.w(TTS_TAG, "TTS error code: $errorCode")
                                mainHandler.post {
                                    speakingMessageId = null
                                    errorMessage = appContext.getString(R.string.tts_error_speaking)
                                }
                            }
                        })
                        val pending = pendingUtterance
                        if (pending != null) {
                            pendingUtterance = null
                            speakInternal(localEngine, pending.first, pending.second)
                        }
                    } else {
                        isInitialized = false
                        initFailed = true
                        if (pendingUtterance != null) {
                            pendingUtterance = null
                            speakingMessageId = null
                            errorMessage = appContext.getString(R.string.tts_not_available)
                        }
                    }
                }
            }
            localEngine
        }
        engine = created.getOrNull()
        if (engine == null) {
            Log.w(TTS_TAG, "Unable to initialize TextToSpeech engine", created.exceptionOrNull())
            initFailed = true
        }
        ttsInstance = engine

        onDispose {
            isDisposed = true
            mainHandler.removeCallbacksAndMessages(null)
            pendingUtterance = null
            speakingMessageId = null
            runCatching {
                engine?.stop()
                engine?.shutdown()
            }
            ttsInstance = null
        }
    }

    fun stopSpeaking() {
        pendingUtterance = null
        speakingMessageId = null
        runCatching { ttsInstance?.stop() }
    }

    fun toggleSpeak(messageId: String, text: String) {
        if (speakingMessageId == messageId) {
            stopSpeaking()
            return
        }

        val engine = ttsInstance
        if (initFailed || engine == null) {
            speakingMessageId = null
            errorMessage = appContext.getString(R.string.tts_not_available)
            return
        }

        if (!isInitialized) {
            errorMessage = null
            pendingUtterance = messageId to text
            speakingMessageId = messageId
            return
        }

        speakInternal(engine, messageId, text)
    }

    return remember {
        TextToSpeechController(
            speakingMessageIdProvider = { speakingMessageId },
            errorMessageProvider = { errorMessage },
            onToggleSpeakAction = { id, text -> toggleSpeak(id, text) },
            onStopAction = { stopSpeaking() },
            onDismissErrorAction = { errorMessage = null }
        )
    }
}
