package com.simiacryptus.skyenet.core.actors

import com.fasterxml.jackson.annotation.JsonIgnore
import com.simiacryptus.jopenai.API
import com.simiacryptus.jopenai.ApiModel
import com.simiacryptus.jopenai.ClientUtil.toContentList
import com.simiacryptus.jopenai.OpenAIClient
import com.simiacryptus.jopenai.models.ChatModels
import com.simiacryptus.jopenai.proxy.ChatProxy
import java.util.function.Function

open class ParsedActor<T>(
  val parserClass: Class<out Function<String, T>>,
  prompt: String,
  name: String? = parserClass.simpleName,
  model: ChatModels = ChatModels.GPT35Turbo,
  temperature: Double = 0.3,
  val parsingModel: ChatModels = ChatModels.GPT35Turbo,
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

  protected fun getParser(api: API) = ChatProxy(
    clazz = parserClass,
    api = (api as OpenAIClient),
    model = parsingModel,
    temperature = temperature,
  ).create()

  override fun answer(vararg messages: ApiModel.ChatMessage, input: List<String>, api: API): ParsedResponse<T> {
    return ParsedResponseImpl(*messages, api = api)
  }
}
