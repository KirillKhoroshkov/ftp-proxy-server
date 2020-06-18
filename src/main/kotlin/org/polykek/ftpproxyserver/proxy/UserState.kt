package org.polykek.ftpproxyserver.proxy

import java.lang.IllegalStateException

class UserState {

    private var isUsernameOk = false
    private var isLogInExpected = false // true before receiving PASS command result
    private var isUserLoggedIn = false
    private var username: String? = null

    fun okUsername() = synchronized(this) {
        if (username == null) {
            throw IllegalStateException("Username is not specified")
        }
        isUsernameOk = true
    }

    fun tryExpectLogIn(): Boolean = synchronized(this) {
        return@synchronized if (isUsernameOk) {
            isLogInExpected = true
            true
        } else {
            false
        }
    }

    fun tryChangeUsername(newUsername: String): Boolean = synchronized(this) {
        return@synchronized if (isLogInExpected || isUserLoggedIn) {
            false
        } else {
            username = newUsername
            isUsernameOk = false
            true
        }
    }

    fun takeUsername(): String = synchronized(this) {
        if (username == null) {
            throw IllegalStateException("Username is not specified")
        }
        return@synchronized username!!
    }

    fun logIn() = synchronized(this) {
        if (username == null) {
            throw IllegalStateException("Username is not specified")
        }
        if (!isUsernameOk) {
            throw IllegalStateException("Username is not ok")
        }
        if (!isLogInExpected) {
            throw IllegalStateException("Log in is not expected")
        }
        isUserLoggedIn = true
    }

    fun logOut() = synchronized(this) {
        isUserLoggedIn = false
        isUsernameOk = false
        isLogInExpected = false
        username = null
    }

    fun isLoggedIn(): Boolean = synchronized(this) {
        return@synchronized isUserLoggedIn
    }
}