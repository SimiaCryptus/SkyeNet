package com.simiacryptus.skyenet.apps.plan


import com.simiacryptus.diff.FileValidationUtils
import com.simiacryptus.jopenai.API
import com.simiacryptus.jopenai.ApiModel
import com.simiacryptus.jopenai.util.ClientUtil.toContentList
import com.simiacryptus.skyenet.Discussable
import com.simiacryptus.skyenet.TabbedDisplay
import com.simiacryptus.skyenet.apps.plan.PlanUtil.buildMermaidGraph
import com.simiacryptus.skyenet.apps.plan.PlanUtil.filterPlan
import com.simiacryptus.skyenet.apps.plan.PlanUtil.getAllDependencies
import com.simiacryptus.skyenet.apps.plan.PlanUtil.render
import com.simiacryptus.skyenet.apps.plan.PlanningTask.*
import com.simiacryptus.skyenet.apps.plan.PlanningTask.Companion.planningActor
import com.simiacryptus.skyenet.apps.plan.TaskType.Companion.getImpl
import com.simiacryptus.skyenet.core.actors.ParsedResponse
import com.simiacryptus.skyenet.core.platform.ApplicationServices
import com.simiacryptus.skyenet.core.platform.Session
import com.simiacryptus.skyenet.core.platform.StorageInterface
import com.simiacryptus.skyenet.core.platform.User
import com.simiacryptus.skyenet.set
import com.simiacryptus.skyenet.webui.application.ApplicationInterface
import com.simiacryptus.skyenet.webui.session.SessionTask
import com.simiacryptus.skyenet.webui.util.MarkdownUtil
import com.simiacryptus.jopenai.util.JsonUtil
import org.slf4j.LoggerFactory
import java.io.File
import java.nio.file.Path
import java.util.concurrent.ThreadPoolExecutor

