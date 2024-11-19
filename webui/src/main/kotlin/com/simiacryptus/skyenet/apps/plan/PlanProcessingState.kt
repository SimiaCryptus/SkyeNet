package com.simiacryptus.skyenet.apps.plan

import com.simiacryptus.skyenet.webui.session.SessionTask
import java.util.concurrent.Future

data class PlanProcessingState(
    val subTasks: Map<String, TaskConfigBase>,
    val tasksByDescription: MutableMap<String?, TaskConfigBase> = subTasks.entries.toTypedArray()
    .associate { it.value.task_description to it.value }.toMutableMap(),
    val taskIdProcessingQueue: MutableList<String> = PlanUtil.executionOrder(subTasks).toMutableList(),
    val taskResult: MutableMap<String, String> = mutableMapOf(),
    val completedTasks: MutableList<String> = mutableListOf(),
    val taskFutures: MutableMap<String, Future<*>> = mutableMapOf(),
    val uitaskMap: MutableMap<String, SessionTask> = mutableMapOf()
)