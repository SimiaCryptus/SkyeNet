package com.simiacryptus.skyenet.webui.test

import com.simiacryptus.diff.addApplyFileDiffLinks
import com.simiacryptus.jopenai.API
import com.simiacryptus.jopenai.models.ApiModel
import com.simiacryptus.jopenai.OpenAIClient
import com.simiacryptus.skyenet.core.actors.CodingActor
import com.simiacryptus.skyenet.core.actors.CodingActor.Companion.indent
import com.simiacryptus.skyenet.core.platform.ApplicationServices
import com.simiacryptus.skyenet.core.platform.AuthorizationInterface.OperationType
import com.simiacryptus.skyenet.core.platform.ClientManager
import com.simiacryptus.skyenet.core.platform.Session
import com.simiacryptus.skyenet.core.platform.User
import com.simiacryptus.skyenet.webui.application.ApplicationInterface
import com.simiacryptus.skyenet.webui.application.ApplicationServer
import com.simiacryptus.skyenet.webui.application.ApplicationSocketManager
import com.simiacryptus.skyenet.webui.session.SocketManager
import com.simiacryptus.skyenet.util.MarkdownUtil.renderMarkdown
import org.slf4j.LoggerFactory
import java.awt.Desktop
import java.nio.file.Files
import java.util.*

open class FilePatchTestApp(
    applicationName: String = "FilePatchTestApp",
    val api: API = OpenAIClient()
) : ApplicationServer(
    applicationName = applicationName,
    path = "/codingActorTest",
) {
    override fun newSession(user: User?, session: Session): SocketManager {
        val socketManager = super.newSession(user, session)
        val ui = (socketManager as ApplicationSocketManager).applicationInterface
        val task = ui.newTask(true)

        val source = """
            |fun main(args: Array<String>) {
            |    println(${'"'}""
            |        Hello, World!  
            |    ${'"'}"")
            |}
        """.trimMargin()
        val sourceFile = Files.createTempFile("source", ".txt").toFile()
        sourceFile.writeText(source)
        sourceFile.deleteOnExit()
        //Desktop.getDesktop().open(sourceFile)

        val patch = """
            |# ${sourceFile.name}
            |
            |```diff
            |-Hello, World!
            |+Goodbye, World!
            |```
        """.trimMargin()
        val newPatch = socketManager.addApplyFileDiffLinks(sourceFile.toPath().parent, patch, {}, ui, api)
        task.complete(renderMarkdown(newPatch, ui = ui))

        return socketManager
    }

    companion object {
        private val log = LoggerFactory.getLogger(FilePatchTestApp::class.java)
    }

}