package com.simiacryptus.skyenet.webui.servlet


import com.simiacryptus.jopenai.util.JsonUtil
import com.simiacryptus.skyenet.apps.coding.ToolAgent
import com.simiacryptus.skyenet.core.platform.ApplicationServices
import com.simiacryptus.skyenet.core.platform.ApplicationServices.authenticationManager
import com.simiacryptus.skyenet.core.platform.ApplicationServices.authorizationManager
import com.simiacryptus.skyenet.core.platform.AuthorizationInterface.OperationType
import com.simiacryptus.skyenet.core.platform.User
import com.simiacryptus.skyenet.interpreter.Interpreter
import com.simiacryptus.skyenet.kotlin.KotlinInterpreter
import com.simiacryptus.skyenet.webui.application.ApplicationDirectory
import com.simiacryptus.skyenet.webui.application.ApplicationServer.Companion.getCookie
import com.simiacryptus.skyenet.webui.util.MarkdownUtil
import com.simiacryptus.skyenet.webui.util.OpenAPI
import jakarta.servlet.http.HttpServlet
import jakarta.servlet.http.HttpServletRequest
import jakarta.servlet.http.HttpServletResponse
import org.eclipse.jetty.webapp.WebAppClassLoader
import org.intellij.lang.annotations.Language
import java.io.File
import java.util.*
import kotlin.reflect.jvm.javaType
import kotlin.reflect.typeOf

abstract class ToolServlet(val app: ApplicationDirectory) : HttpServlet() {

    data class Tool(
        val path: String,
        val openApiDescription: OpenAPI,
        val interpreterString: String,
        val servletCode: String,
    )

    @Language("HTML")
    private fun indexPage() = """
          <html>
          <head>
              <title>Tools</title>
              $header
          </head>
          <body>
          
          <script src="https://cdnjs.cloudflare.com/ajax/libs/prism/1.29.0/prism.min.js"></script>
          <script src="https://cdnjs.cloudflare.com/ajax/libs/prism/1.29.0/plugins/autoloader/prism-autoloader.min.js"></script>
          <script src="https://cdnjs.cloudflare.com/ajax/libs/prism/1.29.0/plugins/toolbar/prism-toolbar.min.js"></script>
          <script src="https://cdnjs.cloudflare.com/ajax/libs/prism/1.29.0/plugins/copy-to-clipboard/prism-copy-to-clipboard.min.js"></script>
          <script src="https://cdnjs.cloudflare.com/ajax/libs/prism/1.29.0/plugins/line-numbers/prism-line-numbers.min.js"></script>
          <script src="https://cdnjs.cloudflare.com/ajax/libs/prism/1.29.0/plugins/match-braces/prism-match-braces.min.js"></script>
          <script src="https://cdnjs.cloudflare.com/ajax/libs/prism/1.29.0/plugins/show-language/prism-show-language.min.js"></script>
          <script type="module">
              import mermaid from 'https://cdn.jsdelivr.net/npm/mermaid@10/dist/mermaid.esm.min.mjs';
          
              mermaid.initialize({startOnLoad: true});
              window.mermaid = mermaid; // Make mermaid globally accessible
          </script>
          <h1>Tools</h1>
          <form action="?import=true" method="post" enctype="multipart/form-data">
              <input type="file" name="file" accept=".json">
              <input type="submit" value="Import Tools">
          </form>
          <div class='tools'>
              ${tools.joinToString("\n") { tool -> "<div class='tool'><a href='?path=${tool.path}'>${tool.path}</a></div>" }}
          </div>
          </body>
          </html>
        """.trimIndent()

