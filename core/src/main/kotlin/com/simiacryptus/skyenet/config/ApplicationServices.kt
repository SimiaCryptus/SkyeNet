package com.simiacryptus.skyenet.config

import java.io.File

object ApplicationServices {
    var isLocked: Boolean = false
        set(value) {
            require(!isLocked) { "ApplicationServices is locked" }
            field = value
        }
    var usageManager: UsageManager = UsageManager()
        set(value) {
            require(!isLocked) { "ApplicationServices is locked" }
            field = value
        }
    var authorizationManager: AuthorizationManager = AuthorizationManager()
        set(value) {
            require(!isLocked) { "ApplicationServices is locked" }
            field = value
        }
    var userSettingsManager: UserSettingsManager = UserSettingsManager()
        set(value) {
            require(!isLocked) { "ApplicationServices is locked" }
            field = value
        }
    var authenticationManager: AuthenticationManager = AuthenticationManager()
        set(value) {
            require(!isLocked) { "ApplicationServices is locked" }
            field = value
        }
    var dataStorageFactory: (File) -> DataStorage = { DataStorage(it) }
        set(value) {
            require(!isLocked) { "ApplicationServices is locked" }
            field = value
        }

}