package com.example.ui.components

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.tween
import androidx.compose.animation.expandVertically
import androidx.compose.animation.fadeIn
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.AutoAwesome
import androidx.compose.material.icons.filled.Cancel
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.EmojiEvents
import androidx.compose.material.icons.filled.Psychology
import androidx.compose.material.icons.filled.Quiz
import androidx.compose.material.icons.filled.Refresh
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.ModalBottomSheet
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.minimumInteractiveComponentSize
import androidx.compose.material3.rememberModalBottomSheetState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import com.example.R
import com.example.data.model.ChatQuizUiState
import com.example.data.model.QuizQuestion
import com.example.ui.theme.LocalSanaColors
import kotlin.math.roundToInt

private val arabicOptionLetters = listOf("أ", "ب", "ج", "د")

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ChatQuizSheet(
    quizState: ChatQuizUiState,
    onSelectOption: (Int) -> Unit,
    onNextQuestion: () -> Unit,
    onRestartQuiz: () -> Unit,
    onRegenerateQuiz: () -> Unit,
    onDismiss: () -> Unit,
    onAskAboutQuestion: ((QuizQuestion, Int?) -> Unit)? = null
) {
    if (quizState is ChatQuizUiState.Hidden) return

    val sheetState = rememberModalBottomSheetState(skipPartiallyExpanded = true)
    ModalBottomSheet(
        onDismissRequest = onDismiss,
        sheetState = sheetState,
        containerColor = MaterialTheme.colorScheme.background,
        modifier = Modifier.testTag("chat_quiz_sheet")
    ) {
        ChatQuizSheetBody(
            quizState = quizState,
            onSelectOption = onSelectOption,
            onNextQuestion = onNextQuestion,
            onRestartQuiz = onRestartQuiz,
            onRegenerateQuiz = onRegenerateQuiz,
            onDismiss = onDismiss,
            onAskAboutQuestion = onAskAboutQuestion
        )
    }
}

