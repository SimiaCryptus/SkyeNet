import ammonite.interp.Interpreter
import ammonite.util.{Imports, Res}

class AutoScript(predef: Array[String] = Array.empty) {

  private val interpreter: Interpreter = ammonite.Main()
    .instantiateInterpreter()
    .getOrElse(throw new RuntimeException("Could not instantiate interpreter"))

  def initialize(): Unit = {
    interpreter.initializePredef(
      basePredefs = scala.Seq.empty,
      customPredefs = scala.Seq.empty,
      extraBridges = scala.Seq.empty
    )
  }

  def run(scalaCode: String): Unit = {
    val imports = interpreter.processExec(predef.mkString("\n") + "\n" + scalaCode, 0, () => {}) match {
      case Res.Failure(trace) => throw new RuntimeException(trace)
      case Res.Exception(ex, _) => throw ex
      case Res.Skip => throw new RuntimeException("Skip")
      case Res.Success(value) => value
    }
    null
  }

//  {
//    predef.foreach(run)
//  }

  def getGlobalSymbols(): Array[String] = {
    Array.empty
  }
}
