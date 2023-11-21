package com.simiacryptus.skyenet.actors.record

import com.simiacryptus.openai.OpenAIAPI
import com.simiacryptus.openai.OpenAIClient
import com.simiacryptus.openai.models.OpenAIModel
import com.simiacryptus.skyenet.actors.ParsedActor
import com.simiacryptus.skyenet.actors.ParsedResponse
import com.simiacryptus.skyenet.util.FunctionWrapper

class ParsedActorInterceptor<T:Any>(
    val inner: ParsedActor<T>,
    val functionInterceptor: FunctionWrapper,
) : ParsedActor<T>(
    parserClass = inner.parserClass,
    prompt = inner.prompt,
    action = inner.action,
    model = inner.model,
    temperature = inner.temperature,
) {
    private inner class ParsedResponseInterceptor(vararg messages: OpenAIClient.ChatMessage, api: OpenAIAPI, private val inner: ParsedResponse<T>) :
        ParsedResponse<T>(this@ParsedActorInterceptor.inner.resultClass) {
        override fun getText() = functionInterceptor.wrap { inner.getText() }
        override fun getObj(clazz: Class<T>) = functionInterceptor.intercept(clazz) { inner.getObj(clazz) } // <-- Cannot use 'T' as reified type parameter. Use a class instead.
    }

    override fun answer(vararg messages: OpenAIClient.ChatMessage, api: OpenAIAPI): ParsedResponse<T> {
        return functionInterceptor.wrap(messages.toList().toTypedArray()) {
            messages: Array<OpenAIClient.ChatMessage> ->
            ParsedResponseInterceptor(*messages, api = api, inner = inner.answer(*messages, api = api))
        }
    }

    override fun response(
        vararg messages: OpenAIClient.ChatMessage,
        model: OpenAIModel,
        api: OpenAIAPI
    ) = functionInterceptor.wrap(messages.toList().toTypedArray(), model) {
        messages: Array<OpenAIClient.ChatMessage>,
        model: OpenAIModel ->
            inner.response(*messages, model = model, api = api)
    }

    override fun answer(vararg questions: String, api: OpenAIAPI) = functionInterceptor.wrap(questions) {
        inner.answer(*it, api = api)
    }

    override fun chatMessages(vararg questions: String) = functionInterceptor.wrap(questions) {
        inner.chatMessages(*it)
    }
}