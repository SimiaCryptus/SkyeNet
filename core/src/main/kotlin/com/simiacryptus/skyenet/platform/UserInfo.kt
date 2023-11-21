package com.simiacryptus.skyenet.platform

data class UserInfo(
    val email: String,
    val id: String? = null, // TODO: Remove default value
    val name: String? = null, // TODO: Remove default value
    val picture: String? = null, // TODO: Remove default value
)