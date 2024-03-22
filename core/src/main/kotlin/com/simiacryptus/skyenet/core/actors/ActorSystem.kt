package com.simiacryptus.skyenet.core.actors

import com.simiacryptus.skyenet.core.actors.record.*
import com.simiacryptus.skyenet.core.platform.ApplicationServices
import com.simiacryptus.skyenet.core.platform.Session
import com.simiacryptus.skyenet.core.platform.StorageInterface
import com.simiacryptus.skyenet.core.platform.User
import com.simiacryptus.skyenet.core.util.FunctionWrapper
import com.simiacryptus.skyenet.core.util.JsonFunctionRecorder
import java.io.File

open class ActorSystem<T : Enum<*>>(
  val actors: Map<String, BaseActor<*, *>>,
  val dataStorage: StorageInterface,
  val user: User?,
  val session: Session
) {
  private val sessionDir = dataStorage.getSessionDir(user, session)
  protected val pool by lazy { ApplicationServices.clientManager.getPool(session, user, dataStorage) }

  private val actorMap = mutableMapOf<T, BaseActor<*, *>>()

  fun getActor(actor: T): BaseActor<*, *> {
    return synchronized(actorMap) {
      actorMap.computeIfAbsent(actor) { innerActor ->
        try {
          val wrapper = getWrapper(actor.name)
          when (val baseActor = actors[actor.name]) {
            null -> throw RuntimeException("No actor for $actor")
            is SimpleActor -> SimpleActorInterceptor(
              inner = baseActor as SimpleActor,
              functionInterceptor = wrapper
            )

            is ParsedActor<*> -> ParsedActorInterceptor(
              inner = (baseActor as ParsedActor<*>),
              functionInterceptor = wrapper
            )

            is CodingActor -> CodingActorInterceptor(
              inner = baseActor as CodingActor,
              functionInterceptor = wrapper
            )

            is ImageActor -> ImageActorInterceptor(
              inner = baseActor as ImageActor,
              functionInterceptor = wrapper
            )

            is TextToSpeechActor -> TextToSpeechActorInterceptor(
              inner = baseActor as TextToSpeechActor,
              functionInterceptor = wrapper
            )

            else -> throw RuntimeException("Unknown actor type: ${baseActor.javaClass}")
          }
        } catch (e: Throwable) {
          log.warn("Error creating actor $actor", e)
          actors[actor.name]!!
        }
      }
    }
  }

  private val wrapperMap = mutableMapOf<String, FunctionWrapper>()
  private fun getWrapper(name: String) = synchronized(wrapperMap) {
    wrapperMap.getOrPut(name) {
      FunctionWrapper(JsonFunctionRecorder(File(sessionDir, ".sys/actors/$name")))
    }
  }

  companion object {
    private val log = org.slf4j.LoggerFactory.getLogger(ActorSystem::class.java)
  }
}
