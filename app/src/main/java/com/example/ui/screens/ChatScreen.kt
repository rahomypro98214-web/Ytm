package com.example.ui.screens

import android.app.Activity
import android.content.ActivityNotFoundException
import android.content.Intent
import android.util.Log
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.core.FastOutSlowInEasing
import androidx.compose.animation.core.RepeatMode
import androidx.compose.animation.core.StartOffset
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.tween
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.combinedClickable
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.imePadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardActions
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.Send
import androidx.compose.material.icons.automirrored.filled.VolumeUp
import androidx.compose.material.icons.filled.AddComment
import androidx.compose.material.icons.filled.AutoAwesome
import androidx.compose.material.icons.filled.Bookmark
import androidx.compose.material.icons.filled.BookmarkAdded
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.filled.ContentCopy
import androidx.compose.material.icons.filled.DeleteSweep
import androidx.compose.material.icons.filled.GraphicEq
import androidx.compose.material.icons.filled.History
import androidx.compose.material.icons.filled.Mic
import androidx.compose.material.icons.filled.MoreVert
import androidx.compose.material.icons.filled.Quiz
import androidx.compose.material.icons.filled.Settings
import androidx.compose.material.icons.filled.Share
import androidx.compose.material.icons.filled.Stop
import androidx.compose.material.icons.filled.StopCircle
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.FilterChip
import androidx.compose.material3.FilterChipDefaults
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.IconButtonDefaults
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.OutlinedTextFieldDefaults
import androidx.compose.material3.SuggestionChip
import androidx.compose.material3.SuggestionChipDefaults
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.minimumInteractiveComponentSize
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.hapticfeedback.HapticFeedbackType
import androidx.compose.ui.platform.LocalClipboardManager
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalHapticFeedback
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.AnnotatedString
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.ViewModelProvider.AndroidViewModelFactory.Companion.APPLICATION_KEY
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewModelScope
import androidx.lifecycle.viewmodel.compose.viewModel
import androidx.lifecycle.viewmodel.initializer
import androidx.lifecycle.viewmodel.viewModelFactory
import com.example.R
import com.example.data.local.ChatConversationEntity
import com.example.data.local.ChatHistorySearchMatch
import com.example.data.local.DEFAULT_CONVERSATION_ID
import com.example.data.local.SanaDatabase
import com.example.data.model.ChatMessage
import com.example.data.model.ChatMode
import com.example.data.model.ChatQuizUiState
import com.example.data.model.QuizQuestion
import com.example.data.model.UiState
import com.example.data.repository.LocalChatRepository
import com.example.data.repository.SanaAiRepository
import com.example.data.repository.UserPreferencesRepository
import com.example.data.repository.formatAiErrorMessage
import com.example.data.repository.isTransientOverloadError
import com.example.ui.SanaViewModel
import com.example.ui.components.ChatQuizSheet
import com.example.ui.components.ConversationHistorySheet
import com.example.ui.components.PromptOfTheDayCarousel
import com.example.ui.components.SanaMarkdownText
import com.example.ui.components.SaveConversationDialog
import com.example.ui.components.SettingsMenuDialog
import com.example.ui.components.SpeechToTextController
import com.example.ui.components.TextToSpeechController
import com.example.ui.components.mergeDictatedText
import com.example.ui.components.rememberSpeechToTextController
import com.example.ui.components.rememberTextToSpeechController
import com.example.ui.theme.LocalSanaColors
import com.google.firebase.Firebase
import com.google.firebase.ai.GenerativeModel
import com.google.firebase.ai.ai
import com.google.firebase.ai.type.content
import com.google.firebase.ai.type.generationConfig
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.Job
import kotlinx.coroutines.TimeoutCancellationException
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.flatMapLatest
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import kotlinx.coroutines.withTimeout

private const val CHAT_TAG = "FirebaseChatVM"
private const val LOCAL_CHAT_USER_ID = "local"

data class ChatDisplayMessage(
    val id: String,
    val isUser: Boolean,
    val text: String,
    val sourceTitle: String? = null
)

/**
 * Standalone ViewModel powered by Firebase AI Logic (`Firebase.ai`) and Room (`LocalChatRepository`)
 * for displaying, streaming, and persisting user and AI messages in [ChatScreen].
 */
