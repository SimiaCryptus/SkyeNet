package com.simiacryptus.skyenet.webui.servlet

import Selenium2S3
import com.simiacryptus.skyenet.core.platform.ApplicationServices
import com.simiacryptus.skyenet.core.platform.ApplicationServices.authenticationManager
import com.simiacryptus.skyenet.core.platform.ApplicationServices.authorizationManager
import com.simiacryptus.skyenet.core.platform.ApplicationServices.uploader
import com.simiacryptus.skyenet.core.platform.AuthorizationInterface.OperationType
import com.simiacryptus.skyenet.core.platform.StorageInterface
import com.simiacryptus.skyenet.core.platform.User
import com.simiacryptus.skyenet.webui.application.ApplicationServer
import com.simiacryptus.skyenet.webui.application.ApplicationServer.Companion.getCookie
import jakarta.servlet.http.HttpServlet
import jakarta.servlet.http.HttpServletRequest
import jakarta.servlet.http.HttpServletResponse
import java.io.File
import java.net.URI
import java.net.URL
import java.util.*

class SessionShareServlet(
  private val server: ApplicationServer,
) : HttpServlet() {

  override fun doGet(req: HttpServletRequest, resp: HttpServletResponse) {

    val user = authenticationManager.getUser(req.getCookie())

    if (!req.parameterMap.containsKey("url")) {
      resp.status = HttpServletResponse.SC_BAD_REQUEST
      resp.writer.write("Url is required")
      return
    }

    val url = req.getParameter("url")
    val host = URI(url).host
    val appName = url.split("/").dropLast(1).last()
    val sessionID = url.split("#").lastOrNull() ?: throw IllegalArgumentException("No session id in url: $url")

    require(
      acceptHost(user, host)
    ) { "Invalid url: $url" }

    val storageInterface = ApplicationServices.dataStorageFactory.invoke(File(File(".skyenet"), appName))
    val session = StorageInterface.parseSessionID(sessionID)
    val json = storageInterface.getJson(if (session.isGlobal()) null else user, session, "info.json", Map::class.java)
    val sessionSettings = (json as? Map<String, String>)?.toMutableMap() ?: mutableMapOf()
    val shareId = if (sessionSettings.containsKey("shareId")) {
      log.info("Reusing shareId: ${sessionSettings["shareId"]}")
      sessionSettings["shareId"]
    } else {
      if (!authorizationManager.isAuthorized(server.javaClass, user, OperationType.Share)) {
        resp.status = HttpServletResponse.SC_FORBIDDEN
        resp.writer.write("Forbidden")
        return
      }
      val shareId = UUID.randomUUID().toString()
      log.info("Generating shareId: $shareId")
      sessionSettings["shareId"] = shareId
      // Be optimistic to prevent duplicate work - TODO: Need way to explicitly regenerate
      storageInterface.setJson(if (session.isGlobal()) null else user, session, "info.json", sessionSettings)
      Selenium2S3().use {
        it.save(
          url = URL(url),
          saveRoot = "$appName/$shareId",
          currentFilename = "index.html",
          cookies = req.cookies
        )
      }
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
      |    <p><a href="${uploader.shareBase}/$appName/$shareId/index.html" target='_blank'>https://share.simiacrypt.us/$appName/$shareId/index.html</a></p>
      |</body>
      |</html>
      """.trimMargin()
    )
  }

  private fun acceptHost(user: User?, host: String?) = when (host) {
    // TODO: This should be configurable without code edits
    "localhost" -> true
    "apps.simiacrypt.us" -> true
    else -> authorizationManager.isAuthorized(server.javaClass, user, OperationType.Admin)
  }

  companion object {
    private val log = org.slf4j.LoggerFactory.getLogger(SessionShareServlet::class.java)
  }
}

