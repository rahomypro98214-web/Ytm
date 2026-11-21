package com.example.ui.screens

import android.app.Activity
import android.content.ActivityNotFoundException
import android.content.Context
import android.content.Intent
import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.net.Uri
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.PickVisualMediaRequest
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.animation.AnimatedContent
import androidx.compose.animation.core.LinearEasing
import androidx.compose.animation.core.RepeatMode
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.tween
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.AddPhotoAlternate
import androidx.compose.material.icons.filled.AutoFixHigh
import androidx.compose.material.icons.filled.Clear
import androidx.compose.material.icons.filled.Image
import androidx.compose.material.icons.filled.Info
import androidx.compose.material.icons.filled.Movie
import androidx.compose.material.icons.filled.Pause
import androidx.compose.material.icons.filled.PlayArrow
import androidx.compose.material.icons.filled.Share
import androidx.compose.material.icons.filled.Videocam
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.FilterChip
import androidx.compose.material3.FilterChipDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.OutlinedTextFieldDefaults
import androidx.compose.material3.SuggestionChip
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.minimumInteractiveComponentSize
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.asImageBitmap
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.core.content.FileProvider
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.example.R
import com.example.data.model.UiState
import com.example.data.repository.StudioAspectRatio
import com.example.data.repository.VideoScenePlan
import com.example.data.repository.VideoStoryboardPlan
import com.example.ui.GeneratedImageResult
import com.example.ui.SanaViewModel
import com.example.ui.components.TextToSpeechController
import com.example.ui.components.rememberTextToSpeechController
import com.example.ui.theme.LocalSanaColors
import java.io.File
import java.io.FileOutputStream
import kotlinx.coroutines.delay

enum class StudioMode {
    IMAGE, VIDEO
}

@Composable
fun StudioScreen(
    viewModel: SanaViewModel,
    modifier: Modifier = Modifier
) {
    val imageState by viewModel.studioImageState.collectAsStateWithLifecycle()
    val storyboardState by viewModel.studioStoryboardState.collectAsStateWithLifecycle()
    val sceneImages by viewModel.studioSceneImages.collectAsStateWithLifecycle()

    StudioScreenContent(
        imageState = imageState,
        storyboardState = storyboardState,
        sceneImages = sceneImages,
        onGenerateImage = { prompt, aspect, refBitmap ->
            viewModel.generateStudioImage(prompt, aspect, refBitmap)
        },
        onGenerateVideoStoryboard = { topic, aspect ->
            viewModel.generateVideoStoryboard(topic, aspect)
        },
        onGenerateSceneFrame = { index, visualPrompt, aspect ->
            viewModel.generateSceneFrame(index, visualPrompt, aspect)
        },
        modifier = modifier
    )
}

