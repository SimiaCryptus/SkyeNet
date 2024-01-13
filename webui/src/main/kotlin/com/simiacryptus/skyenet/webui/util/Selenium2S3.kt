package com.simiacryptus.skyenet.webui.util

import com.simiacryptus.skyenet.core.platform.ApplicationServices.cloud
import org.apache.hc.client5.http.async.methods.SimpleHttpRequest
import org.apache.hc.client5.http.async.methods.SimpleHttpResponse
import org.apache.hc.client5.http.cookie.BasicCookieStore
import org.apache.hc.client5.http.impl.async.HttpAsyncClientBuilder
import org.apache.hc.client5.http.impl.cookie.BasicClientCookie
import org.apache.hc.core5.concurrent.FutureCallback
import org.apache.hc.core5.http.Method
import org.openqa.selenium.By
import org.openqa.selenium.Cookie
import org.openqa.selenium.WebDriver
import org.openqa.selenium.WebElement
import org.openqa.selenium.chrome.ChromeDriver
import org.openqa.selenium.chrome.ChromeDriverService
import org.openqa.selenium.chrome.ChromeOptions
import java.io.File
import java.net.URI
import java.net.URL
import java.time.Duration
import java.util.*
import java.util.concurrent.Executors
import java.util.concurrent.Semaphore
import java.util.concurrent.ThreadPoolExecutor

