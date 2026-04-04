package com.kzzz3.argus.lens.app

import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import com.kzzz3.argus.lens.app.navigation.AppRoute
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
    val authState = remember {
        AuthEntryUiState(
            title = "Stage 1 Login Entry",
            subtitle = "We start with a fake login shell before touching real networking.",
            primaryActionLabel = "Enter login module",
            secondaryActionLabel = "Back to HUD"
        )
    }
    var currentRoute by rememberSaveable { mutableStateOf(AppRoute.Home) }

    when (currentRoute) {
        AppRoute.Home -> HomeHudScreen(
            state = homeState,
            onPrimaryActionClick = { currentRoute = AppRoute.AuthEntry }
        )

        AppRoute.AuthEntry -> AuthEntryScreen(
            state = authState,
            onPrimaryActionClick = {},
            onBackClick = { currentRoute = AppRoute.Home }
        )
    }
}
