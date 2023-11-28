package com.simiacryptus.skyenet.core.platform

import com.google.common.util.concurrent.ListeningExecutorService
import com.google.common.util.concurrent.ListeningScheduledExecutorService
import com.google.common.util.concurrent.MoreExecutors
import com.google.common.util.concurrent.ThreadFactoryBuilder
import com.simiacryptus.jopenai.ApiModel
import com.simiacryptus.jopenai.ClientUtil
import com.simiacryptus.jopenai.HttpClientManager
import com.simiacryptus.jopenai.OpenAIClient

import com.simiacryptus.jopenai.models.OpenAIModel
import org.apache.hc.client5.http.impl.classic.CloseableHttpClient
import org.slf4j.LoggerFactory
import org.slf4j.event.Level
import java.io.File
import java.util.concurrent.*

open class ClientManager {

  private data class SessionKey(val session: Session, val user: User?)

  private val clientCache = mutableMapOf<SessionKey, OpenAIClient>()
  private val poolCache = mutableMapOf<SessionKey, ThreadPoolExecutor>()

  fun getClient(
    session: Session,
    user: User?,
    dataStorage: DataStorage?,
  ): OpenAIClient {
    val key = SessionKey(session, user)
    return if (null == dataStorage) clientCache[key] ?: throw IllegalStateException("No data storage")
    else clientCache.getOrPut(key) { createClient(session, user, dataStorage) }
  }

  fun getPool(
    session: Session,
    user: User?,
    dataStorage: DataStorage?,
  ): ThreadPoolExecutor {
    val key = SessionKey(session, user)
    return poolCache.getOrPut(key) {
      createPool(session, user, dataStorage)
    }
  }

  protected open fun createPool(session: Session, user: User?, dataStorage: DataStorage?) =
    ThreadPoolExecutor(
      0, Integer.MAX_VALUE,
      500, TimeUnit.MILLISECONDS,
      SynchronousQueue(),
      RecordingThreadFactory(session, user)
    )

  inner class RecordingThreadFactory(
    private val session: Session,
    private val user: User?
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
    dataStorage: DataStorage?,
  ): OpenAIClient {
    if (user != null) {
      val userSettings = ApplicationServices.userSettingsManager.getUserSettings(user)
      val logfile = dataStorage?.getSessionDir(user, session)?.resolve(".sys/openai.log")
      logfile?.parentFile?.mkdirs()
      val userApi =
        if (userSettings.apiKey.isNullOrBlank()) null else MonitoredClient(
          key = userSettings.apiKey,
          logfile = logfile,
          session = session,
          user = user,
          workPool = getPool(session, user, dataStorage),
        )
      if (userApi != null) return userApi
    }
    val canUseGlobalKey = ApplicationServices.authorizationManager.isAuthorized(
      null, user, AuthorizationManager.OperationType.GlobalKey
    )
    if (!canUseGlobalKey) throw RuntimeException("No API key")
    val logfile = dataStorage?.getSessionDir(user, session)?.resolve(".sys/openai.log")
    logfile?.parentFile?.mkdirs()
    return (if (ClientUtil.keyTxt.isNullOrBlank()) null else MonitoredClient(
      key = ClientUtil.keyTxt,
      logfile = logfile,
      session = session,
      user = user,
      workPool = getPool(session, user, dataStorage),
    ))!!
  }

  inner class MonitoredClient(
    key: String,
    logfile: File?,
    private val session: Session,
    private val user: User?,
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
  ) {
    override fun incrementTokens(model: OpenAIModel?, tokens: ApiModel.Usage) {
      ApplicationServices.usageManager.incrementUsage(session, user, model!!, tokens)
      super.incrementTokens(model, tokens)
    }
  }

  private val log = LoggerFactory.getLogger(ClientManager::class.java)
}