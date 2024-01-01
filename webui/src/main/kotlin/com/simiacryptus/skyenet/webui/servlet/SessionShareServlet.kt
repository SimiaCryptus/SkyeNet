package com.simiacryptus.skyenet.webui.servlet

import com.simiacryptus.skyenet.core.platform.ApplicationServices.authenticationManager
import com.simiacryptus.skyenet.core.platform.ApplicationServices.authorizationManager
import com.simiacryptus.skyenet.core.platform.AuthorizationInterface.OperationType
import com.simiacryptus.skyenet.webui.application.ApplicationServer
import com.simiacryptus.skyenet.webui.application.ApplicationServer.Companion.getCookie
import jakarta.servlet.http.HttpServlet
import jakarta.servlet.http.HttpServletRequest
import jakarta.servlet.http.HttpServletResponse
import org.apache.http.client.HttpClient
import org.apache.http.client.methods.HttpGet
import org.apache.http.impl.client.HttpClients
import org.openqa.selenium.By
import org.openqa.selenium.Cookie
import org.openqa.selenium.WebDriver
import org.openqa.selenium.WebElement
import org.openqa.selenium.chrome.ChromeDriver
import org.openqa.selenium.chrome.ChromeOptions
import software.amazon.awssdk.core.sync.RequestBody
import software.amazon.awssdk.regions.Region
import software.amazon.awssdk.services.s3.S3Client
import software.amazon.awssdk.services.s3.model.PutObjectRequest
import java.util.*

