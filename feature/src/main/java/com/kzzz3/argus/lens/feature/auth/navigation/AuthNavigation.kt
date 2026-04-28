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
import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable

const val AuthGraphRoutePattern = "auth_graph"
const val LoginRoutePattern = "AuthEntry"
const val RegisterRoutePattern = "RegisterEntry"

@Serializable
@SerialName(AuthGraphRoutePattern)
data object AuthGraphRoute

@Serializable
@SerialName(LoginRoutePattern)
data object LoginRoute

@Serializable
@SerialName(RegisterRoutePattern)
data object RegisterRoute

data class AuthRoutes(
    val authState: AuthEntryUiState,
    val registerState: RegisterUiState,
    val onAuthAction: (AuthEntryAction) -> Unit,
    val onRegisterAction: (RegisterAction) -> Unit,
)

fun NavGraphBuilder.authGraph(
    routes: AuthRoutes,
) {
    navigation<AuthGraphRoute>(
        startDestination = LoginRoute,
    ) {
        composable<LoginRoute> {
            AuthEntryScreen(
                state = routes.authState,
                onAction = routes.onAuthAction,
            )
        }

        composable<RegisterRoute> {
            RegisterScreen(
                state = routes.registerState,
                onAction = routes.onRegisterAction,
            )
        }
    }
}

fun isAuthDestination(route: String?): Boolean {
    return route == LoginRoutePattern || route == RegisterRoutePattern
}
