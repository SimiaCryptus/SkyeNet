package com.simiacryptus.skyenet.core.actors.record

import com.simiacryptus.jopenai.API
import com.simiacryptus.jopenai.models.ChatModels
import com.simiacryptus.jopenai.models.OpenAIModel
import com.simiacryptus.skyenet.core.actors.ParsedActor
import com.simiacryptus.skyenet.core.actors.ParsedResponse
import com.simiacryptus.skyenet.core.util.FunctionWrapper
import java.util.function.Function

class ParsedActorInterceptor(
    val inner: ParsedActor<*>,
    private val functionInterceptor: FunctionWrapper,
) : ParsedActor<Any>(
  parserClass = inner.parserClass as Class<out Function<String, Any>>,
  prompt = inner.prompt,
  name = inner.name,
  model = inner.model,
  temperature = inner.temperature,
  ChatModels.GPT35Turbo,
) {

    override fun answer(vararg messages: com.simiacryptus.jopenai.ApiModel.ChatMessage, input: List<String>, api: API): ParsedResponse<Any> {
      val textResponse = functionInterceptor.wrap(messages.toList().toTypedArray()) {
        inner.answer(*it, input = input, api = api).text
      }
      return object : ParsedResponse<Any>(resultClass) {
        private val parser: Function<String, Any> = getParser(api)

        private val _obj: Any by lazy { parse() }

        private fun parse(): Any = functionInterceptor.inner.intercept(text, resultClass) { parser.apply(text) }
        override val text get() = textResponse
        override val obj get() = _obj
      }
    }

    override fun response(
        vararg input: com.simiacryptus.jopenai.ApiModel.ChatMessage,
        model: OpenAIModel,
        api: API
    ) = functionInterceptor.wrap(input.toList().toTypedArray(), model) {
        messages, model -> inner.response(*messages, model = model, api = api)
    }

}