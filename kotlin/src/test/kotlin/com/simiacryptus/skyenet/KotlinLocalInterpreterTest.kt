package com.simiacryptus.skyenet.heart

import org.junit.jupiter.api.Assertions.*
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Disabled
import org.junit.jupiter.api.Test
import java.util.UUID
import javax.script.ScriptException

class KotlinLocalInterpreterTest {
    private lateinit var interpreter: KotlinLocalInterpreter

    @BeforeEach
    fun setUp() {
        interpreter = KotlinLocalInterpreter()
    }

    @Test
    @Disabled
    fun testRun_validCode() {
        val result = interpreter.run("val x = 5\nx * 2")
        assertEquals(10, result)
    }

    @Test
    fun testRun_invalidCode() {
        assertThrows(ScriptException::class.java) {
            interpreter.run("val x = 5\ny * 2")
        }
    }

    @Test
    fun testGetLanguage() {
        assertEquals("Kotlin", interpreter.getLanguage())
    }

    @Test
    @Disabled
    fun testValidate_validCode() {
        assertNull(interpreter.validate("val x = 5\nx * 2"))
    }

    @Test
    fun testValidate_invalidCode() {
        assertNotNull(interpreter.validate("val x = 5\ny * 2"))
    }

    @Test
    fun testTypeOf() {
        val obj = this
        val typeName = interpreter.typeOf(obj as Object)
        assertEquals(obj.javaClass.name.replace("$", "."), typeName)
    }
}
