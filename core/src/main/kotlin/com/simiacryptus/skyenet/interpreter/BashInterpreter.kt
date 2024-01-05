package com.simiacryptus.skyenet.interpreter

import org.slf4j.LoggerFactory

open class BashInterpreter(
  val defs: Map<String, Any> = mapOf(),
) : Interpreter {
  init {
    require(defs.isEmpty()) { "BashInterpreter does not support symbols" }
  }
  final override fun getLanguage(): String = "bash"
  override fun getSymbols() = defs


  override fun validate(code: String): Throwable? {
    return null
  }

  override fun run(code: String): Any? {
    val wrappedCode = wrapCode(code.trim())
    val process = Runtime.getRuntime().exec(arrayOf("bash"))
    process.outputStream.write(wrappedCode.toByteArray())
    process.outputStream.close()
    val output = process.inputStream.bufferedReader().readText()
    val error = process.errorStream.bufferedReader().readText()
    if (error.isNotEmpty()) throw RuntimeException(error)
    return output
  }

  companion object {
    private val log = LoggerFactory.getLogger(BashInterpreter::class.java)
  }
}