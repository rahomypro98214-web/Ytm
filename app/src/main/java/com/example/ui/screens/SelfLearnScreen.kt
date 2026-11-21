package com.example.ui.screens

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ExperimentalLayoutApi
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.filled.DeleteOutline
import androidx.compose.material.icons.filled.ExpandLess
import androidx.compose.material.icons.filled.ExpandMore
import androidx.compose.material.icons.filled.Language
import androidx.compose.material.icons.filled.Search
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.OutlinedTextFieldDefaults
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.platform.LocalLayoutDirection
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.LayoutDirection
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.example.data.model.KnowledgeSource
import com.example.data.model.StarterContent
import com.example.data.model.UiState
import com.example.ui.SanaViewModel
import com.example.ui.SelfLearnProgress
import com.example.ui.theme.LocalSanaColors

@OptIn(ExperimentalLayoutApi::class)
@Composable
fun SelfLearnScreen(
    viewModel: SanaViewModel,
    modifier: Modifier = Modifier
) {
    val sanaColors = LocalSanaColors.current

    val sourcesState by viewModel.knowledgeSourcesState.collectAsStateWithLifecycle()
    val progressState by viewModel.selfLearnProgress.collectAsStateWithLifecycle()
    val streamingPreview by viewModel.selfLearnStreamingPreview.collectAsStateWithLifecycle()
    val errorState by viewModel.selfLearnError.collectAsStateWithLifecycle()
    val bannerState by viewModel.selfLearnSuccessBanner.collectAsStateWithLifecycle()

    var urlInput by rememberSaveable { mutableStateOf("") }
    var pasteText by rememberSaveable { mutableStateOf("") }
    var filterQuery by rememberSaveable { mutableStateOf("") }

    val sources = (sourcesState as? UiState.Success)?.data ?: emptyList()
    val filteredSources = remember(sources, filterQuery) {
        if (filterQuery.isBlank()) sources
        else {
            val q = filterQuery.trim().lowercase()
            sources.filter {
                it.title.lowercase().contains(q) ||
                    it.summary.lowercase().contains(q) ||
                    it.keywords.any { k -> k.lowercase().contains(q) }
            }
        }
    }

    val totalFacts = sources.sumOf { it.factCount.toInt() }
    val totalKeywords = sources.sumOf { it.keywords.size }

    Column(
        modifier = modifier
            .fillMaxSize()
            .background(sanaColors.chatCanvas)
    ) {
        AnimatedVisibility(visible = !errorState.isNullOrBlank()) {
            Surface(
                color = sanaColors.errorSurface,
                modifier = Modifier
                    .fillMaxWidth()
                    .clickable { viewModel.dismissSelfLearnError() }
            ) {
                Text(
                    text = "${errorState.orEmpty()} (اضغط للإخفاء)",
                    style = MaterialTheme.typography.bodySmall,
                    color = sanaColors.errorText,
                    modifier = Modifier.padding(horizontal = 16.dp, vertical = 10.dp)
                )
            }
        }

        AnimatedVisibility(visible = !bannerState.isNullOrBlank()) {
            Surface(
                color = sanaColors.tealTint,
                modifier = Modifier
                    .fillMaxWidth()
                    .clickable { viewModel.dismissSelfLearnBanner() }
            ) {
                Text(
                    text = "${bannerState.orEmpty()} ✓",
                    style = MaterialTheme.typography.bodySmall.copy(fontWeight = FontWeight.Bold),
                    color = sanaColors.tealDeep,
                    modifier = Modifier.padding(horizontal = 16.dp, vertical = 10.dp)
                )
            }
        }

        LazyColumn(
            modifier = Modifier.fillMaxSize(),
            contentPadding = PaddingValues(16.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            // 1. Knowledge Base Stats Row
            item(key = "stats") {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(10.dp)
                ) {
                    KbStatCard(
                        value = "${sources.size}",
                        label = "مصدر متعلَّم",
                        modifier = Modifier.weight(1f)
                    )
                    KbStatCard(
                        value = "$totalFacts",
                        label = "فكرة بالذاكرة",
                        modifier = Modifier.weight(1f)
                    )
                    KbStatCard(
                        value = "$totalKeywords",
                        label = "كلمة مفتاحية",
                        modifier = Modifier.weight(1f)
                    )
                }
            }

            // 2. Live Pipeline Stepper
            val activeProgress = progressState
            if (activeProgress != null) {
                item(key = "pipeline_progress") {
                    LearningPipelineCard(
                        progress = activeProgress,
                        streamingPreview = streamingPreview
                    )
                }
            }

            // 3. Preset Starter Encyclopedias
            item(key = "presets_header") {
                Text(
                    text = "صفحات معرفية جاهزة للتعلّم السريع:",
                    style = MaterialTheme.typography.titleSmall,
                    color = MaterialTheme.colorScheme.onSurface
                )
            }

            items(StarterContent.presetSites, key = { "preset_${it.id}" }) { site ->
                val alreadyLearned = sources.any { it.sourceType == "site" && it.title == site.name }
                Card(
                    shape = MaterialTheme.shapes.medium,
                    colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
                    border = BorderStroke(1.dp, sanaColors.cardBorder),
                    modifier = Modifier
                        .fillMaxWidth()
                        .clickable(enabled = !alreadyLearned && progressState == null) {
                            viewModel.learnPresetSite(site)
                        }
                        .testTag("preset_site_${site.id}")
                ) {
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(14.dp),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(12.dp)
                    ) {
                        Text(text = site.iconEmoji, fontSize = 24.sp)
                        Column(modifier = Modifier.weight(1f)) {
                            Text(
                                text = site.name,
                                style = MaterialTheme.typography.titleSmall,
                                color = MaterialTheme.colorScheme.onSurface
                            )
                            Text(
                                text = site.url,
                                style = MaterialTheme.typography.bodySmall,
                                color = MaterialTheme.colorScheme.onSurfaceVariant,
                                maxLines = 1,
                                overflow = TextOverflow.Ellipsis
                            )
                        }
                        if (alreadyLearned) {
                            Row(
                                horizontalArrangement = Arrangement.spacedBy(4.dp),
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Icon(
                                    imageVector = Icons.Filled.CheckCircle,
                                    contentDescription = "متعلمة",
                                    tint = sanaColors.tealDeep,
                                    modifier = Modifier.size(16.dp)
                                )
                                Text(
                                    text = "تم التعلّم",
                                    style = MaterialTheme.typography.labelMedium,
                                    color = sanaColors.tealDeep
                                )
                            }
                        } else {
                            Text(
                                text = "تعلّمها ←",
                                style = MaterialTheme.typography.labelLarge,
                                color = sanaColors.tealDeep
                            )
                        }
                    }
                }
            }

            // 4. Learn from Real Web URL
            item(key = "url_learner") {
                Card(
                    shape = MaterialTheme.shapes.medium,
                    colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
                    border = BorderStroke(1.dp, sanaColors.cardBorder)
                ) {
                    Column(
                        modifier = Modifier.padding(16.dp),
                        verticalArrangement = Arrangement.spacedBy(10.dp)
                    ) {
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(8.dp)
                        ) {
                            Icon(
                                imageVector = Icons.Filled.Language,
                                contentDescription = null,
                                tint = sanaColors.tealDeep
                            )
                            Text(
                                text = "تعلّم من رابط صفحة ويب",
                                style = MaterialTheme.typography.titleMedium
                            )
                        }
                        Text(
                            text = "أدخل رابط أي مقال أو صفحة عامة لتقرأها سنا وتستخلص ملخصها وكلماتها المفتاحية بـ Gemini.",
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                        OutlinedTextField(
                            value = urlInput,
                            onValueChange = { urlInput = it },
                            placeholder = { Text("https://ar.wikipedia.org/wiki/...") },
                            singleLine = true,
                            shape = RoundedCornerShape(12.dp),
                            colors = OutlinedTextFieldDefaults.colors(
                                focusedBorderColor = MaterialTheme.colorScheme.primary,
                                unfocusedBorderColor = sanaColors.cardBorder
                            ),
                            modifier = Modifier
                                .fillMaxWidth()
                                .testTag("learn_url_input")
                        )
                        Button(
                            onClick = {
                                viewModel.learnFromUrl(urlInput)
                                urlInput = ""
                            },
                            enabled = urlInput.isNotBlank() && progressState == null,
                            shape = RoundedCornerShape(12.dp),
                            modifier = Modifier
                                .fillMaxWidth()
                                .height(48.dp)
                                .testTag("learn_url_button")
                        ) {
                            Text("اقرأ الرابط وتعلّم منه")
                        }
                    }
                }
            }

            // 5. Learn from Pasted Text
            item(key = "paste_learner") {
                Card(
                    shape = MaterialTheme.shapes.medium,
                    colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
                    border = BorderStroke(1.dp, sanaColors.cardBorder)
                ) {
                    Column(
                        modifier = Modifier.padding(16.dp),
                        verticalArrangement = Arrangement.spacedBy(10.dp)
                    ) {
                        Text(
                            text = "أو علّم سنا من نص ملصوق",
                            style = MaterialTheme.typography.titleMedium
                        )
                        OutlinedTextField(
                            value = pasteText,
                            onValueChange = { pasteText = it },
                            placeholder = {
                                Text("الصق هنا مقالًا أو ملخص درس أو ملاحظات (40 حرفًا على الأقل)…")
                            },
                            minLines = 3,
                            maxLines = 6,
                            shape = RoundedCornerShape(12.dp),
                            colors = OutlinedTextFieldDefaults.colors(
                                focusedBorderColor = MaterialTheme.colorScheme.primary,
                                unfocusedBorderColor = sanaColors.cardBorder
                            ),
                            modifier = Modifier
                                .fillMaxWidth()
                                .testTag("learn_paste_input")
                        )
                        Button(
                            onClick = {
                                viewModel.learnPastedText(pasteText)
                                pasteText = ""
                            },
                            enabled = pasteText.trim().length >= 40 && progressState == null,
                            shape = RoundedCornerShape(12.dp),
                            modifier = Modifier
                                .fillMaxWidth()
                                .height(48.dp)
                                .testTag("learn_paste_button")
                        ) {
                            Text("حلّل واحفظ في الذاكرة")
                        }
                    }
                }
            }

            // 6. Saved Knowledge Base List
            item(key = "kb_header") {
                Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                    Text(
                        text = "ذاكرة سنا السحابية (${sources.size})",
                        style = MaterialTheme.typography.titleMedium
                    )
                    if (sources.size > 2) {
                        OutlinedTextField(
                            value = filterQuery,
                            onValueChange = { filterQuery = it },
                            placeholder = { Text("ابحث في الذاكرة…") },
                            leadingIcon = {
                                Icon(imageVector = Icons.Filled.Search, contentDescription = null)
                            },
                            singleLine = true,
                            shape = RoundedCornerShape(12.dp),
                            modifier = Modifier
                                .fillMaxWidth()
                                .testTag("kb_search_input")
                        )
                    }
                }
            }

            if (sourcesState is UiState.Loading) {
                item(key = "kb_loading") {
                    Box(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(24.dp),
                        contentAlignment = Alignment.Center
                    ) {
                        CircularProgressIndicator()
                    }
                }
            } else if (filteredSources.isEmpty()) {
                item(key = "kb_empty") {
                    Surface(
                        shape = MaterialTheme.shapes.medium,
                        color = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f),
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Text(
                            text = if (sources.isEmpty()) {
                                "ذاكرة سنا ما زالت فارغة — ابدأ بتعلّم إحدى الصفحات الجاهزة أو ألصق نصًا أعلاه."
                            } else {
                                "لا توجد مصادر مطابقة لبحثك."
                            },
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                            modifier = Modifier.padding(18.dp)
                        )
                    }
                }
            } else {
                items(filteredSources, key = { it.id }) { item ->
                    KnowledgeSourceCard(
                        source = item,
                        onDelete = { viewModel.deleteKnowledgeSource(item.id) }
                    )
                }
            }
        }
    }
}

