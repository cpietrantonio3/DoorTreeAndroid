package codewhale.doortreeandroid

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Icon
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import codewhale.doortreeandroid.ui.theme.DoorTreeTheme

@Composable
fun QuickActionCard(
    item: QuickActionItem,
    badge: Pair<StatusBadgeStyle, String>? = null,
    notificationCount: Int = 0,
    height: Dp? = null,
    action: () -> Unit
) {
    val cardModifier = Modifier
        .fillMaxWidth()
        .then(if (height != null) Modifier.height(height) else Modifier)
        .clip(RoundedCornerShape(18.dp))
        .glassCard(cornerRadius = 18.dp)
        .clickable(onClick = action)

    Box(
        modifier = cardModifier
    ) {
        Column(
            modifier = Modifier.padding(if (height != null) 10.dp else 12.dp),
            verticalArrangement = Arrangement.spacedBy(if (height != null) 10.dp else 12.dp)
        ) {
            Box(
                modifier = Modifier
                    .height(if (height != null) 44.dp else 52.dp)
                    .fillMaxWidth(),
                contentAlignment = Alignment.TopStart
            ) {
                Box(
                    modifier = Modifier
                        .clip(RoundedCornerShape(16.dp))
                        .background(item.iconBackground)
                        .padding(12.dp)
                ) {
                    Icon(
                        imageVector = systemIcon(item.icon),
                        contentDescription = null,
                        tint = item.iconColor
                    )
                }
            }

            Column(verticalArrangement = Arrangement.spacedBy(4.dp)) {
                Text(text = item.title, color = DoorTreeTheme.textPrimary)
                Text(text = item.subtitle, color = DoorTreeTheme.textSecondary)
            }

            Spacer(modifier = Modifier.weight(1f, fill = true))

            if (badge != null) {
                StatusBadge(status = badge.first, label = badge.second)
            }
        }

        if (notificationCount > 0) {
            NotificationCountBadge(
                count = notificationCount,
                modifier = Modifier
                    .align(Alignment.TopEnd)
                    .padding(top = 8.dp, end = 8.dp)
            )
        }
    }
}
