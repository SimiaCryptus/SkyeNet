package com.simiacryptus.skyenet.platform

import java.util.*

open class AuthorizationManager {

    enum class OperationType {
        Read,
        Write,
        Execute,
        Delete,
        Admin,
        GlobalKey,
    }

    open fun isAuthorized(
        applicationClass: Class<*>?,
        user: String?,
        operationType: OperationType,
    ) = try {
        if (isUserAuthorized("/permissions/${operationType.name.lowercase(Locale.getDefault())}.txt", user)) {
            log.debug("User {} authorized for {} globally", user, operationType)
            true
        } else if (null != applicationClass) {
            val packagePath = applicationClass.`package`.name.replace('.', '/')
            val opName = operationType.name.lowercase(Locale.getDefault())
            if (isUserAuthorized("/$packagePath/$opName.txt", user)) {
                log.debug("User {} authorized for {} on {}", user, operationType, applicationClass)
                true
            } else {
                log.debug("User {} not authorized for {} on {}", user, operationType, applicationClass)
                false
            }
        } else {
            log.debug("User {} not authorized for {} globally", user, operationType)
            false
        }
    } catch (e: Exception) {
        log.error("Error checking authorization", e)
        false
    }

    private fun isUserAuthorized(permissionPath: String, user: String?) =
        javaClass.getResourceAsStream(permissionPath)?.use { stream ->
            val lines = stream.bufferedReader().readLines()
            lines.any {
                when {
                    it.equals(user, ignoreCase = true) -> true // Exact match
                    it == "." && user != null -> true // Any user
                    it == "*" -> true // Any user including anonymous
                    else -> false
                }
            }
        } ?: false

    private val log = org.slf4j.LoggerFactory.getLogger(AuthorizationManager::class.java)

}