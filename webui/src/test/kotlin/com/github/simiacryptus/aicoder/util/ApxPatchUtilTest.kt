package com.github.simiacryptus.aicoder.util

import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Test

internal class ApxPatchUtilTest {

    @Test
    fun `test patch with simple addition`() {
        val source = "Line 1\nLine 2\nLine 3"
        val patch = """
            +++ 
            @@ -1,3 +1,4 @@
            +Added Line
            Line 1
            Line 2
            Line 3
        """.trimIndent()
        val expected = "Added Line\nLine 1\nLine 2\nLine 3"
        val result = ApxPatchUtil.patch(source, patch)
        assertEquals(expected, result)
    }

    @Test
    fun `test patch with deletion`() {
        val source = "Line 1\nLine 2\nLine 3"
        val patch = """
            --- 
            @@ -1,3 +1,2 @@
            -Line 2
            Line 1
            Line 3
        """.trimIndent()
        val expected = "Line 1\nLine 3"
        val result = ApxPatchUtil.patch(source, patch)
        assertEquals(expected, result)
    }
}