class FirebaseChatViewModel(
    private val localChatRepo: LocalChatRepository,
    private val userPreferencesRepo: UserPreferencesRepository? = null,
    private val aiRepo: SanaAiRepository = SanaAiRepository()
) : ViewModel() {
    val preferredName: StateFlow<String?> =
        userPreferencesRepo?.preferredName
            ?.stateIn(
                scope = viewModelScope,
                started = SharingStarted.WhileSubscribed(5000L),
                initialValue = null
            ) ?: MutableStateFlow<String?>(null).asStateFlow()

    private val chatConfig by lazy {
        generationConfig {
            temperature = 0.7f
            maxOutputTokens = 2048
        }
    }

    private val chatSystemInstruction by lazy {
        content {
            text("أنت سنا، مساعد ذكي بالعربية. قدّم إجابات واضحة وموجزة ومفيدة.")
        }
    }

    private val model by lazy {
        Firebase.ai.generativeModel(
            modelName = "gemini-3.8-flash",
            generationConfig = chatConfig,
            systemInstruction = chatSystemInstruction
        )
    }

    private val fastModel by lazy {
        Firebase.ai.generativeModel(
            modelName = "gemini-3.5-flash-lite",
            generationConfig = chatConfig,
            systemInstruction = chatSystemInstruction
        )
    }

    private val _activeConversationId = MutableStateFlow(DEFAULT_CONVERSATION_ID)
    val activeConversationId: StateFlow<String> = _activeConversationId.asStateFlow()

    val savedConversations: StateFlow<List<ChatConversationEntity>> =
        localChatRepo.observeConversations(LOCAL_CHAT_USER_ID)
            .stateIn(
                scope = viewModelScope,
                started = SharingStarted.WhileSubscribed(5000L),
                initialValue = emptyList()
            )

    private val _historySearchQuery = MutableStateFlow("")
    val historySearchQuery: StateFlow<String> = _historySearchQuery.asStateFlow()

    @OptIn(ExperimentalCoroutinesApi::class)
    val historySearchResults: StateFlow<List<ChatHistorySearchMatch>> =
        _historySearchQuery.flatMapLatest { query ->
            localChatRepo.searchHistory(LOCAL_CHAT_USER_ID, query)
        }
            .stateIn(
                scope = viewModelScope,
                started = SharingStarted.WhileSubscribed(5000L),
                initialValue = emptyList()
            )

    @OptIn(ExperimentalCoroutinesApi::class)
    val messages: StateFlow<List<ChatDisplayMessage>> =
        _activeConversationId.flatMapLatest { convId ->
            localChatRepo.observeMessages(LOCAL_CHAT_USER_ID, convId)
                .map { entities ->
                    entities.map { entity ->
                        ChatDisplayMessage(
                            id = entity.remoteId?.takeIf { it.isNotBlank() } ?: "local_${entity.localId}",
                            isUser = entity.role == "user",
                            text = entity.text,
                            sourceTitle = entity.sourceTitle
                        )
                    }
                }
        }
            .stateIn(
                scope = viewModelScope,
                started = SharingStarted.WhileSubscribed(5000L),
                initialValue = emptyList()
            )

    init {
        if (userPreferencesRepo != null) {
            viewModelScope.launch {
                val restoredId = userPreferencesRepo.activeConversationId(LOCAL_CHAT_USER_ID).first()
                _activeConversationId.value = restoredId
            }
        }
    }

    private val _streamingText = MutableStateFlow<String?>(null)
    val streamingText: StateFlow<String?> = _streamingText.asStateFlow()

    private val _streamingConversationId = MutableStateFlow<String?>(null)
    private var activeChatJob: Job? = null

    private val _isSending = MutableStateFlow(false)
    val isSending: StateFlow<Boolean> = _isSending.asStateFlow()

    val isSendingActiveConversation: StateFlow<Boolean> =
        combine(_isSending, _streamingConversationId, _activeConversationId) { sending, streamConv, activeConv ->
            sending && (streamConv == null || streamConv == activeConv)
        }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000L), false)

    val activeStreamingText: StateFlow<String?> =
        combine(_streamingText, _streamingConversationId, _activeConversationId) { reply, streamConv, activeConv ->
            if (streamConv == null || streamConv == activeConv) reply else null
        }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000L), null)

    private val _error = MutableStateFlow<String?>(null)
    val error: StateFlow<String?> = _error.asStateFlow()

    private val _chatQuizState = MutableStateFlow<ChatQuizUiState>(ChatQuizUiState.Hidden)
    val chatQuizState: StateFlow<ChatQuizUiState> = _chatQuizState.asStateFlow()
    private var activeQuizJob: Job? = null

    fun dismissError() {
        _error.value = null
    }

    fun stopGeneration() {
        activeChatJob?.cancel()
        activeChatJob = null
        _streamingText.value = null
        _streamingConversationId.value = null
        _isSending.value = false
    }

    fun clearMessages() {
        val conversationId = _activeConversationId.value
        if (_streamingConversationId.value == conversationId) {
            stopGeneration()
        }
        dismissChatQuiz()
        _error.value = null
        viewModelScope.launch {
            localChatRepo.clearMessages(LOCAL_CHAT_USER_ID, conversationId)
        }
    }

    fun saveCurrentConversation(title: String) {
        val sourceId = _activeConversationId.value
        viewModelScope.launch {
            val savedId = localChatRepo.saveConversation(
                userId = LOCAL_CHAT_USER_ID,
                sourceConversationId = sourceId,
                customTitle = title
            )
            _activeConversationId.value = savedId
            userPreferencesRepo?.setActiveConversationId(LOCAL_CHAT_USER_ID, savedId)
        }
    }

    fun selectConversation(conversationId: String) {
        _error.value = null
        dismissChatQuiz()
        _activeConversationId.value = conversationId
        userPreferencesRepo?.let { repo ->
            viewModelScope.launch {
                repo.setActiveConversationId(LOCAL_CHAT_USER_ID, conversationId)
            }
        }
    }

    fun startNewConversation() {
        _error.value = null
        dismissChatQuiz()
        val newId = localChatRepo.createNewConversationId()
        _activeConversationId.value = newId
        userPreferencesRepo?.let { repo ->
            viewModelScope.launch {
                repo.setActiveConversationId(LOCAL_CHAT_USER_ID, newId)
            }
        }
    }

    fun startChatContextQuiz() {
        val currentMessages = messages.value
            .filter { it.text.isNotBlank() }
            .map { msg ->
                ChatMessage(
                    id = msg.id,
                    userId = LOCAL_CHAT_USER_ID,
                    role = if (msg.isUser) "user" else "model",
                    text = msg.text,
                    sourceTitle = msg.sourceTitle
                )
            }
        if (currentMessages.isEmpty()) {
            _chatQuizState.value = ChatQuizUiState.Error(
                "ابدأ محادثة قصيرة مع سنا أولًا لتوليد اختبار تفاعلي من سياقها."
            )
            return
        }
        activeQuizJob?.cancel()
        activeQuizJob = viewModelScope.launch {
            _chatQuizState.value = ChatQuizUiState.Generating()
            try {
                val generated = aiRepo.generateQuizFromChatContext(
                    recentMessages = currentMessages,
                    onStreamingProgress = { preview ->
                        if (_chatQuizState.value is ChatQuizUiState.Generating) {
                            _chatQuizState.value = ChatQuizUiState.Generating(preview)
                        }
                    }
                )
                _chatQuizState.value = ChatQuizUiState.Ready(quiz = generated)
            } catch (e: TimeoutCancellationException) {
                Log.e(CHAT_TAG, "Chat quiz generation timed out", e)
                _chatQuizState.value = ChatQuizUiState.Error(formatAiErrorMessage(e))
            } catch (e: CancellationException) {
                throw e
            } catch (e: Exception) {
                Log.e(CHAT_TAG, "Chat quiz generation failed", e)
                _chatQuizState.value = ChatQuizUiState.Error(formatAiErrorMessage(e))
            } finally {
                activeQuizJob = null
            }
        }
    }

    fun selectChatQuizOption(optionIndex: Int) {
        val current = _chatQuizState.value as? ChatQuizUiState.Ready ?: return
        if (current.showSummary) return
        if (current.selectedOptions.containsKey(current.currentIndex)) return
        val question = current.currentQuestion ?: return
        if (optionIndex !in question.options.indices) return
        _chatQuizState.value = current.copy(
            selectedOptions = current.selectedOptions + (current.currentIndex to optionIndex)
        )
    }

    fun nextChatQuizQuestion() {
        val current = _chatQuizState.value as? ChatQuizUiState.Ready ?: return
        if (current.currentIndex < current.quiz.questions.lastIndex) {
            _chatQuizState.value = current.copy(currentIndex = current.currentIndex + 1)
        } else {
            _chatQuizState.value = current.copy(showSummary = true)
        }
    }

    fun restartChatQuiz() {
        val current = _chatQuizState.value as? ChatQuizUiState.Ready ?: return
        _chatQuizState.value = current.copy(
            currentIndex = 0,
            selectedOptions = emptyMap(),
            showSummary = false
        )
    }

    fun dismissChatQuiz() {
        activeQuizJob?.cancel()
        activeQuizJob = null
        _chatQuizState.value = ChatQuizUiState.Hidden
    }

    fun askSanaAboutQuizQuestion(question: QuizQuestion, chosenOption: Int?) {
        dismissChatQuiz()
        val correctText = question.options.getOrNull(question.correctIndex).orEmpty()
        val chosenText = chosenOption?.let { question.options.getOrNull(it) }
        val followUpPrompt = buildString {
            append("اشرحي لي هذا السؤال من الاختبار بتفصيل أوضح مع مثال عملي: «${question.question}».")
            if (chosenText != null && chosenOption != question.correctIndex) {
                append(" اخترتُ «$chosenText» لكن الإجابة الصحيحة هي «$correctText».")
            } else if (correctText.isNotBlank()) {
                append(" الإجابة الصحيحة هي «$correctText».")
            }
        }
        sendMessage(followUpPrompt)
    }

    fun updateHistorySearchQuery(query: String) {
        _historySearchQuery.value = query
    }

    fun clearHistorySearch() {
        _historySearchQuery.value = ""
    }

    fun deleteConversation(conversationId: String) {
        viewModelScope.launch {
            localChatRepo.deleteConversation(LOCAL_CHAT_USER_ID, conversationId)
            if (_activeConversationId.value == conversationId) {
                _activeConversationId.value = DEFAULT_CONVERSATION_ID
                userPreferencesRepo?.setActiveConversationId(LOCAL_CHAT_USER_ID, DEFAULT_CONVERSATION_ID)
            }
        }
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

    fun sendMessage(rawText: String) {
        val text = rawText.trim()
        if (text.isEmpty() || _isSending.value) return

        val conversationId = _activeConversationId.value
        val historySnapshot = messages.value
        _isSending.value = true
        _error.value = null
        _streamingConversationId.value = conversationId
        _streamingText.value = ""

        activeChatJob = viewModelScope.launch {
            localChatRepo.insertMessage(
                userId = LOCAL_CHAT_USER_ID,
                role = "user",
                text = text,
                conversationId = conversationId
            )

            val accumulated = StringBuilder()
            val historyContents = historySnapshot
                .filter { it.text.isNotBlank() }
                .takeLast(8)
                .map { msg ->
                    content(role = if (msg.isUser) "user" else "model") {
                        text(msg.text.take(600))
                    }
                }

            suspend fun streamFrom(targetModel: GenerativeModel, timeoutMs: Long) {
                accumulated.clear()
                withContext(Dispatchers.IO) {
                    val chatSession = targetModel.startChat(history = historyContents)
                    withTimeout(timeoutMs) {
                        chatSession.sendMessageStream(text).collect { chunk ->
                            val piece = chunk.text.orEmpty()
                            if (piece.isNotEmpty()) {
                                accumulated.append(piece)
                                _streamingText.value = accumulated.toString()
                            }
                        }
                    }
                }
            }

            try {
                try {
                    streamFrom(model, 35_000L)
                } catch (e: TimeoutCancellationException) {
                    if (accumulated.isNotBlank()) throw e
                    Log.w(CHAT_TAG, "Primary model timed out; retrying once with gemini-3.5-flash-lite", e)
                    streamFrom(fastModel, 25_000L)
                } catch (e: CancellationException) {
                    throw e
                } catch (e: Exception) {
                    if (accumulated.isBlank() && isTransientOverloadError(e)) {
                        Log.w(CHAT_TAG, "Primary model temporarily unavailable; retrying with gemini-3.5-flash-lite", e)
                        streamFrom(fastModel, 25_000L)
                    } else {
                        throw e
                    }
                }

                if (accumulated.isNotBlank()) {
                    localChatRepo.insertMessage(
                        userId = LOCAL_CHAT_USER_ID,
                        role = "model",
                        text = accumulated.toString(),
                        conversationId = conversationId
                    )
                } else {
                    _error.value = "لم يُرجِع النموذج نصًا. حاول مرة أخرى."
                }
            } catch (e: TimeoutCancellationException) {
                Log.e(CHAT_TAG, "Firebase AI chat timed out", e)
                _error.value = formatAiErrorMessage(e)
            } catch (e: CancellationException) {
                if (accumulated.isNotBlank()) {
                    runCatching {
                        withContext(kotlinx.coroutines.NonCancellable) {
                            localChatRepo.insertMessage(
                                userId = LOCAL_CHAT_USER_ID,
                                role = "model",
                                text = accumulated.toString(),
                                conversationId = conversationId
                            )
                        }
                    }
                }
                throw e
            } catch (e: Exception) {
                Log.e(CHAT_TAG, "Firebase AI chat failed", e)
                _error.value = formatAiErrorMessage(e)
            } finally {
                _streamingText.value = null
                _streamingConversationId.value = null
                _isSending.value = false
                activeChatJob = null
            }
        }
    }

    companion object {
        val Factory: ViewModelProvider.Factory = viewModelFactory {
            initializer {
                val app = checkNotNull(this[APPLICATION_KEY])
                val sanaDb = SanaDatabase.getInstance(app)
                FirebaseChatViewModel(
                    localChatRepo = LocalChatRepository(
                        chatMessageDao = sanaDb.chatMessageDao(),
                        conversationDao = sanaDb.chatConversationDao()
                    ),
                    userPreferencesRepo = UserPreferencesRepository(app)
                )
            }
        }
    }
}

