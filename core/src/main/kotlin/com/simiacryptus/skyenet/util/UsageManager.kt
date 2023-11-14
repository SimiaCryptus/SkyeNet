package com.simiacryptus.skyenet.util

import com.simiacryptus.openai.OpenAIClient
import com.simiacryptus.util.JsonUtil
import java.io.File
import java.io.FileWriter
import java.util.concurrent.Executors
import java.util.concurrent.TimeUnit
import java.util.concurrent.atomic.AtomicInteger

object UsageManager {
    val log = org.slf4j.LoggerFactory.getLogger(UsageManager::class.java)

    private val scheduler = Executors.newSingleThreadScheduledExecutor()
    private val txLogFile = File(".skyenet/usage/log.csv")
    private val txLogFileWriter by lazy { FileWriter(txLogFile, true) }
    private val usagePerSession = HashMap<String, UsageCounters>()
    private val sessionsByUser = HashMap<String, ArrayList<String>>()
    private val usersBySession = HashMap<String, ArrayList<String>>()

    init {
        txLogFile.parentFile.mkdirs()
        loadFromLog(txLogFile)
        scheduler.scheduleAtFixedRate({ saveCounters() }, 1, 1, TimeUnit.HOURS)
    }

    @Suppress("MemberVisibilityCanBePrivate")
    fun loadFromLog(file: File) {
        if (file.exists()) {
            file.readLines().forEach { line ->
                val (sessionId, user, model, tokens) = line.split(",")
                val modelEnum = OpenAIClient.Models.values().find { model == it.modelName } ?: throw RuntimeException("Unknown model $model")
                incrementUsage(sessionId, user, modelEnum, tokens.toInt())
            }
        }
    }
    @Suppress("MemberVisibilityCanBePrivate")
    fun writeCompactLog(file: File) {
        val writer = FileWriter(file)
        usagePerSession.forEach { (sessionId, usage) ->
            val user = usersBySession[sessionId]?.firstOrNull()
            usage.tokensPerModel.forEach { (model, counter) ->
                writer.write("$sessionId,$user,${model.modelName},${counter.get()}\n")
            }
        }
        writer.flush()
        writer.close()
    }

    private fun saveCounters() {
        txLogFile.renameTo(File(txLogFile.absolutePath + "." + System.currentTimeMillis()))
        writeCompactLog(txLogFile)
        File(".skyenet/usage/counters.json").writeText(JsonUtil.toJson(usagePerSession))
    }

    fun incrementUsage(sessionId: String, user: String?, model: OpenAIClient.Model, tokens: Int) {
        val usage = usagePerSession.getOrPut(sessionId) {
            UsageCounters()
        }
        val tokensPerModel = usage.tokensPerModel.getOrPut(model) {
            AtomicInteger()
        }
        tokensPerModel.addAndGet(tokens)
        if (user != null) {
            val sessions = sessionsByUser.getOrPut(user) {
                ArrayList()
            }
            sessions.add(sessionId)
        }
        synchronized(txLogFileWriter) {
            txLogFileWriter.write("$sessionId,$user,${model.modelName},$tokens\n")
            txLogFileWriter.flush()
        }
    }

    fun getUserUsageSummary(user: String): Map<OpenAIClient.Model, Int> {
        val sessions = sessionsByUser[user]
        return sessions?.flatMap { sessionId ->
            val usage = usagePerSession[sessionId]
            usage?.tokensPerModel?.entries?.map { (model, counter) ->
                model to counter.get()
            } ?: emptyList()
        }?.groupBy { it.first }?.mapValues { it.value.map { it.second }.sum() } ?: emptyMap()
    }

    fun getSessionUsageSummary(sessionId: String): Map<OpenAIClient.Model, Int> {
        val usage = usagePerSession[sessionId]
        return usage?.tokensPerModel?.entries?.map { (model, counter) ->
            model to counter.get()
        }?.groupBy { it.first }?.mapValues { it.value.map { it.second }.sum() } ?: emptyMap()
    }

    data class UsageCounters(
        val tokensPerModel: HashMap<OpenAIClient.Model, AtomicInteger> = HashMap(),
    )
}