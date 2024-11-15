package com.simiacryptus.skyenet.core.util

import java.net.URL

interface Selenium : AutoCloseable {
  fun navigate(url: String)
  fun getPageSource(): String
  fun getCurrentUrl(): String
  fun executeScript(script: String): Any?
  fun quit()

  fun save(
    url: URL,
    currentFilename: String?,
    saveRoot: String
  )

  abstract fun setScriptTimeout(timeout: Long)
  abstract fun getBrowserInfo(): String
  fun forceQuit()
  abstract fun isAlive(): Boolean
//
//  open fun setCookies(
//    driver: WebDriver,
//    cookies: Array<out Cookie>?,
//    domain: String? = null
//  )

}