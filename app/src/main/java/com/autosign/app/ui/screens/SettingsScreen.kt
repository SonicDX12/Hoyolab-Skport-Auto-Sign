package com.autosign.app.ui.screens

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.Visibility
import androidx.compose.material.icons.filled.VisibilityOff
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.semantics.heading
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.input.PasswordVisualTransformation
import androidx.compose.ui.text.input.VisualTransformation
import androidx.compose.ui.unit.dp
import com.autosign.app.data.model.AppSettings

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun SettingsScreen(
    settings: AppSettings,
    onSave: (AppSettings) -> Unit,
    onBack: () -> Unit
) {
    var scheduleEnabled by remember { mutableStateOf(settings.scheduleEnabled) }
    var scheduleHour by remember { mutableStateOf(settings.scheduleHour.toString()) }
    var scheduleMinute by remember { mutableStateOf(settings.scheduleMinute.toString().padStart(2, '0')) }
    var androidNotifications by remember { mutableStateOf(settings.androidNotifications) }
    var discordWebhook by remember { mutableStateOf(settings.discordWebhook) }
    var discordUserId by remember { mutableStateOf(settings.discordUserId) }
    var webhookVisible by remember { mutableStateOf(false) }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("Settings") },
                navigationIcon = {
                    IconButton(onClick = onBack) {
                        Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "Back")
                    }
                },
                colors = TopAppBarDefaults.topAppBarColors(containerColor = MaterialTheme.colorScheme.background)
            )
        }
    ) { padding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(padding)
                .verticalScroll(rememberScrollState())
                .padding(horizontal = 20.dp, vertical = 8.dp),
            verticalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            // Schedule section
            SectionHeader("Automatic schedule")

            Row(
                modifier = Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                Column {
                    Text("Enable daily check-in", style = MaterialTheme.typography.bodyLarge)
                    Text("Runs automatically every day", style = MaterialTheme.typography.bodyMedium, color = MaterialTheme.colorScheme.onSurfaceVariant)
                }
                Switch(checked = scheduleEnabled, onCheckedChange = { scheduleEnabled = it })
            }

            AnimatedVisibility(visible = scheduleEnabled) {
                Row(
                    horizontalArrangement = Arrangement.spacedBy(12.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    OutlinedTextField(
                        value = scheduleHour,
                        onValueChange = { if (it.length <= 2) scheduleHour = it },
                        label = { Text("Hour (0–23)") },
                        singleLine = true,
                        modifier = Modifier.weight(1f)
                    )
                    Text(":", style = MaterialTheme.typography.titleLarge)
                    OutlinedTextField(
                        value = scheduleMinute,
                        onValueChange = { if (it.length <= 2) scheduleMinute = it },
                        label = { Text("Minute") },
                        singleLine = true,
                        modifier = Modifier.weight(1f)
                    )
                }
            }

            HorizontalDivider()

            // Notifications
            SectionHeader("Notifications")

            Row(
                modifier = Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                Column {
                    Text("Android notifications", style = MaterialTheme.typography.bodyLarge)
                    Text("Push notification after each sign-in", style = MaterialTheme.typography.bodyMedium, color = MaterialTheme.colorScheme.onSurfaceVariant)
                }
                Switch(checked = androidNotifications, onCheckedChange = { androidNotifications = it })
            }

            HorizontalDivider()

            Text("Optional extras", style = MaterialTheme.typography.labelLarge, color = MaterialTheme.colorScheme.onSurfaceVariant)

            OutlinedTextField(
                value = discordWebhook,
                onValueChange = { discordWebhook = it },
                label = { Text("Discord Webhook URL") },
                placeholder = { Text("https://discord.com/api/webhooks/…") },
                visualTransformation = if (webhookVisible) VisualTransformation.None else PasswordVisualTransformation(),
                trailingIcon = {
                    IconButton(onClick = { webhookVisible = !webhookVisible }) {
                        Icon(if (webhookVisible) Icons.Default.VisibilityOff else Icons.Default.Visibility, null)
                    }
                },
                singleLine = true,
                modifier = Modifier.fillMaxWidth()
            )

            OutlinedTextField(
                value = discordUserId,
                onValueChange = { discordUserId = it },
                label = { Text("Discord User ID") },
                placeholder = { Text("123456789012345678") },
                supportingText = { Text("Optional — mention you on errors") },
                singleLine = true,
                modifier = Modifier.fillMaxWidth()
            )

            Spacer(Modifier.height(8.dp))

            Button(
                onClick = {
                    onSave(AppSettings(
                        scheduleEnabled = scheduleEnabled,
                        scheduleHour = scheduleHour.toIntOrNull()?.coerceIn(0, 23) ?: 10,
                        scheduleMinute = scheduleMinute.toIntOrNull()?.coerceIn(0, 59) ?: 0,
                        androidNotifications = androidNotifications,
                        discordWebhook = discordWebhook.trim(),
                        discordUserId = discordUserId.trim()
                    ))
                },
                modifier = Modifier.fillMaxWidth().height(52.dp)
            ) {
                Text("Save settings")
            }

            Spacer(Modifier.height(16.dp))
        }
    }
}

@Composable
private fun SectionHeader(title: String) {
    Text(
        title,
        modifier = Modifier.semantics { heading() },
        style = MaterialTheme.typography.labelLarge,
        color = MaterialTheme.colorScheme.primary
    )
}
