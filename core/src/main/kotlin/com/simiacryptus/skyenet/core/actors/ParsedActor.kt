package com.simiacryptus.skyenet.core.actors

import com.simiacryptus.jopenai.API
import com.simiacryptus.jopenai.ApiModel
import com.simiacryptus.jopenai.OpenAIClient
import com.simiacryptus.jopenai.describe.AbbrevWhitelistYamlDescriber
import com.simiacryptus.jopenai.describe.TypeDescriber
import com.simiacryptus.jopenai.models.ChatModels
import com.simiacryptus.jopenai.models.OpenAITextModel
import com.simiacryptus.jopenai.proxy.ChatProxy
import com.simiacryptus.jopenai.util.ClientUtil.toContentList
import java.util.function.Function

open class ParsedActor<T : Any>(
  val parserClass: Class<out Function<String, T>>,
  prompt: String,
  name: String? = parserClass.simpleName,
  model: OpenAITextModel = ChatModels.GPT35Turbo,
  temperature: Double = 0.3,
  val parsingModel: OpenAITextModel = ChatModels.GPT35Turbo,
  val deserializerRetries: Int = 2,
) : BaseActor<List<String>, ParsedResponse<T>>(
  prompt = prompt,
  name = name,
  model = model,
  temperature = temperature,
) {
  val resultClass: Class<T> by lazy { parserClass.getMethod("apply", String::class.java).returnType as Class<T> }
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

  fun getParser(api: API): Function<String, T> = object : ChatProxy<Function<String, T>>(
    clazz = parserClass,
    api = (api as OpenAIClient),
    model = parsingModel,
    temperature = 0.1,
    deserializerRetries = deserializerRetries,
  ){
    override val describer: TypeDescriber get() = this@ParsedActor.describer
  }.create()

  open val describer: TypeDescriber = object : AbbrevWhitelistYamlDescriber(
    "com.simiacryptus", "com.github.simiacryptus"
  ) {
    override val includeMethods: Boolean get() = false
  }

  override fun respond(input: List<String>, api: API, vararg messages: ApiModel.ChatMessage): ParsedResponse<T> {
    return ParsedResponseImpl(*messages, api = api)
  }

  override fun withModel(model: OpenAITextModel): ParsedActor<T> = ParsedActor(
    parserClass = parserClass,
    prompt = prompt,
    name = name,
    model = model,
    temperature = temperature,
    parsingModel = parsingModel,
  )
}
