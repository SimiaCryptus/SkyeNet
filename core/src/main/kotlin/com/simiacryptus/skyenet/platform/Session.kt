package com.simiacryptus.skyenet.platform

import com.simiacryptus.skyenet.platform.DataStorage.Companion.validateSessionId

data class Session(
    internal val sessionId: String
) {
    init {
        validateSessionId(this)
    }

    override fun toString() = sessionId
}