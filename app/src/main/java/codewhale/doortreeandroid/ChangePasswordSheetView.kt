package codewhale.doortreeandroid

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import kotlinx.coroutines.delay
import codewhale.doortreeandroid.ui.theme.DoorTreeTheme

@Composable
fun ChangePasswordSheetView(
    authSession: AuthSessionStore,
    prefilledEmail: String,
    onDismiss: () -> Unit
) {
    var email by rememberSaveable { mutableStateOf(prefilledEmail) }
    var errorMessage by rememberSaveable { mutableStateOf("") }
    var successMessage by rememberSaveable { mutableStateOf("") }
    var cooldownRemaining by rememberSaveable { mutableIntStateOf(0) }

    LaunchedEffect(cooldownRemaining) {
        if (cooldownRemaining > 0) {
            delay(1000)
            cooldownRemaining -= 1
        }
    }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(DoorTreeTheme.backgroundPrimary)
            .verticalScroll(rememberScrollState())
            .topSafeAreaPadding()
            .padding(horizontal = DoorTreeTheme.screenHorizontalPadding, vertical = 24.dp),
        verticalArrangement = Arrangement.spacedBy(16.dp)
    ) {
        Row {
            Column(modifier = Modifier.weight(1f), verticalArrangement = Arrangement.spacedBy(6.dp)) {
                Text(text = L("profile.change_password"), color = DoorTreeTheme.textPrimary)
                Text(
                    text = "We’ll send a password reset link to the email on your account.",
                    color = DoorTreeTheme.textSecondary
                )
            }
            HeaderIconButton(systemName = "xmark", onClick = onDismiss)
        }

        Column(
            modifier = Modifier
                .fillMaxWidth()
                .glassCard(cornerRadius = 22.dp)
                .padding(18.dp),
            verticalArrangement = Arrangement.spacedBy(14.dp)
        ) {
            Text(text = "Account email", color = DoorTreeTheme.textSecondary)
            GlassInputField(
                value = email,
                onValueChange = { email = it },
                placeholder = L("common.email"),
                leadingIcon = "envelope.fill"
            )
            Text(
                text = "If you don't see the reset email, check your Junk Mail inbox.",
                color = DoorTreeTheme.textSecondary
            )
            if (errorMessage.isNotBlank()) {
                Text(text = errorMessage, color = DoorTreeTheme.destructive)
            }
            if (successMessage.isNotBlank()) {
                Text(text = successMessage, color = DoorTreeTheme.gradientStart)
            }
            GradientButton(
                title = L("auth.reset.action"),
                icon = "paperplane.fill",
                enabled = !authSession.isAuthenticating && cooldownRemaining == 0,
                onClick = {
                    errorMessage = ""
                    successMessage = ""
                    authSession.sendPasswordReset(email) { error ->
                        if (!error.isNullOrBlank()) {
                            errorMessage = error
                        } else {
                            successMessage = LF("auth.reset.sent", email.trim().lowercase())
                            cooldownRemaining = 30
                        }
                    }
                }
            )
            if (cooldownRemaining > 0) {
                Text(text = LF("auth.reset.cooldown", cooldownRemaining), color = DoorTreeTheme.textSecondary)
            }
            if (successMessage.isNotBlank()) {
                Text(
                    text = L("common.close"),
                    color = DoorTreeTheme.textPrimary,
                    modifier = Modifier
                        .fillMaxWidth()
                        .liquidGlassSurface(cornerRadius = 18.dp, interactive = true)
                        .clickable(onClick = onDismiss)
                        .padding(vertical = 16.dp)
                )
            }
        }
    }
}
