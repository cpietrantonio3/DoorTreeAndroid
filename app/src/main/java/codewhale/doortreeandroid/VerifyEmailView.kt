package codewhale.doortreeandroid

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.Icon
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import codewhale.doortreeandroid.ui.theme.DoorTreeTheme

@Composable
fun VerifyEmailView(
    authSession: AuthSessionStore,
    email: String,
    onVerified: () -> Unit,
    onDismiss: () -> Unit
) {
    var infoMessage by rememberSaveable { mutableStateOf("") }
    var errorMessage by rememberSaveable { mutableStateOf("") }

    Box(modifier = Modifier.fillMaxSize().background(DoorTreeTheme.backgroundPrimary)) {
        AuthBackground()
            Column(
                modifier = Modifier
                    .fillMaxSize()
                    .topSafeAreaPadding()
                    .padding(horizontal = 24.dp, vertical = 24.dp),
                verticalArrangement = Arrangement.spacedBy(16.dp)
            ) {
            Text(text = L("auth.verify.title"), color = DoorTreeTheme.textPrimary)
            Text(text = L("auth.verify.subtitle"), color = DoorTreeTheme.textSecondary)

            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .liquidGlassSurface(cornerRadius = 18.dp, tint = DoorTreeTheme.gradientStart.copy(alpha = 0.14f))
                    .padding(16.dp),
                horizontalArrangement = Arrangement.spacedBy(12.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                Icon(systemIcon("envelope.badge.fill"), contentDescription = null, tint = DoorTreeTheme.gradientStart)
                Text(text = email, color = DoorTreeTheme.textPrimary)
            }

            if (errorMessage.isNotBlank()) {
                Text(text = errorMessage, color = DoorTreeTheme.destructive)
            }
            if (infoMessage.isNotBlank()) {
                Text(text = infoMessage, color = DoorTreeTheme.gradientStart)
            }

            GradientButton(
                title = L("auth.verify.confirm"),
                onClick = {
                    errorMessage = ""
                    infoMessage = ""
                    authSession.refreshVerificationStatus { isVerified, message ->
                        if (isVerified) {
                            onVerified()
                        } else {
                            errorMessage = message ?: L("auth.verify.not_verified")
                        }
                    }
                }
            )

            Text(
                text = L("auth.verify.resend"),
                color = DoorTreeTheme.textPrimary,
                modifier = Modifier.clickable {
                    errorMessage = ""
                    infoMessage = ""
                    authSession.resendVerificationEmail { error ->
                        if (!error.isNullOrBlank()) {
                            errorMessage = error
                        } else {
                            infoMessage = LF("auth.verify.resent", email)
                        }
                    }
                }
            )

            Text(
                text = L("auth.verify.back_to_login"),
                color = DoorTreeTheme.textSecondary,
                modifier = Modifier.clickable {
                    authSession.signOut()
                    onDismiss()
                }
            )
        }
    }
}
