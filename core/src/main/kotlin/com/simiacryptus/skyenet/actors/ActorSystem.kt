package com.simiacryptus.skyenet.actors

import com.simiacryptus.skyenet.actors.record.*
import com.simiacryptus.skyenet.config.DataStorage
import com.simiacryptus.skyenet.util.FunctionWrapper
import com.simiacryptus.skyenet.util.JsonFunctionRecorder
import java.io.File

open class ActorSystem<T:Enum<*>>(
    private val actors: Map<T, BaseActor<*>>,
    private val dataStorage: DataStorage,
    val userId: String?,
    val sessionId: String
) {
    val sessionDir = dataStorage.getSessionDir(userId, sessionId)
    fun getActor(actor: T): BaseActor<*> {
        val wrapper = FunctionWrapper(JsonFunctionRecorder(File(sessionDir, "${actor.name}.json")))
        return when (val baseActor = actors[actor]) {
            null -> throw RuntimeException("No actor for $actor")
            is SimpleActor -> RecordingSimpleActor(baseActor, wrapper)
            is ParsedActor<*> -> RecordingParsedActor(baseActor, wrapper)
            is CodingActor -> RecordingCodingActor(baseActor, wrapper)
            else -> throw RuntimeException("Unknown actor type: ${baseActor.javaClass}")
        }
    }
}
