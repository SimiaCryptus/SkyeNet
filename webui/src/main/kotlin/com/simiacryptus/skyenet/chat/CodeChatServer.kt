package com.simiacryptus.skyenet.chat

import com.simiacryptus.jopenai.OpenAIClient
import com.simiacryptus.jopenai.models.ChatModels
import com.simiacryptus.jopenai.models.OpenAITextModel
import com.simiacryptus.skyenet.application.ApplicationServer
import com.simiacryptus.skyenet.platform.Session
import com.simiacryptus.skyenet.platform.User
import com.simiacryptus.skyenet.servlet.AppInfoServlet
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

    override fun newSession(user: User?, session: Session) = object : ChatSocketManager(
        session = session,
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
        applicationClass = ApplicationServer::class.java,
    ) {
        override fun canWrite(user: User?): Boolean = true
    }

    override val baseResource: Resource
        get() = ClasspathResource(javaClass.classLoader.getResource(resourceBase)!!)

    override fun configure(webAppContext: WebAppContext, path: String, baseUrl: String) {
        webAppContext.addServlet(ServletHolder(javaClass.simpleName + "/appInfo", AppInfoServlet(applicationName)), "/appInfo")
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