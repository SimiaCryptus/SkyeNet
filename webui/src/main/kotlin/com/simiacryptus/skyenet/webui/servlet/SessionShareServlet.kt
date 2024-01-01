package com.simiacryptus.skyenet.webui.servlet

import com.simiacryptus.skyenet.core.platform.ApplicationServices.authenticationManager
import com.simiacryptus.skyenet.core.platform.ApplicationServices.authorizationManager
import com.simiacryptus.skyenet.core.platform.AuthorizationInterface.OperationType
import com.simiacryptus.skyenet.webui.application.ApplicationServer
import com.simiacryptus.skyenet.webui.application.ApplicationServer.Companion.getCookie
import com.simiacryptus.skyenet.webui.util.Selenium2S3
import jakarta.servlet.http.HttpServlet
import jakarta.servlet.http.HttpServletRequest
import jakarta.servlet.http.HttpServletResponse
import org.openqa.selenium.WebDriver
import java.net.URI
import java.util.*

class SessionShareServlet(
  private val server: ApplicationServer,
  private val selenium2S3: Selenium2S3 = Selenium2S3(),
) : HttpServlet() {

  override fun doGet(req: HttpServletRequest, resp: HttpServletResponse) {
    resp.contentType = "text/html"
    resp.status = HttpServletResponse.SC_OK

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
    require(
      when (host) {
        "localhost" -> true
        "apps.simiacrypt.us" -> true
        else -> false
      }
    ) { "Invalid url: $url" }


    val appName = url.split("/").dropLast(1).last()
    val urlbase = url.split("/").dropLast(1).joinToString("/")
    val domain = host.split(".").takeLast(2).joinToString(".")
    val driver: WebDriver = selenium2S3.open(url = url, cookies = req.cookies, domain = domain)
    Thread.sleep(5000)
    val shareId = UUID.randomUUID().toString()
    selenium2S3.save(
      driver = driver,
      urlbase = urlbase,
      bucket = selenium2S3.bucket,
      shareRoot = "/share/$appName/$shareId/",
      cookies = req.cookies,
      currentFilename = "index.html"
    )
    //val shareBase = "https://share.simiacrypt.us/share"
    driver.quit()

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
      |    <p><a href="${selenium2S3.shareBase}/$appName/$shareId/index.html" target='_blank'>https://share.simiacrypt.us/share/$appName/$shareId/index.html</a></p>
      |</body>
      |</html>
      """.trimMargin()
    )
  }

  companion object {
    private val log = org.slf4j.LoggerFactory.getLogger(SessionShareServlet::class.java)
  }
}