/**
 * Self-contained ChatScreen component backed by [FirebaseChatViewModel], Room, and Firebase AI.
 */
@Composable
fun ChatScreen(
    modifier: Modifier = Modifier,
    chatViewModel: FirebaseChatViewModel = viewModel(factory = FirebaseChatViewModel.Factory)
) {
    val messages by chatViewModel.messages.collectAsStateWithLifecycle()
    val streamingText by chatViewModel.activeStreamingText.collectAsStateWithLifecycle()
    val isSending by chatViewModel.isSendingActiveConversation.collectAsStateWithLifecycle()
    val error by chatViewModel.error.collectAsStateWithLifecycle()
    val preferredName by chatViewModel.preferredName.collectAsStateWithLifecycle()
    val savedConversations by chatViewModel.savedConversations.collectAsStateWithLifecycle()
    val activeConversationId by chatViewModel.activeConversationId.collectAsStateWithLifecycle()
    val historySearchQuery by chatViewModel.historySearchQuery.collectAsStateWithLifecycle()
    val historySearchResults by chatViewModel.historySearchResults.collectAsStateWithLifecycle()
    val chatQuizState by chatViewModel.chatQuizState.collectAsStateWithLifecycle()

    ChatScreenContent(
        messagesState = UiState.Success(messages),
        streamingReply = streamingText,
        streamingSource = null,
        isSending = isSending,
        chatError = error,
        selectedMode = null,
        onSelectMode = null,
        onSendMessage = { chatViewModel.sendMessage(it) },
        onRetryLast = {
            messages.lastOrNull { it.isUser }?.let { chatViewModel.sendMessage(it.text) }
        },
        onDismissError = { chatViewModel.dismissError() },
        onClearChat = { chatViewModel.clearMessages() },
        exportTextProvider = {
            messages.joinToString("\n\n") { m -> "${if (m.isUser) "أنت" else "سنا"}: ${m.text}" }
        },
        modifier = modifier,
        preferredName = preferredName,
        onUpdatePreferredName = { chatViewModel.updatePreferredName(it) },
        onClearPreferredName = { chatViewModel.clearPreferredName() },
        savedConversations = savedConversations,
        activeConversationId = activeConversationId,
        onSaveConversation = { chatViewModel.saveCurrentConversation(it) },
        onSelectConversation = { chatViewModel.selectConversation(it) },
        onStartNewConversation = { chatViewModel.startNewConversation() },
        onDeleteConversation = { chatViewModel.deleteConversation(it) },
        onStopGeneration = { chatViewModel.stopGeneration() },
        historySearchQuery = historySearchQuery,
        historySearchResults = historySearchResults,
        onHistorySearchQueryChange = { chatViewModel.updateHistorySearchQuery(it) },
        chatQuizState = chatQuizState,
        onStartChatQuiz = { chatViewModel.startChatContextQuiz() },
        onSelectQuizOption = { chatViewModel.selectChatQuizOption(it) },
        onNextQuizQuestion = { chatViewModel.nextChatQuizQuestion() },
        onRestartChatQuiz = { chatViewModel.restartChatQuiz() },
        onDismissChatQuiz = { chatViewModel.dismissChatQuiz() },
        onAskAboutQuizQuestion = { question, chosen ->
            chatViewModel.askSanaAboutQuizQuestion(question, chosen)
        }
    )
}

/**
 * Integrated ChatScreen wired to [SanaViewModel] (Firebase AI + Knowledge Base RAG + Firestore sync).
 */
@Composable
fun ChatScreen(
    viewModel: SanaViewModel,
    modifier: Modifier = Modifier
) {
    val messagesState by viewModel.chatMessagesState.collectAsStateWithLifecycle()
    val chatMode by viewModel.chatMode.collectAsStateWithLifecycle()
    val streamingReply by viewModel.activeStreamingReply.collectAsStateWithLifecycle()
    val streamingSource by viewModel.streamingSource.collectAsStateWithLifecycle()
    val isSending by viewModel.isSendingActiveConversation.collectAsStateWithLifecycle()
    val chatError by viewModel.chatError.collectAsStateWithLifecycle()
    val preferredName by viewModel.preferredName.collectAsStateWithLifecycle()
    val savedConversations by viewModel.savedConversations.collectAsStateWithLifecycle()
    val activeConversationId by viewModel.activeConversationId.collectAsStateWithLifecycle()
    val historySearchQuery by viewModel.historySearchQuery.collectAsStateWithLifecycle()
    val historySearchResults by viewModel.historySearchResults.collectAsStateWithLifecycle()
    val chatQuizState by viewModel.chatQuizState.collectAsStateWithLifecycle()

    val mappedState: UiState<List<ChatDisplayMessage>> = remember(messagesState) {
        when (val state = messagesState) {
            is UiState.Loading -> UiState.Loading
            is UiState.Error -> UiState.Error(state.message)
            is UiState.Success -> UiState.Success(
                state.data.map { msg -> msg.toDisplayMessage() }
            )
        }
    }

    ChatScreenContent(
        messagesState = mappedState,
        streamingReply = streamingReply,
        streamingSource = streamingSource,
        isSending = isSending,
        chatError = chatError,
        selectedMode = chatMode,
        onSelectMode = { viewModel.selectChatMode(it) },
        onSendMessage = { viewModel.sendMessage(it) },
        onRetryLast = {
            val lastUser = (messagesState as? UiState.Success)?.data?.lastOrNull { it.role == "user" }
            if (lastUser != null) viewModel.sendMessage(lastUser.text)
        },
        onDismissError = { viewModel.dismissChatError() },
        onClearChat = { viewModel.clearChatHistory() },
        exportTextProvider = { viewModel.buildExportChatText() },
        modifier = modifier,
        preferredName = preferredName,
        onUpdatePreferredName = { viewModel.updatePreferredName(it) },
        onClearPreferredName = { viewModel.clearPreferredName() },
        savedConversations = savedConversations,
        activeConversationId = activeConversationId,
        onSaveConversation = { viewModel.saveCurrentConversation(it) },
        onSelectConversation = { viewModel.selectConversation(it) },
        onStartNewConversation = { viewModel.startNewConversation() },
        onDeleteConversation = { viewModel.deleteConversation(it) },
        onStopGeneration = { viewModel.stopChatGeneration() },
        historySearchQuery = historySearchQuery,
        historySearchResults = historySearchResults,
        onHistorySearchQueryChange = { viewModel.updateHistorySearchQuery(it) },
        chatQuizState = chatQuizState,
        onStartChatQuiz = { viewModel.startChatContextQuiz() },
        onSelectQuizOption = { viewModel.selectChatQuizOption(it) },
        onNextQuizQuestion = { viewModel.nextChatQuizQuestion() },
        onRestartChatQuiz = { viewModel.restartChatQuiz() },
        onDismissChatQuiz = { viewModel.dismissChatQuiz() },
        onAskAboutQuizQuestion = { question, chosen ->
            viewModel.askSanaAboutQuizQuestion(question, chosen)
        }
    )
}

private fun ChatMessage.toDisplayMessage() = ChatDisplayMessage(
    id = id,
    isUser = role == "user",
    text = text,
    sourceTitle = sourceTitle
)

