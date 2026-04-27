package com.kzzz3.argus.lens.app

import com.kzzz3.argus.lens.feature.wallet.WalletAction
import com.kzzz3.argus.lens.feature.wallet.WalletEffect
import com.kzzz3.argus.lens.feature.wallet.WalletReducerResult
import com.kzzz3.argus.lens.feature.wallet.WalletState
import org.junit.Assert.assertEquals
import org.junit.Test

class WalletActionRouteRuntimeTest {
    @Test
    fun handleAction_publishesReducedStateBeforeHandlingEffect() {
        val currentState = WalletState(currentAccountId = "account-1")
        val reducedState = currentState.copy(isLoadingSummary = true)
        val effect = WalletEffect.LoadWalletSummary
        val events = mutableListOf<String>()
        var reducerInputState: WalletState? = null
        var reducerInputAction: WalletAction? = null
        var effectInput: WalletEffect? = null
        val runtime = WalletActionRouteRuntime(
            reduceAction = { state, action ->
                reducerInputState = state
                reducerInputAction = action
                WalletReducerResult(reducedState, effect)
            },
            handleEffect = { resolvedEffect, resolvedState ->
                events += "effect"
                effectInput = resolvedEffect
                assertEquals(reducedState, resolvedState)
            },
        )

        runtime.handleAction(
            action = WalletAction.RefreshWalletSummary,
            request = WalletActionRouteRequest(currentState = currentState),
            callbacks = WalletActionRouteCallbacks(
                onWalletStateChanged = {
                    events += "state"
                    assertEquals(reducedState, it)
                },
            ),
        )

        assertEquals(currentState, reducerInputState)
        assertEquals(WalletAction.RefreshWalletSummary, reducerInputAction)
        assertEquals(effect, effectInput)
        assertEquals(listOf("state", "effect"), events)
    }

    @Test
    fun handleAction_passesNullEffectWithReducedStateAfterPublishingState() {
        val currentState = WalletState(manualPayload = "old")
        val reducedState = currentState.copy(manualPayload = "new")
        val events = mutableListOf<String>()
        var effectWasPresent = true
        val runtime = WalletActionRouteRuntime(
            reduceAction = { _, _ -> WalletReducerResult(reducedState, null) },
            handleEffect = { effect, state ->
                events += "effect"
                effectWasPresent = effect != null
                assertEquals(reducedState, state)
            },
        )

        runtime.handleAction(
            action = WalletAction.UpdateManualPayload("new"),
            request = WalletActionRouteRequest(currentState = currentState),
            callbacks = WalletActionRouteCallbacks(
                onWalletStateChanged = {
                    events += "state"
                    assertEquals(reducedState, it)
                },
            ),
        )

        assertEquals(false, effectWasPresent)
        assertEquals(listOf("state", "effect"), events)
    }
}
