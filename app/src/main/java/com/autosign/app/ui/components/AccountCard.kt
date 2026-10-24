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
import androidx.compose.ui.text.font.FontWeight
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

    val colors = MaterialTheme.colorScheme
    val (badgeText, badgeColor, badgeContent) = when (account) {
        is Account.HoYoLab -> Triple("HoYoLAB", colors.primaryContainer, colors.onPrimaryContainer)
        is Account.SkPort -> Triple("SKPORT", colors.tertiaryContainer, colors.onTertiaryContainer)
    }

    SectionCard(modifier = modifier) {
        Column(modifier = Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(12.dp)) {
            // Merged so TalkBack reads nickname+provider+subtitle as one unit
            Row(
                modifier = Modifier.semantics(mergeDescendants = true) {},
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                Column(Modifier.weight(1f)) {
                    Text(account.nickname, style = MaterialTheme.typography.titleLarge, fontWeight = FontWeight.SemiBold, maxLines = 1, overflow = TextOverflow.Ellipsis)
                    Text(subtitle, style = MaterialTheme.typography.bodyMedium, color = colors.onSurfaceVariant)
                }
                Pill(badgeText, containerColor = badgeColor, contentColor = badgeContent)
            }

            if (account.lastSignResult.isNotBlank()) {
                val isSuccess = account.lastSignResult.contains("successful", ignoreCase = true) ||
                    account.lastSignResult.contains("Traveler", ignoreCase = true)
                val statusColor = if (isSuccess) colors.primary else colors.error

                Surface(shape = InnerShape, color = colors.surfaceColorAtElevation(6.dp), modifier = Modifier.fillMaxWidth()) {
                    Row(Modifier.padding(12.dp), verticalAlignment = Alignment.Top, horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                        Icon(
                            if (isSuccess) Icons.Outlined.CheckCircle else Icons.Outlined.ErrorOutline,
                            contentDescription = null, tint = statusColor, modifier = Modifier.size(20.dp)
                        )
                        Column {
                            Text(account.lastSignResult, style = MaterialTheme.typography.bodyMedium, maxLines = 6, overflow = TextOverflow.Ellipsis)
                            if (account.lastSignTime > 0L) {
                                val fmt = SimpleDateFormat("MMM d, HH:mm", Locale.getDefault())
                                Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(4.dp)) {
                                    Icon(Icons.Outlined.Schedule, contentDescription = null, tint = colors.onSurfaceVariant, modifier = Modifier.size(12.dp))
                                    Text(fmt.format(Date(account.lastSignTime)), style = MaterialTheme.typography.labelMedium, color = colors.onSurfaceVariant)
                                }
                            }
                        }
                    }
                }
            }

            Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                OutlinedButton(
                    onClick = onRun,
                    enabled = !runDisabled && !isRunning,
                    modifier = Modifier.weight(1f).height(48.dp),
                    colors = ButtonDefaults.outlinedButtonColors(contentColor = colors.onSurface)
                ) {
                    if (isRunning) {
                        CircularProgressIndicator(modifier = Modifier.size(18.dp), strokeWidth = 2.dp)
                    } else {
                        Icon(Icons.Default.PlayArrow, contentDescription = null)
                    }
                    Spacer(Modifier.width(8.dp))
                    Text("Sign in now")
                }
                FilledTonalIconButton(onClick = onEdit, modifier = Modifier.size(48.dp)) {
                    Icon(Icons.Default.Edit, contentDescription = "Edit")
                }
                FilledTonalIconButton(
                    onClick = onDelete,
                    modifier = Modifier.size(48.dp),
                    colors = IconButtonDefaults.filledTonalIconButtonColors(containerColor = colors.errorContainer, contentColor = colors.onErrorContainer)
                ) {
                    Icon(Icons.Default.Delete, contentDescription = "Delete")
                }
            }
        }
    }
}
