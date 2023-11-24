package com.simiacryptus.skyenet.core.actors.test

import com.simiacryptus.jopenai.ApiModel
import com.simiacryptus.jopenai.ClientUtil.toContentList
import com.simiacryptus.jopenai.OpenAIClient
import com.simiacryptus.skyenet.core.actors.BaseActor
import com.simiacryptus.skyenet.core.actors.opt.ActorOptimization
import org.slf4j.LoggerFactory
import org.slf4j.event.Level

abstract class ActorTestBase<R : Any> {

    open val api = OpenAIClient(logLevel = Level.DEBUG)

    abstract val testCases: List<ActorOptimization.TestCase>
    abstract val actor: BaseActor<R>
    abstract fun actorFactory(prompt: String): BaseActor<R>
    abstract fun getPrompt(actor: BaseActor<R>): String
    abstract fun resultMapper(result: R): String

    open fun opt(
        actor: BaseActor<R> = this.actor,
        testCases: List<ActorOptimization.TestCase> = this.testCases,
        actorFactory: (String) -> BaseActor<R> = this::actorFactory,
        resultMapper: (R) -> String = this::resultMapper
    ) {
        ActorOptimization(
            api
        ).runGeneticGenerations(
            populationSize = 1,
            generations = 1,
            selectionSize = 1,
            actorFactory = actorFactory,
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
            val answer = actor.answer(messages = arrayOf(
                ApiModel.ChatMessage(
                    role = com.simiacryptus.jopenai.ApiModel.Role.system,
                    content = actor.prompt.toContentList()
                ),
            ) + testCase.userMessages.toTypedArray(), api)
            log.info("Answer: ${resultMapper(answer)}")
        }
    }

    companion object {
        private val log = LoggerFactory.getLogger(ActorTestBase::class.java)
    }
}