@Composable
internal fun ChatScreenContent(
    messagesState: UiState<List<ChatDisplayMessage>>,
    streamingReply: String?,
    streamingSource: String?,
    isSending: Boolean,
    chatError: String?,
    selectedMode: ChatMode?,
    onSelectMode: ((ChatMode) -> Unit)?,
    onSendMessage: (String) -> Unit,
    onRetryLast: () -> Unit,
    onDismissError: () -> Unit,
    onClearChat: () -> Unit,
    exportTextProvider: () -> String,
    modifier: Modifier = Modifier,
    ttsController: TextToSpeechController = rememberTextToSpeechController(),
    preferredName: String? = null,
    onUpdatePreferredName: ((String) -> Unit)? = null,
    onClearPreferredName: (() -> Unit)? = null,
    savedConversations: List<ChatConversationEntity> = emptyList(),
    activeConversationId: String = DEFAULT_CONVERSATION_ID,
    onSaveConversation: ((String) -> Unit)? = null,
    onSelectConversation: ((String) -> Unit)? = null,
    onStartNewConversation: (() -> Unit)? = null,
    onDeleteConversation: ((String) -> Unit)? = null,
    onStopGeneration: (() -> Unit)? = null,
    historySearchQuery: String = "",
    historySearchResults: List<ChatHistorySearchMatch> = emptyList(),
    onHistorySearchQueryChange: ((String) -> Unit)? = null,
    speechController: SpeechToTextController? = null,
    chatQuizState: ChatQuizUiState = ChatQuizUiState.Hidden,
    onStartChatQuiz: (() -> Unit)? = null,
    onSelectQuizOption: ((Int) -> Unit)? = null,
    onNextQuizQuestion: (() -> Unit)? = null,
    onRestartChatQuiz: (() -> Unit)? = null,
    onDismissChatQuiz: (() -> Unit)? = null,
    onAskAboutQuizQuestion: ((QuizQuestion, Int?) -> Unit)? = null
) {
    val context = LocalContext.current
    val sanaColors = LocalSanaColors.current

    var inputText by rememberSaveable { mutableStateOf("") }
    var dictationBaseText by remember { mutableStateOf("") }
    var showClearDialog by remember { mutableStateOf(false) }
    var showSettingsDialog by remember { mutableStateOf(false) }
    var showChatOptionsMenu by remember { mutableStateOf(false) }
    var showHistorySheet by remember { mutableStateOf(false) }
    var showSaveConversationDialog by remember { mutableStateOf(false) }
    var searchScrollTarget by remember { mutableStateOf<ChatHistorySearchMatch?>(null) }
    var highlightedMessageKey by remember { mutableStateOf<String?>(null) }
    val listState = rememberLazyListState()

    val defaultSpeechController = rememberSpeechToTextController(
        onPartialResult = { partial ->
            inputText = mergeDictatedText(dictationBaseText, partial)
        },
        onFinalResult = { final ->
            val merged = mergeDictatedText(dictationBaseText, final)
            inputText = merged
            dictationBaseText = merged
        }
    )
    val activeSpeechController = speechController ?: defaultSpeechController

    androidx.compose.runtime.DisposableEffect(activeSpeechController, ttsController) {
        onDispose {
            ttsController.stop()
            activeSpeechController.stopListening()
        }
    }

    val quickPrompts = remember {
        listOf(
            "ماذا تعلّمتِ حتى الآن؟",
            "اكتبي لي قصيدة قصيرة ملهمة",
            "اقترحي 3 أفكار مشاريع ذكية",
            "لخّصي لي أهمية القراءة اليومية"
        )
    }

    val messages = (messagesState as? UiState.Success)?.data ?: emptyList()
    var promptCarouselExpanded by rememberSaveable(messages.isEmpty()) {
        mutableStateOf(messages.isEmpty())
    }
    val showTypingBubble = isSending || streamingReply != null
    val totalMessageItems = messages.size + (if (showTypingBubble) 1 else 0)
    val isLastMessageSpeaking =
        ttsController.speakingMessageId != null &&
            ttsController.speakingMessageId == messages.lastOrNull()?.id

    LaunchedEffect(searchScrollTarget, activeConversationId, messages) {
        val target = searchScrollTarget ?: return@LaunchedEffect
        if (target.conversationId == activeConversationId && messages.isNotEmpty()) {
            val matchIndex = messages.indexOfFirst { msg ->
                msg.id == "local_${target.matchedMessageId}" || msg.text == target.matchedText
            }
            if (matchIndex >= 0) {
                val matchedId = messages[matchIndex].id
                highlightedMessageKey = matchedId
                runCatching {
                    // Index + 1 accounts for prompt_of_day_header item at LazyColumn index 0
                    listState.animateScrollToItem(matchIndex + 1)
                }
                searchScrollTarget = null
            }
        }
    }

    LaunchedEffect(highlightedMessageKey) {
        if (highlightedMessageKey != null) {
            delay(3500L)
            highlightedMessageKey = null
        }
    }

    LaunchedEffect(
        messages.size,
        messages.lastOrNull()?.id,
        showTypingBubble,
        streamingReply?.length ?: 0,
        isLastMessageSpeaking
    ) {
        if (searchScrollTarget == null && totalMessageItems > 0) {
            val totalInList = listState.layoutInfo.totalItemsCount
            val targetIdx = if (totalInList > 0) {
                minOf(totalMessageItems, totalInList - 1)
            } else {
                totalMessageItems
            }
            runCatching {
                if (!streamingReply.isNullOrEmpty()) {
                    listState.scrollToItem(targetIdx)
                } else {
                    listState.animateScrollToItem(targetIdx)
                }
            }
        }
    }

    if (showClearDialog) {
        AlertDialog(
            onDismissRequest = { showClearDialog = false },
            title = {
                Text(
                    text = stringResource(R.string.clear_chat_confirm_title),
                    style = MaterialTheme.typography.titleLarge
                )
            },
            text = {
                Text(
                    text = stringResource(R.string.clear_chat_confirm_body),
                    style = MaterialTheme.typography.bodyMedium
                )
            },
            confirmButton = {
                TextButton(
                    onClick = {
                        showClearDialog = false
                        ttsController.stop()
                        onClearChat()
                    },
                    modifier = Modifier.testTag("confirm_clear_chat_button")
                ) {
                    Text(
                        text = stringResource(R.string.clear_chat_confirm_action),
                        color = sanaColors.errorText
                    )
                }
            },
            dismissButton = {
                TextButton(onClick = { showClearDialog = false }) {
                    Text(stringResource(R.string.cancel))
                }
            }
        )
    }

    if (showSettingsDialog && onUpdatePreferredName != null) {
        SettingsMenuDialog(
            currentPreferredName = preferredName,
            onSavePreferredName = onUpdatePreferredName,
            onClearPreferredName = { onClearPreferredName?.invoke() ?: onUpdatePreferredName("") },
            onDismiss = { showSettingsDialog = false }
        )
    }

    if (showHistorySheet && onSelectConversation != null) {
        ConversationHistorySheet(
            conversations = savedConversations,
            activeConversationId = activeConversationId,
            hasActiveMessages = messages.isNotEmpty(),
            onSelectConversation = { convId ->
                ttsController.stop()
                searchScrollTarget = null
                highlightedMessageKey = null
                onSelectConversation(convId)
            },
            onStartNewConversation = {
                ttsController.stop()
                searchScrollTarget = null
                highlightedMessageKey = null
                onStartNewConversation?.invoke()
            },
            onRequestSaveCurrent = {
                showHistorySheet = false
                showSaveConversationDialog = true
            },
            onDeleteConversation = { convId ->
                onDeleteConversation?.invoke(convId)
            },
            onDismiss = {
                showHistorySheet = false
                onHistorySearchQueryChange?.invoke("")
            },
            searchQuery = historySearchQuery,
            searchResults = historySearchResults,
            onSearchQueryChange = { query ->
                onHistorySearchQueryChange?.invoke(query)
            },
            onSelectSearchMatch = { match ->
                ttsController.stop()
                searchScrollTarget = match
                onSelectConversation(match.conversationId)
            }
        )
    }

    if (showSaveConversationDialog && onSaveConversation != null) {
        val suggestedTitle = remember(messages, savedConversations, activeConversationId) {
            savedConversations.firstOrNull { it.conversationId == activeConversationId }?.title
                ?: messages.firstOrNull { it.isUser }?.text?.take(44)
                ?: ""
        }
        SaveConversationDialog(
            initialTitle = suggestedTitle,
            onSave = { title -> onSaveConversation(title) },
            onDismiss = { showSaveConversationDialog = false }
        )
    }

    if (chatQuizState !is ChatQuizUiState.Hidden) {
        ChatQuizSheet(
            quizState = chatQuizState,
            onSelectOption = { idx -> onSelectQuizOption?.invoke(idx) },
            onNextQuestion = { onNextQuizQuestion?.invoke() },
            onRestartQuiz = { onRestartChatQuiz?.invoke() },
            onRegenerateQuiz = { onStartChatQuiz?.invoke() },
            onDismiss = { onDismissChatQuiz?.invoke() },
            onAskAboutQuestion = onAskAboutQuizQuestion
        )
    }

    Column(
        modifier = modifier
            .fillMaxSize()
            .background(sanaColors.chatCanvas)
            .imePadding()
    ) {
        // Top Mode Selector + History + Overflow Menu
        if (onSelectMode != null || onUpdatePreferredName != null || onSelectConversation != null || messages.isNotEmpty()) {
            Surface(
                color = MaterialTheme.colorScheme.surface,
                tonalElevation = 1.dp
            ) {
                Column {
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(start = 12.dp, end = 4.dp, top = 6.dp, bottom = 6.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Row(
                            modifier = Modifier
                                .weight(1f)
                                .horizontalScroll(rememberScrollState()),
                            horizontalArrangement = Arrangement.spacedBy(8.dp),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            if (onSelectMode != null && selectedMode != null) {
                                ChatMode.entries.forEach { mode ->
                                    FilterChip(
                                        selected = selectedMode == mode,
                                        onClick = { onSelectMode(mode) },
                                        label = {
                                            Text(
                                                text = mode.label,
                                                style = MaterialTheme.typography.labelMedium
                                            )
                                        },
                                        colors = FilterChipDefaults.filterChipColors(
                                            selectedContainerColor = MaterialTheme.colorScheme.secondary,
                                            selectedLabelColor = MaterialTheme.colorScheme.onSecondary
                                        ),
                                        modifier = Modifier.testTag("chat_mode_${mode.key}")
                                    )
                                }
                            } else if (!preferredName.isNullOrBlank()) {
                                Text(
                                    text = stringResource(R.string.top_greeting_format, preferredName.trim()),
                                    style = MaterialTheme.typography.titleSmall.copy(fontWeight = FontWeight.Bold),
                                    color = sanaColors.tealDeep,
                                    modifier = Modifier.testTag("chat_top_greeting")
                                )
                            }
                        }

                        if (onStartChatQuiz != null && messages.isNotEmpty()) {
                            IconButton(
                                onClick = {
                                    ttsController.stop()
                                    onStartChatQuiz()
                                },
                                enabled = !isSending,
                                modifier = Modifier
                                    .minimumInteractiveComponentSize()
                                    .testTag("chat_quiz_button")
                            ) {
                                Icon(
                                    imageVector = Icons.Filled.Quiz,
                                    contentDescription = stringResource(R.string.chat_quiz_title),
                                    tint = sanaColors.amberDeep
                                )
                            }
                        }

                        if (onSelectConversation != null) {
                            IconButton(
                                onClick = { showHistorySheet = true },
                                modifier = Modifier
                                    .minimumInteractiveComponentSize()
                                    .testTag("chat_history_button")
                            ) {
                                Icon(
                                    imageVector = Icons.Filled.History,
                                    contentDescription = stringResource(R.string.chat_history_cd),
                                    tint = sanaColors.tealDeep
                                )
                            }
                        }

                        Box {
                            IconButton(
                                onClick = { showChatOptionsMenu = true },
                                modifier = Modifier
                                    .minimumInteractiveComponentSize()
                                    .testTag("chat_options_menu_button")
                            ) {
                                Icon(
                                    imageVector = Icons.Filled.MoreVert,
                                    contentDescription = stringResource(R.string.chat_options),
                                    tint = MaterialTheme.colorScheme.onSurfaceVariant
                                )
                            }

                            DropdownMenu(
                                expanded = showChatOptionsMenu,
                                onDismissRequest = { showChatOptionsMenu = false }
                            ) {
                                if (onStartChatQuiz != null) {
                                    DropdownMenuItem(
                                        text = { Text(stringResource(R.string.chat_quiz_menu_item)) },
                                        leadingIcon = {
                                            Icon(
                                                imageVector = Icons.Filled.Quiz,
                                                contentDescription = null,
                                                tint = sanaColors.amberDeep
                                            )
                                        },
                                        enabled = messages.isNotEmpty() && !isSending,
                                        onClick = {
                                            showChatOptionsMenu = false
                                            ttsController.stop()
                                            onStartChatQuiz()
                                        },
                                        modifier = Modifier.testTag("chat_quiz_menu_item")
                                    )
                                }
                                if (onSelectConversation != null) {
                                    DropdownMenuItem(
                                        text = { Text(stringResource(R.string.chat_history_title)) },
                                        leadingIcon = {
                                            Icon(
                                                imageVector = Icons.Filled.History,
                                                contentDescription = null
                                            )
                                        },
                                        onClick = {
                                            showChatOptionsMenu = false
                                            showHistorySheet = true
                                        },
                                        modifier = Modifier.testTag("open_history_menu_item")
                                    )
                                    if (onSaveConversation != null) {
                                        DropdownMenuItem(
                                            text = { Text(stringResource(R.string.save_conversation)) },
                                            leadingIcon = {
                                                Icon(
                                                    imageVector = Icons.Filled.BookmarkAdded,
                                                    contentDescription = null
                                                )
                                            },
                                            enabled = messages.isNotEmpty(),
                                            onClick = {
                                                showChatOptionsMenu = false
                                                showSaveConversationDialog = true
                                            },
                                            modifier = Modifier.testTag("save_conversation_menu_item")
                                        )
                                    }
                                    if (onStartNewConversation != null) {
                                        DropdownMenuItem(
                                            text = { Text(stringResource(R.string.new_conversation)) },
                                            leadingIcon = {
                                                Icon(
                                                    imageVector = Icons.Filled.AddComment,
                                                    contentDescription = null
                                                )
                                            },
                                            onClick = {
                                                showChatOptionsMenu = false
                                                ttsController.stop()
                                                onStartNewConversation()
                                            },
                                            modifier = Modifier.testTag("new_conversation_menu_item")
                                        )
                                    }
                                }
                                if (onUpdatePreferredName != null) {
                                    DropdownMenuItem(
                                        text = { Text(stringResource(R.string.settings_title)) },
                                        leadingIcon = {
                                            Icon(
                                                imageVector = Icons.Filled.Settings,
                                                contentDescription = null
                                            )
                                        },
                                        onClick = {
                                            showChatOptionsMenu = false
                                            showSettingsDialog = true
                                        },
                                        modifier = Modifier.testTag("chat_settings_menu_item")
                                    )
                                }
                                DropdownMenuItem(
                                    text = { Text(stringResource(R.string.export_chat)) },
                                    leadingIcon = {
                                        Icon(
                                            imageVector = Icons.Filled.Share,
                                            contentDescription = null
                                        )
                                    },
                                    enabled = messages.isNotEmpty(),
                                    onClick = {
                                        showChatOptionsMenu = false
                                        val exported = exportTextProvider()
                                        if (exported.isNotBlank()) {
                                            val shareIntent = Intent(Intent.ACTION_SEND).apply {
                                                type = "text/plain"
                                                putExtra(Intent.EXTRA_SUBJECT, "محادثة مع سنا")
                                                putExtra(Intent.EXTRA_TEXT, exported)
                                            }
                                            val chooser = Intent.createChooser(
                                                shareIntent,
                                                "مشاركة أو تصدير المحادثة"
                                            ).apply {
                                                if (context !is Activity) {
                                                    addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
                                                }
                                            }
                                            try {
                                                context.startActivity(chooser)
                                            } catch (_: ActivityNotFoundException) {
                                                // Ignore when no share handler is available
                                            }
                                        }
                                    },
                                    modifier = Modifier.testTag("export_chat_menu_item")
                                )
                                DropdownMenuItem(
                                    text = {
                                        Text(
                                            text = stringResource(R.string.clear_chat_history),
                                            color = if (messages.isNotEmpty()) {
                                                sanaColors.errorText
                                            } else {
                                                MaterialTheme.colorScheme.onSurface.copy(alpha = 0.38f)
                                            }
                                        )
                                    },
                                    leadingIcon = {
                                        Icon(
                                            imageVector = Icons.Filled.DeleteSweep,
                                            contentDescription = null,
                                            tint = if (messages.isNotEmpty()) {
                                                sanaColors.errorText
                                            } else {
                                                MaterialTheme.colorScheme.onSurface.copy(alpha = 0.38f)
                                            }
                                        )
                                    },
                                    enabled = messages.isNotEmpty(),
                                    onClick = {
                                        showChatOptionsMenu = false
                                        showClearDialog = true
                                    },
                                    modifier = Modifier.testTag("clear_chat_menu_item")
                                )
                            }
                        }
                    }
                    HorizontalDivider(color = sanaColors.cardBorder)
                }
            }
        }

        // Error Banner with Retry & Dismiss
        AnimatedVisibility(visible = !chatError.isNullOrBlank()) {
            Surface(
                color = sanaColors.errorSurface,
                modifier = Modifier
                    .fillMaxWidth()
                    .clickable { onDismissError() }
            ) {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 16.dp, vertical = 8.dp),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    Text(
                        text = chatError.orEmpty(),
                        style = MaterialTheme.typography.bodySmall,
                        color = sanaColors.errorText,
                        modifier = Modifier.weight(1f)
                    )
                    if (messages.any { it.isUser } && !isSending) {
                        TextButton(
                            onClick = onRetryLast,
                            modifier = Modifier.testTag("retry_chat_button")
                        ) {
                            Text("إعادة المحاولة", color = sanaColors.errorText)
                        }
                    }
                }
            }
        }

        // TTS Error Banner (dismissible)
        AnimatedVisibility(visible = !ttsController.errorMessage.isNullOrBlank()) {
            Surface(
                color = sanaColors.errorSurface,
                modifier = Modifier
                    .fillMaxWidth()
                    .clickable { ttsController.dismissError() }
                    .testTag("tts_error_banner")
            ) {
                Text(
                    text = ttsController.errorMessage.orEmpty(),
                    style = MaterialTheme.typography.bodySmall,
                    color = sanaColors.errorText,
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 16.dp, vertical = 8.dp)
                )
            }
        }

        // Scrollable Messages List
        Box(
            modifier = Modifier
                .weight(1f)
                .fillMaxWidth()
        ) {
            when (messagesState) {
                is UiState.Loading -> {
                    CircularProgressIndicator(
                        modifier = Modifier.align(Alignment.Center),
                        color = MaterialTheme.colorScheme.primary
                    )
                }

                is UiState.Error -> {
                    Column(
                        modifier = Modifier
                            .align(Alignment.Center)
                            .padding(24.dp),
                        horizontalAlignment = Alignment.CenterHorizontally,
                        verticalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        Text(
                            text = messagesState.message,
                            style = MaterialTheme.typography.bodyMedium,
                            color = sanaColors.errorText
                        )
                    }
                }

                is UiState.Success -> {
                    if (messages.isEmpty() && !showTypingBubble) {
                        Column(
                            modifier = Modifier
                                .fillMaxSize()
                                .verticalScroll(rememberScrollState()),
                            horizontalAlignment = Alignment.CenterHorizontally
                        ) {
                            PromptOfTheDayCarousel(
                                expanded = promptCarouselExpanded,
                                enabled = !isSending,
                                onToggleExpanded = { promptCarouselExpanded = !promptCarouselExpanded },
                                onSelectPrompt = { promptText ->
                                    if (!isSending) {
                                        activeSpeechController.stopListening()
                                        ttsController.stop()
                                        dictationBaseText = ""
                                        onSendMessage(promptText)
                                    }
                                }
                            )
                            HorizontalDivider(color = sanaColors.cardBorder)
                            ChatWelcomeEmptyState(
                                preferredName = preferredName,
                                modifier = Modifier.padding(horizontal = 16.dp, vertical = 10.dp)
                            )
                            if (savedConversations.isNotEmpty() && onSelectConversation != null) {
                                OutlinedButton(
                                    onClick = { showHistorySheet = true },
                                    border = BorderStroke(1.dp, sanaColors.tealAccent),
                                    modifier = Modifier
                                        .padding(horizontal = 16.dp, vertical = 4.dp)
                                        .minimumInteractiveComponentSize()
                                        .testTag("empty_state_history_button")
                                ) {
                                    Icon(
                                        imageVector = Icons.Filled.History,
                                        contentDescription = null,
                                        tint = sanaColors.tealDeep,
                                        modifier = Modifier.size(18.dp)
                                    )
                                    androidx.compose.foundation.layout.Spacer(
                                        modifier = Modifier.size(8.dp)
                                    )
                                    Text(
                                        text = "العودة إلى المحادثات المحفوظة (${savedConversations.size})",
                                        color = sanaColors.tealDeep
                                    )
                                }
                            }
                        }
                    } else {
                        LazyColumn(
                            state = listState,
                            modifier = Modifier
                                .fillMaxSize()
                                .testTag("chat_message_list"),
                            contentPadding = PaddingValues(horizontal = 14.dp, vertical = 16.dp),
                            verticalArrangement = Arrangement.spacedBy(12.dp)
                        ) {
                            item(key = "prompt_of_day_header") {
                                PromptOfTheDayCarousel(
                                    expanded = promptCarouselExpanded,
                                    enabled = !isSending,
                                    onToggleExpanded = { promptCarouselExpanded = !promptCarouselExpanded },
                                    onSelectPrompt = { promptText ->
                                        if (!isSending) {
                                            activeSpeechController.stopListening()
                                            ttsController.stop()
                                            dictationBaseText = ""
                                            onSendMessage(promptText)
                                        }
                                    },
                                    modifier = Modifier.clip(RoundedCornerShape(14.dp))
                                )
                            }
                            items(messages, key = { it.id }) { msg ->
                                ChatBubbleItem(
                                    message = msg,
                                    isSpeaking = !msg.isUser && ttsController.speakingMessageId == msg.id,
                                    isHighlighted = highlightedMessageKey == msg.id,
                                    onToggleSpeak = if (!msg.isUser) {
                                        { ttsController.toggleSpeak(msg.id, msg.text) }
                                    } else {
                                        null
                                    }
                                )
                            }
                            if (showTypingBubble) {
                                item(key = "streaming_bubble") {
                                    StreamingBubbleItem(
                                        text = streamingReply.orEmpty(),
                                        sourceTitle = streamingSource,
                                        isSending = isSending,
                                        onStopGeneration = onStopGeneration
                                    )
                                }
                            }
                            item(key = "chat_bottom_anchor") {
                                Spacer(
                                    modifier = Modifier
                                        .height(1.dp)
                                        .testTag("chat_bottom_anchor")
                                )
                            }
                        }
                    }
                }
            }
        }

        // Quick Prompt Chips (+ Interactive Chat Quiz Action Chip)
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .background(MaterialTheme.colorScheme.surface)
                .horizontalScroll(rememberScrollState())
                .padding(horizontal = 14.dp, vertical = 8.dp),
            horizontalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            if (onStartChatQuiz != null && messages.isNotEmpty()) {
                SuggestionChip(
                    onClick = {
                        ttsController.stop()
                        onStartChatQuiz()
                    },
                    enabled = !isSending,
                    icon = {
                        Icon(
                            imageVector = Icons.Filled.Quiz,
                            contentDescription = null,
                            tint = sanaColors.amberDeep,
                            modifier = Modifier.size(16.dp)
                        )
                    },
                    label = {
                        Text(
                            text = stringResource(R.string.chat_quiz_quick_chip),
                            style = MaterialTheme.typography.bodySmall.copy(fontWeight = FontWeight.Bold),
                            color = sanaColors.amberDeep
                        )
                    },
                    colors = SuggestionChipDefaults.suggestionChipColors(
                        containerColor = sanaColors.amberTint
                    ),
                    border = BorderStroke(1.dp, sanaColors.amberAccent),
                    modifier = Modifier.testTag("chat_quick_quiz_chip")
                )
            }
            quickPrompts.forEach { prompt ->
                SuggestionChip(
                    onClick = { onSendMessage(prompt) },
                    enabled = !isSending,
                    label = {
                        Text(
                            text = prompt,
                            style = MaterialTheme.typography.bodySmall
                        )
                    },
                    colors = SuggestionChipDefaults.suggestionChipColors(
                        containerColor = MaterialTheme.colorScheme.surface
                    ),
                    border = BorderStroke(1.dp, sanaColors.cardBorder)
                )
            }
        }

        HorizontalDivider(color = sanaColors.cardBorder)

        // Speech-to-Text Listening / Error Banner
        AnimatedVisibility(
            visible = activeSpeechController.isListening || !activeSpeechController.errorMessage.isNullOrBlank()
        ) {
            val speechErr = activeSpeechController.errorMessage
            Surface(
                color = if (activeSpeechController.isListening) sanaColors.tealTint else sanaColors.errorSurface,
                modifier = Modifier
                    .fillMaxWidth()
                    .then(
                        if (!activeSpeechController.isListening && !speechErr.isNullOrBlank()) {
                            Modifier.clickable { activeSpeechController.dismissError() }
                        } else {
                            Modifier
                        }
                    )
                    .testTag(
                        if (activeSpeechController.isListening) "speech_listening_banner" else "speech_error_banner"
                    )
            ) {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 14.dp, vertical = 6.dp),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    if (activeSpeechController.isListening) {
                        val pulseScale = 1f + (activeSpeechController.audioLevel * 0.25f)
                        Icon(
                            imageVector = Icons.Filled.GraphicEq,
                            contentDescription = null,
                            tint = sanaColors.tealDeep,
                            modifier = Modifier
                                .size(18.dp)
                                .graphicsLayer {
                                    scaleX = pulseScale
                                    scaleY = pulseScale
                                }
                        )
                        Text(
                            text = stringResource(R.string.speech_listening),
                            style = MaterialTheme.typography.labelLarge,
                            color = sanaColors.tealDeep,
                            modifier = Modifier.weight(1f)
                        )
                        TextButton(
                            onClick = { activeSpeechController.stopListening() },
                            modifier = Modifier
                                .minimumInteractiveComponentSize()
                                .testTag("speech_stop_button")
                        ) {
                            Text(
                                text = stringResource(R.string.speech_stop),
                                color = sanaColors.errorText
                            )
                        }
                    } else if (!speechErr.isNullOrBlank()) {
                        Text(
                            text = speechErr,
                            style = MaterialTheme.typography.bodySmall,
                            color = sanaColors.errorText,
                            modifier = Modifier.weight(1f)
                        )
                    }
                }
            }
        }

        // Chat Input Bar (Text Field + Microphone Button + Send Button)
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .background(MaterialTheme.colorScheme.surface)
                .padding(horizontal = 12.dp, vertical = 10.dp),
            horizontalArrangement = Arrangement.spacedBy(8.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            OutlinedTextField(
                value = inputText,
                onValueChange = { inputText = it },
                placeholder = {
                    Text(
                        text = stringResource(R.string.chat_placeholder),
                        style = MaterialTheme.typography.bodyMedium
                    )
                },
                singleLine = false,
                maxLines = 4,
                shape = RoundedCornerShape(16.dp),
                colors = OutlinedTextFieldDefaults.colors(
                    focusedBorderColor = if (activeSpeechController.isListening) {
                        sanaColors.tealAccent
                    } else {
                        MaterialTheme.colorScheme.primary
                    },
                    unfocusedBorderColor = if (activeSpeechController.isListening) {
                        sanaColors.tealAccent
                    } else {
                        sanaColors.cardBorder
                    },
                    focusedContainerColor = sanaColors.chatCanvas,
                    unfocusedContainerColor = sanaColors.chatCanvas
                ),
                keyboardOptions = KeyboardOptions(imeAction = ImeAction.Send),
                keyboardActions = KeyboardActions(
                    onSend = {
                        if (inputText.isNotBlank() && !isSending) {
                            activeSpeechController.stopListening()
                            dictationBaseText = ""
                            onSendMessage(inputText)
                            inputText = ""
                        }
                    }
                ),
                modifier = Modifier
                    .weight(1f)
                    .testTag("chat_input_field")
            )

            val micScale = if (activeSpeechController.isListening) {
                1.04f + (activeSpeechController.audioLevel * 0.12f)
            } else {
                1f
            }
            IconButton(
                onClick = {
                    if (!activeSpeechController.isListening) {
                        ttsController.stop()
                        dictationBaseText = inputText
                    }
                    activeSpeechController.toggleListening()
                },
                enabled = !isSending,
                colors = IconButtonDefaults.filledTonalIconButtonColors(
                    containerColor = if (activeSpeechController.isListening) {
                        sanaColors.errorSurface
                    } else {
                        sanaColors.tealTint
                    },
                    contentColor = if (activeSpeechController.isListening) {
                        sanaColors.errorText
                    } else {
                        sanaColors.tealDeep
                    },
                    disabledContainerColor = sanaColors.cardBorder.copy(alpha = 0.4f),
                    disabledContentColor = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.5f)
                ),
                modifier = Modifier
                    .size(48.dp)
                    .minimumInteractiveComponentSize()
                    .graphicsLayer {
                        scaleX = micScale
                        scaleY = micScale
                    }
                    .testTag("chat_mic_button")
            ) {
                Icon(
                    imageVector = if (activeSpeechController.isListening) {
                        Icons.Filled.Stop
                    } else {
                        Icons.Filled.Mic
                    },
                    contentDescription = if (activeSpeechController.isListening) {
                        stringResource(R.string.mic_stop_cd)
                    } else {
                        stringResource(R.string.mic_start_cd)
                    }
                )
            }

            IconButton(
                onClick = {
                    if (inputText.isNotBlank() && !isSending) {
                        activeSpeechController.stopListening()
                        dictationBaseText = ""
                        onSendMessage(inputText)
                        inputText = ""
                    }
                },
                enabled = inputText.isNotBlank() && !isSending,
                colors = IconButtonDefaults.filledIconButtonColors(
                    containerColor = MaterialTheme.colorScheme.primary,
                    contentColor = MaterialTheme.colorScheme.onPrimary,
                    disabledContainerColor = sanaColors.cardBorder,
                    disabledContentColor = MaterialTheme.colorScheme.onSurfaceVariant
                ),
                modifier = Modifier
                    .size(50.dp)
                    .minimumInteractiveComponentSize()
                    .testTag("chat_send_button")
            ) {
                if (isSending) {
                    CircularProgressIndicator(
                        modifier = Modifier.size(20.dp),
                        color = MaterialTheme.colorScheme.onPrimary,
                        strokeWidth = 2.dp
                    )
                } else {
                    Icon(
                        imageVector = Icons.AutoMirrored.Filled.Send,
                        contentDescription = stringResource(R.string.send_cd)
                    )
                }
            }
        }
    }
}

