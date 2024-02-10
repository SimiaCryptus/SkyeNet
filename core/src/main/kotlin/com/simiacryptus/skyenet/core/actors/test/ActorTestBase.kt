package com.simiacryptus.skyenet.core.actors.test

import com.simiacryptus.jopenai.ApiModel
import com.simiacryptus.jopenai.OpenAIClient
import com.simiacryptus.jopenai.util.ClientUtil.toContentList
import com.simiacryptus.skyenet.core.actors.BaseActor
import com.simiacryptus.skyenet.core.actors.opt.ActorOptimization
import org.slf4j.LoggerFactory
import org.slf4j.event.Level

abstract class ActorTestBase<I: Any, R : Any> {

    open val api = OpenAIClient(logLevel = Level.DEBUG)

    abstract val testCases: List<ActorOptimization.TestCase>
    abstract val actor: BaseActor<I, R>
    abstract fun actorFactory(prompt: String): BaseActor<I, R>
    abstract fun getPrompt(actor: BaseActor<I, R>): String
    abstract fun resultMapper(result: R): String

    open fun opt(
        actor: BaseActor<I, R> = this.actor,
        testCases: List<ActorOptimization.TestCase> = this.testCases,
        actorFactory: (String) -> BaseActor<I, R> = this::actorFactory,
        resultMapper: (R) -> String = this::resultMapper
    ) {
        ActorOptimization(
            api
        ).runGeneticGenerations(
            populationSize = 1,
            generations = 1,
            selectionSize = 1,
            actorFactory = actorFactory as (String) -> BaseActor<List<String>, R>,
            resultMapper = resultMapper,
            prompts = listOf(
                getPrompt(actor),
            ),
            testCases = testCases,
        )
    }

    open fun testOptimize() {
        opt()
    }

    open fun testRun() {
        testCases.forEach { testCase ->
            val messages = arrayOf(
                ApiModel.ChatMessage(
                    role = com.simiacryptus.jopenai.ApiModel.Role.system,
                    content = actor.prompt.toContentList()
                ),
            ) + testCase.userMessages.toTypedArray()
            val answer = answer(messages)
            log.info("Answer: ${resultMapper(answer)}")
        }
    }

    open fun answer(messages: Array<ApiModel.ChatMessage>): R = actor.respond(
        input = (messages.map { it.content?.first()?.text }) as I,
        api=api,
        *messages
    )

    companion object {
        private val log = LoggerFactory.getLogger(ActorTestBase::class.java)
    }
}