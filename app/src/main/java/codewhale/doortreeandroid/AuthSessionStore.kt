package codewhale.doortreeandroid

import android.content.Context
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import com.google.firebase.auth.FirebaseAuth
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.launch
import kotlinx.coroutines.tasks.await
import kotlinx.serialization.Serializable
import kotlinx.serialization.json.Json
import java.time.Instant

class AuthSessionStore(context: Context) {
    private val appContext = context.applicationContext
    private val prefs = appContext.getSharedPreferences("door_tree_auth", Context.MODE_PRIVATE)
    private val restClient = FirebaseRestClient()
    private val firebaseAuth = FirebaseAuth.getInstance()
    private val scope = CoroutineScope(SupervisorJob() + Dispatchers.Main.immediate)
    private val serializer = Json { ignoreUnknownKeys = true }

    private var currentSession: PersistedSession? = null
    private var pendingFirstLoginResetUID: String? = null

    var user by mutableStateOf<AuthUser?>(null)
        private set
    var isRestoringSession by mutableStateOf(true)
        private set
    var isAuthenticating by mutableStateOf(false)
        private set
    var pendingVerificationEmail by mutableStateOf<String?>(null)
    var pendingFirstLoginResetEmail by mutableStateOf<String?>(null)

    init {
        pendingFirstLoginResetUID = prefs.getString(KEY_PENDING_RESET_UID, null)
        pendingFirstLoginResetEmail = prefs.getString(KEY_PENDING_RESET_EMAIL, null)
        scope.launch {
            restoreSession()
        }
    }

    fun signOut() {
        firebaseAuth.signOut()
        currentSession = null
        prefs.edit()
            .remove(KEY_SESSION)
            .remove(KEY_PENDING_RESET_UID)
            .remove(KEY_PENDING_RESET_EMAIL)
            .apply()
        user = null
        pendingVerificationEmail = null
        pendingFirstLoginResetUID = null
        pendingFirstLoginResetEmail = null
        isAuthenticating = false
        isRestoringSession = false
    }

    fun signInWithEmail(email: String, password: String, completion: (String?) -> Unit) {
        val normalizedEmail = normalizeEmail(email)
        val trimmedPassword = password.trim()

        when {
            normalizedEmail.isBlank() -> {
                completion(L("auth.error.enter_email"))
                return
            }
            trimmedPassword.isBlank() -> {
                completion(L("auth.error.enter_password"))
                return
            }
        }

        isAuthenticating = true
        scope.launch {
            val message = runCatching {
                val response = restClient.signInWithEmail(normalizedEmail, password)
                firebaseAuth.signInWithEmailAndPassword(normalizedEmail, password).await()
                val lookup = restClient.lookupAccount(response.idToken).users.firstOrNull()
                currentSession = PersistedSession(
                    uid = response.localId,
                    email = response.email,
                    idToken = response.idToken,
                    refreshToken = response.refreshToken,
                    expiresAtEpochSeconds = Instant.now().epochSecond + response.expiresIn.toLong()
                )
                persistSession()

                when (resolveAccess(lookupEmail = lookup?.email ?: normalizedEmail, isSessionRestore = false)) {
                    AccessResolution.Allowed -> null
                    AccessResolution.NeedsFirstLoginReset -> null
                    AccessResolution.NeedsEmailVerification -> L("auth.verify.before_continue")
                    AccessResolution.Failure -> L("auth.error.sign_in_unavailable")
                }
            }.getOrElse { throwable ->
                readableMessage(throwable, fallbackKey = "auth.error.sign_in_unavailable")
            }

            isAuthenticating = false
            completion(message)
        }
    }

    fun createEmailAccount(email: String, password: String, completion: (String?) -> Unit) {
        val normalizedEmail = normalizeEmail(email)
        val trimmedPassword = password.trim()

        when {
            normalizedEmail.isBlank() -> {
                completion(L("auth.error.enter_email"))
                return
            }
            !normalizedEmail.contains("@") || !normalizedEmail.contains(".") -> {
                completion(L("auth.error.invalid_email"))
                return
            }
            trimmedPassword.isBlank() -> {
                completion(L("auth.error.enter_password"))
                return
            }
            trimmedPassword.length < 6 -> {
                completion(L("auth.error.weak_password"))
                return
            }
        }

        isAuthenticating = true
        scope.launch {
            val message = runCatching {
                val response = restClient.createEmailAccount(normalizedEmail, trimmedPassword)
                firebaseAuth.signInWithEmailAndPassword(normalizedEmail, trimmedPassword).await()
                restClient.sendVerificationEmail(response.idToken)
                pendingVerificationEmail = normalizedEmail
                signOut()
                null
            }.getOrElse { throwable ->
                readableMessage(throwable, fallbackKey = "auth.error.create_account_unavailable")
            }

            isAuthenticating = false
            completion(message)
        }
    }

