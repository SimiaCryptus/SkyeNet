package com.simiacryptus.skyenet.actors.record

import com.simiacryptus.jopenai.API
import com.simiacryptus.jopenai.OpenAIClient
import com.simiacryptus.jopenai.models.OpenAIModel
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
    override fun answer(vararg messages: com.simiacryptus.jopenai.ApiModel.ChatMessage, api: API): CodeResult {
        return functionInterceptor.wrap(messages.toList().toTypedArray()) {
            messages: Array<com.simiacryptus.jopenai.ApiModel.ChatMessage> ->
            CodingResultInterceptor(*messages, api = (api as OpenAIClient), inner = inner.answer(*messages, api = api))
        }
    }

    private inner class CodingResultInterceptor(
        vararg messages: com.simiacryptus.jopenai.ApiModel.ChatMessage,
        private val inner: CodeResult,
        api: OpenAIClient,
    ) : CodeResult {
        override fun getStatus() = functionInterceptor.wrap { inner.getStatus() }
        override fun getCode() = functionInterceptor.wrap { inner.getCode() }
        override fun run() = functionInterceptor.wrap { inner.run() }

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

    override fun chatMessages(vararg questions: String) = functionInterceptor.wrap(questions) {
        inner.chatMessages(*it)
    }

    override fun answer(vararg questions: String, api: API) = functionInterceptor.wrap(questions) {
        inner.answer(*it, api = api)
    }

    override fun answerWithPrefix(
        codePrefix: String,
        vararg messages: com.simiacryptus.jopenai.ApiModel.ChatMessage,
        api: API
    ) = functionInterceptor.wrap(messages.toList().toTypedArray(), codePrefix) {
        messages: Array<com.simiacryptus.jopenai.ApiModel.ChatMessage>,
        codePrefix: String ->
            inner.answerWithPrefix(codePrefix, *messages, api = api)
    }

    override fun answerWithAutoEval(
        vararg messages: String,
        api: API,
        codePrefix: String
    ) = functionInterceptor.wrap(messages.toList().toTypedArray(), codePrefix) {
        messages: Array<String>,
        codePrefix: String ->
            inner.answerWithAutoEval(*messages, api = api, codePrefix = codePrefix)
    }

    override fun answerWithAutoEval(
        vararg messages: com.simiacryptus.jopenai.ApiModel.ChatMessage,
        api: API
    ) = functionInterceptor.wrap(messages.toList().toTypedArray()) {
        inner.answerWithAutoEval(*messages, api = api)
    }
}