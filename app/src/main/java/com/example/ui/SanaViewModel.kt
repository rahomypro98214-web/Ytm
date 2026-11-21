package com.example.ui

import android.graphics.Bitmap
import android.util.Log
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.data.local.ChatConversationEntity
import com.example.data.local.ChatHistorySearchMatch
import com.example.data.local.ChatMessageEntity
import com.example.data.local.DEFAULT_CONVERSATION_ID
import com.example.data.model.ChatMessage
import com.example.data.model.ChatMode
import com.example.data.model.CourseProgress
import com.example.data.model.CourseUi
import com.example.data.model.KnowledgeSource
import com.example.data.model.PresetSite
import com.example.data.model.StarterContent
import com.example.data.model.UiState
import com.example.data.repository.LocalChatRepository
import com.example.data.repository.SanaAiRepository
import com.example.data.repository.SanaFirestoreRepository
import com.example.data.repository.StudioAspectRatio
import com.example.data.repository.UserPreferencesRepository
import com.example.data.repository.VideoStoryboardPlan
import com.example.data.repository.formatAiErrorMessage
import com.google.firebase.Timestamp
import com.google.firebase.firestore.FirebaseFirestoreException
import java.util.Date
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.Job
import kotlinx.coroutines.TimeoutCancellationException
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.catch
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.flatMapLatest
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch

private const val TAG = "SanaViewModel"
private const val STOP_TIMEOUT_MS = 5000L

enum class SanaTab(val label: String) {
    CHAT("محادثة"),
    STUDIO("إبداع"),
    LEARN("تعلَّم"),
    SELF_LEARN("تعلّم ذاتي")
}

data class GeneratedImageResult(
    val prompt: String,
    val aspectRatio: StudioAspectRatio,
    val bitmap: Bitmap
)

enum class LearnSubView {
    LIST, LESSON, QUIZ, RESULT
}

data class LearnUiSession(
    val subView: LearnSubView = LearnSubView.LIST,
    val activeCourseId: String? = null,
    val lessonIndex: Int = 0,
    val quizIndex: Int = 0,
    val quizScore: Int = 0,
    val selectedOption: Int? = null
)

data class SelfLearnProgress(
    val activeStep: Int = 0,
    val sourceName: String = "",
    val steps: List<String> = emptyList()
)