    fun sendPasswordReset(email: String, completion: (String?) -> Unit) {
        val normalizedEmail = normalizeEmail(email)
        if (normalizedEmail.isBlank()) {
            completion(L("auth.error.enter_email"))
            return
        }

        isAuthenticating = true
        scope.launch {
            val message = runCatching {
                restClient.sendPasswordReset(normalizedEmail)
                null
            }.getOrElse { throwable ->
                readableMessage(throwable, fallbackKey = "auth.error.reset_unavailable")
            }

            isAuthenticating = false
            completion(message)
        }
    }

    fun resendVerificationEmail(completion: (String?) -> Unit) {
        val idToken = currentSession?.idToken
        if (idToken.isNullOrBlank()) {
            completion(L("auth.error.resend_requires_sign_in"))
            return
        }

        isAuthenticating = true
        scope.launch {
            val message = runCatching {
                restClient.sendVerificationEmail(idToken)
                null
            }.getOrElse { throwable ->
                readableMessage(throwable, fallbackKey = "auth.error.resend_verification_unavailable")
            }

            isAuthenticating = false
            completion(message)
        }
    }

    fun refreshVerificationStatus(completion: (Boolean, String?) -> Unit) {
        isAuthenticating = true
        scope.launch {
            val outcome = runCatching {
                val idToken = ensureValidIdToken() ?: return@runCatching false to L("auth.error.sign_in_again")
                val lookup = restClient.lookupAccount(idToken).users.firstOrNull()
                if (lookup?.emailVerified == true) {
                    when (resolveAccess(lookupEmail = lookup.email, isSessionRestore = false)) {
                        AccessResolution.Allowed,
                        AccessResolution.NeedsFirstLoginReset -> true to null
                        AccessResolution.NeedsEmailVerification -> false to L("auth.verify.not_verified_detailed")
                        AccessResolution.Failure -> false to L("auth.error.refresh_account_unavailable")
                    }
                } else {
                    false to L("auth.verify.not_verified")
                }
            }.getOrElse { throwable ->
                false to readableMessage(throwable, fallbackKey = "auth.error.refresh_account_unavailable")
            }

            isAuthenticating = false
            completion(outcome.first, outcome.second)
        }
    }

    fun beginFirstLoginResetRequirement(completion: (String?) -> Unit) {
        val session = currentSession
        val uid = pendingFirstLoginResetUID
        if (session == null || uid == null || session.uid != uid) {
            completion(L("auth.error.sign_in_again"))
            return
        }

        persistPendingFirstLoginResetRequest(uid = uid, email = session.email)
        signOut()
        completion(null)
    }

    suspend fun ensureValidIdToken(): String? {
        val session = currentSession ?: return null
        if (session.expiresAtEpochSeconds - Instant.now().epochSecond > 60) {
            return session.idToken
        }

        return runCatching {
            val refreshed = restClient.refreshToken(session.refreshToken)
            currentSession = session.copy(
                uid = refreshed.userId,
                idToken = refreshed.idToken,
                refreshToken = refreshed.refreshToken,
                expiresAtEpochSeconds = Instant.now().epochSecond + refreshed.expiresIn.toLong()
            )
            persistSession()
            currentSession?.idToken
        }.getOrNull()
    }

    private suspend fun restoreSession() {
        val serialized = prefs.getString(KEY_SESSION, null)
        if (serialized.isNullOrBlank()) {
            isRestoringSession = false
            return
        }

        val restored = runCatching { serializer.decodeFromString<PersistedSession>(serialized) }.getOrNull()
        if (restored == null) {
            signOut()
            return
        }

        currentSession = restored
        val idToken = ensureValidIdToken()
        if (idToken.isNullOrBlank()) {
            signOut()
            return
        }

        val lookupEmail = runCatching {
            restClient.lookupAccount(idToken).users.firstOrNull()?.email ?: restored.email
        }.getOrDefault(restored.email)

        resolveAccess(lookupEmail = lookupEmail, isSessionRestore = true)
    }

