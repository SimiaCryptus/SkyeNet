package com.simiacryptus.skyenet.scala

import com.simiacryptus.skyenet.interpreter.{Interpreter, InterpreterTestBase}

import java.util


class ScalaLocalInterpreterTest extends InterpreterTestBase {
//  override def newInterpreter(map: util.Map[String, AnyRef]): Interpreter = {
//    new ScalaLocalInterpreter(map)
//  }

  override def newInterpreter(map: util.Map[String, _]): Interpreter = new ScalaLocalInterpreter(map.asInstanceOf[util.Map[String, Object]])
}
