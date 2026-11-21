package com.example.ui.screens

import androidx.activity.compose.BackHandler
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.AutoAwesome
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.filled.DeleteOutline
import androidx.compose.material.icons.filled.EmojiEvents
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.example.data.model.CourseProgress
import com.example.data.model.CourseUi
import com.example.ui.LearnSubView
import com.example.ui.LearnUiSession
import com.example.ui.SanaViewModel
import com.example.ui.theme.LocalSanaColors
import kotlin.math.roundToInt

@Composable
fun LearnScreen(
    viewModel: SanaViewModel,
    modifier: Modifier = Modifier
) {
    val sanaColors = LocalSanaColors.current
    val courses by viewModel.allCoursesState.collectAsStateWithLifecycle()
    val progressMap by viewModel.courseProgressState.collectAsStateWithLifecycle()
    val session by viewModel.learnSession.collectAsStateWithLifecycle()
    val activeCourse by viewModel.activeCourse.collectAsStateWithLifecycle()
    val isGenerating by viewModel.isGeneratingCourse.collectAsStateWithLifecycle()
    val courseError by viewModel.courseError.collectAsStateWithLifecycle()

    var showGeneratorDialog by rememberSaveable { mutableStateOf(false) }
    var customTopic by rememberSaveable { mutableStateOf("") }

    if (showGeneratorDialog) {
        AlertDialog(
            onDismissRequest = { if (!isGenerating) showGeneratorDialog = false },
            title = {
                Text("إنشاء دورة بالذكاء الاصطناعي ✨", style = MaterialTheme.typography.titleLarge)
            },
            text = {
                Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
                    Text(
                        text = "اكتب أي موضوع تود تعلّمه وستبني سنا دورة من 3 دروس واختبار تفاعلي مخصص.",
                        style = MaterialTheme.typography.bodyMedium,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                    OutlinedTextField(
                        value = customTopic,
                        onValueChange = { customTopic = it },
                        placeholder = { Text("مثال: الأمن السيبراني، علم الفلك، التسويق الرقمي") },
                        singleLine = true,
                        modifier = Modifier
                            .fillMaxWidth()
                            .testTag("custom_course_topic_input")
                    )
                }
            },
            confirmButton = {
                Button(
                    onClick = {
                        viewModel.generateCustomCourse(customTopic)
                        customTopic = ""
                        showGeneratorDialog = false
                    },
                    enabled = customTopic.trim().length >= 3 && !isGenerating,
                    modifier = Modifier.testTag("confirm_generate_course_button")
                ) {
                    Text("أنشئ الدورة")
                }
            },
            dismissButton = {
                TextButton(
                    onClick = { showGeneratorDialog = false },
                    enabled = !isGenerating
                ) {
                    Text("إلغاء")
                }
            }
        )
    }

    Column(
        modifier = modifier
            .fillMaxSize()
            .background(sanaColors.chatCanvas)
    ) {
        AnimatedVisibility(visible = !courseError.isNullOrBlank()) {
            Surface(
                color = sanaColors.errorSurface,
                modifier = Modifier
                    .fillMaxWidth()
                    .clickable { viewModel.dismissCourseError() }
            ) {
                Text(
                    text = "${courseError.orEmpty()} (اضغط للإخفاء)",
                    style = MaterialTheme.typography.bodySmall,
                    color = sanaColors.errorText,
                    modifier = Modifier.padding(horizontal = 16.dp, vertical = 10.dp)
                )
            }
        }

        val currentCourse = activeCourse
        when {
            session.subView == LearnSubView.LIST || currentCourse == null -> {
                CourseListView(
                    courses = courses,
                    progressMap = progressMap,
                    isGenerating = isGenerating,
                    onOpenCourse = { viewModel.openCourse(it) },
                    onOpenGenerator = { showGeneratorDialog = true },
                    onDeleteCustom = { viewModel.deleteCustomCourse(it) }
                )
            }

            session.subView == LearnSubView.LESSON -> {
                BackHandler { viewModel.previousLesson() }
                LessonReaderView(
                    course = currentCourse,
                    lessonIndex = session.lessonIndex,
                    onBackToList = { viewModel.backToCourseList() },
                    onPrevious = { viewModel.previousLesson() },
                    onNext = { viewModel.nextLessonOrStartQuiz(currentCourse) }
                )
            }

            session.subView == LearnSubView.QUIZ -> {
                BackHandler { viewModel.backToCourseList() }
                QuizRunnerView(
                    course = currentCourse,
                    session = session,
                    onBackToList = { viewModel.backToCourseList() },
                    onSelectOption = { idx -> viewModel.selectQuizOption(currentCourse, idx) },
                    onNextQuestion = { viewModel.nextQuestionOrFinish(currentCourse) }
                )
            }

            session.subView == LearnSubView.RESULT -> {
                BackHandler { viewModel.backToCourseList() }
                QuizResultView(
                    course = currentCourse,
                    score = session.quizScore,
                    total = currentCourse.quiz.size,
                    onRetry = { viewModel.retryQuiz() },
                    onBackToList = { viewModel.backToCourseList() }
                )
            }
        }
    }
}

