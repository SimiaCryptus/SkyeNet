package com.simiacryptus.skyenet.core.actors

import com.simiacryptus.skyenet.core.platform.ApplicationServices
import com.simiacryptus.skyenet.core.platform.Session
import com.simiacryptus.skyenet.core.platform.model.StorageInterface
import com.simiacryptus.skyenet.core.platform.model.User

open class ActorSystem<T : Enum<*>>(
    val actors: Map<String, BaseActor<*, *>>,
    dataStorage: StorageInterface,
    user: User?,
    session: Session
) : PoolSystem(dataStorage, user, session) {

    fun getActor(actor: T) = actors.get(actor.name)!!
}

open class PoolSystem(
    val dataStorage: StorageInterface,
    val user: User?,
    val session: Session
) {
    protected val pool by lazy { ApplicationServices.clientManager.getPool(session, user) }

}
