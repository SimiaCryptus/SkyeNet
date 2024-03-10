package com.simiacryptus.skyenet.webui.chat

import com.simiacryptus.jopenai.ApiModel
import com.simiacryptus.jopenai.OpenAIClient
import com.simiacryptus.jopenai.models.OpenAITextModel
import com.simiacryptus.jopenai.util.ClientUtil.toContentList
import com.simiacryptus.skyenet.AgentPatterns
import com.simiacryptus.skyenet.core.platform.Session
import com.simiacryptus.skyenet.core.platform.StorageInterface
import com.simiacryptus.skyenet.webui.application.ApplicationInterface
import com.simiacryptus.skyenet.webui.application.ApplicationServer
import com.simiacryptus.skyenet.webui.session.SocketManagerBase
import com.simiacryptus.skyenet.webui.util.MarkdownUtil

open class ChatSocketManager(
  session: Session,
  val model: OpenAITextModel,
  val userInterfacePrompt: String,
  open val initialAssistantPrompt: String = "",
  open val systemPrompt: String,
  val api: OpenAIClient,
  val temperature: Double = 0.3,
  applicationClass: Class<out ApplicationServer>,
  val storage: StorageInterface?,
) : SocketManagerBase(session, storage, owner = null, applicationClass = applicationClass) {

  init {
    if (userInterfacePrompt.isNotBlank()) {
      send("""aaa,<div class="initial-prompt">${MarkdownUtil.renderMarkdown(userInterfacePrompt)}</div>""")
    }
  }

  protected val messages by lazy {
    val list = listOf(
      ApiModel.ChatMessage(ApiModel.Role.system, systemPrompt.toContentList()),
    ).toMutableList()
    if (initialAssistantPrompt.isNotBlank()) list +=
      ApiModel.ChatMessage(ApiModel.Role.assistant, initialAssistantPrompt.toContentList())
    list
  }

  @Synchronized
  override fun onRun(userMessage: String, socket: ChatSocket) {
    val task = newTask()
    val responseContents = renderResponse(userMessage)
    task.echo(responseContents)
    messages += ApiModel.ChatMessage(ApiModel.Role.user, userMessage.toContentList())
    val messagesCopy = messages.toList()
    try {
      AgentPatterns.retryable(ApplicationInterface(this), task) {
        val response = (api.chat(
          ApiModel.ChatRequest(
            messages = messagesCopy,
            temperature = temperature,
            model = model.modelName,
          ), model
        ).choices.first().message?.content.orEmpty())
        messages.dropLastWhile { it.role == ApiModel.Role.assistant }
        messages += ApiModel.ChatMessage(ApiModel.Role.assistant, response.toContentList())
        onResponse(renderResponse(response), responseContents)
        return@retryable renderResponse(response)
      }
    } catch (e: Exception) {
      log.info("Error in chat", e)
      task.error(ApplicationInterface(this), e)
    }
  }

  open fun renderResponse(response: String) = """<div>${MarkdownUtil.renderMarkdown(response)}</div>"""

  open fun onResponse(response: String, responseContents: String) {}

  companion object {
    private val log = org.slf4j.LoggerFactory.getLogger(ChatSocketManager::class.java)
  }
}