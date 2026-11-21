package com.example.data.repository

import android.graphics.Bitmap
import android.util.Log
import com.example.data.model.ChatMessage
import com.example.data.model.ChatMode
import com.example.data.model.CourseUi
import com.example.data.model.KnowledgeSource
import com.example.data.model.LessonItem
import com.example.data.model.QuizQuestion
import com.google.firebase.Firebase
import com.google.firebase.ai.ai
import com.google.firebase.ai.type.AspectRatio
import com.google.firebase.ai.type.ImageConfig
import com.google.firebase.ai.type.ImagePart
import com.google.firebase.ai.type.ImageSize
import com.google.firebase.ai.type.QuotaExceededException
import com.google.firebase.ai.type.ResponseModality
import com.google.firebase.ai.type.Schema
import com.google.firebase.ai.type.content
import com.google.firebase.ai.type.generationConfig
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.TimeoutCancellationException
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.flow
import kotlinx.coroutines.flow.flowOn
import kotlinx.coroutines.withContext
import kotlinx.coroutines.withTimeout
import okhttp3.OkHttpClient
import okhttp3.Request
import org.json.JSONObject
import java.util.concurrent.TimeUnit

private const val TAG = "SanaAiRepository"

data class KnowledgeAnalysisResult(
    val title: String,
    val summary: String,
    val keywords: List<String>,
    val factCount: Int
)

data class ChatResponseResult(
    val text: String,
    val citedSourceTitle: String?
)

enum class StudioAspectRatio(val label: String, val ratioValue: Float) {
    SQUARE("1:1 مربع", 1f),
    LANDSCAPE("16:9 عريض", 16f / 9f),
    PORTRAIT("9:16 طولي", 9f / 16f),
    CLASSIC_4_3("4:3 كلاسيك", 4f / 3f)
}

data class VideoScenePlan(
    val sceneNumber: Int,
    val durationSeconds: Int,
    val cameraMotion: String,
    val visualPrompt: String,
    val narrationArabic: String
)

data class VideoStoryboardPlan(
    val title: String,
    val mood: String,
    val aspectRatio: StudioAspectRatio,
    val scenes: List<VideoScenePlan>
)

class SanaAiRepository {

    private val httpClient by lazy {
        OkHttpClient.Builder()
            .connectTimeout(15, TimeUnit.SECONDS)
            .readTimeout(20, TimeUnit.SECONDS)
            .followRedirects(true)
            .build()
    }

    private val chatSystemInstruction by lazy {
        content {
            text(
                """
                أنتِ «سنا»، مساعدة ذكية باللغة العربية الفصحى السلسة والدافئة.
                دورك مساعدة المستخدم في:
                1) المحادثة والإجابة بوضوح وإيجاز وأناقة.
                2) الإجابة اعتمادًا على «ذاكرة سنا» (المصادر التي تعلمتها من المستخدم) عندما تكون ذات صلة، مع ذكر اسم المصدر بوضوح.
                3) تلخيص النصوص في نقاط مركزة، أو الترجمة الطبيعية بين العربية والإنجليزية، أو اقتراح أفكار ومشاريع عملية.
                استخدمي تنسيق Markdown بسيط (عناوين قصيرة أو نقاط أو خط عريض) عند الحاجة لسهولة القراءة.
                لا تخترعي حقائق غير موجودة في المصادر عند الإجابة من الذاكرة؛ وإن لم تتوفر المعلومة في الذاكرة أجيبي من معرفتك العامة أو وضّحي ذلك بلطف.
                """.trimIndent()
            )
        }
    }

    private val chatConfig by lazy {
        generationConfig {
            temperature = 0.7f
            maxOutputTokens = 2048
        }
    }

    private val chatModel by lazy {
        Firebase.ai.generativeModel(
            modelName = "gemini-3.8-flash",
            generationConfig = chatConfig,
            systemInstruction = chatSystemInstruction
        )
    }

    private val fastChatModel by lazy {
        Firebase.ai.generativeModel(
            modelName = "gemini-3.5-flash-lite",
            generationConfig = chatConfig,
            systemInstruction = chatSystemInstruction
        )
    }

