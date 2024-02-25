package com.simiacryptus.skyenet.webui.application


import com.simiacryptus.jopenai.util.ClientUtil
import com.simiacryptus.skyenet.core.OutputInterceptor
import com.simiacryptus.skyenet.core.platform.ApplicationServices
import com.simiacryptus.skyenet.webui.chat.ChatServer
import com.simiacryptus.skyenet.webui.servlet.*
import com.simiacryptus.skyenet.webui.util.Selenium2S3
import jakarta.servlet.DispatcherType
import jakarta.servlet.Servlet
import org.eclipse.jetty.server.*
import org.eclipse.jetty.server.handler.ContextHandlerCollection
import org.eclipse.jetty.servlet.FilterHolder
import org.eclipse.jetty.servlet.ServletHolder
import org.eclipse.jetty.servlet.StatisticsServlet
import org.eclipse.jetty.util.resource.Resource
import org.eclipse.jetty.util.resource.Resource.newResource
import org.eclipse.jetty.util.resource.ResourceCollection
import org.eclipse.jetty.webapp.WebAppClassLoader
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
  abstract val childWebApps: List<ChildWebApp>

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
  open val apiKeyServlet = ApiKeyServlet()
  open val welcomeServlet = WelcomeServlet(this)
  abstract val toolServlet : ToolServlet?

  open fun authenticatedWebsite(): OAuthBase? = OAuthGoogle(
    redirectUri = "$domainName/oauth2callback",
    applicationName = "Demo",
    key = {
      val encryptedData =
        javaClass.classLoader!!.getResourceAsStream("client_secret_google_oauth.json.kms")?.readAllBytes()
          ?: throw RuntimeException("Unable to load resource: ${"client_secret_google_oauth.json.kms"}")
      ApplicationServices.cloud!!.decrypt(encryptedData).byteInputStream()
    }
  )

  open fun setupPlatform() {
    ApplicationServices.seleniumFactory = { pool, cookies ->
      Selenium2S3(
        pool,
        cookies,
      )
    }
  }

  protected open fun _main(args: Array<String>) {
    try {
      setupPlatform()
      init(args.contains("--server"))
      ClientUtil.keyTxt = run {
        try {
          val encryptedData = javaClass.classLoader.getResourceAsStream("openai.key.kms")?.readAllBytes()
            ?: throw RuntimeException("Unable to load resource: ${"openai.key.kms"}")
          ApplicationServices.cloud!!.decrypt(encryptedData)
        } catch (e: Throwable) {
          log.warn("Error loading key.txt", e)
          ""
        }
      }
      ApplicationServices.isLocked = true
      val server = start(
        port,
        *(listOfNotNull(
          newWebAppContext("/logout", logoutServlet),
          newWebAppContext("/proxy", proxyHttpServlet),
          toolServlet?.let { newWebAppContext("/tools", it) },
          newWebAppContext("/userInfo", userInfoServlet).let {
            authenticatedWebsite()?.configure(it, true) ?: it
          },
          newWebAppContext("/userSettings", userSettingsServlet).let {
            authenticatedWebsite()?.configure(it, true) ?: it
          },
          newWebAppContext("/usage", usageServlet).let {
            authenticatedWebsite()?.configure(it, true) ?: it
          },
          newWebAppContext("/apiKeys", apiKeyServlet).let {
            authenticatedWebsite()?.configure(it, true) ?: it
          },
          newWebAppContext("/", welcomeResources, "welcome", welcomeServlet).let {
            authenticatedWebsite()?.configure(it, false) ?: it
          },
        ).toTypedArray() + childWebApps.map {
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

  open fun init(isServer: Boolean): ApplicationDirectory {
    OutputInterceptor.setupInterceptor()
    domainName = domainName(isServer)
    return this
  }

  protected open fun start(
    port: Int,
    vararg webAppContexts: WebAppContext
  ): Server {
    val contexts = ContextHandlerCollection()
//    val stats = StatisticsHandler()
    contexts.handlers = (
        listOf(
          newWebAppContext("/stats", StatisticsServlet())) +
          webAppContexts.map {
              it.addFilter(FilterHolder(CorsFilter()), "/*", EnumSet.of(DispatcherType.REQUEST))
              it
            }
        ).toTypedArray()
    val server = Server(port)
   // Increase the number of acceptors and selectors for better scalability in a non-blocking model
   val serverConnector = ServerConnector(server, 4, 8, httpConnectionFactory())
    serverConnector.port = port
    serverConnector.acceptQueueSize = 1000
   serverConnector.idleTimeout = 30000 // Set idle timeout to 30 seconds
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
    val webAppContext = newWebAppContext(path, baseResource, resourceBase = "applicaton")
    server.configure(webAppContext)
    return webAppContext
  }

  protected open fun newWebAppContext(
    path: String,
    baseResource: Resource,
    resourceBase: String,
    indexServlet: Servlet? = null
  ): WebAppContext {
    val context = WebAppContext()
    JettyWebSocketServletContainerInitializer.configure(context, null)
    context.classLoader = WebAppClassLoader(ApplicationServices::class.java.classLoader, context)
    context.isParentLoaderPriority = true
    context.baseResource = baseResource
    context.contextPath = path
    context.welcomeFiles = arrayOf("index.html")
    if (indexServlet != null) {
      context.addServlet(ServletHolder("$path/index", indexServlet), "/index.html")
    }
    return context
  }

  protected open fun newWebAppContext(path: String, servlet: Servlet): WebAppContext {
    val context = WebAppContext()
    JettyWebSocketServletContainerInitializer.configure(context, null)
    context.classLoader = WebAppClassLoader(ApplicationServices::class.java.classLoader, context)
    context.isParentLoaderPriority = true
    context.contextPath = path
    context.resourceBase = "application"
    context.welcomeFiles = arrayOf("index.html")
    context.addServlet(ServletHolder(servlet), "/")
    return context
  }


  companion object {
    private val log = LoggerFactory.getLogger(ApplicationDirectory::class.java)
    fun allResources(resourceName: String) =
      Thread.currentThread().contextClassLoader.getResources(resourceName).toList()
  }

}