package com.simiacryptus.skyenet.core.platform

import com.fasterxml.jackson.annotation.JsonProperty
import com.simiacryptus.jopenai.ApiModel
import com.simiacryptus.jopenai.models.OpenAIModel
import com.simiacryptus.skyenet.core.platform.file.*
import java.io.File
import java.util.*
import java.util.concurrent.atomic.AtomicInteger

object ApplicationServices {
    var isLocked: Boolean = false
        set(value) {
            require(!isLocked) { "ApplicationServices is locked" }
            field = value
        }
    var usageManager: UsageInterface = UsageManager()
        set(value) {
            require(!isLocked) { "ApplicationServices is locked" }
            field = value
        }
    var authorizationManager: AuthorizationInterface = AuthorizationManager()
        set(value) {
            require(!isLocked) { "ApplicationServices is locked" }
            field = value
        }
    var userSettingsManager: UserSettingsInterface = UserSettingsManager()
        set(value) {
            require(!isLocked) { "ApplicationServices is locked" }
            field = value
        }
    var authenticationManager: AuthenticationInterface = AuthenticationManager()
        set(value) {
            require(!isLocked) { "ApplicationServices is locked" }
            field = value
        }
    var dataStorageFactory: (File) -> StorageInterface = { DataStorage(it) }
        set(value) {
            require(!isLocked) { "ApplicationServices is locked" }
            field = value
        }
    var clientManager: ClientManager = ClientManager()
        set(value) {
            require(!isLocked) { "ApplicationServices is locked" }
            field = value
        }

}

interface AuthenticationInterface {
    fun getUser(accessToken: String?): User?

    fun putUser(accessToken: String, user: User): User
    fun logout(accessToken: String, user: User)

    companion object {
        const val AUTH_COOKIE = "sessionId"
    }
}

interface AuthorizationInterface {
    enum class OperationType {
        Read,
        Write,
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

interface StorageInterface {
    fun <T> getJson(
        user: User?,
        session: Session,
        filename: String,
        clazz: Class<T>
    ): T?

    fun getMessages(
        user: User?,
        session: Session
    ): LinkedHashMap<String, String>

    fun getSessionDir(
        user: User?,
        session: Session
    ): File

    fun getSessionName(
        user: User?,
        session: Session
    ): String

    fun getSessionTime(
        user: User?,
        session: Session
    ): Date?

    fun listSessions(
        user: User?
    ): List<Session>

    fun <T : Any> setJson(
        user: User?,
        session: Session,
        filename: String,
        settings: T
    ): T

    fun updateMessage(
        user: User?,
        session: Session,
        messageId: String,
        value: String
    )

    fun listSessions(dir: File): List<String>
    fun userRoot(user: User?): File
    fun deleteSession(user: User?, session: Session)

    companion object {

        fun validateSessionId(
            session: Session
        ) {
            if (!session.sessionId.matches("""([GU]-)?\d{8}-\w{8}""".toRegex())) {
                throw IllegalArgumentException("Invalid session ID: $session")
            }
        }

        fun newGlobalID(): Session {
            val uuid = UUID.randomUUID().toString().split("-").first()
            val yyyyMMdd = java.time.LocalDate.now().toString().replace("-", "")
            //log.debug("New ID: $yyyyMMdd-$uuid")
            return Session("G-$yyyyMMdd-$uuid")
        }

        fun newUserID(): Session {
            val uuid = UUID.randomUUID().toString().split("-").first()
            val yyyyMMdd = java.time.LocalDate.now().toString().replace("-", "")
            //log.debug("New ID: $yyyyMMdd-$uuid")
            return Session("U-$yyyyMMdd-$uuid")
        }

    }
}

interface UserSettingsInterface {
    data class UserSettings(
        val apiKey: String = "",
    )

    fun getUserSettings(user: User): UserSettings

    fun updateUserSettings(user: User, settings: UserSettings)
}


interface UsageInterface {
    fun incrementUsage(session: Session, user: User?, model: OpenAIModel, tokens: ApiModel.Usage)

    fun getUserUsageSummary(user: User): Map<OpenAIModel, ApiModel.Usage>

    fun getSessionUsageSummary(session: Session): Map<OpenAIModel, ApiModel.Usage>

    data class UsageKey(
        val session: Session,
        val user: User?,
        val model: OpenAIModel,
    )

    class UsageValues(
        val inputTokens: AtomicInteger = AtomicInteger(),
        val outputTokens: AtomicInteger = AtomicInteger(),
    ) {
        fun addAndGet(tokens: ApiModel.Usage) {
            inputTokens.addAndGet(tokens.prompt_tokens)
            outputTokens.addAndGet(tokens.completion_tokens)
        }

        fun toUsage() = ApiModel.Usage(
            prompt_tokens = inputTokens.get(),
            completion_tokens = outputTokens.get()
        )
    }

    class UsageCounters(
        val tokensPerModel: HashMap<UsageKey, UsageValues> = HashMap(),
    )
}


data class User(
    @get:JsonProperty("email") val email: String,
    @get:JsonProperty("name") val name: String? = null,
    @get:JsonProperty("id") val id: String? = null,
    @get:JsonProperty("picture") val picture: String? = null,
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

data class Session(
    internal val sessionId: String
) {
    init {
        StorageInterface.validateSessionId(this)
    }

    override fun toString() = sessionId
}