    @Language("HTML")
    private fun toolDetailsPage(tool: Tool) = """
          <html>
          <head>
              <title>Tool Details</title>
              $header
          </head>
          <body>
          
          <script src="https://cdnjs.cloudflare.com/ajax/libs/prism/1.29.0/prism.min.js"></script>
          <script src="https://cdnjs.cloudflare.com/ajax/libs/prism/1.29.0/plugins/autoloader/prism-autoloader.min.js"></script>
          <script src="https://cdnjs.cloudflare.com/ajax/libs/prism/1.29.0/plugins/toolbar/prism-toolbar.min.js"></script>
          <script src="https://cdnjs.cloudflare.com/ajax/libs/prism/1.29.0/plugins/copy-to-clipboard/prism-copy-to-clipboard.min.js"></script>
          <script src="https://cdnjs.cloudflare.com/ajax/libs/prism/1.29.0/plugins/line-numbers/prism-line-numbers.min.js"></script>
          <script src="https://cdnjs.cloudflare.com/ajax/libs/prism/1.29.0/plugins/match-braces/prism-match-braces.min.js"></script>
          <script src="https://cdnjs.cloudflare.com/ajax/libs/prism/1.29.0/plugins/show-language/prism-show-language.min.js"></script>
          <script type="module">
              import mermaid from 'https://cdn.jsdelivr.net/npm/mermaid@10/dist/mermaid.esm.min.mjs';
          
              mermaid.initialize({startOnLoad: true});
              window.mermaid = mermaid; // Make mermaid globally accessible
          </script>
          <h1>Tool Details: ${tool.path}</h1>
          <div class="tabs-container">
              <div class="tabs">
                  <button class="tab-button" data-for-tab="1">Details</button>
                  <button class="tab-button" data-for-tab="2">Code</button>
                  <button class="tab-button" data-for-tab="3">API</button>
              </div>
              <div class="tabs-content">
                  <div class="tab-content active" data-tab="1">
                      <!-- Edit -->
                      <a href='?path=${tool.path}&edit=true'>Edit</a>
                      <!-- Delete -->
                      <a href='?path=${tool.path}&delete=true'>Delete</a>
                      <div class='tool-details'>
                          <div>Path:
                              <div class='tool-path'>${tool.path}</div>
                          </div>
                          <div>Interpreter:
                              <div class='interpreter-display'>${tool.interpreterString}</div>
                          </div>
                      </div>
                  </div>
                  <div class="tab-content" data-tab="2">
                      <div>Code:
                          <div class='code-display'>${MarkdownUtil.renderMarkdown("```kotlin\n${tool.servletCode}\n```")}</div>
                      </div>
                  </div>
                  <div class="tab-content" data-tab="3">
                      <div>API Description:
                          <div class='json-display'>${JsonUtil.toJson(tool.openApiDescription)}</div>
                      </div>
                  </div>
              </div>
          </body>
          </html>
        """.trimIndent()

    private val header
        @Language("HTML")
        get() = """
      <meta charset="UTF-8">
      <meta name='viewport' content='width=device-width,initial-scale=1'>
      <link rel="icon" type="image/svg+xml" href="/favicon.svg"/>
      <link href="https://cdnjs.cloudflare.com/ajax/libs/prism-themes/1.9.0/prism-ghcolors.min.css" rel="stylesheet"/>
      <link href="https://cdnjs.cloudflare.com/ajax/libs/prism/1.29.0/plugins/toolbar/prism-toolbar.min.css"
            rel="stylesheet"/>
      <link href="https://cdnjs.cloudflare.com/ajax/libs/prism/1.29.0/plugins/line-numbers/prism-line-numbers.min.css"
            rel="stylesheet"/>
      <link href="https://cdnjs.cloudflare.com/ajax/libs/prism/1.29.0/plugins/match-braces/prism-match-braces.min.css"
            rel="stylesheet"/>
      <style>
          body {
              font-family: 'Roboto', Arial, sans-serif;
              background-color: #f4f4f4;
              color: #333;
              margin: 0;
              padding: 20px;
          }
      
          h1 {
              color: #5a5a5a;
          }
      
          .tools, .tool-details {
              max-width: 960px;
              margin: auto;
          }
      
          .tool, .edit-form {
              background-color: #fff;
              border: 1px solid #e1e1e1;
              border-radius: 4px;
              padding: 20px;
              margin-bottom: 20px;
          }
      
          .tool-path, .edit-form input[type="text"], .edit-form textarea {
              font-weight: 700;
              color: #2a2a2a;
          }
      
          .json-display, .code-display, .edit-form textarea {
              background-color: #fafafa;
              border: 1px solid #e1e1e1;
              border-radius: 4px;
              padding: 10px;
              white-space: pre-wrap;
              overflow: auto;
          }
      
          .edit-form label {
              display: block;
              margin-top: 20px;
          }
      
          .edit-form input[type="text"], .edit-form textarea {
              width: 100%;
              box-sizing: border-box;
          }
      
          .edit-form input[type="submit"] {
              margin-top: 20px;
              background-color: #4CAF50;
              color: white;
              padding: 10px 24px;
              border: none;
              border-radius: 4px;
              cursor: pointer;
          }
      
          .edit-form input[type="submit"]:hover {
              background-color: #45a049;
          }
      </style>
      <link id="theme_style" href="/main.css" rel="stylesheet"/>
      <script src="/main.js"></script>
      """.trimIndent()

