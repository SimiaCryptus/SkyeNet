package com.simiacryptus.skyenet.webui.servlet

import com.simiacryptus.skyenet.core.platform.ApplicationServices
import com.simiacryptus.skyenet.core.platform.Session
import com.simiacryptus.skyenet.core.platform.model.StorageInterface
import com.simiacryptus.skyenet.webui.application.ApplicationServer.Companion.getCookie
import jakarta.servlet.http.HttpServletRequest
import java.io.File

class SessionFileServlet(val dataStorage: StorageInterface) : FileServlet() {
  override fun getDir(
    req: HttpServletRequest,
  ): File {
    val pathSegments = parsePath(req.pathInfo ?: "/")
    val session = Session(pathSegments.first())
    return dataStorage.getSessionDir(ApplicationServices.authenticationManager.getUser(req.getCookie()), session)
  }

  override fun getZipLink(req: HttpServletRequest, filePath: String): String {
    val pathSegments = parsePath(req.pathInfo ?: "/")
    val session = Session(pathSegments.first())
    return "${req.contextPath}/fileZip?session=$session&path=$filePath"
  }
}