    private val knowledgeSchema by lazy {
        Schema.obj(
            properties = mapOf(
                "title" to Schema.string(description = "عنوان عربي واضح وموجز للمحتوى (٣ إلى ٨ كلمات)"),
                "summary" to Schema.string(description = "ملخص عربي مركز في جملتين إلى ثلاث جمل"),
                "keywords" to Schema.array(
                    items = Schema.string(),
                    description = "أهم 4 إلى 6 كلمات مفتاحية عربية تعبّر عن الموضوع"
                ),
                "factCount" to Schema.integer(description = "عدد الحقائق أو الأفكار الرئيسية المستخرجة (بين 1 و 50)")
            )
        )
    }

    private val knowledgeAnalyzerModel by lazy {
        Firebase.ai.generativeModel(
            modelName = "gemini-3.8-flash",
            generationConfig = generationConfig {
                responseMimeType = "application/json"
                responseSchema = knowledgeSchema
            }
        )
    }

    private val courseSchema by lazy {
        Schema.obj(
            properties = mapOf(
                "title" to Schema.string(description = "عنوان الدورة التدريبية بالعربية"),
                "description" to Schema.string(description = "وصف مختصر للدورة في سطر واحد"),
                "iconEmoji" to Schema.string(description = "رمز إيموجي واحد مناسب لموضوع الدورة"),
                "lessons" to Schema.array(
                    items = Schema.obj(
                        properties = mapOf(
                            "title" to Schema.string(description = "عنوان الدرس"),
                            "body" to Schema.string(description = "شرح الدرس بالعربية بأسلوب واضح ومنظم مع مثال عملي")
                        )
                    ),
                    description = "قائمة من 3 دروس تعليمية قصيرة ومتدرجة"
                ),
                "quiz" to Schema.array(
                    items = Schema.obj(
                        properties = mapOf(
                            "question" to Schema.string(description = "سؤال اختيار من متعدد بالعربية"),
                            "options" to Schema.array(
                                items = Schema.string(),
                                description = "أربعة خيارات للإجابة"
                            ),
                            "correctIndex" to Schema.integer(description = "رقم فهرس الإجابة الصحيحة من 0 إلى 3"),
                            "explanation" to Schema.string(description = "تفسير تعليمي موجز للإجابة الصحيحة")
                        )
                    ),
                    description = "قائمة من 3 أسئلة اختبارية تغطي الدروس"
                )
            )
        )
    }

    private val courseGeneratorModel by lazy {
        Firebase.ai.generativeModel(
            modelName = "gemini-3.8-flash",
            generationConfig = generationConfig {
                responseMimeType = "application/json"
                responseSchema = courseSchema
            }
        )
    }

    private val storyboardSchema by lazy {
        Schema.obj(
            properties = mapOf(
                "title" to Schema.string(description = "عنوان قصير وجذاب للفيديو بالعربية"),
                "mood" to Schema.string(description = "الطابع البصري والموسيقي المقترح للفيديو بالعربية"),
                "scenes" to Schema.array(
                    items = Schema.obj(
                        properties = mapOf(
                            "sceneNumber" to Schema.integer(description = "رقم المشهد بدءًا من 1"),
                            "durationSeconds" to Schema.integer(description = "مدة المشهد بالثواني بين 4 و 8"),
                            "cameraMotion" to Schema.string(description = "حركة الكاميرا المقترحة بالعربية مثل تقريب بطيء أو بانوراما"),
                            "visualPrompt" to Schema.string(description = "وصف بصري دقيق للمشهد لتوليد صورته بالذكاء الاصطناعي"),
                            "narrationArabic" to Schema.string(description = "نص التعليق الصوتي بالعربية الفصحى السلسة للمشهد")
                        )
                    ),
                    description = "قائمة من 3 إلى 4 مشاهد متسلسلة للفيديو"
                )
            )
        )
    }

    private val fastKnowledgeAnalyzerModel by lazy {
        Firebase.ai.generativeModel(
            modelName = "gemini-3.5-flash-lite",
            generationConfig = generationConfig {
                responseMimeType = "application/json"
                responseSchema = knowledgeSchema
            }
        )
    }

    private val videoStoryboardModel by lazy {
        Firebase.ai.generativeModel(
            modelName = "gemini-3.8-flash",
            generationConfig = generationConfig {
                responseMimeType = "application/json"
                responseSchema = storyboardSchema
            }
        )
    }

