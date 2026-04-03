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
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import kotlinx.coroutines.delay
import codewhale.doortreeandroid.ui.theme.DoorTreeTheme

@Composable
fun ForgotPasswordView(
    authSession: AuthSessionStore,
    prefilledEmail: String,
    isMandatoryFirstLogin: Boolean = false,
    onFinished: (() -> Unit)? = null,
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

    Box(modifier = Modifier.fillMaxSize().background(DoorTreeTheme.backgroundPrimary)) {
        AuthBackground()
            Column(
                modifier = Modifier
                    .fillMaxSize()
                    .topSafeAreaPadding()
                    .padding(20.dp),
                verticalArrangement = Arrangement.spacedBy(14.dp)
            ) {
            Text(
                text = if (isMandatoryFirstLogin) L("auth.first_login.reset.title") else L("auth.reset.title"),
                color = DoorTreeTheme.textPrimary
            )
            Text(
                text = if (isMandatoryFirstLogin) L("auth.first_login.reset.subtitle") else L("auth.reset.subtitle"),
                color = DoorTreeTheme.textSecondary
            )
            GlassInputField(
                value = email,
                onValueChange = { email = it },
                placeholder = L("common.email"),
                leadingIcon = "envelope.fill"
            )
            if (errorMessage.isNotBlank()) {
                Text(text = errorMessage, color = DoorTreeTheme.destructive)
            }
            if (successMessage.isNotBlank()) {
                Text(text = successMessage, color = DoorTreeTheme.gradientStart)
            }
            GradientButton(
                title = L("auth.reset.action"),
                enabled = !authSession.isAuthenticating && cooldownRemaining == 0,
                onClick = {
                    errorMessage = ""
                    successMessage = ""
                    authSession.sendPasswordReset(email) { error ->
                        if (!error.isNullOrBlank()) {
                            errorMessage = error
                            return@sendPasswordReset
                        }
                        if (isMandatoryFirstLogin) {
                            authSession.beginFirstLoginResetRequirement { resetError ->
                                if (!resetError.isNullOrBlank()) {
                                    errorMessage = resetError
                                } else {
                                    successMessage = LF("auth.reset.sent", email.trim().lowercase())
                                    cooldownRemaining = 30
                                }
                            }
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

            if (isMandatoryFirstLogin || successMessage.isNotBlank()) {
                Text(
                    text = if (isMandatoryFirstLogin) L("auth.verify.back_to_login") else L("common.close"),
                    color = DoorTreeTheme.textPrimary,
                    modifier = Modifier
                        .fillMaxWidth()
                        .liquidGlassSurface(cornerRadius = 18.dp, interactive = true)
                        .clickable {
                            if (isMandatoryFirstLogin) {
                                authSession.signOut()
                            }
                            onFinished?.invoke()
                            onDismiss()
                        }
                        .padding(vertical = 16.dp)
                )
            } else {
                Text(
                    text = L("common.close"),
                    color = DoorTreeTheme.textSecondary,
                    modifier = Modifier.clickable(onClick = onDismiss)
                )
            }
        }
    }
}
