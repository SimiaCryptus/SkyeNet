package com.simiacryptus.skyenet.core.platform

open class AuthenticationManager {

    private val users = HashMap<String, User>()

    open fun getUser(accessToken: String?) = if (null == accessToken) null else users[accessToken]

    open fun containsUser(value: String): Boolean = users.containsKey(value)

    open fun putUser(accessToken: String, user: User): User {
        users[accessToken] = user
        return user
    }

    companion object {
        const val AUTH_COOKIE = "sessionId"
    }
}