    private val fastVideoStoryboardModel by lazy {
        Firebase.ai.generativeModel(
            modelName = "gemini-3.5-flash-lite",
            generationConfig = generationConfig {
                responseMimeType = "application/json"
                responseSchema = storyboardSchema
            }
        )
    }

    private val fastCourseGeneratorModel by lazy {
        Firebase.ai.generativeModel(
            modelName = "gemini-3.5-flash-lite",
            generationConfig = generationConfig {
                responseMimeType = "application/json"
                responseSchema = courseSchema
            }
        )
    }

    private suspend fun generateWithFastFallback(
        primary: com.google.firebase.ai.GenerativeModel,
        fallback: com.google.firebase.ai.GenerativeModel,
        prompt: String,
        primaryTimeoutMs: Long = 35_000L,
        fallbackTimeoutMs: Long = 25_000L,
        onPartialJson: (suspend (String) -> Unit)? = null
    ): String {
        suspend fun collectStream(
            targetModel: com.google.firebase.ai.GenerativeModel,
            timeoutMs: Long
        ): String {
            val accumulated = StringBuilder()
            withTimeout(timeoutMs) {
                targetModel.generateContentStream(prompt).collect { chunk ->
                    val piece = chunk.text.orEmpty()
                    if (piece.isNotEmpty()) {
                        accumulated.append(piece)
                        onPartialJson?.invoke(accumulated.toString())
                    }
                }
            }
            val result = accumulated.toString().trim()
            if (result.isBlank()) {
                throw IllegalStateException("استجابة النموذج فارغة. حاول مرة أخرى.")
            }
            return result
        }

        return try {
            collectStream(primary, primaryTimeoutMs)
        } catch (e: TimeoutCancellationException) {
            Log.w(TAG, "Primary JSON model timed out; retrying once with gemini-3.5-flash-lite", e)
            collectStream(fallback, fallbackTimeoutMs)
        } catch (e: CancellationException) {
            throw e
        } catch (e: Exception) {
            if (isTransientOverloadError(e)) {
                Log.w(TAG, "Primary JSON model temporarily unavailable; retrying with gemini-3.5-flash-lite", e)
                collectStream(fallback, fallbackTimeoutMs)
            } else {
                throw e
            }
        }
    }

    private fun imageModelFor(aspectRatio: StudioAspectRatio) =
        Firebase.ai.generativeModel(
            modelName = "gemini-nano-banana-2.1",
            generationConfig = generationConfig {
                responseModalities = listOf(ResponseModality.IMAGE)
                imageConfig = ImageConfig.builder()
                    .setAspectRatio(
                        when (aspectRatio) {
                            StudioAspectRatio.SQUARE -> AspectRatio.SQUARE_1x1
                            StudioAspectRatio.LANDSCAPE -> AspectRatio.LANDSCAPE_16x9
                            StudioAspectRatio.PORTRAIT -> AspectRatio.PORTRAIT_9x16
                            StudioAspectRatio.CLASSIC_4_3 -> AspectRatio.LANDSCAPE_4x3
                        }
                    )
                    .setImageSize(ImageSize.SIZE_1K)
                    .build()
            }
        )

