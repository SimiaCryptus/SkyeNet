package com.simiacryptus.skyenet.body

import com.simiacryptus.openai.OpenAIClient
import org.apache.commons.io.FileUtils
import org.eclipse.jetty.webapp.WebAppContext
import java.io.File

open class SkyenetBasicChat(
    applicationName: String,
    baseURL: String,
    oauthConfig: String? = null
) : SkyenetSessionServerBase(
    applicationName = applicationName,
    baseURL = baseURL,
    oauthConfig = oauthConfig,
) {
    override val api: OpenAIClient
        get() = OpenAIClient()

    override fun configure(context: WebAppContext, prefix: String) {
        super.configure(context, prefix)
        if (null != oauthConfig) AuthenticatedWebsite(
            "$baseURL/oauth2callback",
            this@SkyenetBasicChat.applicationName
        ) {
            FileUtils.openInputStream(File(oauthConfig))
        }.configure(context)
    }


    override fun newSession(sessionId: String): SessionInterface {
        val handler = MutableSessionHandler(null)
        val basicChatSession = BasicChatSession(
            parent = this@SkyenetBasicChat,
            sessionId = sessionId
        )
        handler.setDelegate(basicChatSession)
        return handler
    }

}