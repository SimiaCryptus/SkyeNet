package com.simiacryptus.skyenet

import org.junit.jupiter.api.Assertions
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.assertThrows
import java.util.Map

abstract class HeartTestBase {

    @Test
    fun `test run with valid code`() {
        val interpreter = newInterpreter(mapOf<String,Object>() as Map<String,Object>)
        val result = interpreter.run("2 + 2")
        Assertions.assertEquals(4, result)
    }

    @Test
    fun `test run with invalid code`() {
        val interpreter = newInterpreter(mapOf<String,Object>() as Map<String,Object>)
        assertThrows<Exception> { interpreter.run("2 +") }
    }

    @Test
    fun `test validate with valid code`() {
        val interpreter = newInterpreter(mapOf<String,Object>() as Map<String,Object>)
        val result = interpreter.validate("2 + 2")
        Assertions.assertEquals(null, result)
    }

    @Test
    fun `test validate with invalid code`() {
        val interpreter = newInterpreter(mapOf<String,Object>() as Map<String,Object>)
        val result = interpreter.validate("2 +")
        Assertions.assertTrue(result is Exception)
    }

    @Test
    fun `test run with variables`() {
        val interpreter = newInterpreter(mapOf("x" to (2 as Object), "y" to (3 as Object)) as Map<String,Object>)
        val result = interpreter.run("x * y")
        Assertions.assertEquals(6, result)
    }

    @Test
    fun `test validate with variables`() {
        val interpreter = newInterpreter(mapOf("x" to (2 as Object), "y" to (3 as Object)) as Map<String,Object>)
        val result = interpreter.validate("x * y")
        Assertions.assertEquals(null, result)
    }

    class FooBar {
        fun bar(): String {
            return "Foo says Hello World"
        }
    }

    @Test
    fun `test run with tool object`() {
        val interpreter = newInterpreter(mapOf("tool" to (FooBar() as Object)) as Map<String,Object>)
        val result = interpreter.run("tool.bar()")
        Assertions.assertEquals("Foo says Hello World", result)
    }

    @Test
    fun `test validate with tool object`() {
        val interpreter = newInterpreter(mapOf("tool" to (FooBar() as Object)) as Map<String,Object>)
        val result = interpreter.validate("tool.bar()")
        Assertions.assertEquals(null, result)
    }


    @Test
    fun `test run with tool object and invalid code`() {
        val interpreter = newInterpreter(mapOf("tool" to (FooBar() as Object)) as Map<String,Object>)
        assertThrows<Exception> { interpreter.run("tool.baz()") }
    }

    @Test
    fun `test validate with tool object and invalid code`() {
        val interpreter = newInterpreter(mapOf("tool" to (FooBar() as Object)) as Map<String,Object>)
        assertThrows<Exception> { interpreter.validate("tool.baz()") }
    }

    @Test
    fun `test validate with undefined variable`() {
        val interpreter = newInterpreter(mapOf<String,Object>() as Map<String,Object>)
        val result = interpreter.validate("x * y")
        Assertions.assertTrue(result is Exception, "Expected Exception but got $result")
    }

    abstract fun newInterpreter(map: Map<String, Object>): Heart
}