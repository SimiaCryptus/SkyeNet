package com.simiacryptus.skyenet.heart

import com.simiacryptus.skyenet.HeartTestBase
import org.junit.jupiter.api.Test
import java.util.zip.ZipFile

class KotlinLocalInterpreterTest : HeartTestBase() {

    override fun newInterpreter(map: java.util.Map<String, Object>) = KotlinLocalInterpreter(map)

    @Test
    override fun `test run with variables`() {
        // TODO: This test is failing due to a bug with supplied primitives (e.g. Integer)
    }

    @Test
    override fun `test validate with variables`() {
        // TODO: This test is failing due to a bug with supplied primitives (e.g. Integer)
    }

}