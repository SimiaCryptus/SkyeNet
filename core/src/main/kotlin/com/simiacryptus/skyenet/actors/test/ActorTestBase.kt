package com.simiacryptus.skyenet.actors.test

import com.simiacryptus.openai.OpenAIClient
import com.simiacryptus.skyenet.actors.BaseActor
import com.simiacryptus.skyenet.actors.opt.ActorOptimization
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
            val answer = actor.answer(questions = testCase.userMessages.toTypedArray(), api)
            log.info("Answer: ${resultMapper(answer)}")
        }
    }

    companion object {
        private val log = LoggerFactory.getLogger(ActorTestBase::class.java)
    }
}