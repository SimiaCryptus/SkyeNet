package com.simiacryptus.skyenet.prompts

import com.simiacryptus.openai.OpenAIClient

object PromptOptTest {
    fun main() {
        val opt = PromptOptimization(
            OpenAIClient(
                key = "sk-<your key here>",
            )
        )

        opt.runGeneticGeneration(
            seedPrompts = listOf(
                "Hello, I'm your assistant. How can I help you today?"
            ), testCases = listOf(
                TestCase(
                    turns = listOf(
                        Turn(
                            "I need a vacation.",
                            listOf(Expectation.VectorMatch("Sure, what kind of vacation are you interested in? Beach, adventure, or cultural?"))
                        ),
                        Turn(
                            "I love adventure!",
                            listOf(Expectation.VectorMatch("Great, do you prefer mountains or jungles?"))
                        ),
                        Turn(
                            "Mountains for sure.",
                            listOf(Expectation.VectorMatch("How about the Swiss Alps or the Rocky Mountains?"))
                        )
                    ),
                    retries = 2
                ),
                TestCase(
                    turns = listOf(
                        Turn(
                            "I want to relax on a beach.",
                            listOf(Expectation.VectorMatch("Would you like to go to a tropical island or a more crowded, popular beach?"))
                        ),
                        Turn(
                            "Tropical island, please.",
                            listOf(Expectation.VectorMatch("Maldives or Seychelles?"))
                        )
                    ),
                    retries = 2
                ),
                TestCase(
                    turns = listOf(
                        Turn(
                            "I want to learn something new.",
                            listOf(Expectation.VectorMatch("Would you prefer a historical trip or a culinary experience?"))
                        ),
                        Turn(
                            "Historical trip.",
                            listOf(Expectation.VectorMatch("Would Ancient Egypt or Classical Greece interest you?"))
                        )
                    ),
                    retries = 2
                )
            )
        )

        opt.runGeneticGeneration(
            seedPrompts = listOf(
                """You are a search assistant; you interpret user requests and return search results. 
                        |You are given a user request and respond with a call to `search("query text")`.
                        |""".trimMargin()
            ), testCases = listOf(
                TestCase(
                    turns = listOf(
                        Turn(
                            "I want to buy a book.",
                            listOf(
                                Expectation.ContainsMatch("`search('.*')`".toRegex()),
                                Expectation.VectorMatch("Great, what kind of book are you looking for?")
                            )
                        )
                    )
                )
            )
        )
    }

}