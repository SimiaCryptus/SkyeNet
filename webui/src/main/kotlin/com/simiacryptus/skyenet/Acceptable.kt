package com.simiacryptus.skyenet

import com.simiacryptus.jopenai.ApiModel.Role
import com.simiacryptus.skyenet.webui.application.ApplicationInterface
import com.simiacryptus.skyenet.webui.session.SessionTask
import com.simiacryptus.skyenet.webui.util.MarkdownUtil.renderMarkdown
import java.util.concurrent.Callable
import java.util.concurrent.Semaphore
import java.util.concurrent.atomic.AtomicBoolean
import java.util.concurrent.atomic.AtomicReference

class Acceptable<T : Any>(
  private val task: SessionTask,
  private val userMessage: String,
  private val initialResponse: (String) -> T,
  private val outputFn: (T) -> String,
  private val ui: ApplicationInterface,
  private val reviseResponse: (List<Pair<String, Role>>) -> T,
  private val atomicRef: AtomicReference<T> = AtomicReference(),
  private val semaphore: Semaphore = Semaphore(0),
  private val heading: String
) : Callable<T> {

  val tabs = object : TabbedDisplay(task) {
    override fun renderTabButtons(): String {
      return """
        <div class="tabs">
        ${
        tabs.withIndex().joinToString("\n")
        { (index: Int, t: Pair<String, StringBuilder>) ->
          """<button class="tab-button" data-for-tab="$index">${t.first}</button>"""
        }
      }
        ${
        ui.hrefLink("♻") {
          val idx: Int = size
          set(label(idx), "Retrying...")
          task.add("")
          main(idx, this@Acceptable.task)
        }
      } 
        </div>
      """.trimIndent()
    }
  }
  private val acceptGuard = AtomicBoolean(false)

  fun main(tabIndex: Int = tabs.size, task: SessionTask = this.task) {
    try {
      val history = mutableListOf<Pair<String, Role>>()
      history.add(userMessage to Role.user)
      val design = initialResponse(userMessage)
      history.add(outputFn(design) to Role.assistant)
      val tabLabel = tabs.label(tabIndex)
      val tabContent = tabs[tabLabel] ?: tabs.set(tabLabel, "")

      if (tabs.size > tabIndex) {
        tabContent.append(outputFn(design) + "\n" + feedbackForm(tabIndex, tabContent, design, history, task))
      } else {
        tabContent.set(outputFn(design) + "\n" + feedbackForm(tabIndex, tabContent, design, history, task))
      }
      tabs.update()
    } catch (e: Throwable) {
      task.error(ui, e)
      task.complete(ui.hrefLink("🔄 Retry") {
        main(task = task)
      })
    }
  }

  private fun feedbackForm(
    tabIndex: Int?,
    tabContent: StringBuilder,
    design: T,
    history: List<Pair<String, Role>>,
    task: SessionTask,
  ): String = """
              |<!-- START ACCEPT -->
              |<div style="display: flex; flex-direction: column;">
              |${acceptLink(tabIndex, tabContent, design)!!}
              |</div>
              |${textInput(design, tabContent, history, task)!!}
              |<!-- END ACCEPT -->
            """.trimMargin()

  private fun acceptLink(
    tabIndex: Int?,
    tabContent: StringBuilder,
    design: T,
  ) = ui.hrefLink("\uD83D\uDC4D") {
    accept(tabIndex, tabContent, design)
  }

  private fun textInput(
    design: T,
    tabContent: StringBuilder,
    history: List<Pair<String, Role>>,
    task: SessionTask,
  ): String {
    val feedbackGuard = AtomicBoolean(false)
    return ui.textInput { userResponse ->
      if (feedbackGuard.getAndSet(true)) return@textInput
      try {
        feedback(tabContent, userResponse, history, design, task)
      } catch (e: Exception) {
        task.error(ui, e)
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
    val prevValue = tabContent.toString()
    val newValue = (prevValue.substringBefore("<!-- START ACCEPT -->")
        + "<!-- ACCEPTED -->"
        + prevValue.substringAfter("<!-- END ACCEPT -->")
        + "<div class=\"user-message\">"
        + renderMarkdown(userResponse)
        + "</div>")
    tabContent.set(newValue)
    history = history + (renderMarkdown(userResponse) to Role.user)
    task.add("") // Show spinner
    tabs.update()
    val newDesign = reviseResponse(history)
    val newTask = ui.newTask()
    tabContent.set(newValue + "\n" + newTask.placeholder)
    tabs.update()
    task.complete()
    Retryable(ui, newTask) {
      outputFn(newDesign) + "\n" + feedbackForm(
        tabIndex = null,
        tabContent = it,
        design = newDesign,
        history = history,
        task = newTask
      )
    }.apply {
      set(label(size), process(container))
    }
  }

  private fun accept(tabIndex: Int?, tabContent: StringBuilder, design: T) {
    if (acceptGuard.getAndSet(true)) {
      return
    }
    try {
      if(null != tabIndex) tabs.selectedTab = tabIndex
      tabContent?.apply {
        val prevTab = toString()
        val newValue =
          prevTab.substringBefore("<!-- START ACCEPT -->") + "<!-- ACCEPTED -->" + prevTab.substringAfter(
            "<!-- END ACCEPT -->"
          )
        set(newValue)
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
    main()
    semaphore.acquire()
    return atomicRef.get()
  }

}

fun java.lang.StringBuilder.set(newValue: String) {
  clear()
  append(newValue)
}
