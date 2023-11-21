package com.simiacryptus.skyenet.servlet

import com.simiacryptus.skyenet.ApplicationBase
import com.simiacryptus.skyenet.ApplicationBase.Companion.getCookie
import com.simiacryptus.skyenet.ApplicationDirectory
import com.simiacryptus.skyenet.platform.*
import jakarta.servlet.http.HttpServlet
import jakarta.servlet.http.HttpServletRequest
import jakarta.servlet.http.HttpServletResponse
import org.intellij.lang.annotations.Language
import java.nio.file.NoSuchFileException

open class WelcomeServlet(private val parent : ApplicationDirectory) : HttpServlet() {
    override fun doGet(req: HttpServletRequest?, resp: HttpServletResponse?) {
        val user = ApplicationServices.authenticationManager.getUser(req!!.getCookie())
        val requestURI = req.requestURI ?: "/"
        resp?.contentType = when (requestURI) {
            "/" -> "text/html"
            else -> ApplicationBase.getMimeType(requestURI)
        }
        when {
            requestURI == "/" -> resp?.writer?.write(homepage(user).trimIndent())
            requestURI == "/index.html" -> resp?.writer?.write(homepage(user).trimIndent())
            requestURI.startsWith("/userInfo") -> {
                parent.userInfoServlet.doGet(req, resp!!)
            }

            requestURI.startsWith("/userSettings") -> parent.userSettingsServlet.doGet(req, resp!!)
            requestURI.startsWith("/usage") -> parent.usageServlet.doGet(req, resp!!)
            else -> try {
                val inputStream = parent.welcomeResources.addPath(requestURI)?.inputStream
                inputStream?.copyTo(resp?.outputStream!!)
            } catch (e: NoSuchFileException) {
                resp?.sendError(404)
            }
        }
    }

    override fun doPost(req: HttpServletRequest?, resp: HttpServletResponse?) {
        val requestURI = req?.requestURI ?: "/"
        when {
            requestURI.startsWith("/userSettings") -> parent.userSettingsServlet.doPost(req!!, resp!!)
            else -> resp?.sendError(404)
        }
    }

    protected open fun homepage(user: User?): String {
        @Language("HTML")
        val html = """<!DOCTYPE html>
<html lang="en">
<head>
    <meta charset="UTF-8">
    <title>SimiaCryptus Skyenet Apps</title>
    <link rel="icon" type="image/svg+xml" href="favicon.svg"/>
    <link href="chat.css" rel="stylesheet"/>
    <script src="main.js"></script>
</head>
<body>

<div id="modal" class="modal">
    <div class="modal-content">
        <span class="close">&times;</span>
        <div id="modal-content"></div>
    </div>
</div>

<div id="toolbar">
</div>

<div id="namebar">
    <a href="/googleLogin" id="username">Login</a>
</div>

<table id="applist">
    ${
            parent.childWebApps.joinToString("\n") { app ->
                val canRun = ApplicationServices.authorizationManager.isAuthorized(
                    applicationClass = app.server.javaClass,
                    user = user,
                    operationType = AuthorizationManager.OperationType.Write
                )
                val canRead = ApplicationServices.authorizationManager.isAuthorized(
                    applicationClass = app.server.javaClass,
                    user = user,
                    operationType = AuthorizationManager.OperationType.Read
                )
                if (!canRead) return@joinToString ""
                val newGlobalSessionLink =
                    if (canRun) """<a class="new-session-link" href="${app.path}/#${DataStorage.newGlobalID().sessionId}">New Shared Session</a>""" else ""
                val newUserSessionLink =
                    if (canRun) """<a class="new-session-link" href="${app.path}/#${DataStorage.newUserID().sessionId}">New Private Session</a>""" else ""
                """
                        <a
                        <tr>
                            <td>
                                ${app.server.applicationName}
                            </td>
                            <td>
                            
                                <a  href="javascript:void(0);" onclick="showModal('${app.path}/sessions')">List Sessions</a>
                            </td>
                            <td>
                                $newGlobalSessionLink
                            </td>
                            <td>
                                $newUserSessionLink
                            </td>
                        </tr>
                    """.trimIndent()
            }
        }
</table>

</body>
</html>
        """
        return html
    }
}