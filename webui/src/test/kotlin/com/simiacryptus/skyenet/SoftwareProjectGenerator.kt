@file:Suppress("MemberVisibilityCanBePrivate")

package com.simiacryptus.skyenet

import com.fasterxml.jackson.annotation.JsonIgnore
import com.google.common.html.HtmlEscapers
import com.google.common.util.concurrent.Futures.allAsList
import com.google.common.util.concurrent.Futures.transformAsync
import com.google.common.util.concurrent.ListenableFuture
import com.google.common.util.concurrent.MoreExecutors
import com.simiacryptus.openai.OpenAIClient
import com.simiacryptus.openai.proxy.ChatProxy
import com.simiacryptus.util.describe.Description
import com.simiacryptus.skyenet.body.PersistentSessionBase
import com.simiacryptus.skyenet.body.SessionDataStorage
import com.simiacryptus.skyenet.body.SkyenetInterviewer
import com.simiacryptus.util.JsonUtil
import java.awt.Desktop
import java.net.URI
import java.util.*
import java.util.concurrent.Executors

object SoftwareProjectGenerator {

    @Description("A fully specified software project")
    data class ProjectSpecification(
        @Description("The name of the software project")
        val projectName: String? = null,
        @Description("The programming language used for the project")
        val language: String? = null,
        @Description("The project's target environment(s)")
        val environment: String? = null,
        @Description("Libraries used in the project, if any")
        val dependencies: List<String>? = null,
        @Description("A list of use cases for the project")
        val requirements: List<String>? = null,
    ) {

        @JsonIgnore
        fun validate(): List<String> {
            val errors = LinkedList<String>()
            if (null == requirements) errors.add("requirements list is null")
            if (null == projectName) errors.add("projectName is null")
            else if (projectName.isBlank()) errors.add("projectName is blank")
            if (null == language) errors.add("language is null")
            if (null == environment) errors.add("environment is null")
            else if (environment.isEmpty()) errors.add("environment is empty")
            if (null == dependencies) errors.add("dependencies is null")
            return errors
        }
    }

    interface SoftwareGenerator {
        fun generateProject(specification: ProjectSpecification): ProjectDesign
        fun implementFile(
            specification: ProjectSpecification,
            imports: List<FileImplementation>,
            file: FileSpecification,
        ): FileImplementation
    }

    data class ProjectDesign(
        val files: List<FileSpecification>? = null,
    )

    data class FileSpecification(
        @Description("Project-relative path to the file")
        val name: String? = null,
        @Description("A description of the file's purpose")
        val description: String? = null,
        @Description("The programming language used for the file")
        val language: String? = null,
        @Description("Functions and symbols defined in the file; implementation requirements")
        val features: List<String>? = null,
        @Description("A list of file paths that this file depends on; these files must be implemented first and will be passed to the implementation function")
        val imports: List<String>? = null,
    )

    data class FileImplementation(
        @Description("Project-relative path to the file")
        val name: String? = null,
        @Description("A list of all symbols (function signatures, classes, constants, etc) defined in the file that are available to other files")
        val exports: List<String>? = null,
        @Description("Fully-implemented file contents")
        val code: String? = null,
    )


    fun implementProject(sessionID: String, specification: ProjectSpecification, sessionDataStorage: SessionDataStorage) =
        object : PersistentSessionBase(UUID.randomUUID().toString(), sessionDataStorage) {

            val operationID = newID()
            var projectDesign: ProjectDesign? = null
            val fileIds = HashMap<String, String>()
            val futures = HashMap<String, ListenableFuture<FileImplementation>>()

            init {
                designProject()
            }

            fun showProgress(file: FileSpecification) {
                send(
                    """${fileIds.computeIfAbsent(file.name!!) { newID() }},<div>
                    |<h3>${file.name}</h3>
                    |<p>${file.description}</p>
                    |${interviewer.spinner}
                    |</div>""".trimMargin()
                )
            }

