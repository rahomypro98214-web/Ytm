package com.example.ui.components

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
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.Send
import androidx.compose.material.icons.filled.AutoAwesome
import androidx.compose.material.icons.filled.ExpandLess
import androidx.compose.material.icons.filled.ExpandMore
import androidx.compose.material.icons.filled.WbSunny
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.minimumInteractiveComponentSize
import androidx.compose.runtime.Composable
import androidx.compose.runtime.derivedStateOf
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import com.example.R
import com.example.ui.theme.LocalSanaColors
import java.util.Calendar

data class DailyPromptItem(
    val id: String,
    val category: String,
    val title: String,
    val prompt: String,
    val isFeaturedToday: Boolean = false
)

private val CURATED_DAILY_PROMPTS = listOf(
    DailyPromptItem(
        id = "learn_quantum",
        category = "علوم وتقنية",
        title = "مفهوم معقّد في دقيقة",
        prompt = "اشرحي لي مبدأ الحوسبة الكمية بمثال يومي بسيط وممتع كأنني أسمع عنه لأول مرة."
    ),
    DailyPromptItem(
        id = "arabic_eloquence",
        category = "بلاغة وأدب",
        title = "كلمة عربية نادرة",
        prompt = "عرّفيني على كلمة عربية فصيحة نادرة وجميلة مع معناها وأصلها وجملة عصرية تستخدمها."
    ),
    DailyPromptItem(
        id = "productivity_habit",
        category = "تعلّم وإنتاجية",
        title = "خطة تعلّم ذكية",
        prompt = "صمّمي لي خطة مركّزة لمدة ٢٠ دقيقة يوميًا لإتقان مهارة جديدة هذا الأسبوع."
    ),
    DailyPromptItem(
        id = "critical_thinking",
        category = "تفكير ناقد",
        title = "سؤال للنقاش والتأمل",
        prompt = "اطرحي عليّ معضلة فكرية ممتعة في الذكاء الاصطناعي وناقشي معي وجهتي النظر."
    ),
    DailyPromptItem(
        id = "bilingual_idiom",
        category = "ترجمة وثقافة",
        title = "تعبير إنجليزي ومقابله",
        prompt = "أعطيني 3 تعبيرات إنجليزية شائعة في بيئة العمل مع ترجمتها الطبيعية بالعربية."
    ),
    DailyPromptItem(
        id = "creative_story",
        category = "إبداع وكتابة",
        title = "شرارة قصة قصيرة",
        prompt = "ابدئي قصة قصيرة ملهمة عن مدينة عربية مستقبلية في ٣ جمل واتركي لي كتابة النهاية."
    )
)

internal fun buildDailyPromptsCarousel(
    dayOfYear: Int = Calendar.getInstance().get(Calendar.DAY_OF_YEAR)
): List<DailyPromptItem> {
    val size = CURATED_DAILY_PROMPTS.size
    val startIndex = ((dayOfYear % size) + size) % size
    return List(size) { offset ->
        val item = CURATED_DAILY_PROMPTS[(startIndex + offset) % size]
        item.copy(isFeaturedToday = offset == 0)
    }
}

