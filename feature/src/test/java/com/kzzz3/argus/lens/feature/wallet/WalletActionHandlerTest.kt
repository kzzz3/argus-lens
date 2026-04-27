package com.kzzz3.argus.lens.feature.wallet

import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Test

class WalletActionHandlerTest {
    @Test
    fun handleAction_publishesReducedStateBeforeHandlingEffect() {
        val currentState = WalletState(currentAccountId = "account-1")
        val reducedState = currentState.copy(isLoadingSummary = true)
        val effect = WalletEffect.LoadWalletSummary
        val events = mutableListOf<String>()
        var reducerInputState: WalletState? = null
        var reducerInputAction: WalletAction? = null
        var effectInput: WalletEffect? = null
        val handler = WalletActionHandler(
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

        handler.handleAction(
            action = WalletAction.RefreshWalletSummary,
            request = WalletActionRequest(currentState = currentState),
            callbacks = WalletActionCallbacks(
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
        val handler = WalletActionHandler(
            reduceAction = { _, _ -> WalletReducerResult(reducedState, null) },
            handleEffect = { effect, state ->
                events += "effect"
                effectWasPresent = effect != null
                assertEquals(reducedState, state)
            },
        )

        handler.handleAction(
            action = WalletAction.UpdateManualPayload("new"),
            request = WalletActionRequest(currentState = currentState),
            callbacks = WalletActionCallbacks(
                onWalletStateChanged = {
                    events += "state"
                    assertEquals(reducedState, it)
                },
            ),
        )

        assertFalse(effectWasPresent)
        assertEquals(listOf("state", "effect"), events)
    }
}
