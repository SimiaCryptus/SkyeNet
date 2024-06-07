package com.simiacryptus.skyenet.core.platform

import com.google.common.util.concurrent.AtomicDouble
import com.simiacryptus.jopenai.ApiModel
import com.simiacryptus.jopenai.models.ChatModels
import com.simiacryptus.jopenai.models.OpenAIModel
import org.slf4j.Logger
import org.slf4j.LoggerFactory
import java.io.File
import java.sql.Connection
import java.sql.DriverManager
import java.sql.ResultSet
import java.sql.Timestamp
import java.util.concurrent.atomic.AtomicInteger

class HSQLUsageManager(private val dbFile: File) : UsageInterface {

    private val connection: Connection by lazy {
        logger.info("Initializing HSQLUsageManager with database file: ${dbFile.absolutePath}")
        Class.forName("org.hsqldb.jdbc.JDBCDriver")
        val connection = DriverManager.getConnection("jdbc:hsqldb:file:${dbFile.absolutePath};shutdown=true", "SA", "")
        logger.debug("Database connection established: $connection")
        createSchema(connection)
        connection
    }
    private val logger: Logger = LoggerFactory.getLogger(HSQLUsageManager::class.java)

    private fun createSchema(connection: Connection) {
        logger.info("Creating database schema if not exists")
        connection.createStatement().executeUpdate(
            """
            CREATE TABLE IF NOT EXISTS usage (
                session_id VARCHAR(255),
                api_key VARCHAR(255),
                model VARCHAR(255),
                prompt_tokens INT,
                completion_tokens INT,
                cost DOUBLE,
               datetime TIMESTAMP,
               PRIMARY KEY (session_id, api_key, model, prompt_tokens, completion_tokens, cost, datetime)
                )
             """
        )
    }


    private fun updateSchema() {
        logger.info("Updating database schema if needed")
        // Add schema update logic here if needed
    }

    private fun deleteSchema() {
        logger.info("Deleting database schema if exists")
        connection.createStatement().executeUpdate("DROP TABLE IF EXISTS usage")
        logger.debug("Schema deleted")
    }

    override fun incrementUsage(session: Session, apiKey: String?, model: OpenAIModel, tokens: ApiModel.Usage) {
        logger.info("Incrementing usage for session: ${session.sessionId}, apiKey: $apiKey, model: ${model.modelName}")
        val usageKey = UsageInterface.UsageKey(session, apiKey, model)
        val usageValues = getUsageValues(usageKey)
        usageValues.addAndGet(tokens)
        saveUsageValues(usageKey, usageValues)
        logger.debug("Usage incremented for session: ${session.sessionId}, apiKey: $apiKey, model: ${model.modelName}")
    }

    override fun getUserUsageSummary(apiKey: String): Map<OpenAIModel, ApiModel.Usage> {
        logger.debug("Executing SQL query to get user usage summary for apiKey: $apiKey")
        val statement = connection.prepareStatement(
            """
            SELECT model, SUM(prompt_tokens), SUM(completion_tokens), SUM(cost)
            FROM usage
            WHERE api_key = ?
            GROUP BY model
            """
        )
        statement.setString(1, apiKey)
        val resultSet = statement.executeQuery()
        return generateUsageSummary(resultSet)
    }

    override fun getSessionUsageSummary(session: Session): Map<OpenAIModel, ApiModel.Usage> {
        logger.info("Getting session usage summary for session: ${session.sessionId}")
        val statement = connection.prepareStatement(
            """
            SELECT model, SUM(prompt_tokens), SUM(completion_tokens), SUM(cost)
            FROM usage
            WHERE session_id = ?
            GROUP BY model
            """
        )
        statement.setString(1, session.sessionId)
        val resultSet = statement.executeQuery()
        return generateUsageSummary(resultSet)
    }

