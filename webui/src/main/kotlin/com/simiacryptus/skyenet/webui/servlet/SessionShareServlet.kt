package com.simiacryptus.skyenet.webui.servlet

import com.simiacryptus.skyenet.core.platform.ApplicationServices
import com.simiacryptus.skyenet.core.platform.ApplicationServices.authenticationManager
import com.simiacryptus.skyenet.core.platform.ApplicationServices.authorizationManager
import com.simiacryptus.skyenet.core.platform.AuthorizationInterface.OperationType
import com.simiacryptus.skyenet.core.platform.StorageInterface
import com.simiacryptus.skyenet.webui.application.ApplicationServer
import com.simiacryptus.skyenet.webui.application.ApplicationServer.Companion.getCookie
import com.simiacryptus.skyenet.webui.util.Selenium2S3
import jakarta.servlet.http.HttpServlet
import jakarta.servlet.http.HttpServletRequest
import jakarta.servlet.http.HttpServletResponse
import java.io.File
import java.net.URI
import java.util.*

class SessionShareServlet(
  private val server: ApplicationServer,
  private val selenium2S3: Selenium2S3 = Selenium2S3(),
) : HttpServlet() {

  override fun doGet(req: HttpServletRequest, resp: HttpServletResponse) {

    val user = authenticationManager.getUser(req.getCookie())
    if (!authorizationManager.isAuthorized(server.javaClass, user, OperationType.Share)) {
      resp.status = HttpServletResponse.SC_FORBIDDEN
      resp.writer.write("Forbidden")
      return
    }

    if (!req.parameterMap.containsKey("url")) {
      resp.status = HttpServletResponse.SC_BAD_REQUEST
      resp.writer.write("Url is required")
      return
    }

    val url = req.getParameter("url")
    val host = URI(url).host
    val appName = url.split("/").dropLast(1).last()
    val urlbase = url.split("/").dropLast(1).joinToString("/")
    val domain = host.split(".").takeLast(2).joinToString(".")
    val sessionID = url.split("#").lastOrNull() ?: throw IllegalArgumentException("No session id in url: $url")

    require(
      when (host) {
        // TODO: This should be configurable without code edits
        "localhost" -> true
        "apps.simiacrypt.us" -> true
        else -> false
      }
    ) { "Invalid url: $url" }

    val storageInterface = ApplicationServices.dataStorageFactory.invoke(File(File(".skyenet"), appName))
    val session = StorageInterface.parseSessionID(sessionID)
    val json = storageInterface.getJson(if(session.isGlobal()) null else user, session, "info.json", Map::class.java)
    val sessionSettings = (json as? Map<String, String>)?.toMutableMap() ?: mutableMapOf()
    val shareId = if(sessionSettings.containsKey("shareId")) {
      log.info("Reusing shareId: ${sessionSettings["shareId"]}")
      sessionSettings["shareId"]
    } else {
      val shareId = UUID.randomUUID().toString()
      log.info("Generating shareId: $shareId")
      sessionSettings["shareId"] = shareId
      // Be optimistic to prevent duplicate work - TODO: Need way to explicitly regenerate
      storageInterface.setJson(if(session.isGlobal()) null else user, session, "info.json", sessionSettings)
      val driver = selenium2S3.driver()
      selenium2S3.setCookies(req.cookies, domain, driver)
      selenium2S3.save(
        driver = driver,
        urlbase = urlbase,
        bucket = selenium2S3.bucket,
        shareRoot = "share/$appName/$shareId",
        cookies = req.cookies,
        currentFilename = "index.html",
        startUrl = url
      )
      driver.quit()
      log.info("Saved session $sessionID to $appName/$shareId")
      shareId
    }

    resp.contentType = "text/html"
    resp.status = HttpServletResponse.SC_OK
    //language=HTML
    resp.writer.write(
      """
      |<html>
      |<head>
      |    <title>Save Session</title>
      |    <style>
      |    </style>
      |    <link rel="icon" type="image/svg+xml" href="/favicon.svg"/>
      |</head>
      |<body>
      |    <h1>Save Session</h1>
      |    <p>Share this link with your friends:</p>
      |    <p><a href="${selenium2S3.shareBase}/share/$appName/$shareId/index.html" target='_blank'>https://share.simiacrypt.us/share/$appName/$shareId/index.html</a></p>
      |</body>
      |</html>
      """.trimMargin()
    )
  }

  companion object {
    private val log = org.slf4j.LoggerFactory.getLogger(SessionShareServlet::class.java)
  }
}

