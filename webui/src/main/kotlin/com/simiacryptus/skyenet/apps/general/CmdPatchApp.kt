package com.simiacryptus.skyenet.apps.general


import com.simiacryptus.diff.FileValidationUtils
import com.simiacryptus.jopenai.OpenAIClient
import com.simiacryptus.jopenai.models.OpenAITextModel
import com.simiacryptus.skyenet.core.platform.Session
import com.simiacryptus.skyenet.set
import com.simiacryptus.skyenet.webui.session.SessionTask
import org.slf4j.LoggerFactory
import java.io.File
import java.nio.file.Path
import java.util.concurrent.TimeUnit

class CmdPatchApp(
    root: Path,
    session: Session,
    settings: Settings,
    api: OpenAIClient,
    val virtualFiles: Array<out File>?,
    model: OpenAITextModel
) : PatchApp(root.toFile(), session, settings, api, model) {
    companion object {
        private val log = LoggerFactory.getLogger(CmdPatchApp::class.java)

        val String.htmlEscape: String
            get() = this.replace("&", "&amp;")
                .replace("<", "&lt;")
                .replace(">", "&gt;")
                .replace("\"", "&quot;")
                .replace("'", "&#39;")

        fun truncate(output: String, kb: Int = 32): String {
            var returnVal = output
            if (returnVal.length > 1024 * 2 * kb) {
                returnVal = returnVal.substring(0, 1024 * kb) +
                        "\n\n... Output truncated ...\n\n" +
                        returnVal.substring(returnVal.length - 1024 * kb)
            }
            return returnVal
        }


    }

    private fun getFiles(
        virtualFiles: Array<out File>?
    ): MutableSet<Path> {
        val codeFiles = mutableSetOf<Path>()    // Set to avoid duplicates
        virtualFiles?.forEach { file ->
            if (file.isDirectory) {
                if (file.name.startsWith(".")) return@forEach
                if (FileValidationUtils.isGitignore(file.toPath())) return@forEach
                codeFiles.addAll(getFiles(file.listFiles()))
            } else {
                codeFiles.add((file.toPath()))
            }
        }
        return codeFiles
    }

    override fun codeFiles() = getFiles(virtualFiles)
        .filter { it.toFile().length() < 1024 * 1024 / 2 } // Limit to 0.5MB
        .map { root.toPath().relativize(it) ?: it }.toSet()

    override fun codeSummary(paths: List<Path>): String = paths
        .filter {
            val file = settings.workingDirectory?.resolve(it.toFile())
            file?.exists() == true && !file.isDirectory && file.length() < (256 * 1024)
        }
        .joinToString("\n\n") { path ->
            try {
                """
                        |# ${path}
                        |${tripleTilde}${path.toString().split('.').lastOrNull()}
                        |${settings.workingDirectory?.resolve(path.toFile())?.readText(Charsets.UTF_8)}
                        |${tripleTilde}
                        """.trimMargin()
            } catch (e: Exception) {
                log.warn("Error reading file", e)
                "Error reading file `${path}` - ${e.message}"
            }
        }

    override fun projectSummary(): String {
        val codeFiles = codeFiles()
        val str = codeFiles
            .asSequence()
            .filter { settings.workingDirectory?.toPath()?.resolve(it)?.toFile()?.exists() == true }
            .distinct().sorted()
            .joinToString("\n") { path ->
                "* ${path} - ${
                    settings.workingDirectory?.toPath()?.resolve(path)?.toFile()?.length() ?: "?"
                } bytes".trim()
            }
        return str
    }

    override fun output(task: SessionTask): OutputResult = run {
        val command =
            listOf(settings.executable.absolutePath) + settings.arguments.split(" ").filter(String::isNotBlank)
        val processBuilder = ProcessBuilder(command).directory(settings.workingDirectory)
        val buffer = StringBuilder()
        val taskOutput = task.add("")
        val process = processBuilder.start()
        Thread {
            var lastUpdate = 0L
            process.errorStream.bufferedReader().use { reader ->
                var line: String?
                while (reader.readLine().also { line = it } != null) {
                    buffer.append(line).append("\n")
                    if (lastUpdate + TimeUnit.SECONDS.toMillis(15) < System.currentTimeMillis()) {
                        taskOutput?.set("<pre>\n${truncate(buffer.toString()).htmlEscape}\n</pre>")
                        task.append("", true)
                        lastUpdate = System.currentTimeMillis()
                    }
                }
                task.append("", true)
            }
        }.start()
        process.inputStream.bufferedReader().use { reader ->
            var line: String?
            var lastUpdate = 0L
            while (reader.readLine().also { line = it } != null) {
                buffer.append(line).append("\n")
                if (lastUpdate + TimeUnit.SECONDS.toMillis(15) < System.currentTimeMillis()) {
                    taskOutput?.set("<pre>\n${outputString(buffer).htmlEscape}\n</pre>")
                    task.append("", true)
                    lastUpdate = System.currentTimeMillis()
                }
            }
            task.append("", true)
        }
        task.append("", false)
        if (!process.waitFor(5, TimeUnit.MINUTES)) {
            process.destroy()
            throw RuntimeException("Process timed out")
        }
        val exitCode = process.exitValue()
        var output = outputString(buffer)
        taskOutput?.clear()
        OutputResult(exitCode, output)
    }

    private fun outputString(buffer: StringBuilder): String {
        var output = buffer.toString()
        output = output.replace(Regex("\\x1B\\[[0-?]*[ -/]*[@-~]"), "") // Remove terminal escape codes
        output = truncate(output)
        return output
    }

    override fun searchFiles(searchStrings: List<String>): Set<Path> {
        return searchStrings.flatMap { searchString ->
            FileValidationUtils.filteredWalk(settings.workingDirectory!!) { !FileValidationUtils.isGitignore(it.toPath()) }
                .filter { FileValidationUtils.isLLMIncludable(it) }
                .filter { it.readText().contains(searchString, ignoreCase = true) }
                .map { it.toPath() }
                .toList()
        }.toSet()
    }
}