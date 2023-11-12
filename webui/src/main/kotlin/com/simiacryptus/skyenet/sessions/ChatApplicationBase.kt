package com.simiacryptus.skyenet.sessions

abstract class ChatApplicationBase(
    applicationName: String,
    oauthConfig: String? = null,
    temperature: Double = 0.1,
    resourceBase: String = "simpleSession",
) : ApplicationBase(
    applicationName = applicationName,
    oauthConfig = oauthConfig,
    temperature = temperature,
    resourceBase = resourceBase,
) {

    override fun newSession(sessionId: String): SessionInterface {
        val handler = MutableSessionHandler(null)
        handler.setDelegate(AsyncApplicationWrapper(this,sessionId))
        return handler
    }

    abstract fun processMessage(
        sessionId: String,
        userMessage: String,
        session: PersistentSessionBase,
        sessionDiv: SessionDiv,
        socket: MessageWebSocket
    )

    companion object {
        val log = org.slf4j.LoggerFactory.getLogger(ChatApplicationBase::class.java)
    }

}

