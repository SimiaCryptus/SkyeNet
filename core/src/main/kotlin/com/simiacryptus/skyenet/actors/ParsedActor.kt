package com.simiacryptus.skyenet.actors

import com.simiacryptus.openai.OpenAIClient
import com.simiacryptus.openai.proxy.ChatProxy
import java.util.function.Function

class ParsedActor<T>(
    val parserClass: Class<out Function<String, T>>,
    prompt: String,
    val action: String? = null,
    api: OpenAIClient = OpenAIClient(),
    model: OpenAIClient.Models = OpenAIClient.Models.GPT35Turbo,
    temperature: Double = 0.3,
) : BaseActor<ParsedResponse<T>>(
    api = api,
    prompt = prompt,
    name = action,
    model = model,
    temperature = temperature,
) {
    private val parser: Function<String, T> = ChatProxy(
        clazz = parserClass,
        api = api,
        model = OpenAIClient.Models.GPT35Turbo,
        temperature = temperature,
    ).create()
    private inner class ParsedResponseImpl(vararg messages: OpenAIClient.ChatMessage) : ParsedResponse<T> {
        private val _text: String by lazy { response(*messages).choices.first().message?.content ?: throw RuntimeException("No response") }
        private val _obj: T by lazy { parser.apply(getText()) }
        override fun getText(): String = _text
        override fun getObj(): T = _obj
    }

    override fun answer(vararg messages: OpenAIClient.ChatMessage): ParsedResponse<T> {
        return ParsedResponseImpl(*messages)
    }
}

