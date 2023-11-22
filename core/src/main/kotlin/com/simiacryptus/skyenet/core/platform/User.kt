package com.simiacryptus.skyenet.core.platform

import com.fasterxml.jackson.annotation.JsonProperty

data class User(
    @JsonProperty("email") internal val email: String,
    @JsonProperty("name") internal val name: String? = null,
    @JsonProperty("id") internal val id: String? = null,
    @JsonProperty("picture") internal val picture: String? = null,
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