@Composable
private fun ChatWelcomeEmptyState(
    preferredName: String? = null,
    modifier: Modifier = Modifier
) {
    val sanaColors = LocalSanaColors.current
    val cleanName = preferredName?.trim()?.takeIf { it.isNotEmpty() }
    val greetingText = if (cleanName != null) {
        stringResource(R.string.greeting_personalized_format, cleanName)
    } else {
        stringResource(R.string.greeting_default)
    }
    Card(
        modifier = modifier.widthIn(max = 420.dp),
        shape = MaterialTheme.shapes.large,
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
        border = BorderStroke(1.dp, sanaColors.cardBorder)
    ) {
        Column(
            modifier = Modifier.padding(14.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.spacedBy(6.dp)
        ) {
            Box(
                modifier = Modifier
                    .size(34.dp)
                    .clip(CircleShape)
                    .background(sanaColors.tealTint),
                contentAlignment = Alignment.Center
            ) {
                Icon(
                    imageVector = Icons.Filled.AutoAwesome,
                    contentDescription = null,
                    tint = sanaColors.tealDeep
                )
            }
            Text(
                text = greetingText,
                style = MaterialTheme.typography.titleLarge,
                color = MaterialTheme.colorScheme.onSurface,
                modifier = Modifier.testTag("chat_welcome_greeting")
            )
            Text(
                text = "اسألني أي سؤال، أو اختر وضع «تلخيص» أو «ترجمة»، أو أضف صفحات ونصوصًا في تبويب «تعلّم ذاتي» وسأجيبك منها مباشرة مع ذكر المصدر.",
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
        }
    }
}

@OptIn(ExperimentalFoundationApi::class)
@Composable
private fun ChatBubbleItem(
    message: ChatDisplayMessage,
    isSpeaking: Boolean = false,
    isHighlighted: Boolean = false,
    onToggleSpeak: (() -> Unit)? = null
) {
    val sanaColors = LocalSanaColors.current
    val clipboard = LocalClipboardManager.current
    val haptic = LocalHapticFeedback.current
    var copied by remember(message.id) { mutableStateOf(false) }
    var showContextMenu by remember(message.id) { mutableStateOf(false) }

    LaunchedEffect(copied) {
        if (copied) {
            delay(2000L)
            copied = false
        }
    }

    val bubbleShape = RoundedCornerShape(
        topStart = 18.dp,
        topEnd = 18.dp,
        bottomStart = if (message.isUser) 18.dp else 6.dp,
        bottomEnd = if (message.isUser) 6.dp else 18.dp
    )

    val context = LocalContext.current
    val copyAction = {
        clipboard.setText(AnnotatedString(message.text))
        copied = true
    }
    val shareAction = {
        val snippet = formatShareSnippet(message)
        val shareIntent = Intent(Intent.ACTION_SEND).apply {
            type = "text/plain"
            putExtra(Intent.EXTRA_SUBJECT, context.getString(R.string.share_snippet_subject))
            putExtra(Intent.EXTRA_TEXT, snippet)
        }
        val chooserIntent = Intent.createChooser(
            shareIntent,
            context.getString(R.string.share_snippet_chooser_title)
        ).apply {
            if (context !is Activity) {
                addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
            }
        }
        try {
            context.startActivity(chooserIntent)
        } catch (_: ActivityNotFoundException) {
            // Ignore when no share handler is available
        }
    }

    Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = if (message.isUser) Arrangement.End else Arrangement.Start
    ) {
        Box {
            Surface(
                shape = bubbleShape,
                color = if (message.isUser) sanaColors.userBubble else MaterialTheme.colorScheme.surface,
                border = when {
                    isHighlighted -> BorderStroke(2.dp, sanaColors.tealAccent)
                    message.isUser -> null
                    isSpeaking -> BorderStroke(1.5.dp, sanaColors.tealAccent)
                    else -> BorderStroke(1.dp, sanaColors.cardBorder)
                },
                modifier = Modifier
                    .widthIn(max = 320.dp)
                    .clip(bubbleShape)
                    .combinedClickable(
                        onClick = {},
                        onLongClick = {
                            haptic.performHapticFeedback(HapticFeedbackType.LongPress)
                            showContextMenu = true
                        }
                    )
                    .testTag(if (message.isUser) "chat_user_bubble" else "chat_ai_bubble")
            ) {
                Column(
                    modifier = Modifier.padding(horizontal = 14.dp, vertical = 10.dp),
                    verticalArrangement = Arrangement.spacedBy(6.dp)
                ) {
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(4.dp),
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Text(
                            text = if (message.isUser) "أنت" else "سنا · Gemini",
                            style = MaterialTheme.typography.labelSmall.copy(fontWeight = FontWeight.Bold),
                            maxLines = 1,
                            overflow = TextOverflow.Ellipsis,
                            color = if (message.isUser) {
                                sanaColors.onUserBubble.copy(alpha = 0.78f)
                            } else {
                                sanaColors.tealDeep
                            },
                            modifier = Modifier.weight(1f)
                        )
                        if (copied) {
                            Text(
                                text = stringResource(R.string.message_copied),
                                style = MaterialTheme.typography.labelSmall,
                                color = if (message.isUser) {
                                    sanaColors.onUserBubble.copy(alpha = 0.85f)
                                } else {
                                    sanaColors.tealDeep
                                }
                            )
                        }
                        if (onToggleSpeak != null) {
                            IconButton(
                                onClick = onToggleSpeak,
                                modifier = Modifier
                                    .size(32.dp)
                                    .minimumInteractiveComponentSize()
                                    .testTag("tts_button_${message.id}")
                            ) {
                                Icon(
                                    imageVector = if (isSpeaking) {
                                        Icons.Filled.StopCircle
                                    } else {
                                        Icons.AutoMirrored.Filled.VolumeUp
                                    },
                                    contentDescription = if (isSpeaking) {
                                        stringResource(R.string.tts_stop_cd)
                                    } else {
                                        stringResource(R.string.tts_listen_cd)
                                    },
                                    tint = if (isSpeaking) {
                                        sanaColors.tealAccent
                                    } else {
                                        MaterialTheme.colorScheme.onSurfaceVariant
                                    },
                                    modifier = Modifier.size(18.dp)
                                )
                            }
                        }
                        IconButton(
                            onClick = copyAction,
                            modifier = Modifier
                                .size(32.dp)
                                .minimumInteractiveComponentSize()
                                .testTag("copy_message_${message.id}")
                        ) {
                            Icon(
                                imageVector = if (copied) Icons.Filled.Check else Icons.Filled.ContentCopy,
                                contentDescription = stringResource(R.string.copy_message_cd),
                                tint = when {
                                    copied && message.isUser -> sanaColors.onUserBubble
                                    copied -> sanaColors.tealDeep
                                    message.isUser -> sanaColors.onUserBubble.copy(alpha = 0.78f)
                                    else -> MaterialTheme.colorScheme.onSurfaceVariant
                                },
                                modifier = Modifier.size(16.dp)
                            )
                        }
                        IconButton(
                            onClick = shareAction,
                            modifier = Modifier
                                .size(32.dp)
                                .minimumInteractiveComponentSize()
                                .testTag("share_message_${message.id}")
                        ) {
                            Icon(
                                imageVector = Icons.Filled.Share,
                                contentDescription = stringResource(R.string.share_message_cd),
                                tint = if (message.isUser) {
                                    sanaColors.onUserBubble.copy(alpha = 0.78f)
                                } else {
                                    MaterialTheme.colorScheme.onSurfaceVariant
                                },
                                modifier = Modifier.size(16.dp)
                            )
                        }
                    }

                    if (message.isUser) {
                        Text(
                            text = message.text,
                            style = MaterialTheme.typography.bodyMedium,
                            color = sanaColors.onUserBubble
                        )
                    } else {
                        SanaMarkdownText(
                            markdown = message.text,
                            textColor = MaterialTheme.colorScheme.onSurface
                        )
                    }

                    if (isSpeaking && !message.isUser) {
                        Surface(
                            shape = RoundedCornerShape(999.dp),
                            color = sanaColors.tealTint,
                            modifier = Modifier
                                .semantics(mergeDescendants = true) {}
                                .testTag("tts_speaking_badge_${message.id}")
                        ) {
                            Row(
                                modifier = Modifier.padding(horizontal = 10.dp, vertical = 4.dp),
                                horizontalArrangement = Arrangement.spacedBy(4.dp),
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Icon(
                                    imageVector = Icons.AutoMirrored.Filled.VolumeUp,
                                    contentDescription = null,
                                    tint = sanaColors.tealDeep,
                                    modifier = Modifier.size(13.dp)
                                )
                                Text(
                                    text = stringResource(R.string.tts_speaking_badge),
                                    style = MaterialTheme.typography.labelSmall.copy(fontWeight = FontWeight.Bold),
                                    color = sanaColors.tealDeep
                                )
                            }
                        }
                    }

                    if (!message.isUser && !message.sourceTitle.isNullOrBlank()) {
                        Surface(
                            shape = RoundedCornerShape(999.dp),
                            color = sanaColors.tealTint
                        ) {
                            Row(
                                modifier = Modifier.padding(horizontal = 10.dp, vertical = 4.dp),
                                horizontalArrangement = Arrangement.spacedBy(4.dp),
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Icon(
                                    imageVector = Icons.Filled.Bookmark,
                                    contentDescription = null,
                                    tint = sanaColors.tealDeep,
                                    modifier = Modifier.size(13.dp)
                                )
                                Text(
                                    text = "المصدر: ${message.sourceTitle}",
                                    style = MaterialTheme.typography.labelSmall.copy(fontWeight = FontWeight.Bold),
                                    color = sanaColors.tealDeep
                                )
                            }
                        }
                    }
                }
            }

            DropdownMenu(
                expanded = showContextMenu,
                onDismissRequest = { showContextMenu = false },
                modifier = Modifier.testTag("bubble_context_menu_${message.id}")
            ) {
                if (onToggleSpeak != null) {
                    DropdownMenuItem(
                        text = {
                            Text(
                                if (isSpeaking) {
                                    stringResource(R.string.tts_stop_action)
                                } else {
                                    stringResource(R.string.tts_listen_action)
                                }
                            )
                        },
                        leadingIcon = {
                            Icon(
                                imageVector = if (isSpeaking) {
                                    Icons.Filled.StopCircle
                                } else {
                                    Icons.AutoMirrored.Filled.VolumeUp
                                },
                                contentDescription = null
                            )
                        },
                        onClick = {
                            onToggleSpeak()
                            showContextMenu = false
                        },
                        modifier = Modifier.testTag("tts_menu_item_${message.id}")
                    )
                }
                DropdownMenuItem(
                    text = { Text(stringResource(R.string.copy_message_action)) },
                    leadingIcon = {
                        Icon(
                            imageVector = Icons.Filled.ContentCopy,
                            contentDescription = null
                        )
                    },
                    onClick = {
                        copyAction()
                        showContextMenu = false
                    },
                    modifier = Modifier.testTag("copy_message_menu_item_${message.id}")
                )
                DropdownMenuItem(
                    text = { Text(stringResource(R.string.share_message_action)) },
                    leadingIcon = {
                        Icon(
                            imageVector = Icons.Filled.Share,
                            contentDescription = null
                        )
                    },
                    onClick = {
                        showContextMenu = false
                        shareAction()
                    },
                    modifier = Modifier.testTag("share_message_menu_item_${message.id}")
                )
            }
        }
    }
}

