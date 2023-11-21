package com.simiacryptus.skyenet.platform

import com.simiacryptus.util.JsonUtil
import java.io.File
import java.util.*


open class DataStorage(
    private val dataDir: File
) {

    open fun <T> getJson(
        userId: UserInfo?,
        sessionId: SessionID,
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
        userId: UserInfo?,
        sessionId: SessionID
    ): LinkedHashMap<String, String> {
        validateSessionId(sessionId)
        val messageDir = File(this.getSessionDir(userId, sessionId), MESSAGE_DIR)
        val messages = LinkedHashMap<String, String>()
        log.debug("Loading messages for {}: {}", sessionId, messageDir.absolutePath)
        messageDir.listFiles()?.sortedBy { it.lastModified() }?.forEach { file ->
            val message = JsonUtil.objectMapper().readValue(file, String::class.java)
            messages[file.nameWithoutExtension] = message
        }
        log.debug("Loaded {} messages for {}", messages.size, sessionId)
        return messages
    }

    open fun getSessionDir(
        userId: UserInfo?,
        sessionId: SessionID
    ): File {
        validateSessionId(sessionId)
        val parts = sessionId.sessionId.split("-")
        return when (parts.size) {
            3 -> {
                val root = when {
                    parts[0] == "G" -> dataDir
                    parts[0] == "U" -> userRoot(userId)
                    else -> throw IllegalArgumentException("Invalid session ID: $sessionId")
                }
                val dateDir = File(root, parts[1])
                log.debug("Date Dir for {}: {}", sessionId, dateDir.absolutePath)
                val sessionDir = File(dateDir, parts[2])
                log.debug("Instance Dir for {}: {}", sessionId, sessionDir.absolutePath)
                sessionDir
            }

            2 -> {
                val dateDir = File(dataDir, parts[0])
                log.debug("Date Dir for {}: {}", sessionId, dateDir.absolutePath)
                val sessionDir = File(dateDir, parts[1])
                log.debug("Instance Dir for {}: {}", sessionId, sessionDir.absolutePath)
                sessionDir
            }

            else -> {
                throw IllegalArgumentException("Invalid session ID: $sessionId")
            }
        }
    }

    open fun getSessionName(
        userId: UserInfo?,
        sessionId: SessionID
    ): String {
        validateSessionId(sessionId)
        val userMessage = File(this.getSessionDir(userId, sessionId), MESSAGE_DIR).listFiles()
            ?.filter { file -> file.isFile }
            ?.sortedBy { file -> file.lastModified() }
            ?.map { messageFile ->
                val fileText = messageFile.readText()
                val split = fileText.split("<p>")
                if (split.size < 2) {
                    log.debug("Session {}: No messages", sessionId)
                    ""
                } else {
                    val stringList = split[1].split("</p>")
                    if (stringList.isEmpty()) {
                        log.debug("Session {}: No messages", sessionId)
                        ""
                    } else {
                        stringList.first()
                    }
                }
            }?.firstOrNull { it.isNotEmpty() }
        return if (null != userMessage) {
            log.debug("Session {}: {}", sessionId, userMessage)
            userMessage
        } else {
            log.debug("Session {}: No messages", sessionId)
            sessionId.sessionId
        }
    }

    open fun listSessions(
        userId: UserInfo?
    ): List<SessionID> {
        val globalSessions = listSessions(dataDir)
        val userSessions = if (userId == null) listOf() else listSessions(userRoot(userId))
        return globalSessions.map { SessionID("G-$it") } + userSessions.map { SessionID("U-$it") }
    }

    open fun <T : Any> setJson(
        userId: UserInfo?,
        sessionId: SessionID,
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
        userId: UserInfo?,
        sessionId: SessionID,
        messageId: String,
        value: String
    ) {
        validateSessionId(sessionId)
        val file = File(File(this.getSessionDir(userId, sessionId), MESSAGE_DIR), "$messageId.json")
        log.debug("Updating message for {} / {}: {}", sessionId, messageId, file.absolutePath)
        file.parentFile.mkdirs()
        JsonUtil.objectMapper().writeValue(file, value)
    }

    private fun listSessions(dir: File): List<SessionID> {
        val files = dir.listFiles()?.flatMap { it.listFiles()?.toList() ?: listOf() }?.filter { sessionDir ->
            val operationDir = File(sessionDir, MESSAGE_DIR)
            if (!operationDir.exists()) false else {
                val listFiles = operationDir.listFiles()?.filter { it.isFile && !it.name.startsWith("aaa") }
                (listFiles?.size ?: 0) > 0
            }
        }
        log.debug("Sessions: {}", files?.map { it.parentFile.name + "-" + it.name })
        return files?.map { SessionID(it.parentFile.name + "-" + it.name) } ?: listOf()
    }

    private fun userRoot(userId: UserInfo?) = File(
        File(dataDir, "users"),
        userId?.email ?: throw IllegalArgumentException("User required for private session")
    )

    companion object {

        private val log = org.slf4j.LoggerFactory.getLogger(DataStorage::class.java)

        fun validateSessionId(
            sessionId: SessionID
        ) {
            if (!sessionId.sessionId.matches("""([GU]-)?\d{8}-\w{8}""".toRegex())) {
                throw IllegalArgumentException("Invalid session ID: $sessionId")
            }
        }

        fun newGlobalID(): SessionID {
            val uuid = UUID.randomUUID().toString().split("-").first()
            val yyyyMMdd = java.time.LocalDate.now().toString().replace("-", "")
            log.debug("New ID: $yyyyMMdd-$uuid")
            return SessionID("G-$yyyyMMdd-$uuid")
        }

        fun newUserID(): SessionID {
            val uuid = UUID.randomUUID().toString().split("-").first()
            val yyyyMMdd = java.time.LocalDate.now().toString().replace("-", "")
            log.debug("New ID: $yyyyMMdd-$uuid")
            return SessionID("U-$yyyyMMdd-$uuid")
        }

        private const val MESSAGE_DIR = "messages"

    }
}