@Composable
internal fun StudioScreenContent(
    imageState: UiState<GeneratedImageResult?>?,
    storyboardState: UiState<VideoStoryboardPlan?>?,
    sceneImages: Map<Int, UiState<Bitmap>>,
    onGenerateImage: (String, StudioAspectRatio, Bitmap?) -> Unit,
    onGenerateVideoStoryboard: (String, StudioAspectRatio) -> Unit,
    onGenerateSceneFrame: (Int, String, StudioAspectRatio) -> Unit,
    modifier: Modifier = Modifier,
    ttsController: TextToSpeechController = rememberTextToSpeechController()
) {
    val context = LocalContext.current
    val sanaColors = LocalSanaColors.current

    var activeMode by rememberSaveable { mutableStateOf(StudioMode.IMAGE) }
    var imagePrompt by rememberSaveable { mutableStateOf("") }
    var selectedImageAspect by rememberSaveable { mutableStateOf(StudioAspectRatio.SQUARE) }
    var referenceBitmap by remember { mutableStateOf<Bitmap?>(null) }

    var videoTopic by rememberSaveable { mutableStateOf("") }
    var selectedVideoAspect by rememberSaveable { mutableStateOf(StudioAspectRatio.LANDSCAPE) }

    val imagePickerLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.PickVisualMedia()
    ) { uri: Uri? ->
        if (uri != null) {
            referenceBitmap = decodeSampledBitmap(context, uri)
        }
    }

    val imageIdeas = remember {
        listOf(
            "مكتبة عربية دافئة مضيئة بالفوانيس عند الغروب",
            "مدينة مستقبلية مستدامة بحدائق معلقة وأسلوب واقعي",
            "لوحة خط عربي ذهبي على خلفية زمردية حديثة"
        )
    }

    val videoIdeas = remember {
        listOf(
            "قصة تطور الخط العربي في ٢٠ ثانية",
            "رحلة سريعة إلى مستقبل المدن الذكية الخضراء",
            "نصائح يومية ملهمة للتعلّم المستمر"
        )
    }

    Column(
        modifier = modifier
            .fillMaxSize()
            .background(sanaColors.chatCanvas)
            .verticalScroll(rememberScrollState())
            .padding(16.dp),
        verticalArrangement = Arrangement.spacedBy(14.dp)
    ) {
        // Studio Header + Mode Selector
        Card(
            shape = MaterialTheme.shapes.large,
            colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
            border = BorderStroke(1.dp, sanaColors.cardBorder)
        ) {
                Column(
                    modifier = Modifier.padding(16.dp),
                    verticalArrangement = Arrangement.spacedBy(10.dp)
                ) {
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(10.dp)
                    ) {
                        Box(
                            modifier = Modifier
                                .size(38.dp)
                                .clip(RoundedCornerShape(10.dp))
                                .background(sanaColors.tealTint),
                            contentAlignment = Alignment.Center
                        ) {
                            Icon(
                                imageVector = Icons.Filled.AutoFixHigh,
                                contentDescription = null,
                                tint = sanaColors.tealDeep
                            )
                        }
                        Text(
                            text = stringResource(R.string.studio_title),
                            style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.Bold),
                            color = MaterialTheme.colorScheme.onSurface
                        )
                    }

                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(10.dp)
                    ) {
                        FilterChip(
                            selected = activeMode == StudioMode.IMAGE,
                            onClick = {
                                ttsController.stop()
                                activeMode = StudioMode.IMAGE
                            },
                            leadingIcon = {
                                Icon(
                                    imageVector = Icons.Filled.Image,
                                    contentDescription = null,
                                    modifier = Modifier.size(18.dp)
                                )
                            },
                            label = { Text(stringResource(R.string.studio_tab_images)) },
                            colors = FilterChipDefaults.filterChipColors(
                                selectedContainerColor = sanaColors.tealDeep,
                                selectedLabelColor = sanaColors.heroOnBackground,
                                selectedLeadingIconColor = sanaColors.heroOnBackground
                            ),
                            modifier = Modifier
                                .weight(1f)
                                .testTag("studio_mode_image")
                        )

                        FilterChip(
                            selected = activeMode == StudioMode.VIDEO,
                            onClick = { activeMode = StudioMode.VIDEO },
                            leadingIcon = {
                                Icon(
                                    imageVector = Icons.Filled.Videocam,
                                    contentDescription = null,
                                    modifier = Modifier.size(18.dp)
                                )
                            },
                            label = { Text(stringResource(R.string.studio_tab_video)) },
                            colors = FilterChipDefaults.filterChipColors(
                                selectedContainerColor = sanaColors.tealDeep,
                                selectedLabelColor = sanaColors.heroOnBackground,
                                selectedLeadingIconColor = sanaColors.heroOnBackground
                            ),
                            modifier = Modifier
                                .weight(1f)
                                .testTag("studio_mode_video")
                        )
                    }
                }
            }

        if (activeMode == StudioMode.IMAGE) {
            // Image Generator Form
            Card(
                shape = MaterialTheme.shapes.large,
                colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
                border = BorderStroke(1.dp, sanaColors.cardBorder)
            ) {
                    Column(
                        modifier = Modifier.padding(16.dp),
                        verticalArrangement = Arrangement.spacedBy(12.dp)
                    ) {
                        OutlinedTextField(
                            value = imagePrompt,
                            onValueChange = { imagePrompt = it },
                            label = { Text(stringResource(R.string.studio_image_prompt_label)) },
                            placeholder = { Text(stringResource(R.string.studio_image_prompt_placeholder)) },
                            minLines = 2,
                            maxLines = 4,
                            shape = RoundedCornerShape(14.dp),
                            colors = OutlinedTextFieldDefaults.colors(
                                focusedContainerColor = sanaColors.chatCanvas,
                                unfocusedContainerColor = sanaColors.chatCanvas
                            ),
                            modifier = Modifier
                                .fillMaxWidth()
                                .testTag("studio_image_prompt_input")
                        )

                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .horizontalScroll(rememberScrollState()),
                            horizontalArrangement = Arrangement.spacedBy(8.dp)
                        ) {
                            imageIdeas.forEach { idea ->
                                SuggestionChip(
                                    onClick = { imagePrompt = idea },
                                    label = { Text(idea, style = MaterialTheme.typography.labelSmall) }
                                )
                            }
                        }

                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .horizontalScroll(rememberScrollState()),
                            horizontalArrangement = Arrangement.spacedBy(8.dp)
                        ) {
                            StudioAspectRatio.entries.forEach { ratio ->
                                FilterChip(
                                    selected = selectedImageAspect == ratio,
                                    onClick = { selectedImageAspect = ratio },
                                    label = { Text(ratio.label, style = MaterialTheme.typography.labelSmall) },
                                    modifier = Modifier.testTag("studio_image_aspect_${ratio.name.lowercase()}")
                                )
                            }
                        }

                        // Optional Reference Image Picker (Zero-permission Photo Picker)
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(10.dp),
                            modifier = Modifier.fillMaxWidth()
                        ) {
                            OutlinedButton(
                                onClick = {
                                    imagePickerLauncher.launch(
                                        PickVisualMediaRequest(ActivityResultContracts.PickVisualMedia.ImageOnly)
                                    )
                                },
                                modifier = Modifier
                                    .minimumInteractiveComponentSize()
                                    .testTag("studio_pick_reference_button")
                            ) {
                                Icon(
                                    imageVector = Icons.Filled.AddPhotoAlternate,
                                    contentDescription = null,
                                    modifier = Modifier.size(18.dp)
                                )
                                Spacer(modifier = Modifier.width(6.dp))
                                Text(stringResource(R.string.studio_pick_reference_image))
                            }

                            if (referenceBitmap != null) {
                                Image(
                                    bitmap = referenceBitmap!!.asImageBitmap(),
                                    contentDescription = stringResource(R.string.studio_pick_reference_image),
                                    contentScale = ContentScale.Crop,
                                    modifier = Modifier
                                        .size(44.dp)
                                        .clip(RoundedCornerShape(8.dp))
                                )
                                IconButton(
                                    onClick = { referenceBitmap = null },
                                    modifier = Modifier
                                        .minimumInteractiveComponentSize()
                                        .testTag("studio_clear_reference_button")
                                ) {
                                    Icon(
                                        imageVector = Icons.Filled.Clear,
                                        contentDescription = stringResource(R.string.studio_clear_reference_image)
                                    )
                                }
                            }
                        }

                        Button(
                            onClick = {
                                onGenerateImage(imagePrompt, selectedImageAspect, referenceBitmap)
                            },
                            enabled = imagePrompt.isNotBlank() && imageState !is UiState.Loading,
                            modifier = Modifier
                                .fillMaxWidth()
                                .testTag("studio_generate_image_button")
                        ) {
                            Text(stringResource(R.string.studio_generate_image))
                        }
                    }
                }

            // Image Generation Result / Loading / Error
            when (imageState) {
                    is UiState.Loading -> {
                        Card(
                            shape = MaterialTheme.shapes.large,
                            colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
                            border = BorderStroke(1.dp, sanaColors.tealAccent),
                            modifier = Modifier
                                .fillMaxWidth()
                                .testTag("studio_image_loading")
                        ) {
                            Column(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .padding(24.dp),
                                horizontalAlignment = Alignment.CenterHorizontally,
                                verticalArrangement = Arrangement.spacedBy(12.dp)
                            ) {
                                CircularProgressIndicator(color = sanaColors.tealDeep)
                                Text(
                                    text = stringResource(R.string.studio_generating_image),
                                    style = MaterialTheme.typography.bodyMedium,
                                    color = MaterialTheme.colorScheme.onSurfaceVariant
                                )
                            }
                        }
                    }

                    is UiState.Error -> {
                        Surface(
                            shape = MaterialTheme.shapes.medium,
                            color = sanaColors.errorSurface,
                            border = BorderStroke(1.dp, sanaColors.errorText.copy(alpha = 0.35f)),
                            modifier = Modifier
                                .fillMaxWidth()
                                .testTag("studio_image_error")
                        ) {
                            Text(
                                text = imageState.message,
                                style = MaterialTheme.typography.bodyMedium,
                                color = sanaColors.errorText,
                                modifier = Modifier.padding(14.dp)
                            )
                        }
                    }

                    is UiState.Success -> {
                        val result = imageState.data
                        if (result != null) {
                            Card(
                                shape = MaterialTheme.shapes.large,
                                colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
                                border = BorderStroke(1.dp, sanaColors.cardBorder),
                                modifier = Modifier.fillMaxWidth()
                            ) {
                                Column(
                                    modifier = Modifier.padding(14.dp),
                                    verticalArrangement = Arrangement.spacedBy(10.dp)
                                ) {
                                    Image(
                                        bitmap = result.bitmap.asImageBitmap(),
                                        contentDescription = result.prompt,
                                        contentScale = ContentScale.Fit,
                                        modifier = Modifier
                                            .fillMaxWidth()
                                            .aspectRatio(result.aspectRatio.ratioValue)
                                            .clip(RoundedCornerShape(14.dp))
                                            .testTag("studio_generated_image")
                                    )
                                    Text(
                                        text = result.prompt,
                                        style = MaterialTheme.typography.bodySmall,
                                        color = MaterialTheme.colorScheme.onSurfaceVariant
                                    )
                                    OutlinedButton(
                                        onClick = { shareGeneratedBitmap(context, result.bitmap, result.prompt) },
                                        modifier = Modifier
                                            .align(Alignment.End)
                                            .testTag("studio_share_image_button")
                                    ) {
                                        Icon(
                                            imageVector = Icons.Filled.Share,
                                            contentDescription = null,
                                            modifier = Modifier.size(18.dp)
                                        )
                                        Spacer(modifier = Modifier.width(6.dp))
                                        Text(stringResource(R.string.studio_share_image))
                                    }
                                }
                            }
                        }
                    }

                    null -> Unit
                }
        } else {
            // Video Storyboard & Animated Reel Form
            Card(
                shape = MaterialTheme.shapes.large,
                colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
                border = BorderStroke(1.dp, sanaColors.cardBorder)
            ) {
                    Column(
                        modifier = Modifier.padding(16.dp),
                        verticalArrangement = Arrangement.spacedBy(12.dp)
                    ) {
                        Surface(
                            shape = RoundedCornerShape(12.dp),
                            color = sanaColors.tealTint,
                            modifier = Modifier.fillMaxWidth()
                        ) {
                            Row(
                                modifier = Modifier.padding(10.dp),
                                horizontalArrangement = Arrangement.spacedBy(8.dp),
                                verticalAlignment = Alignment.Top
                            ) {
                                Icon(
                                    imageVector = Icons.Filled.Info,
                                    contentDescription = null,
                                    tint = sanaColors.tealDeep,
                                    modifier = Modifier.size(18.dp)
                                )
                                Text(
                                    text = stringResource(R.string.studio_video_honest_note),
                                    style = MaterialTheme.typography.bodySmall,
                                    color = sanaColors.tealDeep
                                )
                            }
                        }

                        OutlinedTextField(
                            value = videoTopic,
                            onValueChange = { videoTopic = it },
                            label = { Text(stringResource(R.string.studio_video_prompt_label)) },
                            placeholder = { Text(stringResource(R.string.studio_video_prompt_placeholder)) },
                            minLines = 2,
                            maxLines = 4,
                            shape = RoundedCornerShape(14.dp),
                            colors = OutlinedTextFieldDefaults.colors(
                                focusedContainerColor = sanaColors.chatCanvas,
                                unfocusedContainerColor = sanaColors.chatCanvas
                            ),
                            modifier = Modifier
                                .fillMaxWidth()
                                .testTag("studio_video_prompt_input")
                        )

                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .horizontalScroll(rememberScrollState()),
                            horizontalArrangement = Arrangement.spacedBy(8.dp)
                        ) {
                            videoIdeas.forEach { idea ->
                                SuggestionChip(
                                    onClick = { videoTopic = idea },
                                    label = { Text(idea, style = MaterialTheme.typography.labelSmall) }
                                )
                            }
                        }

                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .horizontalScroll(rememberScrollState()),
                            horizontalArrangement = Arrangement.spacedBy(8.dp)
                        ) {
                            StudioAspectRatio.entries.forEach { ratio ->
                                FilterChip(
                                    selected = selectedVideoAspect == ratio,
                                    onClick = { selectedVideoAspect = ratio },
                                    label = { Text(ratio.label, style = MaterialTheme.typography.labelSmall) },
                                    modifier = Modifier.testTag("studio_video_aspect_${ratio.name.lowercase()}")
                                )
                            }
                        }

                        Button(
                            onClick = {
                                ttsController.stop()
                                onGenerateVideoStoryboard(videoTopic, selectedVideoAspect)
                            },
                            enabled = videoTopic.isNotBlank() && storyboardState !is UiState.Loading,
                            modifier = Modifier
                                .fillMaxWidth()
                                .testTag("studio_generate_video_button")
                        ) {
                            Text(stringResource(R.string.studio_generate_video))
                        }
                    }
                }

            when (storyboardState) {
                is UiState.Loading -> {
                    Card(
                        shape = MaterialTheme.shapes.large,
                        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
                        border = BorderStroke(1.dp, sanaColors.tealAccent),
                        modifier = Modifier
                            .fillMaxWidth()
                            .testTag("studio_video_loading")
                    ) {
                        Column(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(24.dp),
                            horizontalAlignment = Alignment.CenterHorizontally,
                            verticalArrangement = Arrangement.spacedBy(12.dp)
                        ) {
                            CircularProgressIndicator(color = sanaColors.tealDeep)
                            Text(
                                text = stringResource(R.string.studio_generating_video),
                                style = MaterialTheme.typography.bodyMedium,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                        }
                    }
                }

                is UiState.Error -> {
                    Surface(
                        shape = MaterialTheme.shapes.medium,
                        color = sanaColors.errorSurface,
                        border = BorderStroke(1.dp, sanaColors.errorText.copy(alpha = 0.35f)),
                        modifier = Modifier
                            .fillMaxWidth()
                            .testTag("studio_video_error")
                    ) {
                        Text(
                            text = storyboardState.message,
                            style = MaterialTheme.typography.bodyMedium,
                            color = sanaColors.errorText,
                            modifier = Modifier.padding(14.dp)
                        )
                    }
                }

                is UiState.Success -> {
                    val plan = storyboardState.data
                    if (plan != null) {
                        AnimatedVideoStoryboardPlayer(
                            plan = plan,
                            sceneImages = sceneImages,
                            ttsController = ttsController
                        )

                        plan.scenes.forEachIndexed { index, scene ->
                            ScenePlanCard(
                                index = index,
                                scene = scene,
                                aspectRatio = plan.aspectRatio,
                                frameState = sceneImages[index],
                                onGenerateFrame = {
                                    onGenerateSceneFrame(index, scene.visualPrompt, plan.aspectRatio)
                                }
                            )
                        }
                    }
                }

                null -> Unit
            }
        }
    }
}

