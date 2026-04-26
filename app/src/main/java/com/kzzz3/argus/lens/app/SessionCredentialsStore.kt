package com.kzzz3.argus.lens.app

import com.kzzz3.argus.lens.data.session.SessionCredentials

class SessionCredentialsStore(
    initialCredentials: SessionCredentials = SessionCredentials(),
) {
    var current: SessionCredentials = initialCredentials
        private set

    fun update(credentials: SessionCredentials) {
        current = credentials
    }

    fun clear() {
        current = SessionCredentials()
    }
}
