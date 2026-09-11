package com.autosign.app.ui.components

import androidx.compose.foundation.layout.*
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.Edit
import androidx.compose.material.icons.filled.PlayArrow
import androidx.compose.material.icons.outlined.CheckCircle
import androidx.compose.material.icons.outlined.ErrorOutline
import androidx.compose.material.icons.outlined.Schedule
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import com.autosign.app.data.model.Account
import java.text.SimpleDateFormat
import java.util.*

@Composable
fun AccountCard(
    account: Account,
    isRunning: Boolean,
    runDisabled: Boolean,
    onRun: () -> Unit,
    onEdit: () -> Unit,
    onDelete: () -> Unit,
    modifier: Modifier = Modifier
) {
    val subtitle = when (account) {
        is Account.HoYoLab -> buildList {
            if (account.genshin) add("Genshin")
            if (account.honkaiStarRail) add("Star Rail")
            if (account.honkai3) add("HI3")
            if (account.tearsOfThemis) add("ToT")
            if (account.zenlessZoneZero) add("ZZZ")
        }.joinToString(" · ").ifEmpty { "No games selected" }
        is Account.SkPort -> "Arknights: Endfield"
    }

    val badgeColor = when (account) {
        is Account.HoYoLab -> MaterialTheme.colorScheme.primaryContainer
        is Account.SkPort -> MaterialTheme.colorScheme.secondaryContainer
    }
    val badgeText = when (account) {
        is Account.HoYoLab -> "HoYoLAB"
        is Account.SkPort -> "SKPORT"
    }

    ElevatedCard(modifier = modifier.fillMaxWidth(), shape = MaterialTheme.shapes.large) {
        Column(modifier = Modifier.padding(16.dp)) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                // Merged so TalkBack reads nickname+provider+subtitle as one unit; Edit/Delete stay
                // outside the merge so they remain individually reachable/announced.
                Column(modifier = Modifier.weight(1f).semantics(mergeDescendants = true) {}) {
                    Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                        Text(account.nickname, style = MaterialTheme.typography.titleMedium, maxLines = 1, overflow = TextOverflow.Ellipsis)
                        Surface(color = badgeColor, shape = MaterialTheme.shapes.small) {
                            Text(badgeText, style = MaterialTheme.typography.labelMedium, modifier = Modifier.padding(horizontal = 6.dp, vertical = 2.dp))
                        }
                    }
                    Spacer(Modifier.height(2.dp))
                    Text(subtitle, style = MaterialTheme.typography.bodyMedium, color = MaterialTheme.colorScheme.onSurfaceVariant)
                }
                IconButton(onClick = onRun, enabled = !runDisabled) {
                    if (isRunning) {
                        CircularProgressIndicator(modifier = Modifier.size(20.dp), strokeWidth = 2.dp)
                    } else {
                        Icon(Icons.Default.PlayArrow, contentDescription = "Run this account")
                    }
                }
                IconButton(onClick = onEdit) { Icon(Icons.Default.Edit, contentDescription = "Edit") }
                IconButton(onClick = onDelete) { Icon(Icons.Default.Delete, contentDescription = "Delete", tint = MaterialTheme.colorScheme.error) }
            }

            if (account.lastSignResult.isNotBlank()) {
                Spacer(Modifier.height(8.dp))
                HorizontalDivider()
                Spacer(Modifier.height(8.dp))

                val isSuccess = account.lastSignResult.contains("successful", ignoreCase = true) ||
                    account.lastSignResult.contains("Traveler", ignoreCase = true)
                val statusIcon = if (isSuccess) Icons.Outlined.CheckCircle else Icons.Outlined.ErrorOutline
                val statusColor = if (isSuccess) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.error

                Row(verticalAlignment = Alignment.Top, horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                    Icon(statusIcon, contentDescription = null, tint = statusColor, modifier = Modifier.size(16.dp).padding(top = 2.dp))
                    Column {
                        Text(account.lastSignResult, style = MaterialTheme.typography.bodyMedium, maxLines = 6, overflow = TextOverflow.Ellipsis)
                        if (account.lastSignTime > 0L) {
                            val fmt = SimpleDateFormat("MMM d, HH:mm", Locale.getDefault())
                            Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(3.dp)) {
                                Icon(Icons.Outlined.Schedule, contentDescription = null, tint = MaterialTheme.colorScheme.onSurfaceVariant, modifier = Modifier.size(12.dp))
                                Text(fmt.format(Date(account.lastSignTime)), style = MaterialTheme.typography.labelMedium, color = MaterialTheme.colorScheme.onSurfaceVariant)
                            }
                        }
                    }
                }
            }
        }
    }
}