@Composable
internal fun ChatQuizSheetBody(
    quizState: ChatQuizUiState,
    onSelectOption: (Int) -> Unit,
    onNextQuestion: () -> Unit,
    onRestartQuiz: () -> Unit,
    onRegenerateQuiz: () -> Unit,
    onDismiss: () -> Unit,
    onAskAboutQuestion: ((QuizQuestion, Int?) -> Unit)? = null,
    modifier: Modifier = Modifier
) {
    val sanaColors = LocalSanaColors.current

    Column(
        modifier = modifier
            .fillMaxWidth()
            .navigationBarsPadding(),
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        Column(
            modifier = Modifier
                .widthIn(max = 640.dp)
                .fillMaxWidth()
        ) {
            // Sheet Header
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 20.dp, vertical = 6.dp),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                Box(
                    modifier = Modifier
                        .size(40.dp)
                        .clip(RoundedCornerShape(12.dp))
                        .background(sanaColors.amberTint),
                    contentAlignment = Alignment.Center
                ) {
                    Icon(
                        imageVector = Icons.Filled.Quiz,
                        contentDescription = null,
                        tint = sanaColors.amberDeep,
                        modifier = Modifier.size(22.dp)
                    )
                }
                Column(modifier = Modifier.weight(1f)) {
                    Text(
                        text = stringResource(R.string.chat_quiz_title),
                        style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.Bold),
                        color = MaterialTheme.colorScheme.onSurface
                    )
                    if (quizState is ChatQuizUiState.Ready) {
                        Text(
                            text = quizState.quiz.topicTitle,
                            style = MaterialTheme.typography.bodySmall,
                            color = sanaColors.tealDeep,
                            modifier = Modifier.testTag("chat_quiz_topic_title")
                        )
                    }
                }
                IconButton(
                    onClick = onDismiss,
                    modifier = Modifier
                        .minimumInteractiveComponentSize()
                        .testTag("chat_quiz_close_button")
                ) {
                    Icon(
                        imageVector = Icons.Filled.Close,
                        contentDescription = stringResource(R.string.chat_quiz_close_cd),
                        tint = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
            }

            HorizontalDivider(color = sanaColors.cardBorder)

            when (quizState) {
                is ChatQuizUiState.Hidden -> Unit

                is ChatQuizUiState.Generating -> {
                    Column(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(horizontal = 20.dp, vertical = 24.dp),
                        verticalArrangement = Arrangement.spacedBy(14.dp),
                        horizontalAlignment = Alignment.CenterHorizontally
                    ) {
                        AiPulseLoadingCard(
                            title = stringResource(R.string.chat_quiz_generating),
                            streamingPreview = quizState.streamingPreview,
                            modifier = Modifier.testTag("chat_quiz_loading")
                        )
                        Text(
                            text = stringResource(R.string.chat_quiz_generating_subtitle),
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                            textAlign = TextAlign.Center
                        )
                        OutlinedButton(
                            onClick = onDismiss,
                            shape = RoundedCornerShape(12.dp),
                            modifier = Modifier
                                .minimumInteractiveComponentSize()
                                .testTag("chat_quiz_cancel_button")
                        ) {
                            Text(stringResource(R.string.cancel))
                        }
                    }
                }

                is ChatQuizUiState.Error -> {
                    Column(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(horizontal = 20.dp, vertical = 20.dp),
                        verticalArrangement = Arrangement.spacedBy(14.dp)
                    ) {
                        Surface(
                            shape = MaterialTheme.shapes.medium,
                            color = sanaColors.errorSurface,
                            modifier = Modifier.fillMaxWidth()
                        ) {
                            Text(
                                text = quizState.message,
                                style = MaterialTheme.typography.bodyMedium,
                                color = sanaColors.errorText,
                                modifier = Modifier
                                    .padding(16.dp)
                                    .testTag("chat_quiz_error_text")
                            )
                        }
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.spacedBy(10.dp)
                        ) {
                            OutlinedButton(
                                onClick = onDismiss,
                                shape = RoundedCornerShape(13.dp),
                                modifier = Modifier
                                    .weight(1f)
                                    .height(48.dp)
                                    .minimumInteractiveComponentSize()
                                    .testTag("chat_quiz_error_close_button")
                            ) {
                                Text(stringResource(R.string.cancel))
                            }
                            Button(
                                onClick = onRegenerateQuiz,
                                shape = RoundedCornerShape(13.dp),
                                modifier = Modifier
                                    .weight(1f)
                                    .height(48.dp)
                                    .minimumInteractiveComponentSize()
                                    .testTag("chat_quiz_retry_generate_button")
                            ) {
                                Text(stringResource(R.string.chat_quiz_retry))
                            }
                        }
                    }
                }

                is ChatQuizUiState.Ready -> {
                    if (quizState.showSummary) {
                        ChatQuizSummaryContent(
                            state = quizState,
                            onRestartQuiz = onRestartQuiz,
                            onRegenerateQuiz = onRegenerateQuiz,
                            onAskAboutQuestion = onAskAboutQuestion
                        )
                    } else {
                        ChatQuizQuestionContent(
                            state = quizState,
                            onSelectOption = onSelectOption,
                            onNextQuestion = onNextQuestion,
                            onAskAboutQuestion = onAskAboutQuestion
                        )
                    }
                }
            }
        }
    }
}

