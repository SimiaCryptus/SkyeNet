package com.simiacryptus.skyenet.core.actors

import com.fasterxml.jackson.annotation.JsonIgnore
import com.simiacryptus.jopenai.API
import com.simiacryptus.jopenai.ApiModel.*
import com.simiacryptus.jopenai.ClientUtil.toContentList
import com.simiacryptus.jopenai.OpenAIClient
import com.simiacryptus.jopenai.describe.AbbrevWhitelistYamlDescriber
import com.simiacryptus.jopenai.describe.TypeDescriber
import com.simiacryptus.jopenai.models.ChatModels
import com.simiacryptus.skyenet.core.Interpreter
import com.simiacryptus.skyenet.core.OutputInterceptor
import java.util.*
import javax.script.ScriptException
import kotlin.reflect.KClass

open class CodingActor(
  val interpreterClass: KClass<out Interpreter>,
  val symbols: Map<String, Any> = mapOf(),
  val describer: TypeDescriber = AbbrevWhitelistYamlDescriber(
    "com.simiacryptus",
    "com.github.simiacryptus"
  ),
  name: String? = interpreterClass.simpleName,
  val details: String? = null,
  model: ChatModels = ChatModels.GPT35Turbo,
  val fallbackModel: ChatModels = ChatModels.GPT4Turbo,
  temperature: Double = 0.1,
  val runtimeSymbols: Map<String, Any> = mapOf()
) : BaseActor<CodingActor.CodeRequest, CodingActor.CodeResult>(
  prompt = "",
  name = name,
  model = model,
  temperature = temperature,
) {
  val interpreter: Interpreter
    get() = interpreterClass.java.getConstructor(Map::class.java).newInstance(symbols + runtimeSymbols)

  data class CodeRequest(
    val messages: List<String>,
    val codePrefix: String = "",
    val autoEvaluate: Boolean = false,
    val fixIterations: Int = 4,
    val fixRetries: Int = 4,
  )

  interface CodeResult {
    enum class Status {
      Coding, Correcting, Success, Failure
    }

    val code: String
    val status: CodeResult.Status
    val result: ExecutionResult
  }

  data class ExecutionResult(
    val resultValue: String,
    val resultOutput: String
  )

  override val prompt: String
    get() = if (symbols.isNotEmpty()) """
            |You will translate natural language instructions into 
            |an implementation using ${language} and the script context.
            |Use ``` code blocks labeled with ${language} where appropriate.
            |
            |Defined symbols include {${symbols.keys.joinToString(", ")}} described below:
            |
            |```${this.describer.markupLanguage}
            |${this.apiDescription}
            |```
            |
            |${details ?: ""}
            |""".trimMargin().trim()
    else """
            |You will translate natural language instructions into 
            |an implementation using ${language} and the script context.
            |Use ``` code blocks labeled with ${language} where appropriate.
            |
            |${details ?: ""}
            |""".trimMargin().trim()

  open val apiDescription: String
    get() = this.symbols.map { (name, utilityObj) ->
      """
            |$name:
            |    ${this.describer.describe(utilityObj.javaClass).indent("    ")}
            |""".trimMargin().trim()
    }.joinToString("\n")


  val language: String by lazy { interpreter.getLanguage() }

  override fun chatMessages(questions: CodeRequest): Array<ChatMessage> {
    var chatMessages = arrayOf(
      ChatMessage(
        role = Role.system,
        content = prompt.toContentList()
      ),
    ) + questions.messages.map {
      ChatMessage(
        role = Role.user,
        content = it.toContentList()
      )
    }
    if (questions.codePrefix.isNotBlank()) {
      chatMessages = (chatMessages.dropLast(1) + listOf(
        ChatMessage(Role.assistant, questions.codePrefix.toContentList())
      ) + chatMessages.last()).toTypedArray<ChatMessage>()
    }
    return chatMessages

  }

  override fun answer(
    vararg messages: ChatMessage,
    input: CodeRequest,
    api: API,
  ): CodeResult {
    var result = CodeResultImpl(*messages, api = (api as OpenAIClient), input = input)
    if (!input.autoEvaluate) return result
    for (i in 0..input.fixIterations) try {
      require(result.result.resultValue.length > -1)
      return result
    } catch (ex: Throwable) {
      if (i == input.fixIterations) {
        log.info(
          "Failed to implement ${
            messages.map { it.content?.joinToString("\n") { it.text ?: "" } }.joinToString("\n")
          }"
        )
        throw ex
      }
      val respondWithCode = fixCommand(api, result.code, ex, *messages, model = model)
      val codeBlocks = extractCodeBlocks(respondWithCode)
      val renderedResponse = getRenderedResponse(codeBlocks)
      val codedInstruction = getCode(language, codeBlocks)
      log.info("Response: \n\t${renderedResponse.replace("\n", "\n\t", false)}".trimMargin())
      log.info("New Code: \n\t${codedInstruction.replace("\n", "\n\t", false)}".trimMargin())
      result = CodeResultImpl(*messages, input = input, api = api, givenCode = codedInstruction)
    }
    throw IllegalStateException()
  }

  open fun execute(prefix: String, code: String): ExecutionResult {
    //language=HTML
    log.info("Running $code")
    OutputInterceptor.clearGlobalOutput()
    val result = try {
      interpreter.run((prefix + code).sortCode())
    } catch (e: Exception) {
      when {
        e is ScriptException -> throw FailedToImplementException(e, errorMessage(e, code), code)
        e.cause is ScriptException -> throw FailedToImplementException(
          e,
          errorMessage(e.cause!! as ScriptException, code),
          code
        )
        else -> throw e
      }
    }
    log.info("Result: $result")
    //language=HTML
    val executionResult = ExecutionResult(result.toString(), OutputInterceptor.getGlobalOutput())
    OutputInterceptor.clearGlobalOutput()
    return executionResult
  }

  private inner class CodeResultImpl(
    vararg val messages: ChatMessage,
    val input: CodeRequest,
    val api: OpenAIClient,
    val givenCode: String? = null,
  ) : CodeResult {
    var _status = CodeResult.Status.Coding

    override val status get() = _status

    @JsonIgnore
    override val code: String = givenCode ?: try {
        implement(model)
      } catch (ex: FailedToImplementException) {
        if (fallbackModel != model) {
          try {
            implement(fallbackModel)
          } catch (ex: FailedToImplementException) {
            log.info("Failed to implement ${messages.map { it.content }.joinToString("\n")}")
            _status = CodeResult.Status.Failure
            throw ex
          }
        } else {
          log.info("Failed to implement ${messages.map { it.content }.joinToString("\n")}")
          _status = CodeResult.Status.Failure
          throw ex
        }
      }


    private fun implement(
      model: ChatModels,
    ): String {
      val request = ChatRequest(messages = ArrayList(this.messages.toList()))
      for (codingAttempt in 0..input.fixRetries) {
        try {
          val codeBlocks = extractCodeBlocks(chat(api, request, model))
          val renderedResponse = getRenderedResponse(codeBlocks)
          val codedInstruction = getCode(language, codeBlocks)
          log.info("Response: \n\t${renderedResponse.replace("\n", "\n\t", false)}".trimMargin())
          log.info("New Code: \n\t${codedInstruction.replace("\n", "\n\t", false)}".trimMargin())
          var workingCode = codedInstruction
          for (fixAttempt in 0..input.fixIterations) {
            try {
              val validate = interpreter.validate((input.codePrefix + "\n" + workingCode).sortCode())
              if (validate != null) throw validate
              log.info("Validation succeeded")
              _status = CodeResult.Status.Success
              return workingCode
            } catch (ex: Throwable) {
              if (fixAttempt == input.fixIterations) throw FailedToImplementException(
                ex, """
                                |Failed to fix code:
                                |
                                |```${language.lowercase()}
                                |${workingCode}
                                |```
                                |
                                |```text
                                |${ex.message}
                                |```
                                """.trimMargin().trim(), workingCode
              )
              log.info("Validation failed - ${ex.message}")
              _status = CodeResult.Status.Correcting
              val respondWithCode = fixCommand(api, workingCode, ex, *messages, model = model)
              val codeBlocks = extractCodeBlocks(respondWithCode)
              val response = getRenderedResponse(codeBlocks)
              workingCode = getCode(language, codeBlocks)
              log.info("Response: \n\t${response.replace("\n", "\n\t", false)}".trimMargin())
              log.info("New Code: \n\t${workingCode.replace("\n", "\n\t", false)}".trimMargin())
            }
          }
        } catch (ex: FailedToImplementException) {
          if (codingAttempt == input.fixRetries) {
            log.info("Failed to implement ${messages.map { it.content }.joinToString("\n")}")
            throw ex
          }
          log.info("Retry failed to implement ${messages.map { it.content }.joinToString("\n")}")
          _status = CodeResult.Status.Correcting
        }
      }
      throw IllegalStateException()
    }


    private val executionResult by lazy { execute(input.codePrefix, code) }
    override val result get() = executionResult
  }

  private fun fixCommand(
    api: OpenAIClient,
    previousCode: String,
    error: Throwable,
    vararg promptMessages: ChatMessage,
    model: ChatModels
  ): String = chat(
    api = api,
    request = ChatRequest(
      messages = ArrayList(
        promptMessages.toList() + listOf(
          ChatMessage(
            Role.assistant,
            """
                        |```${language.lowercase()}
                        |${previousCode}
                        |```
                        |""".trimMargin().trim().toContentList()
          ),
          ChatMessage(
            Role.system,
            """
                        |The previous code failed with the following error:
                        |
                        |```
                        |${error.message?.trim() ?: ""}
                        |```
                        |
                        |Correct the code and try again.
                        |""".trimMargin().trim().toContentList()
          )
        )
      )
    ),
    model = model
  )

  private fun chat(api: OpenAIClient, request: ChatRequest, model: ChatModels) =
    api.chat(request.copy(model = model.modelName, temperature = temperature), model)
      .choices.first().message?.content.orEmpty().trim()

  companion object {
    private val log = org.slf4j.LoggerFactory.getLogger(CodingActor::class.java)

    fun String.indent(indent: String = "  ") = this.replace("\n", "\n$indent")

    fun extractCodeBlocks(response: String): List<Pair<String, String>> {
      val codeBlockRegex = Regex("(?s)```(.*?)\\n(.*?)```")
      val languageRegex = Regex("([a-zA-Z0-9-_]+)")

      val result = mutableListOf<Pair<String, String>>()
      var startIndex = 0

      val matches = codeBlockRegex.findAll(response)
      if (matches.count() == 0) return listOf(Pair("text", response))
      for (match in matches) {
        // Add non-code block before the current match as "text"
        if (startIndex < match.range.first) {
          result.add(Pair("text", response.substring(startIndex, match.range.first)))
        }

        // Extract language and code
        val languageMatch = languageRegex.find(match.groupValues[1])
        val language = languageMatch?.groupValues?.get(0) ?: "code"
        val code = match.groupValues[2]

        // Add code block to the result
        result.add(Pair(language, code))

        // Update the start index
        startIndex = match.range.last + 1
      }

      // Add any remaining non-code text after the last code block as "text"
      if (startIndex < response.length) {
        result.add(Pair("text", response.substring(startIndex)))
      }

      return result
    }

    fun getRenderedResponse(respondWithCode: List<Pair<String, String>>) =
      respondWithCode.joinToString("\n") {
        var language = it.first
        if (language == "code") language = "groovy"
        if (language == "text") {
          //language=HTML
          """
                    |<div>
                    |${it.second}
                    |</div>
                    |""".trimMargin().trim()
        } else {
          //language=HTML
          """
                    |<pre><code class="language-$language">
                    |${it.second}
                    |</code></pre>
                    |""".trimMargin().trim()
        }
      }

    fun getCode(language: String, textSegments: List<Pair<String, String>>): String {
      if (textSegments.size == 1) return textSegments.joinToString("\n") { it.second }
      return textSegments.joinToString("\n") {
        if (it.first.lowercase() == "code" || it.first.lowercase() == language.lowercase()) {
          """
                            |${it.second}
                            |""".trimMargin().trim()
        } else {
          ""
        }
      }
    }

    fun String.sortCode(bodyWrapper: (String) -> String = { it }): String {
      val (imports, otherCode) = this.split("\n").partition { it.trim().startsWith("import ") }
      return imports.distinct().sorted().joinToString("\n") + "\n\n" + bodyWrapper(otherCode.joinToString("\n"))
    }

    fun String.camelCase(locale: Locale = Locale.getDefault()): String {
      val words = fromPascalCase().split(" ").map { it.trim() }.filter { it.isNotEmpty() }
      return words.first().lowercase(locale) + words.drop(1).joinToString("") {
        it.replaceFirstChar { c ->
          when {
            c.isLowerCase() -> c.titlecase(locale)
            else -> c.toString()
          }
        }
      }
    }

    fun String.pascalCase(locale: Locale = Locale.getDefault()): String =
      fromPascalCase().split(" ").map { it.trim() }.filter { it.isNotEmpty() }.joinToString("") {
        it.replaceFirstChar { c ->
          when {
            c.isLowerCase() -> c.titlecase(locale)
            else -> c.toString()
          }
        }
      }

    // Detect changes in the case of the first letter and prepend a space
    fun String.fromPascalCase(): String = buildString {
      var lastChar = ' '
      for (c in this@fromPascalCase) {
        if (c.isUpperCase() && lastChar.isLowerCase()) append(' ')
        append(c)
        lastChar = c
      }
    }

    fun String.upperSnakeCase(locale: Locale = Locale.getDefault()): String =
      fromPascalCase().split(" ").map { it.trim() }.filter { it.isNotEmpty() }.joinToString("_") {
        it.replaceFirstChar { c ->
          when {
            c.isLowerCase() -> c.titlecase(locale)
            else -> c.toString()
          }
        }
      }.uppercase(locale)

    fun String.imports(): List<String> {
      return this.split("\n").filter { it.trim().startsWith("import ") }.distinct().sorted()
    }

    fun String.stripImports(): String {
      return this.split("\n").filter { !it.trim().startsWith("import ") }.joinToString("\n")
    }

    fun errorMessage(ex: ScriptException, code: String) = try {
      """
      |```text
      |${ex.message ?: ""} at line ${ex.lineNumber} column ${ex.columnNumber}
      |  ${if (ex.lineNumber > 0) code.split("\n")[ex.lineNumber - 1] else ""}
      |  ${if (ex.columnNumber > 0) " ".repeat(ex.columnNumber - 1) + "^" else ""}
      |```
      """.trimMargin().trim()
    } catch (_: Exception) {
      ex.message ?: ""
    }

  }

  class FailedToImplementException(
    cause: Throwable? = null,
    message: String = "Failed to implement",
    val code: String? = null
  ) : RuntimeException(message, cause)
}
