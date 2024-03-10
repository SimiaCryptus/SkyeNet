package com.simiacryptus.skyenet.core.actors.test

import com.simiacryptus.jopenai.models.ChatModels
import com.simiacryptus.skyenet.core.actors.BaseActor
import com.simiacryptus.skyenet.core.actors.ParsedActor
import com.simiacryptus.skyenet.core.actors.ParsedResponse
import java.util.function.Function

abstract class ParsedActorTestBase<T:Any>(
    private val parserClass: Class<out Function<String, T>>,
) : ActorTestBase<List<String>,ParsedResponse<T>>() {

    override fun actorFactory(prompt: String) = ParsedActor(
      parserClass = parserClass,
      prompt = prompt,
      parsingModel = ChatModels.GPT35Turbo,
      model = ChatModels.GPT35Turbo,
    )

    override fun getPrompt(actor: BaseActor<List<String>,ParsedResponse<T>>): String = actor.prompt

    override fun resultMapper(result: ParsedResponse<T>): String = result.text

}