@Composable
private fun CourseListView(
    courses: List<CourseUi>,
    progressMap: Map<String, CourseProgress>,
    isGenerating: Boolean,
    onOpenCourse: (CourseUi) -> Unit,
    onOpenGenerator: () -> Unit,
    onDeleteCustom: (String) -> Unit
) {
    val sanaColors = LocalSanaColors.current

    LazyColumn(
        modifier = Modifier.fillMaxSize(),
        contentPadding = PaddingValues(16.dp),
        verticalArrangement = Arrangement.spacedBy(12.dp)
    ) {
        item(key = "intro") {
            Text(
                text = "اختر دورة لتقرأ دروسها القصيرة وتجتاز الاختبار التفاعلي، أو اطلب من سنا تأليف دورة جديدة فورًا.",
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
        }

        item(key = "ai_generator_card") {
            Card(
                shape = MaterialTheme.shapes.large,
                colors = CardDefaults.cardColors(containerColor = sanaColors.heroBackground),
                modifier = Modifier
                    .fillMaxWidth()
                    .clickable(enabled = !isGenerating) { onOpenGenerator() }
                    .testTag("open_ai_course_generator")
            ) {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(16.dp),
                    horizontalArrangement = Arrangement.spacedBy(12.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Box(
                        modifier = Modifier
                            .size(44.dp)
                            .clip(RoundedCornerShape(12.dp))
                            .background(sanaColors.tealAccent.copy(alpha = 0.22f)),
                        contentAlignment = Alignment.Center
                    ) {
                        if (isGenerating) {
                            CircularProgressIndicator(
                                modifier = Modifier.size(22.dp),
                                color = sanaColors.mintGlow,
                                strokeWidth = 2.5.dp
                            )
                        } else {
                            Icon(
                                imageVector = Icons.Filled.AutoAwesome,
                                contentDescription = "توليد دورة بالذكاء الاصطناعي",
                                tint = sanaColors.mintGlow
                            )
                        }
                    }
                    Column(modifier = Modifier.weight(1f)) {
                        Text(
                            text = if (isGenerating) "جاري تأليف الدورة بـ Gemini…" else "أنشئ دورة جديدة بالذكاء الاصطناعي ✨",
                            style = MaterialTheme.typography.titleMedium,
                            color = sanaColors.heroOnBackground
                        )
                        Text(
                            text = "اكتب أي موضوع لتوليد 3 دروس واختبار فوري محفوظ في حسابك.",
                            style = MaterialTheme.typography.bodySmall,
                            color = sanaColors.heroSubtext
                        )
                    }
                }
            }
        }

        itemsIndexed(courses, key = { _, item -> item.id }) { index, course ->
            val prog = progressMap[course.id]
            val doneLessons = prog?.completedLessons?.size ?: 0
            val totalSteps = (course.lessons.size + 1).coerceAtLeast(1)
            val completedSteps = doneLessons + (if (prog?.quizBestScore != null) 1 else 0)
            val pct = ((completedSteps.toFloat() / totalSteps) * 100f).roundToInt().coerceIn(0, 100)

            val iconBg = when (index % 3) {
                0 -> sanaColors.tealTint
                1 -> sanaColors.amberTint
                else -> sanaColors.indigoTint
            }

            Card(
                shape = MaterialTheme.shapes.medium,
                colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
                border = BorderStroke(1.dp, sanaColors.cardBorder),
                modifier = Modifier
                    .fillMaxWidth()
                    .clickable { onOpenCourse(course) }
                    .testTag("course_card_${course.id}")
            ) {
                Column(modifier = Modifier.padding(16.dp)) {
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(12.dp)
                    ) {
                        Box(
                            modifier = Modifier
                                .size(46.dp)
                                .clip(RoundedCornerShape(13.dp))
                                .background(iconBg),
                            contentAlignment = Alignment.Center
                        ) {
                            Text(text = course.iconEmoji, fontSize = 22.sp)
                        }

                        Column(modifier = Modifier.weight(1f)) {
                            Row(
                                verticalAlignment = Alignment.CenterVertically,
                                horizontalArrangement = Arrangement.spacedBy(6.dp)
                            ) {
                                Text(
                                    text = course.title,
                                    style = MaterialTheme.typography.titleMedium,
                                    color = MaterialTheme.colorScheme.onSurface,
                                    modifier = Modifier.weight(1f, fill = false)
                                )
                                if (course.isCustom) {
                                    Surface(
                                        shape = RoundedCornerShape(999.dp),
                                        color = sanaColors.tealTint
                                    ) {
                                        Text(
                                            text = "AI",
                                            style = MaterialTheme.typography.labelSmall,
                                            color = sanaColors.tealDeep,
                                            modifier = Modifier.padding(horizontal = 6.dp, vertical = 2.dp)
                                        )
                                    }
                                }
                            }
                            Text(
                                text = course.description,
                                style = MaterialTheme.typography.bodySmall,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                        }

                        Column(horizontalAlignment = Alignment.End) {
                            Text(
                                text = "$pct%",
                                style = MaterialTheme.typography.titleSmall.copy(fontWeight = FontWeight.Bold),
                                color = sanaColors.tealDeep
                            )
                            Text(
                                text = if (prog?.quizBestScore != null) {
                                    "الاختبار: ${prog.quizBestScore}/${course.quiz.size}"
                                } else {
                                    "$doneLessons/${course.lessons.size} دروس"
                                },
                                style = MaterialTheme.typography.labelSmall,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                        }

                        if (course.isCustom) {
                            IconButton(
                                onClick = { onDeleteCustom(course.id) },
                                modifier = Modifier.testTag("delete_course_${course.id}")
                            ) {
                                Icon(
                                    imageVector = Icons.Filled.DeleteOutline,
                                    contentDescription = "حذف الدورة",
                                    tint = sanaColors.errorText
                                )
                            }
                        }
                    }

                    Spacer(modifier = Modifier.height(12.dp))
                    LinearProgressIndicator(
                        progress = { pct / 100f },
                        color = sanaColors.tealAccent,
                        trackColor = MaterialTheme.colorScheme.surfaceVariant,
                        strokeCap = StrokeCap.Round,
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(6.dp)
                    )
                }
            }
        }
    }
}

@Composable
private fun LessonReaderView(
    course: CourseUi,
    lessonIndex: Int,
    onBackToList: () -> Unit,
    onPrevious: () -> Unit,
    onNext: () -> Unit
) {
    val sanaColors = LocalSanaColors.current
    val lesson = course.lessons.getOrNull(lessonIndex) ?: return

    Column(modifier = Modifier.fillMaxSize()) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .background(MaterialTheme.colorScheme.surface)
                .padding(horizontal = 8.dp, vertical = 6.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            IconButton(
                onClick = onBackToList,
                modifier = Modifier.testTag("lesson_back_to_courses")
            ) {
                Icon(
                    imageVector = Icons.AutoMirrored.Filled.ArrowBack,
                    contentDescription = "عودة للدورات"
                )
            }
            Text(
                text = course.title,
                style = MaterialTheme.typography.titleMedium,
                modifier = Modifier.weight(1f)
            )
            Surface(
                shape = RoundedCornerShape(999.dp),
                color = sanaColors.tealTint
            ) {
                Text(
                    text = "درس ${lessonIndex + 1} من ${course.lessons.size}",
                    style = MaterialTheme.typography.labelMedium,
                    color = sanaColors.tealDeep,
                    modifier = Modifier.padding(horizontal = 12.dp, vertical = 6.dp)
                )
            }
        }
        HorizontalDivider(color = sanaColors.cardBorder)

        Column(
            modifier = Modifier
                .weight(1f)
                .verticalScroll(rememberScrollState())
                .padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            Card(
                shape = MaterialTheme.shapes.large,
                colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
                border = BorderStroke(1.dp, sanaColors.cardBorder),
                modifier = Modifier.fillMaxWidth()
            ) {
                Column(
                    modifier = Modifier.padding(20.dp),
                    verticalArrangement = Arrangement.spacedBy(12.dp)
                ) {
                    Text(
                        text = lesson.title,
                        style = MaterialTheme.typography.headlineSmall,
                        color = MaterialTheme.colorScheme.onSurface
                    )
                    Text(
                        text = lesson.body,
                        style = MaterialTheme.typography.bodyLarge,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
            }
        }

        HorizontalDivider(color = sanaColors.cardBorder)
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .background(MaterialTheme.colorScheme.surface)
                .padding(horizontal = 16.dp, vertical = 12.dp),
            horizontalArrangement = Arrangement.spacedBy(10.dp)
        ) {
            if (lessonIndex > 0) {
                OutlinedButton(
                    onClick = onPrevious,
                    shape = RoundedCornerShape(13.dp),
                    modifier = Modifier
                        .weight(1f)
                        .height(48.dp)
                        .testTag("lesson_previous_button")
                ) {
                    Text("السابق")
                }
            }
            Button(
                onClick = onNext,
                shape = RoundedCornerShape(13.dp),
                modifier = Modifier
                    .weight(1f)
                    .height(48.dp)
                    .testTag("lesson_next_button")
            ) {
                Text(
                    text = if (lessonIndex == course.lessons.lastIndex) "ابدأ الاختبار ✍️" else "الدرس التالي"
                )
            }
        }
    }
}

@Composable
private fun QuizRunnerView(
    course: CourseUi,
    session: LearnUiSession,
    onBackToList: () -> Unit,
    onSelectOption: (Int) -> Unit,
    onNextQuestion: () -> Unit
) {
    val sanaColors = LocalSanaColors.current
    val question = course.quiz.getOrNull(session.quizIndex) ?: return
    val answered = session.selectedOption != null

    Column(modifier = Modifier.fillMaxSize()) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .background(MaterialTheme.colorScheme.surface)
                .padding(horizontal = 8.dp, vertical = 6.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            IconButton(
                onClick = onBackToList,
                modifier = Modifier.testTag("quiz_back_to_courses")
            ) {
                Icon(
                    imageVector = Icons.AutoMirrored.Filled.ArrowBack,
                    contentDescription = "عودة للدورات"
                )
            }
            Text(
                text = "الاختبار — ${course.title}",
                style = MaterialTheme.typography.titleMedium,
                modifier = Modifier.weight(1f)
            )
            Surface(
                shape = RoundedCornerShape(999.dp),
                color = sanaColors.amberTint
            ) {
                Text(
                    text = "سؤال ${session.quizIndex + 1} / ${course.quiz.size}",
                    style = MaterialTheme.typography.labelMedium,
                    color = sanaColors.amberDeep,
                    modifier = Modifier.padding(horizontal = 12.dp, vertical = 6.dp)
                )
            }
        }
        HorizontalDivider(color = sanaColors.cardBorder)

        Column(
            modifier = Modifier
                .weight(1f)
                .verticalScroll(rememberScrollState())
                .padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            Card(
                shape = MaterialTheme.shapes.large,
                colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
                border = BorderStroke(1.dp, sanaColors.cardBorder),
                modifier = Modifier.fillMaxWidth()
            ) {
                Column(
                    modifier = Modifier.padding(18.dp),
                    verticalArrangement = Arrangement.spacedBy(10.dp)
                ) {
                    Text(
                        text = question.question,
                        style = MaterialTheme.typography.titleMedium,
                        color = MaterialTheme.colorScheme.onSurface
                    )

                    question.options.forEachIndexed { idx, optText ->
                        val isSelected = session.selectedOption == idx
                        val isCorrect = idx == question.correctIndex

                        val bgColor = when {
                            !answered -> MaterialTheme.colorScheme.surface
                            isCorrect -> sanaColors.tealTint
                            isSelected -> sanaColors.errorSurface
                            else -> MaterialTheme.colorScheme.surface
                        }
                        val borderColor = when {
                            !answered -> sanaColors.cardBorder
                            isCorrect -> sanaColors.tealAccent
                            isSelected -> sanaColors.errorText
                            else -> sanaColors.cardBorder
                        }
                        val textColor = when {
                            !answered -> MaterialTheme.colorScheme.onSurface
                            isCorrect -> sanaColors.tealDeep
                            isSelected -> sanaColors.errorText
                            else -> MaterialTheme.colorScheme.onSurfaceVariant
                        }

                        Surface(
                            shape = RoundedCornerShape(13.dp),
                            color = bgColor,
                            border = BorderStroke(1.5.dp, borderColor),
                            modifier = Modifier
                                .fillMaxWidth()
                                .clickable(enabled = !answered) { onSelectOption(idx) }
                                .testTag("quiz_option_$idx")
                        ) {
                            Row(
                                modifier = Modifier.padding(horizontal = 16.dp, vertical = 14.dp),
                                verticalAlignment = Alignment.CenterVertically,
                                horizontalArrangement = Arrangement.SpaceBetween
                            ) {
                                Text(
                                    text = optText,
                                    style = MaterialTheme.typography.bodyMedium.copy(
                                        fontWeight = if (answered && isCorrect) FontWeight.Bold else FontWeight.Normal
                                    ),
                                    color = textColor,
                                    modifier = Modifier.weight(1f)
                                )
                                if (answered && isCorrect) {
                                    Icon(
                                        imageVector = Icons.Filled.CheckCircle,
                                        contentDescription = "إجابة صحيحة",
                                        tint = sanaColors.tealDeep,
                                        modifier = Modifier.size(18.dp)
                                    )
                                }
                            }
                        }
                    }

                    if (answered && question.explanation.isNotBlank()) {
                        val wasCorrect = session.selectedOption == question.correctIndex
                        Surface(
                            shape = RoundedCornerShape(12.dp),
                            color = if (wasCorrect) sanaColors.tealTint else sanaColors.errorSurface
                        ) {
                            Text(
                                text = question.explanation,
                                style = MaterialTheme.typography.bodySmall,
                                color = if (wasCorrect) sanaColors.tealDeep else sanaColors.errorText,
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .padding(12.dp)
                            )
                        }
                    }
                }
            }
        }

        if (answered) {
            HorizontalDivider(color = sanaColors.cardBorder)
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .background(MaterialTheme.colorScheme.surface)
                    .padding(horizontal = 16.dp, vertical = 12.dp)
            ) {
                Button(
                    onClick = onNextQuestion,
                    shape = RoundedCornerShape(13.dp),
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(48.dp)
                        .testTag("quiz_next_button")
                ) {
                    Text(
                        if (session.quizIndex == course.quiz.lastIndex) "عرض النتيجة" else "السؤال التالي"
                    )
                }
            }
        }
    }
}

