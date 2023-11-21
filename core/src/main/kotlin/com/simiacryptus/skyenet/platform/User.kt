package com.simiacryptus.skyenet.platform

data class User(
    internal val email: String,
    internal val name: String? = null,
    internal val id: String? = null,
    internal val picture: String? = null,
) {
    override fun toString() = email

    override fun equals(other: Any?): Boolean {
        if (this === other) return true
        if (javaClass != other?.javaClass) return false

        other as User

        return email == other.email
    }

    override fun hashCode(): Int {
        return email.hashCode()
    }

}