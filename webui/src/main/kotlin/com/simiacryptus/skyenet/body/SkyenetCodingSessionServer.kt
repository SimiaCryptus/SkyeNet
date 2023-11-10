package com.simiacryptus.skyenet.body

import com.simiacryptus.openai.OpenAIClient
import com.simiacryptus.skyenet.Brain
import com.simiacryptus.skyenet.Heart
import com.simiacryptus.util.describe.AbbrevWhitelistYamlDescriber
import com.simiacryptus.util.describe.TypeDescriber
import jakarta.servlet.http.HttpServlet
import jakarta.servlet.http.HttpServletRequest
import jakarta.servlet.http.HttpServletResponse
import org.eclipse.jetty.servlet.ServletHolder
import org.eclipse.jetty.webapp.WebAppContext

abstract class SkyenetCodingSessionServer(
    applicationName: String,
    val typeDescriber: TypeDescriber = AbbrevWhitelistYamlDescriber("com.simiacryptus", "com.github.simiacryptus"),
    oauthConfig: String? = null,
    val autoRun: Boolean = false,
    resourceBase: String = "simpleSession",
    val maxRetries: Int = 5,
    var maxHistoryCharacters: Int = 4000,
    temperature: Double = 0.1,
    val model: OpenAIClient.Model = OpenAIClient.Models.GPT4,
    var useHistory: Boolean = true,
    private val shortExceptions: Boolean = false,
    val apiKey: String
) : SkyenetSessionServerBase(
    applicationName = applicationName,
    oauthConfig = oauthConfig,
    resourceBase = resourceBase,
    temperature = temperature,
) {

    override val api: OpenAIClient = OpenAIClient(apiKey)

    abstract fun hands(): java.util.Map<String, Object>
    abstract fun heart(hands: java.util.Map<String, Object>): Heart

    override fun configure(context: WebAppContext, prefix: String, baseUrl: String) {
        super.configure(context, prefix, baseUrl)
        context.addServlet(descriptorServlet, prefix + "yamlDescriptor")
    }

    protected open val descriptorServlet = ServletHolder(
        "yamlDescriptor",
        object : HttpServlet() {
            override fun doGet(req: HttpServletRequest, resp: HttpServletResponse) {
                resp.contentType = "text/plain"
                resp.status = HttpServletResponse.SC_OK
                val apiDescription = Brain.apiDescription(hands(), typeDescriber)
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

