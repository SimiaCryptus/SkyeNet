package com.simiacryptus.diff

 import com.simiacryptus.skyenet.webui.application.ApplicationInterface
 import com.simiacryptus.skyenet.webui.session.SocketManagerBase
 import com.simiacryptus.skyenet.webui.util.MarkdownUtil
 import java.util.*
import java.io.BufferedReader
import java.io.InputStreamReader

 fun SocketManagerBase.addShellExecutionLinks(
     response: String,
    ui: ApplicationInterface
): String {
    val shellCodePattern = """(?s)(?<![^\n])```shell\n(.*?)\n```""".toRegex()
    return shellCodePattern.replace(response) { matchResult ->
        val shellCode = matchResult.groupValues[1]
        val executionId = UUID.randomUUID().toString()
         val executionTask = ui.newTask(false)
         val executeButton = hrefLink("Execute", classname = "href-link cmd-button") {
             try {
                val process = Runtime.getRuntime().exec(arrayOf("sh", "-c", shellCode))
                val reader = BufferedReader(InputStreamReader(process.inputStream))
                val errorReader = BufferedReader(InputStreamReader(process.errorStream))
                val output = StringBuilder()
                var line: String?
                while (reader.readLine().also { line = it } != null) {
                    output.append(line).append("\n")
                }
                while (errorReader.readLine().also { line = it } != null) {
                    output.append("Error: ").append(line).append("\n")
                }
                val exitCode = process.waitFor()
                output.append("Exit code: $exitCode")
                executionTask.complete(MarkdownUtil.renderMarkdown("```\n$output\n```", ui = ui))
             } catch (e: Throwable) {
                 executionTask.error(null, e)
             }
        }
        """
        ```shell
        $shellCode
        ```
        <div id="execution-$executionId">
            $executeButton
            ${executionTask.placeholder}
        </div>
        """.trimIndent()
    }
}