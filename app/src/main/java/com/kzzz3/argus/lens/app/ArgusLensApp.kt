package com.kzzz3.argus.lens.app

import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import com.kzzz3.argus.lens.app.navigation.AppRoute
import com.kzzz3.argus.lens.feature.auth.AuthEntryAction
import com.kzzz3.argus.lens.feature.auth.AuthEntryScreen
import com.kzzz3.argus.lens.feature.auth.AuthFormState
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
                when (action) {
                    is AuthEntryAction.ChangeMode -> {
                        authFormState = authFormState.copy(
                            mode = action.mode,
                            submitResult = null,
                        )
                    }

                    is AuthEntryAction.ChangeAccount -> {
                        authFormState = authFormState.copy(
                            account = action.value,
                            submitResult = null,
                        )
                    }

                    is AuthEntryAction.ChangePassword -> {
                        authFormState = authFormState.copy(
                            password = action.value,
                            submitResult = null,
                        )
                    }

                    AuthEntryAction.SubmitPasswordLogin -> {
                        authFormState = authFormState.copy(
                            submitResult = buildDemoPasswordSignInResult(authFormState),
                        )
                    }

                    AuthEntryAction.NavigateBack -> {
                        currentRoute = AppRoute.Home
                        authFormState = authFormState.copy(submitResult = null)
                    }
                }
            }
        )
    }
}
