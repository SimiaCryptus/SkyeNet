package com.simiacryptus.skyenet.servers

import com.simiacryptus.skyenet.sessions.*
import org.slf4j.LoggerFactory

open class ReadOnlyApp(
    applicationName: String,
    temperature: Double = 0.3,
    oauthConfig: String? = null,
) : ApplicationBase(
    applicationName = applicationName,
    oauthConfig = oauthConfig,
    temperature = temperature,
    resourceBase = "readOnly",
) {

    companion object {
        val log = LoggerFactory.getLogger(ReadOnlyApp::class.java)
    }

    override fun newSession(sessionId: String): SessionInterface {
        throw UnsupportedOperationException()
    }


}