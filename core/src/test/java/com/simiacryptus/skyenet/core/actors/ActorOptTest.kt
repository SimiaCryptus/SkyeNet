package com.simiacryptus.skyenet.core.actors

import com.simiacryptus.jopenai.OpenAIClient
import com.simiacryptus.skyenet.core.actors.opt.ActorOptimization
import com.simiacryptus.skyenet.core.actors.opt.ActorOptimization.Companion.toChatMessage
import com.simiacryptus.skyenet.core.actors.opt.Expectation
import org.slf4j.LoggerFactory
import org.slf4j.event.Level
import kotlin.system.exitProcess

object ActorOptTest {

    private val log = LoggerFactory.getLogger(ActorOptTest::class.java)

    @JvmStatic
    fun main(args: Array<String>) {
        try {
            ActorOptimization(
                OpenAIClient(
                    logLevel = Level.DEBUG
                )
            ).runGeneticGenerations(
                populationSize = 7,
                generations = 5,
                actorFactory = { SimpleActor(prompt = it) },
                resultMapper = { it },
                prompts = listOf(
                    """
                    |As the intermediary between the user and the search engine, your main task is to generate search queries based on user requests. 
                    |Please respond to each user request by providing one or more calls to the "`search('query text')`" function.
                    |""".trimMargin(),
                    """
                    |You act as a bridge between the user and the search engine by creating search queries. 
                    |Output one or more calls to "`search('query text')`" in response to each user request.
                    |""".trimMargin().trim(),
                    """
                    |You play the role of a search assistant. 
                    |Provide one or more "`search('query text')`" calls as a response to each user request.
                    |Make sure to use single quotes around the query text.
                    |Surround the search function call with backticks.
                    |""".trimMargin().trim(),
                ),
                testCases = listOf(
                    ActorOptimization.TestCase(
                        userMessages = listOf(
                            "I want to buy a book.",
                            "A history book about Napoleon.",
                        ).map { it.toChatMessage() },
                        expectations = listOf(
                            Expectation.ContainsMatch("""`search\('.*?'\)`""".toRegex(), critical = false),
                            Expectation.ContainsMatch("""search\(.*?\)""".toRegex(), critical = false),
                            Expectation.VectorMatch("Great, what kind of book are you looking for?")
                        )
                    )
                ),
            )
        } catch (e: Throwable) {
            log.error("Error", e)
        } finally {
            exitProcess(0)
        }
    }

}