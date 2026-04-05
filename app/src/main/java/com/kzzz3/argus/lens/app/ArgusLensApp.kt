package com.kzzz3.argus.lens.app

import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import com.kzzz3.argus.lens.app.navigation.AppRoute
import com.kzzz3.argus.lens.app.session.AppSessionState
import com.kzzz3.argus.lens.app.session.createPlaceholderSession
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
    var currentRoute by rememberSaveable { mutableStateOf(AppRoute.Home) }
    var authFormState by rememberSaveable(stateSaver = AuthFormState.Saver) {
        mutableStateOf(AuthFormState())
    }
    var appSessionState by rememberSaveable(stateSaver = AppSessionState.Saver) {
        mutableStateOf(AppSessionState())
    }

    val authState = remember(authFormState) {
        createAuthEntryUiState(
            formState = authFormState,
        )
    }
    val inboxState = remember(appSessionState) {
        InboxPlaceholderUiState(
            title = "Login success",
            subtitle = "You have entered the stage-1 inbox placeholder.",
            sessionLabel = if (appSessionState.isAuthenticated) {
                "Signed in as ${appSessionState.displayName}"
            } else {
                "No active session"
            },
            sessionSummary = if (appSessionState.isAuthenticated) {
                "Account ID: ${appSessionState.accountId}. Session placeholder is active and ready for future backend integration."
            } else {
                "Session placeholder is empty."
            },
            primaryActionLabel = "Sign out to HUD"
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
                    AuthEntryEffect.NavigateToInboxPlaceholder -> {
                        appSessionState = createPlaceholderSession(authFormState.account)
                        currentRoute = AppRoute.InboxPlaceholder
                    }
                    null -> Unit
                }
            }
        )

        AppRoute.InboxPlaceholder -> InboxPlaceholderScreen(
            state = inboxState,
            onPrimaryActionClick = {
                appSessionState = AppSessionState()
                authFormState = AuthFormState()
                currentRoute = AppRoute.Home
            }
        )
    }
}
