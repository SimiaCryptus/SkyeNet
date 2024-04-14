package com.simiacryptus.skyenet.webui.util

import com.simiacryptus.skyenet.AgentPatterns.displayMapInTabs
import com.simiacryptus.skyenet.webui.application.ApplicationInterface
import com.vladsch.flexmark.ext.tables.TablesExtension
import com.vladsch.flexmark.html.HtmlRenderer
import com.vladsch.flexmark.parser.Parser
import com.vladsch.flexmark.util.data.MutableDataSet

object MarkdownUtil {
    fun renderMarkdown(
        markdown: String,
        options: MutableDataSet = defaultOptions(),
        tabs: Boolean = true,
        ui: ApplicationInterface? = null,
    ): String {
        if (markdown.isBlank()) return ""
        val parser = Parser.builder(options).build()
        val renderer = HtmlRenderer.builder(options).build()
        val document = parser.parse(markdown)
        val html = renderer.render(document)
        val mermaidRegex =
            Regex("<pre[^>]*><code class=\"language-mermaid\">(.*?)</code></pre>", RegexOption.DOT_MATCHES_ALL)
        val matches = mermaidRegex.findAll(html)
        var htmlContent = html
        matches.forEach { match ->
            val mermaidCode = match.groups[1]!!.value
            val fixedMermaidCode = fixupMermaidCode(mermaidCode)
            val replacement = if (tabs) """
            |<div class="tabs-container">
            |  <div class="tabs">
            |    <button class="tab-button" data-for-tab="1">Diagram</button>
            |    <button class="tab-button" data-for-tab="2">Source</button>
            |  </div>
            |  <div class="tab-content active" data-tab="1"><pre class="mermaid">$fixedMermaidCode</pre></div>
            |  <div class="tab-content" data-tab="2"><pre><code class="language-mermaid">$fixedMermaidCode</code></pre></div>
            |</div>
            |""".trimMargin() else """
            |<pre class="mermaid">$fixedMermaidCode</pre>
            |""".trimMargin()
            htmlContent = htmlContent.replace(match.value, replacement)
        }
        //language=HTML
        return if (tabs) {
            displayMapInTabs(
                mapOf(
                    "HTML" to htmlContent,
                    "Markdown" to """<pre><code class="language-markdown">${
                        markdown.replace(Regex("<"), "&lt;").replace(Regex(">"), "&gt;")
                    }</code></pre>""",
                    "Hide" to "",
                ), ui = ui
            )
        } else htmlContent
    }

    private fun fixupMermaidCode(code: String): String {
        // Regex to find Mermaid labels that might need fixing
        val labelRegexes = listOf(
            Regex("""\[([^"][^\[\]]*)\]"""),
        )
        val nodeIdRegexes = listOf(
            // start(Start)
            """(?<![+-a-zA-Z0-9_])([a-zA-Z0-9_]+)\([^)]*\)""".toRegex(),
            // checkAPIProvider{Check API Provider}
            """([a-zA-Z0-9_]+)\{[^\{\}]*\}""".toRegex(),
            // checkAPIProvider -->
            """([a-zA-Z0-9_]+)\s*(-->|\*--)""".toRegex(),
            // --> A
            """(-->|\*--)\s*([a-zA-Z0-9_]+)""".toRegex(),
            // parseResponse["Parse Response"]
            """([a-zA-Z0-9_]+)\["[^\[\]]*"\]""".toRegex(),
            // chatCounter: AtomicInteger
            """([a-zA-Z0-9_]+):""".toRegex(),
        )

        // Function to fix individual labels
        fun fixLabel(match: MatchResult): String {
            val label = match.groups[1]!!.value.trim()
            return match.value.replace(label, "\"${label.replace("\"", "'")}\"")
        }

        fun fixNodeId(match: MatchResult): String {
            val nodeId = match.groups[1]!!.value
            return match.value.replace(nodeId, "`${nodeId.trim()}`")
        }

        var replace = code
        for (regex in labelRegexes) {
            replace = regex.replace(replace, ::fixLabel)
        }
        for (regex in nodeIdRegexes) {
            replace = regex.replace(replace, ::fixNodeId)
        }
        return replace
    }

    private fun defaultOptions(): MutableDataSet {
        val options = MutableDataSet()
        options.set(Parser.EXTENSIONS, listOf(TablesExtension.create()))
        return options
    }
}