package com.simiacryptus.skyenet.config

import java.util.HashMap

open class AuthenticationManager {

    data class UserInfo(
        val id: String,
        val email: String,
        val name: String,
        val picture: String
    )

    private val users = HashMap<String, UserInfo>()

    open fun getUser(sessionId: String?) = if (null == sessionId) null else users[sessionId]

    open fun containsKey(value: String): Boolean = users.containsKey(value)

    open fun setUser(sessionId: String, userInfo: UserInfo) {
        users[sessionId] = userInfo
    }

    companion object {
        const val COOKIE_NAME = "sessionId"
        private val log = org.slf4j.LoggerFactory.getLogger(AuthenticationManager::class.java)
    }
}