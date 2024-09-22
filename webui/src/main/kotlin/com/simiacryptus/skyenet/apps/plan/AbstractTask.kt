package com.simiacryptus.skyenet.apps.plan

import com.fasterxml.jackson.databind.annotation.JsonTypeIdResolver
import com.simiacryptus.diff.FileValidationUtils
import com.simiacryptus.jopenai.API
import com.simiacryptus.skyenet.apps.plan.AbstractTask.PlanTaskBase
import com.simiacryptus.skyenet.apps.plan.PlanningTask.PlanTaskTypeIdResolver
import com.simiacryptus.skyenet.set
import com.simiacryptus.skyenet.webui.application.ApplicationInterface
import com.simiacryptus.skyenet.webui.session.SessionTask
import java.io.File
import java.nio.file.FileSystems
import java.nio.file.Files
import java.nio.file.Path
import kotlin.streams.asSequence

abstract class AbstractTask<T : PlanTaskBase>(
    val planSettings: PlanSettings,
    val planTask: T?
) {

    @JsonTypeIdResolver(PlanTaskTypeIdResolver::class)
    open class PlanTaskBase(
        val task_type: String? = null,
        var task_description: String? = null,
        var task_dependencies: List<String>? = null,
        val input_files: List<String>? = null,
        val output_files: List<String>? = null,
        var state: TaskState? = null
    )

    var state: TaskState? = TaskState.Pending
    protected val codeFiles = mutableMapOf<Path, String>()

    protected open val root: Path
        get() = File(planSettings.workingDir).toPath()

    enum class TaskState {
        Pending,
        InProgress,
        Completed,
    }

    protected fun getPriorCode(planProcessingState: PlanProcessingState) =
        planTask?.task_dependencies?.joinToString("\n\n\n") { dependency ->
            """
        |# $dependency
        |
        |${planProcessingState.taskResult[dependency] ?: ""}
        """.trimMargin()
        } ?: ""

    protected fun getInputFileCode(): String =
        ((planTask?.input_files ?: listOf()) + (planTask?.output_files ?: listOf()))
        .flatMap { pattern: String ->
            val matcher = FileSystems.getDefault().getPathMatcher("glob:$pattern")
            Files.walk(root).asSequence()
                .filter { path ->
                    matcher.matches(root.relativize(path)) &&
                            FileValidationUtils.isLLMIncludable(path.toFile())
                }
                .map { path ->
                    root.relativize(path).toString()
                }
                .toList()
        }
        .distinct()
        .sortedBy { it }
        .joinToString("\n\n") { relativePath ->
            val file = root.resolve(relativePath).toFile()
            try {
                """
                |# $relativePath
                |
                |$TRIPLE_TILDE
                |${codeFiles[file.toPath()] ?: file.readText()}
                |$TRIPLE_TILDE
                """.trimMargin()
            } catch (e: Throwable) {
                log.warn("Error reading file: $relativePath", e)
                ""
            }
        }

    protected fun acceptButtonFooter(ui: ApplicationInterface, fn: () -> Unit): String {
        val footerTask = ui.newTask(false)
        lateinit var textHandle: StringBuilder
        textHandle = footerTask.complete(ui.hrefLink("Accept", classname = "href-link cmd-button") {
            try {
                textHandle.set("""<div class="cmd-button">Accepted</div>""")
                footerTask.complete()
            } catch (e: Throwable) {
                log.warn("Error", e)
            }
            fn()
        })!!
        return footerTask.placeholder
    }

    abstract fun promptSegment(): String

    abstract fun run(
        agent: PlanCoordinator,
        taskId: String,
        userMessage: String,
        plan: Map<String, PlanTaskBase>,
        planProcessingState: PlanProcessingState,
        task: SessionTask,
        api: API
    )

    companion object {
        private val log = org.slf4j.LoggerFactory.getLogger(AbstractTask::class.java)
    }
}