package com.example.data.model

import androidx.annotation.Keep
import com.google.firebase.Timestamp
import com.google.firebase.firestore.DocumentId
import com.google.firebase.firestore.IgnoreExtraProperties
import com.google.firebase.firestore.ServerTimestamp

sealed interface UiState<out T> {
    data object Loading : UiState<Nothing>
    data class Success<T>(val data: T) : UiState<T>
    data class Error(val message: String) : UiState<Nothing>
}

enum class ChatMode(val key: String, val label: String) {
    CHAT("chat", "محادثة عامة"),
    KB("kb", "من ذاكرتي"),
    SUMMARIZE("summarize", "تلخيص"),
    TRANSLATE("translate", "ترجمة");

    companion object {
        fun fromKey(raw: String?): ChatMode =
            entries.firstOrNull { it.key == raw } ?: CHAT
    }
}

@Keep
@IgnoreExtraProperties
data class KnowledgeSource(
    @DocumentId val id: String = "",
    val userId: String = "",
    val title: String = "",
    val sourceLabel: String = "",
    val sourceUrl: String? = null,
    val sourceType: String = "text",
    val summary: String = "",
    val content: String = "",
    val keywords: List<String> = emptyList(),
    val factCount: Long = 1L,
    @ServerTimestamp val createdAt: Timestamp? = null,
    @ServerTimestamp val updatedAt: Timestamp? = null
)

@Keep
@IgnoreExtraProperties
data class ChatMessage(
    @DocumentId val id: String = "",
    val userId: String = "",
    val role: String = "user",
    val text: String = "",
    val sourceTitle: String? = null,
    val mode: String? = null,
    @ServerTimestamp val createdAt: Timestamp? = null
)

@Keep
@IgnoreExtraProperties
data class CourseProgress(
    @DocumentId val id: String = "",
    val userId: String = "",
    val completedLessons: List<Long> = emptyList(),
    val quizBestScore: Long? = null,
    val quizTotal: Long? = null,
    @ServerTimestamp val createdAt: Timestamp? = null,
    @ServerTimestamp val updatedAt: Timestamp? = null
)

@Keep
@IgnoreExtraProperties
data class CustomCourseDocument(
    @DocumentId val id: String = "",
    val userId: String = "",
    val title: String = "",
    val description: String = "",
    val iconEmoji: String = "✨",
    val lessons: List<Map<String, Any?>> = emptyList(),
    val quiz: List<Map<String, Any?>> = emptyList(),
    @ServerTimestamp val createdAt: Timestamp? = null
) {
    fun toCourseUi(): CourseUi {
        val parsedLessons = lessons.mapNotNull { map ->
            val t = map["title"] as? String ?: return@mapNotNull null
            val b = map["body"] as? String ?: return@mapNotNull null
            LessonItem(title = t, body = b)
        }
        val parsedQuiz = quiz.mapNotNull { map ->
            val q = map["question"] as? String ?: return@mapNotNull null
            val opts = (map["options"] as? List<*>)?.mapNotNull { it as? String } ?: return@mapNotNull null
            if (opts.isEmpty()) return@mapNotNull null
            val correctIdx = ((map["correctIndex"] as? Number)?.toInt() ?: 0).coerceIn(0, opts.lastIndex)
            val exp = map["explanation"] as? String ?: ""
            QuizQuestion(question = q, options = opts, correctIndex = correctIdx, explanation = exp)
        }
        return CourseUi(
            id = id,
            iconEmoji = iconEmoji.ifBlank { "✨" },
            title = title,
            description = description,
            lessons = parsedLessons,
            quiz = parsedQuiz,
            isCustom = true
        )
    }
}

data class LessonItem(
    val title: String,
    val body: String
)

data class QuizQuestion(
    val question: String,
    val options: List<String>,
    val correctIndex: Int,
    val explanation: String
)

data class ChatContextQuiz(
    val topicTitle: String,
    val topicSummary: String,
    val questions: List<QuizQuestion>
)

sealed interface ChatQuizUiState {
    data object Hidden : ChatQuizUiState
    data class Generating(val streamingPreview: String? = null) : ChatQuizUiState
    data class Ready(
        val quiz: ChatContextQuiz,
        val currentIndex: Int = 0,
        val selectedOptions: Map<Int, Int> = emptyMap(),
        val showSummary: Boolean = false
    ) : ChatQuizUiState {
        val currentQuestion: QuizQuestion?
            get() = quiz.questions.getOrNull(currentIndex)
        val selectedOptionForCurrent: Int?
            get() = selectedOptions[currentIndex]
        val score: Int
            get() = quiz.questions.indices.count { idx ->
                selectedOptions[idx] == quiz.questions[idx].correctIndex
            }
        val answeredCount: Int
            get() = selectedOptions.size
    }
    data class Error(val message: String) : ChatQuizUiState
}

data class CourseUi(
    val id: String,
    val iconEmoji: String,
    val title: String,
    val description: String,
    val lessons: List<LessonItem>,
    val quiz: List<QuizQuestion>,
    val isCustom: Boolean = false
)

data class PresetSite(
    val id: String,
    val iconEmoji: String,
    val name: String,
    val url: String,
    val text: String
)
