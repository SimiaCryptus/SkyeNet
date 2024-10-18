package com.simiacryptus.skyenet.core.platform.test

import com.simiacryptus.jopenai.models.APIProvider
import com.simiacryptus.skyenet.core.platform.model.User
import com.simiacryptus.skyenet.core.platform.model.UserSettingsInterface
import org.junit.jupiter.api.Assertions
import org.junit.jupiter.api.Test
import java.util.*

abstract class UserSettingsTest(private val userSettings: UserSettingsInterface) {


    @Test
    fun `updateUserSettings should store custom settings for user`() {
        val id = UUID.randomUUID().toString()
        val testUser = User(
            email = "$id@example.com",
            name = "Test User",
            id = id
        )

        val newSettings = UserSettingsInterface.UserSettings(apiKeys = mapOf(APIProvider.OpenAI to "12345"))
        userSettings.updateUserSettings(testUser, newSettings)
        val settings = userSettings.getUserSettings(testUser)
        Assertions.assertEquals("12345", settings.apiKeys[APIProvider.OpenAI])
    }

    @Test
    fun `getUserSettings should return updated settings after updateUserSettings is called`() {
        val id = UUID.randomUUID().toString()
        val testUser = User(
            email = "$id@example.com",
            name = "Test User",
            id = id
        )
        val initialSettings = userSettings.getUserSettings(testUser)
        Assertions.assertEquals("", initialSettings.apiKeys[APIProvider.OpenAI])

        val updatedSettings = UserSettingsInterface.UserSettings(apiKeys = mapOf(APIProvider.OpenAI to "67890"))
        userSettings.updateUserSettings(testUser, updatedSettings)

        val settingsAfterUpdate = userSettings.getUserSettings(testUser)
        Assertions.assertEquals("67890", settingsAfterUpdate.apiKeys[APIProvider.OpenAI])
    }
}