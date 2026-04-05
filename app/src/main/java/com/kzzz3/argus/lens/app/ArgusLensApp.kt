package com.kzzz3.argus.lens.app

import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import com.kzzz3.argus.lens.app.navigation.AppRoute
import com.kzzz3.argus.lens.feature.auth.AuthEntryAction
import com.kzzz3.argus.lens.feature.auth.AuthEntryEffect
import com.kzzz3.argus.lens.feature.auth.AuthEntryScreen
import com.kzzz3.argus.lens.feature.auth.AuthFormState
import com.kzzz3.argus.lens.feature.auth.createAuthEntryUiState
import com.kzzz3.argus.lens.feature.auth.reduceAuthFormState
import com.kzzz3.argus.lens.feature.home.HomeHudScreen
import com.kzzz3.argus.lens.feature.home.HomeHudUiState
import com.kzzz3.argus.lens.feature.inbox.InboxPlaceholderScreen
import com.kzzz3.argus.lens.feature.inbox.InboxPlaceholderUiState

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
    val inboxState = remember {
        InboxPlaceholderUiState(
            title = "Login success",
            subtitle = "You have entered the stage-1 inbox placeholder.",
            primaryActionLabel = "Back to HUD"
        )
    }
    var currentRoute by rememberSaveable { mutableStateOf(AppRoute.Home) }
    var authFormState by rememberSaveable(stateSaver = AuthFormState.Saver) {
        mutableStateOf(AuthFormState())
    }

    val authState = remember(authFormState) {
        createAuthEntryUiState(
            formState = authFormState,
        )
    }

    when (currentRoute) {
        AppRoute.Home -> HomeHudScreen(
            state = homeState,
            onPrimaryActionClick = { currentRoute = AppRoute.AuthEntry }
        )

        AppRoute.AuthEntry -> AuthEntryScreen(
            state = authState,
            onAction = { action ->
                val result = reduceAuthFormState(
                    currentState = authFormState,
                    action = action,
                )
                authFormState = result.formState

                when (result.effect) {
                    AuthEntryEffect.NavigateBack -> currentRoute = AppRoute.Home
                    AuthEntryEffect.NavigateToInboxPlaceholder -> currentRoute = AppRoute.InboxPlaceholder
                    null -> Unit
                }
            }
        )

        AppRoute.InboxPlaceholder -> InboxPlaceholderScreen(
            state = inboxState,
            onPrimaryActionClick = { currentRoute = AppRoute.Home }
        )
    }
}
