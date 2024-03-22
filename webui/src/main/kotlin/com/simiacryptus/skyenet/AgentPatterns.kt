package com.simiacryptus.skyenet

import com.simiacryptus.jopenai.API
import com.simiacryptus.jopenai.ApiModel
import com.simiacryptus.jopenai.ApiModel.Role
import com.simiacryptus.jopenai.util.ClientUtil.toContentList
import com.simiacryptus.skyenet.core.actors.BaseActor
import com.simiacryptus.skyenet.webui.application.ApplicationInterface
import com.simiacryptus.skyenet.webui.session.SessionTask
import com.simiacryptus.skyenet.webui.util.MarkdownUtil.renderMarkdown
import java.util.concurrent.Callable
import java.util.concurrent.Semaphore
import java.util.concurrent.atomic.AtomicBoolean
import java.util.concurrent.atomic.AtomicReference

object AgentPatterns {

  fun displayMapInTabs(
    map: Map<String, String>,
  ) = """
    <div class="tabs-container">
    <div class="tabs">${
    map.keys.joinToString("\n") { key ->
      """<button class="tab-button" data-for-tab="$key">$key</button>"""
    }
  }</div>
    ${
    map.entries.withIndex().joinToString("\n") { (idx, t) ->
      val (key, value) = t
      """<div class="tab-content ${
        when {
          idx == 0 -> "active"
          else -> ""
        }
      }" data-tab="$key">$value</div>"""
    }
  }
    </div>
  """.trimIndent()

  open class Retryable(
    val ui: ApplicationInterface,
    val task: SessionTask,
    open val process: (StringBuilder) -> String,
  ) {
    val container = task.add("<div class=\"tabs-container\"></div>")
    private val history = mutableListOf<String>()
    fun newHTML(ui: ApplicationInterface): String = """
    <div class="tabs-container">
      <div class="tabs">
      ${
      history.withIndex().joinToString("\n") { (index, _) ->
        val tabId = "$index"
        """<button class="tab-button" data-for-tab="$tabId">${index + 1}</button>"""
      }
    }
      ${
      ui.hrefLink("♻") {
        val idx = history.size
        history.add("Retrying...")
        container?.clear()
        container?.append(newHTML(ui))
        task.add("")
        val newResult = process(container!!)
        history.removeAt(idx)
        addTab(ui, newResult, idx)
      }
    }
      </div>
      ${
      history.withIndex().joinToString("\n") { (index, content) ->
        """
          <div class="tab-content${if (index == history.size - 1) " active" else ""}" data-tab="$index">
            $content
          </div>
        """.trimIndent()
      }
    }
    </div>
  """.trimIndent()

    fun addTab(ui: ApplicationInterface, content: String, idx: Int = history.size): String {
      history.add(idx, content)
      container?.clear()
      container?.append(newHTML(ui))
      task.complete()
      return content
    }
  }

  fun retryable(
    ui: ApplicationInterface,
    task: SessionTask = ui.newTask(),
    process: (StringBuilder) -> String,
  ) = object : Retryable(ui, task, process){
    init {
      addTab(ui, process(container!!))
    }
  }
  private fun List<Pair<List<ApiModel.ContentPart>, Role>>.toMessageList(): Array<ApiModel.ChatMessage> =
    this.map { (content, role) -> ApiModel.ChatMessage(role = role, content = content) }.toTypedArray()

  fun <T : Any> iterate(
    ui: ApplicationInterface,
    userMessage: String,
    heading: String = renderMarkdown(userMessage),
    initialResponse: (String) -> T,
    reviseResponse: (List<Pair<String, Role>>) -> T,
    outputFn: (T) -> String = { design -> renderMarkdown(design.toString()) },
    task: SessionTask = ui.newTask()
  ): T {
    val atomicRef = AtomicReference<T>()
    val semaphore = Semaphore(0)
    return object : Callable<T> {
      val tabs = mutableListOf<StringBuilder>()
      val options = mutableListOf<T>()
      val tabContainer = task.add("<div class=\"tabs-container\"></div>")
      val acceptGuard = AtomicBoolean(false)
      val feedbackGuard = AtomicBoolean(false)

      fun main(tabIndex: Int = tabs.size) {
        try {
          val history = mutableListOf<Pair<String, Role>>()
          history.add(userMessage to Role.user)
          var design = initialResponse(userMessage)
          options.add(design)
          history.add(outputFn(design) to Role.assistant)
          var textInput: String? = null
          var acceptLink: String? = null
          fun feedbackForm() = """
                  |<div style="display: flex; flex-direction: column;">
                  |${acceptLink!!}
                  |</div>
                  |${textInput!!}
                """.trimMargin()
          textInput = ui.textInput { userResponse ->
            if (feedbackGuard.getAndSet(true)) return@textInput
            val markdown = renderMarkdown(userResponse)
            history.add(markdown to Role.user)
            task.complete()
            design = reviseResponse(history + listOf(userResponse to Role.user))
            if (tabs.size > tabIndex) tabs[tabIndex].append(markdown + "\n" + outputFn(design) + "\n" + feedbackForm())
            else tabs.add(StringBuilder(markdown + "\n" + outputFn(design) + "\n" + feedbackForm()))
            tabContainer?.clear()
            tabContainer?.append(newHTML(tabIndex).trimIndent())
            task.complete()
            feedbackGuard.set(false)
          }
          val idx = tabs.size
          acceptLink = "<!-- START ACCEPT -->" +
              ui.hrefLink("\uD83D\uDC4D") {
                val tab = tabs[tabIndex]
                val prevTab = tab.toString()
                tab.clear()
                tab.append(
                  prevTab.substringBefore("<!-- START ACCEPT -->") + "<!-- ACCEPTED -->" + prevTab.substringAfter(
                    "<!-- END ACCEPT -->"
                  )
                )
                tabContainer?.clear()
                tabContainer?.append(newHTML(tabIndex).trimIndent())
                task.complete()
                accept(acceptGuard, design, idx)
              } + "<!-- END ACCEPT -->"
          if (tabs.size > tabIndex) tabs[tabIndex].append(outputFn(design) + "\n" + feedbackForm())
          else tabs.add(StringBuilder(outputFn(design) + "\n" + feedbackForm()))
          tabContainer?.clear()
          tabContainer?.append(newHTML(idx).trimIndent())
          task.complete()
        } catch (e: Throwable) {
          task.error(ui, e)
          task.complete(ui.hrefLink("🔄 Retry") {
            main()
          })
        }
      }

      // Adjust the logic to handle the replaceTabIndex parameter properly
      private fun newHTML(idx: Int = tabs.size - 1, replaceTabIndex: Int? = null): String {
        replaceTabIndex?.let { tabs.removeAt(it) }
        return """
        <div class="tabs-container">
          <div class="tabs">
          ${
          tabs.withIndex().joinToString("\n") { (index, _) ->
            """<button class="tab-button" data-for-tab="$index">${index + 1}</button>"""
          }
        }
          ${
          ui.hrefLink("♻") {
            tabs.add(StringBuilder("Retrying..."))
            tabContainer?.clear()
            tabContainer?.append(newHTML(idx))
            task.add("")
            main(idx)
          }
        } 
          </div>
          ${
          tabs.withIndex().joinToString("\n") { (index, content) ->
            """
                <div class="tab-content${if (index == idx) " active" else ""}" data-tab="$index">
                $content
                </div>
              """.trimIndent()
          }
        }
        </div>
        """
      }

      private fun accept(
        acceptGuard: AtomicBoolean, design: T, idx: Int
      ) {
        if (acceptGuard.getAndSet(true)) return
        tabContainer?.clear()
        tabContainer?.append(
          """
          <div class="tabs-container">
            <div class="tabs">${
            tabs.indices.joinToString(separator = "\n") { index ->
              "<button class=\"tab-button\" data-for-tab=\"$index\">${index + 1}</button>"
            }
          }</div>${
            tabs.withIndex().joinToString(separator = "\n") { (index, content) ->
              "<div class=\"tab-content${
                when {
                  index == idx -> " active"
                  else -> ""
                }
              }\" data-tab=\"$index\">$content</div>"
            }
          }
          </div>
          """.trimIndent())
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

    }.call()

  }


  fun <I : Any, T : Any> iterate(
    input: String,
    heading: String = renderMarkdown(input),
    actor: BaseActor<I, T>,
    toInput: (String) -> I,
    api: API,
    ui: ApplicationInterface,
    outputFn: (T) -> String = { design -> renderMarkdown(design.toString()) },
    task: SessionTask = ui.newTask()
  ) = iterate(
    ui = ui,
    userMessage = input,
    heading = heading,
    initialResponse = { actor.answer(toInput(it), api = api) },
    reviseResponse = { userMessages: List<Pair<String, Role>> ->
      actor.respond(
        messages = (userMessages.map { ApiModel.ChatMessage(it.second, it.first.toContentList()) }.toTypedArray()),
        input = toInput(input),
        api = api
      )
    },
    outputFn = outputFn,
    task = task
  )

}