package com.simiacryptus.skyenet.body

import com.simiacryptus.openai.OpenAIClient
import com.vladsch.flexmark.html.HtmlRenderer
import com.vladsch.flexmark.parser.Parser
import com.vladsch.flexmark.util.data.MutableDataSet

open class ChatSessionFlexmark(
    parent: SkyenetSessionServerBase,
    model: OpenAIClient.Model = OpenAIClient.Models.GPT35Turbo,
    sessionId: String,
    visiblePrompt: String,
    hiddenPrompt: String,
    systemPrompt: String,
) : ChatSession(
    parent,
    model,
    sessionId,
    visiblePrompt,
    hiddenPrompt,
    systemPrompt
) {

    override fun renderResponse(response: String): String {
        val options = flexmarkOptions()
        val parser = Parser.builder(options).build()
        val renderer = HtmlRenderer.builder(options).build()
        val document = parser.parse(response)
        return renderer.render(document)
    }

    open fun flexmarkOptions() = MutableDataSet()

}