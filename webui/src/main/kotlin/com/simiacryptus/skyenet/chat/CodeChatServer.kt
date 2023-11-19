package com.simiacryptus.skyenet.chat

import com.simiacryptus.openai.OpenAIClient
import com.simiacryptus.openai.models.ChatModels
import com.simiacryptus.openai.models.OpenAITextModel
import com.simiacryptus.skyenet.ApplicationBase
import com.simiacryptus.skyenet.util.ClasspathResource
import org.eclipse.jetty.servlet.ServletHolder
import org.eclipse.jetty.util.resource.Resource
import org.eclipse.jetty.webapp.WebAppContext

class CodeChatServer(
    val language: String,
    val codeSelection: String,
    val api: OpenAIClient,
    val model: OpenAITextModel = ChatModels.GPT35Turbo,
    resourceBase: String = "codeChat",
) : ChatServer(
    resourceBase = resourceBase,
) {
    override val applicationName: String get() = "Code Chat"

    override fun newSession(userId: String?, sessionId: String) = object : ChatSession(
        sessionId = sessionId,
        parent = this@CodeChatServer,
        model = model,
        api = api,
        userInterfacePrompt = """
            |# Code:
            |
            |```$language
            |$codeSelection
            |```
            |
            """.trimMargin().trim(),
        systemPrompt = """
            |You are a helpful AI that helps people with coding.
            |
            |You will be answering questions about the following code:
            |
            |```$language
            |$codeSelection
            |```
            |
            |Responses may use markdown formatting.
            """.trimMargin(),
        applicationClass = ApplicationBase::class.java,
    ) {
        override fun canWrite(user: String?): Boolean = true
    }

    override val baseResource: Resource
        get() = ClasspathResource(javaClass.classLoader.getResource(resourceBase)!!)

    override fun configure(webAppContext: WebAppContext, path: String, baseUrl: String) {
        webAppContext.addServlet(ServletHolder(javaClass.simpleName + "/appInfo", AppInfoServlet()), "/appInfo")
        super.configure(webAppContext, path, baseUrl)
    }

    companion object {
        fun htmlEscape(html: String) = html
            .replace("&", "&amp;")
            .replace("<", "&lt;")
            .replace(">", "&gt;")
            .replace("\"", "&quot;")
            .replace("'", "&#039;")
    }
}