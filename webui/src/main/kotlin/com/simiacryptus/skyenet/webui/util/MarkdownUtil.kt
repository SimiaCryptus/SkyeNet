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

    // Simplified parsing states
    enum class State {
        DEFAULT, IN_NODE, IN_EDGE, IN_LABEL, IN_KEYWORD
    }
    fun fixupMermaidCode(code: String): String {
        val stringBuilder = StringBuilder()
        var index = 0


        var currentState = State.DEFAULT
        var labelStart = -1
        val keywords = listOf("graph", "subgraph", "end", "classDef", "class", "click", "style")

        while (index < code.length) {
            when (currentState) {
                State.DEFAULT -> {
                    if (code.startsWith(keywords.find { code.startsWith(it, index) } ?: "", index)) {
                        // Start of a keyword
                        currentState = State.IN_KEYWORD
                        stringBuilder.append(code[index])
                    } else
                    if (code[index] == '[' || code[index] == '(' || code[index] == '{') {
                        // Possible start of a label
                        currentState = State.IN_LABEL
                        labelStart = index
                    } else if (code[index].isWhitespace() || code[index] == '-') {
                        // Continue in default state, possibly an edge
                        stringBuilder.append(code[index])
                    } else {
                        // Start of a node
                        currentState = State.IN_NODE
                        stringBuilder.append(code[index])
                    }
                }
                State.IN_KEYWORD -> {
                    if (code[index].isWhitespace()) {
                        // End of a keyword
                        currentState = State.DEFAULT
                    }
                    stringBuilder.append(code[index])
                }
                State.IN_NODE -> {
                    if (code[index] == '-' || code[index] == '>' || code[index].isWhitespace()) {
                        // End of a node, start of an edge or space
                        currentState = if (code[index].isWhitespace()) State.DEFAULT else State.IN_EDGE
                        stringBuilder.append(code[index])
                    } else {
                        // Continue in node
                        stringBuilder.append(code[index])
                    }
                }
                State.IN_EDGE -> {
                    if (!code[index].isWhitespace() && code[index] != '-' && code[index] != '>') {
                        // End of an edge, start of a node
                        currentState = State.IN_NODE
                        stringBuilder.append(code[index])
                    } else {
                        // Continue in edge
                        stringBuilder.append(code[index])
                    }
                }
                State.IN_LABEL -> {
                    if (code[index] == ']' || code[index] == ')' || code[index] == '}') {
                        // End of a label
                        val label = code.substring(labelStart + 1, index)
                        val escapedLabel = "\"${label.replace("\"", "'")}\""
                        stringBuilder.append(escapedLabel)
                        stringBuilder.append(code[index])
                        currentState = State.DEFAULT
                    }
                }
            }
            index++
        }

        return stringBuilder.toString()
    }
    private fun defaultOptions(): MutableDataSet {
        val options = MutableDataSet()
        options.set(Parser.EXTENSIONS, listOf(TablesExtension.create()))
        return options
    }

    private val log = org.slf4j.LoggerFactory.getLogger(MarkdownUtil::class.java)
}