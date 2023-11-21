package com.simiacryptus.skyenet.platform

import com.simiacryptus.skyenet.platform.DataStorage.Companion.validateSessionId

data class Session(
    val sessionId: String
) {
    init {
        validateSessionId(this)
    }
}