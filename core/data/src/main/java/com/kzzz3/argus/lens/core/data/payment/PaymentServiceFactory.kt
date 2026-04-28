package com.kzzz3.argus.lens.core.data.payment

import android.content.Context
import com.kzzz3.argus.lens.core.datastore.payment.LocalWalletDetailsStore
import com.kzzz3.argus.lens.core.datastore.payment.LocalWalletSummaryStore
import com.kzzz3.argus.lens.core.network.createAppGson
import com.kzzz3.argus.lens.core.network.createAppRetrofit
import com.kzzz3.argus.lens.core.network.payment.PaymentApiService
import com.kzzz3.argus.lens.session.SessionRepository

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
