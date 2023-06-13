package com.simiacryptus.skyenet.heart

import com.simiacryptus.skyenet.Heart
import com.simiacryptus.skyenet.heart.ScalaLocalInterpreter.log

import java.nio.file.Paths
import java.util.function.Supplier
import scala.jdk.CollectionConverters.MapHasAsScala
import scala.reflect.internal.util.Position
import scala.reflect.runtime.universe
import scala.reflect.runtime.universe._
import scala.tools.nsc.Settings
import scala.tools.nsc.interpreter.shell.ReplReporterImpl
import scala.tools.nsc.interpreter.{IMain, Results}
import scala.util.Try

object ScalaLocalInterpreter {
  private val log = org.slf4j.LoggerFactory.getLogger(getClass)

  def getTypeTag(value: Any): Type = {
    val mirror = runtimeMirror(value.getClass.getClassLoader)
    mirror.reflect(value).symbol.toType
  }
}

class ScalaLocalInterpreter(defs: Map[String, Any] = Map.empty, typeTags: Map[String, Type] = Map.empty) extends Heart {
  def this(defs: java.util.Map[String, Object]) = this(defs.asInstanceOf[java.util.Map[String, Any]].asScala.toMap, defs.asScala.map(x => (x._1, ScalaLocalInterpreter.getTypeTag(x))).toMap)

  private def getClasspathFromManifest(jarPath: String): String = {
    val jarFile = new java.util.jar.JarFile(jarPath)
    val manifest = jarFile.getManifest
    val classPathAttr = manifest.getMainAttributes.getValue("Class-Path")
    if (null == classPathAttr) return jarPath
    jarFile.close()

    val classPathEntries = classPathAttr.split(" ")
    val localPaths = classPathEntries.map { entry =>
      Paths.get(new java.net.URI(entry)).toString
    }
    localPaths.mkString(java.io.File.pathSeparator)
  }

  class CustomReplReporter(settings: Settings) extends ReplReporterImpl(settings) {

    val errors = new StringBuilder

    override def doReport(pos: Position, msg: String, severity: Severity): Unit = {
      super.doReport(pos, msg, severity)
      severity match {
        case ERROR =>
          log.warn(s"[$severity] $msg")
          errors.append(s"[Error] $msg\n")
        case WARNING =>
          log.info(s"[$severity] $msg")
        case INFO =>
          log.debug(s"[$severity] $msg")
        case _ =>
          log.debug(s"[$severity] $msg")
      }
    }

    override def hasErrors: Boolean = super.hasErrors

    override def hasWarnings: Boolean = super.hasWarnings

  }

  val settings = {
    val settings = new Settings
    try {
      val classpathEntries = System.getProperty("java.class.path").split(java.io.File.pathSeparator)
      if (classpathEntries.filter(_.endsWith(".jar")).length == 1) {
        val tmpJarPath = classpathEntries.find(_.endsWith(".jar")).get
        val expandedClasspath = getClasspathFromManifest(tmpJarPath)
        settings.classpath.value = expandedClasspath
      } else {
        settings.usejavacp.value = true
      }
      settings
    } catch {
      case e: Throwable =>
        log.warn("Error loading Scala settings", e)
        settings.usejavacp.value = true
        settings
    }
  }

  private val customReporter = new CustomReplReporter(settings)

  private val engine = {
    val main = new IMain(settings, customReporter)
    defs.foreach { case (key, value) =>
      val valueType = typeTags(key).toString
      log.debug(s"Def Binding $key: $value : $valueType")
      main.bind(key, valueType, value)
    }
    main
  }

  override def getLanguage: String = "Scala"

  override def run(code: String): Any = {
    wrapExecution(() => {
      val wrappedCode =
        s"""val __tempResult__ = { try {
           |  $code
           |} catch {
           |  case e: Throwable => {
           |    e.printStackTrace()
           |    throw e
           |  }
           |} }
           |""".stripMargin
      log.debug(s"Running $wrappedCode")
      customReporter.reset()
      customReporter.errors.clear()
      val result = engine.interpret(wrappedCode)
      result match {
        case Results.Success =>
          val tempResult = engine.valueOfTerm("__tempResult__")
          tempResult.getOrElse {
            throw new RuntimeException("Failed to get value of most recent expression")
          }
        case Results.Error =>
          val errorString = "Error: " + customReporter.errors.toString
          throw new RuntimeException(errorString)
        case Results.Incomplete =>
          val errorString = "Incomplete: "+ customReporter.errors.toString
          throw new RuntimeException(errorString)
      }
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