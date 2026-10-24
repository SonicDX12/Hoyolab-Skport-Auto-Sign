package com.autosign.app.ui.screens

import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.viewModelScope
import androidx.lifecycle.viewmodel.initializer
import androidx.lifecycle.viewmodel.viewModelFactory
import com.autosign.app.data.model.Account
import com.autosign.app.data.model.GameType
import com.autosign.app.data.model.TokenExtraction
import kotlinx.coroutines.Job
import kotlinx.coroutines.channels.Channel
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.receiveAsFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch

enum class AccountProvider { HOYOLAB, SKPORT }

data class AddEditAccountUiState(
    // 1. Editable input
    val provider: AccountProvider = AccountProvider.HOYOLAB,
    val nickname: String = "",
    val hoyoToken: String = "",
    val genshin: Boolean = false,
    val honkaiStarRail: Boolean = false,
    val honkai3: Boolean = false,
    val tearsOfThemis: Boolean = false,
    val zenlessZoneZero: Boolean = false,
    val skAccountToken: String = "",
    val skLanguage: String = "en",
    val discordAvatarUrl: String = "",
    // 3. Persisted snapshot (null when adding)
    val existing: Account? = null,
    // 4. Transient UI-only
    val isWebViewLoginOpen: Boolean = false,
    val isCapturingToken: Boolean = false,
    val hoyoTokenVisible: Boolean = false,
    val skAccountTokenVisible: Boolean = false
) {
    // 2. Derived — getters only, so validity can never drift from the fields above
    val isEdit: Boolean get() = existing != null
    val isValid: Boolean get() = when (provider) {
        AccountProvider.HOYOLAB -> nickname.isNotBlank() && hoyoToken.isNotBlank() &&
            (genshin || honkaiStarRail || honkai3 || tearsOfThemis || zenlessZoneZero)
        AccountProvider.SKPORT -> nickname.isNotBlank() && skAccountToken.isNotBlank()
    }
}

sealed interface AddEditAccountEffect {
    data object TokenCaptured : AddEditAccountEffect
    data class TokenCaptureFailed(val message: String) : AddEditAccountEffect
}

class AddEditAccountViewModel(existing: Account?) : ViewModel() {

    private val _uiState = MutableStateFlow(
        when (existing) {
            is Account.HoYoLab -> AddEditAccountUiState(
                provider = AccountProvider.HOYOLAB, existing = existing, nickname = existing.nickname,
                hoyoToken = existing.token, genshin = existing.genshin, honkaiStarRail = existing.honkaiStarRail,
                honkai3 = existing.honkai3, tearsOfThemis = existing.tearsOfThemis, zenlessZoneZero = existing.zenlessZoneZero,
                discordAvatarUrl = existing.discordAvatarUrl
            )
            is Account.SkPort -> AddEditAccountUiState(
                provider = AccountProvider.SKPORT, existing = existing, nickname = existing.nickname,
                skAccountToken = existing.accountToken, skLanguage = existing.language,
                discordAvatarUrl = existing.discordAvatarUrl
            )
            null -> AddEditAccountUiState()
        }
    )
    val uiState: StateFlow<AddEditAccountUiState> = _uiState.asStateFlow()

    private val _effects = Channel<AddEditAccountEffect>(Channel.BUFFERED)
    val effects = _effects.receiveAsFlow()

    private var loginTimeoutJob: Job? = null

