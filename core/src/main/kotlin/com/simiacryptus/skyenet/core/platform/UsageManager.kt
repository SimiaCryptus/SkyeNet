package com.simiacryptus.skyenet.core.platform

import com.simiacryptus.jopenai.models.*
import com.simiacryptus.jopenai.util.JsonUtil
import java.io.File
import java.io.FileWriter
import java.util.concurrent.ConcurrentHashMap
import java.util.concurrent.Executors
import java.util.concurrent.TimeUnit
import java.util.concurrent.atomic.AtomicInteger

open class UsageManager {

    private val scheduler = Executors.newSingleThreadScheduledExecutor()
    private val txLogFile = File(".skyenet/usage/log.csv")
    @Volatile private var txLogFileWriter: FileWriter?
    private val usagePerSession = ConcurrentHashMap<Session, UsageCounters>()
    private val sessionsByUser = ConcurrentHashMap<User, HashSet<Session>>()
    private val usersBySession = ConcurrentHashMap<Session, HashSet<User>>()

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
                            Session(sessionId),
                            User(email=user),
                            modelEnum,
                            com.simiacryptus.jopenai.ApiModel.Usage(prompt_tokens = tokens.toInt())
                        )

                        "output" -> incrementUsage(
                            Session(sessionId),
                            User(email=user),
                            modelEnum,
                            com.simiacryptus.jopenai.ApiModel.Usage(completion_tokens = tokens.toInt())
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
        FileWriter(file).use { writer ->
            usagePerSession.forEach { (sessionId, usage) ->
                val user = usersBySession[sessionId]?.firstOrNull()
                usage.tokensPerModel.forEach { (model, counter) ->
                    writer.write("$sessionId,${user?.email},${model.model.modelName},${counter.inputTokens.get()},input\n")
                    writer.write("$sessionId,${user?.email},${model.model.modelName},${counter.outputTokens.get()},output\n")
                }
            }
            writer.flush()
        }
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

    open fun incrementUsage(session: Session, user: User?, model: OpenAIModel, tokens: com.simiacryptus.jopenai.ApiModel.Usage) {
        @Suppress("NAME_SHADOWING") val user = if (null == user) null else User(email = user.email) // Hack
        usagePerSession.computeIfAbsent(session) { UsageCounters() }
            .tokensPerModel.computeIfAbsent(UsageKey(session, user, model)) { UsageValues() }
                .addAndGet(tokens)
        if (user != null) {
            sessionsByUser.computeIfAbsent(user) { HashSet() }.add(session)
        }
        try {
            val txLogFileWriter = txLogFileWriter
            if (null != txLogFileWriter) {
                synchronized(txLogFile) {
                    txLogFileWriter.write("$session,${user?.email},${model.modelName},${tokens.prompt_tokens},input\n")
                    txLogFileWriter.write("$session,${user?.email},${model.modelName},${tokens.completion_tokens},output\n")
                    txLogFileWriter.flush()
                }
            }
        } catch (e: Exception) {
            log.warn("Error incrementing usage", e)
        }
    }

    open fun getUserUsageSummary(user: User): Map<OpenAIModel, com.simiacryptus.jopenai.ApiModel.Usage> {
        @Suppress("NAME_SHADOWING") val user = if(null == user) null else User(email= user.email) // Hack
        return sessionsByUser[user]?.flatMap { sessionId ->
            val usage = usagePerSession[sessionId]
            usage?.tokensPerModel?.entries?.map { (model, counter) ->
                model.model to counter.toUsage()
            } ?: emptyList()
        }?.groupBy { it.first }?.mapValues {
            it.value.map { it.second }.reduce { a, b ->
                com.simiacryptus.jopenai.ApiModel.Usage(
                    prompt_tokens = a.prompt_tokens + b.prompt_tokens,
                    completion_tokens = a.completion_tokens + b.completion_tokens
                )
            }
        } ?: emptyMap()
    }

    open fun getSessionUsageSummary(session: Session): Map<OpenAIModel, com.simiacryptus.jopenai.ApiModel.Usage> =
        usagePerSession[session]?.tokensPerModel?.entries?.map { (model, counter) ->
            model.model to counter.toUsage()
        }?.groupBy { it.first }?.mapValues { it.value.map { it.second }.reduce { a, b ->
            com.simiacryptus.jopenai.ApiModel.Usage(
                prompt_tokens = a.prompt_tokens + b.prompt_tokens,
                completion_tokens = a.completion_tokens + b.completion_tokens
            )
        } } ?: emptyMap()

    data class UsageKey(
        val session: Session,
        val user: User?,
        val model: OpenAIModel,
    )

    class UsageValues(
        val inputTokens: AtomicInteger = AtomicInteger(),
        val outputTokens: AtomicInteger = AtomicInteger(),
    ) {
        fun addAndGet(tokens: com.simiacryptus.jopenai.ApiModel.Usage) {
            inputTokens.addAndGet(tokens.prompt_tokens)
            outputTokens.addAndGet(tokens.completion_tokens)
        }

        fun toUsage() = com.simiacryptus.jopenai.ApiModel.Usage(
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