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
      val history = mutableListOf<Pair<String, Role>>()
      history.add(userMessage to Role.user)
      var design = initialResponse(userMessage)
      history.add(outputFn(design) to Role.assistant)
      var acceptLink: String? = null
      var textInput: String? = null
      val feedbackGuard = AtomicBoolean(false)
      fun feedbackForm() = """
              |<div style="display: flex; flex-direction: column;">
              |${acceptLink!!}
              |</div>
              |${textInput!!}
            """.trimMargin()

      val tabLabel = tabs.label(tabIndex)
      val tabContent = tabs[tabLabel] ?: tabs.set(tabLabel, "")
      textInput = ui.textInput { userResponse ->
        if (feedbackGuard.getAndSet(true)) return@textInput
        try {
          val prevValue = tabContent.toString()
          val newValue = (prevValue.substringBefore("<!-- START ACCEPT -->")
              + "<!-- ACCEPTED -->"
              + prevValue.substringAfter("<!-- END ACCEPT -->")
              + renderMarkdown(userResponse))
          tabContent.set(newValue)
          history.add(renderMarkdown(userResponse) to Role.user)
          task.add("") // Show spinner
          tabs.update()
          design = reviseResponse(history + listOf(userResponse to Role.user))
          task.complete()
          tabContent.set(renderMarkdown(newValue) + "\n" + outputFn(design) + "\n" + feedbackForm())
          tabs.update()
        } catch (e: Exception) {
          task.error(ui, e)
          throw e
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
              tabs.selectedTab = tabIndex
              tabContent?.apply {
                val prevTab = toString()
                val newValue =
                  prevTab.substringBefore("<!-- START ACCEPT -->") + "<!-- ACCEPTED -->" + prevTab.substringAfter(
                    "<!-- END ACCEPT -->"
                  )
                set(newValue)
                tabs.update()
              } ?: throw IllegalStateException("Tab $tabIndex not found")
            } catch (e: Exception) {
              task.error(ui, e)
              acceptGuard.set(false)
              throw e
            }
            atomicRef.set(design)
            semaphore.release()
          } + "<!-- END ACCEPT -->"
      if (tabs.size > tabIndex) {
        tabContent?.append(outputFn(design) + "\n" + feedbackForm())
      } else {
        tabContent?.set(outputFn(design) + "\n" + feedbackForm())
      }
      tabs.update()
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
