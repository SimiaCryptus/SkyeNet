package com.simiacryptus.skyenet.body

import com.simiacryptus.util.TypeDescriber
import com.simiacryptus.util.YamlDescriber
import jakarta.servlet.http.HttpServlet
import jakarta.servlet.http.HttpServletRequest
import jakarta.servlet.http.HttpServletResponse
import org.apache.commons.io.FileUtils
import org.eclipse.jetty.servlet.ServletHolder
import org.eclipse.jetty.webapp.WebAppContext
import java.io.File

open class SkyenetInterviewer<T : Any>(
    applicationName: String,
    baseURL: String,
    val dataClass: Class<T>,
    val visiblePrompt: String,
    val describer: TypeDescriber = YamlDescriber(),
    override val oauthConfig: String? = null,
    val continueSession: (String, T) -> SessionInterface,
    val validate: (T) -> List<String>,
) : SkyenetSessionServerBase(
    applicationName = applicationName,
    baseURL = baseURL,
) {

    override fun configure(context: WebAppContext) {
        super.configure(context)

        if (null != oauthConfig) AuthenticatedWebsite("$baseURL/oauth2callback", this@SkyenetInterviewer.applicationName) {
            FileUtils.openInputStream(File(oauthConfig))
        }.configure(context)

        context.addServlet(
            ServletHolder(
                "yamlDescriptor",
                object : HttpServlet() {
                    override fun doGet(req: HttpServletRequest, resp: HttpServletResponse) {
                        resp.contentType = "text/plain"
                        resp.status = HttpServletResponse.SC_OK
                        resp.writer.write(describer.describe(dataClass))
                    }
                }), "/yamlDescriptor"
        )
    }


    override fun newSession(sessionId: String): SessionInterface {
        val handler = MutableSessionHandler(null)
        val interviewSession: InterviewSession<T> = object : InterviewSession<T>(
            parent = this,
            sessionId = sessionId, // This may need to be a new session id
            dataClass = dataClass,
            describer = this@SkyenetInterviewer.describer,
            visiblePrompt = this@SkyenetInterviewer.visiblePrompt,
            isFinished = this@SkyenetInterviewer.validate
        ) {
            override fun onFinished(data: T) {
                handler.setDelegate(continueSession(sessionId, data));
            }

            override fun onUpdate(data: T) {
            }
        }
        handler.setDelegate(interviewSession)
        return handler
    }

}