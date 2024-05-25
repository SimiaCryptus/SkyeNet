package com.simiacryptus.skyenet.core.actors

import com.simiacryptus.skyenet.core.actors.record.*
import com.simiacryptus.skyenet.core.platform.ApplicationServices
import com.simiacryptus.skyenet.core.platform.Session
import com.simiacryptus.skyenet.core.platform.StorageInterface
import com.simiacryptus.skyenet.core.platform.User
import com.simiacryptus.skyenet.core.util.FunctionWrapper
import com.simiacryptus.skyenet.core.util.JsonFunctionRecorder

open class ActorSystem<T : Enum<*>>(
    val actors: Map<String, BaseActor<*, *>>,
    val dataStorage: StorageInterface,
    val user: User?,
    val session: Session
) {
    protected val pool by lazy { ApplicationServices.clientManager.getPool(session, user) }

    private val actorMap = mutableMapOf<T, BaseActor<*, *>>()

    fun getActor(actor: T): BaseActor<*, *> {
        return synchronized(actorMap) {
            actorMap.computeIfAbsent(actor) { innerActor ->
                try {
                    val wrapper = getWrapper(actor.name)
                    when (val baseActor = actors[actor.name]) {
                        null -> throw RuntimeException("No actor for $actor")
                        is SimpleActor -> SimpleActorInterceptor(
                            inner = baseActor,
                            functionInterceptor = wrapper
                        )

                        is ParsedActor<*> -> ParsedActorInterceptor(
                            inner = baseActor,
                            functionInterceptor = wrapper
                        )

                        is CodingActor -> CodingActorInterceptor(
                            inner = baseActor,
                            functionInterceptor = wrapper
                        )

                        is ImageActor -> ImageActorInterceptor(
                            inner = baseActor,
                            functionInterceptor = wrapper
                        )

                        is TextToSpeechActor -> TextToSpeechActorInterceptor(
                            inner = baseActor,
                            functionInterceptor = wrapper
                        )

                        else -> throw RuntimeException("Unknown actor type: ${baseActor.javaClass}")
                    }
                } catch (e: Throwable) {
                    val baseActor = actors[actor.name]!!
                    log.warn("Error creating actor $actor, returning $baseActor", e)
                    baseActor
                }
            }
        }
    }

    private val wrapperMap = mutableMapOf<String, FunctionWrapper>()
    private fun getWrapper(name: String) = synchronized(wrapperMap) {
        wrapperMap.getOrPut(name) {
            FunctionWrapper(JsonFunctionRecorder(
                dataStorage.getSessionDir(user, session).resolve("actors/$name").apply { mkdirs() }
            ))
        }
    }

    companion object {
        private val log = org.slf4j.LoggerFactory.getLogger(ActorSystem::class.java)
    }
}
