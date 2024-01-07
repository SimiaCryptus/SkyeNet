package com.simiacryptus.skyenet.webui.servlet

import Selenium2S3
import com.simiacryptus.skyenet.core.platform.ApplicationServices
import com.simiacryptus.skyenet.core.platform.ApplicationServices.authenticationManager
import com.simiacryptus.skyenet.core.platform.ApplicationServices.authorizationManager
import com.simiacryptus.skyenet.core.platform.ApplicationServices.uploader
import com.simiacryptus.skyenet.core.platform.AuthorizationInterface.OperationType
import com.simiacryptus.skyenet.core.platform.Session
import com.simiacryptus.skyenet.core.platform.StorageInterface
import com.simiacryptus.skyenet.core.platform.User
import com.simiacryptus.skyenet.webui.application.ApplicationServer
import com.simiacryptus.skyenet.webui.application.ApplicationServer.Companion.getCookie
import jakarta.servlet.http.HttpServlet
import jakarta.servlet.http.HttpServletRequest
import jakarta.servlet.http.HttpServletResponse
import org.apache.http.client.HttpClient
import org.apache.http.impl.client.HttpClients
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
    val previousShare = sessionSettings["shareId"]
    val shareId = if (null != previousShare && validateUrl(url(appName, previousShare))) {
      log.info("Reusing shareId: $previousShare")
      previousShare
    } else {
      if (!authorizationManager.isAuthorized(server.javaClass, user, OperationType.Share)) {
        resp.status = HttpServletResponse.SC_FORBIDDEN
        resp.writer.write("Forbidden")
        return
      }
      save(sessionSettings, storageInterface, session, user, url, appName, req, sessionID)
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
      |    <p><a href="${url(appName, shareId)}" target='_blank'>${url(appName, shareId)}</a></p>
      |</body>
      |</html>
      """.trimMargin()
    )
  }

  fun url(appName: String, shareId: String) =
    """${uploader.shareBase}/$appName/$shareId/index.html"""

  private fun save(
    sessionSettings: MutableMap<String, String>,
    storageInterface: StorageInterface,
    session: Session,
    user: User?,
    url: String?,
    appName: String,
    req: HttpServletRequest,
    sessionID: String
  ): String {
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
    return shareId
  }

  private fun acceptHost(user: User?, host: String?): Boolean {
    return when (host) {
      // TODO: This should be configurable without code edits
      "localhost" -> true
      domain -> true
      else -> authorizationManager.isAuthorized(server.javaClass, user, OperationType.Admin)
    }
  }

  companion object {
    private val log = org.slf4j.LoggerFactory.getLogger(SessionShareServlet::class.java)
    fun validateUrl(previousShare: String): Boolean = HttpClients.createSystem().use { httpClient: HttpClient ->
      val responseEntity = httpClient.execute(org.apache.http.client.methods.HttpGet(previousShare))
      return responseEntity.statusLine.statusCode == 200
    }

    var domain = System.getProperty("domain", "apps.simiacrypt.us")
  }
}

