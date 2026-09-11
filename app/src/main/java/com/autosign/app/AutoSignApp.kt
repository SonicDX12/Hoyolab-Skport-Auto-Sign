package com.autosign.app

import android.app.Application
import com.autosign.app.data.repository.AccountRepository

class AutoSignApp : Application() {
    val repository: AccountRepository by lazy { AccountRepository(this) }
}
