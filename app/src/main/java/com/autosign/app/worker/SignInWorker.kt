package com.autosign.app.worker

import android.content.Context
import androidx.work.CoroutineWorker
import androidx.work.WorkerParameters
import com.autosign.app.api.NotificationHelper
import com.autosign.app.api.SignInService
import com.autosign.app.data.repository.AccountRepository

class SignInWorker(
    private val context: Context,
    params: WorkerParameters
) : CoroutineWorker(context, params) {

    companion object {
        const val WORK_NAME = "daily_sign_in"
    }

    private val service = SignInService()

    override suspend fun doWork(): Result {
        val repo = AccountRepository(context)
        val accounts = repo.accounts.value
        val settings = repo.settings.value

        if (accounts.isEmpty()) return Result.success()

        val results = accounts.map { account ->
            val result = service.signIn(account)
            val summary = result.lines.joinToString("\n") { it.first }
            repo.updateSignResult(account.id, summary)
            result
        }

        if (settings.androidNotifications) {
            NotificationHelper.notify(context, results)
        }
        if (settings.discordWebhook.isNotBlank()) {
            service.sendDiscordWebhook(settings.discordWebhook, settings.discordUserId, accounts, results)
        }

        return Result.success()
    }
}
