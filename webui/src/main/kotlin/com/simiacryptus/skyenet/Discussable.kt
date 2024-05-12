package com.simiacryptus.skyenet

import com.simiacryptus.jopenai.ApiModel.Role
import com.simiacryptus.skyenet.webui.application.ApplicationInterface
import com.simiacryptus.skyenet.webui.session.SessionTask
import com.simiacryptus.skyenet.webui.util.MarkdownUtil.renderMarkdown
import java.util.concurrent.Callable
import java.util.concurrent.Semaphore
import java.util.concurrent.atomic.AtomicBoolean
import java.util.concurrent.atomic.AtomicReference

class Discussable<T : Any>(
    private val task: SessionTask,
    private val userMessage: () -> String,
    private val initialResponse: (String) -> T,
    private val outputFn: (T) -> String,
    private val ui: ApplicationInterface,
    private val reviseResponse: (List<Pair<String, Role>>) -> T,
    private val atomicRef: AtomicReference<T> = AtomicReference(),
    private val semaphore: Semaphore = Semaphore(0),
    private val heading: String
) : Callable<T> {

    val tabs = object : TabbedDisplay(task) {
        override fun renderTabButtons() = """
            |<div class="tabs">
            |${
            tabs.withIndex().joinToString("\n")
            { (index: Int, t: Pair<String, StringBuilder>) ->
                """<button class="tab-button" data-for-tab="$index">${t.first}</button>"""
            }
        }
            |${
            ui.hrefLink("♻") {
                val idx: Int = size
                val newTask = ui.newTask(false)
                val header = newTask.header("Retrying...")
                this[label(idx)] = newTask.placeholder
                main(idx, newTask)
                this.selectedTab = idx
                header?.clear()
                newTask.complete()
            }
        } 
            |</div>
          """.trimMargin()
    }
    private val acceptGuard = AtomicBoolean(false)

    private fun main(tabIndex: Int, task: SessionTask) {
        try {
            val history = mutableListOf<Pair<String, Role>>()
            val userMessage = userMessage()
            history.add(userMessage to Role.user)
            val design = initialResponse(userMessage)
            val rendered = outputFn(design)
            history.add(rendered to Role.assistant)
            val tabContent = task.add(rendered)!!
            val feedbackForm = feedbackForm(tabIndex, tabContent, design, history, task)
            tabContent?.append("\n" + feedbackForm.placeholder)
            task.complete()
        } catch (e: Throwable) {
            task.error(ui, e)
            task.complete(ui.hrefLink("🔄 Retry") {
                main(tabIndex = tabIndex, task = task)
            })
        }
    }

    private fun feedbackForm(
        tabIndex: Int?,
        tabContent: StringBuilder,
        design: T,
        history: List<Pair<String, Role>>,
        task: SessionTask,
    ) = ui.newTask(false).apply {
        val feedbackSB = add("<div />")!!
        feedbackSB.clear()
        feedbackSB.append(
            """
          |<div style="display: flex; flex-direction: column;">
          |${acceptLink(tabIndex, tabContent, design, feedbackSB, feedbackTask = this)}
          |</div>
          |${textInput(design, tabContent, history, task, feedbackSB, feedbackTask = this)}
        """.trimMargin()
        )
        complete()
    }

    private fun acceptLink(
        tabIndex: Int?,
        tabContent: StringBuilder,
        design: T,
        feedbackSB: StringBuilder,
        feedbackTask: SessionTask,
    ) = ui.hrefLink("Accept", classname = "href-link cmd-button") {
        feedbackSB.clear()
        feedbackTask.complete()
        accept(tabIndex, tabContent, design)
    }

    private fun textInput(
        design: T,
        tabContent: StringBuilder,
        history: List<Pair<String, Role>>,
        task: SessionTask,
        feedbackSB: StringBuilder,
        feedbackTask: SessionTask,
    ): String {
        val feedbackGuard = AtomicBoolean(false)
        return ui.textInput { userResponse ->
            if (feedbackGuard.getAndSet(true)) return@textInput
            val prev = feedbackSB.toString()
            try {
                feedbackSB.clear()
                feedbackTask.complete()
                feedback(tabContent, userResponse, history, design, task)
            } catch (e: Exception) {
                task.error(ui, e)
                feedbackSB.set(prev)
                feedbackTask.complete()
                throw e
            } finally {
                feedbackGuard.set(false)
            }
        }
    }

    private fun feedback(
        tabContent: StringBuilder,
        userResponse: String,
        history: List<Pair<String, Role>>,
        design: T,
        task: SessionTask,
    ) {
        var history = history
        history = history + (userResponse to Role.user)
        val newValue = (tabContent.toString()
                        + "<div class=\"user-message\">"
                        + renderMarkdown(userResponse, ui = ui)
                        + "</div>")
        tabContent.set(newValue)
        val stringBuilder = task.add("Processing...")
        tabs.update()
        val newDesign = reviseResponse(history)
        val newTask = ui.newTask(root = false)
        tabContent.set(newValue + "\n" + newTask.placeholder)
        tabs.update()
        stringBuilder?.clear()
        task.complete()
        Retryable(ui, newTask) {
            outputFn(newDesign) + "\n" + feedbackForm(
                tabIndex = null,
                tabContent = it,
                design = newDesign,
                history = history,
                task = newTask
            ).placeholder
        }.apply {
            set(label(size), process(container))
        }
    }

    private fun accept(tabIndex: Int?, tabContent: StringBuilder, design: T) {
        if (acceptGuard.getAndSet(true)) {
            return
        }
        try {
            if (null != tabIndex) tabs.selectedTab = tabIndex
            tabContent.apply {
                val prevTab = toString()
                set(prevTab)
                tabs.update()
            }
        } catch (e: Exception) {
            task.error(ui, e)
            acceptGuard.set(false)
            throw e
        }
        atomicRef.set(design)
        semaphore.release()
    }

    override fun call(): T {
        task.echo(heading)
        val idx = tabs.size
        val newTask = ui.newTask(false)
        val header = newTask.header("Processing...")
        tabs[tabs.label(idx)] = newTask.placeholder
        main(idx, newTask)
        header?.clear()
        newTask.complete()
        semaphore.acquire()
        return atomicRef.get()
    }

}

fun java.lang.StringBuilder.set(newValue: String) {
    clear()
    append(newValue)
}
