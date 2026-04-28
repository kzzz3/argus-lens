package com.kzzz3.argus.lens.session

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
