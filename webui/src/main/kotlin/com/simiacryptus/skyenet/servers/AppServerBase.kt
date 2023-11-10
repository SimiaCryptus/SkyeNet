package com.simiacryptus.skyenet.servers


import com.simiacryptus.openai.OpenAIClient
import com.simiacryptus.skyenet.OutputInterceptor
import com.simiacryptus.skyenet.body.AuthenticatedWebsite
import com.simiacryptus.skyenet.body.WebSocketServer
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
import java.awt.Desktop
import java.net.URI
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
                *(arrayOf(authentication.configure(
                    newWebAppContext(
                        "/",
                        Resource.newResource(javaClass.classLoader.getResource("welcome")),
                        object : HttpServlet() {
                            override fun doGet(req: HttpServletRequest?, resp: HttpServletResponse?) {
                                resp?.contentType = "text/html"
                                resp?.writer?.write("""
                                <!DOCTYPE html>
                                <html lang="en">
                                <head>
                                    <meta charset="UTF-8">
                                    <title>SimiaCryptus Skyenet Apps</title>
                                    <link href="chat.css" rel="stylesheet"/>
                                    <link rel="icon" type="image/png" href="favicon.png"/>
                                </head>
                                <body>
                
                                <div id="toolbar">
                                    ${
                                    childWebApps.joinToString("<br/>") {
                                        """<a href="${it.path}">${it.server.applicationName}</a>"""
                                    }
                                }
                                </div>
                
                                </body>
                                </html>
                            """.trimIndent())
                            }
                        }
                    ), false
                ), newWebAppContext("/proxy", ProxyHttpServlet())) + childWebApps.map {
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
        val webAppContext = newWebAppContext(path, server.baseResource ?: throw IllegalStateException("No base resource"))
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