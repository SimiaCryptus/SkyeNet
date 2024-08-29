package com.simiacryptus.skyenet.apps.general

import com.simiacryptus.jopenai.API
import com.simiacryptus.skyenet.TabbedDisplay
import com.simiacryptus.skyenet.core.platform.Session
import com.simiacryptus.skyenet.core.platform.User
import com.simiacryptus.skyenet.webui.application.ApplicationInterface
import com.simiacryptus.skyenet.webui.application.ApplicationServer
import com.simiacryptus.skyenet.webui.session.SessionTask
import com.simiacryptus.skyenet.webui.util.MarkdownUtil
import org.slf4j.LoggerFactory
import kotlin.random.Random

class StressTestApp(
    applicationName: String = "UI Stress Test",
    path: String = "/stressTest",
) : ApplicationServer(
    applicationName = applicationName,
    path = path,
    showMenubar = true
) {
    override fun userMessage(
        session: Session,
        user: User?,
        userMessage: String,
        ui: ApplicationInterface,
        api: API
    ) {
        val task = ui.newTask()
        task.add(MarkdownUtil.renderMarkdown("# UI Stress Test", ui=ui))
        
        // Create nested tabs
        createNestedTabs(task, ui, 3)

    }

    private fun createNestedTabs(task: SessionTask, ui: ApplicationInterface, depth: Int) {
        if (depth <= 0) {
            // Create a complex diagram
            createComplexDiagram(task, ui)

            // Create multiple placeholders and update them
            createAndUpdatePlaceholders(task, ui)
            return
        }

        val tabDisplay = object : TabbedDisplay(task) {
            override fun renderTabButtons(): String {
                return buildString {
                    append("<div class='tabs'>\n")
                    (1..3).forEach { i ->
                        append("<label class='tab-button' data-for-tab='$i'>Tab $i</label>\n")
                    }
                    append("</div>")
                }
            }
        }

        (1..3).forEach { i ->
            val subTask = ui.newTask(false)
            tabDisplay["Tab $i"] = subTask.placeholder
            createNestedTabs(subTask, ui, depth - 1)
        }
    }

    private fun createComplexDiagram(task: SessionTask, ui: ApplicationInterface) {
        val mermaidDiagram = """
            ```mermaid
            graph TD
                A[Start] --> B{Is it?}
                B -->|Yes| C[OK]
                C --> D[Rethink]
                D --> B
                B ---->|No| E[End]
            ```
        """.trimIndent()
        
        task.add(MarkdownUtil.renderMarkdown("## Complex Diagram\n$mermaidDiagram", ui=ui))
    }

    private fun createAndUpdatePlaceholders(task: SessionTask, ui: ApplicationInterface) {
        val placeholders = (1..5).map { ui.newTask(false) }
        
        placeholders.forEach { placeholder ->
            task.add(placeholder.placeholder)
        }

        repeat(10) { iteration ->
            placeholders.forEach { placeholder ->
                val content = "Placeholder content: Iteration $iteration, Random: ${Random.nextInt(100)}"
                placeholder.add(MarkdownUtil.renderMarkdown(content, ui=ui))
                //Thread.sleep(50)
            }
        }
        placeholders.forEach { it.complete() }
    }

    companion object {
        private val log = LoggerFactory.getLogger(StressTestApp::class.java)
    }
}