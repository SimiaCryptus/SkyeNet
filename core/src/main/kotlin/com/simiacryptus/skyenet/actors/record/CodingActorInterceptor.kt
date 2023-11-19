package com.simiacryptus.skyenet.actors.record

import com.simiacryptus.openai.OpenAIClient
import com.simiacryptus.openai.models.OpenAIModel
import com.simiacryptus.skyenet.actors.CodeResult
import com.simiacryptus.skyenet.actors.CodingActor
import com.simiacryptus.skyenet.util.FunctionWrapper

class CodingActorInterceptor(
    val inner: CodingActor,
    val functionInterceptor: FunctionWrapper,
) : CodingActor(
    interpreterClass = inner.interpreterClass,
    symbols = inner.symbols,
    describer = inner.describer,
    name = inner.name,
    details = inner.details,
    model = inner.model,
    fallbackModel = inner.fallbackModel,
    temperature = inner.temperature,
    autoEvaluate = inner.autoEvaluate,
) {
    override fun answer(vararg messages: OpenAIClient.ChatMessage, api: OpenAIClient): CodeResult {
        return functionInterceptor.wrap(messages.toList().toTypedArray()) {
            messages: Array<OpenAIClient.ChatMessage> ->
            CodingResultInterceptor(*messages, api = api, inner = inner.answer(*messages, api = api))
        }
    }

    private inner class CodingResultInterceptor(
        vararg messages: OpenAIClient.ChatMessage,
        private val inner: CodeResult,
        api: OpenAIClient,
    ) : CodeResult {
        override fun getStatus() = functionInterceptor.wrap { inner.getStatus() }
        override fun getCode() = functionInterceptor.wrap { inner.getCode() }
        override fun run() = functionInterceptor.wrap { inner.run() }

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

    override fun answerWithPrefix(
        codePrefix: String,
        vararg messages: OpenAIClient.ChatMessage,
        api: OpenAIClient
    ) = functionInterceptor.wrap(messages.toList().toTypedArray(), codePrefix) {
        messages: Array<OpenAIClient.ChatMessage>,
        codePrefix: String ->
            inner.answerWithPrefix(codePrefix, *messages, api = api)
    }

    override fun answerWithAutoEval(
        vararg messages: String,
        api: OpenAIClient,
        codePrefix: String
    ) = functionInterceptor.wrap(messages.toList().toTypedArray(), codePrefix) {
        messages: Array<String>,
        codePrefix: String ->
            inner.answerWithAutoEval(*messages, api = api, codePrefix = codePrefix)
    }

    override fun answerWithAutoEval(
        vararg messages: OpenAIClient.ChatMessage,
        api: OpenAIClient
    ) = functionInterceptor.wrap(messages.toList().toTypedArray()) {
        inner.answerWithAutoEval(*messages, api = api)
    }
}