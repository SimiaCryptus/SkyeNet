package com.simiacryptus.skyenet.core.platform

import com.google.common.util.concurrent.ListeningScheduledExecutorService
import com.google.common.util.concurrent.MoreExecutors
import com.google.common.util.concurrent.ThreadFactoryBuilder
import com.simiacryptus.jopenai.ApiModel
import com.simiacryptus.jopenai.HttpClientManager
import com.simiacryptus.jopenai.OpenAIClient
import com.simiacryptus.jopenai.models.APIProvider
import com.simiacryptus.jopenai.models.OpenAIModel
import com.simiacryptus.jopenai.util.ClientUtil
import com.simiacryptus.skyenet.core.platform.AuthorizationInterface.OperationType
import com.simiacryptus.skyenet.core.platform.file.DataStorage.Companion.SYS_DIR
import org.apache.hc.client5.http.impl.classic.CloseableHttpClient
import org.apache.hc.core5.http.HttpRequest
import org.slf4j.event.Level
import java.io.File
import java.util.concurrent.*

open class ClientManager {

    private data class SessionKey(val session: Session, val user: User?)

    private val clientCache = mutableMapOf<SessionKey, OpenAIClient>()
    private val poolCache = mutableMapOf<SessionKey, ThreadPoolExecutor>()
    private val scheduledPoolCache = mutableMapOf<SessionKey, ListeningScheduledExecutorService>()

    fun getClient(
        session: Session,
        user: User?,
        dataStorage: StorageInterface?,
    ): OpenAIClient {
        val key = SessionKey(session, user)
        return if (null == dataStorage) clientCache[key] ?: throw IllegalStateException("No data storage")
        else clientCache.getOrPut(key) { createClient(session, user, dataStorage)!! }
    }

    protected open fun createPool(session: Session, user: User?, dataStorage: StorageInterface?) =
        ThreadPoolExecutor(
            0, Integer.MAX_VALUE,
            500, TimeUnit.MILLISECONDS,
            SynchronousQueue(),
            RecordingThreadFactory(session, user)
        )
    /*createScheduledPool*/
    protected open fun createScheduledPool(session: Session, user: User?, dataStorage: StorageInterface?) =
        MoreExecutors.listeningDecorator(ScheduledThreadPoolExecutor(1,))

    fun getPool(
        session: Session,
        user: User?,
        dataStorage: StorageInterface?,
    ): ThreadPoolExecutor {
        val key = SessionKey(session, user)
        return poolCache.getOrPut(key) {
            createPool(session, user, dataStorage)
        }
    }

    fun getScheduledPool(
        session: Session,
        user: User?,
        dataStorage: StorageInterface?,
    ): ListeningScheduledExecutorService {
        val key = SessionKey(session, user)
        return scheduledPoolCache.getOrPut(key) {
            createScheduledPool(session, user, dataStorage)
        }
    }

    inner class RecordingThreadFactory(
        session: Session,
        user: User?
    ) : ThreadFactory {
        private val inner = ThreadFactoryBuilder().setNameFormat("Session $session; User $user; #%d").build()
        val threads = mutableSetOf<Thread>()
        override fun newThread(r: Runnable): Thread {
            inner.newThread(r).also {
                threads.add(it)
                return it
            }
        }
    }

    protected open fun createClient(
        session: Session,
        user: User?,
        dataStorage: StorageInterface?,
    ): OpenAIClient? {
        if (user != null) {
            val userSettings = ApplicationServices.userSettingsManager.getUserSettings(user)
            val logfile = SYS_DIR?.resolve("${if (session.isGlobal()) "global" else user}/$session/openai.log")?.apply { parentFile?.mkdirs() }
            logfile?.parentFile?.mkdirs()
            val userApi =
                if (userSettings.apiKeys.isNotEmpty())
                    MonitoredClient(
                        key = userSettings.apiKeys,
                        apiBase = userSettings.apiBase,
                        logfile = logfile,
                        session = session,
                        user = user,
                        workPool = getPool(session, user, dataStorage),
                    ) else null
            if (userApi != null) return userApi
        }
        val canUseGlobalKey = ApplicationServices.authorizationManager.isAuthorized(
            null, user, OperationType.GlobalKey
        )
        if (!canUseGlobalKey) throw RuntimeException("No API key")
        val logfile = SYS_DIR?.resolve("${if (session.isGlobal()) "global" else user}/$session/openai.log")?.apply { parentFile?.mkdirs() }
        logfile?.parentFile?.mkdirs()
        return (if (ClientUtil.keyMap.isNotEmpty()) {
            MonitoredClient(
                key = ClientUtil.keyMap.mapKeys { APIProvider.valueOf(it.key) },
                logfile = logfile,
                session = session,
                user = user,
                workPool = getPool(session, user, dataStorage),
            )
        } else {
            null
        })!!
    }

    inner class MonitoredClient(
        key: Map<APIProvider, String>,
        logfile: File?,
        private val session: Session,
        private val user: User?,
        apiBase: Map<APIProvider, String> = APIProvider.values().associate { it to (it.base ?: "") },
        scheduledPool: ListeningScheduledExecutorService = HttpClientManager.scheduledPool,
        workPool: ThreadPoolExecutor = HttpClientManager.workPool,
        client: CloseableHttpClient = HttpClientManager.client
    ) : OpenAIClient(
        key = key,
        logLevel = Level.DEBUG,
        logStreams = listOfNotNull(
            logfile?.outputStream()?.buffered()
        ).toMutableList(),
        scheduledPool = scheduledPool,
        workPool = workPool,
        client = client,
        apiBase = apiBase,
    ) {
        var budget = 2.00
        override fun authorize(request: HttpRequest, apiProvider: APIProvider) {
            require(budget > 0.0) { "Budget Exceeded" }
            super.authorize(request, ClientUtil.defaultApiProvider)
        }

        override fun onUsage(model: OpenAIModel?, tokens: ApiModel.Usage) {
            ApplicationServices.usageManager.incrementUsage(session, user, model!!, tokens)
            budget -= tokens.cost ?: 0.0
            super.onUsage(model, tokens)
        }
    }

    companion object
}