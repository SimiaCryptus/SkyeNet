package com.simiacryptus.skyenet.apps.plan

import com.github.simiacryptus.aicoder.util.FileSystemUtils
import com.intellij.openapi.actionSystem.AnActionEvent
import com.intellij.openapi.actionSystem.CommonDataKeys.VIRTUAL_FILE_ARRAY
import com.intellij.openapi.vfs.VirtualFile
import com.intellij.openapi.vfs.isFile
import com.simiacryptus.jopenai.API
import com.simiacryptus.jopenai.ApiModel
import com.simiacryptus.jopenai.describe.Description
import com.simiacryptus.jopenai.util.ClientUtil.toContentList
import com.simiacryptus.jopenai.util.JsonUtil
import com.simiacryptus.skyenet.AgentPatterns
import com.simiacryptus.skyenet.Discussable
import com.simiacryptus.skyenet.TabbedDisplay
import com.simiacryptus.skyenet.core.actors.ParsedResponse
import com.simiacryptus.skyenet.core.platform.ApplicationServices
import com.simiacryptus.skyenet.core.platform.Session
import com.simiacryptus.skyenet.core.platform.StorageInterface
import com.simiacryptus.skyenet.core.platform.User
import com.simiacryptus.skyenet.set
import com.simiacryptus.skyenet.webui.application.ApplicationInterface
import com.simiacryptus.skyenet.webui.session.SessionTask
import com.simiacryptus.skyenet.webui.util.MarkdownUtil
import org.slf4j.LoggerFactory
import java.nio.file.Path
import java.util.*
import java.util.concurrent.Future
import java.util.concurrent.ThreadPoolExecutor

