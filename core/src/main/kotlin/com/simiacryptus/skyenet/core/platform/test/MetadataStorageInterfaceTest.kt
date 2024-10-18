package com.simiacryptus.skyenet.core.platform.test

import com.simiacryptus.skyenet.core.platform.Session
import com.simiacryptus.skyenet.core.platform.model.MetadataStorageInterface
import com.simiacryptus.skyenet.core.platform.model.User
import org.junit.jupiter.api.Assertions.*
import org.junit.jupiter.api.Test
import java.util.*

abstract class MetadataStorageInterfaceTest(val storage: MetadataStorageInterface) {

    @Test
    fun testGetSessionName() {
        // Arrange
        val user = User(email = "test@example.com")
        val session = Session("G-20230101-1234")

        // Act
        val sessionName = storage.getSessionName(user, session)

        // Assert
        assertNotNull(sessionName)
        assertTrue(sessionName is String)
    }

    @Test
    fun testSetSessionName() {
        // Arrange
        val user = User(email = "test@example.com")
        val session = Session("G-20230101-1234")
        val newName = "Test Session"

        // Act
        storage.setSessionName(user, session, newName)
        val retrievedName = storage.getSessionName(user, session)

        // Assert
        assertEquals(newName, retrievedName)
    }

    @Test
    fun testGetMessageIds() {
        // Arrange
        val user = User(email = "test@example.com")
        val session = Session("G-20230101-1234")

        // Act
        val messageIds = storage.getMessageIds(user, session)

        // Assert
        assertNotNull(messageIds)
        assertTrue(messageIds is List<*>)
    }

    @Test
    fun testSetMessageIds() {
        // Arrange
        val user = User(email = "test@example.com")
        val session = Session("G-20230101-1234")
        val newIds = listOf("msg001", "msg002", "msg003")

        // Act
        storage.setMessageIds(user, session, newIds)
        val retrievedIds = storage.getMessageIds(user, session)

        // Assert
        assertEquals(newIds, retrievedIds)
    }

    @Test
    fun testGetSessionTime() {
        // Arrange
        val user = User(email = "test@example.com")
        val session = Session("G-20230101-1234")

        // Act
        val sessionTime = storage.getSessionTime(user, session)

        // Assert
        assertNotNull(sessionTime)
        assertTrue(sessionTime is Date)
    }

    @Test
    fun testSetSessionTime() {
        // Arrange
        val user = User(email = "test@example.com")
        val session = Session("G-20230101-1234")
        val newTime = Date()

        // Act
        storage.setSessionTime(user, session, newTime)
        val retrievedTime = storage.getSessionTime(user, session)

        // Assert
        assertEquals(newTime.toString(), retrievedTime.toString())
    }

    @Test
    fun testListSessions() {
        // Arrange
        val path = ""

        // Act
        val sessions = storage.listSessions(path)

        // Assert
        assertNotNull(sessions)
        assertTrue(sessions is List<*>)
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
            fail("Exception should not be thrown")
        }
    }
}