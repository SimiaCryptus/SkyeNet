package com.simiacryptus.skyenet.core.platform

open class AuthenticationManager {

    private val users = HashMap<String, User>()

    open fun getUser(sessionId: String?) = if (null == sessionId) null else users[sessionId]

    open fun containsUser(value: String): Boolean = users.containsKey(value)

    open fun putUser(sessionId: String, user: User) {
        users[sessionId] = user
    }

    companion object {
        const val AUTH_COOKIE = "sessionId"
    }
}