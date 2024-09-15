package com.simiacryptus.skyenet.core.platform.test

import com.simiacryptus.jopenai.ApiModel
import com.simiacryptus.jopenai.models.ChatModels
import com.simiacryptus.jopenai.models.OpenAIModels
import com.simiacryptus.skyenet.core.platform.StorageInterface
import com.simiacryptus.skyenet.core.platform.UsageInterface
import com.simiacryptus.skyenet.core.platform.User
import org.junit.jupiter.api.Assertions
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test
import kotlin.random.Random

abstract class UsageTest(private val impl: UsageInterface) {
    private val testUser = User(
        email = "test@example.com",
        name = "Test User",
        id = Random.nextInt().toString()
    )

    @BeforeEach
    fun setup() {
        impl.clear()
    }

    @Test
    fun `incrementUsage should increment usage for session`() {
        val model = OpenAIModels.GPT4oMini
        val session = StorageInterface.newGlobalID()
        val usage = ApiModel.Usage(
            prompt_tokens = 10,
            completion_tokens = 20,
            cost = 30.0,
        )
        impl.incrementUsage(session, testUser, model, usage)
        val usageSummary = impl.getSessionUsageSummary(session)
        Assertions.assertEquals(usage, usageSummary[model])
        val userUsageSummary = impl.getUserUsageSummary(testUser)
        Assertions.assertEquals(usage, userUsageSummary[model])
    }

    @Test
    fun `getUserUsageSummary should return correct usage summary`() {
        val model = OpenAIModels.GPT4oMini
        val session = StorageInterface.newGlobalID()
        val usage = ApiModel.Usage(
            prompt_tokens = 15,
            completion_tokens = 25,
            cost = 35.0,
        )
        impl.incrementUsage(session, testUser, model, usage)
        val userUsageSummary = impl.getUserUsageSummary(testUser)
        Assertions.assertEquals(usage, userUsageSummary[model])
    }

    @Test
    fun `clear should reset all usage data`() {
        val model = OpenAIModels.GPT4oMini
        val session = StorageInterface.newGlobalID()
        val usage = ApiModel.Usage(
            prompt_tokens = 20,
            completion_tokens = 30,
            cost = 40.0,
        )
        impl.incrementUsage(session, testUser, model, usage)
        impl.clear()
        val usageSummary = impl.getSessionUsageSummary(session)
        Assertions.assertTrue(usageSummary.isEmpty())
        val userUsageSummary = impl.getUserUsageSummary(testUser)
        Assertions.assertTrue(userUsageSummary.isEmpty())
    }

    @Test
    fun `incrementUsage should handle multiple models correctly`() {
        val model1 = OpenAIModels.GPT4oMini
        val model2 = OpenAIModels.GPT4Turbo
        val session = StorageInterface.newGlobalID()
        val usage1 = ApiModel.Usage(
            prompt_tokens = 10,
            completion_tokens = 20,
            cost = 30.0,
        )
        val usage2 = ApiModel.Usage(
            prompt_tokens = 5,
            completion_tokens = 10,
            cost = 15.0,
        )
        impl.incrementUsage(session, testUser, model1, usage1)
        impl.incrementUsage(session, testUser, model2, usage2)
        val usageSummary = impl.getSessionUsageSummary(session)
        Assertions.assertEquals(usage1, usageSummary[model1])
        Assertions.assertEquals(usage2, usageSummary[model2])
        val userUsageSummary = impl.getUserUsageSummary(testUser)
        Assertions.assertEquals(usage1, userUsageSummary[model1])
        Assertions.assertEquals(usage2, userUsageSummary[model2])
    }

    @Test
    fun `incrementUsage should accumulate usage for the same model`() {
        val model = OpenAIModels.GPT4oMini
        val session = StorageInterface.newGlobalID()
        val usage1 = ApiModel.Usage(
            prompt_tokens = 10,
            completion_tokens = 20,
            cost = 30.0,
        )
        val usage2 = ApiModel.Usage(
            prompt_tokens = 5,
            completion_tokens = 10,
            cost = 15.0,
        )
        impl.incrementUsage(session, testUser, model, usage1)
        impl.incrementUsage(session, testUser, model, usage2)
        val usageSummary = impl.getSessionUsageSummary(session)
        val expectedUsage = ApiModel.Usage(
            prompt_tokens = 15,
            completion_tokens = 30,
            cost = 45.0,
        )
        Assertions.assertEquals(expectedUsage, usageSummary[model])
        val userUsageSummary = impl.getUserUsageSummary(testUser)
        Assertions.assertEquals(expectedUsage, userUsageSummary[model])
    }

    @Test
    fun `getSessionUsageSummary should return empty map for unknown session`() {
        val session = StorageInterface.newGlobalID()
        val usageSummary = impl.getSessionUsageSummary(session)
        Assertions.assertTrue(usageSummary.isEmpty())
    }

    @Test
    fun `getUserUsageSummary should return empty map for unknown user`() {
        val unknownUser = User(
            email = "unknown@example.com",
            name = "Unknown User",
            id = Random.nextInt().toString()
        )
        val userUsageSummary = impl.getUserUsageSummary(unknownUser)
        Assertions.assertTrue(userUsageSummary.isEmpty())
    }
}