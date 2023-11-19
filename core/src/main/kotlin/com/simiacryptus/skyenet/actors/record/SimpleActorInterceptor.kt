package com.simiacryptus.skyenet.actors.record

import com.simiacryptus.openai.OpenAIClient
import com.simiacryptus.openai.models.OpenAIModel
import com.simiacryptus.skyenet.actors.SimpleActor
import com.simiacryptus.skyenet.util.FunctionWrapper

class SimpleActorInterceptor(
    val inner: SimpleActor,
    val functionInterceptor: FunctionWrapper,
) : SimpleActor(
    prompt = inner.prompt,
    name = inner.name,
    model = inner.model,
    temperature = inner.temperature,
) {

    override fun answer(vararg messages: OpenAIClient.ChatMessage, api: OpenAIClient) =
        functionInterceptor.wrap(messages.toList().toTypedArray()) {
            messages: Array<OpenAIClient.ChatMessage> ->
            inner.answer(*messages, api = api)
        }

    override fun response(
        vararg messages: OpenAIClient.ChatMessage,
        model: OpenAIModel,
        api: OpenAIClient
    ) = functionInterceptor.wrap(messages.toList().toTypedArray(), model) {
        messages: Array<OpenAIClient.ChatMessage>,
        model: OpenAIModel ->
            inner.response(*messages, model = model, api = api)
    }

    override fun chatMessages(vararg questions: String) = functionInterceptor.wrap(questions) {
        inner.chatMessages(*it)
    }

    override fun answer(vararg questions: String, api: OpenAIClient) = functionInterceptor.wrap(questions) {
        inner.answer(*it, api = api)
    }
}