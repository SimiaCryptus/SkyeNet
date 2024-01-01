package com.simiacryptus.skyenet.webui.util

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

open class Selenium2S3 {
  open val bucket = "share.simiacrypt.us"
  open val shareBase = "https://s3.us-east-1.amazonaws.com/share.simiacrypt.us/share"

  protected open fun uploadToS3(
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

  protected open fun uploadToS3(bucket: String, path: String, source: String, contentType: String) {
    uploadToS3(bucket, path, contentType, RequestBody.fromString(source))
  }

  open fun save(
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

  protected open fun editPage(html: String): String {
    val doc = org.jsoup.Jsoup.parse(html)
    doc.select("#toolbar").remove()
    doc.select("#namebar").remove()
    doc.select("#main-input").remove()
    return doc.toString()
  }

  open fun open(
    url: String?,
    cookies: Array<out jakarta.servlet.http.Cookie>?,
    domain: String? = null,
    driver: WebDriver = driver()
  ): WebDriver {
    cookies?.forEach { cookie ->
      try {
        log.info(
          """Setting cookie:
            |  name: ${cookie.name}
            |  value: ${cookie.value}
            |  domain: ${cookie.domain ?: domain}
            |  path: ${cookie.path}
            |  maxAge: ${cookie.maxAge}
            |  secure: ${cookie.secure}
            |  isHttpOnly: ${cookie.isHttpOnly}
            |  version: ${cookie.version}
            |  comment: ${cookie.comment}
          """.trimMargin()
        )
        driver.manage().addCookie(
          Cookie(
            /* name = */ cookie.name,
            /* value = */ cookie.value,
            /* domain = */ cookie.domain ?: domain,
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

  open fun driver(): WebDriver {
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

  companion object {
    private val log = org.slf4j.LoggerFactory.getLogger(Selenium2S3::class.java)
  }
}