internal fun formatShareSnippet(message: ChatDisplayMessage): String {
    val speaker = if (message.isUser) "أنت" else "سنا"
    val body = message.text.trim()
    val sourceLine = message.sourceTitle
        ?.takeIf { it.isNotBlank() }
        ?.let { "\nالمصدر: $it" }
        .orEmpty()
    return "$speaker:\n$body$sourceLine"
}

@Composable
private fun StreamingBubbleItem(
    text: String,
    sourceTitle: String?,
    isSending: Boolean = true,
    onStopGeneration: (() -> Unit)? = null
) {
    val sanaColors = LocalSanaColors.current
    val pulseTransition = rememberInfiniteTransition(label = "streaming_bubble_pulse")
    val borderAlpha by pulseTransition.animateFloat(
        initialValue = 0.40f,
        targetValue = 1.0f,
        animationSpec = infiniteRepeatable(
            animation = tween(durationMillis = 850, easing = FastOutSlowInEasing),
            repeatMode = RepeatMode.Reverse
        ),
        label = "streaming_border_pulse"
    )

    Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.Start
    ) {
        Surface(
            shape = RoundedCornerShape(topStart = 18.dp, topEnd = 18.dp, bottomStart = 18.dp, bottomEnd = 6.dp),
            color = MaterialTheme.colorScheme.surface,
            border = BorderStroke(1.5.dp, sanaColors.tealAccent.copy(alpha = borderAlpha)),
            modifier = Modifier
                .widthIn(max = 320.dp)
                .testTag("chat_streaming_bubble")
        ) {
            Column(
                modifier = Modifier.padding(horizontal = 14.dp, vertical = 10.dp),
                verticalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.SpaceBetween,
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(6.dp)
                    ) {
                        com.example.ui.components.AiPulseOrb(
                            size = 20.dp,
                            modifier = Modifier.testTag("chat_loading_pulse")
                        )
                        Text(
                            text = "سنا · Gemini",
                            style = MaterialTheme.typography.labelSmall.copy(fontWeight = FontWeight.Bold),
                            color = sanaColors.tealDeep
                        )
                    }
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(6.dp)
                    ) {
                        TypingDotsIndicator(
                            dotColor = sanaColors.tealAccent,
                            modifier = Modifier.testTag("chat_typing_dots")
                        )
                        if (isSending && onStopGeneration != null) {
                            IconButton(
                                onClick = onStopGeneration,
                                modifier = Modifier
                                    .minimumInteractiveComponentSize()
                                    .size(36.dp)
                                    .testTag("stop_generation_button")
                            ) {
                                Icon(
                                    imageVector = Icons.Filled.StopCircle,
                                    contentDescription = "إيقاف التوليد",
                                    tint = sanaColors.errorText,
                                    modifier = Modifier.size(18.dp)
                                )
                            }
                        }
                    }
                }

                if (text.isBlank()) {
                    Text(
                        text = stringResource(R.string.ai_typing_status),
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                        modifier = Modifier.testTag("chat_typing_status_text")
                    )
                } else {
                    SanaMarkdownText(
                        markdown = text,
                        textColor = MaterialTheme.colorScheme.onSurface
                    )
                }

                if (!sourceTitle.isNullOrBlank()) {
                    Text(
                        text = "المصدر: $sourceTitle",
                        style = MaterialTheme.typography.labelSmall,
                        color = sanaColors.tealDeep
                    )
                }
            }
        }
    }
}

