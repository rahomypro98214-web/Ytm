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

    private val chatModel by lazy {
        Firebase.ai.generativeModel(
            modelName = "gemini-3.8-flash",
            systemInstruction = content {
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

    private val videoStoryboardModel by lazy {
        Firebase.ai.generativeModel(
            modelName = "gemini-3.8-flash",
            generationConfig = generationConfig {
                responseMimeType = "application/json"
                responseSchema = storyboardSchema
            }
        )
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

        val historySnippet = recentHistory.takeLast(8).joinToString("\n") { msg ->
            val speaker = if (msg.role == "user") "المستخدم" else "سنا"
            "$speaker: ${msg.text.take(600)}"
        }

        val fullPrompt = buildString {
            val cleanPreferredName = preferredName?.trim().orEmpty()
            if (cleanPreferredName.isNotEmpty()) {
                appendLine("الاسم المفضّل لمخاطبة المستخدم: $cleanPreferredName")
                appendLine()
            }
            if (kbContext.isNotBlank()) {
                appendLine(kbContext)
                appendLine()
            }
            if (historySnippet.isNotBlank()) {
                appendLine("سجل المحادثة القريب:")
                appendLine(historySnippet)
                appendLine()
            }
            appendLine("التوجيه: $modeDirective")
            appendLine("رسالة المستخدم الحالية: $userMessage")
        }

        val accumulated = StringBuilder()
        try {
            withTimeout(60_000L) {
                chatModel.generateContentStream(fullPrompt).collect { chunk ->
                    val piece = chunk.text.orEmpty()
                    if (piece.isNotEmpty()) {
                        accumulated.append(piece)
                        emit(accumulated.toString() to citedTitle)
                    }
                }
            }
            if (accumulated.isBlank()) {
                throw IllegalStateException("لم يُرجِع النموذج نصًا. حاول مرة أخرى.")
            }
        } catch (e: TimeoutCancellationException) {
            Log.e(TAG, "Streaming reply timed out", e)
            throw IllegalStateException("انتهت مهلة الاتصال بالنموذج (60 ثانية). حاول مجددًا.")
        } catch (e: CancellationException) {
            throw e
        } catch (e: Exception) {
            Log.e(TAG, "Streaming reply failed", e)
            throw e
        }
    }

    suspend fun analyzeKnowledgeContent(
        rawText: String,
        hintTitle: String? = null
    ): KnowledgeAnalysisResult {
        val clipped = rawText.trim().take(10000)
        val prompt = buildString {
            appendLine("حلّلي النص التالي واستخرجي منه عنوانًا عربيًا موجزًا، وملخصًا مركزًا، وأهم الكلمات المفتاحية، وعدد الحقائق الرئيسية.")
            if (!hintTitle.isNullOrBlank()) {
                appendLine("تلميح اسم المصدر: $hintTitle")
            }
            appendLine("النص:")
            appendLine(clipped)
        }
        return try {
            val response = withTimeout(60_000L) {
                knowledgeAnalyzerModel.generateContent(prompt)
            }
            val jsonStr = response.text ?: throw IllegalStateException("استجابة التحليل فارغة.")
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
            throw IllegalStateException("انتهت مهلة تحليل النص.")
        } catch (e: CancellationException) {
            throw e
        } catch (e: Exception) {
            Log.e(TAG, "Knowledge analysis failed", e)
            throw e
        }
    }

    suspend fun generateCourse(topic: String): CourseUi {
        val prompt = """
            أنشئي دورة تعليمية تفاعلية قصيرة وممتعة باللغة العربية حول الموضوع التالي: «$topic».
            الشروط:
            - 3 دروس مركّزة ومرتبة (كل درس بعنوان واضح وشرح عملي بسيط في فقرتين أو ثلاث).
            - 3 أسئلة اختيار من متعدد تغطي ما ورد في الدروس، ولكل سؤال 4 خيارات وفهرس إجابة صحيحة (0..3) وتفسير موجز.
        """.trimIndent()

        return try {
            val response = withTimeout(60_000L) {
                courseGeneratorModel.generateContent(prompt)
            }
            val jsonStr = response.text ?: throw IllegalStateException("لم يُرجِع النموذج بيانات الدورة.")
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
            throw IllegalStateException("انتهت مهلة إنشاء الدورة (60 ثانية).")
        } catch (e: CancellationException) {
            throw e
        } catch (e: Exception) {
            Log.e(TAG, "Course generation failed", e)
            throw e
        }
    }

    suspend fun generateImage(
        prompt: String,
        aspectRatio: StudioAspectRatio = StudioAspectRatio.SQUARE,
        referenceBitmap: Bitmap? = null
    ): Bitmap {
        val cleanPrompt = prompt.trim()
        if (cleanPrompt.isBlank()) {
            throw IllegalArgumentException("أدخل وصفًا واضحًا للصورة أولًا.")
        }
        val imageModel = imageModelFor(aspectRatio)
        return try {
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
            throw IllegalStateException("انتهت مهلة توليد الصورة (60 ثانية). حاول مرة أخرى.")
        } catch (e: QuotaExceededException) {
            Log.e(TAG, "Image generation quota exceeded", e)
            throw IllegalStateException("تم تجاوز حد الاستخدام أو يتطلب المشروع تفعيل خطة Blaze لتوليد الصور.")
        } catch (e: CancellationException) {
            throw e
        } catch (e: Exception) {
            Log.e(TAG, "Image generation failed", e)
            throw e
        }
    }

    suspend fun generateVideoStoryboard(
        topic: String,
        aspectRatio: StudioAspectRatio = StudioAspectRatio.LANDSCAPE
    ): VideoStoryboardPlan {
        val cleanTopic = topic.trim()
        if (cleanTopic.isBlank()) {
            throw IllegalArgumentException("أدخل فكرة الفيديو أولًا.")
        }
        val prompt = """
            أنتِ مخرجة ومصممة فيديوهات قصيرة احترافية.
            صمّمي مخطط فيديو إبداعي متكامل باللغة العربية حول الفكرة التالية: «$cleanTopic».
            اجعلي كل مشهد يتضمن وصفًا بصريًا دقيقًا يصلح لتوليد لقطة فنية، وحركة كاميرا واضحة، وتعليقًا صوتيًا عربيًا جذّابًا ومختصرًا يناسب القراءة الصوتية.
        """.trimIndent()

        return try {
            val response = withTimeout(60_000L) {
                videoStoryboardModel.generateContent(prompt)
            }
            val jsonStr = response.text
                ?: throw IllegalStateException("لم يُرجِع النموذج مخطط الفيديو.")
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
            throw IllegalStateException("انتهت مهلة تصميم مشاهد الفيديو (60 ثانية).")
        } catch (e: CancellationException) {
            throw e
        } catch (e: Exception) {
            Log.e(TAG, "Video storyboard generation failed", e)
            throw e
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
