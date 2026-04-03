package codewhale.doortreeandroid

import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable
import kotlinx.serialization.json.Json
import kotlinx.serialization.json.JsonElement
import kotlinx.serialization.json.JsonObject
import kotlinx.serialization.json.JsonPrimitive
import kotlinx.serialization.json.booleanOrNull
import kotlinx.serialization.json.buildJsonObject
import kotlinx.serialization.json.jsonObject
import kotlinx.serialization.json.jsonPrimitive
import okhttp3.FormBody
import okhttp3.MediaType.Companion.toMediaType
import okhttp3.OkHttpClient
import okhttp3.Request
import okhttp3.RequestBody.Companion.toRequestBody
import java.io.IOException
import java.net.URLEncoder
import java.nio.charset.StandardCharsets

class FirebaseRestException(
    val code: String,
    message: String
) : IOException(message)

class FirebaseRestClient {
    private val json = Json { ignoreUnknownKeys = true }
    private val client = OkHttpClient()
    private val jsonType = "application/json; charset=utf-8".toMediaType()

    suspend fun signInWithEmail(email: String, password: String): AuthResponse {
        return postJson(
            "https://identitytoolkit.googleapis.com/v1/accounts:signInWithPassword?key=${FirebaseConfig.apiKey}",
            buildJsonObject {
                put("email", JsonPrimitive(email))
                put("password", JsonPrimitive(password))
                put("returnSecureToken", JsonPrimitive(true))
            }
        )
    }

    suspend fun createEmailAccount(email: String, password: String): AuthResponse {
        return postJson(
            "https://identitytoolkit.googleapis.com/v1/accounts:signUp?key=${FirebaseConfig.apiKey}",
            buildJsonObject {
                put("email", JsonPrimitive(email))
                put("password", JsonPrimitive(password))
                put("returnSecureToken", JsonPrimitive(true))
            }
        )
    }

    suspend fun sendVerificationEmail(idToken: String) {
        postNoResponse(
            "https://identitytoolkit.googleapis.com/v1/accounts:sendOobCode?key=${FirebaseConfig.apiKey}",
            buildJsonObject {
                put("requestType", JsonPrimitive("VERIFY_EMAIL"))
                put("idToken", JsonPrimitive(idToken))
            }
        )
    }

    suspend fun sendPasswordReset(email: String) {
        postNoResponse(
            "https://identitytoolkit.googleapis.com/v1/accounts:sendOobCode?key=${FirebaseConfig.apiKey}",
            buildJsonObject {
                put("requestType", JsonPrimitive("PASSWORD_RESET"))
                put("email", JsonPrimitive(email))
            }
        )
    }

    suspend fun lookupAccount(idToken: String): LookupResponse {
        return postJson(
            "https://identitytoolkit.googleapis.com/v1/accounts:lookup?key=${FirebaseConfig.apiKey}",
            buildJsonObject {
                put("idToken", JsonPrimitive(idToken))
            }
        )
    }

    suspend fun refreshToken(refreshToken: String): TokenRefreshResponse {
        val formBody = FormBody.Builder()
            .add("grant_type", "refresh_token")
            .add("refresh_token", refreshToken)
            .build()

        return executeRequest(
            Request.Builder()
                .url("https://securetoken.googleapis.com/v1/token?key=${FirebaseConfig.apiKey}")
                .post(formBody)
                .build()
        ) { body ->
            json.decodeFromString<TokenRefreshResponse>(body)
        }
    }

    suspend fun fetchUser(uid: String, idToken: String): JsonElement? {
        val token = URLEncoder.encode(idToken, StandardCharsets.UTF_8.toString())
        return executeNullableJsonGet("${FirebaseConfig.databaseUrl}/users/$uid.json?auth=$token")
    }

    suspend fun fetchMaintenanceRequests(uid: String, idToken: String): JsonElement? {
        val token = URLEncoder.encode(idToken, StandardCharsets.UTF_8.toString())
        return executeNullableJsonGet("${FirebaseConfig.databaseUrl}/users/$uid/maintenance/requests.json?auth=$token")
    }

    suspend fun fetchUsers(idToken: String): JsonElement? {
        val token = URLEncoder.encode(idToken, StandardCharsets.UTF_8.toString())
        return executeNullableJsonGet("${FirebaseConfig.databaseUrl}/users.json?auth=$token")
    }

