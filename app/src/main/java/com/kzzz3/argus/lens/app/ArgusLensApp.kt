package com.kzzz3.argus.lens.app

import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import com.kzzz3.argus.lens.app.navigation.AppRoute
import com.kzzz3.argus.lens.feature.auth.AuthLoginMode
import com.kzzz3.argus.lens.feature.auth.AuthEntryScreen
import com.kzzz3.argus.lens.feature.auth.AuthEntryUiState
import com.kzzz3.argus.lens.feature.home.HomeHudScreen
import com.kzzz3.argus.lens.feature.home.HomeHudUiState

@Composable
fun ArgusLensApp() {
    val homeState = remember {
        HomeHudUiState(
            deviceLabel = "Android Glasses Simulator",
            syncStatus = "Stage 1 Baseline Ready",
            activeMode = "IM Foundation",
            primaryHint = "Next module: login + session bootstrap"
        )
    }
    var currentRoute by rememberSaveable { mutableStateOf(AppRoute.Home) }
    var authMode by rememberSaveable { mutableStateOf(AuthLoginMode.Password) }
    var account by rememberSaveable { mutableStateOf("") }
    var password by rememberSaveable { mutableStateOf("") }
    var submitResult by rememberSaveable { mutableStateOf<String?>(null) }

    val trimmedAccount = account.trim()
    val accountError = when {
        authMode != AuthLoginMode.Password -> null
        trimmedAccount.isEmpty() -> "Account is required"
        trimmedAccount.length < 4 -> "Account must be at least 4 characters"
        else -> null
    }
    val passwordError = when {
        authMode != AuthLoginMode.Password -> null
        password.isEmpty() -> "Password is required"
        password.length < 6 -> "Password must be at least 6 characters"
        else -> null
    }
    val isPrimaryActionEnabled =
        authMode == AuthLoginMode.Password && accountError == null && passwordError == null

    val authState = remember(authMode, account, password, accountError, passwordError, submitResult, isPrimaryActionEnabled) {
        AuthEntryUiState(
            title = "Stage 1 Login Entry",
            subtitle = "We start with a fake login shell before touching real networking.",
            selectedMode = authMode,
            account = account,
            password = password,
            accountError = accountError,
            passwordError = passwordError,
            submitResult = submitResult,
            isPrimaryActionEnabled = isPrimaryActionEnabled,
            primaryActionLabel = "Sign in with password",
            secondaryActionLabel = "Back to HUD"
        )
    }

    when (currentRoute) {
        AppRoute.Home -> HomeHudScreen(
            state = homeState,
            onPrimaryActionClick = { currentRoute = AppRoute.AuthEntry }
        )

        AppRoute.AuthEntry -> AuthEntryScreen(
            state = authState,
            onModeChange = {
                authMode = it
                submitResult = null
            },
            onAccountChange = {
                account = it
                submitResult = null
            },
            onPasswordChange = {
                password = it
                submitResult = null
            },
            onPrimaryActionClick = {
                submitResult = "Demo sign-in passed for $trimmedAccount. Real network login comes next."
            },
            onBackClick = {
                currentRoute = AppRoute.Home
                submitResult = null
            }
        )
    }
}
