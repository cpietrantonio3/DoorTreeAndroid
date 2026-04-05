package codewhale.doortreeandroid

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.Row
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.TextButton
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.dp
import com.google.firebase.database.FirebaseDatabase
import codewhale.doortreeandroid.ui.theme.DoorTreeAndroidTheme
import codewhale.doortreeandroid.ui.theme.DoorTreeTheme
import kotlinx.coroutines.launch
import kotlinx.coroutines.tasks.await

@Composable
fun DoorTreeAndroidApp() {
    val context = LocalContext.current
    remember { Localization.ensureLoaded(context); true }

    val authSession = remember { AuthSessionStore(context) }
    val tenantDataStore = remember { TenantDataStore(authSession) }
    val coroutineScope = rememberCoroutineScope()
    var showSplash by remember { mutableStateOf(true) }
    var showEulaDialog by remember { mutableStateOf(false) }
    var didCheckEula by remember { mutableStateOf(false) }
    var didHandleEulaDecision by remember { mutableStateOf(false) }
    var hasAcceptedEula by remember { mutableStateOf(false) }
    val database = remember { FirebaseDatabase.getInstance().reference }

    LaunchedEffect(authSession.user?.uid) {
        tenantDataStore.handleAuthState(authSession.user?.uid)
        val uid = authSession.user?.uid?.trim().orEmpty()
        if (uid.isEmpty()) {
            didCheckEula = true
            didHandleEulaDecision = false
            hasAcceptedEula = true
            showEulaDialog = false
        } else {
            didCheckEula = false
            didHandleEulaDecision = false
            hasAcceptedEula = false
            showEulaDialog = false

            val accepted = runCatching {
                val snapshot = database.child("users").child(uid).child("EULA").get().await()
                when (val value = snapshot.value) {
                    is Boolean -> value
                    is String -> value.trim().lowercase() in setOf("1", "true", "yes", "agreed")
                    else -> false
                }
            }.getOrDefault(false)

            didCheckEula = true
            hasAcceptedEula = accepted
            showEulaDialog = !accepted
        }
    }

    DoorTreeAndroidTheme {
        Box(
            modifier = Modifier
                .fillMaxSize()
                .background(DoorTreeTheme.backgroundPrimary)
        ) {
            when {
                showSplash -> SplashLoadingView(onFinish = { showSplash = false })
                authSession.isRestoringSession -> AuthLoadingOverlay(
                    title = L("auth.restoring.title"),
                    subtitle = L("auth.restoring.subtitle")
                )
                authSession.user != null && !didCheckEula -> AuthLoadingOverlay(
                    title = "Checking agreement",
                    subtitle = "Reviewing your access requirements."
                )
                authSession.user != null && !hasAcceptedEula -> AuthLoadingOverlay(
                    title = "Agreement required",
                    subtitle = "Please review and accept the agreement to continue."
                )
                authSession.user != null && tenantDataStore.isLoading && tenantDataStore.tenantRecord == null -> AuthLoadingOverlay(
                    title = L("Loading your account"),
                    subtitle = L("Loading your tenant profile.")
                )
                authSession.user != null && tenantDataStore.tenantRecord != null -> ContentView(
                    authSession = authSession,
                    tenantDataStore = tenantDataStore
                )
                authSession.user != null -> TenantLoadFailureView(
                    message = tenantDataStore.loadError ?: L("We couldn't load your tenant profile."),
                    onRetry = tenantDataStore::reload,
                    onSignOut = authSession::signOut
                )
                else -> LoginView(authSession = authSession)
            }
        }

        if (showEulaDialog) {
            AlertDialog(
                onDismissRequest = {
                    if (!didHandleEulaDecision) {
                        didHandleEulaDecision = true
                        val uid = authSession.user?.uid?.trim().orEmpty()
                        if (uid.isNotEmpty()) {
                            coroutineScope.launch {
                                runCatching {
                                    database.child("users").child(uid).child("EULA").setValue(false).await()
                                }
                            }
                        }
                        hasAcceptedEula = false
                        showEulaDialog = false
                        authSession.signOut()
                    }
                },
                title = { Text(L("eula")) },
                text = { Text(L("eula_text")) },
                confirmButton = {
                    TextButton(
                        onClick = {
                            didHandleEulaDecision = true
                            val uid = authSession.user?.uid?.trim().orEmpty()
                            if (uid.isNotEmpty()) {
                                coroutineScope.launch {
                                    runCatching {
                                        database.child("users").child(uid).child("EULA").setValue(true).await()
                                    }
                                }
                            }
                            hasAcceptedEula = true
                            showEulaDialog = false
                        }
                    ) {
                        Text(L("agree"))
                    }
                },
                dismissButton = {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.End
                    ) {
                        TextButton(
                            onClick = {
                                didHandleEulaDecision = true
                                val uid = authSession.user?.uid?.trim().orEmpty()
                                if (uid.isNotEmpty()) {
                                    coroutineScope.launch {
                                        runCatching {
                                            database.child("users").child(uid).child("EULA").setValue(false).await()
                                        }
                                    }
                                }
                                hasAcceptedEula = false
                                showEulaDialog = false
                                authSession.signOut()
                            }
                        ) {
                            Text(L("disagree"), color = DoorTreeTheme.destructive)
                        }

                        TextButton(
                            onClick = {
                                didHandleEulaDecision = true
                                val uid = authSession.user?.uid?.trim().orEmpty()
                                if (uid.isNotEmpty()) {
                                    coroutineScope.launch {
                                        runCatching {
                                            database.child("users").child(uid).child("EULA").setValue(false).await()
                                        }
                                    }
                                }
                                hasAcceptedEula = false
                                showEulaDialog = false
                                authSession.signOut()
                            }
                        ) {
                            Text(L("cancel"))
                        }
                    }
                }
            )
        }
    }
}

@Composable
private fun TenantLoadFailureView(
    message: String,
    onRetry: () -> Unit,
    onSignOut: () -> Unit
) {
    Column(
        modifier = Modifier
            .fillMaxSize()
            .topSafeAreaPadding()
            .padding(DoorTreeTheme.screenHorizontalPadding),
        verticalArrangement = Arrangement.Center,
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        Column(
            modifier = Modifier
                .glassCard(cornerRadius = 24.dp)
                .padding(24.dp),
            verticalArrangement = Arrangement.spacedBy(18.dp),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            Text(text = L("Tenant data unavailable"), color = DoorTreeTheme.textPrimary)
            Text(text = message, color = DoorTreeTheme.textSecondary)
            GradientButton(title = L("Try again"), onClick = onRetry)
            Text(
                text = L("profile.sign_out"),
                color = DoorTreeTheme.destructive,
                modifier = Modifier.clickable(onClick = onSignOut)
            )
        }
    }
}
