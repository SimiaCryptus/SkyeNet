package com.simiacryptus.skyenet.platform

import com.simiacryptus.util.JsonUtil
import java.io.File

open class UserSettingsManager {
    data class UserSettings(
        val apiKey: String = "",
    )

    private val userSettings = HashMap<UserInfo, UserSettings>()
    private val userConfigDirectory = File(".skyenet/users")

    open fun getUserSettings(user: UserInfo): UserSettings {
        return userSettings.getOrPut(user) {
            val file = File(userConfigDirectory, "$user.json")
            if (file.exists()) {
                Companion.log.info("Loading user settings for $user from $file")
                JsonUtil.fromJson(file.readText(), UserSettings::class.java)
            } else {
                Companion.log.info("Creating new user settings for $user at $file")
                UserSettings()
            }
        }
    }

    open fun updateUserSettings(user: UserInfo, settings: UserSettings) {
        userSettings[user] = settings
        val file = File(userConfigDirectory, "$user.json")
        file.parentFile.mkdirs()
        file.writeText(JsonUtil.toJson(settings))
    }

    companion object {
        private val log = org.slf4j.LoggerFactory.getLogger(UserSettingsManager::class.java)
    }

}