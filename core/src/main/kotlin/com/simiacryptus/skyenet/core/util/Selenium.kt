package com.simiacryptus.skyenet.core.util

import java.net.URL

interface Selenium : AutoCloseable {
  fun save(
    url: URL,
    currentFilename: String?,
    saveRoot: String
  )
//
//  open fun setCookies(
//    driver: WebDriver,
//    cookies: Array<out Cookie>?,
//    domain: String? = null
//  )

}