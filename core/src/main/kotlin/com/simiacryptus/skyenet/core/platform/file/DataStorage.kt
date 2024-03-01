package com.simiacryptus.skyenet.core.platform.file

import com.simiacryptus.jopenai.util.JsonUtil
import com.simiacryptus.skyenet.core.platform.Session
import com.simiacryptus.skyenet.core.platform.StorageInterface
import com.simiacryptus.skyenet.core.platform.StorageInterface.Companion.validateSessionId
import com.simiacryptus.skyenet.core.platform.User
import java.io.File
import java.text.SimpleDateFormat
import java.util.*


open class DataStorage(
    private val dataDir: File
) : StorageInterface {

    override fun <T> getJson(
        user: User?,
        session: Session,
        filename: String,
        clazz: Class<T>
    ) = getJson(getSessionDir(user, session), filename, clazz)

    private fun <T> getJson(sessionDir: File, filename: String, clazz: Class<T>): T? {
        val settingsFile = File(sessionDir, filename)
        return if (!settingsFile.exists()) null else {
            JsonUtil.objectMapper().readValue(settingsFile, clazz) as T
        }
    }

    override fun getMessages(
        user: User?,
        session: Session
    ): LinkedHashMap<String, String> {
        validateSessionId(session)
        val messageDir = File(this.getSessionDir(user, session), MESSAGE_DIR)
        val messages = LinkedHashMap<String, String>()
        getMessageIds(user, session).forEach { messageId ->
            val file = File(messageDir, "$messageId.json")
            if (file.exists()) {
                val message = JsonUtil.objectMapper().readValue(file, String::class.java)
                messages[messageId] = message
            }
        }
        //log.debug("Loaded {} messages for {}", messages.size, session)
        return messages
    }

    override fun getSessionDir(
        user: User?,
        session: Session
    ): File {
        if(sessionPaths.containsKey(session)) {
            return sessionPaths[session]!!
        }
        validateSessionId(session)
        val parts = session.sessionId.split("-")
        return when (parts.size) {
            3 -> {
                val root = when {
                    parts[0] == "G" -> dataDir
                    parts[0] == "U" -> userRoot(user)
                    else -> throw IllegalArgumentException("Invalid session ID: $session")
                }
                val dateDir = File(root, parts[1])
                //log.debug("Date Dir for {}: {}", session, dateDir.absolutePath)
                val sessionDir = File(dateDir, parts[2])
                //log.debug("Instance Dir for {}: {}", session, sessionDir.absolutePath)
                sessionDir
            }

            2 -> {
                val dateDir = File(dataDir, parts[0])
                //log.debug("Date Dir for {}: {}", session, dateDir.absolutePath)
                val sessionDir = File(dateDir, parts[1])
                //log.debug("Instance Dir for {}: {}", session, sessionDir.absolutePath)
                sessionDir
            }

            else -> {
                throw IllegalArgumentException("Invalid session ID: $session")
            }
        }
    }

    override fun getSessionName(
        user: User?,
        session: Session
    ): String {
        validateSessionId(session)
        val sessionDir = getSessionDir(user, session)
        val settings = getJson(sessionDir, "settings.json", Map::class.java) ?: mapOf<String,String>()
        if(settings.containsKey("name")) return settings["name"] as String
        val userMessage =
            messageFiles(sessionDir).entries.minByOrNull { it.key.lastModified() }?.value
        return if (null != userMessage) {
            setJson(sessionDir, "settings.json", settings.plus("name" to userMessage))
            //log.debug("Session {}: {}", session, userMessage)
            userMessage
        } else {
            //log.debug("Session {}: No messages", session)
            session.sessionId
        }
    }

    override fun getMessageIds(
        user: User?,
        session: Session
    ): List<String> {
        validateSessionId(session)
        val sessionDir = getSessionDir(user, session)
        val settings = getJson(sessionDir, "internal.json", Map::class.java) ?: mapOf<String,String>()
        if(settings.containsKey("ids")) return settings["ids"].toString().split(",").toList()
        val ids = messageFiles(sessionDir).entries.sortedBy { it.key.lastModified() }
            .map { it.key.nameWithoutExtension }.toList()
        setJson(sessionDir, "internal.json", settings.plus("ids" to ids.joinToString(",")))
        return ids
    }

    override fun setMessageIds(
        user: User?,
        session: Session,
        ids: List<String>
    ) {
        validateSessionId(session)
        val sessionDir = getSessionDir(user, session)
        val settings = getJson(sessionDir, "internal.json", Map::class.java) ?: mapOf<String, String>()
        setJson(sessionDir, "internal.json", settings.plus("ids" to ids.joinToString(",")))
    }

override fun getSessionTime(
        user: User?,
        session: Session
    ): Date? {
        validateSessionId(session)
        val sessionDir = getSessionDir(user, session)
        val settings = getJson(sessionDir, "internal.json", Map::class.java) ?: mapOf<String,String>()
        val dateFormat = SimpleDateFormat.getDateTimeInstance()
        if(settings.containsKey("time")) return dateFormat.parse(settings["time"] as String)
        val file = messageFiles(sessionDir).entries.minByOrNull { it.key.lastModified() }?.key
        return if (null != file) {
            val date = Date(file.lastModified())
            setJson(sessionDir, "internal.json", settings.plus("time" to dateFormat.format(date)))
            date
        } else {
            //log.debug("Session {}: No messages", session)
            null
        }
    }

    private fun messageFiles(sessionDir: File) = File(sessionDir, MESSAGE_DIR).listFiles()
        ?.filter { file -> file.isFile }
        ?.map { messageFile ->
            val fileText = messageFile.readText()
            val split = fileText.split("<p>")
            if (split.size < 2) {
                //log.debug("Session {}: No messages", session)
                messageFile to ""
            } else {
                val stringList = split[1].split("</p>")
                if (stringList.isEmpty()) {
                    //log.debug("Session {}: No messages", session)
                    messageFile to ""
                } else {
                    messageFile to stringList.first()
                }
            }
        }?.filter { it.second.isNotEmpty() }?.toList()?.toMap() ?: mapOf()

    override fun listSessions(
        user: User?
    ): List<Session> {
        val globalSessions = listSessions(dataDir)
        val userSessions = if (user == null) listOf() else listSessions(userRoot(user))
        return globalSessions.map { Session("G-$it") } + userSessions.map { Session("U-$it") }
    }

    override fun <T : Any> setJson(
        user: User?,
        session: Session,
        filename: String,
        settings: T
    ) = setJson(getSessionDir(user, session), filename, settings)

    private fun <T : Any> setJson(sessionDir: File, filename: String, settings: T): T {
        val settingsFile = File(sessionDir, filename)
        settingsFile.parentFile.mkdirs()
        JsonUtil.objectMapper().writeValue(settingsFile, settings)
        return settings
    }

    override fun updateMessage(
        user: User?,
        session: Session,
        messageId: String,
        value: String
    ) {
        validateSessionId(session)
        val file = File(File(this.getSessionDir(user, session), MESSAGE_DIR), "$messageId.json")
        if(!file.exists()) {
            file.parentFile.mkdirs()
            addMessageID(user, session, messageId)
        }
        //log.debug("Updating message for {} / {}: {}", session, messageId, file.absolutePath)
        JsonUtil.objectMapper().writeValue(file, value)
    }

    open protected fun addMessageID(
        user: User?,
        session: Session,
        messageId: String
    ) {
        synchronized(this) {
            setMessageIds(user, session, getMessageIds(user, session) + messageId)
        }
    }

    override fun listSessions(dir: File): List<String> {
        val files = dir.listFiles()?.flatMap { it.listFiles()?.toList() ?: listOf() }?.filter { sessionDir ->
            val operationDir = File(sessionDir, MESSAGE_DIR)
            if (!operationDir.exists()) false else {
                val listFiles = operationDir.listFiles()?.filter { it.isFile && !it.name.startsWith("aaa") }
                (listFiles?.size ?: 0) > 0
            }
        }?.sortedBy { it.lastModified() } ?: listOf()
        //log.debug("Sessions: {}", files.map { it.parentFile.name + "-" + it.name })
        return files.map { it.parentFile.name + "-" + it.name }
    }

    override fun userRoot(user: User?) = File(
        File(dataDir, "users"),
        if (user?.email != null) {
            user.email
        } else {
            throw IllegalArgumentException("User required for private session")
        }
    )

    override fun deleteSession(user: User?, session: Session) {
        validateSessionId(session)
        val sessionDir = getSessionDir(user, session)
        sessionDir.deleteRecursively()
    }

    companion object {

        private val log = org.slf4j.LoggerFactory.getLogger(StorageInterface::class.java)

        private val MESSAGE_DIR = ".sys" + File.separator + "messages"
        val sessionPaths = mutableMapOf<Session, File>()

    }
}

