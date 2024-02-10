package com.simiacryptus.skyenet.webui.servlet

import com.simiacryptus.jopenai.util.JsonUtil
import com.simiacryptus.skyenet.apps.coding.ToolAgent
import com.simiacryptus.skyenet.interpreter.Interpreter
import com.simiacryptus.skyenet.kotlin.KotlinInterpreter
import com.simiacryptus.skyenet.webui.application.ApplicationDirectory
import com.simiacryptus.skyenet.webui.util.MarkdownUtil
import com.simiacryptus.skyenet.webui.util.OpenApi
import jakarta.servlet.http.HttpServlet
import jakarta.servlet.http.HttpServletRequest
import jakarta.servlet.http.HttpServletResponse
import org.eclipse.jetty.webapp.WebAppClassLoader
import java.io.File
import java.util.*
import kotlin.reflect.javaType
import kotlin.reflect.typeOf

abstract class ToolServlet(val app: ApplicationDirectory) : HttpServlet() {

  override fun service(req: HttpServletRequest?, resp: HttpServletResponse?) {
    req ?: return
    resp ?: return
    val path = req.servletPath ?: "/"
    val tool = tools.find { it.path == path }
    if (tool != null) {
      val methodInfo = when (req.method) {
        "GET" -> tool.parsedServlet.onGet
        "POST" -> tool.parsedServlet.onPost
        "PUT" -> tool.parsedServlet.onPut
        "DELETE" -> tool.parsedServlet.onDelete
        else -> null
      } ?: throw IllegalArgumentException("Method not supported")

      // TODO: Check for user cookie or match this key
      // TODO: Isolate tools per user
//    if (apiKey != req?.getHeader("Authorization")?.removePrefix("Bearer ")) {
//      resp?.sendError(403)
//      return
//    }

      serve(tool, methodInfo, req, resp)
    } else {
      super.service(req, resp)
    }
  }

  private fun serve(
    tool: Tool,
    methodInfo: ToolAgent.HandlerInfo,
    req: HttpServletRequest,
    resp: HttpServletResponse
  ) {
    val classLoader = Thread.currentThread().contextClassLoader
    KotlinInterpreter.classLoader = classLoader //req.javaClass.classLoader
    WebAppClassLoader.runWithServerClassAccess {
      require(null != classLoader.loadClass("org.eclipse.jetty.server.Response"))
      require(null != classLoader.loadClass("org.eclipse.jetty.server.Request"))
      fromString(tool.interpreterString).let { (interpreterClass, symbols) ->
        val effectiveSymbols = (symbols + mapOf(
          methodInfo.requestName to req,
          methodInfo.responseName to resp,
          "json" to JsonUtil,
        )).filterKeys { !it.isNullOrBlank() }
        val code = (
              (tool.parsedServlet.imports ?: emptyList()) +
              (tool.parsedServlet.dataClasses?.map { it.code } ?: emptyList()) +
              listOf(methodInfo.methodBody!!)
            ).joinToString("\n")
        val interpreter = interpreterClass.getConstructor(Map::class.java).newInstance(effectiveSymbols)
        interpreter.run(code)
      }
    }
  }

  override fun doGet(req: HttpServletRequest?, resp: HttpServletResponse?) {
    resp?.contentType = "text/html"
    resp?.writer?.write(index())
    resp?.writer?.close()
  }

  private fun index() = """
          <html>
          <head>
            <title>Tools</title>
            <meta charset="UTF-8">
            <meta name='viewport' content='width=device-width,initial-scale=1'>
            <link rel="icon" type="image/svg+xml" href="/favicon.svg"/>
            <link href="https://cdnjs.cloudflare.com/ajax/libs/prism-themes/1.9.0/prism-ghcolors.min.css" rel="stylesheet"/>
            <link href="https://cdnjs.cloudflare.com/ajax/libs/prism/1.29.0/plugins/toolbar/prism-toolbar.min.css" rel="stylesheet"/>
            <link href="https://cdnjs.cloudflare.com/ajax/libs/prism/1.29.0/plugins/line-numbers/prism-line-numbers.min.css" rel="stylesheet"/>
            <link href="https://cdnjs.cloudflare.com/ajax/libs/prism/1.29.0/plugins/match-braces/prism-match-braces.min.css" rel="stylesheet"/>
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
                <div class='interpreter-string'>${tool.interpreterString}</div>
                <div class='json-display'>${MarkdownUtil.renderMarkdown("```json\n${JsonUtil.toJson(tool.openApiDescription)}\n```")}</div>
                <div class='code-display'>${
        MarkdownUtil.renderMarkdown(
          "```kotlin\n${
            tool.parsedServlet.imports?.joinToString(
              "\n"
            )
          }\n```"
        )
      }</div>
                <div class='code-display'>${
        MarkdownUtil.renderMarkdown(
          "```kotlin\n${
            tool.parsedServlet.dataClasses?.joinToString(
              "\n"
            )
          }\n```"
        )
      }</div>
              </div>
          """.trimIndent()
    }
  }
            </div>
            <div class='apikey'>API Key: $apiKey</div>
          </body>
          </html>
        """.trimIndent()


  abstract fun fromString(str: String): InterpreterAndTools

  companion object {
    private val userRoot by lazy { File(File(".skyenet"), "tools").apply { mkdirs() } }

    @OptIn(ExperimentalStdlibApi::class)
    val tools by lazy {
      val file = File(userRoot, "tools.json")
      if (file.exists()) try {
        return@lazy JsonUtil.fromJson(file.readText(), typeOf<List<Tool>>().javaType)
      } catch (e: Throwable) {
        e.printStackTrace()
      }
      mutableListOf<Tool>()
    }

    fun addTool(element: Tool) {
      tools += element
      File(userRoot, "tools.json").writeText(JsonUtil.toJson(tools))
    }

    val apiKey = UUID.randomUUID().toString()
  }
}

data class InterpreterAndTools(
  val interpreterClass: Class<out Interpreter>,
  val symbols: Map<String, Any> = mapOf(),
)

data class Tool(
  val path: String,
  val openApiDescription: OpenApi,
  val interpreterString: String,
  val parsedServlet: ToolAgent.ServletInfo,
)
