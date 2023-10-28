package com.simiacryptus.skyenet.prompts

import com.simiacryptus.openai.OpenAIClient

class PromptOptimization(
    val api: OpenAIClient
) {
    fun runGeneticGeneration(
        seedPrompts: List<String>,
        testCases: List<TestCase>
    ): List<String> {
        // For each of N prompts, Run the test cases and assign each prompt a score
        // Select top log(N) prompts
        // Recombine and mutate to regenerate N prompts
        // Repeat until convergence
        var generation = 0
        var topPrompts = seedPrompts
        for (i in 0..100) {
            val scores = topPrompts.map { prompt ->
                prompt to testCases.map { testCase ->
                    evaluate(prompt, testCase)
                }.average()
            }
            val survivors = scores.sortedByDescending { it.second }.take(10).map { it.first }
            topPrompts = regenerate(survivors, 10)
            println("Generation $generation: ${topPrompts.first()}")
            generation++
        }
        return listOf()
    }

    private fun regenerate(survivors: List<String>, i: Int): List<String> {
        // Use the recombinator to generate i new prompts from the survivors
        val result = listOf<String>().toMutableList()
        result += survivors
        while (result.size < i) {
            if (survivors.size == 1) {
                result += mutate(survivors.first())
                continue
            } else if (survivors.size == 0) {
                throw RuntimeException("No survivors")
            }
            val a = survivors.random()
            val b = survivors.random()
            result += recombine(a, b)
        }
        return result

    }

    interface GeneticApi {
        fun mutate(prompt: String): String
        fun recombine(a: String, b: String): String
    }
    val geneticApi = com.simiacryptus.openai.proxy.ChatProxy(
        clazz = GeneticApi::class.java,
    )

    private fun evaluate(systemPrompt: String, testCase: TestCase): Double {
        // Run the test case and return a score
        TODO()
    }

    private fun mutate(prompt: String): String {
        // Mutate a prompt
        TODO()
    }

    private fun recombine(a: String, b: String): String {
        // Recombine two prompts
        TODO()
    }
}

data class TestCase(val turns: List<Turn>, val retries: Int = 3)

data class Turn(val userMessage: String, val expectations: List<Expectation>)

sealed class Expectation {
    class VectorMatch(val content: String) : Expectation() {
        override fun matches(api: OpenAIClient, prompt: String): Boolean {
            TODO("Not yet implemented")
        }

        override fun score(api: OpenAIClient, prompt: String): Double {
            TODO("Not yet implemented")
        }
    }

    class ContainsMatch(pattern: Regex) : Expectation() {
        override fun matches(api: OpenAIClient, prompt: String): Boolean {
            TODO("Not yet implemented")
        }

        override fun score(api: OpenAIClient, prompt: String): Double {
            TODO("Not yet implemented")
        }

    }

    abstract fun matches(api: OpenAIClient, prompt: String): Boolean

    abstract fun score(api: OpenAIClient, prompt: String): Double


}

