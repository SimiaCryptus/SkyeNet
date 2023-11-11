package com.simiacryptus.skyenet

import com.simiacryptus.skyenet.webui.SessionDataStorage
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
