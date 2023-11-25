package com.simiacryptus.skyenet.core.actors.test

import com.simiacryptus.skyenet.core.Interpreter
import com.simiacryptus.skyenet.core.actors.BaseActor
import com.simiacryptus.skyenet.core.actors.CodingActor
import com.simiacryptus.skyenet.core.actors.CodingActor.CodeResult
import kotlin.reflect.KClass

abstract class CodingActorTestBase : ActorTestBase<CodingActor.CodeRequest, CodeResult>() {
    abstract val interpreterClass: KClass<out Interpreter>
    override fun actorFactory(prompt: String): CodingActor = CodingActor(
        interpreterClass = interpreterClass,
        details = prompt,
    )

    override fun getPrompt(actor: BaseActor<CodingActor.CodeRequest, CodeResult>): String = (actor as CodingActor).details!!
    override fun resultMapper(result: CodeResult): String = result.getCode()
}