@Composable
private fun TypingDotsIndicator(
    dotColor: androidx.compose.ui.graphics.Color,
    modifier: Modifier = Modifier
) {
    val transition = rememberInfiniteTransition(label = "sana_typing_dots")
    val dotOffsets = List(3) { index ->
        transition.animateFloat(
            initialValue = 0f,
            targetValue = -6f,
            animationSpec = infiniteRepeatable(
                animation = tween(
                    durationMillis = 420,
                    easing = FastOutSlowInEasing
                ),
                repeatMode = RepeatMode.Reverse,
                initialStartOffset = StartOffset(index * 140)
            ),
            label = "typing_dot_offset_$index"
        )
    }
    val dotAlphas = List(3) { index ->
        transition.animateFloat(
            initialValue = 0.35f,
            targetValue = 1f,
            animationSpec = infiniteRepeatable(
                animation = tween(
                    durationMillis = 420,
                    easing = FastOutSlowInEasing
                ),
                repeatMode = RepeatMode.Reverse,
                initialStartOffset = StartOffset(index * 140)
            ),
            label = "typing_dot_alpha_$index"
        )
    }

    Row(
        modifier = modifier,
        horizontalArrangement = Arrangement.spacedBy(5.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        repeat(3) { index ->
            Box(
                modifier = Modifier
                    .size(7.dp)
                    .graphicsLayer {
                        translationY = dotOffsets[index].value
                        alpha = dotAlphas[index].value
                    }
                    .clip(CircleShape)
                    .background(dotColor)
            )
        }
    }
}
