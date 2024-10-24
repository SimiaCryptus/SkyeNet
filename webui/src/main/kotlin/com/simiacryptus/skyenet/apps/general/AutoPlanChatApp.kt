package com.simiacryptus.skyenet.apps.general

import com.simiacryptus.jopenai.API
import com.simiacryptus.jopenai.ChatClient
import com.simiacryptus.jopenai.models.TextModel
import com.simiacryptus.skyenet.TabbedDisplay
import com.simiacryptus.skyenet.apps.plan.PlanCoordinator
import com.simiacryptus.skyenet.apps.plan.PlanSettings
import com.simiacryptus.skyenet.apps.plan.PlanTaskBase
import com.simiacryptus.skyenet.apps.plan.TaskType
import com.simiacryptus.skyenet.apps.plan.file.FileModificationTask.FileModificationTaskData
import com.simiacryptus.skyenet.core.actors.ParsedActor
import com.simiacryptus.skyenet.core.platform.Session
import com.simiacryptus.skyenet.core.platform.model.User
import com.simiacryptus.skyenet.util.MarkdownUtil.renderMarkdown
import com.simiacryptus.skyenet.webui.application.ApplicationInterface
import com.simiacryptus.util.JsonUtil
import org.slf4j.LoggerFactory
import java.io.File
import java.util.Date
import java.util.UUID
import java.util.concurrent.Future
import java.util.concurrent.atomic.AtomicReference

