package com.simiacryptus.skyenet.apps.plan


import com.simiacryptus.diff.FileValidationUtils
import com.simiacryptus.jopenai.API
import com.simiacryptus.jopenai.ChatClient
import com.simiacryptus.jopenai.models.ApiModel
import com.simiacryptus.skyenet.Discussable
import com.simiacryptus.skyenet.TabbedDisplay
import com.simiacryptus.skyenet.apps.plan.AbstractTask.PlanTaskBaseInterface
import com.simiacryptus.skyenet.apps.plan.PlanUtil.buildMermaidGraph
import com.simiacryptus.skyenet.apps.plan.PlanUtil.filterPlan
import com.simiacryptus.skyenet.apps.plan.PlanUtil.getAllDependencies
import com.simiacryptus.skyenet.apps.plan.PlanUtil.render
import com.simiacryptus.skyenet.apps.plan.PlanningTask.*
import com.simiacryptus.skyenet.apps.plan.TaskType.Companion.getImpl
import com.simiacryptus.skyenet.core.actors.ParsedResponse
import com.simiacryptus.skyenet.core.platform.ApplicationServices
import com.simiacryptus.skyenet.core.platform.Session
import com.simiacryptus.skyenet.core.platform.StorageInterface
import com.simiacryptus.skyenet.core.platform.User
import com.simiacryptus.skyenet.set
import com.simiacryptus.skyenet.util.MarkdownUtil
import com.simiacryptus.skyenet.webui.application.ApplicationInterface
import com.simiacryptus.skyenet.webui.session.SessionTask
import com.simiacryptus.util.JsonUtil
import org.slf4j.LoggerFactory
import java.io.File
import java.nio.file.Path
import java.util.*
import java.util.concurrent.ThreadPoolExecutor
import java.util.concurrent.TimeUnit

