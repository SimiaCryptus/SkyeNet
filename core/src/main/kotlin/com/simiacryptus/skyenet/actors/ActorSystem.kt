package com.simiacryptus.skyenet.actors

import com.simiacryptus.skyenet.actors.record.*
import com.simiacryptus.skyenet.platform.DataStorage
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
        val wrapper = getWrapper(actor.name)
        return when (val baseActor = actors[actor]) {
            null -> throw RuntimeException("No actor for $actor")
            is SimpleActor -> SimpleActorInterceptor(baseActor, wrapper)
            is ParsedActor<*> -> ParsedActorInterceptor(baseActor, wrapper)
            is CodingActor -> CodingActorInterceptor(baseActor, wrapper)
            else -> throw RuntimeException("Unknown actor type: ${baseActor.javaClass}")
        }
    }

    private val wrapperMap = mutableMapOf<String, FunctionWrapper>()
    private fun getWrapper(name: String) = wrapperMap.computeIfAbsent(name) {
        FunctionWrapper(JsonFunctionRecorder(File(sessionDir, "actors/$name")))
    }
}