@Composable
private fun QuizResultView(
    course: CourseUi,
    score: Int,
    total: Int,
    onRetry: () -> Unit,
    onBackToList: () -> Unit
) {
    val sanaColors = LocalSanaColors.current
    val pct = if (total > 0) ((score.toFloat() / total) * 100f).roundToInt() else 0
    val msg = when {
        pct == 100 -> "ممتاز! درجة كاملة وإتقان رائع 🎉"
        pct >= 60 -> "جيد جدًا! يمكنك مراجعة الدروس لتصل للدرجة الكاملة."
        else -> "بداية طيبة — أعد قراءة الدروس وحاول مجددًا."
    }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(24.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.Center
    ) {
        Card(
            shape = MaterialTheme.shapes.extraLarge,
            colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
            border = BorderStroke(1.dp, sanaColors.cardBorder),
            modifier = Modifier.widthIn(max = 420.dp)
        ) {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(28.dp),
                horizontalAlignment = Alignment.CenterHorizontally,
                verticalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                Box(
                    modifier = Modifier
                        .size(64.dp)
                        .clip(RoundedCornerShape(18.dp))
                        .background(sanaColors.tealTint),
                    contentAlignment = Alignment.Center
                ) {
                    Icon(
                        imageVector = Icons.Filled.EmojiEvents,
                        contentDescription = null,
                        tint = sanaColors.tealDeep,
                        modifier = Modifier.size(34.dp)
                    )
                }

                Text(
                    text = course.title,
                    style = MaterialTheme.typography.titleMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )

                Text(
                    text = "$score / $total",
                    style = MaterialTheme.typography.displaySmall,
                    color = sanaColors.tealDeep
                )

                Text(
                    text = msg,
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurface,
                    textAlign = TextAlign.Center
                )

                Spacer(modifier = Modifier.height(8.dp))

                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(10.dp)
                ) {
                    OutlinedButton(
                        onClick = onRetry,
                        shape = RoundedCornerShape(13.dp),
                        modifier = Modifier
                            .weight(1f)
                            .height(48.dp)
                            .testTag("quiz_retry_button")
                    ) {
                        Text("إعادة الاختبار")
                    }
                    Button(
                        onClick = onBackToList,
                        shape = RoundedCornerShape(13.dp),
                        colors = ButtonDefaults.buttonColors(containerColor = MaterialTheme.colorScheme.primary),
                        modifier = Modifier
                            .weight(1f)
                            .height(48.dp)
                            .testTag("quiz_done_courses_button")
                    ) {
                        Text("عودة للدورات")
                    }
                }
            }
        }
    }
}