@Composable
private fun ChatQuizQuestionContent(
    state: ChatQuizUiState.Ready,
    onSelectOption: (Int) -> Unit,
    onNextQuestion: () -> Unit,
    onAskAboutQuestion: ((QuizQuestion, Int?) -> Unit)?
) {
    val sanaColors = LocalSanaColors.current
    val question = state.currentQuestion ?: return
    val total = state.quiz.questions.size
    val selectedOption = state.selectedOptionForCurrent
    val answered = selectedOption != null
    val progressTarget = ((state.currentIndex + if (answered) 1f else 0.2f) / total.coerceAtLeast(1))
        .coerceIn(0f, 1f)
    val animatedProgress by animateFloatAsState(
        targetValue = progressTarget,
        animationSpec = tween(durationMillis = 350),
        label = "chat_quiz_progress"
    )

    Column(modifier = Modifier.fillMaxWidth()) {
        Column(
            modifier = Modifier
                .weight(1f, fill = false)
                .fillMaxWidth()
                .verticalScroll(rememberScrollState())
                .padding(horizontal = 20.dp, vertical = 14.dp),
            verticalArrangement = Arrangement.spacedBy(14.dp)
        ) {
        // Counter + Score Row
        Row(
            modifier = Modifier.fillMaxWidth(),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.SpaceBetween
        ) {
            Surface(
                shape = RoundedCornerShape(999.dp),
                color = sanaColors.amberTint
            ) {
                Text(
                    text = stringResource(
                        R.string.chat_quiz_question_counter_format,
                        state.currentIndex + 1,
                        total
                    ),
                    style = MaterialTheme.typography.labelMedium.copy(fontWeight = FontWeight.Bold),
                    color = sanaColors.amberDeep,
                    modifier = Modifier
                        .padding(horizontal = 12.dp, vertical = 6.dp)
                        .testTag("chat_quiz_counter")
                )
            }
            Surface(
                shape = RoundedCornerShape(999.dp),
                color = sanaColors.tealTint
            ) {
                Text(
                    text = stringResource(R.string.chat_quiz_score_format, state.score),
                    style = MaterialTheme.typography.labelMedium.copy(fontWeight = FontWeight.Bold),
                    color = sanaColors.tealDeep,
                    modifier = Modifier
                        .padding(horizontal = 12.dp, vertical = 6.dp)
                        .testTag("chat_quiz_score_badge")
                )
            }
        }

        LinearProgressIndicator(
            progress = { animatedProgress },
            color = sanaColors.tealAccent,
            trackColor = MaterialTheme.colorScheme.surfaceVariant,
            strokeCap = StrokeCap.Round,
            modifier = Modifier
                .fillMaxWidth()
                .height(7.dp)
        )

        // Question Card
        Card(
            shape = MaterialTheme.shapes.large,
            colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
            border = BorderStroke(1.dp, sanaColors.cardBorder),
            modifier = Modifier.fillMaxWidth()
        ) {
            Column(
                modifier = Modifier.padding(18.dp),
                verticalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                Text(
                    text = question.question,
                    style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.SemiBold),
                    color = MaterialTheme.colorScheme.onSurface,
                    modifier = Modifier.testTag("chat_quiz_question_text")
                )

                question.options.forEachIndexed { idx, optText ->
                    val isSelected = selectedOption == idx
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
                    val badgeBg = when {
                        !answered -> sanaColors.indigoTint
                        isCorrect -> sanaColors.tealAccent
                        isSelected -> sanaColors.errorText
                        else -> MaterialTheme.colorScheme.surfaceVariant
                    }
                    val badgeTextColor = when {
                        !answered -> MaterialTheme.colorScheme.secondary
                        isCorrect || isSelected -> MaterialTheme.colorScheme.onPrimary
                        else -> MaterialTheme.colorScheme.onSurfaceVariant
                    }
                    val textColor = when {
                        !answered -> MaterialTheme.colorScheme.onSurface
                        isCorrect -> sanaColors.tealDeep
                        isSelected -> sanaColors.errorText
                        else -> MaterialTheme.colorScheme.onSurfaceVariant
                    }

                    Surface(
                        shape = RoundedCornerShape(14.dp),
                        color = bgColor,
                        border = BorderStroke(1.5.dp, borderColor),
                        modifier = Modifier
                            .fillMaxWidth()
                            .minimumInteractiveComponentSize()
                            .clickable(enabled = !answered) { onSelectOption(idx) }
                            .testTag("chat_quiz_option_$idx")
                    ) {
                        Row(
                            modifier = Modifier.padding(horizontal = 14.dp, vertical = 12.dp),
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(12.dp)
                        ) {
                            Box(
                                modifier = Modifier
                                    .size(30.dp)
                                    .clip(RoundedCornerShape(9.dp))
                                    .background(badgeBg),
                                contentAlignment = Alignment.Center
                            ) {
                                Text(
                                    text = arabicOptionLetters.getOrElse(idx) { "${idx + 1}" },
                                    style = MaterialTheme.typography.labelMedium.copy(fontWeight = FontWeight.Bold),
                                    color = badgeTextColor
                                )
                            }
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
                                    contentDescription = stringResource(R.string.chat_quiz_correct_badge),
                                    tint = sanaColors.tealDeep,
                                    modifier = Modifier.size(20.dp)
                                )
                            } else if (answered && isSelected) {
                                Icon(
                                    imageVector = Icons.Filled.Cancel,
                                    contentDescription = stringResource(R.string.chat_quiz_incorrect_badge),
                                    tint = sanaColors.errorText,
                                    modifier = Modifier.size(20.dp)
                                )
                            }
                        }
                    }
                }

                AnimatedVisibility(
                    visible = answered,
                    enter = fadeIn() + expandVertically()
                ) {
                    val wasCorrect = selectedOption == question.correctIndex
                    Surface(
                        shape = RoundedCornerShape(12.dp),
                        color = if (wasCorrect) sanaColors.tealTint else sanaColors.errorSurface,
                        modifier = Modifier
                            .fillMaxWidth()
                            .testTag("chat_quiz_explanation")
                    ) {
                        Column(
                            modifier = Modifier.padding(12.dp),
                            verticalArrangement = Arrangement.spacedBy(6.dp)
                        ) {
                            Text(
                                text = if (wasCorrect) {
                                    stringResource(R.string.chat_quiz_correct_badge)
                                } else {
                                    stringResource(R.string.chat_quiz_incorrect_badge)
                                },
                                style = MaterialTheme.typography.labelLarge.copy(fontWeight = FontWeight.Bold),
                                color = if (wasCorrect) sanaColors.tealDeep else sanaColors.errorText
                            )
                            if (question.explanation.isNotBlank()) {
                                Text(
                                    text = question.explanation,
                                    style = MaterialTheme.typography.bodySmall,
                                    color = if (wasCorrect) sanaColors.tealDeep else sanaColors.errorText
                                )
                            }
                            if (onAskAboutQuestion != null) {
                                TextButton(
                                    onClick = { onAskAboutQuestion(question, selectedOption) },
                                    modifier = Modifier
                                        .minimumInteractiveComponentSize()
                                        .testTag("chat_quiz_ask_sana_question_button")
                                ) {
                                    Icon(
                                        imageVector = Icons.Filled.AutoAwesome,
                                        contentDescription = null,
                                        modifier = Modifier.size(16.dp)
                                    )
                                    Spacer(modifier = Modifier.size(6.dp))
                                    Text(stringResource(R.string.chat_quiz_ask_sana))
                                }
                            }
                        }
                    }
                }
            }
        }

        }

        AnimatedVisibility(visible = answered) {
            Column(modifier = Modifier.fillMaxWidth()) {
                HorizontalDivider(color = sanaColors.cardBorder)
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .background(MaterialTheme.colorScheme.surface)
                        .padding(horizontal = 20.dp, vertical = 12.dp)
                ) {
                    Button(
                        onClick = onNextQuestion,
                        shape = RoundedCornerShape(14.dp),
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(50.dp)
                            .minimumInteractiveComponentSize()
                            .testTag("chat_quiz_next_button")
                    ) {
                        Text(
                            text = if (state.currentIndex == state.quiz.questions.lastIndex) {
                                stringResource(R.string.chat_quiz_show_result)
                            } else {
                                stringResource(R.string.chat_quiz_next)
                            }
                        )
                    }
                }
            }
        }
    }
}

