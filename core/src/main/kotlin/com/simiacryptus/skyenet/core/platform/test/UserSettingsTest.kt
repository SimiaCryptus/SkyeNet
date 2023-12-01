package com.simiacryptus.skyenet.core.platform.test

import com.simiacryptus.skyenet.core.platform.User
import com.simiacryptus.skyenet.core.platform.UserSettingsInterface
import org.junit.jupiter.api.Assertions
import org.junit.jupiter.api.Test

open class UserSettingsTest(val userSettings: UserSettingsInterface) {
    private val testUser = User(email = "test@example.com")


    @Test
    fun `updateUserSettings should store custom settings for user`() {
        val newSettings = UserSettingsInterface.UserSettings(apiKey = "12345")
        userSettings.updateUserSettings(testUser, newSettings)
        val settings = userSettings.getUserSettings(testUser)
      Assertions.assertEquals("12345", settings.apiKey)
    }

    @Test
    fun `getUserSettings should return updated settings after updateUserSettings is called`() {
        val initialSettings = userSettings.getUserSettings(testUser)
      Assertions.assertEquals("", initialSettings.apiKey)

        val updatedSettings = UserSettingsInterface.UserSettings(apiKey = "67890")
        userSettings.updateUserSettings(testUser, updatedSettings)

        val settingsAfterUpdate = userSettings.getUserSettings(testUser)
      Assertions.assertEquals("67890", settingsAfterUpdate.apiKey)
    }
}