package com.simiacryptus.skyenet.scala

import com.simiacryptus.skyenet.core.Heart
import com.simiacryptus.skyenet.core.util.HeartTestBase

import java.util


class ScalaLocalInterpreterTest extends HeartTestBase {
  override def newInterpreter(map: util.Map[String, AnyRef]): Heart = {
    new ScalaLocalInterpreter(map)
  }
}
