package codewhale.doortreeandroid

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.Icon
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.input.PasswordVisualTransformation
import androidx.compose.ui.text.input.VisualTransformation
import androidx.compose.ui.unit.dp
import codewhale.doortreeandroid.ui.theme.DoorTreeTheme

private enum class EmailAuthRoute {
    Form,
    CreateInfo,
    ForgotPassword,
    VerifyEmail,
    RequiredReset
}

@Composable
fun EmailAuthView(
    authSession: AuthSessionStore,
    initialEmail: String = "",
    onDismiss: () -> Unit
) {
    var email by rememberSaveable { mutableStateOf(initialEmail) }
    var password by rememberSaveable { mutableStateOf("") }
    var errorMessage by rememberSaveable { mutableStateOf("") }
    var showPassword by rememberSaveable { mutableStateOf(false) }
    var verificationEmail by rememberSaveable { mutableStateOf("") }
    var requiredResetEmail by rememberSaveable { mutableStateOf(authSession.pendingFirstLoginResetEmail ?: "") }
    var route by rememberSaveable { mutableStateOf(EmailAuthRoute.Form) }

    LaunchedEffect(authSession.pendingVerificationEmail) {
        val pending = authSession.pendingVerificationEmail
        if (!pending.isNullOrBlank()) {
            verificationEmail = pending
            email = pending
            route = EmailAuthRoute.VerifyEmail
        }
    }

    LaunchedEffect(authSession.pendingFirstLoginResetEmail) {
        val pending = authSession.pendingFirstLoginResetEmail
        if (!pending.isNullOrBlank()) {
            requiredResetEmail = pending
            route = EmailAuthRoute.RequiredReset
        }
    }

    Box(modifier = Modifier.fillMaxSize().background(DoorTreeTheme.backgroundPrimary)) {
        when (route) {
            EmailAuthRoute.Form -> EmailAuthForm(
                email = email,
                password = password,
                showPassword = showPassword,
                errorMessage = errorMessage,
                onEmailChange = {
                    email = it
                    if (errorMessage.isNotBlank()) errorMessage = ""
                },
                onPasswordChange = {
                    password = it
                    if (errorMessage.isNotBlank()) errorMessage = ""
                },
                onShowPasswordToggle = { showPassword = !showPassword },
                onDismiss = onDismiss,
                onCreateAccount = { route = EmailAuthRoute.CreateInfo },
                onForgotPassword = { route = EmailAuthRoute.ForgotPassword },
                onContinue = {
                    authSession.signInWithEmail(email, password) { error ->
                        when {
                            !authSession.pendingVerificationEmail.isNullOrBlank() -> {
                                verificationEmail = authSession.pendingVerificationEmail.orEmpty()
                                route = EmailAuthRoute.VerifyEmail
                            }
                            !authSession.pendingFirstLoginResetEmail.isNullOrBlank() -> {
                                requiredResetEmail = authSession.pendingFirstLoginResetEmail.orEmpty()
                                route = EmailAuthRoute.RequiredReset
                            }
                            !error.isNullOrBlank() -> {
                                errorMessage = error
                            }
                            else -> onDismiss()
                        }
                    }
                }
            )

            EmailAuthRoute.CreateInfo -> CreateAccountView(onDismiss = { route = EmailAuthRoute.Form })
            EmailAuthRoute.ForgotPassword -> ForgotPasswordView(
                authSession = authSession,
                prefilledEmail = email,
                onDismiss = { route = EmailAuthRoute.Form }
            )
            EmailAuthRoute.VerifyEmail -> VerifyEmailView(
                authSession = authSession,
                email = verificationEmail,
                onVerified = {
                    if (!authSession.pendingFirstLoginResetEmail.isNullOrBlank()) {
                        requiredResetEmail = authSession.pendingFirstLoginResetEmail.orEmpty()
                        route = EmailAuthRoute.RequiredReset
                    } else {
                        route = EmailAuthRoute.Form
                    }
                },
                onDismiss = { route = EmailAuthRoute.Form }
            )
            EmailAuthRoute.RequiredReset -> ForgotPasswordView(
                authSession = authSession,
                prefilledEmail = requiredResetEmail,
                isMandatoryFirstLogin = true,
                onFinished = {
                    email = requiredResetEmail
                    password = ""
                    errorMessage = ""
                },
                onDismiss = {
                    route = EmailAuthRoute.Form
                    onDismiss()
                }
            )
        }

        if (authSession.isAuthenticating) {
            AuthLoadingOverlay(
                title = L("auth.signing_in.title"),
                subtitle = L("auth.signing_in.subtitle")
            )
        }
    }
}

