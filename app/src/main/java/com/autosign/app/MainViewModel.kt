package com.autosign.app

import android.app.Application
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import androidx.work.*
import com.autosign.app.api.NotificationHelper
import com.autosign.app.api.SignInService
import com.autosign.app.data.model.Account
import com.autosign.app.data.model.AppSettings
import com.autosign.app.data.repository.AccountRepository
import com.autosign.app.worker.SignInWorker
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import java.util.Calendar
import java.util.concurrent.TimeUnit

class MainViewModel(app: Application) : AndroidViewModel(app) {

    private val repo: AccountRepository = (app as AutoSignApp).repository
    private val service = SignInService()
    private val workManager = WorkManager.getInstance(app)

    val accounts: StateFlow<List<Account>> = repo.accounts
    val settings: StateFlow<AppSettings> = repo.settings

    private val _isRunning = MutableStateFlow(false)
    val isRunning: StateFlow<Boolean> = _isRunning.asStateFlow()

    private val _runningAccountId = MutableStateFlow<String?>(null)
    val runningAccountId: StateFlow<String?> = _runningAccountId.asStateFlow()

    fun addAccount(account: Account) = repo.addAccount(account)
    fun updateAccount(account: Account) = repo.updateAccount(account)
    fun deleteAccount(id: String) = repo.deleteAccount(id)
    fun generateId() = repo.generateId()

    fun saveSettings(settings: AppSettings) {
        val old = this.settings.value
        repo.saveSettings(settings)
        // Settings save on every keystroke; only touch WorkManager when the schedule itself changed
        if (old.scheduleEnabled == settings.scheduleEnabled && old.scheduleHour == settings.scheduleHour &&
            old.scheduleMinute == settings.scheduleMinute) return
        if (settings.scheduleEnabled) scheduleWork(settings) else cancelWork()
    }

    fun runNow() {
        if (_isRunning.value || _runningAccountId.value != null) return
        viewModelScope.launch {
            _isRunning.value = true
            try {
                signInAndNotify(accounts.value)
            } finally {
                _isRunning.value = false
            }
        }
    }

    fun runAccount(accountId: String) {
        if (_isRunning.value || _runningAccountId.value != null) return
        val account = accounts.value.firstOrNull { it.id == accountId } ?: return
        viewModelScope.launch {
            _runningAccountId.value = accountId
            try {
                signInAndNotify(listOf(account))
            } finally {
                _runningAccountId.value = null
            }
        }
    }

    private suspend fun signInAndNotify(targets: List<Account>) {
        val currentSettings = settings.value
        val results = targets.map { account ->
            val result = service.signIn(account)
            val summary = result.lines.joinToString("\n") { it.first }
            repo.updateSignResult(account.id, summary)
            result
        }
        if (currentSettings.androidNotifications) {
            NotificationHelper.notify(getApplication(), results)
        }
        if (currentSettings.discordWebhook.isNotBlank()) {
            service.sendDiscordWebhook(currentSettings.discordWebhook, currentSettings.discordUserId, targets, results)
        }
    }

    private fun scheduleWork(settings: AppSettings) {
        val now = Calendar.getInstance()
        val target = Calendar.getInstance().apply {
            set(Calendar.HOUR_OF_DAY, settings.scheduleHour)
            set(Calendar.MINUTE, settings.scheduleMinute)
            set(Calendar.SECOND, 0)
            if (before(now)) add(Calendar.DAY_OF_MONTH, 1)
        }
        val delay = target.timeInMillis - now.timeInMillis

        val request = PeriodicWorkRequestBuilder<SignInWorker>(1, TimeUnit.DAYS)
            .setInitialDelay(delay, TimeUnit.MILLISECONDS)
            .setConstraints(Constraints.Builder().setRequiredNetworkType(NetworkType.CONNECTED).build())
            .build()

        workManager.enqueueUniquePeriodicWork(
            SignInWorker.WORK_NAME,
            ExistingPeriodicWorkPolicy.UPDATE,
            request
        )
    }

    private fun cancelWork() {
        workManager.cancelUniqueWork(SignInWorker.WORK_NAME)
    }
}
