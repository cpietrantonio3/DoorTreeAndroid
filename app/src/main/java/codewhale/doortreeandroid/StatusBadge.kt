package codewhale.doortreeandroid

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.dp
import codewhale.doortreeandroid.ui.theme.DoorTreeTheme

@Composable
fun StatusBadge(
    status: StatusBadgeStyle,
    label: String? = null,
    foregroundOverride: Color? = null,
    backgroundOverride: Color? = null
) {
    Text(
        text = label ?: status.localizedLabel,
        color = foregroundOverride ?: foreground(status),
        modifier = Modifier
            .background(backgroundOverride ?: background(status), RoundedCornerShape(999.dp))
            .padding(horizontal = 9.dp, vertical = 5.dp)
    )
}

private fun background(status: StatusBadgeStyle): Color = when (status) {
    StatusBadgeStyle.Due, StatusBadgeStyle.InProgress -> DoorTreeTheme.dueBackground
    StatusBadgeStyle.Paid, StatusBadgeStyle.Completed -> DoorTreeTheme.paidBackground
    StatusBadgeStyle.Pending -> DoorTreeTheme.pendingBackground
}

private fun foreground(status: StatusBadgeStyle): Color = when (status) {
    StatusBadgeStyle.Due, StatusBadgeStyle.InProgress -> DoorTreeTheme.dueText
    StatusBadgeStyle.Paid, StatusBadgeStyle.Completed -> DoorTreeTheme.paidText
    StatusBadgeStyle.Pending -> DoorTreeTheme.pendingText
}
