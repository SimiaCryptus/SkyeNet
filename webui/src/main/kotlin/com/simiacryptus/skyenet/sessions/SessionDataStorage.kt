package com.simiacryptus.skyenet.sessions

import com.simiacryptus.util.JsonUtil
import java.io.File
import java.util.*
import kotlin.collections.LinkedHashMap

open class SessionDataStorage(
    val dataDir: File = File("sessionData")
) {

    open fun updateMessage(sessionId: String, messageId: String, value: String) {
        validateSessionId(sessionId)
        val file = File(getMessageDir(sessionId), "$messageId.json")
        log.debug("Updating message for $sessionId / $messageId: ${file.absolutePath}")
        file.parentFile.mkdirs()
        JsonUtil.objectMapper().writeValue(file, value)
    }

    open fun loadMessages(sessionId: String): LinkedHashMap<String, String> {
        validateSessionId(sessionId)
        val messageDir = getMessageDir(sessionId)
        val messages = LinkedHashMap<String, String>()
        log.debug("Loading messages for $sessionId: ${messageDir.absolutePath}")
        messageDir.listFiles()?.sortedBy { it.lastModified() }?.forEach { file ->
            val message = JsonUtil.objectMapper().readValue(file, String::class.java)
            messages[file.nameWithoutExtension] = message
        }
        log.debug("Loaded ${messages.size} messages for $sessionId")
        return messages
    }

    protected open fun getMessageDir(sessionId: String): File {
        validateSessionId(sessionId)
        val sessionDir = getInstanceDir(sessionId)
        val messageDir = File(sessionDir, "messages")
        log.debug("Message Dir for $sessionId: ${messageDir.absolutePath}")
        return messageDir
    }

    open fun listSessions(): List<String> {
        val files = dataDir.listFiles()?.flatMap { it.listFiles()?.toList() ?: listOf()  }?.filter { sessionDir ->
            val operationDir = File(sessionDir, "messages")
            if (!operationDir.exists()) false else {
                val listFiles = operationDir.listFiles().filter { it.isFile && !it.name.startsWith("aaa") }
                (listFiles?.size ?: 0) > 0
            }
        }
        log.debug("Sessions: {}", files?.map { it.parentFile.name + "-" + it.name })
        return files?.map { it.parentFile.name + "-" + it.name } ?: listOf()
    }

    open fun getSessionName(sessionId: String): String {
        val userMessages = getUserMessages(sessionId)
        if (userMessages.size > 0) {
            val first = userMessages.first()
            log.debug("Session $sessionId: ${first}")
            return first
        } else {
            log.info("Session $sessionId: No messages")
            return sessionId
        }
    }

    @Suppress("MemberVisibilityCanBePrivate", "Unused")
    fun getUserMessages(sessionId: String): List<String> {
        validateSessionId(sessionId)
        val userMessages = getMessageDir(sessionId).listFiles()?.filter { file ->
            file.isFile
        }?.sortedBy { file ->
            file.lastModified()
            //JsonUtil.objectMapper().readValue(file, OperationStatus::class.java).created
        }?.map { messageFile ->
            val fileText = messageFile.readText()
            val split = fileText.split("<p>")
            if (split.size < 2) {
                log.info("Session $sessionId: No messages")
                ""
            } else {
                val stringList = split[1].split("</p>")
                if (stringList.isEmpty()) {
                    log.info("Session $sessionId: No messages")
                    ""
                } else {
                    stringList.first()
                }
            }
        }?.filter { it.isNotEmpty() } ?: listOf()
        return userMessages
    }

    protected open fun getInstanceDir(sessionId: String): File {
        validateSessionId(sessionId)
        val sessionDir = File(getDateDir(sessionId), getInstanceId(sessionId))
        log.debug("Instance Dir for $sessionId: ${sessionDir.absolutePath}")
        return sessionDir
    }

    open fun getSessionDir(sessionId: String) = getInstanceDir(sessionId)

    protected open fun getDateDir(sessionId: String): File {
        validateSessionId(sessionId)
        val sessionGroupDir = File(dataDir, getDate(sessionId))
        log.debug("Date Dir for $sessionId: ${sessionGroupDir.absolutePath}")
        return sessionGroupDir
    }

    protected open fun getDate(sessionId: String): String {
        validateSessionId(sessionId)
        return sessionId.split("-").firstOrNull() ?: sessionId
    }

    protected open fun getInstanceId(sessionId: String): String {
        validateSessionId(sessionId)
        return stripPrefix(stripPrefix(sessionId, getDate(sessionId)), "-")
    }

    fun <T> getSettings(sessionId: String, clazz: Class<T>): T? {
        validateSessionId(sessionId)
        val settingsFile = File(getSessionDir(sessionId), "settings.json")
        return if (!settingsFile.exists()) null else {
            JsonUtil.objectMapper().readValue(settingsFile, clazz) as T
        }
    }

    fun <T : Any> updateSettings(sessionId: String, settings: T): T {
        validateSessionId(sessionId)
        val settingsFile = File(getSessionDir(sessionId), "settings.json")
        settingsFile.parentFile.mkdirs()
        JsonUtil.objectMapper().writeValue(settingsFile, settings)
        return settings
    }


    companion object {
        fun stripPrefix(text: String, prefix: String): String {
            val startsWith = text.startsWith(prefix)
            return if (startsWith) {
                text.substring(prefix.length)
            } else {
                text
            }
        }

        fun newID(): String {
            val uuid = UUID.randomUUID().toString().split("-").first()
            val yyyyMMdd = java.time.LocalDate.now().toString().replace("-", "")
            log.debug("New ID: $yyyyMMdd-$uuid")
            return "$yyyyMMdd-$uuid"
        }

        fun validateSessionId(sessionId: String) {
            if (!sessionId.matches("""\d{8}-\w{8}""".toRegex())) {
                throw IllegalArgumentException("Invalid session ID: $sessionId")
            }
        }

        private val log = org.slf4j.LoggerFactory.getLogger(SessionDataStorage::class.java)

    }
}
