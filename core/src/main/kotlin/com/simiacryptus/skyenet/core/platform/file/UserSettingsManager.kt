package com.simiacryptus.skyenet.core.platform.file

import com.simiacryptus.jopenai.util.JsonUtil
import com.simiacryptus.skyenet.core.platform.User
import com.simiacryptus.skyenet.core.platform.UserSettingsInterface
import com.simiacryptus.skyenet.core.platform.UserSettingsInterface.UserSettings
import java.io.File

open class UserSettingsManager : UserSettingsInterface {

    private val userSettings = HashMap<User, UserSettings>()
    private val userConfigDirectory = File(".skyenet/users")

    override fun getUserSettings(user: User): UserSettings {
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

    override fun updateUserSettings(user: User, settings: UserSettings) {
        userSettings[user] = settings
        val file = File(userConfigDirectory, "$user.json")
        file.parentFile.mkdirs()
        file.writeText(JsonUtil.toJson(settings))
    }

    companion object {
        private val log = org.slf4j.LoggerFactory.getLogger(UserSettingsManager::class.java)
    }

}