class PlanCoordinator(
    val user: User?,
    val session: Session,
    val dataStorage: StorageInterface,
    val ui: ApplicationInterface,
    val api: API,
    val planSettings: PlanSettings,
    val root: Path
) {

    val pool: ThreadPoolExecutor by lazy { ApplicationServices.clientManager.getPool(session, user) }

    val files: Array<File> by lazy {
        FileValidationUtils.expandFileList(root.toFile())
    }

    val codeFiles: Map<Path, String>
        get() = files
            .filter { it.exists() && it.isFile }
            .filter { !it.name.startsWith(".") }
            .associate { file ->
                root.relativize(file.toPath()) to try {
                    file.inputStream().bufferedReader().use { it.readText() }
                } catch (e: Exception) {
                    log.warn("Error reading file", e)
                    ""
                }
            }


    fun startProcess(userMessage: String) {
        val task = ui.newTask()
        val plan = filterPlan(
            initialPlan(
                codeFiles = codeFiles,
                files = files,
                root = root,
                task = task,
                userMessage = userMessage,
                ui = ui,
                planSettings = planSettings,
                api = api
            )
        )
        executePlan(plan, task, userMessage)
    }
    fun executeTaskBreakdownWithPrompt(jsonInput: String) {
        val task = ui.newTask()
        try {
            val taskBreakdownWithPrompt = JsonUtil.fromJson<PlanUtil.TaskBreakdownWithPrompt>(jsonInput, PlanUtil.TaskBreakdownWithPrompt::class.java)
            val plan = filterPlan(taskBreakdownWithPrompt.plan)
            task.add(MarkdownUtil.renderMarkdown(
                """
                ## Executing TaskBreakdownWithPrompt
                Prompt: ${taskBreakdownWithPrompt.prompt}
                Plan Text:
                ```
                ${taskBreakdownWithPrompt.planText}
                ```
                """.trimIndent(), ui = ui))
            executePlan(plan, task, taskBreakdownWithPrompt.prompt)
        } catch (e: Exception) {
            task.error(ui, e)
        }
    }


    fun executePlan(
        plan: TaskBreakdownInterface,
        task: SessionTask,
        userMessage: String
    ) {
        try {
            val planProcessingState =
                PlanProcessingState((filterPlan(plan).tasksByID?.entries?.toTypedArray<Map.Entry<String, PlanTask>>()
                    ?.associate { it.key to it.value } ?: mapOf()).toMutableMap())
            val diagramTask = ui.newTask(false).apply { task.add(placeholder) }
            executePlan(
                task = task,
                diagramBuffer = diagramTask.add(
                    MarkdownUtil.renderMarkdown(
                        "## Task Dependency Graph\n${TRIPLE_TILDE}mermaid\n${buildMermaidGraph(planProcessingState.subTasks)}\n$TRIPLE_TILDE",
                        ui = ui
                    )
                ),
                subTasks = planProcessingState.subTasks,
                diagramTask = diagramTask,
                planProcessingState = planProcessingState,
                taskIdProcessingQueue = planProcessingState.taskIdProcessingQueue,
                pool = pool,
                userMessage = userMessage,
                plan = plan
            )
        } catch (e: Throwable) {
            log.warn("Error during incremental code generation process", e)
            task.error(ui, e)
        }
    }

    fun executePlan(
        task: SessionTask,
        diagramBuffer: StringBuilder?,
        subTasks: Map<String, PlanTask>,
        diagramTask: SessionTask,
        planProcessingState: PlanProcessingState,
        taskIdProcessingQueue: MutableList<String>,
        pool: ThreadPoolExecutor,
        userMessage: String,
        plan: TaskBreakdownInterface
    ) {
        val taskTabs = object : TabbedDisplay(ui.newTask(false).apply { task.add(placeholder) }) {
            override fun renderTabButtons(): String {
                diagramBuffer?.set(
                    MarkdownUtil.renderMarkdown(
                        """
                                |## Task Dependency Graph
                                |${TRIPLE_TILDE}mermaid
                                |${buildMermaidGraph(subTasks)}
                                |$TRIPLE_TILDE
                                """.trimMargin(), ui = ui
                    )
                )
                diagramTask.complete()
                return buildString {
                    append("<div class='tabs'>\n")
                    super.tabs.withIndex().forEach { (idx, t) ->
                        val (taskId, taskV) = t
                        val subTask = planProcessingState.tasksByDescription[taskId]
                        if (null == subTask) {
                            log.warn("Task tab not found: $taskId")
                        }
                        val isChecked = if (taskId in taskIdProcessingQueue) "checked" else ""
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
        // Initialize task tabs
        taskIdProcessingQueue.forEach { taskId ->
            val newTask = ui.newTask(false)
            planProcessingState.uitaskMap[taskId] = newTask
            val subtask = planProcessingState.subTasks[taskId]
            val description = subtask?.description
            log.debug("Creating task tab: $taskId ${System.identityHashCode(subtask)} $description")
            taskTabs[description ?: taskId] = newTask.placeholder
        }
        Thread.sleep(100)
        while (taskIdProcessingQueue.isNotEmpty()) {
            val taskId = taskIdProcessingQueue.removeAt(0)
            val subTask = planProcessingState.subTasks[taskId] ?: throw RuntimeException("Task not found: $taskId")
            planProcessingState.taskFutures[taskId] = pool.submit {
                subTask.state = AbstractTask.TaskState.Pending
                taskTabs.update()
                log.debug("Awaiting dependencies: ${subTask.task_dependencies?.joinToString(", ") ?: ""}")
                subTask.task_dependencies
                    ?.associate { it to planProcessingState.taskFutures[it] }
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
                val task1 = planProcessingState.uitaskMap.get(taskId) ?: ui.newTask(false).apply {
                    taskTabs[taskId] = placeholder
                }
                try {
                    val dependencies = subTask.task_dependencies?.toMutableSet() ?: mutableSetOf()
                    dependencies += getAllDependencies(
                        subPlanTask = subTask,
                        subTasks = planProcessingState.subTasks,
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
                    getImpl(planSettings, subTask).run(
                        agent = this,
                        taskId = taskId,
                        userMessage = userMessage,
                        plan = plan,
                        planProcessingState = planProcessingState,
                        task = task1,
                        taskTabs = taskTabs
                    )
                } catch (e: Throwable) {
                    log.warn("Error during task execution", e)
                    task1.error(ui, e)
                } finally {
                    planProcessingState.completedTasks.add(element = taskId)
                    subTask.state = AbstractTask.TaskState.Completed
                    log.debug("Completed task: $taskId ${System.identityHashCode(subTask)}")
                    taskTabs.update()
                }
            }
        }
        planProcessingState.taskFutures.forEach { (id, future) ->
            try {
                future.get() ?: log.warn("Dependency not found: $id")
            } catch (e: Throwable) {
                log.warn("Error", e)
            }
        }
    }

    companion object {
        private val log = LoggerFactory.getLogger(PlanCoordinator::class.java)

        fun initialPlan(
            codeFiles: Map<Path, String>,
            files: Array<File>,
            root: Path,
            task: SessionTask,
            userMessage: String,
            ui: ApplicationInterface,
            planSettings: PlanSettings,
            api: API
        ): TaskBreakdownInterface {
            val toInput = inputFn(codeFiles, files, root)
            return filterPlan(
                Discussable(
                    task = task,
                    heading = MarkdownUtil.renderMarkdown(userMessage, ui = ui),
                    userMessage = { userMessage },
                    initialResponse = {
                        planningActor(planSettings).answer(
                            toInput(it),
                            api = api
                        ) as ParsedResponse<TaskBreakdownInterface>
                    },
                    outputFn = { render(
                        withPrompt = PlanUtil.TaskBreakdownWithPrompt(
                            prompt = userMessage,
                            plan = it.obj,
                            planText = JsonUtil.toJson(it)
                        ),
                        ui = ui
                    ) },
                    ui = ui,
                    reviseResponse = { userMessages: List<Pair<String, ApiModel.Role>> ->
                        val messages = userMessages.map { ApiModel.ChatMessage(it.second, it.first.toContentList()) }
                            .toTypedArray<ApiModel.ChatMessage>()
                        planningActor(planSettings).respond(
                            messages = messages,
                            input = toInput(userMessage),
                            api = api
                        ) as ParsedResponse<TaskBreakdownInterface>
                    },
                ).call().obj
            )
        }


        fun inputFn(
            codeFiles: Map<Path, String>,
            files: Array<File>,
            root: Path
        ): (String) -> List<String> {
            val toInput = { it: String ->
                listOf(
                    if (!codeFiles.all { it.key.toFile().isFile } || codeFiles.size > 2) """
                                            | Files:
                                            | ${codeFiles.keys.joinToString("\n") { "* $it" }}  
                                             """.trimMargin() else {
                        files.joinToString("\n\n") {
                            val path = root.relativize(it.toPath())
                            """
                                    |## $path
                                    |
                                    |${(codeFiles[path] ?: "").let { "$TRIPLE_TILDE\n${it/*.indent("  ")*/}\n$TRIPLE_TILDE" }}
                                    """.trimMargin()
                        }
                    },
                    it
                )
            }
            return toInput
        }
    }


}

const val TRIPLE_TILDE = "```"