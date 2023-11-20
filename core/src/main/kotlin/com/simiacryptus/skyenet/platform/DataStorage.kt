package com.simiacryptus.skyenet.platform

import com.simiacryptus.util.JsonUtil
import java.io.File
import java.util.*


open class DataStorage(
    private val dataDir: File
) {

    open fun <T> getJson(
        userId: String?,
        sessionId: String,
        clazz: Class<T>,
        filename: String
    ): T? {
        validateSessionId(sessionId)
        val settingsFile = File(this.getSessionDir(userId, sessionId), filename)
        return if (!settingsFile.exists()) null else {
            JsonUtil.objectMapper().readValue(settingsFile, clazz) as T
        }
    }

    open fun getMessages(
        userId: String?,
        sessionId: String
    ): LinkedHashMap<String, String> {
        validateSessionId(sessionId)
        val messageDir = File(this.getSessionDir(userId, sessionId), MESSAGE_DIR)
        val messages = LinkedHashMap<String, String>()
        log.debug("Loading messages for $sessionId: ${messageDir.absolutePath}")
        messageDir.listFiles()?.sortedBy { it.lastModified() }?.forEach { file ->
            val message = JsonUtil.objectMapper().readValue(file, String::class.java)
            messages[file.nameWithoutExtension] = message
        }
        log.debug("Loaded ${messages.size} messages for $sessionId")
        return messages
    }

    open fun getSessionDir(
        userId: String?,
        sessionId: String
    ): File {
        validateSessionId(sessionId)
        val parts = sessionId.split("-")
        return when (parts.size) {
            3 -> {
                val root = when {
                    parts[0] == "G" -> dataDir
                    parts[0] == "U" -> userRoot(userId)
                    else -> throw IllegalArgumentException("Invalid session ID: $sessionId")
                }
                val dateDir = File(root, parts[1])
                log.debug("Date Dir for $sessionId: ${dateDir.absolutePath}")
                val sessionDir = File(dateDir, parts[2])
                log.debug("Instance Dir for $sessionId: ${sessionDir.absolutePath}")
                sessionDir
            }

            2 -> {
                val dateDir = File(dataDir, parts[0])
                log.debug("Date Dir for $sessionId: ${dateDir.absolutePath}")
                val sessionDir = File(dateDir, parts[1])
                log.debug("Instance Dir for $sessionId: ${sessionDir.absolutePath}")
                sessionDir
            }

            else -> {
                throw IllegalArgumentException("Invalid session ID: $sessionId")
            }
        }
    }

    open fun getSessionName(
        userId: String?,
        sessionId: String
    ): String {
        validateSessionId(sessionId)
        val userMessage = File(this.getSessionDir(userId, sessionId), MESSAGE_DIR).listFiles()
            ?.filter { file -> file.isFile }
            ?.sortedBy { file -> file.lastModified() }
            ?.map { messageFile ->
                val fileText = messageFile.readText()
                val split = fileText.split("<p>")
                if (split.size < 2) {
                    log.debug("Session $sessionId: No messages")
                    ""
                } else {
                    val stringList = split[1].split("</p>")
                    if (stringList.isEmpty()) {
                        log.debug("Session $sessionId: No messages")
                        ""
                    } else {
                        stringList.first()
                    }
                }
            }?.firstOrNull { it.isNotEmpty() }
        return if (null != userMessage) {
            log.debug("Session $sessionId: $userMessage")
            userMessage
        } else {
            log.debug("Session $sessionId: No messages")
            sessionId
        }
    }

    open fun listSessions(
        userId: String?
    ): List<String> {
        val globalSessions = listSessions(dataDir)
        val userSessions = if (userId == null) listOf() else listSessions(userRoot(userId))
        return globalSessions.map { "G-$it" } + userSessions.map { "U-$it" }
    }

    open fun <T : Any> setJson(
        userId: String?,
        sessionId: String,
        settings: T,
        filename: String
    ): T {
        validateSessionId(sessionId)
        val settingsFile = File(this.getSessionDir(userId, sessionId), filename)
        settingsFile.parentFile.mkdirs()
        JsonUtil.objectMapper().writeValue(settingsFile, settings)
        return settings
    }

    open fun updateMessage(
        userId: String?,
        sessionId: String,
        messageId: String,
        value: String
    ) {
        validateSessionId(sessionId)
        val file = File(File(this.getSessionDir(userId, sessionId), MESSAGE_DIR), "$messageId.json")
        log.debug("Updating message for $sessionId / $messageId: ${file.absolutePath}")
        file.parentFile.mkdirs()
        JsonUtil.objectMapper().writeValue(file, value)
    }

    open fun validateSessionId(
        sessionId: String
    ) {
        if (!sessionId.matches("""([GU]-)?\d{8}-\w{8}""".toRegex())) {
            throw IllegalArgumentException("Invalid session ID: $sessionId")
        }
    }

    private fun listSessions(dir: File): List<String> {
        val files = dir.listFiles()?.flatMap { it.listFiles()?.toList() ?: listOf() }?.filter { sessionDir ->
            val operationDir = File(sessionDir, MESSAGE_DIR)
            if (!operationDir.exists()) false else {
                val listFiles = operationDir.listFiles()?.filter { it.isFile && !it.name.startsWith("aaa") }
                (listFiles?.size ?: 0) > 0
            }
        }
        log.debug("Sessions: {}", files?.map { it.parentFile.name + "-" + it.name })
        return files?.map { it.parentFile.name + "-" + it.name } ?: listOf()
    }

    private fun userRoot(userId: String?) = File(
        File(dataDir, "users"),
        userId ?: throw IllegalArgumentException("User ID required for private session")
    )

    companion object {

        private val log = org.slf4j.LoggerFactory.getLogger(DataStorage::class.java)

        fun newGlobalID(): String {
            val uuid = UUID.randomUUID().toString().split("-").first()
            val yyyyMMdd = java.time.LocalDate.now().toString().replace("-", "")
            log.debug("New ID: $yyyyMMdd-$uuid")
            return "G-$yyyyMMdd-$uuid"
        }

        fun newUserID(): String {
            val uuid = UUID.randomUUID().toString().split("-").first()
            val yyyyMMdd = java.time.LocalDate.now().toString().replace("-", "")
            log.debug("New ID: $yyyyMMdd-$uuid")
            return "U-$yyyyMMdd-$uuid"
        }

        private const val MESSAGE_DIR = "messages"
        fun String.stripPrefix(prefix: String) = if (!this.startsWith(prefix)) this else {
            this.substring(prefix.length)
        }

    }
}
