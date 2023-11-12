package com.simiacryptus.skyenet.servers


import com.simiacryptus.openai.OpenAIClient
import com.simiacryptus.skyenet.OutputInterceptor
import com.simiacryptus.skyenet.util.AwsUtil.decryptResource
import com.simiacryptus.skyenet.webui.AuthenticatedWebsite
import com.simiacryptus.skyenet.webui.SessionServerBase
import com.simiacryptus.skyenet.webui.SessionServerBase.UserInfoServlet
import com.simiacryptus.skyenet.webui.WebSocketServer
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

abstract class AppServerBase(
    private val localName: String = "localhost",
    private val port: Int = 8081,
) {
    var domainName: String = ""
    abstract val childWebApps: List<ChildWebApp>

    data class ChildWebApp(
        val path: String,
        val server: WebSocketServer,
        val isAuthenticated: Boolean = false
    )

    private fun domainName(isServer: Boolean) =
        if (isServer) "https://apps.simiacrypt.us" else "http://$localName:$port"

    val welcomeResources = Resource.newResource(javaClass.classLoader.getResource("welcome"))
    val userInfoServlet = UserInfoServlet()

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
                    newWebAppContext("/proxy", ProxyHttpServlet()),
                    authentication.configure(
                        newWebAppContext(
                            "/",
                            welcomeResources,
                            WelcomeServlet()
                        ), false
                    ),
                ) + childWebApps.map {
                    if (it.isAuthenticated) authentication.configure(
                        newWebAppContext(
                            it.path,
                            it.server
                        )
                    ) else newWebAppContext(it.path, it.server)
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
            System.exit(1)
        } finally {
            Thread.sleep(1000)
            System.exit(0)
        }
    }

    inner class WelcomeServlet() : HttpServlet() {
        override fun doGet(req: HttpServletRequest?, resp: HttpServletResponse?) {
            val requestURI = req?.requestURI ?: "/"
            resp?.contentType = when (requestURI) {
                "/" -> "text/html"
                else -> SessionServerBase.getMimeType(requestURI)
            }
            when {
                requestURI == "/" -> resp?.writer?.write(homepage().trimIndent())
                requestURI == "/index.html" -> resp?.writer?.write(homepage().trimIndent())
                requestURI.startsWith("/userInfo") -> userInfoServlet.doGet(req!!, resp!!)
                else -> try {
                    val inputStream = welcomeResources.addPath(requestURI)?.inputStream
                    inputStream?.copyTo(resp?.outputStream!!)
                } catch (e: NoSuchFileException) {
                    resp?.sendError(404)
                }
            }
        }
    }

    @Language("HTML")
    private fun homepage() = """<!DOCTYPE html>
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

<div id="applist">
    ${
        childWebApps.joinToString("<br/>") {
            """<a href="${it.path}">${it.server.applicationName}</a>"""
        }
    }
</div>

</body>
</html>
    """

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

    private fun newWebAppContext(path: String, server: WebSocketServer): WebAppContext {
        val webAppContext =
            newWebAppContext(path, server.baseResource ?: throw IllegalStateException("No base resource"))
        server.configure(webAppContext, baseUrl = "$domainName/$path")
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