package codewhale.doortreeandroid

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import codewhale.doortreeandroid.ui.theme.DoorTreeTheme

@Composable
fun LoginView(authSession: AuthSessionStore) {
    var showEmailAuth by remember { mutableStateOf(false) }
    var authAlertMessage by remember { mutableStateOf("") }

    Box(modifier = Modifier.fillMaxSize()) {
        AuthBackground()

        Column(
            modifier = Modifier
                .fillMaxSize()
                .topSafeAreaPadding()
                .padding(horizontal = 24.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.SpaceBetween
        ) {
            Spacer(modifier = Modifier.size(1.dp))

            Column(
                horizontalAlignment = Alignment.CenterHorizontally,
                verticalArrangement = Arrangement.spacedBy(28.dp)
            ) {
                Column(
                    horizontalAlignment = Alignment.CenterHorizontally,
                    verticalArrangement = Arrangement.spacedBy(20.dp)
                ) {
                    DoorTreeLogoLockup(width = 220.dp)
                    Column(
                        horizontalAlignment = Alignment.CenterHorizontally,
                        verticalArrangement = Arrangement.spacedBy(6.dp)
                    ) {
                        Text(text = L("auth.welcome.title"), color = DoorTreeTheme.textPrimary)
                        Text(text = L("auth.welcome.subtitle"), color = DoorTreeTheme.textSecondary)
                    }
                }

                if (!authSession.pendingVerificationEmail.isNullOrBlank()) {
                    VerificationReminder(
                        email = authSession.pendingVerificationEmail.orEmpty(),
                        onClick = { showEmailAuth = true }
                    )
                }

                GradientButton(
                    title = L("auth.email.continue"),
                    icon = "envelope.fill",
                    onClick = { showEmailAuth = true }
                )
            }

            Box(modifier = Modifier.fillMaxWidth().padding(bottom = 22.dp)) {
                PoweredByCodeWhaleFooter()
            }
        }

        if (showEmailAuth) {
            EmailAuthView(
                authSession = authSession,
                initialEmail = authSession.pendingVerificationEmail
                    ?: authSession.pendingFirstLoginResetEmail
                    ?: "",
                onDismiss = { showEmailAuth = false }
            )
        }

        if (authSession.isAuthenticating) {
            AuthLoadingOverlay(
                title = L("auth.signing_in.title"),
                subtitle = L("auth.signing_in.subtitle")
            )
        }
    }

    if (authAlertMessage.isNotBlank()) {
        AlertDialog(
            onDismissRequest = { authAlertMessage = "" },
            confirmButton = {
                TextButton(onClick = { authAlertMessage = "" }) {
                    Text(L("common.ok"))
                }
            },
            title = { Text(L("auth.alert.title")) },
            text = { Text(authAlertMessage) }
        )
    }
}

@Composable
private fun VerificationReminder(email: String, onClick: () -> Unit) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .liquidGlassSurface(cornerRadius = 20.dp, interactive = true, tint = DoorTreeTheme.gradientStart.copy(alpha = 0.14f))
            .clickable(onClick = onClick)
            .padding(horizontal = 16.dp, vertical = 14.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(12.dp)
    ) {
        androidx.compose.material3.Icon(
            imageVector = systemIcon("envelope.badge.fill"),
            contentDescription = null,
            tint = DoorTreeTheme.gradientStart
        )
        Column(
            modifier = Modifier.weight(1f),
            verticalArrangement = Arrangement.spacedBy(3.dp)
        ) {
            Text(text = L("auth.finish_verifying"), color = DoorTreeTheme.textPrimary)
            Text(text = email, color = DoorTreeTheme.textSecondary)
        }
        androidx.compose.material3.Icon(
            imageVector = systemIcon("chevron.right"),
            contentDescription = null,
            tint = DoorTreeTheme.textSecondary
        )
    }
}