@Composable
private fun ChatQuizSummaryContent(
    state: ChatQuizUiState.Ready,
    onRestartQuiz: () -> Unit,
    onRegenerateQuiz: () -> Unit,
    onAskAboutQuestion: ((QuizQuestion, Int?) -> Unit)?
) {
    val sanaColors = LocalSanaColors.current
    val total = state.quiz.questions.size
    val score = state.score
    val pct = if (total > 0) ((score.toFloat() / total) * 100f).roundToInt() else 0
    val feedback = when {
        pct == 100 -> "ممتاز! أتقنتِ جميع نقاط هذه المحادثة بنجاح."
        pct >= 60 -> "أداء رائع! ترسّخ الفهم لديك بشكل جيد."
        else -> "مراجعة سريعة مع سنا ستثبت هذه المفاهيم بشكل أفضل."
    }

    Column(
        modifier = Modifier
            .fillMaxWidth()
            .verticalScroll(rememberScrollState())
            .padding(horizontal = 20.dp, vertical = 16.dp),
        verticalArrangement = Arrangement.spacedBy(14.dp)
    ) {
        Card(
            shape = MaterialTheme.shapes.extraLarge,
            colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
            border = BorderStroke(1.dp, sanaColors.cardBorder),
            modifier = Modifier
                .fillMaxWidth()
                .testTag("chat_quiz_summary_card")
        ) {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(20.dp),
                horizontalAlignment = Alignment.CenterHorizontally,
                verticalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                Box(
                    modifier = Modifier
                        .size(58.dp)
                        .clip(RoundedCornerShape(16.dp))
                        .background(sanaColors.tealTint),
                    contentAlignment = Alignment.Center
                ) {
                    Icon(
                        imageVector = Icons.Filled.EmojiEvents,
                        contentDescription = null,
                        tint = sanaColors.tealDeep,
                        modifier = Modifier.size(30.dp)
                    )
                }
                Text(
                    text = "$score / $total ($pct%)",
                    style = MaterialTheme.typography.headlineMedium.copy(fontWeight = FontWeight.ExtraBold),
                    color = sanaColors.tealDeep,
                    modifier = Modifier.testTag("chat_quiz_final_score")
                )
                Text(
                    text = feedback,
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurface,
                    textAlign = TextAlign.Center
                )
                Text(
                    text = state.quiz.topicSummary,
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    textAlign = TextAlign.Center
                )
            }
        }

        Text(
            text = stringResource(R.string.chat_quiz_review_heading),
            style = MaterialTheme.typography.titleSmall.copy(fontWeight = FontWeight.Bold),
            color = MaterialTheme.colorScheme.onSurface
        )

        state.quiz.questions.forEachIndexed { idx, question ->
            val chosenIdx = state.selectedOptions[idx]
            val isCorrect = chosenIdx == question.correctIndex
            Card(
                shape = MaterialTheme.shapes.medium,
                colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
                border = BorderStroke(
                    1.dp,
                    if (isCorrect) sanaColors.tealAccent.copy(alpha = 0.5f) else sanaColors.errorText.copy(alpha = 0.5f)
                ),
                modifier = Modifier.fillMaxWidth()
            ) {
                Column(
                    modifier = Modifier.padding(14.dp),
                    verticalArrangement = Arrangement.spacedBy(6.dp)
                ) {
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        Icon(
                            imageVector = if (isCorrect) Icons.Filled.CheckCircle else Icons.Filled.Cancel,
                            contentDescription = if (isCorrect) {
                                stringResource(R.string.chat_quiz_correct_badge)
                            } else {
                                stringResource(R.string.chat_quiz_incorrect_badge)
                            },
                            tint = if (isCorrect) sanaColors.tealDeep else sanaColors.errorText,
                            modifier = Modifier.size(18.dp)
                        )
                        Text(
                            text = "${idx + 1}. ${question.question}",
                            style = MaterialTheme.typography.bodyMedium.copy(fontWeight = FontWeight.Medium),
                            color = MaterialTheme.colorScheme.onSurface,
                            modifier = Modifier.weight(1f)
                        )
                    }
                    val correctOption = question.options.getOrNull(question.correctIndex).orEmpty()
                    Text(
                        text = "الإجابة الصحيحة: $correctOption",
                        style = MaterialTheme.typography.bodySmall,
                        color = sanaColors.tealDeep
                    )
                    if (!isCorrect && chosenIdx != null) {
                        val chosenText = question.options.getOrNull(chosenIdx).orEmpty()
                        Text(
                            text = "إجابتك: $chosenText",
                            style = MaterialTheme.typography.bodySmall,
                            color = sanaColors.errorText
                        )
                    }
                    if (onAskAboutQuestion != null) {
                        TextButton(
                            onClick = { onAskAboutQuestion(question, chosenIdx) },
                            modifier = Modifier.minimumInteractiveComponentSize()
                        ) {
                            Icon(
                                imageVector = Icons.Filled.Psychology,
                                contentDescription = null,
                                modifier = Modifier.size(16.dp)
                            )
                            Spacer(modifier = Modifier.size(6.dp))
                            Text(stringResource(R.string.chat_quiz_ask_sana))
                        }
                    }
                }
            }
        }

        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(10.dp)
        ) {
            OutlinedButton(
                onClick = onRestartQuiz,
                shape = RoundedCornerShape(13.dp),
                modifier = Modifier
                    .weight(1f)
                    .height(48.dp)
                    .minimumInteractiveComponentSize()
                    .testTag("chat_quiz_restart_button")
            ) {
                Text(stringResource(R.string.chat_quiz_restart))
            }
            Button(
                onClick = onRegenerateQuiz,
                shape = RoundedCornerShape(13.dp),
                colors = ButtonDefaults.buttonColors(containerColor = MaterialTheme.colorScheme.primary),
                modifier = Modifier
                    .weight(1f)
                    .height(48.dp)
                    .minimumInteractiveComponentSize()
                    .testTag("chat_quiz_regenerate_button")
            ) {
                Icon(
                    imageVector = Icons.Filled.Refresh,
                    contentDescription = null,
                    modifier = Modifier.size(18.dp)
                )
                Spacer(modifier = Modifier.size(6.dp))
                Text(stringResource(R.string.chat_quiz_regenerate))
            }
        }
    }
}
