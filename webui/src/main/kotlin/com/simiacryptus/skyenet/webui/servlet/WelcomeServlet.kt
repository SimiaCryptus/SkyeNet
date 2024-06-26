package com.simiacryptus.skyenet.webui.servlet

import com.simiacryptus.skyenet.core.platform.ApplicationServices
import com.simiacryptus.skyenet.core.platform.ApplicationServices.authorizationManager
import com.simiacryptus.skyenet.core.platform.AuthorizationInterface.OperationType
import com.simiacryptus.skyenet.core.platform.StorageInterface
import com.simiacryptus.skyenet.core.platform.User
import com.simiacryptus.skyenet.webui.application.ApplicationDirectory
import com.simiacryptus.skyenet.webui.application.ApplicationServer
import com.simiacryptus.skyenet.webui.application.ApplicationServer.Companion.getCookie
import com.simiacryptus.skyenet.webui.util.MarkdownUtil.renderMarkdown
import jakarta.servlet.http.HttpServlet
import jakarta.servlet.http.HttpServletRequest
import jakarta.servlet.http.HttpServletResponse
import org.intellij.lang.annotations.Language
import java.nio.file.NoSuchFileException

open class WelcomeServlet(private val parent: ApplicationDirectory) :
    HttpServlet() {
    override fun doGet(req: HttpServletRequest?, resp: HttpServletResponse?) {
        val user = ApplicationServices.authenticationManager.getUser(req!!.getCookie())
        val requestURI = req.requestURI ?: "/"
        resp?.contentType = when (requestURI) {
            "/" -> "text/html"
            else -> ApplicationServer.getMimeType(requestURI)
        }
        when {
            requestURI == "/" -> resp?.writer?.write(homepage(user).trimIndent())
            requestURI == "/index.html" -> resp?.writer?.write(homepage(user).trimIndent())
            requestURI.startsWith("/userInfo") -> {
                parent.userInfoServlet.service(req, resp!!)
            }

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
            requestURI.startsWith("/userSettings") -> parent.userSettingsServlet.service(req!!, resp!!)
            else -> resp?.sendError(404)
        }
    }

    @Language("Markdown")
    protected open val welcomeMarkdown = """""".trimIndent()

    @Language("Markdown")
    protected open val postAppMarkdown = """""".trimIndent()

    @Language("HTML")
    protected open fun homepage(user: User?) = """
    <!DOCTYPE html>
    <html lang="en">
    <head>
        <meta charset="UTF-8">
        <meta name='viewport' content='width=device-width,initial-scale=1'>
        <title>SimiaCryptus Skyenet Apps</title>
        <link rel="icon" type="image/svg+xml" href="favicon.svg"/>
        <link id="theme_style" href="/main.css" rel="stylesheet"/>
        <script src="functions.js"></script>
        <script src="tabs.js"></script>
        <script src="main.js"></script>
        <script src="theme.js"></script>
        <script src="about.js"></script>
        <style>
            .app-thumbnail {
                width: 50px;
                height: 50px;
                object-fit: cover;
                margin-right: 10px;
            }
        </style>
    </head>
    <body>
    
    
    <div id="modal" class="modal">
        <div class="modal-content">
            <span class="close">&times;</span>
            <div id="modal-content"></div>
        </div>
    </div>
    
    <div id="toolbar"> 
        <div class="dropdown">
            <a class="dropbtn">About</a>
            <div class="dropdown-content">
                <a id="privacy">Privacy Policy</a>
                <a id="tos">Terms of Service</a>
            </div>
        </div>
        <div class="dropdown">
            <a class="dropbtn">Themes</a>
            <div class="dropdown-content">
                <a id="theme_normal">Day</a>
                <a id="theme_night">Night</a>
                <a id="theme_forest">Forest</a>
                <a id="theme_pony">Bubblegum</a>
                <a id="theme_alien">Alien</a>
            </div>
        </div>
    </div>
    
    <div id="namebar">
        <div class="dropdown">
            <a href="/login" id="login">Login</a>
            <a id="username" style="visibility: hidden"></a>
            <div class="dropdown-content">
                <a id="user-settings" style="visibility: hidden">Settings</a>
                <a id="user-usage" style="visibility: hidden">Usage</a>
                <a id="logout" style="visibility: hidden">Logout</a>
            </div>
        </div>
    </div>
    
    ${renderMarkdown(welcomeMarkdown, tabs = false)}
    
    <table class="applist" id='application-list'>
        ${parent.childWebApps.joinToString("\n") { app -> appRow(app, user) }}
    </table>
    
    ${renderMarkdown(postAppMarkdown, tabs = false)}
    
    <footer id="footer">
        <a href="https://github.com/SimiaCryptus/SkyeNet" target="_blank">Powered by SkyeNet</a>
    </footer>
    
    </body>
    <script>
        function showImageModal(src) {
            const modal = document.getElementById("modal");
            const modalContent = document.getElementById("modal-content");
            modalContent.innerHTML = '<img src="' + src + '" style="width: 100%;">';
            modal.style.display = "block";
        }

        document.querySelector(".close").onclick = function() {
            document.getElementById("modal").style.display = "none";
        }
    </script>
    </html>
    """.trimIndent()

    protected open fun appRow(
        app: ApplicationDirectory.ChildWebApp,
        user: User?
    ) = when {
        !authorizationManager.isAuthorized(app.server.javaClass, user, OperationType.Read) -> ""
        else -> """
            <tr>
                <td>
                    ${if(!app.thumbnail.isNullOrBlank()) """<img src="${app.thumbnail}" alt="${app.server.applicationName}" class="app-thumbnail"/>""" else ""}
                    ${app.server.applicationName}
                </td>
                <td>
                    <a  href="javascript:void(0);" onclick="showModal('${app.path}/sessions')">List Sessions</a>
                </td>
                <td>
                    ${
            when {
                !authorizationManager.isAuthorized(app.server.javaClass, user, OperationType.Public) -> ""
                else ->
                    """<a class="new-session-link" href="${app.path}/#${StorageInterface.newGlobalID()}">New Public Session</a>"""
            }
        }
                </td>
                <td>
                    ${
            when {
                !authorizationManager.isAuthorized(app.server.javaClass, user, OperationType.Write) -> ""
                else ->
                    """<a class="new-session-link" href="${app.path}/#${StorageInterface.newUserID()}">New Private Session</a>"""
            }
        }
                </td>
            </tr>
        """.trimIndent()
    }


    private fun imageElement(app: ApplicationDirectory.ChildWebApp) = """
        <img src="${app.thumbnail}" alt="${app.server.applicationName}" class="app-thumbnail" onclick="showImageModal('${app.thumbnail}')"/>
    """.trimIndent()
}