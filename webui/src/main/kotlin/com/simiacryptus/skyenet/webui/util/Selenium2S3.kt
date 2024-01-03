package com.simiacryptus.skyenet.webui.util

import org.apache.http.HttpEntity
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
  // TODO: This should be configurable without code edits
  open val bucket = "share.simiacrypt.us"
  open val shareBase = "https://share.simiacrypt.us"

  protected open fun uploadToS3(
    bucket: String,
    path: String,
    contentType: String,
    requestBody: RequestBody?
  ) {
    S3Client.builder()
      .region(Region.US_EAST_1)
      .build().putObject(
        PutObjectRequest.builder()
          .bucket(bucket).key(path.replace("/{2,}".toRegex(), "/").removePrefix("/"))
          .contentType(contentType)
          .build(),
        requestBody
      )
  }

  open fun save(
    driver: WebDriver,
    urlbase: String,
    bucket: String,
    shareRoot: String,
    cookies: Array<out jakarta.servlet.http.Cookie>?,
    currentFilename: String,
    startUrl: String
  ) {

    HttpClients.createSystem().use { httpClient: HttpClient ->

      driver.get(startUrl)
      Thread.sleep(5000)
      val linkReplacements = mutableMapOf<String, String>()
      val htmlPages = mutableMapOf(currentFilename to editPage(driver.pageSource))
      val jsonPages = mutableMapOf<String,String>()
      val links = currentPageLinks(driver).toMutableList()

      while (links.isNotEmpty()) {
        val href = links.removeFirst()
        try {
          if (linkReplacements.containsKey(href)) continue
          val relative = relativize(urlbase, href) ?: continue
          linkReplacements[href] = "$shareBase/$shareRoot/$relative"
          val contentType = contentType(relative)
          when (contentType) {
            "text/html" -> {
              if (htmlPages.containsKey(relative)) continue
              log.info("Fetching $href")
              driver.get(href)
              Thread.sleep(5000)
              htmlPages[relative] = editPage(driver.pageSource)
              links += currentPageLinks(driver)
            }
            "application/json" -> {
              if (jsonPages.containsKey(relative)) continue
              log.info("Fetching $href")
              val responseEntity = get(href, cookies, httpClient) ?: continue
              jsonPages[relative] = responseEntity.content.buffered().reader().readText()
            }
            else -> {
              val responseEntity = get(href, cookies, httpClient) ?: continue
              val bytes = responseEntity.content.readAllBytes()
              if (!validate(responseEntity, contentType, bytes)) {
                log.warn("Content type mismatch: $href -> $contentType != ${responseEntity.contentType.value}")
              }
              uploadToS3(
                bucket = bucket,
                path = "/$shareRoot/$relative",
                contentType = contentType,
                requestBody = RequestBody.fromBytes(bytes)
              )
            }
          }
        } catch (e: Exception) {
          log.warn("Error processing $href", e)
        }
      }

      htmlPages.forEach { (filename, html) ->
        val finalHtml = linkReplacements.toList().fold(html) { acc, (href, relative) -> acc.replace(href, relative) }
        uploadToS3(
          bucket = bucket,
          path = "/$shareRoot/$filename",
          contentType = "text/html",
          requestBody = RequestBody.fromString(finalHtml)
        )
      }
      jsonPages.forEach { (filename, js) ->
        val finalJs = linkReplacements.toList().fold(js) { acc, (href, relative) -> acc.replace(href, relative) }
        uploadToS3(
          bucket = bucket,
          path = "/$shareRoot/$filename",
          contentType = "application/json",
          requestBody = RequestBody.fromString(finalJs)
        )
      }
    }

  }

  protected open fun currentPageLinks(driver: WebDriver) = listOf(
    driver.findElements(By.xpath("//a[@href]")).map<WebElement?, String?> { it?.getAttribute("href") }.toSet(),
    driver.findElements(By.xpath("//img[@src]")).map<WebElement?, String?> { it?.getAttribute("src") }.toSet(),
    driver.findElements(By.xpath("//link[@href]")).map<WebElement?, String?> { it?.getAttribute("href") }.toSet(),
    driver.findElements(By.xpath("//script[@src]")).map<WebElement?, String?> { it?.getAttribute("src") }.toSet(),
  ).flatten().filterNotNull()

  protected open fun validate(
    responseEntity: HttpEntity,
    contentType: String,
    bytes: ByteArray
  ): Boolean {
    val responseType = responseEntity.contentType.value
    if (!responseType.startsWith(contentType)) {
      log.warn("Content type mismatch: $responseType != $contentType")
      if (responseType.startsWith("text/html")) {
        log.warn("Response Error: ${String(bytes)}")
      }
      return false
    }
    return true
  }

  protected open fun get(
    href: String,
    cookies: Array<out jakarta.servlet.http.Cookie>?,
    httpClient: HttpClient
  ): HttpEntity? {
    val get = HttpGet(href)
    cookies?.forEach { cookie -> get.addHeader("Cookie", "${cookie.name}=${cookie.value}") }
    return httpClient.execute(get).entity
  }

  protected open fun contentType(relative: String): String {
    val extension = relative.split(".").last().split("?").first()
    val contentType = when (extension) {
      "css" -> "text/css"
      "js" -> "text/javascript"
      "json" -> "application/json"
      "pdf" -> "application/pdf"
      "zip" -> "application/zip"
      "tar" -> "application/x-tar"
      "gz" -> "application/gzip"
      "bz2" -> "application/bzip2"
      //"tsv" -> "text/tab-separated-values"
      "csv" -> "text/csv"
      "txt" -> "text/plain"
      "xml" -> "text/xml"
      "svg" -> "image/svg+xml"
      "png" -> "image/png"
      "jpg" -> "image/jpeg"
      "jpeg" -> "image/jpeg"
      "gif" -> "image/gif"
      "ico" -> "image/x-icon"
      "html" -> "text/html"
      else -> "text/plain"
    }
    return contentType
  }

  protected open fun relativize(base: String, href: String) = when {
    href.startsWith(base) -> href.removePrefix(base) // relativize
    href.startsWith("http") -> null // absolute
    else -> href // relative
  }?.replace("/{2,}".toRegex(), "/")?.removePrefix("/")

  protected open fun editPage(html: String): String {
    val doc = org.jsoup.Jsoup.parse(html)
    doc.select("#toolbar").remove()
    doc.select("#namebar").remove()
    doc.select("#main-input").remove()
    return doc.toString()
  }

  open fun setCookies(
    cookies: Array<out jakarta.servlet.http.Cookie>?,
    domain: String?,
    driver: WebDriver
  ) {
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