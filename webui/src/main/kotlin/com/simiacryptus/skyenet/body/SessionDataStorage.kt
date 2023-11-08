package com.simiacryptus.skyenet.body

import com.simiacryptus.util.JsonUtil
import java.io.File

open class SessionDataStorage(
    val dataDir: File = File("sessionData")
) {

    init {
        if (!dataDir.exists()) {
            dataDir.mkdirs()
        }
    }

    open fun updateOperationStatus(sessionId: String, operationId: String, value: OperationStatus) {
        val operationDir = getOperationDir(sessionId)
        val operationFile = File(operationDir, "$operationId.json")
        JsonUtil.objectMapper().writeValue(operationFile, value)
    }

    open fun updateMessage(sessionId: String, messageId: String, value: String) {
        val messageDir = getMessageDir(sessionId)
        val messageFile = File(messageDir, "$messageId.json")

        JsonUtil.objectMapper().writeValue(messageFile, value)
    }

    open fun loadOperations(sessionId: String): MutableMap<String, OperationStatus> {
        val operationDir = getOperationDir(sessionId)
        val operations = mutableMapOf<String, OperationStatus>()

        operationDir.listFiles()?.forEach { file ->
            val operation = JsonUtil.objectMapper().readValue(file, OperationStatus::class.java)
            operations[file.nameWithoutExtension] = operation
        }

        return operations
    }

    open fun loadMessages(sessionId: String): LinkedHashMap<String, String> {
        val messageDir = getMessageDir(sessionId)
        val messages = LinkedHashMap<String, String>()

        messageDir.listFiles()?.forEach { file ->
            val message = JsonUtil.objectMapper().readValue(file, String::class.java)
            messages[file.nameWithoutExtension] = message
        }

        return messages
    }

    protected open fun getOperationDir(sessionId: String): File {
        val sessionDir = getSessionInstanceDir(sessionId)
        val operationDir = File(sessionDir, "operations")
        if (!operationDir.exists()) {
            operationDir.mkdirs()
        }
        return operationDir
    }

    protected open fun getMessageDir(sessionId: String): File {
        val sessionDir = getSessionInstanceDir(sessionId)
        val messageDir = File(sessionDir, "messages")
        if (!messageDir.exists()) {
            messageDir.mkdirs()
        }
        return messageDir
    }

    open fun listSessions(): List<String> {
        // For all sessions, return the session id
        // Filter out sessions which have no operations
        val files = dataDir.listFiles()?.flatMap { it.listFiles().toList() }?.filter { sessionDir ->
            val operationDir = File(sessionDir, "messages")
            if (!operationDir.exists()) false else {
                val listFiles = operationDir.listFiles()
                (listFiles?.size ?: 0) > 2
            }
        }
        return files?.map { it.parentFile.name + "-" + it.name } ?: listOf()
    }

    open fun getSessionName(sessionId: String): String {
        val sessionDir = getSessionInstanceDir(sessionId)
        // Find the earliest operation and return the operation instruction
        val operationDir = File(sessionDir, "operations")
        val operationFiles = operationDir.listFiles()?.filter { file ->
            file.isFile
        }?.sortedBy { file ->
            JsonUtil.objectMapper().readValue(file, OperationStatus::class.java).created
        } ?: listOf()
        if (operationFiles.isNotEmpty()) {
            val operationFile = operationFiles.first()
            val operationStatus = JsonUtil.objectMapper().readValue(operationFile, OperationStatus::class.java)
            return operationStatus.instruction
        } else {
            return sessionId
        }
    }

    protected open fun getSessionInstanceDir(sessionId: String): File {
        val sessionDir = File(getSessionGroupDir(sessionId), getSessionInstanceId(sessionId))
        if (!sessionDir.exists()) {
            sessionDir.mkdirs()
        }
        return sessionDir
    }

    open fun getSessionDir(sessionId: String) = getSessionGroupDir(sessionId)

    protected open fun getSessionGroupDir(sessionId: String): File {
        val sessionGroupDir = File(dataDir, getSessionGroupId(sessionId))
        if (!sessionGroupDir.exists()) {
            sessionGroupDir.mkdirs()
        }
        return sessionGroupDir
    }

    protected open fun getSessionGroupId(sessionId: String): String {
        return sessionId.split("-").firstOrNull() ?: sessionId
    }

    protected open fun getSessionInstanceId(sessionId: String): String {
        return stripPrefix(stripPrefix(sessionId, getSessionGroupId(sessionId)), "-")
    }


    companion object {
        fun stripPrefix(text: String, prefix: String): String {
            val startsWith = text.toString().startsWith(prefix.toString())
            return if (startsWith) {
                text.toString().substring(prefix.length)
            } else {
                text.toString()
            }
        }
    }
}
