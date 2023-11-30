package com.simiacryptus.skyenet.core.actors.record

import com.simiacryptus.jopenai.API
import com.simiacryptus.jopenai.models.ChatModels
import com.simiacryptus.jopenai.models.OpenAIModel
import com.simiacryptus.skyenet.core.actors.ParsedActor
import com.simiacryptus.skyenet.core.actors.ParsedResponse
import com.simiacryptus.skyenet.core.util.FunctionWrapper

class ParsedActorInterceptor(
    val inner: ParsedActor<*>,
    private val functionInterceptor: FunctionWrapper,
) : ParsedActor<Any>(
  parserClass = inner.parserClass as Class<java.util.function.Function<String,Any>>,
  prompt = inner.prompt,
  name = inner.name,
  model = inner.model,
  temperature = inner.temperature,
  ChatModels.GPT35Turbo,
) {

    override fun answer(vararg messages: com.simiacryptus.jopenai.ApiModel.ChatMessage, input: List<String>, api: API) =
        functionInterceptor.wrap(messages.toList().toTypedArray()) {
            inner.answer(*it, input=input, api = api)
        } as ParsedResponse<Any>

    override fun response(
        vararg input: com.simiacryptus.jopenai.ApiModel.ChatMessage,
        model: OpenAIModel,
        api: API
    ) = functionInterceptor.wrap(input.toList().toTypedArray(), model) {
        messages: Array<com.simiacryptus.jopenai.ApiModel.ChatMessage>,
        model: OpenAIModel ->
            inner.response(*messages, model = model, api = api)
    }

}