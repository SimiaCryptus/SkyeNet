package com.simiacryptus.skyenet.core.platform.test

import com.simiacryptus.jopenai.util.JsonUtil
import com.simiacryptus.skyenet.core.platform.Session
import com.simiacryptus.skyenet.core.platform.StorageInterface
import com.simiacryptus.skyenet.core.platform.User
import org.junit.jupiter.api.Assertions
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Assertions.assertTrue
import org.junit.jupiter.api.Test
import java.io.File
import java.util.*

abstract class StorageInterfaceTest(val storage: StorageInterface) {


    @Test
    fun testGetJson() {
        // Arrange
        val user = User(email = "test@example.com")
        val session = Session("G-20230101-1234")
        val filename = "test.json"

        // Act
        val settingsFile = File(storage.getSessionDir(user, session), filename)
        val result = if (!settingsFile.exists()) null else {
            JsonUtil.objectMapper().readValue(settingsFile, Any::class.java) as Any
        }

        // Assert
        Assertions.assertNull(result, "Expected null result for non-existing JSON file")
    }

    @Test
    fun testGetMessages() {
        // Arrange
        val user = User(email = "test@example.com")
        val session = Session("G-20230101-1234")

        // Act
        val messages = storage.getMessages(user, session)

        // Assert
        assertTrue(messages is LinkedHashMap<*, *>, "Expected LinkedHashMap type for messages")
    }

    @Test
    fun testGetSessionDir() {
        // Arrange
        val user = User(email = "test@example.com")
        val session = Session("G-20230101-1234")

        // Act
        val sessionDir = storage.getSessionDir(user, session)

        // Assert
        assertTrue(sessionDir is File, "Expected File type for session directory")
    }

    @Test
    fun testGetSessionName() {
        // Arrange
        val user = User(email = "test@example.com")
        val session = Session("G-20230101-1234")

        // Act
        val sessionName = storage.getSessionName(user, session)

        // Assert
        Assertions.assertNotNull(sessionName)
        assertTrue(sessionName is String)
    }

    @Test
    fun testGetSessionTime() {
        // Arrange
        val user = User(email = "test@example.com")
        val session = Session("G-20230101-1234")
        storage.updateMessage(user, session, "msg001", "<p>Hello, World!</p><p>Hello, World!</p>")

        // Act
        val sessionTime = storage.getSessionTime(user, session)

        // Assert
        Assertions.assertNotNull(sessionTime)
        assertTrue(sessionTime is Date)
    }

    @Test
    fun testListSessions() {
        // Arrange
        val user = User(email = "test@example.com")

        // Act
        val sessions = storage.listSessions(user, "",)

        // Assert
        Assertions.assertNotNull(sessions)
        assertTrue(sessions is List<*>)
    }

    @Test
    fun testSetJson() {
        // Arrange
        val user = User(email = "test@example.com")
        val session = Session("G-20230101-1234")
        val filename = "settings.json"
        val settings = mapOf("theme" to "dark")

        // Act
        val result = storage.setJson(user, session, filename, settings)

        // Assert
        Assertions.assertNotNull(result)
        assertEquals(settings, result)
    }

    @Test
    fun testUpdateMessage() {
        // Arrange
        val user = User(email = "test@example.com")
        val session = Session("G-20230101-1234")
        val messageId = "msg001"
        val value = "Hello, World!"

        // Act and Assert
        try {
            storage.updateMessage(user, session, messageId, value)
            // If no exception is thrown, the test passes.
        } catch (e: Exception) {
            Assertions.fail("Exception should not be thrown")
        }
    }

    @Test
    fun testListSessionsWithDir() {
        // Arrange
        val directory = File(System.getProperty("user.dir")) // Example directory

        // Act
        val sessionList = storage.listSessions(directory, "")

        // Assert
        Assertions.assertNotNull(sessionList)
        assertTrue(sessionList is List<*>)
    }

    @Test
    fun testUserRoot() {
        // Arrange
        val user = User(email = "test@example.com")

        // Act
        val userRoot = storage.userRoot(user)

        // Assert
        Assertions.assertNotNull(userRoot)
        assertTrue(userRoot is File)
    }

    @Test
    fun testDeleteSession() {
        // Arrange
        val user = User(email = "test@example.com")
        val session = Session("G-20230101-1234")

        // Act and Assert
        try {
            storage.deleteSession(user, session)
            // If no exception is thrown, the test passes.
        } catch (e: Exception) {
            Assertions.fail("Exception should not be thrown")
        }
    }
    // Continue writing tests for each method in StorageInterface...
    // ...
}
