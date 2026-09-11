package com.autosign.app.ui.screens

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
import androidx.compose.ui.text.input.PasswordVisualTransformation
import androidx.compose.ui.text.input.VisualTransformation
import androidx.compose.ui.unit.dp
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.compose.LocalLifecycleOwner
import androidx.lifecycle.repeatOnLifecycle
import com.autosign.app.data.model.Account
import com.autosign.app.data.model.GameType
import com.autosign.app.ui.components.TokenLoginWebView

// ACCOUNT_TOKEN is scoped to Path=/cookie_store/account_token (not "/"), so CookieManager only
// returns it for a URL under that path — never for the sign-in page itself.
private const val SKPORT_ACCOUNT_TOKEN_URL = "https://game.skport.com/cookie_store/account_token"

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun AddEditAccountScreen(
    viewModel: AddEditAccountViewModel,
    onSave: (Account) -> Unit,
    onBack: () -> Unit,
    generateId: () -> String
) {
    val uiState by viewModel.uiState.collectAsState()
    val snackbarHostState = remember { SnackbarHostState() }
    val lifecycleOwner = LocalLifecycleOwner.current

    LaunchedEffect(Unit) {
        lifecycleOwner.lifecycle.repeatOnLifecycle(Lifecycle.State.STARTED) {
            viewModel.effects.collect { effect ->
                when (effect) {
                    AddEditAccountEffect.TokenCaptured -> snackbarHostState.showSnackbar("Logged in — token captured")
                    is AddEditAccountEffect.TokenCaptureFailed -> snackbarHostState.showSnackbar(effect.message)
                }
            }
        }
    }

    val languages = listOf("en" to "English", "ja" to "日本語", "zh_Hant" to "繁體中文", "zh_Hans" to "简体中文", "ko" to "한국어", "ru_RU" to "Русский")

    Scaffold(
        snackbarHost = { SnackbarHost(snackbarHostState) },
        topBar = {
            TopAppBar(
                title = { Text(if (uiState.isEdit) "Edit account" else "Add account") },
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
            if (!uiState.isEdit) {
                Text("Account type", style = MaterialTheme.typography.labelLarge, color = MaterialTheme.colorScheme.onSurfaceVariant)
                SingleChoiceSegmentedButtonRow(modifier = Modifier.fillMaxWidth()) {
                    SegmentedButton(
                        selected = uiState.provider == AccountProvider.HOYOLAB,
                        onClick = { viewModel.onProviderChange(AccountProvider.HOYOLAB) },
                        shape = SegmentedButtonDefaults.itemShape(0, 2)
                    ) { Text("HoYoLAB") }
                    SegmentedButton(
                        selected = uiState.provider == AccountProvider.SKPORT,
                        onClick = { viewModel.onProviderChange(AccountProvider.SKPORT) },
                        shape = SegmentedButtonDefaults.itemShape(1, 2)
                    ) { Text("SKPORT") }
                }
            }

            HorizontalDivider()

            if (uiState.provider == AccountProvider.HOYOLAB) {
                OutlinedTextField(
                    value = uiState.nickname,
                    onValueChange = viewModel::onNicknameChange,
                    label = { Text("Nickname") },
                    singleLine = true,
                    modifier = Modifier.fillMaxWidth()
                )

                OutlinedButton(onClick = viewModel::onWebViewLoginOpen, modifier = Modifier.fillMaxWidth()) {
                    Text("Log in with browser")
                }

                OutlinedTextField(
                    value = uiState.hoyoToken,
                    onValueChange = viewModel::onHoyoTokenChange,
                    label = { Text("Cookie token") },
                    placeholder = { Text("ltoken_v2=…; ltuid_v2=…;") },
                    supportingText = { Text("From HoYoLAB browser cookies, or use browser login above") },
                    visualTransformation = if (uiState.hoyoTokenVisible) VisualTransformation.None else PasswordVisualTransformation(),
                    trailingIcon = {
                        IconButton(onClick = viewModel::onHoyoTokenVisibilityToggle) {
                            Icon(if (uiState.hoyoTokenVisible) Icons.Default.VisibilityOff else Icons.Default.Visibility, null)
                        }
                    },
                    modifier = Modifier.fillMaxWidth(),
                    minLines = 2
                )

                Text("Games", style = MaterialTheme.typography.labelLarge, color = MaterialTheme.colorScheme.onSurfaceVariant)
                listOf(
                    GameType.GENSHIN to uiState.genshin,
                    GameType.STAR_RAIL to uiState.honkaiStarRail,
                    GameType.HONKAI_3 to uiState.honkai3,
                    GameType.TEARS_OF_THEMIS to uiState.tearsOfThemis,
                    GameType.ZENLESS_ZONE_ZERO to uiState.zenlessZoneZero
                ).forEach { (game, checked) ->
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.SpaceBetween
                    ) {
                        Text(game.displayName, style = MaterialTheme.typography.bodyLarge)
                        Switch(checked = checked, onCheckedChange = { viewModel.onGameToggle(game, it) })
                    }
                }
            } else {
                OutlinedTextField(
                    value = uiState.nickname,
                    onValueChange = viewModel::onNicknameChange,
                    label = { Text("Nickname") },
                    singleLine = true,
                    modifier = Modifier.fillMaxWidth()
                )

                OutlinedButton(onClick = viewModel::onWebViewLoginOpen, modifier = Modifier.fillMaxWidth()) {
                    Text("Log in with browser")
                }

                OutlinedTextField(
                    value = uiState.skAccountToken,
                    onValueChange = viewModel::onSkAccountTokenChange,
                    label = { Text("Account token") },
                    placeholder = { Text("ACCOUNT_TOKEN cookie value") },
                    supportingText = { Text("From game.skport.com — use browser login above, or paste manually") },
                    visualTransformation = if (uiState.skAccountTokenVisible) VisualTransformation.None else PasswordVisualTransformation(),
                    trailingIcon = {
                        IconButton(onClick = viewModel::onSkAccountTokenVisibilityToggle) {
                            Icon(if (uiState.skAccountTokenVisible) Icons.Default.VisibilityOff else Icons.Default.Visibility, null)
                        }
                    },
                    modifier = Modifier.fillMaxWidth(),
                    minLines = 2
                )

                ExposedDropdownMenuBox(expanded = uiState.languageMenuExpanded, onExpandedChange = viewModel::onLanguageMenuToggle) {
                    OutlinedTextField(
                        value = languages.firstOrNull { it.first == uiState.skLanguage }?.second ?: uiState.skLanguage,
                        onValueChange = {},
                        readOnly = true,
                        label = { Text("Language") },
                        trailingIcon = { ExposedDropdownMenuDefaults.TrailingIcon(expanded = uiState.languageMenuExpanded) },
                        modifier = Modifier.fillMaxWidth().menuAnchor()
                    )
                    ExposedDropdownMenu(expanded = uiState.languageMenuExpanded, onDismissRequest = { viewModel.onLanguageMenuToggle(false) }) {
                        languages.forEach { (code, name) ->
                            DropdownMenuItem(
                                text = { Text(name) },
                                onClick = { viewModel.onSkLanguageChange(code) }
                            )
                        }
                    }
                }
            }

            HorizontalDivider()

            Text("Discord notification", style = MaterialTheme.typography.labelLarge, color = MaterialTheme.colorScheme.onSurfaceVariant)
            OutlinedTextField(
                value = uiState.discordAvatarUrl,
                onValueChange = viewModel::onDiscordAvatarUrlChange,
                label = { Text("Avatar URL") },
                placeholder = { Text("https://…") },
                supportingText = { Text("Optional — shown as this account's icon in Discord messages") },
                singleLine = true,
                modifier = Modifier.fillMaxWidth()
            )

            Spacer(Modifier.height(8.dp))

            Button(
                onClick = { onSave(viewModel.buildAccountToSave(generateId)) },
                enabled = uiState.isValid,
                shape = MaterialTheme.shapes.large,
                modifier = Modifier.fillMaxWidth().height(52.dp)
            ) {
                Text(if (uiState.isEdit) "Save changes" else "Add account")
            }

            Spacer(Modifier.height(16.dp))
        }
    }

    if (uiState.isWebViewLoginOpen) {
        TokenLoginWebView(
            loginUrl = if (uiState.provider == AccountProvider.HOYOLAB) "https://www.hoyolab.com/home" else "https://game.skport.com/endfield/sign-in",
            onCookiesUpdated = viewModel::onCookiesObserved,
            onDismiss = viewModel::onWebViewLoginDismiss,
            // The SkPort sign-in SPA appears not to mount its content in a plain mobile WebView;
            // a desktop Chrome UA is the officially-documented environment (their README walks
            // through extracting the cookie via desktop DevTools) and gets it to render.
            userAgentOverride = if (uiState.provider == AccountProvider.SKPORT)
                "Mozilla/5.0 (Windows NT 10.0; Win64; x64) AppleWebKit/537.36 (KHTML, like Gecko) Chrome/130.0.0.0 Safari/537.36"
            else null,
            cookieLookupUrl = if (uiState.provider == AccountProvider.SKPORT) SKPORT_ACCOUNT_TOKEN_URL else null
        )
    }
}
