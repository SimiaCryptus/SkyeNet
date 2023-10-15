package com.simiacryptus.skyenet

import com.simiacryptus.openai.OpenAIClient
import com.simiacryptus.openai.proxy.ChatProxy
import com.simiacryptus.skyenet.body.ChatSessionFlexmark
import com.simiacryptus.skyenet.body.PersistentSessionBase
import com.simiacryptus.skyenet.body.SessionDiv
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
    temperature = temperature
) {
    interface StoryAPI {

        data class StoryParameters(
            val title: String = "",
            val shortDescription: String = "",
            val writingStyle: String = "",
        )

        fun generateStoryIdeas(storyDescription: String, count: Int = 10): StoryParametersList

        data class StoryParametersList(
            val items: List<StoryParameters>
        )

        @Description(
            """
            Seed the story and provide the first page.
            The length should be about 600 words, or 3-4 minutes of reading.
            Establish a random setting and some characters.
            Provide an initial context for the story and the reader's first choice.
            This first page should be long enough to be interesting.
        """
        )
        fun getFirstStoryPage(story: StoryParameters): StoryPage

        fun accumulateSummary(previousSummary: StorySummary?, pages: List<String>): StorySummary

        data class StorySummary(
            val description: String = "",
            val notes: List<String> = listOf(),
            //val characters: Map<String, CharacterSummary> = mapOf(),
        )

        data class CharacterSummary(
            val description: String = "",
            val notes: List<String> = listOf()
        )

        fun nextStoryPage(story: StoryParameters, summary: StorySummary?, prevPages: List<String>, choice: String): StoryPage

        data class StoryPage(
            @Description("Full text of page, written in the second person, with markdown formatting. It does not include the user choice prompt.")
            val text: String = "",
            @Description("A choice of action for the user to select from, written briefly in the first person")
            val choices: List<Choice> = listOf()
        )

        data class Choice(
            val text: String = "",
        )
    }

    val storyAPI = ChatProxy(
        clazz = StoryAPI::class.java,
        api = api,
        model = OpenAIClient.Models.GPT4,
        temperature = temperature
    ).create()

    override fun processMessage(
        sessionId: String,
        userMessage: String,
        session: PersistentSessionBase,
        sessionUI: SessionUI,
        sessionDiv: SessionDiv
    ) {
        try {
            sessionDiv.append("""<div>${ChatSessionFlexmark.renderMarkdown(userMessage)}</div>""", true)
            val storyParameters = storyAPI.generateStoryIdeas(userMessage)
            storyParameters.items.forEach { storyParameters ->
                sessionDiv.append(
                    """<div>${
                        sessionUI.hrefLink {
                            sessionDiv.append("<hr/><div><em>${storyParameters.title}</em></div>", true)
                            extracted(
                                sessionDiv = sessionDiv,
                                history = listOf(),
                                storyPage = storyAPI.getFirstStoryPage(storyParameters),
                                sessionUI = sessionUI,
                                storyParameters = storyParameters
                            )
                        }
                    }${storyParameters.title}</a> - ${storyParameters.shortDescription}</div>""", true)
            }
        } catch (e: Throwable) {
            logger.warn("Error", e)
        }
    }

    private fun extracted(
        sessionDiv: SessionDiv,
        history: List<String>,
        storyPage: StoryAPI.StoryPage,
        sessionUI: SessionUI,
        storyParameters: StoryAPI.StoryParameters,
        summary: StoryAPI.StorySummary? = null
    ) {
        var summary = summary
        var history = history
        if (history.size > 5) {
            summary = storyAPI.accumulateSummary(summary, history + storyPage.text)
            history = listOf()
        }
        sessionDiv.append(("""
                <div>${ChatSessionFlexmark.renderMarkdown(storyPage.text)}</div>                
                <ol>
                    ${
            storyPage.choices.joinToString("\n") { choice ->
                sessionDiv.append("", true)
                "<li>${
                    sessionUI.hrefLink {
                        sessionDiv.append(
                            """<div><em>${ChatSessionFlexmark.renderMarkdown(choice.text)}</em></div>""",
                            true
                        )
                        extracted(
                            sessionDiv = sessionDiv,
                            history = history + (storyPage.text + "\n\n" + choice.text),
                            storyPage = storyAPI.nextStoryPage(
                                storyParameters,
                                summary,
                                history + storyPage.text,
                                choice.text
                            ),
                            sessionUI = sessionUI,
                            storyParameters = storyParameters,
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
            val httpServer = StoryGenerator("StoryGenerator", baseURL).start(port)
            Desktop.getDesktop().browse(URI(baseURL))
            httpServer.join()
        }
    }
}