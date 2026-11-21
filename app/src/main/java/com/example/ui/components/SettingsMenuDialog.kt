package com.example.ui.components

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardActions
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.AutoAwesome
import androidx.compose.material.icons.filled.Clear
import androidx.compose.material.icons.filled.Person
import androidx.compose.material.icons.filled.Settings
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.minimumInteractiveComponentSize
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.unit.dp
import com.example.R
import com.example.data.repository.UserPreferencesRepository
import com.example.ui.theme.LocalSanaColors

@Composable
fun SettingsMenuDialog(
    currentPreferredName: String?,
    accountDisplayName: String? = null,
    onSavePreferredName: (String) -> Unit,
    onClearPreferredName: () -> Unit,
    onDismiss: () -> Unit
) {
    val sanaColors = LocalSanaColors.current
    var nameInput by remember(currentPreferredName) {
        mutableStateOf(currentPreferredName.orEmpty())
    }
    val trimmedPreview = nameInput.trim().take(UserPreferencesRepository.MAX_PREFERRED_NAME_LENGTH)
    val previewGreeting = if (trimmedPreview.isNotEmpty()) {
        stringResource(R.string.greeting_personalized_format, trimmedPreview)
    } else {
        stringResource(R.string.greeting_default)
    }

    AlertDialog(
        onDismissRequest = onDismiss,
        modifier = Modifier.testTag("settings_menu_dialog"),
        icon = {
            Icon(
                imageVector = Icons.Filled.Settings,
                contentDescription = null,
                tint = sanaColors.tealDeep
            )
        },
        title = {
            Text(
                text = stringResource(R.string.settings_title),
                style = MaterialTheme.typography.titleLarge
            )
        },
        text = {
            Column(
                modifier = Modifier.fillMaxWidth(),
                verticalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                // Live greeting preview card
                Surface(
                    shape = RoundedCornerShape(14.dp),
                    color = sanaColors.tealTint,
                    border = BorderStroke(1.dp, sanaColors.tealAccent.copy(alpha = 0.35f)),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Row(
                        modifier = Modifier.padding(horizontal = 12.dp, vertical = 10.dp),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        Icon(
                            imageVector = Icons.Filled.AutoAwesome,
                            contentDescription = null,
                            tint = sanaColors.tealDeep,
                            modifier = Modifier.size(18.dp)
                        )
                        Text(
                            text = previewGreeting,
                            style = MaterialTheme.typography.titleSmall.copy(fontWeight = FontWeight.Bold),
                            color = sanaColors.tealDeep,
                            modifier = Modifier.testTag("settings_greeting_preview")
                        )
                    }
                }

                if (!accountDisplayName.isNullOrBlank()) {
                    Text(
                        text = accountDisplayName,
                        style = MaterialTheme.typography.labelMedium,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }

                OutlinedTextField(
                    value = nameInput,
                    onValueChange = { updated ->
                        if (updated.length <= UserPreferencesRepository.MAX_PREFERRED_NAME_LENGTH) {
                            nameInput = updated
                        }
                    },
                    label = { Text(stringResource(R.string.settings_preferred_name_label)) },
                    placeholder = { Text(stringResource(R.string.settings_preferred_name_placeholder)) },
                    supportingText = {
                        Text(
                            text = stringResource(R.string.settings_preferred_name_helper),
                            style = MaterialTheme.typography.labelSmall
                        )
                    },
                    leadingIcon = {
                        Icon(
                            imageVector = Icons.Filled.Person,
                            contentDescription = null
                        )
                    },
                    trailingIcon = {
                        if (nameInput.isNotEmpty()) {
                            IconButton(
                                onClick = { nameInput = "" },
                                modifier = Modifier
                                    .minimumInteractiveComponentSize()
                                    .testTag("clear_name_input_button")
                            ) {
                                Icon(
                                    imageVector = Icons.Filled.Clear,
                                    contentDescription = stringResource(R.string.settings_clear_input_cd)
                                )
                            }
                        }
                    },
                    singleLine = true,
                    keyboardOptions = KeyboardOptions(imeAction = ImeAction.Done),
                    keyboardActions = KeyboardActions(
                        onDone = {
                            onSavePreferredName(nameInput)
                            onDismiss()
                        }
                    ),
                    shape = RoundedCornerShape(14.dp),
                    modifier = Modifier
                        .fillMaxWidth()
                        .testTag("preferred_name_input")
                )

                if (!currentPreferredName.isNullOrBlank()) {
                    TextButton(
                        onClick = {
                            onClearPreferredName()
                            onDismiss()
                        },
                        modifier = Modifier
                            .align(Alignment.End)
                            .testTag("clear_preferred_name_button")
                    ) {
                        Text(
                            text = stringResource(R.string.settings_reset_default),
                            color = sanaColors.errorText
                        )
                    }
                }
            }
        },
        confirmButton = {
            Button(
                onClick = {
                    onSavePreferredName(nameInput)
                    onDismiss()
                },
                modifier = Modifier.testTag("save_preferred_name_button")
            ) {
                Text(stringResource(R.string.settings_save))
            }
        },
        dismissButton = {
            TextButton(
                onClick = onDismiss,
                modifier = Modifier.testTag("cancel_settings_button")
            ) {
                Text(stringResource(R.string.cancel))
            }
        }
    )
}