class SanaViewModel(
    private val firestoreRepo: SanaFirestoreRepository,
    private val aiRepo: SanaAiRepository,
    private val localChatRepo: LocalChatRepository,
    private val currentUserId: String,
    private val userPreferencesRepo: UserPreferencesRepository? = null
) : ViewModel() {

    val preferredName: StateFlow<String?> =
        userPreferencesRepo?.preferredName
            ?.stateIn(viewModelScope, SharingStarted.WhileSubscribed(STOP_TIMEOUT_MS), null)
            ?: MutableStateFlow<String?>(null).asStateFlow()

    val knowledgeSourcesState: StateFlow<UiState<List<KnowledgeSource>>> =
        firestoreRepo.observeKnowledgeSources(currentUserId)
            .map<List<KnowledgeSource>, UiState<List<KnowledgeSource>>> { UiState.Success(it) }
            .catch { error ->
                val firestoreError = generateSequence(error) { it.cause }
                    .filterIsInstance<FirebaseFirestoreException>()
                    .firstOrNull()
                if (firestoreError == null && error is CancellationException) throw error
                val rootError = firestoreError ?: error
                Log.w(TAG, "Error observing knowledgeSources", rootError)
                emit(UiState.Error(rootError.localizedMessage ?: "تعذر تحميل مصادر المعرفة."))
            }
            .stateIn(viewModelScope, SharingStarted.WhileSubscribed(STOP_TIMEOUT_MS), UiState.Loading)

    private val _activeConversationId = MutableStateFlow(DEFAULT_CONVERSATION_ID)
    val activeConversationId: StateFlow<String> = _activeConversationId.asStateFlow()

    val savedConversations: StateFlow<List<ChatConversationEntity>> =
        localChatRepo.observeConversations(currentUserId)
            .catch { error ->
                if (error is CancellationException) throw error
                Log.w(TAG, "Error observing saved conversations", error)
                emit(emptyList())
            }
            .stateIn(viewModelScope, SharingStarted.WhileSubscribed(STOP_TIMEOUT_MS), emptyList())

    private val _historySearchQuery = MutableStateFlow("")
    val historySearchQuery: StateFlow<String> = _historySearchQuery.asStateFlow()

    @OptIn(ExperimentalCoroutinesApi::class)
    val historySearchResults: StateFlow<List<ChatHistorySearchMatch>> =
        _historySearchQuery.flatMapLatest { query ->
            localChatRepo.searchHistory(currentUserId, query)
        }
            .catch { error ->
                if (error is CancellationException) throw error
                Log.w(TAG, "Error searching saved chat history", error)
                emit(emptyList())
            }
            .stateIn(viewModelScope, SharingStarted.WhileSubscribed(STOP_TIMEOUT_MS), emptyList())

    @OptIn(ExperimentalCoroutinesApi::class)
    val chatMessagesState: StateFlow<UiState<List<ChatMessage>>> =
        _activeConversationId.flatMapLatest { convId ->
            localChatRepo.observeMessages(currentUserId, convId)
                .map<List<ChatMessageEntity>, UiState<List<ChatMessage>>> { entities ->
                    UiState.Success(entities.map { it.toChatMessage() })
                }
        }
            .catch { error ->
                if (error is CancellationException) throw error
                Log.w(TAG, "Error observing local chatMessages", error)
                emit(UiState.Error(error.localizedMessage ?: "تعذر تحميل سجل المحادثة."))
            }
            .stateIn(viewModelScope, SharingStarted.WhileSubscribed(STOP_TIMEOUT_MS), UiState.Loading)

    init {
        if (userPreferencesRepo != null) {
            viewModelScope.launch {
                val restoredId = userPreferencesRepo.activeConversationId(currentUserId).first()
                _activeConversationId.value = restoredId
            }
        }
        viewModelScope.launch {
            firestoreRepo.observeChatMessages(currentUserId)
                .catch { error ->
                    val firestoreError = generateSequence(error) { it.cause }
                        .filterIsInstance<FirebaseFirestoreException>()
                        .firstOrNull()
                    if (firestoreError == null && error is CancellationException) throw error
                    Log.w(TAG, "Background chat sync error", firestoreError ?: error)
                }
                .collect { remoteMessages ->
                    try {
                        localChatRepo.syncFromRemote(currentUserId, remoteMessages)
                    } catch (e: CancellationException) {
                        throw e
                    } catch (e: Exception) {
                        Log.w(TAG, "Error syncing remote chat messages locally", e)
                    }
                }
        }
    }

    val courseProgressState: StateFlow<Map<String, CourseProgress>> =
        firestoreRepo.observeCourseProgress(currentUserId)
            .catch { error ->
                val firestoreError = generateSequence(error) { it.cause }
                    .filterIsInstance<FirebaseFirestoreException>()
                    .firstOrNull()
                if (firestoreError == null && error is CancellationException) throw error
                Log.w(TAG, "Error observing courseProgress", firestoreError ?: error)
                emit(emptyMap())
            }
            .stateIn(viewModelScope, SharingStarted.WhileSubscribed(STOP_TIMEOUT_MS), emptyMap())

    val allCoursesState: StateFlow<List<CourseUi>> =
        firestoreRepo.observeCustomCourses(currentUserId)
            .map { custom -> custom + StarterContent.builtInCourses }
            .catch { error ->
                val firestoreError = generateSequence(error) { it.cause }
                    .filterIsInstance<FirebaseFirestoreException>()
                    .firstOrNull()
                if (firestoreError == null && error is CancellationException) throw error
                Log.w(TAG, "Error observing customCourses", firestoreError ?: error)
                emit(StarterContent.builtInCourses)
            }
            .stateIn(
                viewModelScope,
                SharingStarted.WhileSubscribed(STOP_TIMEOUT_MS),
                StarterContent.builtInCourses
            )

    private val _selectedTab = MutableStateFlow(SanaTab.CHAT)
    val selectedTab: StateFlow<SanaTab> = _selectedTab.asStateFlow()

    private val _chatMode = MutableStateFlow(ChatMode.CHAT)
    val chatMode: StateFlow<ChatMode> = _chatMode.asStateFlow()

    private val _streamingReply = MutableStateFlow<String?>(null)
    val streamingReply: StateFlow<String?> = _streamingReply.asStateFlow()

    private val _streamingSource = MutableStateFlow<String?>(null)
    val streamingSource: StateFlow<String?> = _streamingSource.asStateFlow()

    private val _streamingConversationId = MutableStateFlow<String?>(null)
    private var activeChatJob: Job? = null

    private val _isSending = MutableStateFlow(false)
    val isSending: StateFlow<Boolean> = _isSending.asStateFlow()

    val isSendingActiveConversation: StateFlow<Boolean> =
        combine(_isSending, _streamingConversationId, _activeConversationId) { sending, streamConv, activeConv ->
            sending && (streamConv == null || streamConv == activeConv)
        }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(STOP_TIMEOUT_MS), false)

    val activeStreamingReply: StateFlow<String?> =
        combine(_streamingReply, _streamingConversationId, _activeConversationId) { reply, streamConv, activeConv ->
            if (streamConv == null || streamConv == activeConv) reply else null
        }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(STOP_TIMEOUT_MS), null)

    private val _chatError = MutableStateFlow<String?>(null)
    val chatError: StateFlow<String?> = _chatError.asStateFlow()

    private val _learnSession = MutableStateFlow(LearnUiSession())
    val learnSession: StateFlow<LearnUiSession> = _learnSession.asStateFlow()

    private val _isGeneratingCourse = MutableStateFlow(false)
    val isGeneratingCourse: StateFlow<Boolean> = _isGeneratingCourse.asStateFlow()

    private val _courseStreamingPreview = MutableStateFlow<String?>(null)
    val courseStreamingPreview: StateFlow<String?> = _courseStreamingPreview.asStateFlow()

    private val _courseError = MutableStateFlow<String?>(null)
    val courseError: StateFlow<String?> = _courseError.asStateFlow()

    private val _selfLearnProgress = MutableStateFlow<SelfLearnProgress?>(null)
    val selfLearnProgress: StateFlow<SelfLearnProgress?> = _selfLearnProgress.asStateFlow()

    private val _selfLearnStreamingPreview = MutableStateFlow<String?>(null)
    val selfLearnStreamingPreview: StateFlow<String?> = _selfLearnStreamingPreview.asStateFlow()

    private val _selfLearnError = MutableStateFlow<String?>(null)
    val selfLearnError: StateFlow<String?> = _selfLearnError.asStateFlow()

    private val _selfLearnSuccessBanner = MutableStateFlow<String?>(null)
    val selfLearnSuccessBanner: StateFlow<String?> = _selfLearnSuccessBanner.asStateFlow()

    private val _studioImageState = MutableStateFlow<UiState<GeneratedImageResult?>?>(null)
    val studioImageState: StateFlow<UiState<GeneratedImageResult?>?> = _studioImageState.asStateFlow()

    private val _studioStoryboardState = MutableStateFlow<UiState<VideoStoryboardPlan?>?>(null)
    val studioStoryboardState: StateFlow<UiState<VideoStoryboardPlan?>?> = _studioStoryboardState.asStateFlow()

    private val _storyboardStreamingPreview = MutableStateFlow<String?>(null)
    val storyboardStreamingPreview: StateFlow<String?> = _storyboardStreamingPreview.asStateFlow()

    private val _studioSceneImages = MutableStateFlow<Map<Int, UiState<Bitmap>>>(emptyMap())
    val studioSceneImages: StateFlow<Map<Int, UiState<Bitmap>>> = _studioSceneImages.asStateFlow()

    val activeCourse: StateFlow<CourseUi?> =
        combine(allCoursesState, _learnSession) { courses, session ->
            courses.firstOrNull { it.id == session.activeCourseId }
        }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(STOP_TIMEOUT_MS), null)

    fun selectTab(tab: SanaTab) {
        _selectedTab.value = tab
    }

    fun selectChatMode(mode: ChatMode) {
        _chatMode.value = mode
    }

    fun dismissChatError() {
        _chatError.value = null
    }

    fun updatePreferredName(name: String) {
        val repo = userPreferencesRepo ?: return
        viewModelScope.launch {
            repo.setPreferredName(name)
        }
    }

    fun clearPreferredName() {
        val repo = userPreferencesRepo ?: return
        viewModelScope.launch {
            repo.clearPreferredName()
        }
    }

    fun saveCurrentConversation(title: String) {
        val sourceId = _activeConversationId.value
        viewModelScope.launch {
            val savedId = localChatRepo.saveConversation(
                userId = currentUserId,
                sourceConversationId = sourceId,
                customTitle = title
            )
            _activeConversationId.value = savedId
            userPreferencesRepo?.setActiveConversationId(currentUserId, savedId)
        }
    }

    fun selectConversation(conversationId: String) {
        _chatError.value = null
        _activeConversationId.value = conversationId
        userPreferencesRepo?.let { repo ->
            viewModelScope.launch {
                repo.setActiveConversationId(currentUserId, conversationId)
            }
        }
    }

    fun startNewConversation() {
        _chatError.value = null
        val newId = localChatRepo.createNewConversationId()
        _activeConversationId.value = newId
        userPreferencesRepo?.let { repo ->
            viewModelScope.launch {
                repo.setActiveConversationId(currentUserId, newId)
            }
        }
    }

    fun updateHistorySearchQuery(query: String) {
        _historySearchQuery.value = query
    }

    fun clearHistorySearch() {
        _historySearchQuery.value = ""
    }

    fun deleteConversation(conversationId: String) {
        viewModelScope.launch {
            localChatRepo.deleteConversation(currentUserId, conversationId)
            if (_activeConversationId.value == conversationId) {
                val fallbackId = DEFAULT_CONVERSATION_ID
                _activeConversationId.value = fallbackId
                userPreferencesRepo?.setActiveConversationId(currentUserId, fallbackId)
            }
        }
    }

    fun stopChatGeneration() {
        activeChatJob?.cancel()
        activeChatJob = null
        _streamingReply.value = null
        _streamingSource.value = null
        _streamingConversationId.value = null
        _isSending.value = false
    }

    fun sendMessage(rawText: String) {
        val messageText = rawText.trim()
        if (messageText.isEmpty() || _isSending.value) return

        val mode = _chatMode.value
        val conversationId = _activeConversationId.value
        val currentMessages = (chatMessagesState.value as? UiState.Success)?.data ?: emptyList()
        val kb = (knowledgeSourcesState.value as? UiState.Success)?.data ?: emptyList()

        activeChatJob = viewModelScope.launch {
            _isSending.value = true
            _chatError.value = null
            _streamingConversationId.value = conversationId
            _streamingReply.value = ""
            _streamingSource.value = null

            val localUserId = try {
                localChatRepo.insertMessage(
                    userId = currentUserId,
                    role = "user",
                    text = messageText,
                    mode = mode.key,
                    conversationId = conversationId
                )
            } catch (e: CancellationException) {
                throw e
            } catch (e: Exception) {
                Log.e(TAG, "Failed to save user message to Room", e)
                _chatError.value = e.localizedMessage ?: "تعذر حفظ الرسالة محليًا."
                _isSending.value = false
                _streamingReply.value = null
                _streamingConversationId.value = null
                return@launch
            }

            if (conversationId == DEFAULT_CONVERSATION_ID) {
                viewModelScope.launch {
                    val saveUserResult = firestoreRepo.addChatMessage(
                        role = "user",
                        text = messageText,
                        mode = mode.key
                    )
                    saveUserResult.onSuccess { remoteId ->
                        localChatRepo.markSynced(localUserId, remoteId)
                    }.onFailure { err ->
                        Log.w(TAG, "Cloud sync for user message failed", err)
                    }
                }
            }

            var finalReply = ""
            var finalSource: String? = null
            try {
                aiRepo.streamReply(
                    userMessage = messageText,
                    mode = mode,
                    recentHistory = currentMessages,
                    knowledgeBase = kb,
                    preferredName = preferredName.value
                ).collect { (partial, cited) ->
                    finalReply = partial
                    finalSource = cited
                    _streamingReply.value = partial
                    _streamingSource.value = cited
                }

                if (finalReply.isNotBlank()) {
                    val localModelId = localChatRepo.insertMessage(
                        userId = currentUserId,
                        role = "model",
                        text = finalReply,
                        sourceTitle = finalSource,
                        mode = mode.key,
                        conversationId = conversationId
                    )
                    if (conversationId == DEFAULT_CONVERSATION_ID) {
                        viewModelScope.launch {
                            val saveModelResult = firestoreRepo.addChatMessage(
                                role = "model",
                                text = finalReply,
                                sourceTitle = finalSource,
                                mode = mode.key
                            )
                            saveModelResult.onSuccess { remoteId ->
                                localChatRepo.markSynced(localModelId, remoteId)
                            }.onFailure { err ->
                                Log.w(TAG, "Cloud sync for model message failed", err)
                            }
                        }
                    }
                }
            } catch (e: TimeoutCancellationException) {
                Log.e(TAG, "AI chat reply timed out", e)
                _chatError.value = formatAiErrorMessage(e)
            } catch (e: CancellationException) {
                if (finalReply.isNotBlank()) {
                    runCatching {
                        kotlinx.coroutines.withContext(kotlinx.coroutines.NonCancellable) {
                            localChatRepo.insertMessage(
                                userId = currentUserId,
                                role = "model",
                                text = finalReply,
                                sourceTitle = finalSource,
                                mode = mode.key,
                                conversationId = conversationId
                            )
                        }
                    }
                }
                throw e
            } catch (e: Exception) {
                Log.e(TAG, "AI chat reply failed", e)
                _chatError.value = formatAiErrorMessage(e)
            } finally {
                _streamingReply.value = null
                _streamingSource.value = null
                _streamingConversationId.value = null
                _isSending.value = false
                activeChatJob = null
            }
        }
    }

    fun clearChatHistory() {
        val conversationId = _activeConversationId.value
        if (_streamingConversationId.value == conversationId) {
            stopChatGeneration()
        }
        val messages = (chatMessagesState.value as? UiState.Success)?.data ?: emptyList()
        _chatError.value = null
        viewModelScope.launch {
            localChatRepo.clearMessages(currentUserId, conversationId)
            if (conversationId == DEFAULT_CONVERSATION_ID) {
                val remoteIds = messages.map { it.id }.filter { !it.startsWith("local_") }
                if (remoteIds.isNotEmpty()) {
                    val result = firestoreRepo.clearChatHistory(remoteIds)
                    if (result.isFailure) {
                        Log.w(TAG, "Failed to clear remote chat history", result.exceptionOrNull())
                    }
                }
            }
        }
    }

    fun buildExportChatText(): String {
        val messages = (chatMessagesState.value as? UiState.Success)?.data ?: emptyList()
        if (messages.isEmpty()) return ""
        return messages.joinToString("\n\n") { msg ->
            val who = if (msg.role == "user") "أنت" else "سنا"
            val src = if (!msg.sourceTitle.isNullOrBlank()) "\n[المصدر: ${msg.sourceTitle}]" else ""
            "$who: ${msg.text}$src"
        }
    }

    // ---------- Self-Learning (Knowledge Base) ----------

    fun dismissSelfLearnError() {
        _selfLearnError.value = null
    }

    fun dismissSelfLearnBanner() {
        _selfLearnSuccessBanner.value = null
    }

    fun learnPresetSite(site: PresetSite) {
        if (_selfLearnProgress.value != null) return
        val steps = listOf(
            "الاتصال بـ ${site.name}…",
            "قراءة المحتوى واستخلاص الأفكار…",
            "تحليل بالذكاء الاصطناعي واستخراج الكلمات المفتاحية…",
            "حفظ المعرفة في ذاكرة سنا السحابية…"
        )
        viewModelScope.launch {
            _selfLearnError.value = null
            _selfLearnSuccessBanner.value = null
            _selfLearnStreamingPreview.value = null
            try {
                _selfLearnProgress.value = SelfLearnProgress(1, site.name, steps)
                val analysis = aiRepo.analyzeKnowledgeContent(site.text, site.name) { partial ->
                    _selfLearnStreamingPreview.value = partial
                }
                _selfLearnProgress.value = SelfLearnProgress(3, site.name, steps)
                val saveRes = firestoreRepo.addKnowledgeSource(
                    title = site.name,
                    sourceLabel = site.name,
                    sourceUrl = site.url,
                    sourceType = "site",
                    summary = analysis.summary,
                    content = site.text,
                    keywords = analysis.keywords,
                    factCount = analysis.factCount
                )
                if (saveRes.isFailure) {
                    throw saveRes.exceptionOrNull() ?: IllegalStateException("فشل الحفظ")
                }
                _selfLearnSuccessBanner.value = "تعلّمت سنا «${site.name}» وأضافتها إلى ذاكرتها."
            } catch (e: TimeoutCancellationException) {
                Log.e(TAG, "learnPresetSite timed out", e)
                _selfLearnError.value = formatAiErrorMessage(e)
            } catch (e: CancellationException) {
                throw e
            } catch (e: Exception) {
                Log.e(TAG, "learnPresetSite failed", e)
                _selfLearnError.value = formatAiErrorMessage(e)
            } finally {
                _selfLearnProgress.value = null
                _selfLearnStreamingPreview.value = null
            }
        }
    }

    fun learnPastedText(rawText: String) {
        val text = rawText.trim()
        if (text.length < 40) {
            _selfLearnError.value = "النص قصير جدًا — أدخل 40 حرفًا على الأقل لتتعلم منه سنا."
            return
        }
        if (_selfLearnProgress.value != null) return
        val steps = listOf(
            "تجهيز النص للقراءة…",
            "تحليل المعنى واستخراج الملخص…",
            "توليد الكلمات المفتاحية…",
            "حفظ المعرفة في السحابة…"
        )
        viewModelScope.launch {
            _selfLearnError.value = null
            _selfLearnSuccessBanner.value = null
            _selfLearnStreamingPreview.value = null
            try {
                _selfLearnProgress.value = SelfLearnProgress(1, "نص مضاف", steps)
                val analysis = aiRepo.analyzeKnowledgeContent(text) { partial ->
                    _selfLearnStreamingPreview.value = partial
                }
                _selfLearnProgress.value = SelfLearnProgress(3, analysis.title, steps)
                val saveRes = firestoreRepo.addKnowledgeSource(
                    title = analysis.title,
                    sourceLabel = "نص مضاف يدويًا",
                    sourceUrl = null,
                    sourceType = "text",
                    summary = analysis.summary,
                    content = text,
                    keywords = analysis.keywords,
                    factCount = analysis.factCount
                )
                if (saveRes.isFailure) {
                    throw saveRes.exceptionOrNull() ?: IllegalStateException("فشل الحفظ")
                }
                _selfLearnSuccessBanner.value = "تعلّمت سنا «${analysis.title}» (${analysis.factCount} أفكار رئيسية)."
            } catch (e: TimeoutCancellationException) {
                Log.e(TAG, "learnPastedText timed out", e)
                _selfLearnError.value = formatAiErrorMessage(e)
            } catch (e: CancellationException) {
                throw e
            } catch (e: Exception) {
                Log.e(TAG, "learnPastedText failed", e)
                _selfLearnError.value = formatAiErrorMessage(e)
            } finally {
                _selfLearnProgress.value = null
                _selfLearnStreamingPreview.value = null
            }
        }
    }

    fun learnFromUrl(rawUrl: String) {
        val trimmed = rawUrl.trim()
        if (trimmed.isBlank()) {
            _selfLearnError.value = "أدخل رابط صفحة ويب صالحًا."
            return
        }
        if (_selfLearnProgress.value != null) return
        val steps = listOf(
            "الاتصال بصفحة الويب وجلب المحتوى…",
            "تنظيف النص واستخراج المقال…",
            "تحليل Gemini وتوليد الملخص والكلمات المفتاحية…",
            "حفظ المعرفة في ذاكرة سنا السحابية…"
        )
        viewModelScope.launch {
            _selfLearnError.value = null
            _selfLearnSuccessBanner.value = null
            _selfLearnStreamingPreview.value = null
            try {
                _selfLearnProgress.value = SelfLearnProgress(0, trimmed, steps)
                val (pageTitle, extractedText) = aiRepo.fetchWebPageText(trimmed)
                _selfLearnProgress.value = SelfLearnProgress(2, pageTitle, steps)
                val analysis = aiRepo.analyzeKnowledgeContent(extractedText, pageTitle) { partial ->
                    _selfLearnStreamingPreview.value = partial
                }
                _selfLearnProgress.value = SelfLearnProgress(3, analysis.title, steps)
                val normalizedUrl = if (trimmed.startsWith("http")) trimmed else "https://$trimmed"
                val hostLabel = runCatching {
                    java.net.URI(normalizedUrl).host?.removePrefix("www.") ?: pageTitle
                }.getOrDefault(pageTitle)

                val saveRes = firestoreRepo.addKnowledgeSource(
                    title = analysis.title,
                    sourceLabel = hostLabel,
                    sourceUrl = normalizedUrl,
                    sourceType = "url",
                    summary = analysis.summary,
                    content = extractedText,
                    keywords = analysis.keywords,
                    factCount = analysis.factCount
                )
                if (saveRes.isFailure) {
                    throw saveRes.exceptionOrNull() ?: IllegalStateException("فشل الحفظ")
                }
                _selfLearnSuccessBanner.value = "تعلّمت سنا من «$hostLabel» وأضافتها إلى الذاكرة."
            } catch (e: TimeoutCancellationException) {
                Log.e(TAG, "learnFromUrl timed out", e)
                _selfLearnError.value = formatAiErrorMessage(e)
            } catch (e: CancellationException) {
                throw e
            } catch (e: Exception) {
                Log.e(TAG, "learnFromUrl failed", e)
                _selfLearnError.value = formatAiErrorMessage(e)
            } finally {
                _selfLearnProgress.value = null
                _selfLearnStreamingPreview.value = null
            }
        }
    }

    fun deleteKnowledgeSource(sourceId: String) {
        viewModelScope.launch {
            val res = firestoreRepo.deleteKnowledgeSource(sourceId)
            if (res.isFailure) {
                _selfLearnError.value = res.exceptionOrNull()?.localizedMessage ?: "تعذر حذف المصدر."
            }
        }
    }

    // ---------- Interactive Courses (Learn) ----------

    fun dismissCourseError() {
        _courseError.value = null
    }

    fun openCourse(course: CourseUi) {
        _learnSession.value = LearnUiSession(
            subView = LearnSubView.LESSON,
            activeCourseId = course.id,
            lessonIndex = 0
        )
        markLessonVisited(course.id, 0)
    }

    fun backToCourseList() {
        _learnSession.value = LearnUiSession(subView = LearnSubView.LIST)
    }

    fun previousLesson() {
        val current = _learnSession.value
        if (current.lessonIndex > 0) {
            val nextIdx = current.lessonIndex - 1
            _learnSession.value = current.copy(lessonIndex = nextIdx)
        } else {
            backToCourseList()
        }
    }

    fun nextLessonOrStartQuiz(course: CourseUi) {
        val current = _learnSession.value
        if (current.lessonIndex < course.lessons.lastIndex) {
            val nextIdx = current.lessonIndex + 1
            _learnSession.value = current.copy(lessonIndex = nextIdx)
            markLessonVisited(course.id, nextIdx)
        } else {
            _learnSession.value = current.copy(
                subView = LearnSubView.QUIZ,
                quizIndex = 0,
                quizScore = 0,
                selectedOption = null
            )
        }
    }

    fun selectQuizOption(course: CourseUi, optionIndex: Int) {
        val current = _learnSession.value
        if (current.selectedOption != null) return
        val q = course.quiz.getOrNull(current.quizIndex) ?: return
        val gained = if (optionIndex == q.correctIndex) 1 else 0
        _learnSession.value = current.copy(
            selectedOption = optionIndex,
            quizScore = current.quizScore + gained
        )
    }

    fun nextQuestionOrFinish(course: CourseUi) {
        val current = _learnSession.value
        if (current.quizIndex < course.quiz.lastIndex) {
            _learnSession.value = current.copy(
                quizIndex = current.quizIndex + 1,
                selectedOption = null
            )
        } else {
            _learnSession.value = current.copy(subView = LearnSubView.RESULT)
            val existing = courseProgressState.value[course.id]
            viewModelScope.launch {
                val res = firestoreRepo.saveQuizResult(
                    courseId = course.id,
                    score = current.quizScore,
                    total = course.quiz.size,
                    existing = existing
                )
                if (res.isFailure) {
                    _courseError.value = res.exceptionOrNull()?.localizedMessage
                        ?: "تعذر حفظ نتيجة الاختبار."
                }
            }
        }
    }

    fun retryQuiz() {
        val current = _learnSession.value
        _learnSession.value = current.copy(
            subView = LearnSubView.QUIZ,
            quizIndex = 0,
            quizScore = 0,
            selectedOption = null
        )
    }

    fun generateCustomCourse(topic: String) {
        val cleanTopic = topic.trim()
        if (cleanTopic.length < 3 || _isGeneratingCourse.value) return
        viewModelScope.launch {
            _isGeneratingCourse.value = true
            _courseError.value = null
            _courseStreamingPreview.value = null
            try {
                val generated = aiRepo.generateCourse(cleanTopic) { partial ->
                    _courseStreamingPreview.value = partial
                }
                val saveRes = firestoreRepo.saveCustomCourse(generated)
                if (saveRes.isFailure) {
                    throw saveRes.exceptionOrNull() ?: IllegalStateException("تعذر حفظ الدورة.")
                }
            } catch (e: TimeoutCancellationException) {
                Log.e(TAG, "generateCustomCourse timed out", e)
                _courseError.value = formatAiErrorMessage(e)
            } catch (e: CancellationException) {
                throw e
            } catch (e: Exception) {
                Log.e(TAG, "generateCustomCourse failed", e)
                _courseError.value = formatAiErrorMessage(e)
            } finally {
                _isGeneratingCourse.value = false
                _courseStreamingPreview.value = null
            }
        }
    }

    fun deleteCustomCourse(courseId: String) {
        viewModelScope.launch {
            val res = firestoreRepo.deleteCustomCourse(courseId)
            if (res.isFailure) {
                _courseError.value = res.exceptionOrNull()?.localizedMessage ?: "تعذر حذف الدورة."
            }
        }
    }

    // ---------- Creative Studio (Images & Video Storyboards) ----------

    fun generateStudioImage(
        prompt: String,
        aspectRatio: StudioAspectRatio,
        referenceBitmap: Bitmap? = null
    ) {
        val clean = prompt.trim()
        if (clean.isEmpty() || _studioImageState.value is UiState.Loading) return
        viewModelScope.launch {
            _studioImageState.value = UiState.Loading
            try {
                val bitmap = aiRepo.generateImage(
                    prompt = clean,
                    aspectRatio = aspectRatio,
                    referenceBitmap = referenceBitmap
                )
                _studioImageState.value = UiState.Success(
                    GeneratedImageResult(
                        prompt = clean,
                        aspectRatio = aspectRatio,
                        bitmap = bitmap
                    )
                )
            } catch (e: TimeoutCancellationException) {
                Log.e(TAG, "generateStudioImage timed out", e)
                _studioImageState.value = UiState.Error(formatAiErrorMessage(e))
            } catch (e: CancellationException) {
                throw e
            } catch (e: Exception) {
                Log.e(TAG, "generateStudioImage failed", e)
                _studioImageState.value = UiState.Error(formatAiErrorMessage(e))
            }
        }
    }

    fun clearStudioImage() {
        _studioImageState.value = null
    }

    fun generateVideoStoryboard(
        topic: String,
        aspectRatio: StudioAspectRatio
    ) {
        val clean = topic.trim()
        if (clean.isEmpty() || _studioStoryboardState.value is UiState.Loading) return
        viewModelScope.launch {
            _studioStoryboardState.value = UiState.Loading
            _storyboardStreamingPreview.value = null
            _studioSceneImages.value = emptyMap()
            try {
                val plan = aiRepo.generateVideoStoryboard(
                    topic = clean,
                    aspectRatio = aspectRatio,
                    onStreamingProgress = { partial ->
                        _storyboardStreamingPreview.value = partial
                    }
                )
                _studioStoryboardState.value = UiState.Success(plan)
            } catch (e: TimeoutCancellationException) {
                Log.e(TAG, "generateVideoStoryboard timed out", e)
                _studioStoryboardState.value = UiState.Error(formatAiErrorMessage(e))
            } catch (e: CancellationException) {
                throw e
            } catch (e: Exception) {
                Log.e(TAG, "generateVideoStoryboard failed", e)
                _studioStoryboardState.value = UiState.Error(formatAiErrorMessage(e))
            } finally {
                _storyboardStreamingPreview.value = null
            }
        }
    }

    fun generateSceneFrame(
        sceneIndex: Int,
        visualPrompt: String,
        aspectRatio: StudioAspectRatio
    ) {
        if (_studioSceneImages.value[sceneIndex] is UiState.Loading) return
        viewModelScope.launch {
            _studioSceneImages.value = _studioSceneImages.value + (sceneIndex to UiState.Loading)
            try {
                val bitmap = aiRepo.generateImage(
                    prompt = visualPrompt,
                    aspectRatio = aspectRatio
                )
                _studioSceneImages.value = _studioSceneImages.value + (sceneIndex to UiState.Success(bitmap))
            } catch (e: TimeoutCancellationException) {
                Log.e(TAG, "generateSceneFrame timed out for scene $sceneIndex", e)
                _studioSceneImages.value = _studioSceneImages.value + (
                    sceneIndex to UiState.Error(formatAiErrorMessage(e))
                )
            } catch (e: CancellationException) {
                throw e
            } catch (e: Exception) {
                Log.e(TAG, "generateSceneFrame failed for scene $sceneIndex", e)
                _studioSceneImages.value = _studioSceneImages.value + (
                    sceneIndex to UiState.Error(formatAiErrorMessage(e))
                )
            }
        }
    }

    fun clearStudioStoryboard() {
        _studioStoryboardState.value = null
        _studioSceneImages.value = emptyMap()
    }

    private fun markLessonVisited(courseId: String, lessonIndex: Int) {
        val existing = courseProgressState.value[courseId]
        if (existing?.completedLessons?.contains(lessonIndex.toLong()) == true) return
        viewModelScope.launch {
            firestoreRepo.markLessonCompleted(courseId, lessonIndex, existing)
        }
    }

    private fun ChatMessageEntity.toChatMessage(): ChatMessage = ChatMessage(
        id = remoteId?.takeIf { it.isNotBlank() } ?: "local_$localId",
        userId = userId,
        role = role,
        text = text,
        sourceTitle = sourceTitle,
        mode = mode,
        createdAt = Timestamp(Date(createdAtMillis))
    )
}
