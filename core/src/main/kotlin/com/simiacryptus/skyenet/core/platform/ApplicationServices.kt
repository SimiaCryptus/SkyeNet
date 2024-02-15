package com.simiacryptus.skyenet.core.platform

import com.fasterxml.jackson.annotation.JsonProperty
import com.google.common.util.concurrent.AtomicDouble
import com.simiacryptus.jopenai.ApiModel
import com.simiacryptus.jopenai.models.OpenAIModel
import com.simiacryptus.skyenet.core.platform.file.*
import com.simiacryptus.skyenet.core.util.Selenium
import java.io.File
import java.nio.ByteBuffer
import java.util.*
import java.util.concurrent.ThreadPoolExecutor
import java.util.concurrent.atomic.AtomicInteger
import kotlin.random.Random

object ApplicationServices {
    var isLocked: Boolean = false
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
    var dataStorageRoot: File = File(System.getProperty("user.home"), ".skyenet")
        set(value) {
            require(!isLocked) { "ApplicationServices is locked" }
            field = value
        }
    var clientManager: ClientManager = ClientManager()
        set(value) {
            require(!isLocked) { "ApplicationServices is locked" }
            field = value
        }

    var cloud: CloudPlatformInterface? = AwsPlatform.get()
        set(value) {
            require(!isLocked) { "ApplicationServices is locked" }
            field = value
        }


    var seleniumFactory: ((ThreadPoolExecutor, Array<out jakarta.servlet.http.Cookie>?) -> Selenium)? = null
        set(value) {
            require(!isLocked) { "ApplicationServices is locked" }
            field = value
        }
    var usageManager: UsageInterface = UsageManager(File(dataStorageRoot, ".skyenet/usage"))
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
  fun getMessageIds(
    user: User?,
    session: Session
  ): List<String>

  fun setMessageIds(
    user: User?,
    session: Session,
    ids: List<String>
  )

  companion object {

        fun validateSessionId(
            session: Session
        ) {
            if (!session.sessionId.matches("""([GU]-)?\d{8}-[\w+-.]{4}""".toRegex())) {
                throw IllegalArgumentException("Invalid session ID: $session")
            }
        }

        fun newGlobalID(): Session {
            val yyyyMMdd = java.time.LocalDate.now().toString().replace("-", "")
            //log.debug("New ID: $yyyyMMdd-$uuid")
            return Session("G-$yyyyMMdd-${id2()}")
        }

        fun long64() = Base64.getEncoder().encodeToString(ByteBuffer.allocate(8).putLong(Random.nextLong()).array())
          .toString().replace("=", "").replace("/", ".").replace("+", "-")

        fun newUserID(): Session {
            val yyyyMMdd = java.time.LocalDate.now().toString().replace("-", "")
            //log.debug("New ID: $yyyyMMdd-$uuid")
            return Session("U-$yyyyMMdd-${id2()}")
        }

      private fun id2() = long64().filter {
          when (it) {
              in 'a'..'z' -> true
              in 'A'..'Z' -> true
              in '0'..'9' -> true
              else -> false
          }
      }.take(4)

      fun parseSessionID(sessionID: String): Session {
            val session = Session(sessionID)
            validateSessionId(session)
            return session
        }

    }
}

interface UserSettingsInterface {
    data class UserSettings(
        val apiKey: String = "",
        val apiBase: String? = "https://api.openai.com/v1",
    )

    fun getUserSettings(user: User): UserSettings

    fun updateUserSettings(user: User, settings: UserSettings)
}


interface UsageInterface {
    fun incrementUsage(session: Session, user: User?, model: OpenAIModel, tokens: ApiModel.Usage) = incrementUsage(
        session, when (user) {
            null -> null
            else -> ApplicationServices.userSettingsManager.getUserSettings(user).apiKey
        }, model, tokens
    )
    fun incrementUsage(session: Session, apiKey: String?, model: OpenAIModel, tokens: ApiModel.Usage)

    fun getUserUsageSummary(user: User): Map<OpenAIModel, ApiModel.Usage> = getUserUsageSummary(
        ApplicationServices.userSettingsManager.getUserSettings(user).apiKey
    )
    fun getUserUsageSummary(apiKey: String): Map<OpenAIModel, ApiModel.Usage>

    fun getSessionUsageSummary(session: Session): Map<OpenAIModel, ApiModel.Usage>
    fun clear()

    data class UsageKey(
        val session: Session,
        val apiKey: String?,
        val model: OpenAIModel,
    )

    class UsageValues(
        val inputTokens: AtomicInteger = AtomicInteger(),
        val outputTokens: AtomicInteger = AtomicInteger(),
        val cost: AtomicDouble = AtomicDouble(),
    ) {
        fun addAndGet(tokens: ApiModel.Usage) {
            inputTokens.addAndGet(tokens.prompt_tokens)
            outputTokens.addAndGet(tokens.completion_tokens)
            cost.addAndGet(tokens.cost ?: 0.0)
        }

        fun toUsage() = ApiModel.Usage(
            prompt_tokens = inputTokens.get(),
            completion_tokens = outputTokens.get(),
            cost = cost.get()
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
    fun isGlobal(): Boolean = sessionId.startsWith("G-")
}


interface CloudPlatformInterface {
    val shareBase: String

    fun upload(
        path: String,
        contentType: String,
        bytes: ByteArray
    ) : String

    fun upload(
        path: String,
        contentType: String,
        request: String
    ) : String

    fun encrypt(fileBytes: ByteArray, keyId: String): String?
    fun decrypt(encryptedData: ByteArray): String
}
