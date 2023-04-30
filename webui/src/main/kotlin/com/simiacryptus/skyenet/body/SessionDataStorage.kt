package com.simiacryptus.skyenet.body

import com.fasterxml.jackson.databind.ObjectMapper
import com.fasterxml.jackson.module.kotlin.KotlinFeature
import com.fasterxml.jackson.module.kotlin.KotlinModule
import java.io.File

class SessionDataStorage(
    val dataDir: File = File("sessionData")
) {
    private val objectMapper = ObjectMapper().registerModule(
        KotlinModule.Builder()
            .withReflectionCacheSize(512)
            .configure(KotlinFeature.NullToEmptyCollection, false)
            .configure(KotlinFeature.NullToEmptyMap, false)
            .configure(KotlinFeature.NullIsSameAsDefault, false)
            .configure(KotlinFeature.SingletonSupport, false)
            .configure(KotlinFeature.StrictNullChecks, false)
            .build()
    )

    init {
        if (!dataDir.exists()) {
            dataDir.mkdirs()
        }
    }

    fun updateOperationStatus(sessionId: String, operationId: String, value: OperationStatus) {
        val operationDir = getOperationDir(sessionId)
        val operationFile = File(operationDir, "$operationId.json")

        objectMapper.writeValue(operationFile, value)
    }

    fun updateMessage(sessionId: String, messageId: String, value: String) {
        val messageDir = getMessageDir(sessionId)
        val messageFile = File(messageDir, "$messageId.json")

        objectMapper.writeValue(messageFile, value)
    }

    fun loadOperations(sessionId: String): MutableMap<String, OperationStatus> {
        val operationDir = getOperationDir(sessionId)
        val operations = mutableMapOf<String, OperationStatus>()

        operationDir.listFiles()?.forEach { file ->
            val operation = objectMapper.readValue(file, OperationStatus::class.java)
            operations[file.nameWithoutExtension] = operation
        }

        return operations
    }

    fun loadMessages(sessionId: String): LinkedHashMap<String, String> {
        val messageDir = getMessageDir(sessionId)
        val messages = LinkedHashMap<String, String>()

        messageDir.listFiles()?.forEach { file ->
            val message = objectMapper.readValue(file, String::class.java)
            messages[file.nameWithoutExtension] = message
        }

        return messages
    }

    private fun getSessionDir(sessionId: String): File {
        val sessionDir = File(dataDir, sessionId)
        if (!sessionDir.exists()) {
            sessionDir.mkdirs()
        }
        return sessionDir
    }

    private fun getOperationDir(sessionId: String): File {
        val sessionDir = getSessionDir(sessionId)
        val operationDir = File(sessionDir, "operations")
        if (!operationDir.exists()) {
            operationDir.mkdirs()
        }
        return operationDir
    }

    private fun getMessageDir(sessionId: String): File {
        val sessionDir = getSessionDir(sessionId)
        val messageDir = File(sessionDir, "messages")
        if (!messageDir.exists()) {
            messageDir.mkdirs()
        }
        return messageDir
    }

    fun listSessions(): List<String> {
        // For all sessions, return the session id
        // Filter out sessions which have no operations
        return dataDir.listFiles()?.filter { sessionDir ->
            val operationDir = File(sessionDir, "operations")
            operationDir.exists() && operationDir.listFiles()?.isNotEmpty() ?: false
        }?.map { sessionDir ->
            sessionDir.name
        } ?: listOf()
    }

    fun getSessionName(sessionId: String): String {
        val sessionDir = getSessionDir(sessionId)
        // Find the earliest operation and return the operation instruction
        val operationDir = File(sessionDir, "operations")
        val operationFiles = operationDir.listFiles()?.filter { file ->
            file.isFile
        }?.sortedBy { file ->
            objectMapper.readValue(file, OperationStatus::class.java).created
        } ?: listOf()
        if (operationFiles.isNotEmpty()) {
            val operationFile = operationFiles.first()
            val operationStatus = objectMapper.readValue(operationFile, OperationStatus::class.java)
            return operationStatus.instruction
        } else {
            return sessionId
        }
    }
}