    fun streamReply(
        userMessage: String,
        mode: ChatMode,
        recentHistory: List<ChatMessage>,
        knowledgeBase: List<KnowledgeSource>,
        preferredName: String? = null
    ): Flow<Pair<String, String?>> = flow {
        val relevantSources = findRelevantSources(userMessage, knowledgeBase)
        val citedTitle = relevantSources.firstOrNull()?.title

        val kbContext = if (relevantSources.isNotEmpty()) {
            buildString {
                appendLine("=== مقتطفات من ذاكرة سنا (مصادر تعلّمتها المستخدم) ===")
                relevantSources.forEach { src ->
                    appendLine("- المصدر: ${src.title} (${src.sourceLabel})")
                    appendLine("  الملخص: ${src.summary}")
                    appendLine("  المحتوى: ${src.content.take(1800)}")
                }
                appendLine("====================================================")
            }
        } else {
            ""
        }

        val modeDirective = when (mode) {
            ChatMode.CHAT -> "أجيبي بأسلوب سنا الذكي والمباشر."
            ChatMode.KB -> if (relevantSources.isNotEmpty()) {
                "اعتمدي في إجابتك أولًا على مقتطفات ذاكرة سنا المرفقة أعلاه واذكري اسم المصدر."
            } else {
                "ابحثي في السياق؛ إذا لم تكن هناك مصادر محفوظة ذات صلة، نبّهي المستخدم بلطف لإضافة مصدر في تبويب «تعلّم ذاتي» ثم قدّمي مساعدة مبدئية."
            }
            ChatMode.SUMMARIZE -> "لخّصي النص أو الموضوع التالي في نقاط عربية واضحة ومرتبة مع خلاصة سريعة في النهاية."
            ChatMode.TRANSLATE -> "ترجمي النص التالي بين العربية والإنجليزية بأسلوب طبيعي وسلس يحافظ على المعنى الدقيق."
        }

        val chatHistory = recentHistory
            .filter { it.text.isNotBlank() }
            .takeLast(8)
            .map { msg ->
                content(role = if (msg.role == "user") "user" else "model") {
                    text(msg.text.take(600))
                }
            }

        val currentTurnPrompt = buildString {
            val cleanPreferredName = preferredName?.trim().orEmpty()
            if (cleanPreferredName.isNotEmpty()) {
                appendLine("الاسم المفضّل لمخاطبة المستخدم: $cleanPreferredName")
            }
            if (kbContext.isNotBlank()) {
                appendLine(kbContext)
            }
            appendLine("التوجيه: $modeDirective")
            append("رسالة المستخدم: $userMessage")
        }

        val accumulated = StringBuilder()
        suspend fun collectFromModel(
            targetModel: com.google.firebase.ai.GenerativeModel,
            timeoutMs: Long
        ) {
            accumulated.clear()
            val chatSession = targetModel.startChat(history = chatHistory)
            withTimeout(timeoutMs) {
                chatSession.sendMessageStream(currentTurnPrompt).collect { chunk ->
                    val piece = chunk.text.orEmpty()
                    if (piece.isNotEmpty()) {
                        accumulated.append(piece)
                        emit(accumulated.toString() to citedTitle)
                    }
                }
            }
        }

        try {
            try {
                collectFromModel(chatModel, 35_000L)
            } catch (e: TimeoutCancellationException) {
                if (accumulated.isNotBlank()) throw e
                Log.w(TAG, "Primary model timed out; retrying once with gemini-3.5-flash-lite", e)
                collectFromModel(fastChatModel, 25_000L)
            } catch (e: CancellationException) {
                throw e
            } catch (e: Exception) {
                if (accumulated.isBlank() && isTransientOverloadError(e)) {
                    Log.w(TAG, "Primary model temporarily unavailable; retrying with gemini-3.5-flash-lite", e)
                    collectFromModel(fastChatModel, 25_000L)
                } else {
                    throw e
                }
            }

            if (accumulated.isBlank()) {
                throw IllegalStateException("لم يُرجِع النموذج نصًا. حاول مرة أخرى.")
            }
        } catch (e: TimeoutCancellationException) {
            Log.e(TAG, "Streaming reply timed out after fallback", e)
            throw IllegalStateException(formatAiErrorMessage(e), e)
        } catch (e: CancellationException) {
            throw e
        } catch (e: Exception) {
            Log.e(TAG, "Streaming reply failed", e)
            throw IllegalStateException(formatAiErrorMessage(e), e)
        }
    }.flowOn(Dispatchers.IO)

