package com.simiacryptus.skyenet.apps.coding

import com.simiacryptus.jopenai.API
import com.simiacryptus.skyenet.core.Interpreter
import com.simiacryptus.skyenet.core.actors.ActorSystem
import com.simiacryptus.skyenet.core.actors.CodingActor
import com.simiacryptus.skyenet.core.actors.CodingActor.CodeResult
import com.simiacryptus.skyenet.core.platform.*
import com.simiacryptus.skyenet.core.platform.AuthorizationInterface.OperationType
import com.simiacryptus.skyenet.webui.application.ApplicationInterface
import com.simiacryptus.skyenet.webui.session.SessionTask
import com.simiacryptus.skyenet.webui.util.MarkdownUtil
import org.slf4j.LoggerFactory
import java.util.*
import kotlin.reflect.KClass

class CodingAgent<T:Interpreter>(
        val api: API,
        dataStorage: StorageInterface,
        session: Session,
        user: User?,
        val ui: ApplicationInterface,
        val interpreter: KClass<T>,
        val symbols: Map<String, Any>,
) : ActorSystem<CodingAgent.ActorTypes>(
    actorMap(interpreter, symbols), dataStorage, user, session
) {
    val actor by lazy { getActor(ActorTypes.CodingActor) as CodingActor }

    enum class ActorTypes {
        CodingActor
    }

    fun start(
        userMessage: String,
    ) {
        val message = ui.newTask()
        try {
            message.echo(MarkdownUtil.renderMarkdown(userMessage))
            val response = actor.answer(CodingActor.CodeRequest(listOf(userMessage)), api = api)
            displayCode(user, ui, message, response, userMessage, api)
        } catch (e: Throwable) {
            log.warn("Error", e)
            message.error(e)
        }
    }

    private fun displayCode(
        user: User?,
        ui: ApplicationInterface,
        task: SessionTask,
        response: CodeResult,
        userMessage: String,
        api: API
    ) {
        try {
            task.add(
                MarkdownUtil.renderMarkdown(
                    //language=Markdown
                    """
                |```${actor.language.lowercase(Locale.getDefault())}
                |${response.code}
                |```
                """.trimMargin().trim()
                )
            )

            val canPlay = ApplicationServices.authorizationManager.isAuthorized(
                this::class.java,
                user,
              OperationType.Execute
            )
            val playLink = task.add(if (!canPlay) "" else {
                ui.hrefLink("▶", "href-link play-button") {
                    val header = task.header("Running...")
                    try {
                        val result = response.result
                        header?.clear()
                        task.header("Result")
                        task.add(result.resultValue, tag = "pre")
                        task.header("Output")
                        task.add(result.resultOutput, tag = "pre")
                        task.complete()
                    } catch (e: Throwable) {
                        log.warn("Error", e)
                        task.error(e)
                    }
                }
            })

            var formHandle: StringBuilder? = null
            formHandle = task.add(ui.textInput { feedback ->
                try {
                    formHandle?.clear()
                    playLink?.clear()
                    task.echo(MarkdownUtil.renderMarkdown(feedback))
                    val revisedCode = actor.answer(CodingActor.CodeRequest(listOf(userMessage, response.code, feedback)), api = api)
                    displayCode(user, ui, task, revisedCode, userMessage, api)
                } catch (e: Throwable) {
                    log.warn("Error", e)
                    task.error(e)
                }
            })

            task.complete()
        } catch (e: Throwable) {
            log.warn("Error", e)
            task.error(e)
        }
    }

    companion object {
        private val log = LoggerFactory.getLogger(CodingAgent::class.java)

        fun <T:Interpreter> actorMap(interpreterKClass: KClass<T>, symbols: Map<String, Any>) = mapOf(
            ActorTypes.CodingActor to CodingActor(interpreterKClass, symbols = symbols)
        )
    }

}