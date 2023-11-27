package com.simiacryptus.skyenet.webui.application


import com.simiacryptus.jopenai.ClientUtil
import com.simiacryptus.skyenet.core.OutputInterceptor
import com.simiacryptus.skyenet.core.platform.ApplicationServices
import com.simiacryptus.skyenet.core.util.AwsUtil.decryptResource
import com.simiacryptus.skyenet.webui.chat.ChatServer
import com.simiacryptus.skyenet.webui.servlet.*
import jakarta.servlet.DispatcherType
import jakarta.servlet.Servlet
import org.eclipse.jetty.server.*
import org.eclipse.jetty.server.handler.ContextHandlerCollection
import org.eclipse.jetty.servlet.FilterHolder
import org.eclipse.jetty.servlet.ServletHolder
import org.eclipse.jetty.util.resource.Resource
import org.eclipse.jetty.util.resource.Resource.newResource
import org.eclipse.jetty.util.resource.ResourceCollection
import org.eclipse.jetty.webapp.WebAppContext
import org.eclipse.jetty.websocket.server.config.JettyWebSocketServletContainerInitializer
import org.slf4j.LoggerFactory
import java.awt.Desktop
import java.net.URI
import java.util.*
import kotlin.system.exitProcess

abstract class ApplicationDirectory(
    val localName: String = "localhost",
    val publicName: String = "localhost",
    val port: Int = 8081,
) {
    var domainName: String = "" // Resolved in _main
        private set
    abstract val childWebApps: List<com.simiacryptus.skyenet.webui.application.ApplicationDirectory.ChildWebApp>

    data class ChildWebApp(
        val path: String,
        val server: ChatServer,
    )

    private fun domainName(isServer: Boolean) =
        if (isServer) "https://$publicName" else "http://$localName:$port"


    open val welcomeResources = ResourceCollection(allResources("welcome").map(::newResource))
    open val userInfoServlet = UserInfoServlet()
    open val userSettingsServlet = UserSettingsServlet()
    open val logoutServlet = LogoutServlet()
    open val usageServlet = UsageServlet()
    open val proxyHttpServlet = ProxyHttpServlet()
    open val welcomeServlet = WelcomeServlet(this)
    open fun authenticatedWebsite(): OAuthBase? = OAuthGoogle(
        redirectUri = "$domainName/oauth2callback",
        applicationName = "Demo",
        key = { decryptResource("client_secret_google_oauth.json.kms").byteInputStream() }
    )

    protected open fun _main(args: Array<String>) {
        try {
            init(args.contains("--server"))
            ClientUtil.keyTxt = decryptResource("openai.key.kms", javaClass.classLoader)
            ApplicationServices.isLocked = true
            val welcomeContext = newWebAppContext("/", welcomeResources, welcomeServlet)
            val server = start(
                port,
                *(arrayOf(
                    newWebAppContext("/userInfo", userInfoServlet),
                    newWebAppContext("/userSettings", userSettingsServlet),
                    newWebAppContext("/usage", usageServlet),
                    newWebAppContext("/proxy", proxyHttpServlet),
                    authenticatedWebsite()?.configure(welcomeContext, false) ?: welcomeContext,
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

    open fun init(isServer: Boolean): com.simiacryptus.skyenet.webui.application.ApplicationDirectory {
        OutputInterceptor.setupInterceptor()
        domainName = domainName(isServer)
        return this
    }

    protected open fun start(
        port: Int,
        vararg webAppContexts: WebAppContext
    ): Server {
        val contexts = ContextHandlerCollection()
        contexts.handlers = webAppContexts.map {
            it.addFilter(FilterHolder(CorsFilter()), "/*", EnumSet.of(DispatcherType.REQUEST))
            it
        }.toTypedArray()
        val server = Server(port)
        val serverConnector = ServerConnector(server, httpConnectionFactory())
        serverConnector.port = port
        server.connectors = arrayOf(serverConnector)
        server.handler = contexts
        server.start()
        if (!server.isStarted) throw IllegalStateException("Server failed to start")
        return server
    }

    protected open fun httpConnectionFactory(): HttpConnectionFactory {
        val httpConfig = HttpConfiguration()
        httpConfig.addCustomizer(ForwardedRequestCustomizer())
        return HttpConnectionFactory(httpConfig)
    }

    protected open fun newWebAppContext(path: String, server: ChatServer): WebAppContext {
        val baseResource = server.baseResource ?: throw IllegalStateException("No base resource")
        val webAppContext = newWebAppContext(path, baseResource)
        server.configure(webAppContext, path = path, baseUrl = "$domainName/$path")
        return webAppContext
    }

    protected open fun newWebAppContext(
        path: String,
        baseResource: Resource,
        indexServlet: Servlet? = null
    ): WebAppContext {
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

    protected open fun newWebAppContext(path: String, servlet: Servlet): WebAppContext {
        val context = WebAppContext()
        JettyWebSocketServletContainerInitializer.configure(context, null)
        context.contextPath = path
        //context.welcomeFiles = arrayOf("index.html")
        context.addServlet(ServletHolder("index", servlet), "/")
        return context
    }


    companion object {
        private val log = LoggerFactory.getLogger(com.simiacryptus.skyenet.webui.application.ApplicationDirectory::class.java)
        fun allResources(resourceName: String) =
            Thread.currentThread().contextClassLoader.getResources(resourceName).toList()
    }

}