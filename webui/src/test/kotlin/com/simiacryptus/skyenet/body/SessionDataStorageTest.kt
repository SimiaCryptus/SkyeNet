package com.simiacryptus.skyenet.body

import org.junit.jupiter.api.AfterEach
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Assertions.assertTrue
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test
import java.io.File
import java.nio.file.Files

class SessionDataStorageTest {
    private lateinit var tempDir: File
    private lateinit var storage: SessionDataStorage

    @BeforeEach
    fun setUp() {
        tempDir = Files.createTempDirectory("sessionDataTest").toFile()
        storage = SessionDataStorage(dataDir = tempDir)
    }

    @AfterEach
    fun tearDown() {
        tempDir.deleteRecursively()
    }

    @Test
    fun testUpdateAndLoadOperationStatus() {
        val sessionId = "session1"
        val operationId = "operation1"
        val operationStatus = OperationStatus(
            status = OperationStatus.OperationState.Running
        )

        storage.updateOperationStatus(sessionId, operationId, operationStatus)
        val operations = storage.loadOperations(sessionId)

        assertEquals(1, operations.size)
        assertTrue(operations.containsKey(operationId))
        assertEquals(operationStatus, operations[operationId])
    }

    @Test
    fun testUpdateAndLoadMessage() {
        val sessionId = "session1"
        val messageId = "message1"
        val message = "This is a test message."

        storage.updateMessage(sessionId, messageId, message)
        val messages = storage.loadMessages(sessionId)

        assertEquals(1, messages.size)
        assertTrue(messages.containsKey(messageId))
        assertEquals(message, messages[messageId])
    }
}