class PlanCoordinator(
    val user: User?,
    val session: Session,
    val dataStorage: StorageInterface,
    val ui: ApplicationInterface,
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

    fun executeTaskBreakdownWithPrompt(jsonInput: String, api: API) {
        val task = ui.newTask()
        try {
            lateinit var taskBreakdownWithPrompt: PlanUtil.TaskBreakdownWithPrompt
            val plan = filterPlan {
                taskBreakdownWithPrompt = JsonUtil.fromJson(jsonInput, PlanUtil.TaskBreakdownWithPrompt::class.java)
                taskBreakdownWithPrompt.plan
            }
            task.add(
                MarkdownUtil.renderMarkdown(
                """
                |## Executing TaskBreakdownWithPrompt
                |Prompt: ${taskBreakdownWithPrompt.prompt}
                |Plan Text:
                |```
                |${taskBreakdownWithPrompt.planText}
                |```
                """.trimMargin(), ui = ui))
            executePlan(plan ?: emptyMap(), task, taskBreakdownWithPrompt.prompt, api)
        } catch (e: Exception) {
            task.error(ui, e)
        }
    }

    fun executePlan(
        plan: Map<String, PlanTaskBaseInterface>,
        task: SessionTask,
        userMessage: String,
        api: API
    ): PlanProcessingState {
        val api = (api as ChatClient).getChildClient().apply {
            val createFile = task.createFile(".logs/api-${UUID.randomUUID()}.log")
            createFile.second?.apply {
                logStreams += this.outputStream().buffered()
                task.verbose("API log: <a href=\"file:///$this\">$this</a>")
            }
        }
        val planProcessingState = newState(plan)
        try {
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
                plan = plan,
                api = api
            )
        } catch (e: Throwable) {
            log.warn("Error during incremental code generation process", e)
            task.error(ui, e)
        }
        return planProcessingState
    }

    private fun newState(plan: Map<String, PlanTaskBaseInterface>) =
        PlanProcessingState(
            subTasks = (filterPlan { plan }?.entries?.toTypedArray<Map.Entry<String, PlanTaskBaseInterface>>()
                ?.associate { it.key to it.value } ?: mapOf()).toMutableMap()
        )

    fun executePlan(
        task: SessionTask,
        diagramBuffer: StringBuilder?,
        subTasks: Map<String, PlanTaskBaseInterface>,
        diagramTask: SessionTask,
        planProcessingState: PlanProcessingState,
        taskIdProcessingQueue: MutableList<String>,
        pool: ThreadPoolExecutor,
        userMessage: String,
        plan: Map<String, PlanTaskBaseInterface>,
        api: API
    ) {
        val sessionTask = ui.newTask(false).apply { task.add(placeholder) }
        val api = (api as ChatClient).getChildClient().apply {
            val createFile = sessionTask.createFile(".logs/api-${UUID.randomUUID()}.log")
            createFile.second?.apply {
                logStreams += this.outputStream().buffered()
                sessionTask.verbose("API log: <a href=\"file:///$this\">$this</a>")
            }
        }
        val taskTabs = object : TabbedDisplay(sessionTask) {
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
        taskIdProcessingQueue.forEach { taskId ->
            val newTask = ui.newTask(false)
            planProcessingState.uitaskMap[taskId] = newTask
            val subtask = planProcessingState.subTasks[taskId]
            val description = subtask?.task_description
            log.debug("Creating task tab: $taskId ${System.identityHashCode(subtask)} $description")
            taskTabs[description ?: taskId] = newTask.placeholder
        }
        Thread.sleep(100)
        while (taskIdProcessingQueue.isNotEmpty()) {
            val taskId = taskIdProcessingQueue.removeAt(0)
            val subTask = planProcessingState.subTasks[taskId] ?: throw RuntimeException("Task not found: $taskId")
            planProcessingState.taskFutures[taskId] = pool.submit {
                subTask.state = AbstractTask.TaskState.Pending
                //taskTabs.update()
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
                //taskTabs.update()
                log.debug("Running task: ${System.identityHashCode(subTask)} ${subTask.task_description}")
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
                            |## Task `${taskId}`
                            |${subTask.task_description ?: ""}
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
                    val api = (api as ChatClient).getChildClient().apply {
                        val createFile = task1.createFile(".logs/api-${UUID.randomUUID()}.log")
                        createFile.second?.apply {
                            logStreams += this.outputStream().buffered()
                            task1.verbose("API log: <a href=\"file:///$this\">$this</a>")
                        }
                    }
                    getImpl(planSettings, subTask).run(
                        agent = this,
                        taskId = taskId,
                        userMessage = userMessage,
                        plan = plan,
                        planProcessingState = planProcessingState,
                        task = task1,
                        api = api
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
        val start = System.currentTimeMillis()
        planProcessingState.taskFutures.forEach { (id, future) ->
            try {
                future.get(
                    (TimeUnit.MINUTES.toMillis(1) - (System.currentTimeMillis() - start)).coerceAtLeast(0),
                    TimeUnit.MILLISECONDS
                ) ?: log.warn("Dependency not found: $id")
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
        ): PlanUtil.TaskBreakdownWithPrompt {
            val api = (api as ChatClient).getChildClient().apply {
                val createFile = task.createFile(".logs/api-${UUID.randomUUID()}.log")
                createFile.second?.apply {
                    logStreams += this.outputStream().buffered()
                    task.verbose("API log: <a href=\"file:///$this\">$this</a>")
                }
            }
            val toInput = inputFn(codeFiles, files, root)
            return if (planSettings.allowBlocking)
                Discussable(
                    task = task,
                    heading = MarkdownUtil.renderMarkdown(userMessage, ui = ui),
                    userMessage = { userMessage },
                    initialResponse = {
                        newPlan(
                            api,
                            planSettings,
                            toInput(userMessage)
                        )
                    },
                    outputFn = {
                        try {
                            render(
                                withPrompt = PlanUtil.TaskBreakdownWithPrompt(
                                    prompt = userMessage,
                                    plan = it.obj,
                                    planText = it.text
                                ),
                                ui = ui
                            )
                        } catch (e: Throwable) {
                            log.warn("Error rendering task breakdown", e)
                            task.error(ui, e)
                            e.message ?: e.javaClass.simpleName
                        }
                    },
                    ui = ui,
                    reviseResponse = { userMessages: List<Pair<String, ApiModel.Role>> ->
                        newPlan(
                            api,
                            planSettings,
                            userMessages.map { it.first })
                    },
                ).call().let {
                    PlanUtil.TaskBreakdownWithPrompt(
                        prompt = userMessage,
                        plan = filterPlan { it.obj } ?: emptyMap(),
                        planText = it.text
                    )
                }
            else newPlan(
                api,
                planSettings,
                toInput(userMessage)
            ).let {
                PlanUtil.TaskBreakdownWithPrompt(
                    prompt = userMessage,
                    plan = filterPlan { it.obj } ?: emptyMap(),
                    planText = it.text
                )
            }
        }

        private fun newPlan(
            api: API,
            planSettings: PlanSettings,
            inStrings: List<String>
        ): ParsedResponse<Map<String, PlanTaskBaseInterface>> {
            val planningActor = planSettings.planningActor<TaskBreakdownInterface<*>>()
            return planningActor.respond(
                messages = planningActor.chatMessages(inStrings),
                input = inStrings,
                api = api
            ).map(Map::class.java) { it.tasksByID ?: emptyMap<String, PlanTaskBaseInterface>() } as ParsedResponse<Map<String, PlanTaskBaseInterface>>
        }


        private fun inputFn(
            codeFiles: Map<Path, String>,
            files: Array<File>,
            root: Path
        ) = { str: String ->
            listOf(
                if (!codeFiles.all { it.key.toFile().isFile } || codeFiles.size > 2) """
                                        |Files:
                                        |${codeFiles.keys.joinToString("\n") { "* $it" }}  
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
                str
            )
        }
    }
}

const val TRIPLE_TILDE = "```"