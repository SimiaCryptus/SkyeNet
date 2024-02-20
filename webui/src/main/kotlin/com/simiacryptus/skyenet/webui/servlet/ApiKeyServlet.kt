package com.simiacryptus.skyenet.webui.servlet

import com.simiacryptus.jopenai.ApiModel
import com.simiacryptus.jopenai.models.OpenAIModel
import com.simiacryptus.jopenai.util.JsonUtil
import com.simiacryptus.skyenet.core.platform.ApplicationServices
import com.simiacryptus.skyenet.webui.application.ApplicationServer.Companion.getCookie
import jakarta.servlet.http.HttpServlet
import jakarta.servlet.http.HttpServletRequest
import jakarta.servlet.http.HttpServletResponse
import java.io.File
import java.net.URLEncoder
import java.util.*
import kotlin.reflect.jvm.javaType
import kotlin.reflect.typeOf

class ApiKeyServlet : HttpServlet() {

  data class ApiKeyRecord(
    val owner: String,
    val apiKey: String,
    val mappedKey: String,
    val budget: Double,
    val comment: String
  )

  override fun doGet(req: HttpServletRequest, resp: HttpServletResponse) {
    resp.contentType = "text/html"
    val user = ApplicationServices.authenticationManager.getUser(req.getCookie()) ?: return resp.sendError(
      HttpServletResponse.SC_UNAUTHORIZED
    )
    val action = req.getParameter("action")
    val apiKey = req.getParameter("apiKey")

    when (action?.toLowerCase(Locale.ROOT)) {
      "edit" -> {
        val record = apiKeyRecords.find { it.apiKey == apiKey && it.owner == user.email }
        if (record != null) {
          serveEditPage(resp, record)
        } else {
          resp.writer.write("API Key record not found")
        }
      }

      "delete" -> { // Fix the null safety check consistency
        val record = apiKeyRecords.find { it.apiKey == apiKey && it.owner == user?.email }
        if (record != null) {
          apiKeyRecords.remove(record)
          saveRecords()
          resp.writer.write("API Key record deleted")
        } else {
          resp.writer.write("API Key record not found")
        }
      }

      "create" -> {
        // Reuse the serveEditPage function but with an empty record for creation
        serveEditPage(
          resp,
          ApiKeyRecord(
            user.email,
            UUID.randomUUID().toString(),
            ApplicationServices.userSettingsManager.getUserSettings(user).apiKey,
            0.0,
            ""
          )
        )
      }

      else -> {
        resp.writer.write(indexPage(req))
      }
    }

  }