class PlanCoordinator(
    val user: User?,
    val session: Session,
    val dataStorage: StorageInterface,
    val ui: ApplicationInterface,
    val api: API,
    val settings: Settings,
    val event: AnActionEvent,
    val root: Path
) {
    private val taskBreakdownActor by lazy { settings.planningActor() }

    data class TaskBreakdownResult(
        val tasksByID: Map<String, Task>? = null,
        val finalTaskID: String? = null,
    )

    data class Task(
        val description: String? = null,
        val taskType: TaskType? = null,
        var task_dependencies: List<String>? = null,
        val input_files: List<String>? = null,
        val output_files: List<String>? = null,
        var state: AbstractTask.TaskState? = null,
        @Description("Command and arguments (in list form) for the task")
        val command: List<String>? = null,
    )

    val virtualFiles by lazy {
        FileSystemUtils.expandFileList(VIRTUAL_FILE_ARRAY.getData(event.dataContext) ?: arrayOf())
    }

    private val codeFiles: Map<Path, String>
        get() = virtualFiles
            .filter { it.exists() && it.isFile }
            .filter { !it.name.startsWith(".") }
            .associate { file -> getKey(file) to getValue(file) }


    private fun getValue(file: VirtualFile) = try {
        file.inputStream.bufferedReader().use { it.readText() }
    } catch (e: Exception) {
        log.warn("Error reading file", e)
        ""
    }

    private fun getKey(file: VirtualFile) = root.relativize(file.toNioPath())

    fun startProcess(userMessage: String) {
        val codeFiles = codeFiles
        val eventStatus = if (!codeFiles.all { it.key.toFile().isFile } || codeFiles.size > 2) """
 Files:
 ${codeFiles.keys.joinToString("\n") { "* $it" }}  
     """.trimMargin() else {
            """
            |${
                virtualFiles.joinToString("\n\n") {
                    val path = root.relativize(it.toNioPath())
                    """
 ## $path
              |
 ${(codeFiles[path] ?: "").let { "$TRIPLE_TILDE\n${it/*.indent("  ")*/}\n$TRIPLE_TILDE" }}
             """.trimMargin()
                }
            }
           """.trimMargin()
        }
        val task = ui.newTask()
        val toInput = { it: String ->
            listOf(
                eventStatus,
                it
            )
        }
        val highLevelPlan = Discussable(
            task = task,
            heading = MarkdownUtil.renderMarkdown(userMessage, ui = ui),
            userMessage = { userMessage },
            initialResponse = { it: String -> taskBreakdownActor.answer(toInput(it), api = api) },
            outputFn = { design: ParsedResponse<TaskBreakdownResult> ->
                AgentPatterns.displayMapInTabs(
                    mapOf(
                        "Text" to MarkdownUtil.renderMarkdown(design.text, ui = ui),
                        "JSON" to MarkdownUtil.renderMarkdown(
                            "${TRIPLE_TILDE}json\n${JsonUtil.toJson(design.obj)/*.indent("  ")*/}\n$TRIPLE_TILDE",
                            ui = ui
                        ),
                    )
                )
            },
            ui = ui,
            reviseResponse = { userMessages: List<Pair<String, ApiModel.Role>> ->
                taskBreakdownActor.respond(
                    messages = (userMessages.map { ApiModel.ChatMessage(it.second, it.first.toContentList()) }
                        .toTypedArray<ApiModel.ChatMessage>()),
                    input = toInput(userMessage),
                    api = api
                )
            },
        ).call()

        initPlan(highLevelPlan, userMessage, task)
    }

    fun initPlan(
        plan: ParsedResponse<TaskBreakdownResult>,
        userMessage: String,
        task: SessionTask
    ) {
        try {
            val tasksByID =
                plan.obj.tasksByID?.entries?.toTypedArray()?.associate { it.key to it.value } ?: mapOf()
            val pool: ThreadPoolExecutor = ApplicationServices.clientManager.getPool(session, user)
            val genState = GenState(tasksByID.toMutableMap())
            val diagramTask = ui.newTask(false).apply { task.add(placeholder) }
            val diagramBuffer =
                diagramTask.add(
                    MarkdownUtil.renderMarkdown(
                        "## Task Dependency Graph\n${TRIPLE_TILDE}mermaid\n${buildMermaidGraph(genState.subTasks)}\n$TRIPLE_TILDE",
                        ui = ui
                    )
                )
            val taskTabs = object : TabbedDisplay(ui.newTask(false).apply { task.add(placeholder) }) {
                override fun renderTabButtons(): String {
                    diagramBuffer?.set(
                        MarkdownUtil.renderMarkdown(
                            "## Task Dependency Graph\n${TRIPLE_TILDE}mermaid\n${
                                buildMermaidGraph(
                                    genState.subTasks
                                )
                            }\n$TRIPLE_TILDE", ui = ui
                        )
                    )
                    diagramTask.complete()
                    return buildString {
                        append("<div class='tabs'>\n")
                        super.tabs.withIndex().forEach { (idx, t) ->
                            val (taskId, taskV) = t
                            val subTask = genState.tasksByDescription[taskId]
                            if (null == subTask) {
                                log.warn("Task tab not found: $taskId")
                            }
                            val isChecked = if (taskId in genState.taskIdProcessingQueue) "checked" else ""
                            val style = when (subTask?.state) {
                                AbstractTask.TaskState.Completed -> " style='text-decoration: line-through;'"
                                null -> " style='opacity: 20%;'"
                                AbstractTask.TaskState.Pending -> " style='opacity: 30%;'"
                                else -> ""
                            }
                            append("<label class='tab-button' data-for-tab='${idx}'$style><input type='checkbox' $isChecked disabled /> $taskId</label><br/>\n")
                        }
                        append("</div>")
                    }
                }
            }
            genState.taskIdProcessingQueue.forEach { taskId ->
                val newTask = ui.newTask(false)
                genState.uitaskMap[taskId] = newTask
                val subtask = genState.subTasks[taskId]
                val description = subtask?.description
                log.debug("Creating task tab: $taskId ${System.identityHashCode(subtask)} $description")
                taskTabs[description ?: taskId] = newTask.placeholder
            }
            Thread.sleep(100)
            while (genState.taskIdProcessingQueue.isNotEmpty()) {
                val taskId = genState.taskIdProcessingQueue.removeAt(0)
                val subTask = genState.subTasks[taskId] ?: throw RuntimeException("Task not found: $taskId")
                genState.taskFutures[taskId] = pool.submit {
                    subTask.state = AbstractTask.TaskState.Pending
                    taskTabs.update()
                    log.debug("Awaiting dependencies: ${subTask.task_dependencies?.joinToString(", ") ?: ""}")
                    subTask.task_dependencies
                        ?.associate { it to genState.taskFutures[it] }
                        ?.forEach { (id, future) ->
                            try {
                                future?.get() ?: log.warn("Dependency not found: $id")
                            } catch (e: Throwable) {
                                log.warn("Error", e)
                            }
                        }
                    subTask.state = AbstractTask.TaskState.InProgress
                    taskTabs.update()
                    log.debug("Running task: ${System.identityHashCode(subTask)} ${subTask.description}")
                    val task1 = genState.uitaskMap.get(taskId) ?: ui.newTask(false).apply {
                        taskTabs[taskId] = placeholder
                    }
                    try {
                        val dependencies = subTask.task_dependencies?.toMutableSet() ?: mutableSetOf()
                        dependencies += getAllDependencies(
                            subTask = subTask,
                            subTasks = genState.subTasks,
                            visited = mutableSetOf()
                        )

                        task1.add(
                            MarkdownUtil.renderMarkdown(
                                """
             ## Task `${taskId}`
             ${subTask.description ?: ""}
                      |
                      |${TRIPLE_TILDE}json
                      |${JsonUtil.toJson(data = subTask)/*.indent("  ")*/}
                      |$TRIPLE_TILDE
                      |
                      |### Dependencies:
                      |${dependencies.joinToString("\n") { "- $it" }}
                      |
                      """.trimMargin(), ui = ui
                            )
                        )
                        settings.getImpl(subTask).run(
                            agent = this,
                            taskId = taskId,
                            userMessage = userMessage,
                            plan = plan,
                            genState = genState,
                            task = task1,
                            taskTabs = taskTabs
                        )
                    } catch (e: Throwable) {
                        log.warn("Error during task execution", e)
                        task1.error(ui, e)
                    } finally {
                        genState.completedTasks.add(element = taskId)
                        subTask.state = AbstractTask.TaskState.Completed
                        log.debug("Completed task: $taskId ${System.identityHashCode(subTask)}")
                        taskTabs.update()
                    }
                }
            }
            genState.taskFutures.forEach { (id, future) ->
                try {
                    future.get() ?: log.warn("Dependency not found: $id")
                } catch (e: Throwable) {
                    log.warn("Error", e)
                }
            }
        } catch (e: Throwable) {
            log.warn("Error during incremental code generation process", e)
            task.error(ui, e)
        }
    }

    private fun getAllDependencies(
        subTask: Task,
        subTasks: Map<String, Task>,
        visited: MutableSet<String>
    ): List<String> {
        val dependencies = subTask.task_dependencies?.toMutableList() ?: mutableListOf()
        subTask.task_dependencies?.forEach { dep ->
            if (dep in visited) return@forEach
            val subTask = subTasks[dep]
            if (subTask != null) {
                visited.add(dep)
                dependencies.addAll(getAllDependencies(subTask, subTasks, visited))
            }
        }
        return dependencies
    }

    companion object {
        val log = LoggerFactory.getLogger(PlanCoordinator::class.java)

        fun executionOrder(tasks: Map<String, Task>): List<String> {
            val taskIds: MutableList<String> = mutableListOf()
            val taskMap = tasks.toMutableMap()
            while (taskMap.isNotEmpty()) {
                val nextTasks =
                    taskMap.filter { (_, task) -> task.task_dependencies?.all { taskIds.contains(it) } ?: true }
                if (nextTasks.isEmpty()) {
                    throw RuntimeException("Circular dependency detected in task breakdown")
                }
                taskIds.addAll(nextTasks.keys)
                nextTasks.keys.forEach { taskMap.remove(it) }
            }
            return taskIds
        }

        val isWindows = System.getProperty("os.name").lowercase(Locale.getDefault()).contains("windows")
        private fun sanitizeForMermaid(input: String) = input
            .replace(" ", "_")
            .replace("\"", "\\\"")
            .replace("[", "\\[")
            .replace("]", "\\]")
            .replace("(", "\\(")
            .replace(")", "\\)")
            .let { "`$it`" }

        private fun escapeMermaidCharacters(input: String) = input
            .replace("\"", "\\\"")
            .let { '"' + it + '"' }

        private fun buildMermaidGraph(subTasks: Map<String, Task>): String {
            val graphBuilder = StringBuilder("graph TD;\n")
            subTasks.forEach { (taskId, task) ->
                val sanitizedTaskId = sanitizeForMermaid(taskId)
                val taskType = task.taskType?.name ?: "Unknown"
                val escapedDescription = escapeMermaidCharacters(task.description ?: "")
                graphBuilder.append("    ${sanitizedTaskId}[$escapedDescription]:::$taskType;\n")
                task.task_dependencies?.forEach { dependency ->
                    val sanitizedDependency = sanitizeForMermaid(dependency)
                    graphBuilder.append("    $sanitizedDependency --> ${sanitizedTaskId};\n")
                }
            }
            graphBuilder.append("    classDef default fill:#f9f9f9,stroke:#333,stroke-width:2px;\n")
            graphBuilder.append("    classDef NewFile fill:lightblue,stroke:#333,stroke-width:2px;\n")
            graphBuilder.append("    classDef EditFile fill:lightgreen,stroke:#333,stroke-width:2px;\n")
            graphBuilder.append("    classDef Documentation fill:lightyellow,stroke:#333,stroke-width:2px;\n")
            graphBuilder.append("    classDef Inquiry fill:orange,stroke:#333,stroke-width:2px;\n")
            graphBuilder.append("    classDef TaskPlanning fill:lightgrey,stroke:#333,stroke-width:2px;\n")
            return graphBuilder.toString()
        }

    }

    data class GenState(
        val subTasks: Map<String, Task>,
        val tasksByDescription: MutableMap<String?, Task> = subTasks.entries.toTypedArray()
            .associate { it.value.description to it.value }.toMutableMap(),
        val taskIdProcessingQueue: MutableList<String> = executionOrder(subTasks).toMutableList(),
        val taskResult: MutableMap<String, String> = mutableMapOf(),
        val completedTasks: MutableList<String> = mutableListOf(),
        val taskFutures: MutableMap<String, Future<*>> = mutableMapOf(),
        val uitaskMap: MutableMap<String, SessionTask> = mutableMapOf()
    )

}

const val TRIPLE_TILDE = "```"