@Composable
private fun EmailAuthForm(
    email: String,
    password: String,
    showPassword: Boolean,
    errorMessage: String,
    onEmailChange: (String) -> Unit,
    onPasswordChange: (String) -> Unit,
    onShowPasswordToggle: () -> Unit,
    onDismiss: () -> Unit,
    onCreateAccount: () -> Unit,
    onForgotPassword: () -> Unit,
    onContinue: () -> Unit
) {
    AuthBackground()

        Column(
            modifier = Modifier
                .fillMaxSize()
                .topSafeAreaPadding()
                .padding(horizontal = 24.dp, vertical = 24.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.SpaceBetween
        ) {
        Column(modifier = Modifier.fillMaxWidth(), verticalArrangement = Arrangement.spacedBy(24.dp)) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically
            ) {
                Row(
                    modifier = Modifier.clickable(onClick = onDismiss),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(6.dp)
                ) {
                    Icon(systemIcon("chevron.left"), contentDescription = null, tint = DoorTreeTheme.textPrimary)
                    Text(text = L("common.back"), color = DoorTreeTheme.textPrimary)
                }
            }

            Spacer(modifier = Modifier.weight(1f, fill = false))

            Column(
                modifier = Modifier.fillMaxWidth(),
                horizontalAlignment = Alignment.CenterHorizontally,
                verticalArrangement = Arrangement.spacedBy(22.dp)
            ) {
                DoorTreeLogoLockup(width = 160.dp)

                Column(horizontalAlignment = Alignment.CenterHorizontally, verticalArrangement = Arrangement.spacedBy(6.dp)) {
                    Text(text = L("auth.email.title"), color = DoorTreeTheme.textPrimary)
                    Text(text = L("auth.email.subtitle"), color = DoorTreeTheme.textSecondary)
                }

                Column(modifier = Modifier.fillMaxWidth(), verticalArrangement = Arrangement.spacedBy(12.dp)) {
                    GlassInputField(
                        value = email,
                        onValueChange = onEmailChange,
                        placeholder = L("auth.email.placeholder"),
                        leadingIcon = "envelope.fill"
                    )
                    GlassInputField(
                        value = password,
                        onValueChange = onPasswordChange,
                        placeholder = L("auth.password.placeholder"),
                        leadingIcon = "lock.fill",
                        visualTransformation = if (showPassword) VisualTransformation.None else PasswordVisualTransformation(),
                        trailingContent = {
                            Icon(
                                imageVector = systemIcon(if (showPassword) "eye.slash" else "eye"),
                                contentDescription = null,
                                tint = DoorTreeTheme.textSecondary,
                                modifier = Modifier.clickable(onClick = onShowPasswordToggle)
                            )
                        }
                    )
                    if (errorMessage.isNotBlank()) {
                        Text(text = errorMessage, color = DoorTreeTheme.destructive)
                    }
                }

                GradientButton(title = L("common.continue"), onClick = onContinue)

                Row(horizontalArrangement = Arrangement.spacedBy(18.dp)) {
                    Text(
                        text = L("auth.create_account.link"),
                        color = DoorTreeTheme.textSecondary,
                        modifier = Modifier.clickable(onClick = onCreateAccount)
                    )
                    Text(
                        text = L("auth.forgot_password.link"),
                        color = DoorTreeTheme.textSecondary,
                        modifier = Modifier.clickable(onClick = onForgotPassword)
                    )
                }
            }
        }
    }
}
