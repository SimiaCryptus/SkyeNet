package com.simiacryptus.skyenet.core.platform

import com.simiacryptus.skyenet.core.platform.DataStorage.Companion.validateSessionId

data class Session(
    internal val sessionId: String
) {
    init {
        validateSessionId(this)
    }

    override fun toString() = sessionId
}