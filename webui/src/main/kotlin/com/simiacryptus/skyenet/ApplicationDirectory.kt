package com.simiacryptus.skyenet


import com.google.api.services.oauth2.model.Userinfo
import com.simiacryptus.openai.OpenAIClient
import com.simiacryptus.skyenet.servlet.*
import com.simiacryptus.skyenet.session.SessionDataStorage
import com.simiacryptus.skyenet.chat.ChatServer
import com.simiacryptus.skyenet.util.AuthorizationManager
import com.simiacryptus.skyenet.util.AwsUtil.decryptResource
import jakarta.servlet.DispatcherType
import jakarta.servlet.Servlet
import jakarta.servlet.http.HttpServlet
import jakarta.servlet.http.HttpServletRequest
import jakarta.servlet.http.HttpServletResponse
import org.eclipse.jetty.server.Server
import org.eclipse.jetty.server.handler.ContextHandlerCollection
import org.eclipse.jetty.servlet.FilterHolder
import org.eclipse.jetty.servlet.ServletHolder
import org.eclipse.jetty.util.resource.Resource
import org.eclipse.jetty.webapp.WebAppContext
import org.eclipse.jetty.websocket.server.config.JettyWebSocketServletContainerInitializer
import org.intellij.lang.annotations.Language
import java.awt.Desktop
import java.net.URI
import java.nio.file.NoSuchFileException
import java.util.*
import kotlin.system.exitProcess

abstract class ApplicationDirectory(
    private val localName: String = "localhost",
    private val publicName: String = "localhost",
    private val port: Int = 8081,
) {
    var domainName: String = ""
    abstract val childWebApps: List<ChildWebApp>

    data class ChildWebApp(
        val path: String,
        val server: ChatServer,
    )

    private fun domainName(isServer: Boolean) =
        if (isServer) "https://$publicName" else "http://$localName:$port"

    val welcomeResources = Resource.newResource(javaClass.classLoader.getResource("welcome"))
    val userInfoServlet = UserInfoServlet()
    val userSettingsServlet = UserSettingsServlet()
    val usageServlet = UsageServlet()

    protected fun _main(args: Array<String>) {
        try {
            OutputInterceptor.setupInterceptor()
            val isServer = args.contains("--server")
            domainName = domainName(isServer)
            OpenAIClient.keyTxt = decryptResource("openai.key.kms", javaClass.classLoader)

            val authentication = AuthenticatedWebsite(
                redirectUri = "$domainName/oauth2callback",
                applicationName = "Demo",
                key = { decryptResource("client_secret_google_oauth.json.kms").byteInputStream() }
            )

            val server = start(
                port,
                *(arrayOf(
                    newWebAppContext("/userInfo", userInfoServlet),
                    newWebAppContext("/userSettings", userSettingsServlet),
                    newWebAppContext("/usage", usageServlet),
                    newWebAppContext("/proxy", ProxyHttpServlet()),
                    authentication.configure(
                        newWebAppContext(
                            "/",
                            welcomeResources,
                            WelcomeServlet()
                        ), false
                    ),
                ) + childWebApps.map {
                    newWebAppContext(it.path, it.server)
                })
            )
            try {
                Desktop.getDesktop().browse(URI("$domainName/"))
            } catch (e: Throwable) {
                // Ignore
            }
            server.join()
        } catch (e: Throwable) {
            e.printStackTrace()
            Thread.sleep(1000)
            exitProcess(1)
        } finally {
            Thread.sleep(1000)
            exitProcess(0)
        }
    }

    private inner class WelcomeServlet : HttpServlet() {
        override fun doGet(req: HttpServletRequest?, resp: HttpServletResponse?) {
            val user = AuthenticatedWebsite.getUser(req!!)
            val requestURI = req.requestURI ?: "/"
            resp?.contentType = when (requestURI) {
                "/" -> "text/html"
                else -> ApplicationBase.getMimeType(requestURI)
            }
            when {
                requestURI == "/" -> resp?.writer?.write(homepage(user).trimIndent())
                requestURI == "/index.html" -> resp?.writer?.write(homepage(user).trimIndent())
                requestURI.startsWith("/userInfo") -> userInfoServlet.doGet(req, resp!!)
                requestURI.startsWith("/userSettings") -> userSettingsServlet.doGet(req, resp!!)
                requestURI.startsWith("/usage") -> usageServlet.doGet(req, resp!!)
                else -> try {
                    val inputStream = welcomeResources.addPath(requestURI)?.inputStream
                    inputStream?.copyTo(resp?.outputStream!!)
                } catch (e: NoSuchFileException) {
                    resp?.sendError(404)
                }
            }
        }

        override fun doPost(req: HttpServletRequest?, resp: HttpServletResponse?) {
            val requestURI = req?.requestURI ?: "/"
            when {
                requestURI.startsWith("/userSettings") -> userSettingsServlet.doPost(req!!, resp!!)
                else -> resp?.sendError(404)
            }
        }
    }

    private fun homepage(user: Userinfo?): String {
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
    
    <div id="toolbar">
    </div>
    
    <div id="namebar">
        <a href="/googleLogin" id="username">Login</a>
    </div>
    
    <table id="applist">
        ${
            childWebApps.joinToString("\n") { app ->
                val canRun = AuthorizationManager.isAuthorized(
                    applicationClass = app.server.javaClass,
                    user = user?.email,
                    operationType = AuthorizationManager.OperationType.Write
                )
                val canRead = AuthorizationManager.isAuthorized(
                    applicationClass = app.server.javaClass,
                    user = user?.email,
                    operationType = AuthorizationManager.OperationType.Read
                )
                if (!canRead) return@joinToString ""
                val newSessionLink = if(canRun) """<a href="${app.path}/#${SessionDataStorage.newID()}">New</a>""" else ""
                """
                    <tr>
                        <td>
                            ${app.server.applicationName}
                        </td>
                        <td>
                            $newSessionLink
                        </td>
                        <td>
                            <a href="${app.path}/sessions">List</a>
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

    private fun start(
        port: Int,
        vararg webAppContexts: WebAppContext
    ): Server {
        val contexts = ContextHandlerCollection()
        contexts.handlers = webAppContexts.map {
            it.addFilter(FilterHolder(CorsFilter()), "/*", EnumSet.of(DispatcherType.REQUEST))
            it
        }.toTypedArray()
        val server = Server(port)
        server.handler = contexts
        server.start()
        return server
    }

    private fun newWebAppContext(path: String, server: ChatServer): WebAppContext {
        val webAppContext =
            newWebAppContext(path, server.baseResource ?: throw IllegalStateException("No base resource"))
        server.configure(webAppContext, path = path, baseUrl = "$domainName/$path")
        return webAppContext
    }

    private fun newWebAppContext(path: String, baseResource: Resource, indexServlet: Servlet? = null): WebAppContext {
        val context = WebAppContext()
        JettyWebSocketServletContainerInitializer.configure(context, null)
        context.baseResource = baseResource
        context.contextPath = path
        context.welcomeFiles = arrayOf("index.html")
        if (indexServlet != null) {
            context.addServlet(ServletHolder("index", indexServlet), "/index.html")
            context.addServlet(ServletHolder("index", indexServlet), "/")
        }
        return context
    }

    private fun newWebAppContext(path: String, servlet: Servlet): WebAppContext {
        val context = WebAppContext()
        JettyWebSocketServletContainerInitializer.configure(context, null)
        context.contextPath = path
        //context.welcomeFiles = arrayOf("index.html")
        context.addServlet(ServletHolder("index", servlet), "/")
        return context
    }
}