    private fun serveEditPage(req: HttpServletRequest, resp: HttpServletResponse, tool: Tool) {
        resp.contentType = "text/html"
        val formHtml = """
        <html>
        <head>
            <title>Edit Tool: ${tool.path}</title>
            $header
        </head>
        <body>
            <h1>Edit Tool: ${tool.path}</h1>
            <form class="edit-form" action="edit" method="post">
                <input type="hidden" name="path" value="${tool.path}">
                <label for="newpath">New Path:</label>
                <input type="text" id="newpath" name="newpath" value="${tool.path}">
                <label for="interpreterString">Interpreter String:</label>
                <input type="text" id="interpreterString" name="interpreterString" value="${tool.interpreterString}">
                <label for="servletCode">Servlet Code:</label>
                <textarea id="servletCode" name="servletCode" rows="20">${tool.servletCode}</textarea>
                <label for="openApiDescription">OpenAPI Description:</label>
                <textarea id="openApiDescription" name="openApiDescription" rows="20">${JsonUtil.toJson(tool.openApiDescription)}</textarea>
                <input type="submit" value="Submit">
            </form>
        </body>
        </html>
    """.trimIndent()
        resp.writer.write(formHtml)
        resp.writer.close()
    }

    override fun doGet(req: HttpServletRequest?, resp: HttpServletResponse?) {

        val user = authenticationManager.getUser(req?.getCookie())
        if (!authorizationManager.isAuthorized(ToolServlet.javaClass, user, OperationType.Admin)) {
            resp?.sendError(403)
            return
        }

        resp?.contentType = "text/html"

        val path = req?.getParameter("path")
        if (req?.getParameter("edit") != null) {
            val tool = tools.find { it.path == path }
            if (tool != null) {
                serveEditPage(req, resp!!, tool)
            } else {
                resp!!.writer.write("Tool not found")
            }
            return
        }

        if (req?.getParameter("delete") != null) {
            val tool = tools.find { it.path == path }
            if (tool != null) {
                tools.remove(tool)
                File(userRoot, "tools.json").writeText(JsonUtil.toJson(tools))
                resp!!.sendRedirect("?")
            } else {
                resp!!.writer.write("Tool not found")
            }
            return
        }

        if (req?.getParameter("export") != null) {
            resp?.contentType = "application/json"
            resp?.addHeader("Content-Disposition", "attachment; filename=\"tools.json\"")
            resp?.writer?.write(JsonUtil.toJson(tools))
            return
        }

        if (path != null) {
            // Display details for a single tool
            val tool = tools.find { it.path == path }
            if (tool != null) {
                resp?.writer?.write(toolDetailsPage(tool))
            } else {
                resp?.writer?.write("Tool not found")
            }
        } else {
            // Display index page
            resp?.writer?.write(indexPage())
        }
        resp?.writer?.close()
    }

