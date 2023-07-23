package com.simiacryptus.skyenet

import com.simiacryptus.openai.OpenAIClient
import com.simiacryptus.openai.proxy.ChatProxy
import com.simiacryptus.skyenet.body.ChatSessionFlexmark
import com.simiacryptus.skyenet.body.SkyenetMacroChat
import com.simiacryptus.util.JsonUtil
import com.simiacryptus.util.describe.Description
import java.awt.Desktop
import java.net.URI

class SkyenetScienceBook(
    applicationName: String,
    baseURL: String,
    temperature: Double = 0.3
) : SkyenetMacroChat(
    applicationName = applicationName,
    baseURL = baseURL,
    temperature = temperature
) {
    interface ScienceAuthorAPI {
        fun parseProjectSpec(spec: String): ProjectSpec

        data class ProjectSpec(
            val title: String,
            val domain: String,
            val targetAudience: TargetAudience,
            val materials: List<String>,
            val equipment: List<String>,
        )

        data class TargetAudience(
            val ageGroup: String
        )

        data class WritingStyle(
            val targetAudience: TargetAudience,
            val notes: MutableMap<String,String> = mutableMapOf()
        )

        fun createExperiments(spec: ProjectSpec, count: Int): ExperimentList

        data class ExperimentList(
            val experimentList: List<ExperimentSummary>
        )

        data class ExperimentSummary(
            @Description("A very brief description of the science experiment for listing/indexing purposes")
            val title: String,
            @Description("A brief description of the science experiment")
            val description: String,
            @Description("A list of materials used; this should be limited to 2-5 items, usually drawn from the available project materials")
            val materials: List<String>,
            @Description("A list of equipment used; this should be limited to what is strictly needed, and drawn from the available project materials")
            val equipment: List<String>,
        )

        data class ExperimentDetails(
            @Description("A very brief description of the science experiment for listing/indexing purposes")
            val title: String,
            @Description("A narrative description of the relevant science background")
            val background: String,
            @Description("A narrative description of the science experiment")
            val description: String,
            @Description("A map of materials used to the quantities required for the experiment")
            val materials: Map<String, String>,
            @Description("A map of equipment to quantity required for the experiment")
            val equipment: Map<String, Int>,
            @Description("A checklist for experiment preparation")
            val labSetup: List<String>,
            @Description("A list of steps to perform the experiment")
            val steps: List<ExperimentStepData>,
            @Description("A map of observations to make during the experiment (with expected results)")
            val observations: Map<String, String>,
            @Description("A list of ideas for variations on the experiment")
            val variations: List<ExperimentVariation>,
            val supervisionNotes: List<String>,
        )

        data class ExperimentStepData(
            val step: String,
            val observations: List<String>,
        )

        data class ExperimentVariation(
            val title: String,
            val details: String,
            @Description("A map of observations to make during the experiment (with expected results)")
            val observations: Map<String, String>,
        )

        fun detailExperiment(experiment: ExperimentSummary): ExperimentDetails

        @Description("Create a detailed notebook writeup")
        fun getFullLabNotebook(
            style: WritingStyle,
            experiment: ExperimentDetails
        ): LabNotebook

        fun modifyNotebook(
            notebook: String,
            usertext: String
        ): LabNotebook

        data class LabNotebook(
            val markdown: String
        )
    }

    val scienceAuthorAPI = ChatProxy(
        clazz = ScienceAuthorAPI::class.java,
        api = api,
        model = OpenAIClient.Models.GPT35Turbo,
        temperature = temperature
    ).create()
    override fun processMessage(
        userMessage: String,
        sessionUI: SessionUI,
        sendUpdate: (String, Boolean) -> Unit
    ) {
        sendUpdate("""<div>$userMessage</div>""", true)
        val spec = scienceAuthorAPI.parseProjectSpec(userMessage)
        sendUpdate("""<div><pre>${JsonUtil.toJson(spec)}</pre></div>""", true)
        val experiments = scienceAuthorAPI.createExperiments(spec, 20)
        for (experiment in experiments.experimentList.toMutableList().shuffled()) {
            sendUpdate("""<div><pre>${JsonUtil.toJson(experiment)}</pre>${sessionUI.hrefLink {
                sendUpdate("", true)
                val details = scienceAuthorAPI.detailExperiment(experiment)
                sendUpdate("""<div><pre>${JsonUtil.toJson(details)}</pre></div>""", true)
                val fullLabNotebook = scienceAuthorAPI.getFullLabNotebook(
                    style = ScienceAuthorAPI.WritingStyle(
                        targetAudience = spec.targetAudience,
                        notes = mutableMapOf(
                            "description" to "Should be conversational and friendly, introducing the experimenter to both the experiment and background theory",
                            "instructions" to "Fully detailed instructions, including measurements, amounts, notes, and precautions. Use a conversational style that invites the student to make predictions and includes explanations.",
                            "notes" to "Briefly formatted at the bottom to be readable by the adult with full detail but not attention-drawing"
                        )
                    ),
                    experiment = details,
                )
                postExperiment(sendUpdate, fullLabNotebook, sessionUI, scienceAuthorAPI)
            } }Expand</a></div>""", true)
        }
    }

    private fun postExperiment(
        sendUpdate: (String, Boolean) -> Unit,
        fullLabNotebook: ScienceAuthorAPI.LabNotebook,
        sessionUI: SessionUI,
        scienceAuthorAPI: ScienceAuthorAPI
    ) {
        sendUpdate(
            """<div>${ChatSessionFlexmark.renderMarkdown(fullLabNotebook.markdown)}</div>${
                sessionUI.textInput { userInput ->
                    sendUpdate("", true)
                    val labNotebook = scienceAuthorAPI.modifyNotebook(fullLabNotebook.markdown, userInput)
                    postExperiment(sendUpdate, labNotebook, sessionUI, scienceAuthorAPI)
                }
            }""", false)
    }


    companion object {

        const val port = 8081
        const val baseURL = "http://localhost:$port"

        @JvmStatic
        fun main(args: Array<String>) {
            val httpServer = SkyenetScienceBook("SkyenetScienceBook", baseURL).start(port)
            Desktop.getDesktop().browse(URI(baseURL))
            httpServer.join()
        }
    }

}