    suspend fun analyzeKnowledgeContent(
        rawText: String,
        hintTitle: String? = null,
        onStreamingProgress: (suspend (String) -> Unit)? = null
    ): KnowledgeAnalysisResult = withContext(Dispatchers.IO) {
        val clipped = rawText.trim().take(10000)
        val prompt = buildString {
            appendLine("حلّلي النص التالي واستخرجي منه عنوانًا عربيًا موجزًا، وملخصًا مركزًا، وأهم الكلمات المفتاحية، وعدد الحقائق الرئيسية.")
            if (!hintTitle.isNullOrBlank()) {
                appendLine("تلميح اسم المصدر: $hintTitle")
            }
            appendLine("النص:")
            appendLine(clipped)
        }
        try {
            val jsonStr = generateWithFastFallback(
                primary = knowledgeAnalyzerModel,
                fallback = fastKnowledgeAnalyzerModel,
                prompt = prompt,
                onPartialJson = onStreamingProgress?.let { callback ->
                    { raw ->
                        val preview = extractPartialStringField(raw, "summary")
                            ?: extractPartialStringField(raw, "title")
                            ?: "جارٍ استخلاص الأفكار (${raw.length} حرف)…"
                        callback(preview)
                    }
                }
            )
            val json = JSONObject(jsonStr)
            val title = json.optString("title").ifBlank { hintTitle ?: "مصدر معرفي" }
            val summary = json.optString("summary").ifBlank { clipped.take(220) }
            val kwArray = json.optJSONArray("keywords")
            val keywords = buildList {
                if (kwArray != null) {
                    for (i in 0 until kwArray.length()) {
                        val kw = kwArray.optString(i).trim()
                        if (kw.isNotBlank()) add(kw)
                    }
                }
            }.take(6)
            val factCount = json.optInt("factCount", 3).coerceIn(1, 50)
            KnowledgeAnalysisResult(
                title = title,
                summary = summary,
                keywords = keywords,
                factCount = factCount
            )
        } catch (e: TimeoutCancellationException) {
            Log.e(TAG, "Knowledge analysis timed out", e)
            throw IllegalStateException("انتهت مهلة تحليل النص. تحقّق من الاتصال وأعد المحاولة.", e)
        } catch (e: CancellationException) {
            throw e
        } catch (e: Exception) {
            Log.e(TAG, "Knowledge analysis failed", e)
            throw IllegalStateException(formatAiErrorMessage(e), e)
        }
    }

    suspend fun generateCourse(
        topic: String,
        onStreamingProgress: (suspend (String) -> Unit)? = null
    ): CourseUi = withContext(Dispatchers.IO) {
        val prompt = """
            أنشئي دورة تعليمية تفاعلية قصيرة وممتعة باللغة العربية حول الموضوع التالي: «$topic».
            الشروط:
            - 3 دروس مركّزة ومرتبة (كل درس بعنوان واضح وشرح عملي بسيط في فقرتين أو ثلاث).
            - 3 أسئلة اختيار من متعدد تغطي ما ورد في الدروس، ولكل سؤال 4 خيارات وفهرس إجابة صحيحة (0..3) وتفسير موجز.
        """.trimIndent()

        try {
            val jsonStr = generateWithFastFallback(
                primary = courseGeneratorModel,
                fallback = fastCourseGeneratorModel,
                prompt = prompt,
                onPartialJson = onStreamingProgress?.let { callback ->
                    { raw ->
                        val preview = extractPartialStringField(raw, "description")
                            ?: extractPartialStringField(raw, "title")
                            ?: "جارٍ تأليف الدروس والاختبار…"
                        callback(preview)
                    }
                }
            )
            val json = JSONObject(jsonStr)
            val title = json.optString("title").ifBlank { topic }
            val description = json.optString("description").ifBlank { "دورة تفاعلية مولّدة في $topic" }
            val iconEmoji = json.optString("iconEmoji").ifBlank { "✨" }

            val lessonsJson = json.optJSONArray("lessons")
            val lessons = buildList {
                if (lessonsJson != null) {
                    for (i in 0 until lessonsJson.length()) {
                        val obj = lessonsJson.optJSONObject(i) ?: continue
                        val lt = obj.optString("title").trim()
                        val lb = obj.optString("body").trim()
                        if (lt.isNotBlank() && lb.isNotBlank()) {
                            add(LessonItem(title = lt, body = lb))
                        }
                    }
                }
            }

            val quizJson = json.optJSONArray("quiz")
            val quiz = buildList {
                if (quizJson != null) {
                    for (i in 0 until quizJson.length()) {
                        val obj = quizJson.optJSONObject(i) ?: continue
                        val qText = obj.optString("question").trim()
                        val optsArr = obj.optJSONArray("options")
                        val opts = buildList {
                            if (optsArr != null) {
                                for (j in 0 until optsArr.length()) {
                                    val opt = optsArr.optString(j).trim()
                                    if (opt.isNotBlank()) add(opt)
                                }
                            }
                        }
                        if (qText.isNotBlank() && opts.size >= 2) {
                            val idx = obj.optInt("correctIndex", 0).coerceIn(0, opts.lastIndex)
                            val exp = obj.optString("explanation").trim()
                            add(QuizQuestion(qText, opts, idx, exp))
                        }
                    }
                }
            }

            if (lessons.isEmpty() || quiz.isEmpty()) {
                throw IllegalStateException("تعذر بناء الدروس أو الأسئلة بشكل مكتمل. جرّب صياغة موضوع أوضح.")
            }

            CourseUi(
                id = "",
                iconEmoji = iconEmoji,
                title = title,
                description = description,
                lessons = lessons,
                quiz = quiz,
                isCustom = true
            )
        } catch (e: TimeoutCancellationException) {
            Log.e(TAG, "Course generation timed out", e)
            throw IllegalStateException("انتهت مهلة إنشاء الدورة. تحقّق من الاتصال ثم حاول مرة أخرى.", e)
        } catch (e: CancellationException) {
            throw e
        } catch (e: Exception) {
            Log.e(TAG, "Course generation failed", e)
            throw IllegalStateException(formatAiErrorMessage(e), e)
        }
    }

