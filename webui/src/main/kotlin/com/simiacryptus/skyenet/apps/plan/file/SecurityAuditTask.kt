package com.simiacryptus.skyenet.apps.plan.file

import com.simiacryptus.jopenai.describe.Description
import com.simiacryptus.skyenet.apps.plan.PlanSettings
import com.simiacryptus.skyenet.apps.plan.TaskType
import com.simiacryptus.skyenet.apps.plan.file.SecurityAuditTask.SecurityAuditTaskConfigData
import org.slf4j.LoggerFactory

class SecurityAuditTask(
  planSettings: PlanSettings,
  planTask: SecurityAuditTaskConfigData?
) : AbstractAnalysisTask<SecurityAuditTaskConfigData>(planSettings, planTask) {


  class SecurityAuditTaskConfigData(
    @Description("List of files to be audited")
    val filesToAudit: List<String>? = null,
    @Description("Specific areas of focus for the security audit")
    val focusAreas: List<String>? = null,
    task_description: String? = null,
    task_dependencies: List<String>? = null,
    input_files: List<String>? = null,
    output_files: List<String>? = null,
    state: TaskState? = null
  ) : FileTaskConfigBase(
    task_type = TaskType.SecurityAudit.name,
    task_description = task_description,
    task_dependencies = task_dependencies,
    input_files = input_files,
    output_files = output_files,
    state = state
  )

  override val actorName: String = "SecurityAudit"
  override val actorPrompt: String = """
Perform a comprehensive security audit for the provided code files. Analyze the code for:
1. Potential security vulnerabilities
2. Insecure coding practices
3. Compliance with security standards and best practices
4. Proper handling of sensitive data
5. Authentication and authorization issues
6. Input validation and sanitization

Provide a detailed audit report with specific examples and recommendations for each issue found.
Format the response as a markdown document with appropriate headings and code snippets.
Use diff format to show the proposed security fixes clearly.
    """.trimIndent()

  override fun getAnalysisInstruction(): String = "Perform a security audit on the following code"

  override fun promptSegment(): String {
    return """
SecurityAudit - Perform an automated security audit and provide suggestions for improving code security
  ** Specify the files to be audited
  ** Optionally provide specific areas of focus for the security audit
        """.trimMargin()
  }


  companion object {
    private val log = LoggerFactory.getLogger(SecurityAuditTask::class.java)
  }
}