    private suspend fun resolveAccess(
        lookupEmail: String,
        isSessionRestore: Boolean
    ): AccessResolution {
        val session = currentSession ?: return AccessResolution.Failure.also {
            isRestoringSession = false
        }

        val idToken = ensureValidIdToken() ?: return AccessResolution.Failure.also {
            isRestoringSession = false
            user = null
        }

        val isFirstLoginComplete = runCatching {
            restClient.fetchIsFirstLoginComplete(session.uid, idToken)
        }.getOrElse {
            if (isSessionRestore) isRestoringSession = false
            user = null
            pendingVerificationEmail = null
            pendingFirstLoginResetEmail = null
            pendingFirstLoginResetUID = null
            return AccessResolution.Failure
        }

        if (isFirstLoginComplete) {
            clearPersistedFirstLoginResetRequest()
            pendingVerificationEmail = null
            pendingFirstLoginResetEmail = null
            pendingFirstLoginResetUID = null
            user = AuthUser(uid = session.uid, email = session.email)
            if (isSessionRestore) isRestoringSession = false
            return AccessResolution.Allowed
        }

        if (hasPersistedFirstLoginResetRequest(session.uid)) {
            val updated = runCatching {
                restClient.markFirstLoginComplete(session.uid, idToken)
            }.getOrDefault(false)
            if (updated) {
                clearPersistedFirstLoginResetRequest()
                pendingVerificationEmail = null
                pendingFirstLoginResetEmail = null
                pendingFirstLoginResetUID = null
                user = AuthUser(uid = session.uid, email = session.email)
                if (isSessionRestore) isRestoringSession = false
                return AccessResolution.Allowed
            }

            user = null
            if (isSessionRestore) isRestoringSession = false
            return AccessResolution.Failure
        }

        pendingVerificationEmail = null
        pendingFirstLoginResetUID = session.uid
        pendingFirstLoginResetEmail = lookupEmail.ifBlank { session.email }
        user = null
        if (isSessionRestore) isRestoringSession = false
        return AccessResolution.NeedsFirstLoginReset
    }

    private fun persistPendingFirstLoginResetRequest(uid: String, email: String?) {
        pendingFirstLoginResetUID = uid
        pendingFirstLoginResetEmail = email
        prefs.edit()
            .putString(KEY_PENDING_RESET_UID, uid)
            .putString(KEY_PENDING_RESET_EMAIL, email)
            .apply()
    }

    private fun clearPersistedFirstLoginResetRequest() {
        prefs.edit()
            .remove(KEY_PENDING_RESET_UID)
            .remove(KEY_PENDING_RESET_EMAIL)
            .apply()
    }

    private fun hasPersistedFirstLoginResetRequest(uid: String): Boolean {
        return prefs.getString(KEY_PENDING_RESET_UID, null) == uid
    }

    private fun persistSession() {
        val session = currentSession ?: return
        prefs.edit().putString(KEY_SESSION, serializer.encodeToString(PersistedSession.serializer(), session)).apply()
    }

    private fun readableMessage(throwable: Throwable, fallbackKey: String): String {
        val code = (throwable as? FirebaseRestException)?.code.orEmpty()
        return when (code) {
            "INVALID_LOGIN_CREDENTIALS", "INVALID_PASSWORD" -> L("auth.error.invalid_credentials")
            "EMAIL_NOT_FOUND" -> L("auth.error.no_account")
            "USER_DISABLED" -> L("auth.error.account_disabled")
            "EMAIL_EXISTS" -> L("auth.error.email_already_in_use")
            "WEAK_PASSWORD" -> L("auth.error.weak_password")
            "TOO_MANY_ATTEMPTS_TRY_LATER" -> L("auth.error.too_many_attempts")
            "INVALID_ID_TOKEN", "TOKEN_EXPIRED", "USER_NOT_FOUND" -> L("auth.error.sign_in_again")
            else -> L(fallbackKey)
        }
    }

    private fun normalizeEmail(value: String): String {
        return value.trim().lowercase()
    }

    @Serializable
    private data class PersistedSession(
        val uid: String,
        val email: String,
        val idToken: String,
        val refreshToken: String,
        val expiresAtEpochSeconds: Long
    )

    private enum class AccessResolution {
        Allowed,
        NeedsEmailVerification,
        NeedsFirstLoginReset,
        Failure
    }

    companion object {
        private const val KEY_SESSION = "door_tree_session"
        private const val KEY_PENDING_RESET_UID = "pendingFirstLoginResetUID"
        private const val KEY_PENDING_RESET_EMAIL = "pendingFirstLoginResetEmail"
    }
}
