package com.autosign.app.ui.screens

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.AccountCircle
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.filled.Forum
import androidx.compose.material.icons.filled.Key
import androidx.compose.material.icons.filled.Language
import androidx.compose.material.icons.filled.OpenInBrowser
import androidx.compose.material.icons.filled.SportsEsports
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
import com.autosign.app.ui.components.*

// ACCOUNT_TOKEN is scoped to Path=/cookie_store/account_token (not "/"), so CookieManager only
// returns it for a URL under that path — never for the sign-in page itself.
private const val SKPORT_ACCOUNT_TOKEN_URL = "https://game.skport.com/cookie_store/account_token"

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
    val isHoyo = uiState.provider == AccountProvider.HOYOLAB

    Box(Modifier.fillMaxSize()) {
        Column(
            Modifier.centeredContent().verticalScroll(rememberScrollState()).padding(horizontal = 16.dp),
            verticalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            ScreenHeader(
                title = if (uiState.isEdit) "Edit account" else "Add account",
                navigationIcon = {
                    IconButton(onClick = onBack) {
                        Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "Back")
                    }
                }
            )

            if (!uiState.isEdit) {
                SectionTitle("Account type", Icons.Filled.AccountCircle)
                DividedCard(listOf(
                    {
                        ChoiceRow("HoYoLAB", selected = isHoyo, subtitle = "Genshin, Star Rail, HI3, ToT, ZZZ",
                            onClick = { viewModel.onProviderChange(AccountProvider.HOYOLAB) })
                    },
                    {
                        ChoiceRow("SKPORT", selected = !isHoyo, subtitle = "Arknights: Endfield",
                            onClick = { viewModel.onProviderChange(AccountProvider.SKPORT) })
                    }
                ))
            }

            SectionTitle("Login", Icons.Filled.Key)
            SectionCard {
                Column(Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(12.dp)) {
                    OutlinedTextField(
                        value = uiState.nickname,
                        onValueChange = viewModel::onNicknameChange,
                        label = { Text("Nickname") },
                        singleLine = true,
                        shape = InnerShape,
                        modifier = Modifier.fillMaxWidth()
                    )
                    ActionButton(
                        "Log in with browser",
                        onClick = viewModel::onWebViewLoginOpen,
                        icon = Icons.Filled.OpenInBrowser,
                        modifier = Modifier.fillMaxWidth()
                    )
                    val tokenVisible = if (isHoyo) uiState.hoyoTokenVisible else uiState.skAccountTokenVisible
                    OutlinedTextField(
                        value = if (isHoyo) uiState.hoyoToken else uiState.skAccountToken,
                        onValueChange = if (isHoyo) viewModel::onHoyoTokenChange else viewModel::onSkAccountTokenChange,
                        label = { Text(if (isHoyo) "Cookie token" else "Account token") },
                        placeholder = { Text(if (isHoyo) "ltoken_v2=…; ltuid_v2=…;" else "ACCOUNT_TOKEN cookie value") },
                        supportingText = {
                            Text(if (isHoyo) "From HoYoLAB browser cookies, or use browser login above"
                                else "From game.skport.com — use browser login above, or paste manually")
                        },
                        visualTransformation = if (tokenVisible) VisualTransformation.None else PasswordVisualTransformation(),
                        trailingIcon = {
                            IconButton(onClick = if (isHoyo) viewModel::onHoyoTokenVisibilityToggle else viewModel::onSkAccountTokenVisibilityToggle) {
                                Icon(
                                    if (tokenVisible) Icons.Default.VisibilityOff else Icons.Default.Visibility,
                                    contentDescription = if (tokenVisible) "Hide token" else "Show token"
                                )
                            }
                        },
                        shape = InnerShape,
                        modifier = Modifier.fillMaxWidth(),
                        minLines = 2
                    )
                }
            }

            if (isHoyo) {
                SectionTitle("Games", Icons.Filled.SportsEsports)
                DividedCard(listOf(
                    GameType.GENSHIN to uiState.genshin,
                    GameType.STAR_RAIL to uiState.honkaiStarRail,
                    GameType.HONKAI_3 to uiState.honkai3,
                    GameType.TEARS_OF_THEMIS to uiState.tearsOfThemis,
                    GameType.ZENLESS_ZONE_ZERO to uiState.zenlessZoneZero
                ).map { (game, checked) ->
                    { ChoiceRow(game.displayName, selected = checked, onClick = { viewModel.onGameToggle(game, !checked) }) }
                })
            } else {
                SectionTitle("Language", Icons.Filled.Language)
                DividedCard(languages.map { (code, name) ->
                    { ChoiceRow(name, selected = uiState.skLanguage == code, onClick = { viewModel.onSkLanguageChange(code) }) }
                })
            }

            SectionTitle("Discord notification", Icons.Filled.Forum)
            SectionCard {
                OutlinedTextField(
                    value = uiState.discordAvatarUrl,
                    onValueChange = viewModel::onDiscordAvatarUrlChange,
                    label = { Text("Avatar URL") },
                    placeholder = { Text("https://…") },
                    supportingText = { Text("Optional — shown as this account's icon in Discord messages") },
                    singleLine = true,
                    shape = InnerShape,
                    modifier = Modifier.fillMaxWidth().padding(16.dp)
                )
            }

            Spacer(Modifier.height(8.dp))
            ActionButton(
                if (uiState.isEdit) "Save changes" else "Add account",
                onClick = { onSave(viewModel.buildAccountToSave(generateId)) },
                icon = Icons.Filled.Check,
                primary = true,
                enabled = uiState.isValid,
                modifier = Modifier.fillMaxWidth()
            )
            Spacer(Modifier.height(16.dp))
        }

        SnackbarHost(snackbarHostState, Modifier.align(Alignment.BottomCenter))
    }

    if (uiState.isWebViewLoginOpen) {
        TokenLoginWebView(
            loginUrl = if (isHoyo) "https://www.hoyolab.com/home" else "https://game.skport.com/endfield/sign-in",
            onCookiesUpdated = viewModel::onCookiesObserved,
            onDismiss = viewModel::onWebViewLoginDismiss,
            // The SkPort sign-in SPA appears not to mount its content in a plain mobile WebView;
            // a desktop Chrome UA is the officially-documented environment (their README walks
            // through extracting the cookie via desktop DevTools) and gets it to render.
            userAgentOverride = if (!isHoyo)
                "Mozilla/5.0 (Windows NT 10.0; Win64; x64) AppleWebKit/537.36 (KHTML, like Gecko) Chrome/130.0.0.0 Safari/537.36"
            else null,
            cookieLookupUrl = if (!isHoyo) SKPORT_ACCOUNT_TOKEN_URL else null
        )
    }
}
