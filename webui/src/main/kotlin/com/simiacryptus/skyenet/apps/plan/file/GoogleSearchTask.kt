package com.simiacryptus.skyenet.apps.plan.file

import com.simiacryptus.jopenai.API
import com.simiacryptus.jopenai.describe.Description
import com.simiacryptus.skyenet.apps.plan.*
import com.simiacryptus.skyenet.util.MarkdownUtil
import com.simiacryptus.skyenet.webui.session.SessionTask
import org.slf4j.LoggerFactory
import java.net.URI
import java.net.URLEncoder
import java.net.http.HttpClient
import java.net.http.HttpRequest
import java.net.http.HttpResponse
import com.fasterxml.jackson.databind.ObjectMapper
import com.fasterxml.jackson.module.kotlin.readValue
import com.simiacryptus.util.JsonUtil

class GoogleSearchTask(
  planSettings: PlanSettings,
  planTask: GoogleSearchTaskData?
) : AbstractTask<GoogleSearchTask.GoogleSearchTaskData>(planSettings, planTask) {
  class GoogleSearchTaskData(
    @Description("The search query to use for Google search")
    val search_query: String,
    @Description("The number of results to return (max 10)")
    val num_results: Int = 5,
    task_description: String? = null,
    task_dependencies: List<String>? = null,
    state: TaskState? = null,
  ) : PlanTaskBase(
    task_type = TaskType.GoogleSearch.name,
    task_description = task_description,
    task_dependencies = task_dependencies,
    state = state
  )

  override fun promptSegment() = """
GoogleSearch - Search Google for web results
** Specify the search query
** Specify the number of results to return (max 10)
""".trimMargin()

  override fun run(
    agent: PlanCoordinator,
    messages: List<String>,
    task: SessionTask,
    api: API,
    resultFn: (String) -> Unit
  ) {
    val searchResults = performGoogleSearch()
    val formattedResults = formatSearchResults(searchResults)
    task.add(MarkdownUtil.renderMarkdown(formattedResults, ui = agent.ui))
    resultFn(formattedResults)
  }

  private fun performGoogleSearch(): String {
    val client = HttpClient.newBuilder().build()
    val encodedQuery = URLEncoder.encode(planTask?.search_query, "UTF-8")
    val uriBuilder = "https://www.googleapis.com/customsearch/v1?key=${planSettings.googleApiKey}&cx=${planSettings.googleSearchEngineId}&q=$encodedQuery&num=${planTask?.num_results}"
    
    val request = HttpRequest.newBuilder()
      .uri(URI.create(uriBuilder))
      .GET()
      .build()

    val response = client.send(request, HttpResponse.BodyHandlers.ofString())
    if (response.statusCode() != 200) {
      throw RuntimeException("Google API request failed with status ${response.statusCode()}: ${response.body()}")
    }
    return response.body()
  }

  private fun formatSearchResults(results: String): String {
    val mapper = ObjectMapper()
    val searchResults: Map<String, Any> = mapper.readValue(results)
    return buildString {
      appendLine("# Google Search Results")
      appendLine()
      val items = searchResults["items"] as List<Map<String, Any>>?
      items?.forEachIndexed { index, item ->
        appendLine("# ${index + 1}. [${item["title"]}](${item["link"]})")
        appendLine("${item["htmlSnippet"]}")
        appendLine("Pagemap:")
        appendLine("```json")
        appendLine(JsonUtil.toJson(item["pagemap"] ?: ""))
        appendLine("```")
        appendLine()
      } ?: appendLine("No results found.")
    }
  }

  companion object {
    private val log = LoggerFactory.getLogger(GoogleSearchTask::class.java)
  }
}