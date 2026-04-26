package com.kzzz3.argus.lens.data.payment

import android.content.Context
import com.kzzz3.argus.lens.data.network.createAppGson
import com.kzzz3.argus.lens.data.network.createAppRetrofit
import com.kzzz3.argus.lens.data.session.SessionRepository

fun createPaymentRepository(
    context: Context,
    sessionRepository: SessionRepository,
): PaymentRepository {
    val gson = createAppGson()
    val retrofit = createAppRetrofit(gson = gson)

    return CachedPaymentRepository(
        remoteRepository = RemotePaymentRepository(
            sessionRepository = sessionRepository,
            paymentApiService = retrofit.create(PaymentApiService::class.java),
            gson = gson,
        ),
        sessionRepository = sessionRepository,
        walletSummaryStore = LocalWalletSummaryStore(context),
        walletDetailsStore = LocalWalletDetailsStore(context, gson),
    )
}
