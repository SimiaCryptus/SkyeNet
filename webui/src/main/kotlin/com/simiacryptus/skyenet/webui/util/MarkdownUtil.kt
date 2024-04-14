package com.simiacryptus.skyenet.webui.util

import com.simiacryptus.skyenet.AgentPatterns.displayMapInTabs
import com.simiacryptus.skyenet.webui.application.ApplicationInterface
import com.vladsch.flexmark.ext.tables.TablesExtension
import com.vladsch.flexmark.html.HtmlRenderer
import com.vladsch.flexmark.parser.Parser
import com.vladsch.flexmark.util.data.MutableDataSet
import org.apache.commons.text.StringEscapeUtils
import java.nio.file.Files

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
            var mermaidCode = match.groups[1]!!.value
            // HTML Decode mermaidCode
            mermaidCode = mermaidCode
            val fixedMermaidCode = fixupMermaidCode(mermaidCode)
            var mermaidDiagramHTML = """<pre class="mermaid">$fixedMermaidCode</pre>"""
            try {
                mermaidDiagramHTML = """${renderMermaidToSVG(fixedMermaidCode)}"""
            } catch (e: Exception) {
                log.warn("Failed to render Mermaid diagram", e)
            }
            val replacement = if (tabs) """
            |<div class="tabs-container">
            |  <div class="tabs">
            |    <button class="tab-button" data-for-tab="1">Diagram</button>
            |    <button class="tab-button" data-for-tab="2">Source</button>
            |  </div>
            |  <div class="tab-content active" data-tab="1">$mermaidDiagramHTML</div>
            |  <div class="tab-content" data-tab="2"><pre><code class="language-mermaid">$fixedMermaidCode</code></pre></div>
            |</div>
            |""".trimMargin() else """
            |$mermaidDiagramHTML
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

    var MMDC_CMD: List<String> = listOf("powershell", "mmdc")
    private fun renderMermaidToSVG(mermaidCode: String): String {
        // mmdc -i input.mmd -o output.svg
        val tempInputFile = Files.createTempFile("mermaid", ".mmd").toFile()
        val tempOutputFile = Files.createTempFile("mermaid", ".svg").toFile()
        tempInputFile.writeText(StringEscapeUtils.unescapeHtml4(mermaidCode))
        val strings = MMDC_CMD + listOf("-i", tempInputFile.absolutePath, "-o", tempOutputFile.absolutePath)
        val processBuilder =
            ProcessBuilder(*strings.toTypedArray())
        processBuilder.redirectErrorStream(true)
        val process = processBuilder.start()
        val output = StringBuilder()
        val errorOutput = StringBuilder()
        process.inputStream.bufferedReader().use {
            it.lines().forEach { line -> output.append(line) }
        }
        process.errorStream.bufferedReader().use {
            it.lines().forEach { line -> errorOutput.append(line) }
        }
        process.waitFor()
        val svgContent = tempOutputFile.readText()
        tempInputFile.delete()
        tempOutputFile.delete()
        if (output.isNotEmpty()) {
            log.error("Mermaid CLI Output: $output")
        }
        if (errorOutput.isNotEmpty()) {
            log.error("Mermaid CLI Error: $errorOutput")
        }
        if(svgContent.isNullOrBlank()) {
            throw RuntimeException("Mermaid CLI failed to generate SVG")
        }
        return svgContent
    }

    private fun fixupMermaidCode(code: String): String {
        // Regex to find Mermaid labels that might need fixing
        val labelRegexes = listOf(
            Regex("""\[([^"][^\[\]]*)\]"""),
        )
        val nodeIdRegexes = listOf(
            // start(Start)
            """(?<![+\-"a-zA-Z0-9_])([a-zA-Z0-9_]+)\([^)]*\)""".toRegex(),
            // checkAPIProvider{Check API Provider}
            """([a-zA-Z0-9_]+)\{[^\{\}]*\}""".toRegex(),
            // checkAPIProvider -->
            """([a-zA-Z0-9_]+)\s*(?:\.\.>|:|-->|\*--)""".toRegex(),
            // --> A
            """(?:\.\.>|-->|\*--)\s*([a-zA-Z0-9_]+)""".toRegex(),
            // parseResponse["Parse Response"]
            """([a-zA-Z0-9_]+)\["[^\[\]]*"\]""".toRegex(),
        )

        // Function to fix individual labels
        fun fixLabel(match: MatchResult): String {
            val matchGroup = match.groups[1]!!
            val label = matchGroup.value.trim()
            val newValue = "\"${label.replace("\"", "'")}\""
            val range1 = matchGroup.range
            val range0 = match.range
            return match.value.let {
                val start = range1.first - range0.first
                val end = start + (range1.last - range1.first) + 1
                it.substring(0, start.coerceAtLeast(0)) + newValue + it.substring(end.coerceAtMost(it.length))
            }
        }

        fun fixNodeId(match: MatchResult): String {
            val matchGroup = match.groups[1]!!
            val nodeId = matchGroup.value
            val newValue = "`${nodeId.trim()}`"
            val range1 = matchGroup.range
            val range0 = match.range
            return match.value.let {
                val start = range1.first - range0.first
                val end = start + (range1.last - range1.first) + 1
                it.substring(0, start.coerceAtLeast(0)) + newValue + it.substring(end.coerceAtMost(it.length))
            }
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

    private val log = org.slf4j.LoggerFactory.getLogger(MarkdownUtil::class.java)
}