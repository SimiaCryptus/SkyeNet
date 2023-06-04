package com.simiacryptus.skyenet.heart

import org.junit.jupiter.api.function.Executable
import org.junit.jupiter.api.{Assertions, Disabled, Test}

import java.util
import scala.reflect.runtime.universe._

class ScalaLocalInterpreterTest {

  private val dummyTypeTag: Type = typeOf[String]
  private val dummyMap: util.Map[String, Any] = new util.HashMap[String, Any]()
  private val dummyTypeTags: util.Map[String, Type] = new util.HashMap[String, Type]()

  @Test
  def testConstructor() {
    val interpreter = new ScalaLocalInterpreter(dummyMap, dummyTypeTags)
    Assertions.assertNotNull(interpreter, "ScalaLocalInterpreter object should not be null.")
  }

  @Test
  def testGetLanguage() {
    val interpreter = new ScalaLocalInterpreter(dummyMap, dummyTypeTags)
    Assertions.assertEquals("Scala", interpreter.getLanguage, "ScalaLocalInterpreter should return 'Scala' as the language.")
  }

  @Test
  def testValidate() {
    val interpreter = new ScalaLocalInterpreter(dummyMap, dummyTypeTags)
    val code = "val x = 10"
    Assertions.assertNull(interpreter.validate(code), "ScalaLocalInterpreter should validate correct Scala code without returning an Exception.")
  }

  @Test
  @Disabled
  def testValidateWithIncorrectCode() {
    val interpreter = new ScalaLocalInterpreter(dummyMap, dummyTypeTags)
    val code = "val x = "
    Assertions.assertNotNull(interpreter.validate(code), "ScalaLocalInterpreter should return an Exception when trying to validate incorrect Scala code.")
  }

  @Test
  def testRun() {
    val interpreter = new ScalaLocalInterpreter(dummyMap, dummyTypeTags)
    val code = "val x = 10"
    Assertions.assertEquals((), interpreter.run(code), "ScalaLocalInterpreter should run the code without throwing an Exception.")
  }

  @Test
  def testRunWithIncorrectCode() {
    val interpreter = new ScalaLocalInterpreter(dummyMap, dummyTypeTags)
    val code = "val x = "
    Assertions.assertThrows(classOf[RuntimeException], new Executable {
      override def execute(): Unit = {
        interpreter.run(code)
      }
    }, "ScalaLocalInterpreter should throw a RuntimeException when running incorrect Scala code.")
  }

  // Add other tests here...
}
