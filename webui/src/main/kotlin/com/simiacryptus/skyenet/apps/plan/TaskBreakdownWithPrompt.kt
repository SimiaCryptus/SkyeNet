package com.simiacryptus.skyenet.apps.plan

data class TaskBreakdownWithPrompt(
    val prompt: String,
    val plan: Map<String, PlanTaskBase>,
    val planText: String
)