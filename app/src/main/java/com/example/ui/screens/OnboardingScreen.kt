package com.example.ui.screens

import androidx.compose.animation.animateColorAsState
import androidx.compose.animation.core.Spring
import androidx.compose.animation.core.animateDpAsState
import androidx.compose.animation.core.spring
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.safeDrawing
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.layout.windowInsetsPadding
import androidx.compose.foundation.pager.HorizontalPager
import androidx.compose.foundation.pager.rememberPagerState
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.automirrored.filled.ArrowForward
import androidx.compose.material.icons.filled.AutoAwesome
import androidx.compose.material.icons.filled.AutoFixHigh
import androidx.compose.material.icons.filled.ChatBubble
import androidx.compose.material.icons.filled.History
import androidx.compose.material.icons.filled.Psychology
import androidx.compose.material.icons.filled.School
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.minimumInteractiveComponentSize
import androidx.compose.runtime.Composable
import androidx.compose.runtime.Immutable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.drawBehind
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import com.example.R
import com.example.ui.theme.LocalSanaColors
import kotlinx.coroutines.launch

@Immutable
private data class OnboardingFeatureHighlight(
    val icon: ImageVector,
    val label: String,
    val detail: String
)

@Immutable
private data class OnboardingPageData(
    val title: String,
    val body: String,
    val primaryIcon: ImageVector,
    val secondaryIcon: ImageVector,
    val useHeroArtwork: Boolean,
    val accentColor: Color,
    val highlights: List<OnboardingFeatureHighlight>
)