class SessionShareServlet(
  private val server: ApplicationServer,
) : HttpServlet() {
  override fun doGet(req: HttpServletRequest, resp: HttpServletResponse) {
    resp.contentType = "text/html"
    resp.status = HttpServletResponse.SC_OK

    val user = authenticationManager.getUser(req.getCookie())
    if(!authorizationManager.isAuthorized(server.javaClass, user, OperationType.Share)) {
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
    require(
      when {
        url.startsWith("http://localhost:") -> true
        url.startsWith("https://apps.simiacrypt.us/") -> true
        else -> false
      }
    ) { "Invalid url: $url" }

    val appName = url.split("/").dropLast(1).last()
    val urlbase = url.split("/").dropLast(1).joinToString("/")
    val driver: WebDriver = open(url, req.cookies)
    Thread.sleep(5000)
    val shareId = UUID.randomUUID().toString()
    save(
      driver = driver,
      urlbase = urlbase,
      bucket = "share.simiacrypt.us",
      shareRoot = "/share/$appName/$shareId/",
      cookies = req.cookies,
      currentFilename = "index.html"
    )
    //val shareBase = "https://share.simiacrypt.us/share"
    val shareBase = "https://s3.us-east-1.amazonaws.com/share.simiacrypt.us/share"
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
      |    <p><a href="$shareBase/$appName/$shareId/index.html" target='_blank'>https://share.simiacrypt.us/share/$appName/$shareId/index.html</a></p>
      |</body>
      |</html>
      """.trimMargin()
    )
  }

  companion object {
    private val log = org.slf4j.LoggerFactory.getLogger(SessionShareServlet::class.java)
    private fun uploadToS3(
      bucket: String,
      path: String,
      contentType: String,
      requestBody: RequestBody?
    ) {
      val s3 = S3Client.builder()
        .region(Region.US_EAST_1)
        .build()
      val response = s3.putObject(
        PutObjectRequest.builder()
          .bucket(bucket).key(path.replace("/{2,}".toRegex(), "/").removePrefix("/"))
          .contentType(contentType)
          //.acl(ObjectCannedACL.PUBLIC_READ)
          .build(),
        requestBody
      )
    }

    private fun uploadToS3(bucket: String, path: String, source: String, contentType: String) {
      uploadToS3(bucket, path, contentType, RequestBody.fromString(source))
    }

    fun save(
      driver: WebDriver,
      urlbase: String,
      bucket: String,
      shareRoot: String,
      cookies: Array<out jakarta.servlet.http.Cookie>?,
      currentFilename: String
    ) {
      var html = editPage(driver.pageSource)

      HttpClients.createSystem().use { httpClient: HttpClient ->
        listOf(
          driver.findElements(By.xpath("//a[@href]")).map<WebElement?, String?> { it?.getAttribute("href") }.toSet(),
          driver.findElements(By.xpath("//img[@src]")).map<WebElement?, String?> { it?.getAttribute("src") }.toSet(),
          driver.findElements(By.xpath("//link[@href]")).map<WebElement?, String?> { it?.getAttribute("href") }.toSet(),
          driver.findElements(By.xpath("//script[@src]")).map<WebElement?, String?> { it?.getAttribute("src") }.toSet(),
        ).flatten().filterNotNull().forEach { href ->
          val relative = when {
            href.startsWith(urlbase) -> href.removePrefix(urlbase) // relativize
            href.startsWith("http") -> null // absolute
            else -> href // relative
          }?.replace("/{2,}".toRegex(), "/")?.removePrefix("/")
          if (relative == null) return@forEach
          val extension = relative.split(".").last().split("?").first()
          val contentType = when (extension) {
            "css" -> "text/css"
            "js" -> "text/javascript"
            "svg" -> "image/svg+xml"
            "png" -> "image/png"
            "jpg" -> "image/jpeg"
            "jpeg" -> "image/jpeg"
            "gif" -> "image/gif"
            "html" -> "text/html"
            else -> "text/plain"
          }

          val get = HttpGet(href)
          cookies?.forEach { cookie -> get.addHeader("Cookie", "${cookie.name}=${cookie.value}") }
          val response = httpClient.execute(get)
          val bytes = response.entity.content.readAllBytes()
          val requestBody = RequestBody.fromBytes(bytes)
          val responseType = response.entity.contentType.value
          if (!responseType.startsWith(contentType)) {
            log.warn("Content type mismatch: $responseType != $contentType")
            if (responseType.startsWith("text/html")) {
              log.warn("Response Error: ${String(bytes)}")
            }
            return@forEach
          }
          uploadToS3(bucket = bucket, path = shareRoot + relative, contentType = contentType, requestBody = requestBody)
          if (href != relative) html = html.replace(href, relative)
        }
      }

      uploadToS3(bucket, shareRoot + currentFilename, html, "text/html")
    }

    private fun editPage(html: String): String {
      val doc = org.jsoup.Jsoup.parse(html)
      doc.select("#toolbar").remove()
      return doc.toString()
    }

    fun open(
      url: String?,
      cookies: Array<out jakarta.servlet.http.Cookie>?,
      driver: WebDriver = driver()
    ): WebDriver {
      cookies?.forEach { cookie ->
        try {
          log.info("""Setting cookie:
            |  name: ${cookie.name}
            |  value: ${cookie.value}
            |  domain: ${cookie.domain}
            |  path: ${cookie.path}
            |  maxAge: ${cookie.maxAge}
            |  secure: ${cookie.secure}
            |  isHttpOnly: ${cookie.isHttpOnly}
            |  version: ${cookie.version}
            |  comment: ${cookie.comment}
          """.trimMargin())
          driver.manage().addCookie(
            Cookie(
              /* name = */ cookie.name,
              /* value = */ cookie.value,
              /* domain = */ cookie.domain,
              /* path = */ cookie.path,
              /* expiry = */ Date(cookie.maxAge * 1000L),
              /* isSecure = */ cookie.secure,
              /* isHttpOnly = */ cookie.isHttpOnly
            )
          )
        } catch (e: Exception) {
          log.warn("Error setting cookie: $cookie", e)
        }
      }
      driver.get(url)
      return driver
    }

    fun driver(): WebDriver {
      val osname = System.getProperty("os.name")
      val chromePath = when {
        // Windows
        osname.contains("Windows") -> "C:\\Program Files\\Google\\Chrome\\Application\\chromedriver.exe"
        // Ubuntu
        osname.contains("Linux") -> "/usr/bin/chromedriver"
        else -> throw RuntimeException("Not implemented for $osname")
      }
      System.setProperty("webdriver.chrome.driver", chromePath)
      val options = ChromeOptions()
      options.addArguments("--headless")
      val driver: WebDriver = ChromeDriver(options)
      return driver
    }
  }
}