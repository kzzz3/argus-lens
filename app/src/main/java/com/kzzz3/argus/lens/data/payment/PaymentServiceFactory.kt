package com.kzzz3.argus.lens.data.payment

import com.kzzz3.argus.lens.data.network.createAppGson
import com.kzzz3.argus.lens.data.network.createAppRetrofit
import com.kzzz3.argus.lens.data.session.SessionRepository

fun createPaymentRepository(
    sessionRepository: SessionRepository,
): PaymentRepository {
    val gson = createAppGson()
    val retrofit = createAppRetrofit(gson = gson)

    return RemotePaymentRepository(
        sessionRepository = sessionRepository,
        paymentApiService = retrofit.create(PaymentApiService::class.java),
        gson = gson,
    )
}
