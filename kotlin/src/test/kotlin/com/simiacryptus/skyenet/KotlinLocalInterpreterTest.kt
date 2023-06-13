package com.simiacryptus.skyenet.heart

import com.simiacryptus.skyenet.HeartTestBase
import org.junit.jupiter.api.Assertions.*
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Disabled
import org.junit.jupiter.api.Test
import javax.script.ScriptException

class KotlinLocalInterpreterTest : HeartTestBase() {
    override fun newInterpreter(map: java.util.Map<String, Object>) = KotlinLocalInterpreter(map)
}