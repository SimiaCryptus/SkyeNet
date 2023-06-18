package com.simiacryptus.skyenet.heart

import com.simiacryptus.skyenet.HeartTestBase
import org.junit.jupiter.api.Assertions.*
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Disabled
import org.junit.jupiter.api.Test

class KotlinDaemonInterpreterTest : HeartTestBase() {
    override fun newInterpreter(map: java.util.Map<String, Object>) = KotlinDaemonInterpreter(map)
}