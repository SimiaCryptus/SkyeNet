package com.simiacryptus.skyenet

import com.simiacryptus.jopenai.ApiModel
import com.simiacryptus.skyenet.webui.application.ApplicationInterface
import com.simiacryptus.skyenet.webui.session.SessionTask
import com.simiacryptus.skyenet.webui.util.MarkdownUtil
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
  private val reviseResponse: (List<Pair<String, ApiModel.Role>>) -> T,
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
          main(idx)
        }
      } 
        </div>
      """.trimIndent()
    }
  }
  private val acceptGuard = AtomicBoolean(false)

  fun main(tabIndex: Int = tabs.size) {
    try {
      val history = mutableListOf<Pair<String, ApiModel.Role>>()
      history.add(userMessage to ApiModel.Role.user)
      var design = initialResponse(userMessage)
      history.add(outputFn(design) to ApiModel.Role.assistant)
      var acceptLink: String? = null
      var textInput: String? = null
      val feedbackGuard = AtomicBoolean(false)
      fun feedbackForm() = """
              |<div style="display: flex; flex-direction: column;">
              |${acceptLink!!}
              |</div>
              |${textInput!!}
            """.trimMargin()
      textInput = ui.textInput { userResponse ->
        if (feedbackGuard.getAndSet(true)) return@textInput
        try {
          val tab = tabs[tabs.label(tabIndex)] ?: tabs.set(tabs.label(tabIndex), "")
          val prevTab = tab.toString()
          tab.set(
            prevTab.substringBefore("<!-- START ACCEPT -->")
                + "<!-- ACCEPTED -->"
                + prevTab.substringAfter("<!-- END ACCEPT -->")
                + MarkdownUtil.renderMarkdown(userResponse)
          )
          history.add(MarkdownUtil.renderMarkdown(userResponse) to ApiModel.Role.user)
          tabs.update()
          task.add("")
          design = reviseResponse(history + listOf(userResponse to ApiModel.Role.user))
          if (tabs.size > tabIndex) {
            tabs.update()
          } else {
            tabs.set(
              tabs.label(tabIndex),
              MarkdownUtil.renderMarkdown(userResponse) + "\n" + outputFn(design) + "\n" + feedbackForm()
            )
          }
        } finally {
          feedbackGuard.set(false)
        }
      }
      acceptLink = "<!-- START ACCEPT -->" +
          ui.hrefLink("\uD83D\uDC4D") {
            if (acceptGuard.getAndSet(true)) {
              return@hrefLink
            }
            try {
              tabs[tabs.label(tabIndex)]?.apply {
                val prevTab = toString()
                val newValue =
                  prevTab.substringBefore("<!-- START ACCEPT -->") + "<!-- ACCEPTED -->" + prevTab.substringAfter(
                    "<!-- END ACCEPT -->"
                  )
                set(newValue)
                tabs.update()
              } ?: tabs.set(tabs.label(tabIndex), "Tab $tabIndex not found")
            } catch (e: Exception) {
              task.error(ui, e)
              acceptGuard.set(false)
              throw e
            }
            atomicRef.set(design)
            semaphore.release()
          } + "<!-- END ACCEPT -->"
      if (tabs.size > tabIndex) {
        tabs[tabs.label(tabIndex)]!!.append(outputFn(design) + "\n" + feedbackForm())
        tabs.update()
      } else {
        tabs[tabs.label(tabs.size)] = outputFn(design) + "\n" + feedbackForm()
      }
    } catch (e: Throwable) {
      task.error(ui, e)
      task.complete(ui.hrefLink("🔄 Retry") {
        main()
      })
    }
  }

  override fun call(): T {
    task.echo(heading)
    main()
    semaphore.acquire()
    val result = atomicRef.get()
    return result
  }

}

fun java.lang.StringBuilder.set(newValue: String) {
  clear()
  append(newValue)
}
