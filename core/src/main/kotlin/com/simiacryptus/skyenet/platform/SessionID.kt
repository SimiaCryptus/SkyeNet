package com.simiacryptus.skyenet.platform

import com.simiacryptus.skyenet.platform.DataStorage.Companion.validateSessionId

data class SessionID(
    val sessionId: String
) {
    init {
        validateSessionId(this)
    }
}