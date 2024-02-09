package com.simiacryptus.skyenet.webui.application

import com.simiacryptus.skyenet.interpreter.Interpreter
import jakarta.servlet.http.HttpServlet
import jakarta.servlet.http.HttpServletRequest
import jakarta.servlet.http.HttpServletResponse
import java.util.*

class ToolServlet(val app: ApplicationDirectory) : HttpServlet() {


  data class Tool(
    val path: String,
    val openApiDescription: String,
    val code: String,
    val interpreterClass: Class<out Interpreter>,
    val symbols: Map<String, Any> = mapOf(),
  )
  override fun doGet(req: HttpServletRequest?, resp: HttpServletResponse?) {
    resp?.contentType = "text/html"
    resp?.writer?.write(
      """
        <html>
        <head>
          <title>Tools</title>
          <link href="https://fonts.googleapis.com/css?family=Roboto:400,700&display=swap" rel="stylesheet">
          <style>
            body { font-family: 'Roboto', Arial, sans-serif; background-color: #f4f4f4; color: #333; margin: 0; padding: 20px; }
            h1 { color: #5a5a5a; }
            .tools { max-width: 960px; margin: auto; }
            .tool { background-color: #fff; border: 1px solid #e1e1e1; border-radius: 4px; padding: 20px; margin-bottom: 20px; }
            .tool-path { font-weight: 700; color: #2a2a2a; }
            .json-display, .code-display { background-color: #fafafa; border: 1px solid #e1e1e1; border-radius: 4px; padding: 10px; white-space: pre-wrap; overflow: auto; }
            .apikey { background-color: #eaeaea; padding: 10px; border-radius: 4px; margin-top: 20px; }
          </style>
        </head>
        <body>
          <h1>Tools</h1>
          <div class='tools'>
            ${
        tools.joinToString("") { tool ->
          """
            <div class='tool'>
              <div class='tool-path'>${tool.path}</div>
              <div class='json-display'>${tool.openApiDescription}</div>
              <div class='code-display'>${tool.code}</div>
            </div>
        """.trimIndent()
        }
      }
          </div>
          <div class='apikey'>API Key: $apiKey</div>
        </body>
        </html>
      """.trimIndent()
    )
    resp?.writer?.close()

  }

  override fun doPost(req: HttpServletRequest?, resp: HttpServletResponse?) {
    val path = (req?.servletPath ?: "").removePrefix("/")
    if (apiKey != req?.getHeader("Authorization")?.removePrefix("Bearer ")) {
      resp?.sendError(403)
      return
    }
    tools.find { it.path == path }?.apply {
      interpreterClass.getConstructor(Map::class.java).newInstance(symbols + mapOf(
        "req" to req,
        "resp" to resp
      )).run(code)
    } ?: run {
      resp?.sendError(404)
    }
    super.doPost(req, resp)
  }

  companion object {
    val tools = mutableListOf<Tool>()
    val apiKey = UUID.randomUUID().toString()
  }
}