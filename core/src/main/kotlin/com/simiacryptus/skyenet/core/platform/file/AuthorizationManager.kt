package com.simiacryptus.skyenet.core.platform.file

import com.simiacryptus.skyenet.core.platform.AuthorizationInterface
import com.simiacryptus.skyenet.core.platform.User
import java.util.*

open class AuthorizationManager : AuthorizationInterface {

    override fun isAuthorized(
        applicationClass: Class<*>?,
        user: User?,
        operationType: AuthorizationInterface.OperationType,
    ) = try {
        if (isUserAuthorized("/permissions/${operationType.name.lowercase(Locale.getDefault())}.txt", user)) {
            log.debug("User {} authorized for {} globally", user, operationType)
            true
        } else if (null != applicationClass) {
            val packagePath = applicationClass.`package`.name.replace('.', '/')
            val opName = operationType.name.lowercase(Locale.getDefault())
            if (isUserAuthorized("/permissions/$packagePath/$opName.txt", user)) {
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

    fun isUserAuthorized(permissionPath: String, user: User?) =
        javaClass.getResourceAsStream(permissionPath)?.use { stream ->
            val lines = stream.bufferedReader().readLines()
            lines.any { line ->
                matches(user, line)
            }
        } ?: false

    open fun matches(user: User?, line: String) = when {
        line.equals(user?.email, ignoreCase = true) -> true // Exact match
        line == "." && user != null -> true // Any user
        line == "*" -> true // Any user including anonymous
        else -> false
    }

    companion object {
        private val log = org.slf4j.LoggerFactory.getLogger(AuthorizationManager::class.java)
    }

}