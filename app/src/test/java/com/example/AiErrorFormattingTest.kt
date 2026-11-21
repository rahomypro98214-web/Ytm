package com.example

import com.example.data.repository.extractPartialStringField
import com.example.data.repository.formatAiErrorMessage
import com.example.data.repository.isTransientOverloadError
import kotlinx.coroutines.delay
import kotlinx.coroutines.runBlocking
import kotlinx.coroutines.withTimeout
import org.json.JSONException
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test
import java.net.UnknownHostException

class AiErrorFormattingTest {

    @Test
    fun detectsTransientOverloadAndTimeoutErrors() {
        assertTrue(isTransientOverloadError(RuntimeException("503 Service Unavailable")))
        assertTrue(isTransientOverloadError(RuntimeException("Model is currently overloaded")))
        assertTrue(isTransientOverloadError(RuntimeException("DEADLINE_EXCEEDED timeout")))
        assertFalse(isTransientOverloadError(RuntimeException("Invalid argument")))
    }

    @Test
    fun formatsAppCheckNetworkTimeoutAndJsonErrorsInArabic() {
        val appCheckMsg = formatAiErrorMessage(
            RuntimeException("HTTP 403 App Check token is invalid")
        )
        assertTrue(appCheckMsg.contains("App Check"))

        val networkMsg = formatAiErrorMessage(
            UnknownHostException("Unable to resolve host firebasevertexai.googleapis.com")
        )
        assertTrue(networkMsg.contains("VPN"))

        val quotaMsg = formatAiErrorMessage(
            RuntimeException("HTTP 429 RESOURCE_EXHAUSTED quota exceeded")
        )
        assertTrue(quotaMsg.contains("حد الاستخدام"))

        val jsonMsg = formatAiErrorMessage(JSONException("Unterminated string"))
        assertTrue(jsonMsg.contains("إعادة المحاولة"))

        val timeoutError = runCatching {
            runBlocking {
                withTimeout(1L) {
                    delay(100L)
                }
            }
        }.exceptionOrNull()!!
        val timeoutMsg = formatAiErrorMessage(timeoutError)
        assertTrue(timeoutMsg.contains("المهلة"))
    }

    @Test
    fun extractsPartialStringFieldFromStreamingJsonChunks() {
        val partialChunk = """{"title": "مقدمة في الخط العربي\nالجزء الأول", "mood": "ملهم"""
        assertEquals("مقدمة في الخط العربي الجزء الأول", extractPartialStringField(partialChunk, "title"))
        assertEquals("ملهم", extractPartialStringField(partialChunk, "mood"))
        assertNull(extractPartialStringField("""{"title": "أ"}""", "title"))
    }
}

