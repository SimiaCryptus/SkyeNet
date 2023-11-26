package com.simiacryptus.skyenet.scala

import com.simiacryptus.skyenet.core.Interpreter
import com.simiacryptus.skyenet.core.util.InterpreterTestBase

import java.util


class ScalaLocalInterpreterTest extends InterpreterTestBase {
  override def newInterpreter(map: util.Map[String, AnyRef]): Interpreter = {
    new ScalaLocalInterpreter(map)
  }
}
