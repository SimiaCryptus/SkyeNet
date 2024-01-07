package com.simiacryptus.skyenet.interpreter

import com.simiacryptus.skyenet.webui.util.MarkdownUtil.renderMarkdown
import org.slf4j.LoggerFactory
import java.util.concurrent.TimeUnit

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
    val cmd = arrayOf("bash")
    val cwd = defs["workingDir"]?.toString()?.let { java.io.File(it) } ?: java.io.File(".")
    val processBuilder = ProcessBuilder(*cmd).directory(cwd)
    defs["env"]?.let { env -> processBuilder.environment().putAll((env as Map<String, String>)) }
    val process = processBuilder.start()

    process.outputStream.write(wrappedCode.toByteArray())
    process.outputStream.close()
    val output = process.inputStream.bufferedReader().readText()
    val error = process.errorStream.bufferedReader().readText()

    val waitFor = process.waitFor(5, TimeUnit.MINUTES)
    if (!waitFor) {
      process.destroy()
      throw RuntimeException("Timeout; output: $output; error: $error")
    } else if (error.isNotEmpty()) {
      //throw RuntimeException(error)
      return renderMarkdown(
        """
        |ERROR:
        |```text
        |$error
        |```
        |
        |OUTPUT:
        |```text
        |$output
        |```
        """.trimMargin()
      )
    } else {
      return output
    }
  }

  companion object {
    private val log = LoggerFactory.getLogger(BashInterpreter::class.java)
  }
}