package com.simiacryptus.skyenet.core.platform.test

import com.simiacryptus.jopenai.ApiModel
import com.simiacryptus.jopenai.models.ChatModels
import com.simiacryptus.skyenet.core.platform.StorageInterface
import com.simiacryptus.skyenet.core.platform.UsageInterface
import com.simiacryptus.skyenet.core.platform.User
import org.junit.jupiter.api.Assertions
import org.junit.jupiter.api.Test
import kotlin.random.Random

abstract class UsageTest(private val impl: UsageInterface) {
  private val testUser = User(
    email = "test@example.com",
    name = "Test User",
    id = Random.nextInt().toString()
  )

  @Test
  fun `incrementUsage should increment usage for session`() {
    val model = ChatModels.GPT35Turbo
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
}