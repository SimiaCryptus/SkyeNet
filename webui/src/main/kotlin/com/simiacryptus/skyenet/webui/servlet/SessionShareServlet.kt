package com.simiacryptus.skyenet.webui.servlet

import com.simiacryptus.skyenet.core.platform.ApplicationServices
import com.simiacryptus.skyenet.core.platform.ApplicationServices.authenticationManager
import com.simiacryptus.skyenet.core.platform.ApplicationServices.authorizationManager
import com.simiacryptus.skyenet.core.platform.ApplicationServices.cloud
import com.simiacryptus.skyenet.core.platform.AuthorizationInterface.OperationType
import com.simiacryptus.skyenet.core.platform.StorageInterface
import com.simiacryptus.skyenet.core.platform.StorageInterface.Companion.long64
import com.simiacryptus.skyenet.core.platform.User
import com.simiacryptus.skyenet.core.util.Selenium
import com.simiacryptus.skyenet.webui.application.ApplicationServer
import com.simiacryptus.skyenet.webui.application.ApplicationServer.Companion.getCookie
import com.simiacryptus.skyenet.webui.util.Selenium2S3
import jakarta.servlet.http.HttpServlet
import jakarta.servlet.http.HttpServletRequest
import jakarta.servlet.http.HttpServletResponse
import org.apache.http.client.HttpClient
import org.apache.http.impl.client.HttpClients
import java.io.File
import java.net.URI

class SessionShareServlet(
    private val server: ApplicationServer,
) : HttpServlet() {

    override fun doGet(req: HttpServletRequest, resp: HttpServletResponse) {

        val user = authenticationManager.getUser(req.getCookie())
        val cookies = req.cookies

        if (!req.parameterMap.containsKey("url")) {
            resp.status = HttpServletResponse.SC_BAD_REQUEST
            resp.writer.write("Url is required")
            return
        }

        val url = req.getParameter("url")
        val host = URI(url).host
        val appName = url.split("/").dropLast(1).last()
        val sessionID = url.split("#").lastOrNull() ?: throw IllegalArgumentException("No session id in url: $url")

        require(acceptHost(user, host)) { "Invalid url: $url" }

        val storageInterface = ApplicationServices.dataStorageFactory.invoke(File(File(".skyenet"), appName))
        val session = StorageInterface.parseSessionID(sessionID)
        val pool = ApplicationServices.clientManager.getPool(session, user, server.dataStorage)
        val json = storageInterface.getJson(
            if (session.isGlobal()) null else user,
            session,
            ".sys/$session/info.json",
            Map::class.java
        )
        val sessionSettings = (json as? Map<String, String>)?.toMutableMap() ?: mutableMapOf()
        val previousShare = sessionSettings["shareId"]
        when {
            null != previousShare && validateUrl(url(appName, previousShare)) -> {
                log.info("Reusing shareId: $previousShare")

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
        |    <h1>Sharing URL</h1>
        |    <p><a href="${url(appName, previousShare)}" target='_blank'>${url(appName, previousShare)}</a></p>
        |</body>
        |</html>
        """.trimMargin()
                )
            }

            !authorizationManager.isAuthorized(server.javaClass, user, OperationType.Share) -> {
                resp.status = HttpServletResponse.SC_FORBIDDEN
                resp.writer.write("Forbidden")
                return
            }

            else -> {
                val shareId = long64()
                currentlyProcessing.add(shareId)
                pool.submit {
                    try {
                        log.info("Generating shareId: $shareId")
                        sessionSettings["shareId"] = shareId
                        storageInterface.setJson(
                            if (session.isGlobal()) null else user,
                            session = session, filename = ".sys/$session/info.json", settings = sessionSettings
                        )
//            val selenium2S3 = Selenium2S3(
//              pool = pool,
//              cookies = cookies,
//            )
                        val selenium2S3: Selenium = ApplicationServices.seleniumFactory?.invoke(pool, cookies)!!
                        if (selenium2S3 is Selenium2S3) {
                            selenium2S3.loadImages = req.getParameter("loadImages")?.toBoolean() ?: false
                        }
                        selenium2S3.save(
                            url = URI(url).toURL(),
                            saveRoot = "$appName/$shareId",
                            currentFilename = "index.html",
                        )
                        log.info("Saved session $sessionID to $appName/$shareId")
                    } catch (e: Throwable) {
                        log.error("Error saving session $sessionID to $appName/$shareId", e)
                    } finally {
                        currentlyProcessing.remove(shareId)
                    }
                }
                resp.contentType = "text/html"
                resp.status = HttpServletResponse.SC_OK
                //language=HTML
                resp.writer.write(
                    """
          |<html>
          |<head>
          |    <title>Saving Session</title>
          |    <style>
          |    </style>
          |    <link rel="icon" type="image/svg+xml" href="/favicon.svg"/>
          |</head>
          |<body>
          |    <h1>Saving Session... This page will soon be ready!</h1>
          |    <p><a href="${url(appName, shareId)}" target='_blank'>${url(appName, shareId)}</a></p>
          |    <p>To monitor progress, you can use the session threads page</p>
          |</body>
          |</html>
          """.trimMargin()
                )
            }
        }


    }

    private fun url(appName: String, shareId: String) =
        """${cloud!!.shareBase}/$appName/$shareId/index.html"""

    private fun acceptHost(user: User?, host: String?): Boolean {
        return when (host) {
            "localhost" -> true
            domain -> true
            else -> authorizationManager.isAuthorized(server.javaClass, user, OperationType.Admin)
        }
    }

    companion object {
        private val log = org.slf4j.LoggerFactory.getLogger(SessionShareServlet::class.java)
        private val currentlyProcessing = mutableSetOf<String>()
        fun validateUrl(previousShare: String): Boolean = when {
            currentlyProcessing.contains(previousShare) -> true
            else -> HttpClients.createSystem().use { httpClient: HttpClient ->
                val responseEntity = httpClient.execute(org.apache.http.client.methods.HttpGet(previousShare))
                return responseEntity.statusLine.statusCode == 200
            }
        }

        var domain = System.getProperty("domain", "apps.simiacrypt.us")
    }
}

