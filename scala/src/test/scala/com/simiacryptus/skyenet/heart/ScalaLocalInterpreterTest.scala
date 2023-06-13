package com.test

import com.simiacryptus.skyenet.heart.ScalaLocalInterpreter
import com.simiacryptus.skyenet.{Heart, HeartTestBase}

import java.util


class ScalaLocalInterpreterTest extends HeartTestBase {
  override def newInterpreter(map: util.Map[String, AnyRef]): Heart = {
    new ScalaLocalInterpreter(map)
  }
}