    override fun clear() {
        logger.debug("Executing SQL statement to clear all usage data")
        connection.createStatement().executeUpdate("DELETE FROM usage")
    }

    private fun getUsageValues(usageKey: UsageInterface.UsageKey): UsageInterface.UsageValues {
        logger.info("Getting usage values for session: ${usageKey.session.sessionId}, apiKey: ${usageKey.apiKey}, model: ${usageKey.model.modelName}")
        //logger.debug("Executing SQL query to get usage values for session: ${usageKey.session.sessionId}, apiKey: ${usageKey.apiKey}, model: ${usageKey.model.modelName}")
        val statement = connection.prepareStatement(
            """
            SELECT COALESCE(SUM(prompt_tokens), 0), COALESCE(SUM(completion_tokens), 0), COALESCE(SUM(cost), 0)
            FROM usage
            WHERE session_id = ? AND api_key = ? AND model = ?
            """
        )
        statement.setString(1, usageKey.session.sessionId)
        statement.setString(2, usageKey.apiKey ?: "")
        statement.setString(3, usageKey.model.toString())
        val resultSet = statement.executeQuery()
        resultSet.next()
        return UsageInterface.UsageValues(
            AtomicInteger(resultSet.getInt(1)),
            AtomicInteger(resultSet.getInt(2)),
            AtomicDouble(resultSet.getDouble(3))
        )
    }

    private fun saveUsageValues(usageKey: UsageInterface.UsageKey, usageValues: UsageInterface.UsageValues) {
        logger.info("Saving usage values for session: ${usageKey.session.sessionId}, apiKey: ${usageKey.apiKey}, model: ${usageKey.model.modelName}")
        logger.debug("Executing SQL statement to save usage values for session: ${usageKey.session.sessionId}, apiKey: ${usageKey.apiKey}, model: ${usageKey.model.modelName}")
        val statement = connection.prepareStatement(
            """
            INSERT INTO usage (session_id, api_key, model, prompt_tokens, completion_tokens, cost, datetime)
            VALUES (?, ?, ?, ?, ?, ?, ?)
            """
        )
        statement.setString(1, usageKey.session.sessionId)
        statement.setString(2, usageKey.apiKey ?: "")
        statement.setString(3, usageKey.model.modelName)
        statement.setInt(4, usageValues.inputTokens.get())
        statement.setInt(5, usageValues.outputTokens.get())
        statement.setDouble(6, usageValues.cost.get())
       statement.setTimestamp(7, Timestamp(System.currentTimeMillis()))
        logger.debug("Executing statement: $statement")
        logger.debug("With parameters: ${usageKey.session.sessionId}, ${usageKey.apiKey}, ${usageKey.model.modelName}, ${usageValues.inputTokens.get()}, ${usageValues.outputTokens.get()}, ${usageValues.cost.get()}")
        statement.executeUpdate()
    }

    private fun generateUsageSummary(resultSet: ResultSet): Map<OpenAIModel, ApiModel.Usage> {
        logger.debug("Generating usage summary from result set")
        val summary = mutableMapOf<OpenAIModel, ApiModel.Usage>()
        while (resultSet.next()) {
            val string = resultSet.getString(1)
            val model = openAIModel(string) ?: continue
            val usage = ApiModel.Usage(
                prompt_tokens = resultSet.getInt(2),
                completion_tokens = resultSet.getInt(3),
                cost = resultSet.getDouble(4)
            )
            summary[model] = usage
        }
        return summary
    }

    private fun openAIModel(string: String): OpenAIModel? {
        logger.debug("Retrieving OpenAI model for string: $string")
        val model = ChatModels.values().filter {
            it.key == string || it.value.modelName == string || it.value.name == string
        }.toList().firstOrNull()?.second ?: return null
        logger.debug("OpenAI model retrieved: $model")
        return model
    }

    companion object {
        private val log = org.slf4j.LoggerFactory.getLogger(HSQLUsageManager::class.java)
    }
}