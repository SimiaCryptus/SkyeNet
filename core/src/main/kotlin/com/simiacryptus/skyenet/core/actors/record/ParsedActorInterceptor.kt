package com.simiacryptus.skyenet.core.actors.record

import com.simiacryptus.jopenai.API
import com.simiacryptus.jopenai.models.OpenAIModel
import com.simiacryptus.skyenet.core.actors.ParsedActor
import com.simiacryptus.skyenet.core.actors.ParsedResponse
import com.simiacryptus.skyenet.core.util.FunctionWrapper

class ParsedActorInterceptor<T:Any>(
    val inner: ParsedActor<T>,
    private val functionInterceptor: FunctionWrapper,
) : ParsedActor<T>(
    parserClass = inner.parserClass,
    prompt = inner.prompt,
    action = inner.action,
    model = inner.model,
    temperature = inner.temperature,
) {
    override fun answer(vararg messages: com.simiacryptus.jopenai.ApiModel.ChatMessage, api: API) =
        functionInterceptor.wrap(messages.toList().toTypedArray()) {
            inner.answer(*it, api = api)
        }

    override fun response(
        vararg messages: com.simiacryptus.jopenai.ApiModel.ChatMessage,
        model: OpenAIModel,
        api: API
    ) = functionInterceptor.wrap(messages.toList().toTypedArray(), model) {
        messages: Array<com.simiacryptus.jopenai.ApiModel.ChatMessage>,
        model: OpenAIModel ->
            inner.response(*messages, model = model, api = api)
    }

    override fun answer(vararg questions: String, api: API) = functionInterceptor.wrap(questions) {
        inner.answer(*it, api = api)
    }

}