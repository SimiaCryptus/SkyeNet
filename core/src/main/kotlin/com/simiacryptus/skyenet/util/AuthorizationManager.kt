package com.simiacryptus.skyenet.util

import java.nio.file.Files
import java.nio.file.Paths
import java.util.*

object AuthorizationManager {

    enum class OperationType {
        Read,
        Write,
        Execute,
        Admin,
        GlobalKey,
    }

    fun isAuthorized(
        applicationClass: Class<*>?,
        user: String?,
        operationType: OperationType,
    ) = try {
        if (isUserAuthorized("/permissions/${operationType.name.lowercase(Locale.getDefault())}.txt", user)) {
            log.debug("User {} authorized for {} globally", user, operationType)
            true
        } else if(null != applicationClass) {
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

    private fun isUserAuthorized(permissionPath: String, user: String?): Boolean {
        val url = this::class.java.getResource(permissionPath)
        if (url == null) {
            log.debug("No permissions file found at $permissionPath")
            return false
        }
        val path = Paths.get(url.toURI())
        val lines = Files.readAllLines(path)
        return lines.any { it.equals(user, ignoreCase = true) || it == "*" }
    }

    val log = org.slf4j.LoggerFactory.getLogger(AuthorizationManager::class.java)

}