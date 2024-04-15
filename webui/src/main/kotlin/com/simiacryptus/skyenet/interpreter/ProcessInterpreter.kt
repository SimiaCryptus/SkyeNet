package com.simiacryptus.skyenet.interpreter

import org.slf4j.LoggerFactory
import java.util.concurrent.TimeUnit

open class ProcessInterpreter(
    private val defs: Map<String, Any> = mapOf(),
) : Interpreter {

    val command: List<String>
        get() = defs["command"]?.let { command ->
            when (command) {
                is String -> command.split(" ")
                is List<*> -> command.map { it.toString() }
                else -> throw IllegalArgumentException("Invalid command: $command")
            }
        } ?: listOf("bash")

    final override fun getLanguage(): String = defs["language"]?.toString() ?: "bash"
    override fun getSymbols() = defs


    override fun validate(code: String): Throwable? {
        // Always valid
        return null
    }

    override fun run(code: String): Any? {
        val wrappedCode = wrapCode(code.trim())
        val cmd = command.toTypedArray()
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
            return (
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

    companion object
}