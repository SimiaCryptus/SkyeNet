@file:Suppress("PLATFORM_CLASS_MAPPED_TO_KOTLIN")

package com.simiacryptus.skyenet.groovy

import com.simiacryptus.skyenet.core.util.InterpreterTestBase

class GroovyInterpreterTest : InterpreterTestBase() {
    override fun newInterpreter(map: java.util.Map<String,Object>) = GroovyInterpreter(map)

}

