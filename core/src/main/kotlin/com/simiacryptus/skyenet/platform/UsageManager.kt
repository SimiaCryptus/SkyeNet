package com.simiacryptus.skyenet.platform

import com.simiacryptus.openai.OpenAIClient
import com.simiacryptus.openai.models.*
import com.simiacryptus.util.JsonUtil
import java.io.File
import java.io.FileWriter
import java.util.concurrent.Executors
import java.util.concurrent.TimeUnit
import java.util.concurrent.atomic.AtomicInteger

open class UsageManager {

    private val scheduler = Executors.newSingleThreadScheduledExecutor()
    private val txLogFile = File(".skyenet/usage/log.csv")
    @Volatile private var txLogFileWriter: FileWriter?
    private val usagePerSession = HashMap<SessionID, UsageCounters>()
    private val sessionsByUser = HashMap<UserInfo, HashSet<SessionID>>()
    private val usersBySession = HashMap<SessionID, HashSet<UserInfo>>()

    init {
        txLogFile.parentFile.mkdirs()
        loadFromLog(txLogFile)
        txLogFileWriter = FileWriter(txLogFile, true)
        scheduler.scheduleAtFixedRate({ saveCounters() }, 1, 1, TimeUnit.HOURS)
    }

    @Suppress("MemberVisibilityCanBePrivate")
    open fun loadFromLog(file: File) {
        if (file.exists()) {
            try {
                file.readLines().forEach { line ->
                    val (sessionId, user, model, tokens, direction) = line.split(",")
                    val modelEnum = listOf(
                        ChatModels.values(),
                        CompletionModels.values(),
                        EditModels.values(),
                        EmbeddingModels.values()
                    ).flatMap { it.toList() }.find { model == it.modelName }
                        ?: throw RuntimeException("Unknown model $model")
                    when (direction) {
                        "input" -> incrementUsage(
                            SessionID(sessionId),
                            UserInfo(email=user),
                            modelEnum,
                            OpenAIClient.Usage(prompt_tokens = tokens.toInt())
                        )

                        "output" -> incrementUsage(
                            SessionID(sessionId),
                            UserInfo(email=user),
                            modelEnum,
                            OpenAIClient.Usage(completion_tokens = tokens.toInt())
                        )

                        else -> throw RuntimeException("Unknown direction $direction")
                    }
                }
            } catch (e: Exception) {
                log.warn("Error loading log file", e)
            }
        }
    }

    @Suppress("MemberVisibilityCanBePrivate")
    protected open fun writeCompactLog(file: File) {
        val writer = FileWriter(file)
        usagePerSession.forEach { (sessionId, usage) ->
            val user = usersBySession[sessionId]?.firstOrNull()
            usage.tokensPerModel.forEach { (model, counter) ->
                writer.write("$sessionId,$user,${model.model.modelName},${counter.inputTokens.get()},input\n")
                writer.write("$sessionId,$user,${model.model.modelName},${counter.outputTokens.get()},output\n")
            }
        }
        writer.flush()
        writer.close()
    }

    private fun saveCounters() {
        txLogFileWriter = FileWriter(txLogFile, true)
        val timedFile = File(txLogFile.absolutePath + "." + System.currentTimeMillis())
        writeCompactLog(timedFile)
        val swapFile = File(txLogFile.absolutePath + ".old")
        synchronized(txLogFile) {
            try {
                txLogFileWriter?.close()
            } catch (e: Exception) {
                log.warn("Error closing log file", e)
            }
            try {
                txLogFile.renameTo(swapFile)
            } catch (e: Exception) {
                log.warn("Error renaming log file", e)
            }
            try {
                timedFile.renameTo(txLogFile)
            } catch (e: Exception) {
                log.warn("Error renaming log file", e)
            }
            try {
                swapFile.renameTo(timedFile)
            } catch (e: Exception) {
                log.warn("Error renaming log file", e)
            }
            txLogFileWriter = FileWriter(txLogFile, true)
        }
        File(".skyenet/usage/counters.json").writeText(JsonUtil.toJson(usagePerSession))
    }

    open fun incrementUsage(sessionId: SessionID, user: UserInfo?, model: OpenAIModel, tokens: OpenAIClient.Usage) {
        @Suppress("NAME_SHADOWING") val user = if(null == user) null else UserInfo(email= user.email) // Hack
        val usage = usagePerSession.getOrPut(sessionId) {
            UsageCounters()
        }
        val tokensPerModel = usage.tokensPerModel.getOrPut(UsageKey(sessionId, user, model)) {
            UsageValues()
        }
        tokensPerModel.addAndGet(tokens)
        if (user != null) {
            val sessions = sessionsByUser.getOrPut(user) {
                HashSet()
            }
            sessions.add(sessionId)
        }
        try {
            val txLogFileWriter = txLogFileWriter
            if(null != txLogFileWriter) {
                synchronized(txLogFile) {
                    txLogFileWriter.write("$sessionId,$user,${model.modelName},${tokens.prompt_tokens},input\n")
                    txLogFileWriter.write("$sessionId,$user,${model.modelName},${tokens.completion_tokens},output\n")
                    txLogFileWriter.flush()
                }
            }
        } catch (e: Exception) {
            log.warn("Error incrementing usage", e)
        }
    }

    open fun getUserUsageSummary(user: UserInfo): Map<OpenAIModel, OpenAIClient.Usage> {
        @Suppress("NAME_SHADOWING") val user = if(null == user) null else UserInfo(email= user.email) // Hack
        return sessionsByUser[user]?.flatMap { sessionId ->
            val usage = usagePerSession[sessionId]
            usage?.tokensPerModel?.entries?.map { (model, counter) ->
                model.model to counter.toUsage()
            } ?: emptyList()
        }?.groupBy { it.first }?.mapValues {
            it.value.map { it.second }.reduce { a, b ->
                OpenAIClient.Usage(
                    prompt_tokens = a.prompt_tokens + b.prompt_tokens,
                    completion_tokens = a.completion_tokens + b.completion_tokens
                )
            }
        } ?: emptyMap()
    }

    open fun getSessionUsageSummary(sessionId: SessionID): Map<OpenAIModel, OpenAIClient.Usage> =
        usagePerSession[sessionId]?.tokensPerModel?.entries?.map { (model, counter) ->
            model.model to counter.toUsage()
        }?.groupBy { it.first }?.mapValues { it.value.map { it.second }.reduce { a, b ->
            OpenAIClient.Usage(
                prompt_tokens = a.prompt_tokens + b.prompt_tokens,
                completion_tokens = a.completion_tokens + b.completion_tokens
            )
        } } ?: emptyMap()

    data class UsageKey(
        val sessionId: SessionID,
        val user: UserInfo?,
        val model: OpenAIModel,
    )

    class UsageValues(
        val inputTokens: AtomicInteger = AtomicInteger(),
        val outputTokens: AtomicInteger = AtomicInteger(),
    ) {
        fun addAndGet(tokens: OpenAIClient.Usage) {
            inputTokens.addAndGet(tokens.prompt_tokens)
            outputTokens.addAndGet(tokens.completion_tokens)
        }

        fun toUsage() = OpenAIClient.Usage(
            prompt_tokens = inputTokens.get(),
            completion_tokens = outputTokens.get()
        )
    }

    data class UsageCounters(
        val tokensPerModel: HashMap<UsageKey, UsageValues> = HashMap(),
    )

    companion object {
        private val log = org.slf4j.LoggerFactory.getLogger(UsageManager::class.java)
    }
}