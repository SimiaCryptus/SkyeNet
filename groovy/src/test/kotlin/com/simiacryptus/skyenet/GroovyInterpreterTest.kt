package com.simiacryptus.skyenet.heart.test

import com.simiacryptus.skyenet.HeartTestBase
import com.simiacryptus.skyenet.heart.GroovyInterpreter
import org.junit.jupiter.api.Test

class GroovyInterpreterTest : HeartTestBase() {
    override fun newInterpreter(map: java.util.Map<String,Object>) = GroovyInterpreter(map)

}