    suspend fun generateImage(
        prompt: String,
        aspectRatio: StudioAspectRatio = StudioAspectRatio.SQUARE,
        referenceBitmap: Bitmap? = null
    ): Bitmap = withContext(Dispatchers.IO) {
        val cleanPrompt = prompt.trim()
        if (cleanPrompt.isBlank()) {
            throw IllegalArgumentException("أدخل وصفًا واضحًا للصورة أولًا.")
        }
        val imageModel = imageModelFor(aspectRatio)
        try {
            val response = withTimeout(60_000L) {
                if (referenceBitmap != null) {
                    imageModel.generateContent(
                        content {
                            image(referenceBitmap)
                            text(cleanPrompt)
                        }
                    )
                } else {
                    imageModel.generateContent(cleanPrompt)
                }
            }
            val bitmap = response.candidates.firstOrNull()
                ?.content?.parts?.filterIsInstance<ImagePart>()
                ?.firstOrNull()
                ?.image
            bitmap ?: throw IllegalStateException("لم يُرجِع النموذج صورة لهذا الوصف. جرّب صياغة أخرى.")
        } catch (e: TimeoutCancellationException) {
            Log.e(TAG, "Image generation timed out", e)
            throw IllegalStateException("انتهت مهلة توليد الصورة (60 ثانية). حاول مرة أخرى.", e)
        } catch (e: QuotaExceededException) {
            Log.e(TAG, "Image generation quota exceeded", e)
            throw IllegalStateException("تم تجاوز حد الاستخدام أو يتطلب المشروع تفعيل خطة Blaze لتوليد الصور.", e)
        } catch (e: CancellationException) {
            throw e
        } catch (e: Exception) {
            Log.e(TAG, "Image generation failed", e)
            throw IllegalStateException(formatAiErrorMessage(e), e)
        }
    }

