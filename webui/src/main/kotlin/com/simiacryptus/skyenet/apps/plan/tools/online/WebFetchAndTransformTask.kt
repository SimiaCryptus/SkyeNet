package com.simiacryptus.skyenet.apps.plan.tools.online

import com.simiacryptus.jopenai.API
import com.simiacryptus.jopenai.ChatClient
import com.simiacryptus.jopenai.OpenAIClient
import com.simiacryptus.jopenai.describe.Description
import com.simiacryptus.skyenet.apps.plan.*
import com.simiacryptus.skyenet.core.actors.SimpleActor
import com.simiacryptus.skyenet.util.MarkdownUtil
import com.simiacryptus.skyenet.webui.session.SessionTask
import org.apache.hc.client5.http.classic.methods.HttpGet
import org.apache.hc.client5.http.impl.classic.HttpClients
import org.apache.hc.core5.http.io.entity.EntityUtils
import org.jsoup.Jsoup
import org.jsoup.nodes.Document
import org.jsoup.select.NodeFilter
import org.slf4j.LoggerFactory

open class WebFetchAndTransformTask(
  planSettings: PlanSettings,
  planTask: WebFetchAndTransformTaskConfigData?
) : AbstractTask<WebFetchAndTransformTask.WebFetchAndTransformTaskConfigData>(planSettings, planTask) {
  class WebFetchAndTransformTaskConfigData(
    @Description("The URL to fetch")
    val url: String,
    @Description("The desired format or focus for the transformation")
    val transformationGoal: String,
    task_description: String? = null,
    task_dependencies: List<String>? = null,
    state: TaskState? = null,
  ) : TaskConfigBase(
    task_type = TaskType.WebFetchAndTransform.name,
    task_description = task_description,
    task_dependencies = task_dependencies,
    state = state
  )

  override fun promptSegment() = """
        WebFetchAndTransform - Fetch a web page, strip HTML, and transform content
        ** Specify the URL to fetch
        ** Specify the desired format or focus for the transformation
    """.trimIndent()

  override fun run(
    agent: PlanCoordinator,
    messages: List<String>,
    task: SessionTask,
    api: ChatClient,
    resultFn: (String) -> Unit,
    api2: OpenAIClient,
    planSettings: PlanSettings
  ) {
    val fetchedContent = fetchAndStripHtml(taskConfig?.url ?: "")
    val transformedContent = transformContent(fetchedContent, taskConfig?.transformationGoal ?: "", api, planSettings)
    task.add(MarkdownUtil.renderMarkdown(transformedContent, ui = agent.ui))
    resultFn(transformedContent)
  }

  private fun fetchAndStripHtml(url: String): String {
    HttpClients.createDefault().use { httpClient ->
      val httpGet = HttpGet(url)
      httpClient.execute(httpGet).use { response ->
        val entity = response.entity
        val content = EntityUtils.toString(entity)
        return scrubHtml(content)
      }
    }
  }

  private fun transformContent(content: String, transformationGoal: String, api: API, planSettings: PlanSettings): String {
    val prompt = """
            Transform the following web content according to this goal: $transformationGoal
            
            Content:
            $content
            
            Transformed content:
        """.trimIndent()
    return SimpleActor(
      prompt = prompt,
      model = planSettings.defaultModel,
    ).answer(
      listOf(
        "Transform the following web content according to this goal: $transformationGoal\n\n$content",
      ), api
    )
  }

  companion object {
    private val log = LoggerFactory.getLogger(WebFetchAndTransformTask::class.java)
    fun scrubHtml(str: String, maxLength: Int = 100 * 1024): String {
      val document: Document = Jsoup.parse(str)
      // Remove unnecessary elements, attributes, and optimize the document
      document.apply {
        if (document.body().html().length > maxLength) return@apply
        select("script, style, link, meta, iframe, noscript").remove() // Remove unnecessary and potentially harmful tags
        outputSettings().prettyPrint(false) // Disable pretty printing for compact output
        if (document.body().html().length > maxLength) return@apply
        // Remove comments
        select("*").forEach { it.childNodes().removeAll { node -> node.nodeName() == "#comment" } }
        if (document.body().html().length > maxLength) return@apply
        // Remove data-* attributes
        select("*[data-*]").forEach { it.attributes().removeAll { attr -> attr.key.startsWith("data-") } }
        if (document.body().html().length > maxLength) return@apply
        select("*").forEach { element ->
          val importantAttributes = setOf("href", "src", "alt", "title", "width", "height", "style", "class", "id", "name")
          element.attributes().removeAll { it.key !in importantAttributes }
        }
        if (document.body().html().length > maxLength) return@apply
        // Remove empty elements
        select("*").filter { node, depth ->
          if(node.text().isBlank() && node.attributes().isEmpty() && !node.hasAttr("img")) NodeFilter.FilterResult.REMOVE else NodeFilter.FilterResult.CONTINUE
        }
        if (document.body().html().length > maxLength) return@apply
        // Unwrap single-child elements with no attributes
        select("*").forEach { element ->
          if (element.childNodes().size == 1 && element.childNodes()[0].nodeName() == "#text" && element.attributes().isEmpty()) {
            element.unwrap()
          }
        }
        if (document.body().html().length > maxLength) return@apply
        // Convert relative URLs to absolute
        select("[href],[src]").forEach { element ->
          element.attr("href").let { href -> element.attr("href", href) }
          element.attr("src").let { src -> element.attr("src", src) }
        }
        if (document.body().html().length > maxLength) return@apply
        // Remove empty attributes
        select("*").forEach { element ->
          element.attributes().removeAll { it.value.isBlank() }
        }
      }

      // Truncate if necessary
      val result = document.body().html()
      return if (result.length > maxLength) {
        result.substring(0, maxLength)
      } else {
        result
      }
    }
  }
}