package com.simiacryptus.skyenet.apps.plan

import com.simiacryptus.diff.FileValidationUtils
import com.simiacryptus.skyenet.TabbedDisplay
import com.simiacryptus.skyenet.core.actors.ParsedResponse
import com.simiacryptus.skyenet.set
import com.simiacryptus.skyenet.webui.application.ApplicationInterface
import com.simiacryptus.skyenet.webui.session.SessionTask
import java.io.File
import java.nio.file.Path

abstract class AbstractTask(
    val settings: Settings,
    val planTask: PlanTask
) {
    val outputFiles: List<String>? = planTask.output_files
    val inputFiles: List<String>? = planTask.input_files
    val taskDependencies: List<String>? = planTask.task_dependencies
    val description: String? = planTask.description
    var state: TaskState? = TaskState.Pending
    val codeFiles = mutableMapOf<Path, String>()

    open val root: Path
        get() = File(settings.workingDir).toPath()

    enum class TaskState {
        Pending,
        InProgress,
        Completed,
    }

    fun getPriorCode(genState: PlanCoordinator.GenState) = taskDependencies?.joinToString("\n\n\n") { dependency ->
        """
        |# $dependency
        |
        |${genState.taskResult[dependency] ?: ""}
        """.trimMargin()
    } ?: ""


    fun getInputFileCode(): String = ((inputFiles ?: listOf()) + (outputFiles ?: listOf()))
        .filter { FileValidationUtils.isLLMIncludable(root.toFile().resolve(it)) }.joinToString("\n\n") {
            try {
                """
                |# $it
                |
                |$TRIPLE_TILDE
                |${codeFiles[File(it).toPath()] ?: root.resolve(it).toFile().readText()}
                |$TRIPLE_TILDE
                """.trimMargin()
            } catch (e: Throwable) {
                PlanCoordinator.log.warn("Error: root=$root    ", e)
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
                PlanCoordinator.log.warn("Error", e)
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
        plan: ParsedResponse<PlanCoordinator.TaskBreakdownResult>,
        genState: PlanCoordinator.GenState,
        task: SessionTask,
        taskTabs: TabbedDisplay
    )
}