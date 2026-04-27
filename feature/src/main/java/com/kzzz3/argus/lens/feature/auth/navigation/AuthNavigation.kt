package com.kzzz3.argus.lens.feature.auth.navigation

import androidx.navigation.NavGraphBuilder
import androidx.navigation.compose.composable
import androidx.navigation.navigation
import com.kzzz3.argus.lens.feature.auth.AuthEntryAction
import com.kzzz3.argus.lens.feature.auth.AuthEntryScreen
import com.kzzz3.argus.lens.feature.auth.AuthEntryUiState
import com.kzzz3.argus.lens.feature.register.RegisterAction
import com.kzzz3.argus.lens.feature.register.RegisterScreen
import com.kzzz3.argus.lens.feature.register.RegisterUiState

const val AuthGraphRoute = "auth_graph"

enum class AuthDestination(val route: String) {
    Login("AuthEntry"),
    Register("RegisterEntry"),
}

fun NavGraphBuilder.authGraph(
    authState: AuthEntryUiState,
    registerState: RegisterUiState,
    onAuthAction: (AuthEntryAction) -> Unit,
    onRegisterAction: (RegisterAction) -> Unit,
) {
    navigation(
        startDestination = AuthDestination.Login.route,
        route = AuthGraphRoute,
    ) {
        composable(AuthDestination.Login.route) {
            AuthEntryScreen(
                state = authState,
                onAction = onAuthAction,
            )
        }

        composable(AuthDestination.Register.route) {
            RegisterScreen(
                state = registerState,
                onAction = onRegisterAction,
            )
        }
    }
}

fun isAuthDestination(route: String?): Boolean {
    return AuthDestination.entries.any { destination -> destination.route == route }
}