@Composable
private fun KbStatCard(
    value: String,
    label: String,
    modifier: Modifier = Modifier
) {
    val sanaColors = LocalSanaColors.current
    Card(
        modifier = modifier,
        shape = MaterialTheme.shapes.medium,
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
        border = BorderStroke(1.dp, sanaColors.cardBorder)
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(vertical = 12.dp, horizontal = 8.dp),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            Text(
                text = value,
                style = MaterialTheme.typography.headlineSmall,
                color = sanaColors.tealDeep
            )
            Text(
                text = label,
                style = MaterialTheme.typography.labelSmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
        }
    }
}

@Composable
private fun LearningPipelineCard(
    progress: SelfLearnProgress,
    streamingPreview: String? = null
) {
    val sanaColors = LocalSanaColors.current
    Surface(
        shape = MaterialTheme.shapes.medium,
        color = sanaColors.heroBackground,
        modifier = Modifier.fillMaxWidth()
    ) {
        Column(
            modifier = Modifier.padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                com.example.ui.components.AiPulseOrb(size = 26.dp)
                Text(
                    text = "جاري تعلّم: ${progress.sourceName}",
                    style = MaterialTheme.typography.titleSmall,
                    color = sanaColors.heroOnBackground,
                    modifier = Modifier.weight(1f)
                )
            }
            if (!streamingPreview.isNullOrBlank()) {
                Text(
                    text = streamingPreview,
                    style = MaterialTheme.typography.bodySmall,
                    color = sanaColors.mintGlow,
                    maxLines = 2,
                    overflow = TextOverflow.Ellipsis
                )
            }
            progress.steps.forEachIndexed { idx, stepText ->
                val isDone = idx < progress.activeStep
                val isCurrent = idx == progress.activeStep
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    when {
                        isDone -> Icon(
                            imageVector = Icons.Filled.Check,
                            contentDescription = null,
                            tint = sanaColors.mintGlow,
                            modifier = Modifier.size(16.dp)
                        )
                        isCurrent -> CircularProgressIndicator(
                            modifier = Modifier.size(14.dp),
                            strokeWidth = 2.dp,
                            color = sanaColors.mintGlow
                        )
                        else -> Spacer(modifier = Modifier.size(16.dp))
                    }
                    Text(
                        text = stepText,
                        style = MaterialTheme.typography.bodySmall,
                        color = if (isDone || isCurrent) sanaColors.mintGlow else sanaColors.heroSubtext
                    )
                }
            }
        }
    }
}