    suspend fun generateVideoStoryboard(
        topic: String,
        aspectRatio: StudioAspectRatio = StudioAspectRatio.LANDSCAPE,
        onStreamingProgress: (suspend (String) -> Unit)? = null
    ): VideoStoryboardPlan = withContext(Dispatchers.IO) {
        val cleanTopic = topic.trim()
        if (cleanTopic.isBlank()) {
            throw IllegalArgumentException("أدخل فكرة الفيديو أولًا.")
        }
        val prompt = """
            أنتِ مخرجة ومصممة فيديوهات قصيرة احترافية.
            صمّمي مخطط فيديو إبداعي متكامل باللغة العربية حول الفكرة التالية: «$cleanTopic».
            اجعلي كل مشهد يتضمن وصفًا بصريًا دقيقًا يصلح لتوليد لقطة فنية، وحركة كاميرا واضحة، وتعليقًا صوتيًا عربيًا جذّابًا ومختصرًا يناسب القراءة الصوتية.
        """.trimIndent()

        try {
            val jsonStr = generateWithFastFallback(
                primary = videoStoryboardModel,
                fallback = fastVideoStoryboardModel,
                prompt = prompt,
                onPartialJson = onStreamingProgress?.let { callback ->
                    { raw ->
                        val preview = extractPartialStringField(raw, "mood")
                            ?: extractPartialStringField(raw, "title")
                            ?: "جارٍ كتابة مشاهد الفيديو…"
                        callback(preview)
                    }
                }
            )
            val json = JSONObject(jsonStr)
            val title = json.optString("title").ifBlank { cleanTopic }
            val mood = json.optString("mood").ifBlank { "بصري حديث وإلهامي" }
            val scenesArr = json.optJSONArray("scenes")
            val scenes = buildList {
                if (scenesArr != null) {
                    for (i in 0 until scenesArr.length()) {
                        val obj = scenesArr.optJSONObject(i) ?: continue
                        val visual = obj.optString("visualPrompt").trim()
                        val narration = obj.optString("narrationArabic").trim()
                        if (visual.isNotBlank() && narration.isNotBlank()) {
                            add(
                                VideoScenePlan(
                                    sceneNumber = obj.optInt("sceneNumber", i + 1),
                                    durationSeconds = obj.optInt("durationSeconds", 5).coerceIn(3, 10),
                                    cameraMotion = obj.optString("cameraMotion").ifBlank { "تقريب سينمائي ناعم" },
                                    visualPrompt = visual,
                                    narrationArabic = narration
                                )
                            )
                        }
                    }
                }
            }
            if (scenes.isEmpty()) {
                throw IllegalStateException("تعذر استخراج مشاهد الفيديو. جرّب وصفًا أوضح للفكرة.")
            }
            VideoStoryboardPlan(
                title = title,
                mood = mood,
                aspectRatio = aspectRatio,
                scenes = scenes
            )
        } catch (e: TimeoutCancellationException) {
            Log.e(TAG, "Video storyboard generation timed out", e)
            throw IllegalStateException("انتهت مهلة تصميم مشاهد الفيديو. تحقّق من الاتصال ثم أعد المحاولة.", e)
        } catch (e: CancellationException) {
            throw e
        } catch (e: Exception) {
            Log.e(TAG, "Video storyboard generation failed", e)
            throw IllegalStateException(formatAiErrorMessage(e), e)
        }
    }

    suspend fun fetchWebPageText(rawUrl: String): Pair<String, String> = withContext(Dispatchers.IO) {
        val normalizedUrl = if (rawUrl.startsWith("http://") || rawUrl.startsWith("https://")) {
            rawUrl.trim()
        } else {
            "https://${rawUrl.trim()}"
        }
        val request = Request.Builder()
            .url(normalizedUrl)
            .header("User-Agent", "Mozilla/5.0 (Linux; Android 14) SanaAI/1.0")
            .build()

        val html = withTimeout(30_000L) {
            httpClient.newCall(request).execute().use { response ->
                if (!response.isSuccessful) {
                    throw IllegalStateException("تعذر فتح الرابط (رمز الاستجابة ${response.code}).")
                }
                response.body?.string() ?: throw IllegalStateException("الصفحة فارغة.")
            }
        }

        val pageTitle = Regex("(?is)<title[^>]*>(.*?)</title>")
            .find(html)?.groupValues?.getOrNull(1)
            ?.replace(Regex("\\s+"), " ")
            ?.trim()
            ?: normalizedUrl

        val stripped = html
            .replace(Regex("(?is)<script[^>]*>.*?</script>"), " ")
            .replace(Regex("(?is)<style[^>]*>.*?</style>"), " ")
            .replace(Regex("(?is)<noscript[^>]*>.*?</noscript>"), " ")
            .replace(Regex("(?is)<nav[^>]*>.*?</nav>"), " ")
            .replace(Regex("(?is)<footer[^>]*>.*?</footer>"), " ")
            .replace(Regex("(?is)<[^>]+>"), " ")
            .replace("&nbsp;", " ")
            .replace("&amp;", "&")
            .replace("&lt;", "<")
            .replace("&gt;", ">")
            .replace("&quot;", "\"")
            .replace(Regex("\\s+"), " ")
            .trim()

        if (stripped.length < 40) {
            throw IllegalStateException("لم يُستخرج نص كافٍ من هذه الصفحة (قد تعتمد على JavaScript فقط). جرّب لصق النص مباشرة.")
        }

        pageTitle to stripped.take(12000)
    }

