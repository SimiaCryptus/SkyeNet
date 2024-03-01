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
  val actors: Map<T, BaseActor<*, *>>,
  val dataStorage: StorageInterface,
  val user: User?,
  val session: Session
) {
  private val sessionDir = dataStorage.getSessionDir(user, session)
  protected val pool by lazy { ApplicationServices.clientManager.getPool(session, user, dataStorage) }

  private val actorMap = mutableMapOf<T, BaseActor<*, *>>()

  fun getActor(actor: T): BaseActor<*, *> {
    return synchronized(actorMap) {
      actorMap.computeIfAbsent(actor) {
        try {
          val wrapper = getWrapper(actor.name)
          when (val baseActor = actors[actor]) {
            null -> throw RuntimeException("No actor for $actor")
            is SimpleActor -> SimpleActorInterceptor(baseActor, wrapper)
            is ParsedActor<*> -> ParsedActorInterceptor(baseActor, wrapper)
            is CodingActor -> CodingActorInterceptor(baseActor, wrapper)
            is ImageActor -> ImageActorInterceptor(baseActor, wrapper)
            is TextToSpeechActor -> TextToSpeechActorInterceptor(baseActor, wrapper)
            else -> throw RuntimeException("Unknown actor type: ${baseActor.javaClass}")
          }
        } catch (e: Throwable) {
          log.warn("Error creating actor $actor", e)
          actors[actor]!!
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
