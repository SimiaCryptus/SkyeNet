package com.simiacryptus.skyenet.webui.util

import com.vladsch.flexmark.ext.tables.TablesExtension
import com.vladsch.flexmark.html.HtmlRenderer
import com.vladsch.flexmark.parser.Parser
import com.vladsch.flexmark.util.data.MutableDataSet
import org.intellij.lang.annotations.Language

object MarkdownUtil {
  fun renderMarkdown(markdown: String, options: MutableDataSet = defaultOptions(), tabs : Boolean = true): String {
    if (markdown.isBlank()) return ""
    val parser = Parser.builder(options).build()
    val renderer = HtmlRenderer.builder(options).build()
    val document = parser.parse(markdown)
    val html = renderer.render(document)
    @Language("HTML") val htmlContent = html.replace(
      Regex("<pre[^>]*><code class=\"language-mermaid\">(.*?)</code></pre>", RegexOption.DOT_MATCHES_ALL),
      if(tabs) """
        |<div class="tabs-container">
        |  <div class="tabs">
        |    <button class="tab-button" data-for-tab="1">Diagram</button>
        |    <button class="tab-button" data-for-tab="2">Source</button>
        |  </div>
        |  <div class="tab-content active" data-tab="1"><pre class="mermaid">$1</pre></div>
        |  <div class="tab-content" data-tab="2"><pre><code class="language-mermaid">$1</code></pre></div>
        |</div>
        |""".trimMargin() else """
        |<pre class="mermaid">$1</pre>
        |""".trimMargin()
    )
    //language=HTML
    return if(tabs) """
      |<div class="tabs-container">
      |    <div class="tabs">
      |        <button class="tab-button" data-for-tab="1">HTML</button>
      |        <button class="tab-button" data-for-tab="2">Markdown</button>
      |        <button class="tab-button" data-for-tab="3">Hide</button>
      |    </div>
      |    <div class="tab-content active" data-tab="1">$htmlContent</div>
      |    <div class="tab-content" data-tab="2">
      |        <pre><code class="language-markdown">${markdown.replace(Regex("<"), "&lt;").replace(Regex(">"), "&gt;")}</code></pre>
      |    </div>
      |</div>
    """.trimMargin() else htmlContent
  }

    private fun defaultOptions(): MutableDataSet {
        val options = MutableDataSet()
        options.set(Parser.EXTENSIONS, listOf(TablesExtension.create()))
        return options
    }
}