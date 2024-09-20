package com.simiacryptus.skyenet.apps.plan

class DisabledTaskException(taskType: TaskType<*>) : Exception("Task type $taskType is disabled")