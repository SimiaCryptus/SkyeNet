package com.simiacryptus.skyenet.actors.test

import com.simiacryptus.skyenet.actors.BaseActor
import com.simiacryptus.skyenet.actors.ParsedActor
import com.simiacryptus.skyenet.actors.ParsedResponse
import java.util.function.Function

abstract class ParsedActorTestBase<T:Any>(
    private val parserClass: Class<out Function<String, T>>,
) : ActorTestBase<ParsedResponse<T>>() {

    override fun actorFactory(prompt: String) = ParsedActor(
        parserClass = parserClass,
        prompt = prompt,
    )

    override fun getPrompt(actor: BaseActor<ParsedResponse<T>>): String = actor.prompt

    override fun resultMapper(result: ParsedResponse<T>): String = result.getText()

}