open class AutoPlanChatApp(
    applicationName: String = "Auto Plan Chat App",
    path: String = "/autoPlanChat",
    planSettings: PlanSettings,
    model: TextModel,
    parsingModel: TextModel,
    domainName: String = "localhost",
    showMenubar: Boolean = true,
    api: API? = null,
    val maxTaskHistoryChars: Int = 20000,
    val maxTasksPerIteration: Int = 3
) : PlanChatApp(
    applicationName = applicationName,
    path = path,
    planSettings = planSettings,
    model = model,
    parsingModel = parsingModel,
    domainName = domainName,
    showMenubar = showMenubar,
    api = api,
) {
    override val stickyInput = true
    override val singleInput = false
    companion object {
        private val log = LoggerFactory.getLogger(AutoPlanChatApp::class.java)
    }

    data class ThinkingStatus(
        var initialPrompt: String? = null,
        val goals: Goals? = null,
        val knowledge: Knowledge? = null,
        val executionContext: ExecutionContext? = null
    )

    data class Goals(
        val shortTerm: MutableList<Any>? = null,
        val longTerm: MutableList<Any>? = null
    )

    data class Knowledge(
        val facts: MutableList<Any>? = null,
        val hypotheses: MutableList<Any>? = null,
        val openQuestions: MutableList<Any>? = null
    )

    data class ExecutionContext(
        val completedTasks: MutableList<Any>? = null,
        val currentTask: CurrentTask? = null,
        val nextSteps: MutableList<Any>? = null
    )

    data class CurrentTask(
        val taskId: String? = null,
        val description: String? = null
    )

    private val thinkingStatus = AtomicReference<ThinkingStatus?>(null)
    private lateinit var initActor: ParsedActor<ThinkingStatus>
    private lateinit var updateActor: ParsedActor<ThinkingStatus>
    private val currentUserMessage = AtomicReference<String?>(null)

    data class ExecutionRecord(
        val time: Date? = Date(),
        val task: PlanTaskBase? = null,
        val result: Any? = null
    )

    data class Tasks(
        val tasks: MutableList<PlanTaskBase>? = null
    )

    val executionRecords = mutableListOf<ExecutionRecord>()
    override fun userMessage(session: Session, user: User?, userMessage: String, ui: ApplicationInterface, api: API) {
        try {
            log.info("Received user message: $userMessage")
            if (thinkingStatus.get() == null) {
                log.info("Starting new auto plan chat")
                startAutoPlanChat(session, user, userMessage, ui, api)
            } else {
                log.info("Injecting user message into ongoing chat")
                val userMessageTask = ui.newTask()
                userMessageTask.echo(renderMarkdown("User: $userMessage", ui = ui))
                currentUserMessage.set(userMessage)
            }
        } catch (e: Exception) {
            log.error("Error processing user message", e)
            ui.newTask().add(renderMarkdown("An error occurred while processing your message: ${e.message}", ui = ui))
        }
    }

    private fun startAutoPlanChat(session: Session, user: User?, userMessage: String, ui: ApplicationInterface, api: API) {
        val task = ui.newTask(true)
        val api = (api as ChatClient).getChildClient().apply {
            val createFile = task.createFile(".logs/api-${UUID.randomUUID()}.log")
            createFile.second?.apply {
                logStreams += this.outputStream().buffered()
                task.verbose("API log: <a href=\"file:///$this\">$this</a>")
            }
        }

        val tabbedDisplay = TabbedDisplay(task)
        val executor = ui.socketManager!!.pool
        executor.execute {
            try {
                tabbedDisplay.update()
                task.complete()

                val initialPromptTask = ui.newTask(false)
                initialPromptTask.add(renderMarkdown("Starting Auto Plan Chat for prompt: $userMessage"))
                tabbedDisplay["Initial Prompt"] = initialPromptTask.placeholder
                val planSettings = getSettings(session, user, PlanSettings::class.java) ?: planSettings.copy(allowBlocking = false)
                api.budget = planSettings.budget
                val coordinator = PlanCoordinator(
                    user = user,
                    session = session,
                    dataStorage = dataStorage,
                    ui = ui,
                    root = planSettings.workingDir?.let { File(it).toPath() } ?: dataStorage.getDataDir(user, session).toPath(),
                    planSettings = planSettings
                )
                initActor = ParsedActor(
                    name = "ThinkingStatusInitializer",
                    resultClass = ThinkingStatus::class.java,
                    exampleInstance = ThinkingStatus(
                        initialPrompt = "Example prompt",
                        goals = Goals(
                            shortTerm = mutableListOf("Understand the user's request"),
                            longTerm = mutableListOf("Complete the user's task")
                        ),
                        knowledge = Knowledge(
                            facts = mutableListOf("Initial Context: User's request received"),
                            openQuestions = mutableListOf("What is the first task?")
                        ),
                        executionContext = ExecutionContext(
                            nextSteps = mutableListOf("Analyze the initial prompt", "Identify key objectives"),
                        )
                    ),
                    prompt = """
                            Given the user's initial prompt, initialize the thinking status for an AI assistant.
                            Set short-term and long-term goals.
                            Generate relevant open questions and hypotheses to guide the planning process.
                            Initialize the knowledge base with any relevant information from the initial prompt.
                            Set up the execution context with initial next steps, progress (0-100), estimated time remaining, and confidence level (0-100).
                            Identify potential challenges and available resources.
                            """.trimIndent(),
                    model = planSettings.defaultModel,
                    parsingModel = planSettings.parsingModel,
                    temperature = planSettings.temperature,
                    describer = planSettings.describer()
                )
                updateActor = ParsedActor(
                    name = "UpdateQuestionsActor",
                    resultClass = ThinkingStatus::class.java,
                    exampleInstance = ThinkingStatus(
                        initialPrompt = "Example prompt",
                        goals = Goals(
                            shortTerm = mutableListOf("Analyze task results"),
                            longTerm = mutableListOf("Complete the user's request")
                        ),
                        knowledge = Knowledge(
                            facts = mutableListOf(
                                "Initial Context: User's request received",
                                "Task 1 Result: Analyzed user's request"
                            ),
                            openQuestions = mutableListOf("What is the next task?", "Are there any remaining tasks?")
                        ),
                        executionContext = ExecutionContext(
                            completedTasks = mutableListOf("task_1"),
                            nextSteps = mutableListOf("Analyze task results", "Determine next action"),
                        )
                    ),
                    prompt = """
                            Given the current thinking status, the last completed task, and its result,
                            update the open questions to guide the next steps of the planning process.
                            Consider what information is still needed and what new questions arise from the task result.
                            Update the current goal if necessary, adjust the progress, suggest next steps, and add any new insights.
                            Update the knowledge base with new facts and hypotheses.
                            Update the estimated time remaining and adjust the confidence level based on progress.
                            Reassess challenges, available resources, and alternative approaches.
                        """.trimIndent(),
                    model = planSettings.defaultModel,
                    parsingModel = planSettings.parsingModel,
                    temperature = planSettings.temperature,
                    describer = planSettings.describer()
                )
                val initialStatus = initActor.answer(initialPrompt(userMessage), this.api!!).obj
                initialStatus.initialPrompt = userMessage
                thinkingStatus.set(initialStatus)
                initialPromptTask.complete(renderMarkdown("Initial Thinking Status:\n${formatThinkingStatus(thinkingStatus.get()!!)}"))

                var iteration = 0
                while (iteration++ < 100) {
                    task.complete()
                    val task = ui.newTask(false).apply { tabbedDisplay["Iteration $iteration"] = placeholder }
                    val api = api.getChildClient().apply {
                        val createFile = task.createFile(".logs/api-${UUID.randomUUID()}.log")
                        createFile.second?.apply {
                            logStreams += this.outputStream().buffered()
                            task.verbose("API log: <a href=\"file:///$this\">$this</a>")
                        }
                    }
                    val tabbedDisplay = TabbedDisplay(task)
                    tabbedDisplay.update()
                    val nextTask = try {
                        val describer1 = planSettings.describer()
                        val chooserResult = ParsedActor<Tasks>(
                            name = "SingleTaskChooser",
                            resultClass = Tasks::class.java,
                            exampleInstance = Tasks(
                                listOf(
                                    FileModificationTaskData(
                                        task_description = "Modify the file 'example.txt' to include the given input."
                                    )
                                ).toMutableList()
                            ),
                            prompt = """
                    Given the following input, choose up to $maxTasksPerIteration tasks to execute. Do not create a full plan, just select the most appropriate task types for the given input.
                    Available task types:
                    
                    ${
                                TaskType.Companion.getAvailableTaskTypes(coordinator.planSettings).joinToString<TaskType<*>>("\n") { taskType ->
                                    "* ${TaskType.Companion.getImpl(coordinator.planSettings, taskType).promptSegment()}"
                                }
                            }
                    
                    Choose the most suitable task types and provide details of how they should be executed.
                            """.trimIndent(),
                            model = coordinator.planSettings.defaultModel,
                            parsingModel = coordinator.planSettings.parsingModel,
                            temperature = coordinator.planSettings.temperature,
                            describer = describer1,
                            parserPrompt = """
                    Task Subtype Schema:
                    
                    ${
                                TaskType.Companion.getAvailableTaskTypes(coordinator.planSettings).joinToString<TaskType<*>>("\n\n") { taskType ->
                                    """
                    ${taskType.name}:
                      ${describer1.describe(taskType.taskDataClass).replace("\n", "\n  ")}
                    """.trim()
                                }
                            }
                                    """.trimIndent()
                        ).answer(
                            initialPrompt(userMessage) +
                                    listOf(
                                        """
                                                        Current thinking status: ${formatThinkingStatus(thinkingStatus.get()!!)}
                                                        Please choose the next single task to execute based on the current status.
                                                        If there are no tasks to execute, return {}.
                                                    """.trimIndent()
                                    )
                                    + formatEvalRecords(), api
                        ).obj
                        chooserResult.tasks?.take(maxTasksPerIteration)?.mapNotNull { task ->
                            (if (task.task_type == null) {
                                null
                            } else {
                                TaskType.Companion.getImpl(coordinator.planSettings, task)
                            })?.planTask
                        }
                    } catch (e: Exception) {
                        log.error("Error choosing next task", e)
                        tabbedDisplay["Errors"]?.append(renderMarkdown("Error choosing next task: ${e.message}"))
                        break
                    }
                    if (nextTask?.isEmpty() != false) {
                        task.add(renderMarkdown("No more tasks to execute. Finishing Auto Plan Chat."))
                        break
                    }

                    val taskResults = mutableListOf<Pair<PlanTaskBase, Future<String>>>()
                    for ((index, currentTask) in nextTask.withIndex()) {
                        val currentTaskId = "task_${(thinkingStatus.get()!!.executionContext?.completedTasks?.size ?: 0) + index + 1}"
                        val taskExecutionTask = ui.newTask(false)
                        taskExecutionTask.add(
                            renderMarkdown(
                                "Executing task: `$currentTaskId` - ${currentTask.task_description}\n```json\n${
                                    JsonUtil.toJson(currentTask)
                                }\n```"
                            )
                        )
                        tabbedDisplay["Task Execution $currentTaskId"] = taskExecutionTask.placeholder
                        val future = executor.submit<String> {
                            try {
                                val api = api.getChildClient().apply {
                                    val createFile = task.createFile(".logs/api-${UUID.randomUUID()}.log")
                                    createFile.second?.apply {
                                        logStreams += this.outputStream().buffered()
                                        task.verbose("API log: <a href=\"file:///$this\">$this</a>")
                                    }
                                }
                                val taskImpl = TaskType.Companion.getImpl(coordinator.planSettings, currentTask)
                                val result = StringBuilder()
                                taskImpl.run(
                                    agent = coordinator,
                                    taskId = currentTaskId,
                                    messages = listOf(
                                        userMessage,
                                        "Current thinking status:\n${formatThinkingStatus(thinkingStatus.get()!!)}"
                                    ) + formatEvalRecords(),
                                    task = taskExecutionTask,
                                    api = api,
                                    resultFn = { result.append(it) }
                                )
                                result.toString()
                            } catch (e: Exception) {
                                taskExecutionTask.error(ui, e)
                                log.error("Error executing task", e)
                                "Error executing task: ${e.message}"
                            }
                        }
                        taskResults.add(Pair(currentTask, future))
                    }
                    val completedTasks = taskResults.map { (task, future) ->
                        val result = future.get()
                        ExecutionRecord(
                            time = Date(),
                            task = task,
                            result = result
                        )
                    }
                    executionRecords.addAll(completedTasks)

                    val thinkingStatusTask = ui.newTask(false).apply { tabbedDisplay["Thinking Status"] = placeholder }
                    //thinkingStatusTask.add(renderMarkdown("Updating Thinking Status:\n${formatThinkingStatus(thinkingStatus.get()!!)}"))
                    this.thinkingStatus.set(updateActor.answer(
                        initialPrompt("Current thinking status: ${formatThinkingStatus(this.thinkingStatus.get()!!)}") +
                                completedTasks.flatMap { record ->
                                    listOf(
                                        "Completed task: ${record.task?.task_description}",
                                        "Task result: ${record.result}"
                                    )
                                } + (currentUserMessage.get()?.let<String, List<String>> { listOf("User message: $it") } ?: listOf<String>()),
                        api
                    ).obj.apply<ThinkingStatus> {
                        this@AutoPlanChatApp.currentUserMessage.set(null)
                        knowledge?.facts?.apply {
                            this.addAll(completedTasks.mapIndexed { index, (task, result) ->
                                "Task ${(executionContext?.completedTasks?.size ?: 0) + index + 1} Result: $result"
                            })
                        }
                    })
                    thinkingStatusTask.complete(renderMarkdown("Updated Thinking Status:\n${formatThinkingStatus(thinkingStatus.get()!!)}"))
                }
            } catch (e: Exception) {
                task.error(ui, e)
                log.error("Error in startAutoPlanChat", e)
            } finally {
                val summaryTask = ui.newTask(false).apply { tabbedDisplay["Summary"] = placeholder }
                summaryTask.add(
                    renderMarkdown(
                        "Auto Plan Chat completed. Final thinking status:\n${thinkingStatus.get()?.let {
                                formatThinkingStatus(it)
                            } ?: "null"
                        }"))
                task.complete()
            }
        }

    }

    private fun formatEvalRecords(maxTotalLength: Int = maxTaskHistoryChars): List<String> {
        var currentLength = 0
        val formattedRecords = mutableListOf<String>()
        for (record in executionRecords.reversed()) {
            val formattedRecord = "Task ${executionRecords.indexOf(record) + 1} Result: ${record.result}"
            if (currentLength + formattedRecord.length > maxTotalLength) {
                formattedRecords.add("... (earlier records truncated)")
                break
            }
            formattedRecords.add(0, formattedRecord)
            currentLength += formattedRecord.length
        }
        return formattedRecords
    }

    private fun formatThinkingStatus(thinkingStatus: ThinkingStatus) = """
```json
${JsonUtil.toJson(thinkingStatus)}
```
"""


    protected open fun initialPrompt(userMessage: String): List<String> = listOf(userMessage) + contextData()
    protected open fun contextData(): List<String> = emptyList()
}