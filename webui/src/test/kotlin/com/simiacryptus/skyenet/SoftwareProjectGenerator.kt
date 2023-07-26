package com.simiacryptus.skyenet

import com.simiacryptus.openai.OpenAIClient
import com.simiacryptus.openai.proxy.ChatProxy
import com.simiacryptus.skyenet.body.ChatSessionFlexmark
import com.simiacryptus.skyenet.body.SkyenetMacroChat
import com.simiacryptus.util.describe.Description
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
            val shortDescription: String = "",
            val programmingLanguage: String = "",
        )

        fun generateProjectIdeas(projectDescription: String, count: Int = 10): ProjectParametersList

        data class ProjectParametersList(
            val items: List<ProjectParameters>
        )

        @Description(
            """
            Provide the initial details of the software project.
            This should include the project's purpose, the proposed technology stack, and the initial set of features.
            This initial description should be detailed enough to provide a clear direction for the project.
        """
        )
        fun getFirstProjectDetails(project: ProjectParameters): ProjectDetails

        fun accumulateSummary(previousSummary: ProjectSummary?, pages: List<String>): ProjectSummary

        data class ProjectSummary(
            val description: String = "",
            val notes: List<String> = listOf(),
        )

        fun nextProjectDetails(project: ProjectParameters, summary: ProjectSummary?, prevPages: List<String>, choice: String): ProjectDetails

        data class ProjectDetails(
            @Description("Full text of the project details, written in the second person, with markdown formatting. It does not include the user choice prompt.")
            val text: String = "",
            @Description("A choice of action for the user to select from, written briefly in the first person")
            val choices: List<Choice> = listOf()
        )

        data class Choice(
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
        userMessage: String,
        sessionUI: SessionUI,
        sendUpdate: (String, Boolean) -> Unit
    ) {
        try {
            sendUpdate("""<div>${ChatSessionFlexmark.renderMarkdown(userMessage)}</div>""", true)
            val projectParameters = projectAPI.generateProjectIdeas(userMessage)
            projectParameters.items.forEach { projectParameters ->
                sendUpdate(
                    """<div>${
                        sessionUI.hrefLink {
                            sendUpdate("<hr/><div><em>${projectParameters.title}</em></div>", true)
                            extracted(
                                sendUpdate = sendUpdate,
                                history = listOf(),
                                projectDetails = projectAPI.getFirstProjectDetails(projectParameters),
                                sessionUI = sessionUI,
                                projectParameters = projectParameters
                            )
                        }
                    }${projectParameters.title}</a> - ${projectParameters.shortDescription}</div>""", true)
            }
        } catch (e: Throwable) {
            logger.warn("Error", e)
        }
    }

    private fun extracted(
        sendUpdate: (String, Boolean) -> Unit,
        history: List<String>,
        projectDetails: ProjectAPI.ProjectDetails,
        sessionUI: SessionUI,
        projectParameters: ProjectAPI.ProjectParameters,
        summary: ProjectAPI.ProjectSummary? = null
    ) {
        var summary = summary
        var history = history
        if (history.size > 5) {
            summary = projectAPI.accumulateSummary(summary, history + projectDetails.text)
            history = listOf()
        }
        sendUpdate(("""
                <div>${ChatSessionFlexmark.renderMarkdown(projectDetails.text)}</div>                
                <ol>
                    ${
            projectDetails.choices.joinToString("\n") { choice ->
                sendUpdate("", true)
                "<li>${
                    sessionUI.hrefLink {
                        sendUpdate(
                            """<div><em>${ChatSessionFlexmark.renderMarkdown(choice.text)}</em></div>""",
                            true
                        )
                        extracted(
                            sendUpdate = sendUpdate,
                            history = history + (projectDetails.text + "\n\n" + choice.text),
                            projectDetails = projectAPI.nextProjectDetails(
                                projectParameters,
                                summary,
                                history + projectDetails.text,
                                choice.text
                            ),
                            sessionUI = sessionUI,
                            projectParameters = projectParameters,
                            summary = summary
                        )
                    }
                }${choice.text}</a></li>"
            }
        }
                </ol>""".trimIndent()), false)
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