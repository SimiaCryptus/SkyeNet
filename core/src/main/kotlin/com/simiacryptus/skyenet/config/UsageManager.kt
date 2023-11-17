package com.simiacryptus.skyenet.config

import com.simiacryptus.openai.Model
import com.simiacryptus.openai.Models
import com.simiacryptus.openai.OpenAIClient
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
    private val usagePerSession = HashMap<String, UsageCounters>()
    private val sessionsByUser = HashMap<String, ArrayList<String>>()
    private val usersBySession = HashMap<String, ArrayList<String>>()

    init {
        txLogFile.parentFile.mkdirs()
        loadFromLog(txLogFile)
        txLogFileWriter = FileWriter(txLogFile, true)
        scheduler.scheduleAtFixedRate({ saveCounters() }, 1, 1, TimeUnit.HOURS)
    }

    @Suppress("MemberVisibilityCanBePrivate")
    open fun loadFromLog(file: File) {
        if (file.exists()) {
            file.readLines().forEach { line ->
                val (sessionId, user, model, tokens) = line.split(",")
                val modelEnum = Models.values().find { model == it.modelName } ?: throw RuntimeException("Unknown model $model")
                incrementUsage(sessionId, user, modelEnum, tokens.toInt())
            }
        }
    }

    @Suppress("MemberVisibilityCanBePrivate")
    open fun writeCompactLog(file: File) {
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

    open fun incrementUsage(sessionId: String, user: String?, model: Model, tokens: Int) {
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
        try {
            val txLogFileWriter = txLogFileWriter
            if(null != txLogFileWriter) {
                synchronized(txLogFile) {
                    txLogFileWriter.write("$sessionId,$user,${model.modelName},$tokens\n")
                    txLogFileWriter.flush()
                }
            }
        } catch (e: Exception) {
            log.warn("Error incrementing usage", e)
        }
    }

    open fun getUserUsageSummary(user: String): Map<Model, Int> {
        val sessions = sessionsByUser[user]
        return sessions?.flatMap { sessionId ->
            val usage = usagePerSession[sessionId]
            usage?.tokensPerModel?.entries?.map { (model, counter) ->
                model to counter.get()
            } ?: emptyList()
        }?.groupBy { it.first }?.mapValues { it.value.map { it.second }.sum() } ?: emptyMap()
    }

    open fun getSessionUsageSummary(sessionId: String): Map<Model, Int> {
        val usage = usagePerSession[sessionId]
        return usage?.tokensPerModel?.entries?.map { (model, counter) ->
            model to counter.get()
        }?.groupBy { it.first }?.mapValues { it.value.map { it.second }.sum() } ?: emptyMap()
    }

    data class UsageCounters(
        val tokensPerModel: HashMap<Model, AtomicInteger> = HashMap(),
    )

    companion object {
        private val log = org.slf4j.LoggerFactory.getLogger(UsageManager::class.java)
    }
}