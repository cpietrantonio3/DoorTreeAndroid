package codewhale.doortreeandroid

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.dp
import codewhale.doortreeandroid.ui.theme.DoorTreeAndroidTheme
import codewhale.doortreeandroid.ui.theme.DoorTreeTheme

@Composable
fun DoorTreeAndroidApp() {
    val context = LocalContext.current
    remember { Localization.ensureLoaded(context); true }

    val authSession = remember { AuthSessionStore(context) }
    val tenantDataStore = remember { TenantDataStore(authSession) }
    var showSplash by remember { mutableStateOf(true) }

    LaunchedEffect(authSession.user?.uid) {
        tenantDataStore.handleAuthState(authSession.user?.uid)
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
