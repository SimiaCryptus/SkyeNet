package com.simiacryptus.skyenet.heart.test

import com.simiacryptus.skyenet.util.HeartTestBase
import com.simiacryptus.skyenet.heart.GroovyInterpreter

class GroovyInterpreterTest : HeartTestBase() {
    override fun newInterpreter(map: java.util.Map<String,Object>) = GroovyInterpreter(map)

}

