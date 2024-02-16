package com.simiacryptus.skyenet.webui.servlet

import com.google.common.cache.CacheBuilder
import com.google.common.cache.CacheLoader
import com.google.common.cache.RemovalListener
import com.simiacryptus.skyenet.core.platform.ApplicationServices
import com.simiacryptus.skyenet.core.platform.Session
import com.simiacryptus.skyenet.core.platform.StorageInterface
import com.simiacryptus.skyenet.webui.application.ApplicationServer
import com.simiacryptus.skyenet.webui.application.ApplicationServer.Companion.getCookie
import jakarta.servlet.WriteListener
import jakarta.servlet.http.HttpServlet
import jakarta.servlet.http.HttpServletRequest
import jakarta.servlet.http.HttpServletResponse
import org.slf4j.LoggerFactory
import java.io.File
import java.nio.MappedByteBuffer
import java.nio.channels.FileChannel
import java.nio.file.StandardOpenOption

class FileServlet(val dataStorage: StorageInterface) : HttpServlet() {
  private val channelCache = CacheBuilder
    .newBuilder().maximumSize(100)
    .removalListener(RemovalListener<File, FileChannel> { notification ->
      notification.value?.close()
    }).build(object : CacheLoader<File, FileChannel>() {
      override fun load(key: File): FileChannel {
        return FileChannel.open(key.toPath(), StandardOpenOption.WRITE, StandardOpenOption.READ)
      }
    })

  override fun doGet(req: HttpServletRequest, resp: HttpServletResponse) {
    val pathSegments = parsePath(req.pathInfo ?: "/")
    val session = Session(pathSegments.first())
    val sessionDir =
      dataStorage.getSessionDir(ApplicationServices.authenticationManager.getUser(req.getCookie()), session)
    val file = File(sessionDir, pathSegments.drop(1).joinToString("/"))
    when {
      file.isFile -> {
        val channel = channelCache.get(file)
        val mappedByteBuffer: MappedByteBuffer = channel.map(FileChannel.MapMode.READ_ONLY, 0, channel.size())
        resp.contentType = ApplicationServer.getMimeType(file.name)
        resp.status = HttpServletResponse.SC_OK
        val async = req.startAsync()
        resp.outputStream.apply {
          setWriteListener(object : WriteListener {
            val buffer = ByteArray(256 * 1024)
            override fun onWritePossible() {
              while (isReady) {
                val start = mappedByteBuffer.position()
                val attemptedReadSize = buffer.size.coerceAtMost(mappedByteBuffer.remaining())
                mappedByteBuffer.get(buffer, 0, attemptedReadSize)
                val end = mappedByteBuffer.position()
                val readBytes = end - start
                if (readBytes == 0) {
                  async.complete()
                  return
                }
                write(buffer, 0, readBytes)
              }
            }

            override fun onError(throwable: Throwable) {
              log.warn("Error writing file", throwable)
            }
          })
        }
      }
      req.pathInfo?.endsWith("/") == false -> {
        resp.sendRedirect(req.requestURI + "/")
      }
      else -> {
        resp.contentType = "text/html"
        resp.status = HttpServletResponse.SC_OK
        val files = file.listFiles()
          ?.filter { it.isFile }
//          ?.filter { !it.name.startsWith(".") }
          ?.sortedBy { it.name }
          ?.joinToString("<br/>\n") {
            """<a class="file-item" href="${it.name}">${it.name}</a>"""
          } ?: ""
        val folders = file.listFiles()
          ?.filter { !it.isFile }
//          ?.filter { !it.name.startsWith(".") }
          ?.sortedBy { it.name }
          ?.joinToString("<br/>\n") {
            """<a class="folder-item" href="${it.name}/">${it.name}</a>"""
          } ?: ""
        resp.writer.write(directoryHTML(req, session, pathSegments.drop(1).joinToString("/"), folders, files))
      }
    }
  }

  private fun directoryHTML(
    req: HttpServletRequest,
    session: Session,
    filePath: String,
    folders: String,
    files: String
  ) = """
                        |<html>
                        |<head>
                        |<title>Files</title>
                        |<style>
                        |    body {
                        |        font-family: 'Arial', sans-serif;
                        |        background-color: #f4f4f4;
                        |        color: #333;
                        |        margin: 0;
                        |        padding: 20px;
                        |    }
                        |
                        |    .archive-title, .folders-title, .files-title {
                        |        font-size: 24px;
                        |        font-weight: bold;
                        |        margin-top: 0;
                        |    }
                        |
                        |    .zip-link {
                        |        color: #0056b3;
                        |        text-decoration: none;
                        |        font-size: 16px;
                        |        background-color: #e7f3ff;
                        |        padding: 10px 15px;
                        |        border-radius: 5px;
                        |        display: inline-block;
                        |        margin-top: 10px;
                        |    }
                        |
                        |    .zip-link:hover {
                        |        background-color: #d1e7ff;
                        |    }
                        |
                        |    .folders-container, .files-container {
                        |        background-color: white;
                        |        border: 1px solid #ddd;
                        |        padding: 15px;
                        |        border-radius: 5px;
                        |        margin-top: 20px;
                        |    }
                        |
                        |    .folder-item, .file-item {
                        |        color: #0056b3;
                        |        text-decoration: none;
                        |        display: block;
                        |        margin-bottom: 10px;
                        |        padding: 5px 0;
                        |    }
                        |
                        |    .folder-item:hover, .file-item:hover {
                        |        text-decoration: underline;
                        |    }
                        |
                        |    h1 {
                        |        border-bottom: 2px solid #ddd;
                        |        padding-bottom: 10px;
                        |    }
                        |</style>
                        |</head>
                        |<body>
                        |<h1 class="archive-title">Archive</h1>
                        |<a href="${req.contextPath}/fileZip?session=$session&path=$filePath" class="zip-link">ZIP</a>
                        |<h1 class="folders-title">Folders</h1>
                        |<div class="folders-container">
                        |$folders
                        |</div>
                        |<h1 class="files-title">Files</h1>
                        |<div class="files-container">
                        |$files
                        |</div>
                        |</body>
                        |</html>
                        """.trimMargin()

  companion object {
    val log = LoggerFactory.getLogger(FileServlet::class.java)
    fun parsePath(path: String): List<String> {
      val pathSegments = path.split("/").filter { it.isNotBlank() }
      pathSegments.forEach {
        when {
          it == ".." -> throw IllegalArgumentException("Invalid path")
          it.any {
            when {
              it == ':' -> true
              it == '/' -> true
              it == '~' -> true
              it == '\\' -> true
              it.code < 32 -> true
              it.code > 126 -> true
              else -> false
            }
          } -> throw IllegalArgumentException("Invalid path")
        }
      }
      return pathSegments
    }
  }

}