    override fun doPost(req: HttpServletRequest?, resp: HttpServletResponse?) {
        req ?: return
        resp ?: return

        val path = req.getParameter("path")
        val tool = tools.find { it.path == path }
        if (tool != null) {
            tools.remove(tool)
            val newpath = req.getParameter("newpath") ?: req.getParameter("path")
            tools.add(
                tool.copy(
                    path = newpath,
                    interpreterString = req.getParameter("interpreterString"),
                    servletCode = req.getParameter("servletCode"),
                    openApiDescription = JsonUtil.fromJson(req.getParameter("openApiDescription"), OpenAPI::class.java)
                )
            )
            File(userRoot, "tools.json").writeText(JsonUtil.toJson(tools))
            resp.sendRedirect("?path=$newpath&editSuccess=true") // Redirect to the tool's detail page or an edit success page
        } else {
            if (req.getParameter("import") != null) {
                val inputStream = req.getPart("file")?.inputStream
                val toolsJson = inputStream?.bufferedReader().use { it?.readText() }
                if (toolsJson != null) {
                    val importedTools: List<Tool> = JsonUtil.fromJson(toolsJson, typeOf<List<Tool>>().javaType)
                    tools.clear()
                    tools.addAll(importedTools)
                    File(userRoot, "tools.json").writeText(JsonUtil.toJson(tools))
                    resp.sendRedirect("?importSuccess=true")
                } else {
                    resp.sendError(HttpServletResponse.SC_BAD_REQUEST, "Invalid file or format")
                }
                return
            }
            resp.sendError(HttpServletResponse.SC_NOT_FOUND, "Tool not found")
        }
    }

    companion object {
        private val userRoot by lazy {
            File(
                File(ApplicationServices.dataStorageRoot, ".skyenet"),
                "tools"
            ).apply { mkdirs() }
        }

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
        val instanceCache = mutableMapOf<Tool, HttpServlet>()

    }

    override fun service(req: HttpServletRequest?, resp: HttpServletResponse?) {
        req ?: return
        resp ?: return
        val path = req.servletPath ?: "/"
        val tool = tools.find { it.path == path }
        if (tool != null) {
            // TODO: Isolate tools per user
            val user = authenticationManager.getUser(req.getCookie())
            val isAdmin = authorizationManager.isAuthorized(
                ToolServlet.javaClass, user, OperationType.Admin
            )
            val isHeaderAuth = apiKey == req.getHeader("Authorization")?.removePrefix("Bearer ")
            if (!isAdmin && !isHeaderAuth) {
                resp.sendError(403)
            } else {
                try {
                    val servlet = instanceCache.computeIfAbsent(tool) { construct(user!!, tool) }
                    servlet.service(req, resp)
                } catch (e: RuntimeException) {
                    throw e
                } catch (e: Throwable) {
                    throw RuntimeException(e)
                }
            }
        } else {
            super.service(req, resp)
        }
    }

    private fun construct(user: User, tool: Tool): HttpServlet {
        val returnBuffer = ToolAgent.ServletBuffer()
        val classLoader = Thread.currentThread().contextClassLoader
        val prevCL = KotlinInterpreter.classLoader
        KotlinInterpreter.classLoader = classLoader //req.javaClass.classLoader
        try {
            WebAppClassLoader.runWithServerClassAccess<Any?> {
                require(null != classLoader.loadClass("org.eclipse.jetty.server.Response"))
                require(null != classLoader.loadClass("org.eclipse.jetty.server.Request"))
                this.fromString(user, tool.interpreterString).let { (interpreterClass, symbols) ->
                    val effectiveSymbols = (symbols + mapOf(
                        "returnBuffer" to returnBuffer,
                        "json" to JsonUtil,
                    )).filterKeys { !it.isNullOrBlank() }
                    interpreterClass.getConstructor(Map::class.java).newInstance(effectiveSymbols).run(tool.servletCode)
                }
            }
        } finally {
            KotlinInterpreter.classLoader = prevCL
        }

        val first = returnBuffer.first()
        return first
    }

    abstract fun fromString(user: User, str: String): InterpreterAndTools

}

data class InterpreterAndTools(
    val interpreterClass: Class<out Interpreter>,
    val symbols: Map<String, Any> = mapOf(),
)


