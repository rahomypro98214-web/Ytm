package com.example.ui

import androidx.activity.compose.BackHandler
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.Logout
import androidx.compose.material.icons.filled.AutoFixHigh
import androidx.compose.material.icons.filled.ChatBubble
import androidx.compose.material.icons.filled.DeleteSweep
import androidx.compose.material.icons.filled.Info
import androidx.compose.material.icons.filled.Psychology
import androidx.compose.material.icons.filled.School
import androidx.compose.material.icons.filled.Settings
import androidx.compose.material.icons.outlined.AutoFixHigh
import androidx.compose.material.icons.outlined.ChatBubbleOutline
import androidx.compose.material.icons.outlined.Psychology
import androidx.compose.material.icons.outlined.School
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.ModalBottomSheet
import androidx.compose.material3.NavigationBar
import androidx.compose.material3.NavigationBarItem
import androidx.compose.material3.NavigationRail
import androidx.compose.material3.NavigationRailItem
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.credentials.CredentialManager
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.example.R
import com.example.firebase.signOutUser
import com.example.ui.components.SettingsMenuDialog
import com.example.ui.screens.ChatScreen
import com.example.ui.screens.LearnScreen
import com.example.ui.screens.SelfLearnScreen
import com.example.ui.screens.StudioScreen
import com.example.ui.theme.LocalSanaColors

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun SanaAppShell(
    viewModel: SanaViewModel,
    userDisplayName: String?,
    onSignedOut: () -> Unit,
    onReplayOnboarding: (() -> Unit)? = null
) {
    val context = LocalContext.current
    val scope = rememberCoroutineScope()
    val credentialManager = remember(context) { CredentialManager.create(context) }
    val sanaColors = LocalSanaColors.current

    val selectedTab by viewModel.selectedTab.collectAsStateWithLifecycle()
    val learnSession by viewModel.learnSession.collectAsStateWithLifecycle()
    val chatMessagesState by viewModel.chatMessagesState.collectAsStateWithLifecycle()
    val preferredName by viewModel.preferredName.collectAsStateWithLifecycle()
    val hasChatMessages = (chatMessagesState as? com.example.data.model.UiState.Success)
        ?.data?.isNotEmpty() == true
    val effectiveDisplayName = preferredName?.takeIf { it.isNotBlank() } ?: userDisplayName

    var showMenu by remember { mutableStateOf(false) }
    var showAboutSheet by remember { mutableStateOf(false) }
    var showClearChatDialog by remember { mutableStateOf(false) }
    var showSettingsDialog by remember { mutableStateOf(false) }

    // Return to Chat tab when back is pressed on top-level secondary tabs
    BackHandler(enabled = selectedTab != SanaTab.CHAT && learnSession.subView == LearnSubView.LIST) {
        viewModel.selectTab(SanaTab.CHAT)
    }

    if (showClearChatDialog) {
        AlertDialog(
            onDismissRequest = { showClearChatDialog = false },
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
                        showClearChatDialog = false
                        viewModel.clearChatHistory()
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
                TextButton(onClick = { showClearChatDialog = false }) {
                    Text(stringResource(R.string.cancel))
                }
            }
        )
    }

    if (showSettingsDialog) {
        SettingsMenuDialog(
            currentPreferredName = preferredName,
            accountDisplayName = userDisplayName,
            onSavePreferredName = { viewModel.updatePreferredName(it) },
            onClearPreferredName = { viewModel.clearPreferredName() },
            onDismiss = { showSettingsDialog = false }
        )
    }

    if (showAboutSheet) {
        ModalBottomSheet(onDismissRequest = { showAboutSheet = false }) {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 24.dp)
                    .padding(bottom = 32.dp),
                verticalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(120.dp)
                        .clip(MaterialTheme.shapes.medium)
                ) {
                    Image(
                        painter = painterResource(id = R.drawable.img_sana_hero),
                        contentDescription = "سنا",
                        contentScale = ContentScale.Crop,
                        modifier = Modifier.fillMaxSize()
                    )
                    Box(
                        modifier = Modifier
                            .fillMaxSize()
                            .background(sanaColors.heroBackground.copy(alpha = 0.45f))
                    )
                    Text(
                        text = "سنا — ذكاء يفهم لغتك، ويتعلم، ويعلّمك",
                        style = MaterialTheme.typography.titleLarge,
                        color = sanaColors.heroOnBackground,
                        modifier = Modifier
                            .align(Alignment.BottomStart)
                            .padding(14.dp)
                    )
                }

                Text(
                    text = "تجمع سنا بين المحادثة العربية الذكية (Gemini 3.8 Flash)، وبناء ذاكرة معرفية من صفحات الويب والنصوص، ونظام دورات واختبارات تفاعلية متزامنة سحابيًا.",
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
        }
    }

    BoxWithConstraints(modifier = Modifier.fillMaxSize()) {
        val useRail = maxWidth >= 600.dp

        Scaffold(
            topBar = {
                TopAppBar(
                    colors = TopAppBarDefaults.topAppBarColors(
                        containerColor = sanaColors.heroBackground,
                        titleContentColor = sanaColors.heroOnBackground,
                        actionIconContentColor = sanaColors.heroOnBackground
                    ),
                    title = {
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(10.dp)
                        ) {
                            Box(
                                modifier = Modifier
                                    .size(34.dp)
                                    .clip(RoundedCornerShape(10.dp))
                                    .background(
                                        Brush.linearGradient(
                                            listOf(sanaColors.tealAccent, sanaColors.mintGlow)
                                        )
                                    ),
                                contentAlignment = Alignment.Center
                            ) {
                                Text(
                                    text = "س",
                                    style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.ExtraBold),
                                    color = sanaColors.heroBackground
                                )
                            }
                            Column {
                                Text(
                                    text = stringResource(R.string.app_name),
                                    style = MaterialTheme.typography.titleLarge
                                )
                                Row(
                                    verticalAlignment = Alignment.CenterVertically,
                                    horizontalArrangement = Arrangement.spacedBy(5.dp)
                                ) {
                                    Box(
                                        modifier = Modifier
                                            .size(7.dp)
                                            .clip(CircleShape)
                                            .background(sanaColors.mintGlow)
                                    )
                                    val safePreferredName = preferredName?.takeIf { it.isNotBlank() }
                                    Text(
                                        text = if (safePreferredName != null) {
                                            stringResource(R.string.top_greeting_format, safePreferredName)
                                        } else {
                                            "متصل بـ Gemini · ذاكرة سحابية"
                                        },
                                        style = MaterialTheme.typography.labelSmall,
                                        color = if (safePreferredName != null) {
                                            sanaColors.mintGlow
                                        } else {
                                            sanaColors.heroSubtext
                                        },
                                        modifier = Modifier.testTag("top_bar_personalized_greeting")
                                    )
                                }
                            }
                        }
                    },
                    actions = {
                        IconButton(
                            onClick = { showSettingsDialog = true },
                            modifier = Modifier.testTag("settings_button")
                        ) {
                            Icon(
                                imageVector = Icons.Filled.Settings,
                                contentDescription = stringResource(R.string.settings_cd)
                            )
                        }

                        IconButton(
                            onClick = { showAboutSheet = true },
                            modifier = Modifier.testTag("about_sana_button")
                        ) {
                            Icon(
                                imageVector = Icons.Filled.Info,
                                contentDescription = stringResource(R.string.about_sana)
                            )
                        }

                        Box {
                            IconButton(
                                onClick = { showMenu = true },
                                modifier = Modifier.testTag("account_menu_button")
                            ) {
                                Surface(
                                    shape = CircleShape,
                                    color = sanaColors.tealAccent.copy(alpha = 0.24f),
                                    modifier = Modifier.size(32.dp)
                                ) {
                                    Box(contentAlignment = Alignment.Center) {
                                        Text(
                                            text = effectiveDisplayName?.firstOrNull()?.toString() ?: "س",
                                            style = MaterialTheme.typography.labelLarge,
                                            color = sanaColors.mintGlow
                                        )
                                    }
                                }
                            }

                            DropdownMenu(
                                expanded = showMenu,
                                onDismissRequest = { showMenu = false }
                            ) {
                                val menuGreetingName = preferredName?.takeIf { it.isNotBlank() }
                                val menuDisplayName = effectiveDisplayName?.takeIf { it.isNotBlank() }
                                if (menuDisplayName != null) {
                                    DropdownMenuItem(
                                        text = {
                                            Text(
                                                text = if (menuGreetingName != null) {
                                                    stringResource(R.string.top_greeting_format, menuGreetingName)
                                                } else {
                                                    menuDisplayName
                                                },
                                                style = MaterialTheme.typography.titleSmall
                                            )
                                        },
                                        onClick = {
                                            showMenu = false
                                            showSettingsDialog = true
                                        }
                                    )
                                }
                                DropdownMenuItem(
                                    text = { Text(stringResource(R.string.settings_title)) },
                                    leadingIcon = {
                                        Icon(
                                            imageVector = Icons.Filled.Settings,
                                            contentDescription = null
                                        )
                                    },
                                    onClick = {
                                        showMenu = false
                                        showSettingsDialog = true
                                    },
                                    modifier = Modifier.testTag("settings_menu_item")
                                )
                                if (onReplayOnboarding != null) {
                                    DropdownMenuItem(
                                        text = { Text(stringResource(R.string.onboarding_replay_tour)) },
                                        leadingIcon = {
                                            Icon(
                                                imageVector = Icons.Filled.Info,
                                                contentDescription = null
                                            )
                                        },
                                        onClick = {
                                            showMenu = false
                                            onReplayOnboarding()
                                        },
                                        modifier = Modifier.testTag("replay_onboarding_menu_item")
                                    )
                                }
                                if (selectedTab == SanaTab.CHAT) {
                                    DropdownMenuItem(
                                        text = {
                                            Text(
                                                text = stringResource(R.string.clear_chat_history),
                                                color = if (hasChatMessages) {
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
                                                tint = if (hasChatMessages) {
                                                    sanaColors.errorText
                                                } else {
                                                    MaterialTheme.colorScheme.onSurface.copy(alpha = 0.38f)
                                                }
                                            )
                                        },
                                        enabled = hasChatMessages,
                                        onClick = {
                                            showMenu = false
                                            showClearChatDialog = true
                                        },
                                        modifier = Modifier.testTag("top_bar_clear_chat_menu_item")
                                    )
                                    HorizontalDivider()
                                }
                                DropdownMenuItem(
                                    text = { Text(stringResource(R.string.sign_out)) },
                                    leadingIcon = {
                                        Icon(
                                            imageVector = Icons.AutoMirrored.Filled.Logout,
                                            contentDescription = null
                                        )
                                    },
                                    onClick = {
                                        showMenu = false
                                        signOutUser(credentialManager, scope, onSignedOut)
                                    },
                                    modifier = Modifier.testTag("sign_out_menu_item")
                                )
                            }
                        }
                    }
                )
            },
            bottomBar = {
                if (!useRail) {
                    NavigationBar(
                        containerColor = MaterialTheme.colorScheme.surface,
                        windowInsets = WindowInsets(0, 0, 0, 0),
                        modifier = Modifier.navigationBarsPadding()
                    ) {
                        SanaTab.entries.forEach { tab ->
                            val selected = selectedTab == tab
                            NavigationBarItem(
                                selected = selected,
                                onClick = { viewModel.selectTab(tab) },
                                icon = {
                                    Icon(
                                        imageVector = tabIcon(tab, selected),
                                        contentDescription = tab.label
                                    )
                                },
                                label = {
                                    Text(
                                        text = tab.label,
                                        style = MaterialTheme.typography.labelMedium
                                    )
                                },
                                modifier = Modifier.testTag("nav_tab_${tab.name.lowercase()}")
                            )
                        }
                    }
                }
            }
        ) { innerPadding ->
            Row(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(innerPadding)
            ) {
                if (useRail) {
                    NavigationRail(
                        containerColor = MaterialTheme.colorScheme.surface,
                        modifier = Modifier.fillMaxHeight()
                    ) {
                        Spacer(modifier = Modifier.height(12.dp))
                        SanaTab.entries.forEach { tab ->
                            val selected = selectedTab == tab
                            NavigationRailItem(
                                selected = selected,
                                onClick = { viewModel.selectTab(tab) },
                                icon = {
                                    Icon(
                                        imageVector = tabIcon(tab, selected),
                                        contentDescription = tab.label
                                    )
                                },
                                label = {
                                    Text(
                                        text = tab.label,
                                        style = MaterialTheme.typography.labelMedium
                                    )
                                },
                                modifier = Modifier.testTag("rail_tab_${tab.name.lowercase()}")
                            )
                        }
                    }
                }

                Box(
                    modifier = Modifier
                        .weight(1f)
                        .fillMaxHeight(),
                    contentAlignment = Alignment.TopCenter
                ) {
                    Box(
                        modifier = Modifier
                            .widthIn(max = 760.dp)
                            .fillMaxSize()
                    ) {
                        when (selectedTab) {
                            SanaTab.CHAT -> ChatScreen(viewModel = viewModel)
                            SanaTab.STUDIO -> StudioScreen(viewModel = viewModel)
                            SanaTab.LEARN -> LearnScreen(viewModel = viewModel)
                            SanaTab.SELF_LEARN -> SelfLearnScreen(viewModel = viewModel)
                        }
                    }
                }
            }
        }
    }
}

private fun tabIcon(tab: SanaTab, selected: Boolean) = when (tab) {
    SanaTab.CHAT -> if (selected) Icons.Filled.ChatBubble else Icons.Outlined.ChatBubbleOutline
    SanaTab.STUDIO -> if (selected) Icons.Filled.AutoFixHigh else Icons.Outlined.AutoFixHigh
    SanaTab.LEARN -> if (selected) Icons.Filled.School else Icons.Outlined.School
    SanaTab.SELF_LEARN -> if (selected) Icons.Filled.Psychology else Icons.Outlined.Psychology
}