@Composable
private fun AnimatedVideoStoryboardPlayer(
    plan: VideoStoryboardPlan,
    sceneImages: Map<Int, UiState<Bitmap>>,
    ttsController: TextToSpeechController
) {
    val context = LocalContext.current
    val sanaColors = LocalSanaColors.current
    var isPlaying by remember(plan) { mutableStateOf(false) }
    var currentSceneIndex by remember(plan) { mutableIntStateOf(0) }

    val safeIndex = currentSceneIndex.coerceIn(0, plan.scenes.lastIndex)
    val currentScene = plan.scenes[safeIndex]

    LaunchedEffect(isPlaying, safeIndex, plan) {
        if (isPlaying) {
            ttsController.toggleSpeak("scene_$safeIndex", currentScene.narrationArabic)
            delay(currentScene.durationSeconds * 1000L)
            if (safeIndex < plan.scenes.lastIndex) {
                currentSceneIndex = safeIndex + 1
            } else {
                isPlaying = false
                currentSceneIndex = 0
                ttsController.stop()
            }
        }
    }

    val infiniteTransition = rememberInfiniteTransition(label = "ken_burns")
    val zoomScale by infiniteTransition.animateFloat(
        initialValue = 1.0f,
        targetValue = if (isPlaying) 1.08f else 1.0f,
        animationSpec = infiniteRepeatable(
            animation = tween(durationMillis = 3500, easing = LinearEasing),
            repeatMode = RepeatMode.Reverse
        ),
        label = "ken_burns_zoom"
    )

    Card(
        shape = MaterialTheme.shapes.large,
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
        border = BorderStroke(1.dp, sanaColors.tealAccent),
        modifier = Modifier
            .fillMaxWidth()
            .testTag("studio_video_preview_player")
    ) {
        Column(
            modifier = Modifier.padding(14.dp),
            verticalArrangement = Arrangement.spacedBy(10.dp)
        ) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Column(modifier = Modifier.weight(1f)) {
                    Text(
                        text = plan.title,
                        style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.Bold),
                        color = MaterialTheme.colorScheme.onSurface
                    )
                    Text(
                        text = plan.mood,
                        style = MaterialTheme.typography.labelSmall,
                        color = sanaColors.tealDeep
                    )
                }
                IconButton(
                    onClick = { shareStoryboardScript(context, plan) },
                    modifier = Modifier
                        .minimumInteractiveComponentSize()
                        .testTag("studio_share_script_button")
                ) {
                    Icon(
                        imageVector = Icons.Filled.Share,
                        contentDescription = stringResource(R.string.studio_share_script)
                    )
                }
            }

            // Preview Stage
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .aspectRatio(plan.aspectRatio.ratioValue.coerceIn(0.65f, 1.85f))
                    .clip(RoundedCornerShape(16.dp))
                    .background(
                        Brush.linearGradient(
                            listOf(sanaColors.heroBackground, sanaColors.tealDeep)
                        )
                    )
            ) {
                val frameBitmap = (sceneImages[safeIndex] as? UiState.Success)?.data
                AnimatedContent(targetState = frameBitmap, label = "scene_frame") { bmp ->
                    if (bmp != null) {
                        Image(
                            bitmap = bmp.asImageBitmap(),
                            contentDescription = currentScene.visualPrompt,
                            contentScale = ContentScale.Crop,
                            modifier = Modifier
                                .fillMaxSize()
                                .graphicsLayer {
                                    scaleX = zoomScale
                                    scaleY = zoomScale
                                }
                        )
                    } else {
                        Column(
                            modifier = Modifier
                                .fillMaxSize()
                                .padding(20.dp),
                            horizontalAlignment = Alignment.CenterHorizontally,
                            verticalArrangement = Arrangement.Center
                        ) {
                            Icon(
                                imageVector = Icons.Filled.Movie,
                                contentDescription = null,
                                tint = sanaColors.mintGlow,
                                modifier = Modifier.size(34.dp)
                            )
                            Spacer(modifier = Modifier.height(6.dp))
                            Text(
                                text = currentScene.cameraMotion,
                                style = MaterialTheme.typography.labelMedium,
                                color = sanaColors.mintGlow
                            )
                        }
                    }
                }

                // Bottom subtitle overlay
                Box(
                    modifier = Modifier
                        .align(Alignment.BottomCenter)
                        .fillMaxWidth()
                        .background(sanaColors.heroBackground.copy(alpha = 0.78f))
                        .padding(horizontal = 12.dp, vertical = 8.dp)
                ) {
                    Text(
                        text = currentScene.narrationArabic,
                        style = MaterialTheme.typography.bodySmall.copy(fontWeight = FontWeight.Medium),
                        color = sanaColors.heroOnBackground
                    )
                }

                // Scene badge top-start
                Surface(
                    shape = RoundedCornerShape(999.dp),
                    color = sanaColors.heroBackground.copy(alpha = 0.75f),
                    modifier = Modifier
                        .align(Alignment.TopStart)
                        .padding(10.dp)
                ) {
                    Text(
                        text = "المشهد ${safeIndex + 1}/${plan.scenes.size} · ${currentScene.durationSeconds}ث",
                        style = MaterialTheme.typography.labelSmall,
                        color = sanaColors.mintGlow,
                        modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp)
                    )
                }
            }

            Button(
                onClick = {
                    if (isPlaying) {
                        isPlaying = false
                        ttsController.stop()
                    } else {
                        isPlaying = true
                    }
                },
                modifier = Modifier
                    .fillMaxWidth()
                    .testTag("studio_play_reel_button")
            ) {
                Icon(
                    imageVector = if (isPlaying) Icons.Filled.Pause else Icons.Filled.PlayArrow,
                    contentDescription = null,
                    modifier = Modifier.size(18.dp)
                )
                Spacer(modifier = Modifier.width(6.dp))
                Text(
                    if (isPlaying) stringResource(R.string.studio_pause_reel)
                    else stringResource(R.string.studio_play_reel)
                )
            }
        }
    }
}

