package codewhale.doortreeandroid

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
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import codewhale.doortreeandroid.ui.theme.DoorTreeTheme

@Composable
fun CreateAccountView(onDismiss: () -> Unit) {
    Box(modifier = Modifier.fillMaxSize()) {
        AuthBackground()
            Column(
                modifier = Modifier
                    .fillMaxSize()
                    .topSafeAreaPadding()
                    .padding(horizontal = 24.dp, vertical = 48.dp),
                verticalArrangement = Arrangement.spacedBy(16.dp)
            ) {
            Text(text = L("auth.account_setup.title"), color = DoorTreeTheme.textPrimary)
            Text(text = L("auth.account_setup.subtitle"), color = DoorTreeTheme.textSecondary)

            CreateAccountInfoRow(
                icon = "person.crop.rectangle.stack.fill",
                title = L("auth.account_setup.step1.title"),
                detail = L("auth.account_setup.step1.detail")
            )
            CreateAccountInfoRow(
                icon = "envelope.badge.fill",
                title = L("auth.account_setup.step2.title"),
                detail = L("auth.account_setup.step2.detail")
            )
            CreateAccountInfoRow(
                icon = "key.fill",
                title = L("auth.account_setup.step3.title"),
                detail = L("auth.account_setup.step3.detail")
            )

            GradientButton(title = L("common.ok"), onClick = onDismiss)
        }
    }
}

@Composable
private fun CreateAccountInfoRow(
    icon: String,
    title: String,
    detail: String
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .liquidGlassSurface(cornerRadius = 20.dp)
            .padding(14.dp),
        horizontalArrangement = Arrangement.spacedBy(12.dp)
    ) {
        Box(
            modifier = Modifier
                .liquidGlassSurface(cornerRadius = 14.dp, tint = DoorTreeTheme.gradientStart.copy(alpha = 0.16f))
                .padding(10.dp)
        ) {
            Icon(systemIcon(icon), contentDescription = null, tint = DoorTreeTheme.gradientStart)
        }
        Column(verticalArrangement = Arrangement.spacedBy(4.dp)) {
            Text(text = title, color = DoorTreeTheme.textPrimary)
            Text(text = detail, color = DoorTreeTheme.textSecondary)
        }
    }
}
