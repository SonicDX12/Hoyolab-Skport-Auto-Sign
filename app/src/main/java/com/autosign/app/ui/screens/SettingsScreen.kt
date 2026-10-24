package com.autosign.app.ui.screens

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.AccessTime
import androidx.compose.material.icons.filled.EventRepeat
import androidx.compose.material.icons.filled.Forum
import androidx.compose.material.icons.filled.Notifications
import androidx.compose.material.icons.filled.Schedule
import androidx.compose.material.icons.filled.Visibility
import androidx.compose.material.icons.filled.VisibilityOff
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.input.PasswordVisualTransformation
import androidx.compose.ui.text.input.VisualTransformation
import androidx.compose.ui.unit.dp
import com.autosign.app.data.model.AppSettings
import com.autosign.app.ui.components.*

/** Every change is saved right away, so nothing is lost if the app is closed. */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun SettingsScreen(
    settings: AppSettings,
    onChange: (AppSettings) -> Unit
) {
    var webhookVisible by remember { mutableStateOf(false) }
    var timePickerOpen by remember { mutableStateOf(false) }
    val time = "%02d:%02d".format(settings.scheduleHour, settings.scheduleMinute)

    Column(
        Modifier.centeredContent().verticalScroll(rememberScrollState()).padding(horizontal = 16.dp),
        verticalArrangement = Arrangement.spacedBy(8.dp)
    ) {
        ScreenHeader("Settings")

        SectionTitle("Automatic schedule", Icons.Filled.Schedule)
        SettingsSwitchItem(
            title = "Daily check-in",
            subtitle = "Signs in every account automatically each day",
            icon = Icons.Filled.EventRepeat,
            checked = settings.scheduleEnabled,
            onCheckedChange = { onChange(settings.copy(scheduleEnabled = it)) }
        )
        SettingsItem(
            title = "Check-in time",
            subtitle = time,
            icon = Icons.Filled.AccessTime,
            enabled = settings.scheduleEnabled,
            onClick = { timePickerOpen = true }
        )

        SectionTitle("Notifications", Icons.Filled.Notifications)
        SettingsSwitchItem(
            title = "Android notifications",
            subtitle = "Push notification after each sign-in",
            icon = Icons.Filled.Notifications,
            checked = settings.androidNotifications,
            onCheckedChange = { onChange(settings.copy(androidNotifications = it)) }
        )

        SectionTitle("Discord (optional)", Icons.Filled.Forum)
        SectionCard {
            Column(Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(12.dp)) {
                OutlinedTextField(
                    value = settings.discordWebhook,
                    onValueChange = { onChange(settings.copy(discordWebhook = it.trim())) },
                    label = { Text("Webhook URL") },
                    placeholder = { Text("https://discord.com/api/webhooks/…") },
                    visualTransformation = if (webhookVisible) VisualTransformation.None else PasswordVisualTransformation(),
                    trailingIcon = {
                        IconButton(onClick = { webhookVisible = !webhookVisible }) {
                            Icon(
                                if (webhookVisible) Icons.Default.VisibilityOff else Icons.Default.Visibility,
                                contentDescription = if (webhookVisible) "Hide webhook" else "Show webhook"
                            )
                        }
                    },
                    singleLine = true,
                    shape = InnerShape,
                    modifier = Modifier.fillMaxWidth()
                )
                OutlinedTextField(
                    value = settings.discordUserId,
                    onValueChange = { onChange(settings.copy(discordUserId = it.trim())) },
                    label = { Text("User ID") },
                    placeholder = { Text("123456789012345678") },
                    supportingText = { Text("Mentions you when a sign-in fails") },
                    singleLine = true,
                    shape = InnerShape,
                    modifier = Modifier.fillMaxWidth()
                )
            }
        }

        Spacer(Modifier.height(if (isWideScreen()) 16.dp else BottomBarClearance))
    }

    if (timePickerOpen) {
        val state = rememberTimePickerState(settings.scheduleHour, settings.scheduleMinute, is24Hour = true)
        AlertDialog(
            onDismissRequest = { timePickerOpen = false },
            title = { Text("Check-in time") },
            text = { TimePicker(state) },
            confirmButton = {
                TextButton(onClick = {
                    onChange(settings.copy(scheduleHour = state.hour, scheduleMinute = state.minute))
                    timePickerOpen = false
                }) { Text("OK") }
            },
            dismissButton = { TextButton(onClick = { timePickerOpen = false }) { Text("Cancel") } }
        )
    }
}