    private fun findRelevantSources(
        query: String,
        sources: List<KnowledgeSource>
    ): List<KnowledgeSource> {
        if (sources.isEmpty()) return emptyList()
        val queryTokens = tokenizeArabic(query)
        if (queryTokens.isEmpty()) return sources.take(2)

        val scored = sources.map { src ->
            val docText = "${src.title} ${src.keywords.joinToString(" ")} ${src.summary} ${src.content}"
            val docTokens = tokenizeArabic(docText).toSet()
            val overlap = queryTokens.count { it in docTokens }
            src to overlap
        }.filter { it.second > 0 }
            .sortedByDescending { it.second }
            .map { it.first }

        return if (scored.isNotEmpty()) scored.take(2) else emptyList()
    }

    private fun tokenizeArabic(text: String): List<String> {
        return text
            .replace(Regex("[أإآ]"), "ا")
            .replace('ة', 'ه')
            .replace('ى', 'ي')
            .split(Regex("[\\s،.,؛:!؟?\"'()\\-—/]+"))
            .map { it.trim().lowercase() }
            .filter { it.length > 2 }
    }
}

internal fun extractPartialStringField(partialJson: String, fieldName: String): String? {
    val keyPattern = Regex("\"${Regex.escape(fieldName)}\"\\s*:\\s*\"([^\"]{2,})")
    val raw = keyPattern.find(partialJson)?.groupValues?.getOrNull(1) ?: return null
    return raw
        .replace("\\n", " ")
        .replace("\\\"", "\"")
        .trim()
        .takeIf { it.length >= 2 }
}

internal fun isTransientOverloadError(error: Throwable): Boolean {
    val msg = generateSequence(error) { it.cause }
        .mapNotNull { it.message }
        .joinToString(" ")
        .lowercase()
    return msg.contains("503") ||
        msg.contains("500") ||
        msg.contains("unavailable") ||
        msg.contains("overloaded") ||
        msg.contains("deadline") ||
        msg.contains("timeout")
}

internal fun formatAiErrorMessage(error: Throwable): String {
    if (error is TimeoutCancellationException) {
        return "استغرق الاتصال بالذكاء الاصطناعي وقتًا طويلًا وانتهت المهلة. تأكد من استقرار الإنترنت ثم اضغط «إعادة المحاولة»."
    }
    if (error is QuotaExceededException) {
        return "تم تجاوز الحد المسموح حاليًا لطلبات الذكاء الاصطناعي. انتظر قليلًا ثم أعد المحاولة."
    }
    val fullMsg = generateSequence(error) { it.cause }
        .mapNotNull { it.message }
        .joinToString(" | ")
    val lower = fullMsg.lowercase()
    val localized = error.localizedMessage?.takeIf { it.isNotBlank() }
    return when {
        lower.contains("app check") || lower.contains("appcheck") || lower.contains("attestation") || lower.contains("403") ->
            "فشل التحقق الأمني (App Check 403). تأكد من تشغيل النسخة الرسمية المهيأة داخل AI Studio أو إعادة التهيئة."
        lower.contains("quota") || lower.contains("429") || lower.contains("resource_exhausted") ->
            "تم تجاوز حد الاستخدام المسموح للذكاء الاصطناعي حاليًا. حاول بعد قليل."
        lower.contains("unable to resolve host") ||
            lower.contains("unknownhost") ||
            lower.contains("failed to connect") ||
            lower.contains("network") ||
            lower.contains("socket") ->
            "تعذر الوصول إلى خوادم الذكاء الاصطناعي. تحقّق من الاتصال بالإنترنت أو جرّب إيقاف VPN مؤقتًا ثم أعد المحاولة."
        error is org.json.JSONException || lower.contains("json") ->
            "وصلت استجابة غير مكتملة من النموذج. اضغط «إعادة المحاولة»."
        localized != null ->
            localized
        else ->
            "تعذر إتمام طلب الذكاء الاصطناعي حاليًا. اضغط «إعادة المحاولة»."
    }
}
