package com.simiacryptus.skyenet.core.actors.test

import com.simiacryptus.skyenet.core.actors.BaseActor
import com.simiacryptus.skyenet.core.actors.ImageActor
import com.simiacryptus.skyenet.core.actors.ImageResponse
import com.simiacryptus.skyenet.core.actors.ParsedResponse
import java.util.function.Function

abstract class ImageActorTestBase() : ActorTestBase<ImageResponse>() {
    override fun actorFactory(prompt: String) = ImageActor(
        prompt = prompt,
    )

}