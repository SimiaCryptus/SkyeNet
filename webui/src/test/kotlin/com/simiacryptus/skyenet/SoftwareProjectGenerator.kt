package com.simiacryptus.skyenet

import com.simiacryptus.openai.OpenAIClient
import com.simiacryptus.openai.proxy.ChatProxy
import com.simiacryptus.skyenet.body.ChatSessionFlexmark
import com.simiacryptus.skyenet.body.PersistentSessionBase
import com.simiacryptus.skyenet.body.SkyenetMacroChat
import com.simiacryptus.util.JsonUtil
import java.awt.Desktop
import java.net.URI

class SoftwareProjectGenerator(
    applicationName: String,
    baseURL: String,
    temperature: Double = 0.3
) : SkyenetMacroChat(
    applicationName = applicationName,
    baseURL = baseURL,
    temperature = temperature
) {
    interface ProjectAPI {

        data class ProjectParameters(
            val title: String = "",
            val description: String = "",
            val programmingLanguage: String = "",
            val requirements: List<String> = listOf(),
        )

        fun parseProject(projectDescription: String): ProjectParameters

        fun expandProject(project: ProjectParameters): FileSpecList

        data class FileSpecList(
            val items: List<FileSpec>
        )

        data class FileSpec(
            val filepath: String = "",
            val requirements: List<String> = listOf(),
        )

        fun implementFile(file: FileSpec): FileImpl

        data class FileImpl(
            val filepath: String = "",
            val language: String = "",
            val text: String = "",
        )


    }

    val projectAPI = ChatProxy(
        clazz = ProjectAPI::class.java,
        api = api,
        model = OpenAIClient.Models.GPT4,
        temperature = temperature
    ).create()

    override fun processMessage(
        sessionId: String,
        userMessage: String,
        session: PersistentSessionBase,
        sessionUI: SessionUI,
        sendUpdate: (String, Boolean) -> Unit
    ) {
        try {
            sendUpdate("""<div>${ChatSessionFlexmark.renderMarkdown(userMessage)}</div>""", true)
            val projectParameters = projectAPI.parseProject(userMessage)
            sendUpdate("""<pre>${JsonUtil.toJson(projectParameters)}</pre>""", true)
            //sendUpdate("<hr/><div><em>${projectParameters.title}</em></div>", true)
            val fileSpecList = projectAPI.expandProject(projectParameters)

            fileSpecList.items.forEach { fileSpec ->
                sendUpdate(
                    """<div>${
                        sessionUI.hrefLink {
                            sendUpdate("<hr/><div><em>${fileSpec.filepath}</em></div>", true)
                            val fileImpl = projectAPI.implementFile(fileSpec)
                            sendUpdate("<pre>${fileImpl.text}</pre>", false)
                        }
                    }${fileSpec.filepath}</a></div>""", false)
            }

        } catch (e: Throwable) {
            logger.warn("Error", e)
        }
    }

    companion object {

        const val port = 8081
        const val baseURL = "http://localhost:$port"

        @JvmStatic
        fun main(args: Array<String>) {
            val httpServer = SoftwareProjectGenerator("SoftwareProjectGenerator", baseURL).start(port)
            Desktop.getDesktop().browse(URI(baseURL))
            httpServer.join()
        }
    }
}