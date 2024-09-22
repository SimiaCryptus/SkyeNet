package com.simiacryptus.skyenet.apps.plan

import com.simiacryptus.jopenai.API
import com.simiacryptus.jopenai.describe.Description
import com.simiacryptus.jopenai.models.ApiModel
import com.simiacryptus.skyenet.apps.coding.CodingAgent
import com.simiacryptus.skyenet.apps.plan.RunShellCommandTask.RunShellCommandTaskData
import com.simiacryptus.skyenet.core.actors.CodingActor
import com.simiacryptus.skyenet.interpreter.ProcessInterpreter
import com.simiacryptus.skyenet.webui.session.SessionTask
import com.simiacryptus.util.JsonUtil
import org.slf4j.LoggerFactory
import java.io.File
import java.util.concurrent.Semaphore
import kotlin.reflect.KClass

class RunShellCommandTask(
    planSettings: PlanSettings,
    planTask: RunShellCommandTaskData?
) : AbstractTask<RunShellCommandTaskData>(planSettings, planTask) {

    class RunShellCommandTaskData(
        @Description("The shell command to be executed")
        val command: String? = null,
        @Description("The working directory for the command execution")
        val workingDir: String? = null,
        task_type: String? = null,
        task_description: String? = null,
        task_dependencies: List<String>? = null,
        input_files: List<String>? = null,
        output_files: List<String>? = null,
        state: TaskState? = null
    ) : PlanTaskBase(
        task_type = task_type,
        task_description = task_description,
        task_dependencies = task_dependencies,
        input_files = input_files,
        output_files = output_files,
        state = state
    )

    val shellCommandActor by lazy {
        CodingActor(
            name = "RunShellCommand",
            interpreterClass = ProcessInterpreter::class,
            details = """
Execute the following shell command(s) and provide the output. Ensure to handle any errors or exceptions gracefully.
Note: This task is for running simple and safe commands. Avoid executing commands that can cause harm to the system or compromise security.
            """.trimMargin(),
            symbols = mapOf<String, Any>(
                "env" to (planSettings.env ?: emptyMap()),
                "workingDir" to (planTask?.workingDir?.let { File(it).absolutePath } ?: File(
                    planSettings.workingDir
                ).absolutePath),
                "language" to (planSettings.language ?: "bash"),
                "command" to (planTask?.command ?: planSettings.command),
            ),
            model = planSettings.getTaskSettings(planTask?.task_type!! as TaskType<*>).model
                ?: planSettings.defaultModel,
            temperature = planSettings.temperature,
        )
    }

    override fun promptSegment(): String {
        return """
            |RunShellCommand - Execute shell commands and provide the output
            |  ** Specify the command to be executed, or describe the task to be performed
            |  ** List input files/tasks to be examined when writing the command
            |  ** Optionally specify a working directory for the command execution
            """.trimMargin()
    }

    override fun run(
        agent: PlanCoordinator,
        taskId: String,
        userMessage: String,
        plan: Map<String, PlanTaskBase>,
        planProcessingState: PlanProcessingState,
        task: SessionTask,
        api: API
    ) {
        val semaphore = Semaphore(0)
        object : CodingAgent<ProcessInterpreter>(
            api = api,
            dataStorage = agent.dataStorage,
            session = agent.session,
            user = agent.user,
            ui = agent.ui,
            interpreter = shellCommandActor.interpreterClass as KClass<ProcessInterpreter>,
            symbols = shellCommandActor.symbols,
            temperature = shellCommandActor.temperature,
            details = shellCommandActor.details,
            model = shellCommandActor.model,
            mainTask = task,
        ) {
            override fun displayFeedback(
                task: SessionTask,
                request: CodingActor.CodeRequest,
                response: CodingActor.CodeResult
            ) {
                val formText = StringBuilder()
                var formHandle: StringBuilder? = null
                formHandle = task.add(
                    """
 <div style="display: flex;flex-direction: column;">
 ${if (!super.canPlay) "" else super.playButton(task, request, response, formText) { formHandle!! }}
 ${acceptButton(response)}
 </div>
 ${super.reviseMsg(task, request, response, formText) { formHandle!! }}
                    """.trimMargin(), className = "reply-message"
                )
                formText.append(formHandle.toString())
                formHandle.toString()
                task.complete()
            }

            fun acceptButton(
                response: CodingActor.CodeResult
            ): String {
                return ui.hrefLink("Accept", "href-link play-button") {
                    planProcessingState.taskResult[taskId] = response.let {
                        """
                        |## Shell Command Output
                        |
                        |$TRIPLE_TILDE
                        |${response.code}
                        |$TRIPLE_TILDE
                        |
                        |${TRIPLE_TILDE}
                        |${response.renderedResponse}
                        |${TRIPLE_TILDE}
                        |
                        """.trimMargin()
                    }
                    semaphore.release()
                }
            }
        }.apply<CodingAgent<ProcessInterpreter>> {
            start(
                codeRequest(
                    listOf(
                        userMessage to ApiModel.Role.user,
                        JsonUtil.toJson(plan) to ApiModel.Role.assistant,
                        getPriorCode(planProcessingState) to ApiModel.Role.assistant,
                        getInputFileCode() to ApiModel.Role.assistant,
                    )
                )
            )
        }
        try {
            semaphore.acquire()
        } catch (e: Throwable) {
            log.warn("Error", e)
        }
        log.debug("Completed shell command: $taskId")
    }

    companion object {
        private val log = LoggerFactory.getLogger(RunShellCommandTask::class.java)
    }
}