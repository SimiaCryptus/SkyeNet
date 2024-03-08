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
       val html = renderer.render(document)
       // Basic Mermaid block detection and wrapping
       val enhancedHtml = html.replace(Regex("<pre[^>]*><code class=\"language-mermaid\">(.*?)</code></pre>", RegexOption.DOT_MATCHES_ALL), "<pre class=\"mermaid\">$1</pre>")
       return enhancedHtml
    }

    private fun defaultOptions(): MutableDataSet {
        val options = MutableDataSet()
        options.set(Parser.EXTENSIONS, listOf(TablesExtension.create()))
        return options
    }
}