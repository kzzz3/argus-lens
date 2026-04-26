package com.kzzz3.argus.lens.app

import com.kzzz3.argus.lens.feature.wallet.WalletState
import com.kzzz3.argus.lens.model.session.AppSessionState
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Job
import kotlinx.coroutines.launch

class WalletRequestRuntime(
    private val scope: CoroutineScope,
) {
    private var generation: Int = 0
    private val jobs = mutableSetOf<Job>()

    fun invalidate() {
        generation += 1
        jobs.toList().forEach { it.cancel() }
        jobs.clear()
    }

    fun launchStateRequest(
        requestSession: AppSessionState,
        getCurrentSession: () -> AppSessionState,
        getCurrentState: () -> WalletState,
        setState: (WalletState) -> Unit,
        block: suspend (WalletState) -> WalletState,
    ) {
        val requestAccountId = requestSession.accountId
        val requestGeneration = generation
        val job = scope.launch {
            val nextState = block(getCurrentState())
            setState(
                applyWalletRequestResult(
                    currentState = getCurrentState(),
                    isActive = shouldApplyWalletRequestResult(
                        currentSession = getCurrentSession(),
                        requestAccountId = requestAccountId,
                        requestGeneration = requestGeneration,
                        activeGeneration = generation,
                    ),
                ) { nextState }
            )
        }
        jobs += job
        job.invokeOnCompletion {
            jobs.remove(job)
        }
    }
}
