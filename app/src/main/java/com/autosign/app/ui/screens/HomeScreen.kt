package com.autosign.app.ui.screens

import androidx.compose.animation.*
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.PlayArrow
import androidx.compose.material.icons.filled.Settings
import androidx.compose.material.icons.outlined.Inbox
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import com.autosign.app.data.model.Account
import com.autosign.app.ui.components.AccountCard
import com.autosign.app.ui.theme.LocalReducedMotion
import com.autosign.app.ui.theme.defaultEffectsSpec
import com.autosign.app.ui.theme.defaultSpatialSpec

@Stable
interface HomeActions {
    fun onAddAccount()
    fun onEditAccount(account: Account)
    fun onDeleteAccount(accountId: String)
    fun onRunNow()
    fun onRunAccount(accountId: String)
    fun onSettings()
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun HomeScreen(
    accounts: List<Account>,
    isRunning: Boolean,
    runningAccountId: String?,
    actions: HomeActions
) {
    var deleteTarget by remember { mutableStateOf<Account?>(null) }
    val anyRunning = isRunning || runningAccountId != null

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("AutoSign") },
                actions = {
                    IconButton(onClick = actions::onSettings) {
                        Icon(Icons.Default.Settings, contentDescription = "Settings")
                    }
                },
                colors = TopAppBarDefaults.topAppBarColors(
                    containerColor = MaterialTheme.colorScheme.background
                )
            )
        },
        floatingActionButton = {
            Column(horizontalAlignment = Alignment.End, verticalArrangement = Arrangement.spacedBy(12.dp)) {
                if (accounts.isNotEmpty()) {
                    SmallFloatingActionButton(
                        onClick = actions::onRunNow,
                        containerColor = MaterialTheme.colorScheme.secondaryContainer
                    ) {
                        if (isRunning) {
                            CircularProgressIndicator(modifier = Modifier.size(20.dp), strokeWidth = 2.dp)
                        } else {
                            Icon(Icons.Default.PlayArrow, contentDescription = "Run now")
                        }
                    }
                }
                FloatingActionButton(onClick = actions::onAddAccount, shape = MaterialTheme.shapes.extraLarge) {
                    Icon(Icons.Default.Add, contentDescription = "Add account")
                }
            }
        }
    ) { padding ->
        if (accounts.isEmpty()) {
            Box(modifier = Modifier.fillMaxSize().padding(padding), contentAlignment = Alignment.Center) {
                Column(horizontalAlignment = Alignment.CenterHorizontally, verticalArrangement = Arrangement.spacedBy(12.dp)) {
                    Icon(Icons.Outlined.Inbox, contentDescription = null, modifier = Modifier.size(64.dp), tint = MaterialTheme.colorScheme.onSurfaceVariant)
                    Text("No accounts yet", style = MaterialTheme.typography.titleMedium, color = MaterialTheme.colorScheme.onSurfaceVariant)
                    Text("Tap + to add a HoYoLAB or SKPORT account", style = MaterialTheme.typography.bodyMedium, color = MaterialTheme.colorScheme.onSurfaceVariant, textAlign = TextAlign.Center)
                }
            }
        } else {
            val reducedMotion = LocalReducedMotion.current
            LazyColumn(
                modifier = Modifier.fillMaxSize().padding(padding),
                contentPadding = PaddingValues(horizontal = 16.dp, vertical = 12.dp),
                verticalArrangement = Arrangement.spacedBy(10.dp)
            ) {
                items(accounts, key = { it.id }) { account ->
                    AnimatedVisibility(
                        visible = true,
                        enter = if (reducedMotion) EnterTransition.None
                        else fadeIn(defaultEffectsSpec()) + slideInVertically(defaultSpatialSpec())
                    ) {
                        AccountCard(
                            account = account,
                            isRunning = runningAccountId == account.id,
                            runDisabled = anyRunning && runningAccountId != account.id,
                            onRun = { actions.onRunAccount(account.id) },
                            onEdit = { actions.onEditAccount(account) },
                            onDelete = { deleteTarget = account }
                        )
                    }
                }
                item { Spacer(Modifier.height(80.dp)) }
            }
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