    suspend fun fetchIsFirstLoginComplete(uid: String, idToken: String): Boolean {
        val snapshot = fetchUser(uid, idToken)?.jsonObject ?: return false
        return snapshot["isFirstLoginComplete"]?.jsonPrimitive?.booleanOrNull ?: false
    }

    suspend fun markFirstLoginComplete(uid: String, idToken: String): Boolean {
        val token = URLEncoder.encode(idToken, StandardCharsets.UTF_8.toString())
        return executeBooleanWrite(
            url = "${FirebaseConfig.databaseUrl}/users/$uid/isFirstLoginComplete.json?auth=$token",
            method = "PUT",
            body = JsonPrimitive(true).toString()
        )
    }

    suspend fun updateNotificationSetting(
        uid: String,
        idToken: String,
        key: String,
        value: Boolean
    ): Boolean {
        val token = URLEncoder.encode(idToken, StandardCharsets.UTF_8.toString())
        val body = buildJsonObject {
            put(key, JsonPrimitive(value))
        }.toString()
        return executeBooleanWrite(
            url = "${FirebaseConfig.databaseUrl}/users/$uid/notificationSettings.json?auth=$token",
            method = "PATCH",
            body = body
        )
    }

    suspend fun patchDatabaseRoot(
        idToken: String,
        body: JsonObject
    ): Boolean {
        val token = URLEncoder.encode(idToken, StandardCharsets.UTF_8.toString())
        return executeBooleanWrite(
            url = "${FirebaseConfig.databaseUrl}/.json?auth=$token",
            method = "PATCH",
            body = body.toString()
        )
    }

    private suspend inline fun <reified T> postJson(url: String, body: JsonObject): T {
        return executeRequest(
            Request.Builder()
                .url(url)
                .post(body.toString().toRequestBody(jsonType))
                .build()
        ) { rawBody ->
            json.decodeFromString<T>(rawBody)
        }
    }

    private suspend fun postNoResponse(url: String, body: JsonObject) {
        executeRequest(
            Request.Builder()
                .url(url)
                .post(body.toString().toRequestBody(jsonType))
                .build()
        ) { }
    }

    private suspend fun executeNullableJsonGet(url: String): JsonElement? {
        return executeRequest(
            Request.Builder()
                .url(url)
                .get()
                .build()
        ) { body ->
            val element = json.parseToJsonElement(body)
            if (element is JsonPrimitive && element.isString && element.content == "null") null else element
        }
    }

    private suspend fun executeBooleanWrite(url: String, method: String, body: String): Boolean {
        return executeRequest(
            Request.Builder()
                .url(url)
                .method(method, body.toRequestBody(jsonType))
                .build()
        ) { true }
    }

    private suspend fun <T> executeRequest(request: Request, mapper: (String) -> T): T {
        return withContext(Dispatchers.IO) {
            client.newCall(request).execute().use { response ->
                val rawBody = response.body?.string().orEmpty()
                if (!response.isSuccessful) {
                    throw parseError(rawBody, response.message)
                }
                mapper(rawBody)
            }
        }
    }

    private fun parseError(rawBody: String, fallbackMessage: String): FirebaseRestException {
        val code = runCatching {
            val element = json.parseToJsonElement(rawBody)
            element.jsonObject["error"]
                ?.jsonObject
                ?.get("message")
                ?.toString()
                ?.trim('"')
        }.getOrNull().orEmpty()
        val normalized = if (code.isBlank()) "UNKNOWN" else code
        return FirebaseRestException(normalized, if (normalized == "UNKNOWN") fallbackMessage else normalized)
    }
}

@Serializable
data class AuthResponse(
    val email: String = "",
    val localId: String = "",
    val idToken: String = "",
    val refreshToken: String = "",
    val expiresIn: String = ""
)

@Serializable
data class TokenRefreshResponse(
    @SerialName("refresh_token") val refreshToken: String,
    @SerialName("user_id") val userId: String,
    @SerialName("id_token") val idToken: String,
    @SerialName("expires_in") val expiresIn: String
)

@Serializable
data class LookupResponse(
    val users: List<LookupUser> = emptyList()
)

@Serializable
data class LookupUser(
    val localId: String = "",
    val email: String = "",
    val emailVerified: Boolean = false,
    val providerUserInfo: List<ProviderUserInfo> = emptyList()
)

@Serializable
data class ProviderUserInfo(
    val providerId: String = ""
)
