package com.simiacryptus.skyenet

import com.simiacryptus.openai.OpenAIClient
import com.simiacryptus.openai.proxy.ChatProxy
import com.simiacryptus.skyenet.body.ChatSessionFlexmark
import com.simiacryptus.skyenet.body.SkyenetMacroChat
import com.simiacryptus.util.describe.Description
import java.awt.Desktop
import java.net.URI

class StoryGenerator(
    applicationName: String,
    baseURL: String,
    temperature: Double = 0.3
) : SkyenetMacroChat(
    applicationName = applicationName,
    baseURL = baseURL,
    temperature = temperature
) {
    interface StoryAPI {

        fun parseStoryParameters(storyDescription: String): StoryParameters

        fun getFirstStoryPage(storyDescription: String): StoryPage

        fun nextStoryPage(story: StoryParameters, prevPage: StoryPage, choice: String): StoryPage

        data class StoryParameters(
            @Description("The length of each page of the story")
            val pageLength: String,
            val readingLevel: String,
            val genre: String,
            val storyDescription: String
        )

        data class StoryPage(
            @Description("Full text of page, written in the second person, with markdown formatting. This is the text to be displayed to the user.")
            val text: String,
            @Description("List of notes about the state of the story/player/universe. These are not displayed to the user.")
            val notes: List<String>,
            @Description("A choice of action for the user to select from, written in the first person")
            val choices: List<Choice>
        )

        data class Choice(
            val text: String,
        )
    }

    val storyAPI = ChatProxy(
        clazz = StoryAPI::class.java,
        api = api,
        model = OpenAIClient.Models.GPT35Turbo,
        temperature = temperature
    ).create()

    override fun processMessage(
        userMessage: String,
        sessionUI: SessionUI,
        sendUpdate: (String, Boolean) -> Unit
    ) {
        sendUpdate("""<div>${ChatSessionFlexmark.renderMarkdown(userMessage)}</div>""", true)
        val storyPage = storyAPI.getFirstStoryPage(userMessage)
        val storyParameters = storyAPI.parseStoryParameters(userMessage)
        //language=HTML
        extracted(sendUpdate, storyPage, sessionUI, storyParameters)
    }

    private fun extracted(
        sendUpdate: (String, Boolean) -> Unit,
        storyPage: StoryAPI.StoryPage,
        sessionUI: SessionUI,
        storyParameters: StoryAPI.StoryParameters
    ) {
        sendUpdate(("""
                <div>${ChatSessionFlexmark.renderMarkdown(storyPage.text)}</div>
                
                <ol>
                    ${
            storyPage.choices.joinToString("\n") { choice ->
                "<li>${
                    sessionUI.hrefLink {
                        sendUpdate(
                            """<div>${ChatSessionFlexmark.renderMarkdown(choice.text)}</div>""".trimIndent(),
                            true
                        )
                        val nextStoryPage = storyAPI.nextStoryPage(storyParameters, storyPage, choice.text)
                        extracted(sendUpdate, nextStoryPage, sessionUI, storyParameters)
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
            val httpServer = StoryGenerator("StoryGenerator", baseURL).start(port)
            Desktop.getDesktop().browse(URI(baseURL))
            httpServer.join()
        }
    }
}