@Composable
fun OnboardingScreen(
    onFinish: () -> Unit,
    modifier: Modifier = Modifier
) {
    val sanaColors = LocalSanaColors.current
    val scope = rememberCoroutineScope()

    val pages = listOf(
        OnboardingPageData(
            title = stringResource(R.string.onboarding_page_1_title),
            body = stringResource(R.string.onboarding_page_1_body),
            primaryIcon = Icons.Filled.ChatBubble,
            secondaryIcon = Icons.Filled.History,
            useHeroArtwork = true,
            accentColor = sanaColors.tealAccent,
            highlights = listOf(
                OnboardingFeatureHighlight(
                    icon = Icons.Filled.AutoAwesome,
                    label = "ردود فورية متدفقة",
                    detail = "محادثة وتلخيص وترجمة عربية مدعومة بـ Gemini"
                ),
                OnboardingFeatureHighlight(
                    icon = Icons.Filled.History,
                    label = "سجل محادثات محفوظ محليًا",
                    detail = "احفظ محادثاتك السابقة عبر Room وعد إليها في أي وقت"
                )
            )
        ),
        OnboardingPageData(
            title = stringResource(R.string.onboarding_page_2_title),
            body = stringResource(R.string.onboarding_page_2_body),
            primaryIcon = Icons.Filled.Psychology,
            secondaryIcon = Icons.Filled.History,
            useHeroArtwork = false,
            accentColor = sanaColors.amberAccent,
            highlights = listOf(
                OnboardingFeatureHighlight(
                    icon = Icons.Filled.Psychology,
                    label = "تعلّم ذاتي من مصادرك",
                    detail = "أضف نصوصًا أو روابط ويب لتجيبك سنا مع ذكر المصدر"
                ),
                OnboardingFeatureHighlight(
                    icon = Icons.Filled.History,
                    label = "تنقّل سهل بين المحادثات",
                    detail = "ابدأ موضوعًا جديدًا أو استرجع أي محادثة بضغطة واحدة"
                )
            )
        ),
        OnboardingPageData(
            title = stringResource(R.string.onboarding_page_3_title),
            body = stringResource(R.string.onboarding_page_3_body),
            primaryIcon = Icons.Filled.AutoFixHigh,
            secondaryIcon = Icons.Filled.School,
            useHeroArtwork = false,
            accentColor = sanaColors.tealDeep,
            highlights = listOf(
                OnboardingFeatureHighlight(
                    icon = Icons.Filled.AutoFixHigh,
                    label = "استوديو الصور والفيديو",
                    detail = "أنشئ صورًا فنية وسيناريوهات فيديو مع تعليق صوتي"
                ),
                OnboardingFeatureHighlight(
                    icon = Icons.Filled.School,
                    label = "دورات واختبارات تفاعلية",
                    detail = "ولّد مسارًا تعليميًا متكاملًا في أي موضوع تريده"
                )
            )
        )
    )

    val pagerState = rememberPagerState(pageCount = { pages.size })
    val isLastPage = pagerState.currentPage == pages.lastIndex

    Surface(
        modifier = modifier
            .fillMaxSize()
            .testTag("onboarding_screen"),
        color = MaterialTheme.colorScheme.background
    ) {
        Box(
            modifier = Modifier
                .fillMaxSize()
                .drawBehind {
                    drawCircle(
                        brush = Brush.radialGradient(
                            colors = listOf(
                                sanaColors.tealAccent.copy(alpha = 0.14f),
                                Color.Transparent
                            ),
                            center = Offset(size.width * 0.85f, size.height * 0.12f),
                            radius = size.maxDimension * 0.55f
                        ),
                        radius = size.maxDimension * 0.55f,
                        center = Offset(size.width * 0.85f, size.height * 0.12f)
                    )
                }
                .windowInsetsPadding(WindowInsets.safeDrawing)
        ) {
            Column(
                modifier = Modifier
                    .align(Alignment.Center)
                    .widthIn(max = 560.dp)
                    .fillMaxSize()
                    .padding(horizontal = 20.dp, vertical = 14.dp),
                verticalArrangement = Arrangement.SpaceBetween
            ) {
                // Top Brand Row + Skip Action
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(10.dp)
                    ) {
                        Box(
                            modifier = Modifier
                                .size(38.dp)
                                .clip(RoundedCornerShape(12.dp))
                                .background(
                                    Brush.linearGradient(
                                        listOf(sanaColors.tealDeep, sanaColors.tealAccent)
                                    )
                                ),
                            contentAlignment = Alignment.Center
                        ) {
                            Text(
                                text = "س",
                                style = MaterialTheme.typography.titleLarge.copy(fontWeight = FontWeight.ExtraBold),
                                color = Color.White
                            )
                        }
                        Text(
                            text = stringResource(R.string.app_name),
                            style = MaterialTheme.typography.titleLarge.copy(fontWeight = FontWeight.Bold),
                            color = MaterialTheme.colorScheme.onBackground
                        )
                    }

                    if (!isLastPage) {
                        TextButton(
                            onClick = onFinish,
                            modifier = Modifier
                                .minimumInteractiveComponentSize()
                                .testTag("onboarding_skip_button")
                        ) {
                            Text(
                                text = stringResource(R.string.onboarding_skip),
                                style = MaterialTheme.typography.labelLarge,
                                color = sanaColors.tealDeep
                            )
                        }
                    } else {
                        Spacer(modifier = Modifier.height(48.dp))
                    }
                }

                // Pager Pages
                HorizontalPager(
                    state = pagerState,
                    modifier = Modifier
                        .weight(1f)
                        .fillMaxWidth()
                        .testTag("onboarding_pager")
                ) { pageIndex ->
                    OnboardingPageContent(
                        page = pages[pageIndex],
                        pageIndex = pageIndex,
                        modifier = Modifier.fillMaxSize()
                    )
                }

                // Bottom Indicators & Navigation Buttons
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(top = 12.dp, bottom = 6.dp),
                    horizontalAlignment = Alignment.CenterHorizontally,
                    verticalArrangement = Arrangement.spacedBy(16.dp)
                ) {
                    Row(
                        horizontalArrangement = Arrangement.spacedBy(8.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        pages.forEachIndexed { index, _ ->
                            val selected = pagerState.currentPage == index
                            val width by animateDpAsState(
                                targetValue = if (selected) 28.dp else 10.dp,
                                animationSpec = spring(stiffness = Spring.StiffnessMediumLow),
                                label = "indicator_width_$index"
                            )
                            val color by animateColorAsState(
                                targetValue = if (selected) {
                                    sanaColors.tealDeep
                                } else {
                                    sanaColors.cardBorder
                                },
                                label = "indicator_color_$index"
                            )
                            Box(
                                modifier = Modifier
                                    .height(10.dp)
                                    .width(width)
                                    .clip(CircleShape)
                                    .background(color)
                                    .testTag("onboarding_indicator_$index")
                            )
                        }
                    }

                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(12.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        if (pagerState.currentPage > 0) {
                            OutlinedButton(
                                onClick = {
                                    scope.launch {
                                        pagerState.animateScrollToPage(pagerState.currentPage - 1)
                                    }
                                },
                                shape = RoundedCornerShape(14.dp),
                                border = BorderStroke(1.dp, sanaColors.cardBorder),
                                modifier = Modifier
                                    .height(54.dp)
                                    .minimumInteractiveComponentSize()
                                    .testTag("onboarding_back_button")
                            ) {
                                Icon(
                                    imageVector = Icons.AutoMirrored.Filled.ArrowBack,
                                    contentDescription = stringResource(R.string.onboarding_previous),
                                    modifier = Modifier.size(18.dp)
                                )
                                Spacer(modifier = Modifier.width(6.dp))
                                Text(
                                    text = stringResource(R.string.onboarding_previous),
                                    style = MaterialTheme.typography.labelLarge
                                )
                            }
                        }

                        Button(
                            onClick = {
                                if (isLastPage) {
                                    onFinish()
                                } else {
                                    scope.launch {
                                        pagerState.animateScrollToPage(pagerState.currentPage + 1)
                                    }
                                }
                            },
                            shape = RoundedCornerShape(14.dp),
                            colors = ButtonDefaults.buttonColors(
                                containerColor = MaterialTheme.colorScheme.primary,
                                contentColor = MaterialTheme.colorScheme.onPrimary
                            ),
                            modifier = Modifier
                                .weight(1f)
                                .height(54.dp)
                                .minimumInteractiveComponentSize()
                                .testTag("onboarding_next_button")
                        ) {
                            Text(
                                text = if (isLastPage) {
                                    stringResource(R.string.onboarding_get_started)
                                } else {
                                    stringResource(R.string.onboarding_next)
                                },
                                style = MaterialTheme.typography.labelLarge
                            )
                            if (!isLastPage) {
                                Spacer(modifier = Modifier.width(8.dp))
                                Icon(
                                    imageVector = Icons.AutoMirrored.Filled.ArrowForward,
                                    contentDescription = null,
                                    modifier = Modifier.size(18.dp)
                                )
                            }
                        }
                    }
                }
            }
        }
    }
}

