package com.simiacryptus.skyenet.platform

import com.simiacryptus.openai.OpenAIClient
import com.simiacryptus.openai.OpenAIClientBase
import com.simiacryptus.openai.models.OpenAIModel
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
        val canUseGlobalKey = ApplicationServices.authorizationManager.isAuthorized(null, user,
            AuthorizationManager.OperationType.GlobalKey
        )
        if (!canUseGlobalKey) throw RuntimeException("No API key")
        val logfile = dataStorage.getSessionDir(user, session).resolve(".sys/openai.log")
        logfile.parentFile?.mkdirs()
        return createClient(session, user, logfile)!!
    }

    open protected fun createClient(
        session: Session,
        user: User?,
        logfile: File,
        key: String? = OpenAIClientBase.keyTxt
    ): OpenAIClient? = if (key.isNullOrBlank()) null else object : OpenAIClient(
        key = key,
        logLevel = Level.DEBUG,
        logStreams = mutableListOf(
            logfile?.outputStream()?.buffered()
        ).filterNotNull().toMutableList(),
    ) {
        override fun incrementTokens(model: OpenAIModel?, tokens: Usage) {
            ApplicationServices.usageManager.incrementUsage(session, user, model!!, tokens)
            super.incrementTokens(model, tokens)
        }
    }

    private val log = org.slf4j.LoggerFactory.getLogger(ClientManager::class.java)
}