    fun onProviderChange(p: AccountProvider) = _uiState.update { it.copy(provider = p) }
    fun onNicknameChange(v: String) = _uiState.update { it.copy(nickname = v) }
    fun onHoyoTokenChange(v: String) = _uiState.update { it.copy(hoyoToken = v) }
    fun onGameToggle(game: GameType, checked: Boolean) = _uiState.update { s ->
        when (game) {
            GameType.GENSHIN -> s.copy(genshin = checked)
            GameType.STAR_RAIL -> s.copy(honkaiStarRail = checked)
            GameType.HONKAI_3 -> s.copy(honkai3 = checked)
            GameType.TEARS_OF_THEMIS -> s.copy(tearsOfThemis = checked)
            GameType.ZENLESS_ZONE_ZERO -> s.copy(zenlessZoneZero = checked)
            GameType.ENDFIELD -> s
        }
    }
    fun onSkAccountTokenChange(v: String) = _uiState.update { it.copy(skAccountToken = v) }
    fun onDiscordAvatarUrlChange(v: String) = _uiState.update { it.copy(discordAvatarUrl = v) }
    fun onSkLanguageChange(v: String) = _uiState.update { it.copy(skLanguage = v) }
    fun onHoyoTokenVisibilityToggle() = _uiState.update { it.copy(hoyoTokenVisible = !it.hoyoTokenVisible) }
    fun onSkAccountTokenVisibilityToggle() = _uiState.update { it.copy(skAccountTokenVisible = !it.skAccountTokenVisible) }

    fun onWebViewLoginOpen() {
        _uiState.update { it.copy(isWebViewLoginOpen = true, isCapturingToken = true) }
        loginTimeoutJob = viewModelScope.launch {
            delay(5 * 60 * 1000L)
            _uiState.update { it.copy(isWebViewLoginOpen = false, isCapturingToken = false) }
            _effects.send(AddEditAccountEffect.TokenCaptureFailed("Login timed out — try again"))
        }
    }

    fun onWebViewLoginDismiss() {
        loginTimeoutJob?.cancel()
        _uiState.update { it.copy(isWebViewLoginOpen = false, isCapturingToken = false) }
    }

    fun onCookiesObserved(rawCookieHeader: String) {
        val state = _uiState.value
        if (!state.isCapturingToken) return
        val extracted = when (state.provider) {
            AccountProvider.HOYOLAB -> TokenExtraction.extractHoYoLabToken(rawCookieHeader)
            AccountProvider.SKPORT -> TokenExtraction.extractSkPortToken(rawCookieHeader)
        } ?: return // not logged in yet — normal while waiting, keep the WebView open
        loginTimeoutJob?.cancel()
        _uiState.update { s ->
            when (s.provider) {
                AccountProvider.HOYOLAB -> s.copy(hoyoToken = extracted, isWebViewLoginOpen = false, isCapturingToken = false)
                AccountProvider.SKPORT -> s.copy(skAccountToken = extracted, isWebViewLoginOpen = false, isCapturingToken = false)
            }
        }
        viewModelScope.launch { _effects.send(AddEditAccountEffect.TokenCaptured) }
    }

    fun buildAccountToSave(generateId: () -> String): Account {
        val s = _uiState.value
        return when (s.provider) {
            AccountProvider.HOYOLAB -> Account.HoYoLab(
                id = s.existing?.id ?: generateId(), nickname = s.nickname.trim(), token = s.hoyoToken.trim(),
                genshin = s.genshin, honkaiStarRail = s.honkaiStarRail, honkai3 = s.honkai3,
                tearsOfThemis = s.tearsOfThemis, zenlessZoneZero = s.zenlessZoneZero,
                lastSignResult = s.existing?.lastSignResult ?: "", lastSignTime = s.existing?.lastSignTime ?: 0L,
                discordAvatarUrl = s.discordAvatarUrl.trim()
            )
            AccountProvider.SKPORT -> Account.SkPort(
                id = s.existing?.id ?: generateId(), nickname = s.nickname.trim(), accountToken = s.skAccountToken.trim(),
                language = s.skLanguage,
                lastSignResult = s.existing?.lastSignResult ?: "", lastSignTime = s.existing?.lastSignTime ?: 0L,
                discordAvatarUrl = s.discordAvatarUrl.trim()
            )
        }
    }

    companion object {
        fun factory(existing: Account?): ViewModelProvider.Factory = viewModelFactory {
            initializer { AddEditAccountViewModel(existing) }
        }
    }
}
