package com.simiacryptus.skyenet

import com.simiacryptus.jopenai.ApiModel
import com.simiacryptus.skyenet.webui.application.ApplicationInterface
import com.simiacryptus.skyenet.webui.session.SessionTask
import com.simiacryptus.skyenet.webui.util.MarkdownUtil
import java.util.concurrent.Callable
import java.util.concurrent.Semaphore
import java.util.concurrent.atomic.AtomicBoolean
import java.util.concurrent.atomic.AtomicReference

class Iteratable<T : Any>(
  private val task: SessionTask,
  private val userMessage: String,
  private val initialResponse: (String) -> T,
  private val outputFn: (T) -> String,
  private val ui: ApplicationInterface,
  private val reviseResponse: (List<Pair<String, ApiModel.Role>>) -> T,
  private val atomicRef: AtomicReference<T>,
  private val semaphore: Semaphore,
  private val heading: String
) : Callable<T> {
  val tabs = object : TabbedDisplay(task) {
    override fun render(): String {
      return """
      <div class="tabs-container">
        <div class="tabs">
        ${
        tabs.withIndex()
          .joinToString("\n") { (index: Int, t: Pair<String, StringBuilder>) ->
            """<button class="tab-button" data-for-tab="$index">${t.first}</button>"""
          }
      }
        ${
        ui.hrefLink("♻") {
          val idx: Int = size
          set(label(idx), "Retrying...")
          main(idx)
        }
      } 
        </div>
        ${
        tabs.withIndex().joinToString("\n") { (index, content) ->
            """
              <div class="tab-content${
              if (index == (size - 1).coerceIn(
                  0,
                  size - 1
                )
              ) " active" else ""
            }" data-tab="$index">
              ${content.second}
              </div>
            """.trimIndent()
          }
      }
      </div>
      """
    }
  }
  val options = mutableListOf<T>()
  private val acceptGuard = AtomicBoolean(false)
  private val feedbackGuard = AtomicBoolean(false)

  fun main(tabIndex: Int = tabs.size) {
    try {
      val history = mutableListOf<Pair<String, ApiModel.Role>>()
      history.add(userMessage to ApiModel.Role.user)
      var design = initialResponse(userMessage)
      options.add(design)
      history.add(outputFn(design) to ApiModel.Role.assistant)
      var acceptLink: String? = null
      var textInput: String? = null
      fun feedbackForm() = """
              |<div style="display: flex; flex-direction: column;">
              |${acceptLink!!}
              |</div>
              |${textInput!!}
            """.trimMargin()
      textInput = ui.textInput { userResponse ->
        if (feedbackGuard.getAndSet(true)) return@textInput
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
        feedbackGuard.set(false)
      }
      val idx = tabs.size
      acceptLink = "<!-- START ACCEPT -->" +
          ui.hrefLink("\uD83D\uDC4D") {
            val tab = tabs[tabs.label(tabIndex)] ?: tabs.set(tabs.label(tabIndex), "")
            val prevTab = tab.toString()
            tab.set(
              prevTab.substringBefore("<!-- START ACCEPT -->") + "<!-- ACCEPTED -->" + prevTab.substringAfter(
                "<!-- END ACCEPT -->"
              )
            )
            accept(acceptGuard, design, idx)
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

  private fun accept(
    acceptGuard: AtomicBoolean, design: T, idx: Int
  ) {
    if (acceptGuard.getAndSet(true)) return
    val finalTxt = """
      <div class="tabs-container">
        <div class="tabs">${
      tabs.tabs.withIndex().joinToString(separator = "\n") { (index, t) ->
        val (key, _) = t
        "<button class=\"tab-button\" data-for-tab=\"$index\">${key}</button>"
      }
    }</div>${
      tabs.tabs.withIndex()
        .joinToString(separator = "\n") { (index, content) ->
          "<div class=\"tab-content${
            when {
              index == idx.coerceIn(0, tabs.size - 1) -> " active"
              else -> ""
            }
          }\" data-tab=\"$index\">${content.second}</div>"
        }
    }
      </div>
      """.trimIndent()
    //tabs.clear()
    //tabs.addTab("Accepted", finalTxt)
    tabs["Accepted"]?.apply { set(finalTxt) }
      ?: tabs.set("Accepted", finalTxt)
    task.complete()
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
