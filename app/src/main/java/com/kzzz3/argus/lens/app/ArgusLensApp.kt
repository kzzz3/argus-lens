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
import com.kzzz3.argus.lens.feature.auth.buildDemoPasswordSignInResult
import com.kzzz3.argus.lens.feature.auth.createAuthEntryUiState
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

    val authState = remember(authMode, account, password, submitResult) {
        createAuthEntryUiState(
            selectedMode = authMode,
            account = account,
            password = password,
            submitResult = submitResult,
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
                submitResult = buildDemoPasswordSignInResult(account)
            },
            onBackClick = {
                currentRoute = AppRoute.Home
                submitResult = null
            }
        )
    }
}
