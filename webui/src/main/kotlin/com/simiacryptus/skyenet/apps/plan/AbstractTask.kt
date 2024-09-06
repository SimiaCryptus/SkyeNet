package com.simiacryptus.skyenet.apps.plan

import com.simiacryptus.diff.FileValidationUtils
import com.simiacryptus.jopenai.API
import com.simiacryptus.skyenet.TabbedDisplay
import com.simiacryptus.skyenet.apps.plan.PlanningTask.PlanTask
import com.simiacryptus.skyenet.apps.plan.PlanningTask.TaskBreakdownInterface
import com.simiacryptus.skyenet.set
import com.simiacryptus.skyenet.webui.application.ApplicationInterface
import com.simiacryptus.skyenet.webui.session.SessionTask
import java.io.File
import java.nio.file.Path
import java.nio.file.Files
import java.nio.file.FileSystems
import kotlin.streams.asSequence

abstract class AbstractTask(
    val planSettings: PlanSettings,
    val planTask: PlanTask
) {
    var state: TaskState? = TaskState.Pending
    val codeFiles = mutableMapOf<Path, String>()

    open val root: Path
        get() = File(planSettings.workingDir).toPath()

    enum class TaskState {
        Pending,
        InProgress,
        Completed,
    }

    fun getPriorCode(planProcessingState: PlanProcessingState) =
        planTask.task_dependencies?.joinToString("\n\n\n") { dependency ->
            """
        |# $dependency
        |
        |${planProcessingState.taskResult[dependency] ?: ""}
        """.trimMargin()
        } ?: ""

    fun getInputFileCode(): String = ((planTask.input_files ?: listOf()) + (planTask.output_files ?: listOf()))
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

    fun acceptButtonFooter(ui: ApplicationInterface, fn: () -> Unit): String {
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
        plan: TaskBreakdownInterface,
        planProcessingState: PlanProcessingState,
        task: SessionTask,
        taskTabs: TabbedDisplay,
        api: API
    )

    companion object {
        private val log = org.slf4j.LoggerFactory.getLogger(AbstractTask::class.java)
    }
}