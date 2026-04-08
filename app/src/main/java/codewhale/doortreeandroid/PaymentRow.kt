package codewhale.doortreeandroid

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.dp
import codewhale.doortreeandroid.ui.theme.DoorTreeTheme

@Composable
fun PaymentRow(
    payment: PaymentItem,
    titleOverride: String? = null,
    badgeForegroundOverride: Color? = null,
    badgeBackgroundOverride: Color? = null
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .liquidGlassSurface(cornerRadius = 14.dp)
            .padding(14.dp),
        horizontalArrangement = Arrangement.spacedBy(14.dp)
    ) {
        Column(verticalArrangement = Arrangement.spacedBy(6.dp)) {
            Text(text = titleOverride ?: payment.month, color = DoorTreeTheme.textPrimary)
            Text(text = payment.date, color = DoorTreeTheme.textSecondary)
        }

        Spacer(modifier = Modifier.weight(1f))

        Text(text = payment.amount, color = DoorTreeTheme.textPrimary)

        StatusBadge(
            status = payment.status,
            foregroundOverride = badgeForegroundOverride,
            backgroundOverride = badgeBackgroundOverride
        )
    }
}
