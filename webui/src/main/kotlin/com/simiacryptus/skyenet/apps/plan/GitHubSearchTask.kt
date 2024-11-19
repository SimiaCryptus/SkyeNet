package com.simiacryptus.skyenet.apps.plan

import com.fasterxml.jackson.databind.ObjectMapper
import com.fasterxml.jackson.module.kotlin.readValue
import com.simiacryptus.jopenai.ChatClient
import com.simiacryptus.jopenai.OpenAIClient
import com.simiacryptus.jopenai.describe.Description
import com.simiacryptus.skyenet.util.MarkdownUtil
import com.simiacryptus.skyenet.webui.session.SessionTask
import org.slf4j.LoggerFactory
import java.net.URI
import java.net.http.HttpClient
import java.net.http.HttpRequest
import java.net.http.HttpResponse

class GitHubSearchTask(
  planSettings: PlanSettings,
  planTask: GitHubSearchTaskConfigData?
) : AbstractTask<GitHubSearchTask.GitHubSearchTaskConfigData>(planSettings, planTask) {
  class GitHubSearchTaskConfigData(
    @Description("The search query to use for GitHub search")
    val search_query: String,
    @Description("The type of GitHub search to perform (code, commits, issues, repositories, topics, users)")
    val search_type: String,
    @Description("The number of results to return (max 100)")
    val per_page: Int = 30,
    @Description("Sort order for results")
    val sort: String? = null,
    @Description("Sort direction (asc or desc)")
    val order: String? = null,
    task_description: String? = null,
    task_dependencies: List<String>? = null,
    state: TaskState? = null,
  ) : TaskConfigBase(
    task_type = TaskType.GitHubSearch.name,
    task_description = task_description,
    task_dependencies = task_dependencies,
    state = state
  )

  override fun promptSegment() = """
GitHubSearch - Search GitHub for code, commits, issues, repositories, topics, or users
** Specify the search query
** Specify the type of search (code, commits, issues, repositories, topics, users)
** Specify the number of results to return (max 100)
** Optionally specify sort order (e.g. stars, forks, updated)
** Optionally specify sort direction (asc or desc)
""".trimMargin()

  override fun run(
    agent: PlanCoordinator,
    messages: List<String>,
    task: SessionTask,
    api: ChatClient,
    resultFn: (String) -> Unit,
    api2: OpenAIClient,
    planSettings: PlanSettings
  ) {
    val searchResults = performGitHubSearch(planSettings)
    val formattedResults = formatSearchResults(searchResults)
    task.add(MarkdownUtil.renderMarkdown(formattedResults, ui = agent.ui))
    resultFn(formattedResults)
  }

  private fun performGitHubSearch(planSettings: PlanSettings): String {
    val client = HttpClient.newBuilder().build()
    val uriBuilder = StringBuilder("https://api.github.com/search/${planTask?.search_type}?q=${planTask?.search_query}&per_page=${planTask?.per_page}")
    planTask?.sort?.let { uriBuilder.append("&sort=$it") }
    planTask?.order?.let { uriBuilder.append("&order=$it") }
    val request = HttpRequest.newBuilder()
      .uri(URI.create(uriBuilder.toString()))
      .header("Accept", "application/vnd.github+json")
      .header("Authorization", "Bearer ${planSettings.githubToken}")
      .header("X-GitHub-Api-Version", "2022-11-28")
      .GET()
      .build()

    val response = client.send(request, HttpResponse.BodyHandlers.ofString())
    if (response.statusCode() != 200) {
      throw RuntimeException("GitHub API request failed with status ${response.statusCode()}: ${response.body()}")
    }
    return response.body()
  }

  private fun formatSearchResults(results: String): String {
    val mapper = ObjectMapper()
    val searchResults: Map<String, Any> = mapper.readValue(results)
    return buildString {
      appendLine("# GitHub Search Results")
      appendLine()
      appendLine("Total results: ${searchResults["total_count"]}")
      appendLine()
      appendLine("## Top Results:")
      appendLine()
      val items = searchResults["items"] as List<Map<String, Any>>
      items.take(10).forEach { item ->
        when (planTask?.search_type) {
          "repositories" -> formatRepositoryResult(item)
          "code" -> formatCodeResult(item)
          "commits" -> formatCommitResult(item)
          "issues" -> formatIssueResult(item)
          "users" -> formatUserResult(item)
          "topics" -> formatTopicResult(item)
          else -> appendLine("- ${item["name"] ?: item["title"] ?: item["login"]}")
        }
        appendLine()
      }
    }
  }

  private fun StringBuilder.formatTopicResult(topic: Map<String, Any>) {
    appendLine("### [${topic["name"]}](${topic["url"]})")
    appendLine("${topic["short_description"]}")
    appendLine("Featured: ${topic["featured"]} | Curated: ${topic["curated"]}")
  }

  private fun StringBuilder.formatRepositoryResult(repo: Map<String, Any>) {
    appendLine("### ${repo["full_name"]}")
    appendLine("${repo["description"]}")
    appendLine("Stars: ${repo["stargazers_count"]} | Forks: ${repo["forks_count"]}")
    appendLine("[View on GitHub](${repo["html_url"]})")
  }

  private fun StringBuilder.formatCodeResult(code: Map<String, Any>) {
    val repo = code["repository"] as Map<String, Any>
    appendLine("### [${repo["full_name"]}](${code["html_url"]})")
    appendLine("File: ${code["path"]}")
    appendLine("```")
    appendLine(code["text_matches"]?.toString()?.take(200) ?: "")
    appendLine("```")
  }

  private fun StringBuilder.formatCommitResult(commit: Map<String, Any>) {
    val repo = commit["repository"] as Map<String, Any>
    appendLine("### [${repo["full_name"]}](${commit["html_url"]})")
    appendLine("${(commit["commit"] as Map<String, Any>)["message"]}")
    appendLine("Author: ${(commit["author"] as Map<String, Any>)["login"]} | Date: ${((commit["commit"] as Map<String, Any>)["author"] as Map<String, Any>)["date"]}")
  }

  private fun StringBuilder.formatIssueResult(issue: Map<String, Any>) {
    appendLine("### [${issue["title"]}](${issue["html_url"]})")
    appendLine("State: ${issue["state"]} | Comments: ${issue["comments"]}")
    appendLine("Created by ${(issue["user"] as Map<String, Any>)["login"]} on ${issue["created_at"]}")
  }

  private fun StringBuilder.formatUserResult(user: Map<String, Any>) {
    appendLine("### [${user["login"]}](${user["html_url"]})")
    appendLine("Type: ${user["type"]} | Repos: ${user["public_repos"]}")
    appendLine("![Avatar](${user["avatar_url"]})")
  }

  companion object {
    private val log = LoggerFactory.getLogger(GitHubSearchTask::class.java)
  }
}