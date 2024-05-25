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
import com.simiacryptus.skyenet.core.platform.ApplicationServices.dataStorageFactory
import com.simiacryptus.skyenet.core.platform.ApplicationServices.dataStorageRoot
import com.simiacryptus.skyenet.core.platform.ApplicationServices.userSettingsManager
import com.simiacryptus.skyenet.core.platform.AuthorizationInterface.OperationType
import org.apache.hc.client5.http.impl.classic.CloseableHttpClient
import org.apache.hc.core5.http.HttpRequest
import org.slf4j.LoggerFactory
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
        log.debug("Fetching client for session: {}, user: {}", session, user)
        val key = SessionKey(session, user)
        return if (null == dataStorage) clientCache[key] ?: throw IllegalStateException("No data storage")
        else clientCache.getOrPut(key) { createClient(session, user, dataStorage)!! }
    }

    protected open fun createPool(session: Session, user: User?) =
        ThreadPoolExecutor(
            0, Integer.MAX_VALUE,
            500, TimeUnit.MILLISECONDS,
            SynchronousQueue(),
            RecordingThreadFactory(session, user)
        )

    /*createScheduledPool*/
    protected open fun createScheduledPool(session: Session, user: User?, dataStorage: StorageInterface?) =
        MoreExecutors.listeningDecorator(ScheduledThreadPoolExecutor(1))

    fun getPool(
        session: Session,
        user: User?,
    ): ThreadPoolExecutor {
        log.debug("Fetching thread pool for session: {}, user: {}", session, user)
        val key = SessionKey(session, user)
        return poolCache.getOrPut(key) {
            createPool(session, user)
        }
    }

    fun getScheduledPool(
        session: Session,
        user: User?,
        dataStorage: StorageInterface?,
    ): ListeningScheduledExecutorService {
        log.debug("Fetching scheduled pool for session: {}, user: {}", session, user)
        val key = SessionKey(session, user)
        return scheduledPoolCache.getOrPut(key) {
            createScheduledPool(session, user, dataStorage)
        }
    }

    inner class RecordingThreadFactory(
        val session: Session,
        val user: User?
    ) : ThreadFactory {
        private val inner = ThreadFactoryBuilder().setNameFormat("Session $session; User $user; #%d").build()
        val threads = mutableSetOf<Thread>()
        override fun newThread(r: Runnable): Thread {
            log.debug("Creating new thread for session: {}, user: {}", session, user)
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
        log.debug("Creating client for session: {}, user: {}", session, user)
        val sessionDir = dataStorageFactory(dataStorageRoot).getSessionDir(user, session).apply { mkdirs() }
        if (user != null) {
            val userSettings = userSettingsManager.getUserSettings(user)
            val userApi =
                if (userSettings.apiKeys.isNotEmpty())
                    MonitoredClient(
                        key = userSettings.apiKeys,
                        apiBase = userSettings.apiBase,
                        logfile = sessionDir.resolve("openai.log"),
                        session = session,
                        user = user,
                        workPool = getPool(session, user),
                    ) else null
            if (userApi != null) return userApi
        }
        val canUseGlobalKey = ApplicationServices.authorizationManager.isAuthorized(
            null, user, OperationType.GlobalKey
        )
        if (!canUseGlobalKey) throw RuntimeException("No API key")
        return (if (ClientUtil.keyMap.isNotEmpty()) {
            MonitoredClient(
                key = ClientUtil.keyMap.mapKeys { APIProvider.valueOf(it.key) },
                logfile = sessionDir?.resolve("openai.log"),
                session = session,
                user = user,
                workPool = getPool(session, user),
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
            log.debug("Authorizing request for session: {}, user: {}, apiProvider: {}", session, user, apiProvider)
            require(budget > 0.0) { "Budget Exceeded" }
            super.authorize(request, ClientUtil.defaultApiProvider)
        }

        override fun onUsage(model: OpenAIModel?, tokens: ApiModel.Usage) {
            log.debug(
                "Usage recorded for session: {}, user: {}, model: {}, tokens: {}",
                session,
                user,
                model,
                tokens
            )
            ApplicationServices.usageManager.incrementUsage(session, user, model!!, tokens)
            budget -= tokens.cost ?: 0.0
            super.onUsage(model, tokens)
        }
    }

    companion object {
        private val log = LoggerFactory.getLogger(ClientManager::class.java)
    }
}