@Composable
fun PromptOfTheDayCarousel(
    expanded: Boolean,
    enabled: Boolean,
    onToggleExpanded: () -> Unit,
    onSelectPrompt: (String) -> Unit,
    modifier: Modifier = Modifier,
    dayOfYear: Int = Calendar.getInstance().get(Calendar.DAY_OF_YEAR)
) {
    val sanaColors = LocalSanaColors.current
    val dailyPrompts = remember(dayOfYear) { buildDailyPromptsCarousel(dayOfYear) }
    val listState = rememberLazyListState()
    val firstVisibleIndex by remember {
        derivedStateOf { listState.firstVisibleItemIndex }
    }

    Column(
        modifier = modifier
            .fillMaxWidth()
            .background(MaterialTheme.colorScheme.surface)
            .testTag("prompt_of_day_section")
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(start = 14.dp, end = 6.dp, top = 4.dp, bottom = 2.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(6.dp)
        ) {
            Surface(
                shape = RoundedCornerShape(999.dp),
                color = sanaColors.tealTint
            ) {
                Row(
                    modifier = Modifier.padding(horizontal = 8.dp, vertical = 3.dp),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(4.dp)
                ) {
                    Icon(
                        imageVector = Icons.Filled.WbSunny,
                        contentDescription = null,
                        tint = sanaColors.tealDeep,
                        modifier = Modifier.size(13.dp)
                    )
                    Text(
                        text = stringResource(R.string.prompt_of_day_header),
                        style = MaterialTheme.typography.labelSmall.copy(fontWeight = FontWeight.Bold),
                        color = sanaColors.tealDeep,
                        modifier = Modifier.testTag("prompt_of_day_header_label")
                    )
                }
            }

            if (expanded) {
                Row(
                    horizontalArrangement = Arrangement.spacedBy(4.dp),
                    verticalAlignment = Alignment.CenterVertically,
                    modifier = Modifier.weight(1f)
                ) {
                    dailyPrompts.forEachIndexed { idx, _ ->
                        val active = idx == firstVisibleIndex
                        Box(
                            modifier = Modifier
                                .size(if (active) 7.dp else 5.dp)
                                .clip(CircleShape)
                                .background(
                                    if (active) sanaColors.tealAccent
                                    else sanaColors.cardBorder
                                )
                        )
                    }
                }
            } else {
                Text(
                    text = dailyPrompts.first().title,
                    style = MaterialTheme.typography.labelMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis,
                    modifier = Modifier.weight(1f)
                )
            }

            IconButton(
                onClick = onToggleExpanded,
                modifier = Modifier
                    .size(34.dp)
                    .minimumInteractiveComponentSize()
                    .testTag("toggle_prompt_carousel_button")
            ) {
                Icon(
                    imageVector = if (expanded) Icons.Filled.ExpandLess else Icons.Filled.ExpandMore,
                    contentDescription = if (expanded) {
                        stringResource(R.string.prompt_of_day_toggle_hide)
                    } else {
                        stringResource(R.string.prompt_of_day_toggle_show)
                    },
                    tint = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
        }

        AnimatedVisibility(visible = expanded) {
        LazyRow(
            state = listState,
            contentPadding = PaddingValues(start = 12.dp, end = 12.dp, bottom = 8.dp, top = 0.dp),
            horizontalArrangement = Arrangement.spacedBy(10.dp),
            modifier = Modifier
                .fillMaxWidth()
                .testTag("prompt_of_day_carousel")
        ) {
            itemsIndexed(dailyPrompts, key = { _, item -> item.id }) { index, item ->
                DailyPromptCard(
                    item = item,
                    enabled = enabled,
                    onClick = { onSelectPrompt(item.prompt) },
                    modifier = Modifier.testTag("prompt_of_day_card_$index")
                )
            }
        }
        }
    }
}

@Composable
private fun DailyPromptCard(
    item: DailyPromptItem,
    enabled: Boolean,
    onClick: () -> Unit,
    modifier: Modifier = Modifier
) {
    val sanaColors = LocalSanaColors.current
    val cardBrush = if (item.isFeaturedToday) {
        Brush.linearGradient(
            listOf(sanaColors.tealTint, MaterialTheme.colorScheme.surface)
        )
    } else {
        Brush.linearGradient(
            listOf(sanaColors.chatCanvas, MaterialTheme.colorScheme.surface)
        )
    }

    Card(
        shape = RoundedCornerShape(14.dp),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
        border = BorderStroke(
            width = if (item.isFeaturedToday) 1.5.dp else 1.dp,
            color = if (item.isFeaturedToday) sanaColors.tealAccent else sanaColors.cardBorder
        ),
        modifier = modifier
            .width(236.dp)
            .clip(RoundedCornerShape(14.dp))
            .clickable(enabled = enabled, onClick = onClick)
    ) {
        Column(
            modifier = Modifier
                .background(cardBrush)
                .padding(horizontal = 10.dp, vertical = 7.dp),
            verticalArrangement = Arrangement.spacedBy(3.dp)
        ) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(4.dp)
                ) {
                    Icon(
                        imageVector = Icons.Filled.AutoAwesome,
                        contentDescription = null,
                        tint = if (item.isFeaturedToday) sanaColors.tealDeep else sanaColors.tealAccent,
                        modifier = Modifier.size(12.dp)
                    )
                    Text(
                        text = item.category,
                        style = MaterialTheme.typography.labelSmall.copy(fontWeight = FontWeight.Bold),
                        color = sanaColors.tealDeep
                    )
                }
                if (item.isFeaturedToday) {
                    Surface(
                        shape = RoundedCornerShape(999.dp),
                        color = sanaColors.heroBackground
                    ) {
                        Text(
                            text = "مميز اليوم",
                            style = MaterialTheme.typography.labelSmall,
                            color = sanaColors.mintGlow,
                            modifier = Modifier.padding(horizontal = 6.dp, vertical = 1.dp)
                        )
                    }
                }
            }

            Text(
                text = item.title,
                style = MaterialTheme.typography.titleSmall.copy(fontWeight = FontWeight.Bold),
                color = MaterialTheme.colorScheme.onSurface,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis
            )

            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(6.dp),
                modifier = Modifier.fillMaxWidth()
            ) {
                Text(
                    text = item.prompt,
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis,
                    modifier = Modifier.weight(1f)
                )
                Icon(
                    imageVector = Icons.AutoMirrored.Filled.Send,
                    contentDescription = stringResource(R.string.prompt_of_day_cta),
                    tint = if (enabled) sanaColors.tealDeep else MaterialTheme.colorScheme.onSurfaceVariant,
                    modifier = Modifier.size(14.dp)
                )
            }
        }
    }
}
