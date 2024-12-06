package com.simiacryptus.skyenet.apps.plan

import com.simiacryptus.jopenai.ChatClient
import com.simiacryptus.jopenai.OpenAIClient
import com.simiacryptus.jopenai.describe.Description
import com.simiacryptus.skyenet.core.util.Selenium
import com.simiacryptus.skyenet.util.Selenium2S3
import com.simiacryptus.skyenet.webui.session.SessionTask
import io.github.bonigarcia.wdm.WebDriverManager
import org.openqa.selenium.chrome.ChromeDriver
import org.openqa.selenium.chrome.ChromeOptions
import org.slf4j.LoggerFactory
import java.util.concurrent.ConcurrentHashMap

class SeleniumSessionTask(
  planSettings: PlanSettings,
  planTask: SeleniumSessionTaskConfigData?
) : AbstractTask<SeleniumSessionTask.SeleniumSessionTaskConfigData>(planSettings, planTask) {
  companion object {
    private val log = LoggerFactory.getLogger(SeleniumSessionTask::class.java)
    private val activeSessions = ConcurrentHashMap<String, Selenium>()
    private const val TIMEOUT_MS = 30000L // 30 second timeout
    private const val MAX_SESSIONS = 10 // Maximum number of concurrent sessions

    init {
      try {
        // Setup WebDriverManager for Chrome
        WebDriverManager.chromedriver().setup()
        // You can add support for other browsers as needed
        // WebDriverManager.firefoxdriver().setup()
        // WebDriverManager.edgedriver().setup()
        log.info("WebDriverManager initialized successfully")
      } catch (e: Exception) {
        log.error("Failed to initialize WebDriverManager or Selenium factory", e)
        throw IllegalStateException("Failed to initialize Selenium configuration", e)
      }
    }


    fun closeSession(sessionId: String) {
      activeSessions.remove(sessionId)?.let { session ->
        try {
          session.quit()
        } catch (e: Exception) {
          log.error("Error closing session $sessionId", e)
          session.forceQuit() // Add force quit as fallback
          throw e // Propagate exception after cleanup
        }
      }
    }

    private fun cleanupInactiveSessions() {
      activeSessions.entries.removeIf { (id, session) ->
        try {
          if (!session.isAlive()) {
            log.info("Removing inactive session $id")
            session.quit()
            true
          } else false
        } catch (e: Exception) {
          log.warn("Error checking session $id, removing", e)
          try {
            session.forceQuit()
          } catch (e2: Exception) {
            log.error("Failed to force quit session $id", e2)
          }
          true
        }
      }
    }

    fun closeAllSessions() {
      activeSessions.forEach { (id, session) ->
        try {
          session.quit()
        } catch (e: Exception) {
          log.error("Error closing session $id", e)
          try {
            session.forceQuit()
          } catch (e2: Exception) {
            log.error("Failed to force quit session $id", e2)
          }
        }
      }
      activeSessions.clear()
    }

    fun getActiveSessionCount(): Int = activeSessions.size
    private fun createErrorMessage(e: Exception, command: String): String = buildString {
      append("Error: ${e.message}\n")
      append(e.stackTrace.take(3).joinToString("\n"))
      append("\nFailed command: $command")
    }
  }

  class SeleniumSessionTaskConfigData(
    @Description("The URL to navigate to (optional if reusing existing session)")
    val url: String = "",
    @Description("JavaScript commands to execute")
    val commands: List<String> = listOf(),
    @Description("Session ID for reusing existing sessions")
    val sessionId: String? = null,
    @Description("Timeout in milliseconds for commands")
    val timeout: Long = TIMEOUT_MS,
    @Description("Whether to close the session after execution")
    val closeSession: Boolean = false,
    task_description: String? = null,
    task_dependencies: List<String>? = null,
    state: TaskState? = null,
  ) : TaskConfigBase(
    task_type = TaskType.SeleniumSession.name,
    task_description = task_description,
    task_dependencies = task_dependencies,
    state = state
  )

  override fun promptSegment(): String {
    val activeSessionsInfo = activeSessions.entries.joinToString("\n") { (id, session) ->
      "  ** Session $id: ${session.getCurrentUrl()}"
    }
    return """
SeleniumSession - Create and manage a stateful Selenium browser session
  * Specify the URL to navigate to
  * Provide JavaScript commands to execute in sequence
  * Can be used for web scraping, testing, or automation
  * Session persists between commands for stateful interactions
  * Optionally specify sessionId to reuse an existing session
  * Set closeSession=true to close the session after execution
Active Sessions:
$activeSessionsInfo
""".trimMargin()
  }

  override fun run(
    agent: PlanCoordinator,
    messages: List<String>,
    task: SessionTask,
    api: ChatClient,
    resultFn: (String) -> Unit,
    api2: OpenAIClient,
    planSettings: PlanSettings
  ) {
    val seleniumFactory: (pool: java.util.concurrent.ThreadPoolExecutor, cookies: Array<out jakarta.servlet.http.Cookie>?) -> com.simiacryptus.skyenet.core.util.Selenium =
      { pool, cookies ->
        val chromeOptions = ChromeOptions().apply {
          addArguments("--headless")
          addArguments("--disable-gpu")
          addArguments("--no-sandbox")
          addArguments("--disable-dev-shm-usage")
        }
        try {
          Selenium2S3(
            pool = pool,
            cookies = cookies,
            driver = ChromeDriver(chromeOptions)
          )
        } catch (e: Exception) {
          throw IllegalStateException("Failed to initialize Selenium", e)
        }
      }
    requireNotNull(planTask) { "SeleniumSessionTaskData is required" }
    var selenium: Selenium? = null
    try {
      // Cleanup inactive sessions before potentially creating new one
      cleanupInactiveSessions()
      // Check session limit
      if (activeSessions.size >= MAX_SESSIONS && planTask.sessionId == null) {
        throw IllegalStateException("Maximum number of concurrent sessions ($MAX_SESSIONS) reached")
      }
      selenium = planTask.sessionId?.let { id -> activeSessions[id] }
        ?: seleniumFactory(agent.pool, null).also { newSession ->
          planTask.sessionId?.let { id -> activeSessions[id] = newSession }
        }
      log.info("Starting Selenium session ${planTask.sessionId ?: "temporary"} for URL: ${planTask.url} with timeout ${planTask.timeout}ms")

      selenium.setScriptTimeout(planTask.timeout)

      // Navigate to initial URL
      // Navigate if URL is provided, regardless of whether it's a new or existing session
      if (planTask.url.isNotBlank()) {
        selenium.navigate(planTask.url)
      }

      // Execute each command in sequence
      val results = planTask.commands.map { command ->
        try {
          log.debug("Executing command: $command")
          val startTime = System.currentTimeMillis()
          val result = selenium.executeScript(command)?.toString() ?: "null"
          val duration = System.currentTimeMillis() - startTime
          log.debug("Command completed in ${duration}ms")
          result
        } catch (e: Exception) {
          log.error("Error executing command: $command", e)
          createErrorMessage(e, command)
        }
      }

      val result = formatResults(planTask, selenium, results)

      task.add(result)
      resultFn(result)
    } finally {
      // Close session if it's temporary or explicitly requested to be closed
      if ((planTask.sessionId == null || planTask.closeSession) && selenium != null) {
        log.info("Closing temporary session")
        try {
          selenium.quit()
          if (planTask.sessionId != null) {
            activeSessions.remove(planTask.sessionId)
          }
        } catch (e: Exception) {
          log.error("Error closing temporary session", e)
          selenium.forceQuit()
          if (planTask.sessionId != null) {
            activeSessions.remove(planTask.sessionId)
          }
        }
      }
    }
  }

  private fun formatResults(
    planTask: SeleniumSessionTaskConfigData,
    selenium: Selenium,
    results: List<String>
  ): String = buildString(capacity = 16384) { // Pre-allocate buffer for better performance
    appendLine("## Selenium Session Results")
    if (planTask.url.isNotBlank()) {
      appendLine("Initial URL: ${planTask.url}")
    }
    appendLine("Session ID: ${planTask.sessionId ?: "temporary"}")
    appendLine("Final URL: ${selenium.getCurrentUrl()}")
    appendLine("Timeout: ${planTask.timeout}ms")
    appendLine("Browser Info: ${selenium.getBrowserInfo()}")
    appendLine("\nCommand Results:")
    results.forEachIndexed { index, result ->
      appendLine("### Command ${index + 1}")
      appendLine("```javascript")
      appendLine(planTask.commands[index])
      appendLine("```")
      appendLine("Result:")
      appendLine("```")
      appendLine(result.take(5000)) // Limit result size
      appendLine("```")
    }
    try {
      appendLine("\nFinal Page Source:")
      appendLine("```html")
      appendLine(selenium.getPageSource().take(10000)) // Limit page source size
      appendLine("```")
    } catch (e: Exception) {
      appendLine("\nError getting page source: ${e.message}")
    }
  }
}