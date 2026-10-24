package com.autosign.app.ui.screens

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.GridItemSpan
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.foundation.lazy.grid.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.PlayArrow
import androidx.compose.material.icons.outlined.Inbox
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import com.autosign.app.data.model.Account
import com.autosign.app.ui.components.*
import com.autosign.app.ui.theme.LocalReducedMotion

@Stable
interface HomeActions {
    fun onAddAccount()
    fun onEditAccount(account: Account)
    fun onDeleteAccount(accountId: String)
    fun onRunNow()
    fun onRunAccount(accountId: String)
}

@Composable
fun HomeScreen(
    accounts: List<Account>,
    isRunning: Boolean,
    runningAccountId: String?,
    actions: HomeActions
) {
    var deleteTarget by remember { mutableStateOf<Account?>(null) }
    val anyRunning = isRunning || runningAccountId != null
    val wide = isWideScreen()
    val reducedMotion = LocalReducedMotion.current

    LazyVerticalGrid(
        columns = GridCells.Fixed(if (wide) 2 else 1),
        modifier = Modifier.centeredContent(),
        contentPadding = PaddingValues(start = 16.dp, end = 16.dp, bottom = if (wide) 16.dp else BottomBarClearance),
        verticalArrangement = Arrangement.spacedBy(12.dp),
        horizontalArrangement = Arrangement.spacedBy(12.dp)
    ) {
        item(span = { GridItemSpan(maxLineSpan) }) {
            ScreenHeader(
                title = "AutoSign",
                subtitle = when (accounts.size) {
                    0 -> "Daily check-in for HoYoLAB & SKPORT"
                    1 -> "1 account"
                    else -> "${accounts.size} accounts"
                }
            ) {
                if (accounts.isNotEmpty()) {
                    HeaderIconButton(
                        icon = {
                            if (isRunning) CircularProgressIndicator(Modifier.size(20.dp), strokeWidth = 2.dp)
                            else Icon(Icons.Default.PlayArrow, contentDescription = "Sign in all accounts")
                        },
                        onClick = actions::onRunNow,
                        enabled = !anyRunning
                    )
                }
                HeaderIconButton(
                    icon = { Icon(Icons.Default.Add, contentDescription = "Add account") },
                    onClick = actions::onAddAccount,
                    highlighted = true
                )
            }
        }

        if (accounts.isEmpty()) {
            item(span = { GridItemSpan(maxLineSpan) }) {
                SectionCard(Modifier.padding(top = 32.dp), onClick = actions::onAddAccount) {
                    Column(
                        Modifier.fillMaxWidth().padding(32.dp),
                        horizontalAlignment = Alignment.CenterHorizontally,
                        verticalArrangement = Arrangement.spacedBy(12.dp)
                    ) {
                        Icon(Icons.Outlined.Inbox, contentDescription = null, modifier = Modifier.size(64.dp), tint = MaterialTheme.colorScheme.primary)
                        Text("No accounts yet", style = MaterialTheme.typography.titleMedium)
                        Text("Tap + to add a HoYoLAB or SKPORT account", style = MaterialTheme.typography.bodyMedium, color = MaterialTheme.colorScheme.onSurfaceVariant, textAlign = TextAlign.Center)
                    }
                }
            }
        }

        items(accounts, key = { it.id }) { account ->
            AccountCard(
                account = account,
                isRunning = runningAccountId == account.id,
                runDisabled = anyRunning && runningAccountId != account.id,
                onRun = { actions.onRunAccount(account.id) },
                onEdit = { actions.onEditAccount(account) },
                onDelete = { deleteTarget = account },
                modifier = if (reducedMotion) Modifier else Modifier.animateItem()
            )
        }
    }

    deleteTarget?.let { target ->
        AlertDialog(
            onDismissRequest = { deleteTarget = null },
            title = { Text("Remove account?") },
            text = { Text("\"${target.nickname}\" will be permanently removed.") },
            confirmButton = {
                TextButton(onClick = { actions.onDeleteAccount(target.id); deleteTarget = null }) {
                    Text("Remove", color = MaterialTheme.colorScheme.error)
                }
            },
            dismissButton = {
                TextButton(onClick = { deleteTarget = null }) { Text("Cancel") }
            }
        )
    }
}
