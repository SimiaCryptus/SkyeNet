package com.simiacryptus.skyenet.webui.util

import com.vladsch.flexmark.ext.tables.TablesExtension
import com.vladsch.flexmark.html.HtmlRenderer
import com.vladsch.flexmark.parser.Parser
import com.vladsch.flexmark.util.data.MutableDataSet

object MarkdownUtil {
    fun renderMarkdown(response: String, options: MutableDataSet = defaultOptions()): String {
        val parser = Parser.builder(options).build()
        val renderer = HtmlRenderer.builder(options).build()
        val document = parser.parse(response)
        return renderer.render(document)
    }

    private fun defaultOptions(): MutableDataSet {
        val options = MutableDataSet()
        options.set(Parser.EXTENSIONS, listOf(TablesExtension.create()))
        return options
    }
}