            fun implementFile(file: FileSpecification): ListenableFuture<FileImplementation> {
                val id: String = fileIds.computeIfAbsent(file.name!!) { newID() }
                return futures.computeIfAbsent(id) {
                    val importFutures = file.imports?.flatMap { import -> projectDesign!!.files!!.filter { it.name == import } }?.map(::implementFile) ?: listOf()
                    log.info("Initializing ${file.name} implementation task with ${importFutures.size} imports")
                    transformAsync(
                        allAsList(importFutures),
                        { imports ->
                            log.info("Submitting ${file.name} for implementation")
                            pool.submit<FileImplementation> {
                                log.info("Implementing ${file.name}")
                                val contents = generator.implementFile(specification, imports.map { it.copy(code = "") }, file)
                                log.info("Implemented ${file.name}")
                                send(
                                    """$id,<div>
                                    |<h3>${file.name}</h3>
                                    |<p>${file.description}</p>
                                    |<pre>
                                    |${JsonUtil.toJson(contents.exports ?: listOf<String>())}
                                    |</pre>
                                    |<pre><code class="language-${file.language?.lowercase()}">
                                    |${HtmlEscapers.htmlEscaper().escape(contents.code!!)}
                                    |</code></pre>
                                    |<button class="regen-button" data-id="$id">♲</button>
                                    |</div>""".trimMargin()
                                )
                                sessionDataStorage.getSessionDir(sessionID).resolve(file.name).writeText(contents.code)
                                contents
                            }
                        },
                        pool
                    )
                }
            }

            override fun onCmd(id: String, code: String) {
                if(id == operationID) {
                    if(code == "run") {
                        Thread {
                            send("""$operationID,<div>
                            |<pre>${JsonUtil.toJson(projectDesign!!)}</pre>
                            |</div>""".trimMargin())
                            projectDesign!!.files?.forEach(::showProgress)
                            projectDesign!!.files?.forEach(::implementFile)
                        }.start()
                    } else if(code == "regen") {
                        designProject()
                    }
                } else if (code == "regen") {
                    val fileName = fileIds.toList().find { it.second == id }?.first
                    if (null == fileName) log.warn("No file found for id $id")
                    else {
                        val fileSpecification = projectDesign?.files?.find { it.name == fileName }
                        if (null == fileSpecification) log.warn("No file specification found for $fileName")
                        else {
                            futures.remove(fileName)?.cancel(true)
                            showProgress(fileSpecification)
                            implementFile(fileSpecification)
                        }
                    }
                }
            }

            private fun designProject() {
                Thread {
                    send("""$operationID,<div>Designing Project... ${interviewer.spinner}</div>""")
                    projectDesign = generator.generateProject(specification)
                    send("""$operationID,<div>
                        |<pre>${JsonUtil.toJson(projectDesign!!)}</pre>
                        |<button class="play-button" data-id="$operationID">▶</button>
                        |<button class="regen-button" data-id="$operationID">♲</button>
                        |</div>""".trimMargin())
                }.start()
            }

            override fun run(userMessage: String) {
                TODO("Not yet implemented")
            }

        }

    private fun newID() = (0..5).map { ('a'..'z').random() }.joinToString("")

    val api = OpenAIClient(OpenAIClient.keyTxt)
    val log = org.slf4j.LoggerFactory.getLogger(SoftwareProjectGenerator::class.java)!!
    var sessionDataStorage: SessionDataStorage? = null
    const val port = 8081
    const val baseURL = "http://localhost:$port"
    val visiblePrompt = """
        |Hello! I am here to assist you in specifying a software project! 
        |I will guide you through a series of questions to gather the necessary information. 
        |Don't worry if you're not sure about any technical details; I'm here to help!
        |What would you like to build today?
        """.trimMargin()
    const val applicationName = "Project Specification Interviewer"
    var interviewer: SkyenetInterviewer<ProjectSpecification> = SkyenetInterviewer(
        applicationName = applicationName,
        baseURL = baseURL,
        dataClass = ProjectSpecification::class.java,
        visiblePrompt = visiblePrompt,
        continueSession = { sessionID, data -> implementProject(sessionID, data, sessionDataStorage!!) },
        validate = ProjectSpecification::validate,
        apiKey = OpenAIClient.keyTxt
    )
    val pool = MoreExecutors.listeningDecorator(Executors.newCachedThreadPool())
    val generator = ChatProxy(SoftwareGenerator::class.java, api).create()

    @JvmStatic
    fun main(args: Array<String>) {
        val httpServer = interviewer.start(port)
        sessionDataStorage = interviewer.sessionDataStorage
        Desktop.getDesktop().browse(URI(baseURL))
        httpServer.join()
    }


}