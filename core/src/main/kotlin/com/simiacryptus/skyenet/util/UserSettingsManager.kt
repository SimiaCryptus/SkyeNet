package com.simiacryptus.skyenet.util

import com.simiacryptus.util.JsonUtil
import java.io.File

object UserSettingsManager {
    data class UserSettings(
        val apiKey: String = "",
    )

    private val log = org.slf4j.LoggerFactory.getLogger(UserSettingsManager::class.java)
    private val userSettings = HashMap<String, UserSettings>()
    private val userConfigDirectory = File(".skyenet/users")

    fun getUserSettings(user: String): UserSettings {
        return userSettings.getOrPut(user) {
            val file = File(userConfigDirectory, "$user.json")
            if (file.exists()) {
                log.info("Loading user settings for $user from $file")
                JsonUtil.fromJson(file.readText(), UserSettings::class.java)
            } else {
                log.info("Creating new user settings for $user at $file")
                UserSettings()
            }
        }
    }

    fun updateUserSettings(user: String, settings: UserSettings) {
        userSettings[user] = settings
        val file = File(userConfigDirectory, "$user.json")
        file.parentFile.mkdirs()
        file.writeText(JsonUtil.toJson(settings))
    }

}