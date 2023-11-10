package com.simiacryptus.skyenet.body

import com.simiacryptus.openai.OpenAIClient
import com.simiacryptus.util.describe.TypeDescriber
import com.simiacryptus.util.describe.YamlDescriber
import jakarta.servlet.http.HttpServlet
import jakarta.servlet.http.HttpServletRequest
import jakarta.servlet.http.HttpServletResponse
import org.eclipse.jetty.servlet.ServletHolder
import org.eclipse.jetty.webapp.WebAppContext

@Suppress("unused")
open class SkyenetInterviewer<T : Any>(
    applicationName: String,
    val dataClass: Class<T>,
    val visiblePrompt: String,
    val describer: TypeDescriber = YamlDescriber(),
    oauthConfig: String? = null,
    val continueSession: (String, T) -> SessionInterface,
    val validate: (T) -> List<String>,
    private val apiKey: String
) : SkyenetSessionServerBase(
    applicationName = applicationName,
    oauthConfig = oauthConfig,
) {

    override val api: OpenAIClient = OpenAIClient(apiKey)

    override fun configure(context: WebAppContext, prefix: String, baseUrl: String) {
        super.configure(context, prefix, baseUrl)
        context.addServlet(
            ServletHolder(
                "yamlDescriptor",
                object : HttpServlet() {
                    override fun doGet(req: HttpServletRequest, resp: HttpServletResponse) {
                        resp.contentType = "text/plain"
                        resp.status = HttpServletResponse.SC_OK
                        resp.writer.write(describer.describe(dataClass))
                    }
                }), prefix + "yamlDescriptor"
        )
    }


    override fun newSession(sessionId: String): SessionInterface {
        val handler = MutableSessionHandler(null)
        val interviewSession: InterviewSession<T> = object : InterviewSession<T>(
            parent = this@SkyenetInterviewer,
            sessionId = sessionId, // This may need to be a new session id
            dataClass = dataClass,
            describer = this@SkyenetInterviewer.describer,
            visiblePrompt = this@SkyenetInterviewer.visiblePrompt,
            isFinished = this@SkyenetInterviewer.validate
        ) {
            override fun onFinished(data: T) {
                handler.setDelegate(continueSession(sessionId, data))
            }

            override fun onUpdate(data: T) {
            }
        }
        handler.setDelegate(interviewSession)
        return handler
    }

}