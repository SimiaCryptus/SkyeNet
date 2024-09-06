package com.simiacryptus.skyenet.apps.general

import com.simiacryptus.diff.FileValidationUtils
import com.simiacryptus.jopenai.ChatClient
import com.simiacryptus.jopenai.models.OpenAITextModel
import com.simiacryptus.skyenet.core.platform.Session
import com.simiacryptus.skyenet.webui.session.SessionTask
import java.io.File
import java.nio.file.Path

class CommandPatchApp(
    root: File,
    session: Session,
    settings: Settings,
    api: ChatClient,
    model: OpenAITextModel,
    private val files: Array<out File>?,
    val command: String,
) : PatchApp(root, session, settings, api, model) {
    override fun codeFiles() = getFiles(files)
        .filter { it.toFile().length() < 1024 * 1024 / 2 } // Limit to 0.5MB
        .map { root.toPath().relativize(it) ?: it }.toSet()

    override fun codeSummary(paths: List<Path>): String = paths
        .filter { it.toFile().exists() }
        .joinToString("\n\n") { path ->
            """
                |# ${settings.workingDirectory?.toPath()?.relativize(path)}
                |$tripleTilde${path.toString().split('.').lastOrNull()}
                |${path.toFile().readText(Charsets.UTF_8)}
                |$tripleTilde
            """.trimMargin()
        }

    override fun output(task: SessionTask) = OutputResult(
        exitCode = 1,
        output = command
    )

    override fun projectSummary(): String {
        val codeFiles = codeFiles()
        return codeFiles
            .asSequence()
            .filter { settings.workingDirectory?.toPath()?.resolve(it)?.toFile()?.exists() == true }
            .distinct().sorted()
            .joinToString("\n") { path ->
                "* ${path} - ${
                    settings.workingDirectory?.toPath()?.resolve(path)?.toFile()?.length() ?: "?"
                } bytes".trim()
            }
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

    companion object {
        fun getFiles(
            files: Array<out File>?
        ): MutableSet<Path> {
            val codeFiles = mutableSetOf<Path>()    // Set to avoid duplicates
            files?.forEach { file ->
                if (file.isDirectory) {
                    if (file.name.startsWith(".")) return@forEach
                    if (FileValidationUtils.isGitignore(file.toPath())) return@forEach
                    if (file.name.endsWith(".png")) return@forEach
                    if (file.length() > 1024 * 256) return@forEach
                    codeFiles.addAll(getFiles(file.listFiles()))
                } else {
                    codeFiles.add((file.toPath()))
                }
            }
            return codeFiles
        }
    }
}