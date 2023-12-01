package com.simiacryptus.skyenet.core

import com.simiacryptus.skyenet.core.platform.file.DataStorage
import com.simiacryptus.skyenet.core.platform.StorageInterface
import org.junit.jupiter.api.AfterEach
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Assertions.assertTrue
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test
import java.io.File
import java.nio.file.Files

class DataStorageTest {
    private lateinit var tempDir: File
    private lateinit var storage: StorageInterface

    @BeforeEach
    fun setUp() {
        tempDir = Files.createTempDirectory("sessionDataTest").toFile()
        storage = DataStorage(dataDir = tempDir)
    }

    @AfterEach
    fun tearDown() {
        tempDir.deleteRecursively()
    }

    @Test
    fun testUpdateAndLoadMessage() {
        val sessionId = StorageInterface.newGlobalID()
        val messageId = "message1"
        val message = "This is a test message."

        storage.updateMessage(null, sessionId, messageId, message)
        val messages = storage.getMessages(null, sessionId)

        assertEquals(1, messages.size)
        assertTrue(messages.containsKey(messageId))
        assertEquals(message, messages[messageId])
    }
}
