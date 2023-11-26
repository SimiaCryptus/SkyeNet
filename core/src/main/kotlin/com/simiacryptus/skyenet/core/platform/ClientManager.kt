package com.simiacryptus.skyenet.core.platform

import com.simiacryptus.jopenai.ApiModel
import com.simiacryptus.jopenai.ClientUtil
import com.simiacryptus.jopenai.OpenAIClient

import com.simiacryptus.jopenai.models.OpenAIModel
import org.slf4j.LoggerFactory
import org.slf4j.event.Level
import java.io.File

open class ClientManager {

    open fun createClient(
        session: Session,
        user: User?,
        dataStorage: DataStorage,
    ): OpenAIClient {
        if (user != null) {
            val userSettings = ApplicationServices.userSettingsManager.getUserSettings(user)
            val logfile = dataStorage.getSessionDir(user, session).resolve(".sys/openai.log")
            logfile.parentFile?.mkdirs()
            val userApi = createClient(session, user, logfile, userSettings.apiKey)
            if (userApi != null) return userApi
        }
        val canUseGlobalKey = ApplicationServices.authorizationManager.isAuthorized(
            null, user, AuthorizationManager.OperationType.GlobalKey
        )
        if (!canUseGlobalKey) throw RuntimeException("No API key")
        val logfile = dataStorage.getSessionDir(user, session).resolve(".sys/openai.log")
        logfile.parentFile?.mkdirs()
        return createClient(session, user, logfile)!!
    }

    protected open fun createClient(
        session: Session, user: User?, logfile: File, key: String? = ClientUtil.keyTxt
    ): OpenAIClient? = if (key.isNullOrBlank()) null else MonitoredClient(key, logfile, session, user)

    inner class MonitoredClient(
        key: String,
        logfile: File,
        private val session: Session,
        private val user: User?
    ) : OpenAIClient(
        key = key,
        logLevel = Level.DEBUG,
        logStreams = mutableListOf(
            logfile.outputStream().buffered()
        ),
    ) {
        override fun incrementTokens(model: OpenAIModel?, tokens: ApiModel.Usage) {
            ApplicationServices.usageManager.incrementUsage(session, user, model!!, tokens)
            super.incrementTokens(model, tokens)
        }
    }

    private val log = LoggerFactory.getLogger(ClientManager::class.java)
}