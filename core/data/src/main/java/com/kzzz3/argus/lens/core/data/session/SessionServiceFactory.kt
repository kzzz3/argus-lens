package com.kzzz3.argus.lens.core.data.session

import android.content.Context
import com.kzzz3.argus.lens.core.datastore.session.createLocalSessionCredentialsSnapshot
import com.kzzz3.argus.lens.core.datastore.session.createLocalSessionSnapshot
import com.kzzz3.argus.lens.core.datastore.session.createLocalSessionStore
import com.kzzz3.argus.lens.model.session.AppSessionState
import com.kzzz3.argus.lens.session.SessionCredentials
import com.kzzz3.argus.lens.session.SessionRepository

fun createSessionRepository(context: Context): SessionRepository {
    return createLocalSessionStore(context.applicationContext)
}

fun createInitialSessionSnapshot(context: Context): AppSessionState {
    return createLocalSessionSnapshot(context.applicationContext)
}

fun createInitialSessionCredentials(context: Context): SessionCredentials {
    return createLocalSessionCredentialsSnapshot(context.applicationContext)
}
