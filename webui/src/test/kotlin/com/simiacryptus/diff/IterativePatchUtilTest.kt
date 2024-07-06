package com.simiacryptus.diff

import org.junit.jupiter.api.Assertions
import org.junit.jupiter.api.Test

class IterativePatchUtilTest {

    @Test
    fun testPatchExactMatch() {
        val source = """
            line1
            line2
            line3
        """.trimIndent()
        val patch = """
            line1
            line2
            line3
        """.trimIndent()
        val result = IterativePatchUtil.applyPatch(source, patch)
        Assertions.assertEquals(source.trim().replace("\r\n", "\n"), result.replace("\r\n", "\n"))
    }

    // ... (other existing tests)
    @Test
    fun testPatchAddLine() {
        val source = """
            line1
            line2
            line3
        """.trimIndent()
        val patch = """
            line1
            line2
            +newLine
            line3
        """.trimIndent()
        val expected = """
            line1
            line2
            newLine
            line3
        """.trimIndent()
        val result = IterativePatchUtil.applyPatch(source, patch)
        Assertions.assertEquals(expected.trim().replace("\r\n", "\n"), result.replace("\r\n", "\n"))
    }

    @Test
    fun testPatchModifyLine() {
        val source = """
            line1
            line2
            line3
        """.trimIndent()
        val patch = """
            line1
            -line2
            +modifiedLine2
            line3
        """.trimIndent()
        val expected = """
            line1
            modifiedLine2
            line3
        """.trimIndent()
        val result = IterativePatchUtil.applyPatch(source, patch)
        Assertions.assertEquals(expected.trim().replace("\r\n", "\n"), result.replace("\r\n", "\n"))
    }

    @Test
    fun testPatchRemoveLine() {
        val source = """
            line1
            line2
            line3
        """.trimIndent()
        val patch = """
            line1
          - line2
            line3
        """.trimIndent()
        val expected = """
            line1
              line3
        """.trimIndent()
        val result = IterativePatchUtil.applyPatch(source, patch)
        Assertions.assertEquals(expected.trim().replace("\r\n", "\n"), result.replace("\r\n", "\n"))
    }

    @Test
    fun testFromData1() {
        val source = """
        function updateTabs() {
            document.querySelectorAll('.tab-button').forEach(button => {
                button.addEventListener('click', (event) => { // Ensure the event is passed as a parameter
                    event.stopPropagation();
                    const forTab = button.getAttribute('data-for-tab');
                    let tabsParent = button.closest('.tabs-container');
                    tabsParent.querySelectorAll('.tab-content').forEach(content => {
                        const contentParent = content.closest('.tabs-container');
                        if (contentParent === tabsParent) {
                            if (content.getAttribute('data-tab') === forTab) {
                                content.classList.add('active');
                            } else if (content.classList.contains('active')) {
                                content.classList.remove('active')
                            }
                        }
                    });
                })
            });
        }
        """.trimIndent()
        val patch = """
        tabsParent.querySelectorAll('.tab-content').forEach(content => {
            const contentParent = content.closest('.tabs-container');
            if (contentParent === tabsParent) {
                if (content.getAttribute('data-tab') === forTab) {
                    content.classList.add('active');
        +           button.classList.add('active'); // Mark the button as active
                } else if (content.classList.contains('active')) {
                    content.classList.remove('active')
        +           button.classList.remove('active'); // Ensure the button is not marked as active
                }
            }
        });
        """.trimIndent()
        val expected = """
        function updateTabs() {
            document.querySelectorAll('.tab-button').forEach(button => {
                button.addEventListener('click', (event) => { // Ensure the event is passed as a parameter
                    event.stopPropagation();
                    const forTab = button.getAttribute('data-for-tab');
                    let tabsParent = button.closest('.tabs-container');
                    tabsParent.querySelectorAll('.tab-content').forEach(content => {
                        const contentParent = content.closest('.tabs-container');
                        if (contentParent === tabsParent) {
                            if (content.getAttribute('data-tab') === forTab) {
                                content.classList.add('active');
                                button.classList.add('active'); // Mark the button as active
                            } else if (content.classList.contains('active')) {
                                content.classList.remove('active')
                                button.classList.remove('active'); // Ensure the button is not marked as active
                            }
                        }
                    });
                })
            });
        }
        """.trimIndent()
        val result = IterativePatchUtil.applyPatch(source, patch)
        Assertions.assertEquals(
            expected.replace("\\s*\r?\n\\s*".toRegex(), "\n"),
            result.replace("\\s*\r?\n\\s*".toRegex(), "\n")
        )
    }



    @Test
    fun testGeneratePatchNoChanges() {
        val oldCode = """
            |line1
            |line2
            |line3
        """.trimMargin()
        val newCode = oldCode
        val result = IterativePatchUtil.generatePatch(oldCode, newCode)
        val expected = """
        """.trimMargin()
        Assertions.assertEquals(expected.trim().replace("\r\n", "\n"), result.trim().replace("\r\n", "\n"))
    }

    @Test
    fun testGeneratePatchAddLine() {
        val oldCode = """
            |line1
            |line2
            |line3
        """.trimMargin()
        val newCode = """
            |line1
            |line2
            |newLine
            |line3
        """.trimMargin()
        val result = IterativePatchUtil.generatePatch(oldCode, newCode)
        val expected = """
            |  line1
            |  line2
            |+ newLine
            |  line3
        """.trimMargin()
        Assertions.assertEquals(expected.trim().replace("\r\n", "\n"), result.trim().replace("\r\n", "\n"))
    }

    @Test
    fun testGeneratePatchRemoveLine() {
        val oldCode = """
            line1
            line2
            line3
        """.trimIndent()
        val newCode = """
            line1
            line3
        """.trimIndent()
        val result = IterativePatchUtil.generatePatch(oldCode, newCode)
        val expected = """
            |  line1
            |- line2
            |  line3
        """.trimMargin()
        Assertions.assertEquals(expected.trim().replace("\r\n", "\n"), result.trim().replace("\r\n", "\n"))
    }

    @Test
    fun testGeneratePatchModifyLine() {
        val oldCode = """
            line1
            line2
            line3
        """.trimIndent()
        val newCode = """
            line1
            modifiedLine2
            line3
        """.trimIndent()
        val result = IterativePatchUtil.generatePatch(oldCode, newCode)
        val expected = """
            |  line1
            |+ modifiedLine2
            |- line2
            |  line3
        """.trimMargin()
        Assertions.assertEquals(expected.trim().replace("\r\n", "\n"), result.trim().replace("\r\n", "\n"))
    }

    @Test
    fun testGeneratePatchComplexChanges() {
        val oldCode = """
            function example() {
                console.log("Hello");
                // Some comment
                return true;
            }
        """.trimIndent()
        val newCode = """
            function example() {
                console.log("Hello, World!");
                // Modified comment
                let x = 5;
                return x > 0;
            }
        """.trimIndent()
        val result = IterativePatchUtil.generatePatch(oldCode, newCode)
        val expected = """
            |  function example() {
            |+     console.log("Hello, World!");
            |+     // Modified comment
            |+     let x = 5;
            |+     return x > 0;
            |-     console.log("Hello");
            |-     // Some comment
            |-     return true;
            |  }            
        """.trimMargin()
        Assertions.assertEquals(expected.trim().replace("\r\n", "\n"), result.trim().replace("\r\n", "\n"))
    }
}
