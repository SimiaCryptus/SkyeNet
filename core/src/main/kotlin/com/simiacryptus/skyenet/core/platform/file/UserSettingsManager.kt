package com.simiacryptus.skyenet.core.platform.file

import com.simiacryptus.skyenet.core.platform.model.ApplicationServicesConfig.dataStorageRoot
import com.simiacryptus.skyenet.core.platform.model.User
import com.simiacryptus.skyenet.core.platform.model.UserSettingsInterface
import com.simiacryptus.skyenet.core.platform.model.UserSettingsInterface.UserSettings
import com.simiacryptus.util.JsonUtil
import java.io.File

open class UserSettingsManager : UserSettingsInterface {

    private val userSettings = HashMap<User, UserSettings>()
    private val userConfigDirectory by lazy { dataStorageRoot.resolve("users").apply { mkdirs() } }

    override fun getUserSettings(user: User): UserSettings {
        return userSettings.getOrPut(user) {
            val file = File(userConfigDirectory, "$user.json")
            if (file.exists()) {
                try {
                    log.info("Loading user settings for $user from $file")
                    return@getOrPut JsonUtil.fromJson(file.readText(), UserSettings::class.java)
                } catch (e: Throwable) {
                    log.warn("Error loading user settings for $user from $file", e)
                }
            }
            log.info("Creating new user settings for $user at $file", RuntimeException())
            return@getOrPut UserSettings()
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