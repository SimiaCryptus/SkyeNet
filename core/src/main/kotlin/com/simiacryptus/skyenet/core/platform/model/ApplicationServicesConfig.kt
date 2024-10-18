package com.simiacryptus.skyenet.core.platform.model

import java.io.File

object ApplicationServicesConfig {

    var isLocked: Boolean = false
        set(value) {
            require(!isLocked) { "ApplicationServices is locked" }
            field = value
        }
    var dataStorageRoot: File = File(System.getProperty("user.home"), ".skyenet")
        set(value) {
            require(!isLocked) { "ApplicationServices is locked" }
            field = value
        }
}