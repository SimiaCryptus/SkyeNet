package com.simiacryptus.skyenet.body

import com.simiacryptus.skyenet.Brain
import com.simiacryptus.skyenet.Heart
import com.simiacryptus.util.TypeDescriber
import com.simiacryptus.util.YamlDescriber
import jakarta.servlet.http.HttpServlet
import jakarta.servlet.http.HttpServletRequest
import jakarta.servlet.http.HttpServletResponse
import org.apache.commons.io.FileUtils
import org.eclipse.jetty.servlet.ServletHolder
import org.eclipse.jetty.webapp.WebAppContext
import java.io.File

abstract class SkyenetCodingSessionServer(
    applicationName: String,
    val yamlDescriber: TypeDescriber = YamlDescriber(),
    oauthConfig: String? = null,
    val autoRun: Boolean = false,
    resourceBase: String = "simpleSession",
    val maxRetries: Int = 5,
    var maxHistoryCharacters: Int = 4000,
    baseURL: String = "http://localhost:8080",
    temperature: Double = 0.1,
    val model: String = "gpt-3.5-turbo",
    var useHistory: Boolean = true,
    val shortExceptions: Boolean = true,
    override val apiKey: String
) : SkyenetSessionServerBase(
    applicationName = applicationName,
    oauthConfig = oauthConfig,
    resourceBase = resourceBase,
    baseURL = baseURL,
    temperature = temperature,
) {

    abstract fun hands(): java.util.Map<String, Object>
    abstract fun heart(hands: java.util.Map<String, Object>): Heart

    override fun configure(context: WebAppContext) {
        super.configure(context)

        if (null != oauthConfig) (AuthenticatedWebsite("$baseURL/oauth2callback", this@SkyenetCodingSessionServer.applicationName) {
            FileUtils.openInputStream(File(oauthConfig))
        }).configure(context)

        context.addServlet(descriptorServlet, "/yamlDescriptor")
    }

    protected open val descriptorServlet = ServletHolder(
        "yamlDescriptor",
        object : HttpServlet() {
            override fun doGet(req: HttpServletRequest, resp: HttpServletResponse) {
                resp.contentType = "text/plain"
                resp.status = HttpServletResponse.SC_OK
                val apiDescription = Brain.apiDescription(hands(), yamlDescriber)
                resp.writer.write(apiDescription)
            }
        })

    override fun newSession(sessionId: String) = SkyenetCodingSession(sessionId, this)

    companion object {
        val logger = org.slf4j.LoggerFactory.getLogger(SkyenetCodingSessionServer::class.java)
    }

    open fun toString(e: Throwable) : String {
        return if(shortExceptions) {
            e.message ?: e.toString()
        } else
        {
            val sw = java.io.StringWriter()
            e.printStackTrace(java.io.PrintWriter(sw))
            sw.toString()
        }
    }

}

