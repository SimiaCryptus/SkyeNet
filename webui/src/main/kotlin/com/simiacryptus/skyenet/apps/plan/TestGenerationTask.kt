package com.simiacryptus.skyenet.apps.plan

import org.slf4j.LoggerFactory

class TestGenerationTask(
    settings: Settings,
    planTask: PlanTask
) : AbstractAnalysisTask(settings, planTask) {
    override val actorName: String = "TestGeneration"
    override val actorPrompt: String = """
                |Generate comprehensive unit tests for the provided code files. The tests should:
                |1. Cover all public methods and functions
                |2. Include both positive and negative test cases
                |3. Test edge cases and boundary conditions
                |4. Ensure high code coverage
                |5. Follow best practices for unit testing in the given programming language
                |
                |Provide the generated tests as complete, runnable code files.
                |Use appropriate testing frameworks and assertion libraries for the target language.
                |Include setup and teardown methods if necessary.
                |Add comments explaining the purpose of each test case.
                |
                |Response format:
                |- Use ${TRIPLE_TILDE} code blocks with a header specifying the new test file path.
                |- Specify the language for syntax highlighting after the opening triple backticks.
                |- Separate code blocks with a single blank line.
                |
                |Example:
                |
                |Here are the generated unit tests:
                |                
                |### src/test/java/com/example/UtilsTest.java
                |${TRIPLE_TILDE}java
                |import org.junit.jupiter.api.Test;
                |import static org.junit.jupiter.api.Assertions.*;
                |                
                |public class UtilsTest {
                |    @Test
                |    public void testExampleFunction() {
                |        // Test the happy path
                |        assertEquals(3, Utils.exampleFunction(1, 2));
                |        
                |        // Test edge cases
                |        assertEquals(0, Utils.exampleFunction(0, 0));
                |        assertEquals(-1, Utils.exampleFunction(-1, 0));
                |        
                |        // Test error conditions
                |        assertThrows(IllegalArgumentException.class, () -> {
                |            Utils.exampleFunction(Integer.MAX_VALUE, 1);
                |        });
                |    }
                |}
 ${TRIPLE_TILDE}
    """.trimMargin()

    override fun getAnalysisInstruction(): String = "Generate tests for the following code"

    override fun promptSegment(): String {
        return """
            |TestGeneration - Generate unit tests for the specified code files
            |  ** Specify the files for which tests should be generated
            |  ** List input files/tasks to be examined when generating tests
        """.trimMargin()
    }

    companion object {
        private val log = LoggerFactory.getLogger(TestGenerationTask::class.java)
    }
}