@OptIn(ExperimentalLayoutApi::class)
@Composable
private fun KnowledgeSourceCard(
    source: KnowledgeSource,
    onDelete: () -> Unit
) {
    val sanaColors = LocalSanaColors.current
    var expanded by rememberSaveable(source.id) { mutableStateOf(false) }

    val (badgeBg, badgeText, badgeLabel) = when (source.sourceType) {
        "site" -> Triple(sanaColors.tealTint, sanaColors.tealDeep, "موسوعة")
        "url" -> Triple(sanaColors.indigoTint, MaterialTheme.colorScheme.secondary, "رابط ويب")
        else -> Triple(sanaColors.amberTint, sanaColors.amberDeep, "نص")
    }

    Card(
        shape = MaterialTheme.shapes.medium,
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
        border = BorderStroke(1.dp, sanaColors.cardBorder),
        modifier = Modifier
            .fillMaxWidth()
            .clickable { expanded = !expanded }
            .testTag("kb_item_${source.id}")
    ) {
        Column(
            modifier = Modifier.padding(14.dp),
            verticalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                Column(modifier = Modifier.weight(1f)) {
                    Text(
                        text = source.title,
                        style = MaterialTheme.typography.titleSmall,
                        color = MaterialTheme.colorScheme.onSurface
                    )
                    Text(
                        text = source.sourceLabel,
                        style = MaterialTheme.typography.labelSmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }

                Surface(shape = RoundedCornerShape(999.dp), color = badgeBg) {
                    Text(
                        text = badgeLabel,
                        style = MaterialTheme.typography.labelSmall,
                        color = badgeText,
                        modifier = Modifier.padding(horizontal = 10.dp, vertical = 4.dp)
                    )
                }

                IconButton(
                    onClick = { expanded = !expanded },
                    modifier = Modifier.testTag("expand_kb_${source.id}")
                ) {
                    Icon(
                        imageVector = if (expanded) Icons.Filled.ExpandLess else Icons.Filled.ExpandMore,
                        contentDescription = if (expanded) "طي" else "توسيع"
                    )
                }

                IconButton(
                    onClick = onDelete,
                    modifier = Modifier.testTag("delete_kb_${source.id}")
                ) {
                    Icon(
                        imageVector = Icons.Filled.DeleteOutline,
                        contentDescription = "حذف من الذاكرة",
                        tint = sanaColors.errorText
                    )
                }
            }

            if (source.summary.isNotBlank()) {
                Text(
                    text = source.summary,
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    maxLines = if (expanded) Int.MAX_VALUE else 2,
                    overflow = TextOverflow.Ellipsis
                )
            }

            if (expanded) {
                Surface(
                    shape = RoundedCornerShape(10.dp),
                    color = sanaColors.chatCanvas
                ) {
                    Text(
                        text = source.content.take(1500),
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurface,
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(10.dp)
                    )
                }
            }

            if (source.keywords.isNotEmpty()) {
                FlowRow(
                    horizontalArrangement = Arrangement.spacedBy(6.dp),
                    verticalArrangement = Arrangement.spacedBy(6.dp)
                ) {
                    source.keywords.forEach { kw ->
                        Surface(
                            shape = RoundedCornerShape(999.dp),
                            color = MaterialTheme.colorScheme.background,
                            border = BorderStroke(1.dp, sanaColors.cardBorder)
                        ) {
                            Text(
                                text = kw,
                                style = MaterialTheme.typography.labelSmall,
                                color = MaterialTheme.colorScheme.onSurfaceVariant,
                                modifier = Modifier.padding(horizontal = 10.dp, vertical = 3.dp)
                            )
                        }
                    }
                }
            }
        }
    }
}
