package com.simiacryptus.skyenet.heart

import org.junit.jupiter.api.Assertions.*
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Disabled
import org.junit.jupiter.api.Test

class KotlinDaemonInterpreterTest {

    private var interpreter : KotlinDaemonInterpreter? = null

    @BeforeEach
    fun setUp() {
        interpreter = KotlinDaemonInterpreter()
    }

    @Test
    fun getLanguage() {
        assertEquals("kotlin", interpreter?.getLanguage(), "The method should return 'kotlin' as the language")
    }

    @Test
    @Disabled
    fun run() {
        val code = "3 + 5"
        val result = interpreter?.run(code)
        assertNotNull(result, "Result of the script execution should not be null")
        assertEquals(8, result, "Execution of code '3 + 5' should return 8")
    }

    @Test
    @Disabled
    fun runWithInvalidCode() {
        val code = "3 + "
        val exception = assertThrows(Exception::class.java) { interpreter?.run(code) }
        assertTrue(exception.message!!.contains("unexpected EOF"), "Invalid code should throw an exception")
    }

    @Test
    @Disabled
    fun validateWithValidCode() {
        val code = "3 + 5"
        assertNull(interpreter?.validate(code), "Valid code should not return an exception")
    }

    @Test
    fun validateWithInvalidCode() {
        val code = "3 + "
        assertNotNull(interpreter?.validate(code), "Invalid code should return an exception")
    }
}