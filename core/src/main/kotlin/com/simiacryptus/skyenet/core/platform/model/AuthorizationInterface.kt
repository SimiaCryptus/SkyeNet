package com.simiacryptus.skyenet.core.platform.model

interface AuthorizationInterface {
    enum class OperationType {
        Read,
        Write,
        Public,
        Share,
        Execute,
        Delete,
        Admin,
        GlobalKey,
    }

    fun isAuthorized(
        applicationClass: Class<*>?,
        user: User?,
        operationType: OperationType,
    ): Boolean
}