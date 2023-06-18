@file:Suppress("MemberVisibilityCanBePrivate")
package com.simiacryptus.skyenet

import com.fasterxml.jackson.annotation.JsonIgnore
import com.google.common.util.concurrent.MoreExecutors
import com.simiacryptus.openai.OpenAIClient
import com.simiacryptus.openai.proxy.ChatProxy
import com.simiacryptus.util.describe.Description
import com.simiacryptus.skyenet.body.PersistentSessionBase
import com.simiacryptus.skyenet.body.SessionDataStorage
import com.simiacryptus.skyenet.body.SkyenetInterviewer
import com.simiacryptus.util.JsonUtil
import java.awt.Desktop
import java.io.File
import java.net.URI
import java.util.*
import java.util.concurrent.Executors

object ShortStoryGenerator {

    @Description("A fully specified short story")
    data class StorySpecification(
        @Description("The title of the short story")
        val storyTitle: String? = null,
        @Description("The genre of the story")
        val genre: String? = null,
        @Description("The story's main characters")
        val characters: List<String>? = null,
        @Description("A brief plot for the story")
        val plot: String? = null,
    ) {

        @JsonIgnore
        fun validate(): List<String> {
            val errors = LinkedList<String>()
            if (null == storyTitle) errors.add("storyTitle is null")
            else if (storyTitle.isBlank()) errors.add("storyTitle is blank")
            if (null == genre) errors.add("genre is null")
            if (null == characters) errors.add("characters is null")
            if (null == plot) errors.add("plot is null")
            else if (plot.isEmpty()) errors.add("plot is empty")
            return errors
        }
    }

    interface StoryGenerator {
        fun generateStory(specification: StorySpecification): StoryDraft
    }

    data class StoryDraft(
        @Description("The story draft content")
        val content: String? = null,
    )

    fun implementStory(specification: StorySpecification, sessionDataStorage: SessionDataStorage) =
        object : PersistentSessionBase(UUID.randomUUID().toString(), sessionDataStorage) {

            val operationID = newID()
            var storyDraft: StoryDraft? = null

            init {
                draftStory()
            }

            override fun onCmd(id: String, code: String) {
                if(id == operationID) {
                    if(code == "run") {
                        Thread {
                            send("""$operationID,<div>
                            |<pre>${JsonUtil.toJson(storyDraft!!)}</pre>
                            |</div>""".trimMargin())
                        }.start()
                    } else if(code == "regen") {
                        draftStory()
                    }
                }
            }

            private fun draftStory() {
                Thread {
                    send("""$operationID,<div>Drafting Story... ${interviewer.spinner}</div>""")
                    storyDraft = generator.generateStory(specification)
                    send("""$operationID,<div>
                        |<pre>${JsonUtil.toJson(storyDraft!!)}</pre>
                        |<button class="regen-button" data-id="$operationID">♲</button>
                        |</div>""".trimMargin())
                }.start()
            }

            override fun run(userMessage: String) {
                TODO("Not yet implemented")
            }

        }

    private fun newID() = (0..5).map { ('a'..'z').random() }.joinToString("")

    val api = OpenAIClient(OpenAIClient.keyTxt)
    val log = org.slf4j.LoggerFactory.getLogger(ShortStoryGenerator::class.java)!!
    var sessionDataStorage: SessionDataStorage? = null
    const val port = 8081
    const val baseURL = "http://localhost:$port"
    val visiblePrompt = """
        |Hello! I am here to assist you in creating a short story! 
        |I will guide you through a series of questions to gather the necessary information. 
        |Don't worry if you're not sure about any details; I'm here to help!
        |What would you like to write about today?
        """.trimMargin()
    const val applicationName = "Story Creation Assistant"
    var interviewer: SkyenetInterviewer<StorySpecification> = SkyenetInterviewer(
        applicationName = applicationName,
        baseURL = baseURL,
        dataClass = StorySpecification::class.java,
        visiblePrompt = visiblePrompt,
        continueSession = { _, data -> implementStory(data, sessionDataStorage!!) },
        validate = StorySpecification::validate,
        apiKey = OpenAIClient.keyTxt
    )
    val pool = MoreExecutors.listeningDecorator(Executors.newCachedThreadPool())
    val generator = ChatProxy(StoryGenerator::class.java, api).create()

    @JvmStatic
    fun main(args: Array<String>) {
        val httpServer = interviewer.start(port)
        sessionDataStorage = interviewer.sessionDataStorage
        Desktop.getDesktop().browse(URI(baseURL))
        httpServer.join()
    }
}
