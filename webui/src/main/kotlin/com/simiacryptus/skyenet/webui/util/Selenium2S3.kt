import com.simiacryptus.skyenet.core.platform.ApplicationServices.uploader
import org.apache.http.HttpEntity
import org.apache.http.client.HttpClient
import org.apache.http.client.methods.HttpGet
import org.apache.http.impl.client.HttpClients
import org.openqa.selenium.By
import org.openqa.selenium.Cookie
import org.openqa.selenium.WebDriver
import org.openqa.selenium.WebElement
import org.openqa.selenium.chrome.ChromeDriver
import org.openqa.selenium.chrome.ChromeDriverService
import org.openqa.selenium.chrome.ChromeOptions
import java.net.URL
import java.util.*

open class Selenium2S3 : AutoCloseable {

  open fun save(
    url: URL,
    saveRoot: String,
    currentFilename: String? = null,
    cookies: Array<out jakarta.servlet.http.Cookie>? = null
  ) {
    val urlbase = url.toString().split("/").dropLast(1).joinToString("/")
    val domain = url.host.split(".").takeLast(2).joinToString(".")

    if (domain == "localhost") {
      driver.navigate().to(url)
      setCookies(driver, cookies)
      driver.navigate().refresh()
    } else {
      setCookies(driver, cookies, domain)
      driver.navigate().to(url)
    }

    HttpClients.createSystem().use { httpClient: HttpClient ->

      Thread.sleep(5000)
      val linkReplacements = mutableMapOf<String, String>()
      val htmlPages = mutableMapOf((currentFilename ?: url.file.split("/").last()) to editPage(driver.pageSource))
      val jsonPages = mutableMapOf<String, String>()
      val links = currentPageLinks(driver).toMutableList()

      while (links.isNotEmpty()) {
        val href = links.removeFirst()
        try {
          if (linkReplacements.containsKey(href)) continue
          val relative = relativize(urlbase, href) ?: continue
          linkReplacements[href] = "${uploader.shareBase}/$saveRoot/$relative"
          when (val contentType = contentType(relative)) {
            "text/html" -> {
              if (htmlPages.containsKey(relative)) continue
              log.info("Fetching $href")
              driver.navigate().to(href)
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
              uploader.upload(
                path = "/$saveRoot/$relative",
                contentType = contentType,
                bytes = bytes
              )
            }
          }
        } catch (e: Exception) {
          log.warn("Error processing $href", e)
        }
      }

      htmlPages.forEach { (filename, html) ->
        val finalHtml = linkReplacements.toList().fold(html) { acc, (href, relative) -> acc.replace(href, relative) }
        uploader.upload(
          path = "/$saveRoot/$filename",
          contentType = "text/html",
          request = finalHtml
        )
      }
      jsonPages.forEach { (filename, js) ->
        val finalJs = linkReplacements.toList().fold(js) { acc, (href, relative) -> acc.replace(href, relative) }
        uploader.upload(
          path = "/$saveRoot/$filename",
          contentType = "application/json",
          request = finalJs
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

  private val driver: WebDriver by lazy {
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
    ChromeDriver(chromeDriverService, options)
  }

  override fun close() {
    driver.close()
    chromeDriverService.close()
  }

  private val chromeDriverService by lazy { ChromeDriverService.createDefaultService() }

  companion object {
    private val log = org.slf4j.LoggerFactory.getLogger(Selenium2S3::class.java)

    init {
      Runtime.getRuntime().addShutdownHook(Thread {
        try {
        } catch (e: Exception) {
          log.warn("Error closing Selenium2S3", e)
        }
      })
    }
  }

}