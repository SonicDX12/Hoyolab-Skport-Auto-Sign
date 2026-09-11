package com.autosign.app

import android.Manifest
import android.os.Build
import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.result.contract.ActivityResultContracts
import androidx.activity.viewModels
import androidx.compose.runtime.*
import androidx.compose.runtime.collectAsState
import androidx.lifecycle.viewmodel.compose.viewModel
import androidx.navigation.NavType
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.rememberNavController
import androidx.navigation.navArgument
import com.autosign.app.data.model.Account
import com.autosign.app.ui.screens.AddEditAccountScreen
import com.autosign.app.ui.screens.AddEditAccountViewModel
import com.autosign.app.ui.screens.HomeActions
import com.autosign.app.ui.screens.HomeScreen
import com.autosign.app.ui.screens.SettingsScreen
import com.autosign.app.ui.theme.AutoSignTheme

class MainActivity : ComponentActivity() {

    private val viewModel: MainViewModel by viewModels()

    private val notificationPermission = registerForActivityResult(ActivityResultContracts.RequestPermission()) {}

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
            notificationPermission.launch(Manifest.permission.POST_NOTIFICATIONS)
        }

        setContent {
            AutoSignTheme {
                val navController = rememberNavController()
                val accounts by viewModel.accounts.collectAsState()
                val settings by viewModel.settings.collectAsState()
                val isRunning by viewModel.isRunning.collectAsState()
                val runningAccountId by viewModel.runningAccountId.collectAsState()

                val homeActions = remember(navController, viewModel) {
                    object : HomeActions {
                        override fun onAddAccount() { navController.navigate("add_account") }
                        override fun onEditAccount(account: Account) { navController.navigate("edit_account/${account.id}") }
                        override fun onDeleteAccount(accountId: String) { viewModel.deleteAccount(accountId) }
                        override fun onRunNow() { viewModel.runNow() }
                        override fun onRunAccount(accountId: String) { viewModel.runAccount(accountId) }
                        override fun onSettings() { navController.navigate("settings") }
                    }
                }

                NavHost(navController = navController, startDestination = "home") {

                    composable("home") {
                        HomeScreen(
                            accounts = accounts,
                            isRunning = isRunning,
                            runningAccountId = runningAccountId,
                            actions = homeActions
                        )
                    }

                    composable("add_account") {
                        val vm: AddEditAccountViewModel = viewModel(factory = AddEditAccountViewModel.factory(existing = null))
                        AddEditAccountScreen(
                            viewModel = vm,
                            onSave = { account ->
                                viewModel.addAccount(account)
                                navController.popBackStack()
                            },
                            onBack = { navController.popBackStack() },
                            generateId = { viewModel.generateId() }
                        )
                    }

                    composable(
                        "edit_account/{accountId}",
                        arguments = listOf(navArgument("accountId") { type = NavType.StringType })
                    ) { backStackEntry ->
                        val accountId = backStackEntry.arguments?.getString("accountId")
                        val account = accounts.firstOrNull { it.id == accountId }
                        if (account != null) {
                            val vm: AddEditAccountViewModel = viewModel(factory = AddEditAccountViewModel.factory(existing = account))
                            AddEditAccountScreen(
                                viewModel = vm,
                                onSave = { updated ->
                                    viewModel.updateAccount(updated)
                                    navController.popBackStack()
                                },
                                onBack = { navController.popBackStack() },
                                generateId = { viewModel.generateId() }
                            )
                        }
                    }

                    composable("settings") {
                        SettingsScreen(
                            settings = settings,
                            onSave = { updated ->
                                viewModel.saveSettings(updated)
                                navController.popBackStack()
                            },
                            onBack = { navController.popBackStack() }
                        )
                    }
                }
            }
        }
    }
}
