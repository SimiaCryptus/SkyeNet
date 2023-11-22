package com.simiacryptus.skyenet.core.actors.test

import com.simiacryptus.skyenet.core.Heart
import com.simiacryptus.skyenet.core.actors.BaseActor
import com.simiacryptus.skyenet.core.actors.CodeResult
import com.simiacryptus.skyenet.core.actors.CodingActor
import kotlin.reflect.KClass

abstract class CodingActorTestBase : ActorTestBase<CodeResult>() {
    abstract val interpreterClass: KClass<out Heart>
    override fun actorFactory(prompt: String): CodingActor = CodingActor(
        interpreterClass = interpreterClass,
        details = prompt,
    )

    override fun getPrompt(actor: BaseActor<CodeResult>): String = (actor as CodingActor).details!!
    override fun resultMapper(result: CodeResult): String = result.getCode()
}

