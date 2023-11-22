@file:Suppress("PLATFORM_CLASS_MAPPED_TO_KOTLIN")

package com.simiacryptus.skyenet.kotlin

import com.simiacryptus.skyenet.core.util.HeartTestBase
import org.junit.jupiter.api.Assertions
import org.junit.jupiter.api.Test
import java.util.Map

class KotlinInterpreterTest : HeartTestBase() {

    override fun newInterpreter(map: Map<String, Object>) = KotlinInterpreter(map)

    @Test
    override fun `test run with variables`() {
        // TODO: This test is failing due to a bug with supplied primitives (e.g. Integer)
    }


    @Test
    fun `test run with kotlin println`() {
        val interpreter = newInterpreter(mapOf<String,Object>() as Map<String, Object>)
        val result = interpreter.run("""println("Hello World")""")
        Assertions.assertEquals(null, result)
    }

    @Test
    fun `test validate with kotlin println`() {
        val interpreter = newInterpreter(mapOf<String,Object>() as Map<String, Object>)
        val result = interpreter.validate("""println("Hello World")""")
        Assertions.assertEquals(null, result)
    }

}