@Composable
private fun OnboardingPageContent(
    page: OnboardingPageData,
    pageIndex: Int,
    modifier: Modifier = Modifier
) {
    val sanaColors = LocalSanaColors.current

    Column(
        modifier = modifier
            .verticalScroll(rememberScrollState())
            .padding(vertical = 8.dp)
            .testTag("onboarding_page_$pageIndex"),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.spacedBy(16.dp)
    ) {
        // Visual Showcase Card
        Card(
            shape = MaterialTheme.shapes.extraLarge,
            colors = CardDefaults.cardColors(containerColor = sanaColors.heroBackground),
            elevation = CardDefaults.cardElevation(defaultElevation = 3.dp),
            modifier = Modifier
                .fillMaxWidth()
                .height(210.dp)
        ) {
            Box(modifier = Modifier.fillMaxSize()) {
                if (page.useHeroArtwork) {
                    Image(
                        painter = painterResource(id = R.drawable.img_sana_onboarding),
                        contentDescription = stringResource(R.string.onboarding_hero_cd),
                        contentScale = ContentScale.Crop,
                        modifier = Modifier.fillMaxSize()
                    )
                    Box(
                        modifier = Modifier
                            .fillMaxSize()
                            .background(
                                Brush.verticalGradient(
                                    listOf(
                                        Color.Transparent,
                                        sanaColors.heroBackground.copy(alpha = 0.72f)
                                    )
                                )
                            )
                    )
                } else {
                    Box(
                        modifier = Modifier
                            .fillMaxSize()
                            .background(
                                Brush.linearGradient(
                                    listOf(
                                        sanaColors.heroBackground,
                                        page.accentColor.copy(alpha = 0.55f)
                                    )
                                )
                            )
                    )
                }

                // Floating Icon Cluster
                Row(
                    modifier = Modifier
                        .align(Alignment.BottomStart)
                        .padding(18.dp),
                    horizontalArrangement = Arrangement.spacedBy(10.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Surface(
                        shape = RoundedCornerShape(14.dp),
                        color = page.accentColor,
                        shadowElevation = 4.dp,
                        modifier = Modifier.size(48.dp)
                    ) {
                        Box(contentAlignment = Alignment.Center) {
                            Icon(
                                imageVector = page.primaryIcon,
                                contentDescription = null,
                                tint = Color.White,
                                modifier = Modifier.size(24.dp)
                            )
                        }
                    }
                    Surface(
                        shape = RoundedCornerShape(14.dp),
                        color = MaterialTheme.colorScheme.surface.copy(alpha = 0.92f),
                        shadowElevation = 2.dp,
                        modifier = Modifier.size(42.dp)
                    ) {
                        Box(contentAlignment = Alignment.Center) {
                            Icon(
                                imageVector = page.secondaryIcon,
                                contentDescription = null,
                                tint = sanaColors.heroBackground,
                                modifier = Modifier.size(20.dp)
                            )
                        }
                    }
                }
            }
        }

        Column(
            modifier = Modifier.fillMaxWidth(),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            Text(
                text = page.title,
                style = MaterialTheme.typography.headlineSmall.copy(fontWeight = FontWeight.Bold),
                color = MaterialTheme.colorScheme.onBackground,
                textAlign = TextAlign.Center
            )
            Text(
                text = page.body,
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                textAlign = TextAlign.Center
            )
        }

        // Feature Highlight Cards
        Column(
            modifier = Modifier.fillMaxWidth(),
            verticalArrangement = Arrangement.spacedBy(10.dp)
        ) {
            page.highlights.forEach { highlight ->
                Card(
                    shape = MaterialTheme.shapes.medium,
                    colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
                    border = BorderStroke(1.dp, sanaColors.cardBorder),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(14.dp),
                        horizontalArrangement = Arrangement.spacedBy(12.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Box(
                            modifier = Modifier
                                .size(40.dp)
                                .clip(RoundedCornerShape(12.dp))
                                .background(sanaColors.tealTint),
                            contentAlignment = Alignment.Center
                        ) {
                            Icon(
                                imageVector = highlight.icon,
                                contentDescription = null,
                                tint = sanaColors.tealDeep,
                                modifier = Modifier.size(20.dp)
                            )
                        }
                        Column(modifier = Modifier.weight(1f)) {
                            Text(
                                text = highlight.label,
                                style = MaterialTheme.typography.titleSmall.copy(fontWeight = FontWeight.SemiBold),
                                color = MaterialTheme.colorScheme.onSurface
                            )
                            Text(
                                text = highlight.detail,
                                style = MaterialTheme.typography.bodySmall,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                        }
                    }
                }
            }
        }
    }
}