open class Selenium2S3(
  val pool: ThreadPoolExecutor = Executors.newCachedThreadPool() as ThreadPoolExecutor,
  val cookies: Array<out jakarta.servlet.http.Cookie>?,
) : AutoCloseable {

  private val chromeDriverService by lazy { ChromeDriverService.createDefaultService() }

  private val driver: WebDriver by lazy {
    val osname = System.getProperty("os.name")
    val chromePath = when {
      // Windows
      osname.contains("Windows") -> listOf(
        "C:\\Program Files\\Google\\Chrome\\Application\\chromedriver.exe",
        "C:\\Program Files (x86)\\Google\\Chrome\\Application\\chromedriver.exe"
      )
      // Ubuntu
      osname.contains("Linux") -> listOf("/usr/bin/chromedriver")
      else -> throw RuntimeException("Not implemented for $osname")
    }
    System.setProperty("webdriver.chrome.driver",
      chromePath.find { File(it).exists() } ?: throw RuntimeException("Chrome not found"))
    val options = ChromeOptions()
    options.addArguments("--headless", "--blink-settings=imagesEnabled=false")
    options.setPageLoadTimeout(Duration.of(90, java.time.temporal.ChronoUnit.SECONDS))
    ChromeDriver(chromeDriverService, options)
  }

  val cookieStore by lazy {
    BasicCookieStore().apply {
      cookies?.forEach { cookie ->
        addCookie(
          BasicClientCookie(
            cookie.name,
            cookie.value
          )
        )
      }
    }
  }
  private val httpClient by lazy {
    HttpAsyncClientBuilder.create()
      .useSystemProperties()
      .setDefaultCookieStore(cookieStore)
      .setThreadFactory(pool.threadFactory)
      .build()
      .also { it.start() }
  }

  fun save(
    url: URL,
    currentFilename: String?,
    saveRoot: String
  ) {
    driver.navigate().to(url)
    setCookies(driver, cookies) // domain = url.host.split(".").takeLast(2).joinToString(".")
    driver.navigate().refresh()
    Thread.sleep(5000) // Wait for javascript to load

    val linkReplacements = mutableMapOf<String, String>()
    val htmlPages = mutableMapOf((currentFilename ?: url.file.split("/").last()) to editPage(driver.pageSource))
    val jsonPages = mutableMapOf<String, String>()
    val links = currentPageLinks(driver).toMutableList()
    val completionSemaphores = mutableListOf<Semaphore>()

    while (links.isNotEmpty()) {
      val href = links.removeFirst()
      try {
        if (linkReplacements.containsKey(href)) continue
        val relative = relativize(url.toString().split("/").dropLast(1).joinToString("/"), href) ?: continue
        linkReplacements[href] = "${cloud!!.shareBase}/$saveRoot/$relative"
        when (val mimeType = mimeType(relative)) {

          "text/html" -> {
            if (htmlPages.containsKey(relative)) continue
            log.info("Fetching $href")
            val semaphore = Semaphore(0)
            completionSemaphores += semaphore
            httpClient.execute(get(href), object : FutureCallback<SimpleHttpResponse> {

              override fun completed(p0: SimpleHttpResponse?) {
                log.debug("Fetched $href")
                val html = p0?.body?.bodyText ?: ""
                htmlPages[relative] = html
                links += currentPageLinks(html)
                semaphore.release()
              }

              override fun failed(p0: java.lang.Exception?) {
                log.info("Error fetching $href", p0)
                semaphore.release()
              }

              override fun cancelled() {
                log.info("Cancelled fetching $href")
                semaphore.release()
              }

            })
          }

          "application/json" -> {
            if (jsonPages.containsKey(relative)) continue
            log.info("Fetching $href")
            val semaphore = Semaphore(0)
            completionSemaphores += semaphore
            httpClient.execute(get(href), object : FutureCallback<SimpleHttpResponse> {

              override fun completed(p0: SimpleHttpResponse?) {
                log.debug("Fetched $href")
                jsonPages[relative] = p0?.body?.bodyText ?: ""
                semaphore.release()
              }

              override fun failed(p0: java.lang.Exception?) {
                log.info("Error fetching $href", p0)
                semaphore.release()
              }

              override fun cancelled() {
                log.info("Cancelled fetching $href")
                semaphore.release()
              }

            })
          }

          else -> {
            val semaphore = Semaphore(0)
            completionSemaphores += semaphore
            val request = get(href)
            httpClient.execute(request, object : FutureCallback<SimpleHttpResponse> {

              override fun completed(p0: SimpleHttpResponse?) {
                try {
                  log.debug("Fetched $request")
                  val bytes = p0?.body?.bodyBytes ?: return
                  if (validate(mimeType, p0.body.contentType.mimeType, bytes))
                    cloud!!.upload(
                      path = "/$saveRoot/$relative",
                      contentType = mimeType,
                      bytes = bytes
                    )
                } finally {
                  semaphore.release()
                }
              }

              override fun failed(p0: java.lang.Exception?) {
                log.info("Error fetching $href", p0)
                semaphore.release()
              }

              override fun cancelled() {
                log.info("Cancelled fetching $href")
                semaphore.release()
              }

            })
          }
        }
      } catch (e: Exception) {
        log.warn("Error processing $href", e)
      }
    }

    log.debug("Waiting for completion")
    completionSemaphores.forEach { it.acquire(); it.release() }

    log.debug("Saving")
    (htmlPages.map { (filename, html) ->
      pool.submit {
        try {
          val finalHtml = linkReplacements.toList().fold(html) { acc, (href, relative) -> acc.replace(href, relative) }
          cloud!!.upload(
            path = "/$saveRoot/$filename",
            contentType = "text/html",
            request = finalHtml
          )
        } catch (e: Exception) {
          log.warn("Error processing $filename", e)
        }
      }
    } + jsonPages.map { (filename, js) ->
      pool.submit {
        try {
          val finalJs = linkReplacements.toList().fold(js) { acc, (href, relative) -> acc.replace(href, relative) }
          cloud!!.upload(
            path = "/$saveRoot/$filename",
            contentType = "application/json",
            request = finalJs
          )
        } catch (e: Exception) {
          log.warn("Error processing $filename", e)
        }
      }
    }).forEach {
      try {
        it.get()
      } catch (e: Exception) {
        log.warn("Error processing", e)
      }
    }
    log.debug("Done")
  }

  private fun get(href: String): SimpleHttpRequest {
    val request = SimpleHttpRequest(Method.GET, URI(href))
    cookies?.forEach { cookie ->
      request.addHeader("Cookie", "${cookie.name}=${cookie.value}")
    }
    return request
  }

  protected open fun currentPageLinks(driver: WebDriver): List<String> = listOf(
    driver.findElements(By.xpath("//a[@href]")).map<WebElement?, String?> { it?.getAttribute("href") }.toSet(),
    driver.findElements(By.xpath("//img[@src]")).map<WebElement?, String?> { it?.getAttribute("src") }.toSet(),
    driver.findElements(By.xpath("//link[@href]")).map<WebElement?, String?> { it?.getAttribute("href") }.toSet(),
    driver.findElements(By.xpath("//script[@src]")).map<WebElement?, String?> { it?.getAttribute("src") }.toSet(),
    driver.findElements(By.xpath("//source[@src]")).map<WebElement?, String?> { it?.getAttribute("src") }.toSet(),
  ).flatten().filterNotNull()

  protected open fun currentPageLinks(html: String): List<String> = listOf(
    org.jsoup.Jsoup.parse(html).select("a[href]").map { it.attr("href") }.toSet(),
    org.jsoup.Jsoup.parse(html).select("img[src]").map { it.attr("src") }.toSet(),
    org.jsoup.Jsoup.parse(html).select("link[href]").map { it.attr("href") }.toSet(),
    org.jsoup.Jsoup.parse(html).select("script[src]").map { it.attr("src") }.toSet(),
    org.jsoup.Jsoup.parse(html).select("source[src]").map { it.attr("src") }.toSet(),
  ).flatten().filterNotNull()

  protected open fun validate(
    expected: String,
    actual: String,
    bytes: ByteArray
  ): Boolean {
    if (!actual.startsWith(expected)) {
      log.warn("Content type mismatch: $actual != $expected")
      if (actual.startsWith("text/html")) {
        log.warn("Response Error: ${String(bytes)}", Exception())
      }
      return false
    }
    return true
  }

  protected open fun mimeType(relative: String): String {
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
      "mp3" -> "audio/mpeg"
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
      "htm" -> "text/html"
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
    driver: WebDriver,
    cookies: Array<out jakarta.servlet.http.Cookie>?,
    domain: String? = null
  ) {
    cookies?.forEach { cookie ->
      try {
        driver.manage().addCookie(
          Cookie(
            /* name = */ cookie.name,
            /* value = */ cookie.value,
            /* domain = */ cookie.domain ?: domain,
            /* path = */ cookie.path,
            /* expiry = */ if (cookie.maxAge <= 0) null else Date(cookie.maxAge * 1000L),
            /* isSecure = */ cookie.secure,
            /* isHttpOnly = */ cookie.isHttpOnly
          )
        )
      } catch (e: Exception) {
        log.warn("Error setting cookie: $cookie", e)
      }
    }
  }

  override fun close() {
    log.debug("Closing", Exception())
    driver.close()
    chromeDriverService.close()
    httpClient.close()
  }


  companion object {
    private val log = org.slf4j.LoggerFactory.getLogger(Selenium2S3::class.java)

    init {
      Runtime.getRuntime().addShutdownHook(Thread {
        try {
        } catch (e: Exception) {
          log.warn("Error closing com.simiacryptus.skyenet.webui.util.Selenium2S3", e)
        }
      })
    }
  }

}