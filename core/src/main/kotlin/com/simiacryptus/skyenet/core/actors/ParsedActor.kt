package com.simiacryptus.skyenet.core.actors

import com.simiacryptus.jopenai.API
import com.simiacryptus.jopenai.ApiModel
import com.simiacryptus.jopenai.OpenAIClient
import com.simiacryptus.jopenai.describe.AbbrevWhitelistYamlDescriber
import com.simiacryptus.jopenai.describe.TypeDescriber
import com.simiacryptus.jopenai.models.ChatModels
import com.simiacryptus.jopenai.util.ClientUtil.toContentList
import com.simiacryptus.jopenai.util.JsonUtil
import java.util.function.Function

open class ParsedActor<T : Any>(
  val parserClass: Class<out Function<String, T>>? = null,
  val resultClass: Class<T> = parserClass!!.getMethod("apply", String::class.java).returnType as Class<T>,
  prompt: String,
  name: String? = (parserClass?.simpleName ?: resultClass.simpleName),
  model: ChatModels,
  temperature: Double = 0.3,
  val parsingModel: ChatModels,
  val deserializerRetries: Int = 2,
) : BaseActor<List<String>, ParsedResponse<T>>(
  prompt = prompt,
  name = name,
  model = model,
  temperature = temperature,
) {
  override fun chatMessages(questions: List<String>) = arrayOf(
    ApiModel.ChatMessage(
      role = ApiModel.Role.system,
      content = prompt.toContentList()
    ),
  ) + questions.map {
    ApiModel.ChatMessage(
      role = ApiModel.Role.user,
      content = it.toContentList()
    )
  }

  private inner class ParsedResponseImpl(vararg messages: ApiModel.ChatMessage, api: API) :
    ParsedResponse<T>(resultClass) {
    override val text = response(*messages, api = api).choices.first().message?.content ?: throw RuntimeException("No response")
    private val _obj: T by lazy { getParser(api).apply(text) }
    override val obj get() = _obj
  }

  fun getParser(api: API) = Function<String, T> { input ->
    describer.coverMethods = false
    val describe = describer.describe(resultClass)
    val prompt = """
            |Parse the user's message into a json object described by:
            |
            |```yaml
            |${describe.replace("\n", "\n  ")}
            |```
          """.trimMargin()
    for (i in 0 until deserializerRetries) {
      try {
        val content = (api as OpenAIClient).chat(
          ApiModel.ChatRequest(
            messages = listOf(
              ApiModel.ChatMessage(role = ApiModel.Role.system, content = prompt.toContentList()),
              ApiModel.ChatMessage(role = ApiModel.Role.user, content = input.toContentList()),
            ),
            temperature = temperature,
            model = model.modelName,
          ),
          model = model,
        ).choices.first().message?.content
        var contentUnwrapped = content?.trim() ?: throw RuntimeException("No response")

        // If Plaintext is found before the { or ```, strip it
        if(!contentUnwrapped.startsWith("{") && !contentUnwrapped.startsWith("```")) {
          val start = contentUnwrapped.indexOf("{").coerceAtMost(contentUnwrapped.indexOf("```"))
          val end = contentUnwrapped.lastIndexOf("}").coerceAtLeast(contentUnwrapped.lastIndexOf("```") + 2) + 1
          contentUnwrapped = contentUnwrapped.substring(start, end)
        }

        // if input is wrapped in a ```json block, remove the block
        if (contentUnwrapped.startsWith("```json") && contentUnwrapped.endsWith("```")) {
          contentUnwrapped = contentUnwrapped.substring(7, contentUnwrapped.length - 3)
        }

        contentUnwrapped.let { return@Function JsonUtil.fromJson<T>(it, resultClass) }
      } catch (e: Exception) {
        log.info("Failed to parse response", e)
      }
    }
    throw RuntimeException("No response")
  }

  open val describer: TypeDescriber = object : AbbrevWhitelistYamlDescriber(
    "com.simiacryptus", "com.github.simiacryptus"
  ) {
    override val includeMethods: Boolean get() = false
  }

  override fun respond(input: List<String>, api: API, vararg messages: ApiModel.ChatMessage): ParsedResponse<T> {
    return ParsedResponseImpl(*messages, api = api)
  }

  override fun withModel(model: ChatModels): ParsedActor<T> = ParsedActor(
    parserClass = parserClass,
    prompt = prompt,
    name = name,
    model = model,
    temperature = temperature,
    parsingModel = parsingModel,
  )
  companion object {
    private val log = org.slf4j.LoggerFactory.getLogger(ParsedActor::class.java)
  }
}