@Composable
private fun ScenePlanCard(
    index: Int,
    scene: VideoScenePlan,
    aspectRatio: StudioAspectRatio,
    frameState: UiState<Bitmap>?,
    onGenerateFrame: () -> Unit
) {
    val sanaColors = LocalSanaColors.current
    Card(
        shape = MaterialTheme.shapes.medium,
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
        border = BorderStroke(1.dp, sanaColors.cardBorder),
        modifier = Modifier
            .fillMaxWidth()
            .testTag("studio_scene_card_$index")
    ) {
        Column(
            modifier = Modifier.padding(14.dp),
            verticalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    text = "المشهد ${scene.sceneNumber} · ${scene.durationSeconds} ثوانٍ",
                    style = MaterialTheme.typography.titleSmall.copy(fontWeight = FontWeight.Bold),
                    color = sanaColors.tealDeep
                )
                Text(
                    text = scene.cameraMotion,
                    style = MaterialTheme.typography.labelSmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }

            Text(
                text = "التعليق الصوتي: ${scene.narrationArabic}",
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurface
            )

            Text(
                text = "الوصف البصري: ${scene.visualPrompt}",
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )

            when (frameState) {
                is UiState.Loading -> {
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        CircularProgressIndicator(modifier = Modifier.size(18.dp), strokeWidth = 2.dp)
                        Text(
                            text = "جارٍ رسم لقطة المشهد…",
                            style = MaterialTheme.typography.labelSmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                }

                is UiState.Error -> {
                    Text(
                        text = frameState.message,
                        style = MaterialTheme.typography.bodySmall,
                        color = sanaColors.errorText
                    )
                }

                is UiState.Success -> {
                    Image(
                        bitmap = frameState.data.asImageBitmap(),
                        contentDescription = scene.visualPrompt,
                        contentScale = ContentScale.Crop,
                        modifier = Modifier
                            .fillMaxWidth()
                            .aspectRatio(aspectRatio.ratioValue)
                            .clip(RoundedCornerShape(12.dp))
                    )
                }

                null -> Unit
            }

            OutlinedButton(
                onClick = onGenerateFrame,
                enabled = frameState !is UiState.Loading,
                modifier = Modifier
                    .align(Alignment.End)
                    .testTag("studio_generate_scene_frame_$index")
            ) {
                Icon(
                    imageVector = Icons.Filled.Image,
                    contentDescription = null,
                    modifier = Modifier.size(16.dp)
                )
                Spacer(modifier = Modifier.width(6.dp))
                Text(stringResource(R.string.studio_generate_scene_frame))
            }
        }
    }
}

