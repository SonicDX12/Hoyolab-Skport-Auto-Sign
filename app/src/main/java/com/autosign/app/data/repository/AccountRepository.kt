package com.autosign.app.data.repository

import android.content.Context
import com.autosign.app.data.model.Account
import com.autosign.app.data.model.AppSettings
import com.autosign.app.data.storage.AccountStorage
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import java.util.UUID

class AccountRepository(context: Context) {

    private val storage = AccountStorage(context)

    private val _accounts = MutableStateFlow<List<Account>>(emptyList())
    val accounts: StateFlow<List<Account>> = _accounts.asStateFlow()

    private val _settings = MutableStateFlow(AppSettings())
    val settings: StateFlow<AppSettings> = _settings.asStateFlow()

    init {
        _accounts.value = storage.loadAccounts()
        _settings.value = storage.loadSettings()
    }

    fun addAccount(account: Account) {
        val updated = _accounts.value + account
        _accounts.value = updated
        storage.saveAccounts(updated)
    }

    fun updateAccount(account: Account) {
        val updated = _accounts.value.map { if (it.id == account.id) account else it }
        _accounts.value = updated
        storage.saveAccounts(updated)
    }

    fun deleteAccount(accountId: String) {
        val updated = _accounts.value.filter { it.id != accountId }
        _accounts.value = updated
        storage.saveAccounts(updated)
    }

    fun updateSignResult(accountId: String, result: String) {
        val updated = _accounts.value.map { acc ->
            if (acc.id != accountId) acc
            else when (acc) {
                is Account.HoYoLab -> acc.copy(lastSignResult = result, lastSignTime = System.currentTimeMillis())
                is Account.SkPort -> acc.copy(lastSignResult = result, lastSignTime = System.currentTimeMillis())
            }
        }
        _accounts.value = updated
        storage.saveAccounts(updated)
    }

    fun saveSettings(settings: AppSettings) {
        _settings.value = settings
        storage.saveSettings(settings)
    }

    fun generateId(): String = UUID.randomUUID().toString()
}