  override fun doPost(req: HttpServletRequest, resp: HttpServletResponse) {
    val apiKey = req.getParameter("apiKey")
    val mappedKey = req.getParameter("mappedKey")
    val budget = req.getParameter("budget").toDoubleOrNull()
    val comment = req.getParameter("comment")
    val user = ApplicationServices.authenticationManager.getUser(req.getCookie())
    val record = apiKeyRecords.find { it.apiKey == apiKey && it.owner == user?.email }

    if (record != null && budget != null && user != null) { // Ensure user is not null before proceeding
      apiKeyRecords.remove(record)
      apiKeyRecords.add(
        record.copy(
          mappedKey = mappedKey ?: record.mappedKey,
          budget = budget,
          comment = comment ?: ""
        )
      )
      saveRecords()
      resp.sendRedirect("?action=edit&apiKey=$apiKey&editSuccess=true")
    } else if (apiKey != null && budget != null) {
      // Create a new record if apiKey is not found
      val newRecord = ApiKeyRecord(user?.email ?: "", apiKey, mappedKey ?: "", budget, comment ?: "")
      apiKeyRecords.add(newRecord)
      saveRecords()
      resp.sendRedirect(
        "?action=edit&apiKey=${
          URLEncoder.encode(
            apiKey,
            "UTF-8"
          )
        }&creationSuccess=true"
      ) // Encode apiKey to prevent URL manipulation
    } else {
      resp.sendError(HttpServletResponse.SC_BAD_REQUEST, "Invalid input")
    }
  }

  private fun indexPage(req: HttpServletRequest): String {
    val user = ApplicationServices.authenticationManager.getUser(req.getCookie()) ?: return ""
    return """
          <html>
          <head>
              <title>API Key Records</title>
              <style>
                  body { font-family: Arial, sans-serif; margin: 20px; }
                  .records { margin-bottom: 20px; }
                  .record { margin: 10px 0; }
                  a { text-decoration: none; color: #007BFF; }
              </style>
          </head>
          <body>
              <h1>API Key Records</h1>
              <div class='records'>
                  ${
      apiKeyRecords.filter { it.owner == user.email }.joinToString("\n") { record ->
        "<div class='record'><a href='?action=edit&apiKey=${record.apiKey}'>${record.apiKey}</a></div>"
      }
    }
              </div>
              <a href="?action=create">Create New API Key Record</a>
          </body>
          </html>
      """.trimIndent()
  }

  private fun serveEditPage(resp: HttpServletResponse, record: ApiKeyRecord) {
    val usageSummary = ApplicationServices.usageManager.getUserUsageSummary(record.apiKey)
    //language=HTML
    resp.writer.write(
      """
      <html>
      <head>
          <title>Edit API Key Record: ${record.apiKey}</title>
          <style>
              body {
                  font-family: Arial, sans-serif;
                  margin: 20px;
              }
      
              form > label {
                  display: block;
                  margin-top: 10px;
              }
      
              form > input[type="text"], textarea {
                  margin-bottom: 10px;
                  display: block;
                  width: 100%;
                  box-sizing: border-box;
              }
      
              form > input[type="text"]#mappedKey {
                  width: 50%;
              }
      
              textarea {
                  height: 100px;
              }
      
              form > input[type="submit"] {
                  margin-top: 10px;
              }
      
              form {
                  max-width: 600px;
              }
      
              h2 {
                  margin-top: 20px;
              }
      
              div {
                  margin-bottom: 10px;
              }
          </style>
      </head>
      <body>
      <h1>Edit API Key Record: ${record.apiKey}</h1>
      <form action="edit" method="post">
          <input type="hidden" name="apiKey" value="${record.apiKey}">
          <label for="mappedKey">Mapped Key:</label>
          <input type="text" id="mappedKey" name="mappedKey" value="${record.mappedKey}" style="width: 100%;">
          <label for="budget">Budget:</label>
          <input type="text" id="budget" name="budget" value="${record.budget}">
          <label for="comment">Description:</label>
          <textarea id="comment" name="comment">${record.comment}</textarea>
          <input type="submit" value="Submit">
      </form>
      <!-- Usage Summary -->
      <h2>Usage Summary</h2>
      ${
        usageSummary.entries.joinToString { (model: OpenAIModel, usage: ApiModel.Usage) ->
          """
          <div>
            <h3>${model.modelName}</h3>
            <p>total_tokens: ${usage.total_tokens}</p>
            <p>Cost: ${usage.cost}</p>
          </div>
          """
        }
      }
      </body>
      </html>
        """.trimIndent()
    )
  }

  companion object {
    private val userRoot by lazy {
      File(
        File(ApplicationServices.dataStorageRoot, ".skyenet"),
        "apiKeys"
      ).apply { mkdirs() }
    }

    val apiKeyRecords by lazy {
      val file = File(userRoot, "apiKeys.json")
      if (file.exists()) try {
        return@lazy JsonUtil.fromJson(file.readText(), typeOf<List<ApiKeyRecord>>().javaType)
      } catch (e: Throwable) {
        e.printStackTrace()
      }
      mutableListOf<ApiKeyRecord>()
    }

    private fun saveRecords() {
      File(userRoot, "apiKeys.json").writeText(JsonUtil.toJson(apiKeyRecords))
    }
  }
}