package com.simiacryptus.skyenet.heart

import com.simiacryptus.skyenet.Heart

import java.util.function.Supplier
import scala.jdk.CollectionConverters.MapHasAsScala
import scala.reflect.runtime.universe._
import scala.tools.nsc.Settings
import scala.tools.nsc.interpreter.IMain
import scala.tools.nsc.interpreter.shell.ReplReporterImpl
import scala.util.Try

class ScalaLocalInterpreter(defs: Map[String, Any] = Map.empty, typeTags: Map[String, Type] = Map.empty) extends Heart {
  def this(defs: java.util.Map[String, Any], typeTags: java.util.Map[String, Type]) = this(defs.asScala.toMap, typeTags.asScala.toMap)

  private val engine = {
    val settings = new Settings
    settings.usejavacp.value = true
    val main = new IMain(settings, new ReplReporterImpl(settings))
    defs.foreach { case (key, value) =>
      val valueType = typeTags(key).typeSymbol.asClass.fullName
      main.bind(key, valueType, value)
    }
    main
  }
  override def getLanguage: String = "Scala"

  override def run(code: String): Any = {
    wrapExecution(() => {
      engine.interpret(code)
    })
  }

  override def validate(code: String): Exception = {
    Try(engine.compileString(code)).toEither match {
      case Right(_) => null
      case Left(e) => e.asInstanceOf[Exception]
    }
  }

  override def wrapCode(code: String): String = code

  override def wrapExecution[T](fn: Supplier[T]): T = fn.get()


}

object ScalaLocalInterpreter {
  def main(args: Array[String]): Unit = {
    val defs = Map(
      "message" -> "hello", "function" -> ((x: Int) => x * x)
    )
    val typeTags = Map(
      "message" -> typeOf[String], "function" -> typeOf[Function1[Int, Int]]
    )
    val interpreter = new ScalaLocalInterpreter(defs, typeTags)
    interpreter.run("System.out.println(message)")
    interpreter.run("System.out.println(function(5))")
  }

}