private fun decodeSampledBitmap(context: Context, uri: Uri): Bitmap? {
    return runCatching {
        context.contentResolver.openInputStream(uri)?.use { stream ->
            val original = BitmapFactory.decodeStream(stream) ?: return null
            val maxDim = 1024
            if (original.width <= maxDim && original.height <= maxDim) return original
            val scale = maxDim.toFloat() / maxOf(original.width, original.height)
            Bitmap.createScaledBitmap(
                original,
                (original.width * scale).toInt().coerceAtLeast(1),
                (original.height * scale).toInt().coerceAtLeast(1),
                true
            )
        }
    }.getOrNull()
}

private fun shareGeneratedBitmap(context: Context, bitmap: Bitmap, prompt: String) {
    runCatching {
        val dir = File(context.cacheDir, "shared_images").apply { mkdirs() }
        val file = File(dir, "sana_image_${System.currentTimeMillis()}.png")
        FileOutputStream(file).use { out ->
            bitmap.compress(Bitmap.CompressFormat.PNG, 95, out)
        }
        val uri = FileProvider.getUriForFile(context, "${context.packageName}.fileprovider", file)
        val shareIntent = Intent(Intent.ACTION_SEND).apply {
            type = "image/png"
            putExtra(Intent.EXTRA_STREAM, uri)
            putExtra(Intent.EXTRA_TEXT, prompt)
            addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION)
        }
        val chooser = Intent.createChooser(shareIntent, context.getString(R.string.studio_share_image)).apply {
            if (context !is Activity) addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
        }
        context.startActivity(chooser)
    }
}

private fun shareStoryboardScript(context: Context, plan: VideoStoryboardPlan) {
    val script = buildString {
        appendLine("🎬 ${plan.title}")
        appendLine("الطابع: ${plan.mood}")
        appendLine()
        plan.scenes.forEach { scene ->
            appendLine("المشهد ${scene.sceneNumber} (${scene.durationSeconds}ث) — ${scene.cameraMotion}")
            appendLine("التعليق: ${scene.narrationArabic}")
            appendLine("الوصف البصري: ${scene.visualPrompt}")
            appendLine()
        }
    }.trim()

    val shareIntent = Intent(Intent.ACTION_SEND).apply {
        type = "text/plain"
        putExtra(Intent.EXTRA_SUBJECT, plan.title)
        putExtra(Intent.EXTRA_TEXT, script)
    }
    val chooser = Intent.createChooser(shareIntent, context.getString(R.string.studio_share_script)).apply {
        if (context !is Activity) addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
    }
    try {
        context.startActivity(chooser)
    } catch (_: ActivityNotFoundException) {
        // Ignore when no handler is available
    }
}
