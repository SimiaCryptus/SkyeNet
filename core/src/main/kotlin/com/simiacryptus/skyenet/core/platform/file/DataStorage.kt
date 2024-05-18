package com.simiacryptus.skyenet.core.platform.file

import com.simiacryptus.jopenai.util.JsonUtil
import com.simiacryptus.skyenet.core.platform.ApplicationServices
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

    override fun getMessages(
        user: User?,
        session: Session
    ): LinkedHashMap<String, String> {
        validateSessionId(session)
        log.debug("Fetching messages for session: ${session.sessionId}, user: ${user?.email}")
        val messageDir =
            ApplicationServices.dataStorageRoot.resolve("${if (session.isGlobal()) "global" else user}/$session/messages/")
                .apply { mkdirs() }
        val messages = LinkedHashMap<String, String>()
        getMessageIds(user, session).forEach { messageId ->
            val file = File(messageDir, "$messageId.json")
            if (file.exists()) {
                val message = JsonUtil.objectMapper().readValue(file, String::class.java)
                messages[messageId] = message
            }
        }
        log.debug("Loaded ${messages.size} messages for session: ${session.sessionId}")
        return messages
    }

    override fun getSessionDir(
        user: User?,
        session: Session
    ): File {
        if (sessionPaths.containsKey(session)) {
            return sessionPaths[session]!!
        }
        validateSessionId(session)
        log.debug("Getting session directory for session: ${session.sessionId}, user: ${user?.email}")
        val parts = session.sessionId.split("-")
        return when (parts.size) {
            3 -> {
                val root = when {
                    parts[0] == "G" -> dataDir
                    parts[0] == "U" -> userRoot(user)
                    else -> throw IllegalArgumentException("Invalid session ID: $session")
                }
                val dateDir = File(root, parts[1])
                val sessionDir = File(dateDir, parts[2])
                log.debug("Session directory for session: ${session.sessionId} is ${sessionDir.absolutePath}")
                sessionDir
            }

            2 -> {
                val dateDir = File(dataDir, parts[0])
                val sessionDir = File(dateDir, parts[1])
                log.debug("Session directory for session: ${session.sessionId} is ${sessionDir.absolutePath}")
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
        log.debug("Fetching session name for session: ${session.sessionId}, user: ${user?.email}")
        val sessionDir = getSessionDir(user, session)
        val settings = run {
            val settingsFile = File(sessionDir, "settings.json")
            if (!settingsFile.exists()) null else {
                JsonUtil.objectMapper().readValue(settingsFile, Map::class.java) as Map<*, *>
            }
        } ?: mapOf<String, String>()
        if (settings.containsKey("name")) return settings["name"] as String
        val userMessage =
            messageFiles(session, user).entries.minByOrNull { it.key.lastModified() }?.value
        return if (null != userMessage) {
            setJson(sessionDir, "settings.json", settings.plus("name" to userMessage))
            log.debug("Session name for session: ${session.sessionId} is $userMessage")
            userMessage
        } else {
            log.debug("Session ${session.sessionId} has no messages")
            session.sessionId
        }
    }

    override fun getMessageIds(
        user: User?,
        session: Session
    ): List<String> {
        validateSessionId(session)
        log.debug("Fetching message IDs for session: ${session.sessionId}, user: ${user?.email}")
        val sessionDir = getSessionDir(user, session)
        val settings = run {
            val settingsFile = File(
                ApplicationServices.dataStorageRoot,
                "${if (session.isGlobal()) "global" else user}/$session/internal.json"
            )
            if (!settingsFile.exists()) null else {
                JsonUtil.objectMapper().readValue(settingsFile, Map::class.java) as Map<*, *>
            }
        } ?: mapOf<String, String>()
        if (settings.containsKey("ids")) return settings["ids"].toString().split(",").toList()
        val ids = messageFiles(session, user).entries.sortedBy { it.key.lastModified() }
            .map { it.key.nameWithoutExtension }.toList()
        setJson(
            ApplicationServices.dataStorageRoot,
            "${if (session.isGlobal()) "global" else user}/$session/internal.json",
            settings.plus("ids" to ids.joinToString(","))
        )
        log.debug("Message IDs for session: ${session.sessionId} are $ids")
        return ids
    }

    override fun setMessageIds(
        user: User?,
        session: Session,
        ids: List<String>
    ) {
        validateSessionId(session)
        log.debug("Setting message IDs for session: ${session.sessionId}, user: ${user?.email} to $ids")
        val settings = run {
            val settingsFile = File(
                ApplicationServices.dataStorageRoot,
                "${if (session.isGlobal()) "global" else user}/$session/internal.json"
            )
            if (!settingsFile.exists()) null else {
                JsonUtil.objectMapper().readValue(settingsFile, Map::class.java) as Map<*, *>
            }
        } ?: mapOf<String, String>()
        setJson(
            ApplicationServices.dataStorageRoot,
            "${if (session.isGlobal()) "global" else user}/$session/internal.json",
            settings.plus("ids" to ids.joinToString(","))
        )
    }

    override fun getSessionTime(
        user: User?,
        session: Session
    ): Date? {
        validateSessionId(session)
        log.debug("Fetching session time for session: ${session.sessionId}, user: ${user?.email}")
        val settingsFile =
            ApplicationServices.dataStorageRoot.resolve("${if (session.isGlobal()) "global" else user}/$session/internal.json")
        val settings = run {
            if (!settingsFile.exists()) null else {
                JsonUtil.objectMapper().readValue(settingsFile, Map::class.java) as Map<*, *>
            }
        } ?: mapOf<String, String>()
        val dateFormat = SimpleDateFormat.getDateTimeInstance()
        if (settings.containsKey("time")) return dateFormat.parse(settings["time"] as String)
        val messageFiles = messageFiles(session, user)
        val file = messageFiles.entries.minByOrNull { it.key.lastModified() }?.key
        return if (null != file) {
            val date = Date(file.lastModified())
            setJson(
                ApplicationServices.dataStorageRoot,
                "${if (session.isGlobal()) "global" else user}/$session/internal.json",
                settings.plus("time" to dateFormat.format(date))
            )
            log.debug("Session time for session: ${session.sessionId} is $date")
            date
        } else {
            log.debug("Session ${session.sessionId} has no messages")
            null
        }
    }

    private fun messageFiles(
        session: Session,
        user: User?,
    ) =
        ApplicationServices.dataStorageRoot.resolve("${if (session.isGlobal()) "global" else user}/$session/messages")
            .apply { mkdirs() }.listFiles()
            ?.filter { file -> file.isFile }
            ?.map { messageFile ->
                val fileText = messageFile.readText()
                val split = fileText.split("<p>")
                if (split.size < 2) {
                    log.debug("Session ${session.sessionId} has no messages in file ${messageFile.name}")
                    messageFile to ""
                } else {
                    val stringList = split[1].split("</p>")
                    if (stringList.isEmpty()) {
                        log.debug("Session ${session.sessionId} has no messages in file ${messageFile.name}")
                        messageFile to ""
                    } else {
                        messageFile to stringList.first()
                    }
                }
            }?.filter { it.second.isNotEmpty() }?.toList()?.toMap() ?: mapOf()

    override fun listSessions(
        user: User?
    ): List<Session> {
        log.debug("Listing sessions for user: ${user?.email}")
        val globalSessions = listSessions(dataDir)
        val userSessions = if (user == null) listOf() else listSessions(userRoot(user))
        log.debug("Found ${globalSessions.size} global sessions and ${userSessions.size} user sessions for user: ${user?.email}")
        return ((globalSessions.map {
            try {
                Session("G-$it")
            } catch (e: Exception) {
                null
            }
        }).toList() + (userSessions.map {
            try {
                Session("U-$it")
            } catch (e: Exception) {
                null
            }
        }).toList()).filterNotNull()
    }

    override fun <T : Any> setJson(
        user: User?,
        session: Session,
        filename: String,
        settings: T
    ) = setJson(getSessionDir(user, session), filename, settings)

    private fun <T : Any> setJson(sessionDir: File, filename: String, settings: T): T {
        log.debug("Setting JSON for session directory: ${sessionDir.absolutePath}, filename: $filename")
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
        log.debug("Updating message for session: ${session.sessionId}, messageId: $messageId, user: ${user?.email}")
        val file =
            ApplicationServices.dataStorageRoot.resolve("${if (session.isGlobal()) "global" else user}/$session/messages/$messageId.json")
                .apply { parentFile.mkdirs() }
        if (!file.exists()) {
            file.parentFile.mkdirs()
            addMessageID(user, session, messageId)
        }
        JsonUtil.objectMapper().writeValue(file, value)
    }

    protected open fun addMessageID(
        user: User?,
        session: Session,
        messageId: String
    ) {
        synchronized(this) {
            log.debug("Adding message ID for session: ${session.sessionId}, messageId: $messageId, user: ${user?.email}")
            setMessageIds(user, session, getMessageIds(user, session) + messageId)
        }
    }

    override fun listSessions(dir: File): List<String> {
        log.debug("Listing sessions in directory: ${dir.absolutePath}")
        val files = dir.listFiles()?.flatMap { it.listFiles()?.toList() ?: listOf() }?.filter { sessionDir ->
            val operationDir = File(sessionDir, "messages")
            if (!operationDir.exists()) false else {
                val listFiles = operationDir.listFiles()?.flatMap {
                    when {
                        it.isDirectory -> it.listFiles()?.toList() ?: listOf()
                        else -> listOf(it)
                    }
                }?.filter { it.isFile && !it.name.startsWith("aaa") }
                (listFiles?.size ?: 0) > 0
            }
        }?.sortedBy { it.lastModified() } ?: listOf()
        log.debug("Found ${files.size} sessions in directory: ${dir.absolutePath}")
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
        log.debug("Deleting session: ${session.sessionId}, user: ${user?.email}")
        val sessionDir = getSessionDir(user, session)
        sessionDir.deleteRecursively()
    }

    companion object {

        val log = org.slf4j.LoggerFactory.getLogger(DataStorage::class.java)
        val sessionPaths = mutableMapOf<Session, File>()

    }
}