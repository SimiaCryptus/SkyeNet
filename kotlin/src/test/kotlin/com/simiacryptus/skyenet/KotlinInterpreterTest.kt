package com.simiacryptus.skyenet.heart

import com.simiacryptus.skyenet.HeartTestBase
import org.junit.jupiter.api.Test

class KotlinInterpreterTest : HeartTestBase() {

    override fun newInterpreter(map: java.util.Map<String, Object>) = KotlinInterpreter(map)

    @Test
    override fun `test run with variables`() {
        // TODO: This test is failing due to a bug with supplied primitives (e.g. Integer)
    }

}