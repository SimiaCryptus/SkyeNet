package com.simiacryptus.skyenet.core.actors.test

import com.simiacryptus.skyenet.core.actors.ImageActor
import com.simiacryptus.skyenet.core.actors.ImageResponse

abstract class ImageActorTestBase() : ActorTestBase<List<String>,ImageResponse>() {
    override fun actorFactory(prompt: String) = ImageActor(
        prompt = prompt,
    )

}