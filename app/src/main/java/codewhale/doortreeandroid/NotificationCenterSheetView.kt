package codewhale.doortreeandroid

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.Icon
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.sp
import androidx.compose.ui.unit.dp
import codewhale.doortreeandroid.ui.theme.DoorTreeTheme

@Composable
fun NotificationCenterSheetView(
    tenantDataStore: TenantDataStore,
    onDismiss: () -> Unit
) {
    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(DoorTreeTheme.backgroundPrimary)
            .verticalScroll(rememberScrollState())
            .padding(
                start = DoorTreeTheme.screenHorizontalPadding,
                top = 8.dp,
                end = DoorTreeTheme.screenHorizontalPadding,
                bottom = 24.dp
            ),
        verticalArrangement = Arrangement.spacedBy(16.dp)
    ) {
        Row(verticalAlignment = Alignment.Top, horizontalArrangement = Arrangement.spacedBy(14.dp)) {
            Column(modifier = Modifier.weight(1f), verticalArrangement = Arrangement.spacedBy(6.dp)) {
                Text(
                    text = "Notifications",
                    color = DoorTreeTheme.textPrimary,
                    fontWeight = FontWeight.Bold,
                    fontSize = 28.sp
                )
                Text(
                    text = when (tenantDataStore.unreadNotificationCount) {
                        0 -> "All caught up."
                        1 -> "1 unread update"
                        else -> "${tenantDataStore.unreadNotificationCount} unread updates"
                    },
                    color = DoorTreeTheme.textSecondary
                )
            }
            HeaderIconButton(systemName = "xmark", onClick = onDismiss)
        }

        Row(
            modifier = Modifier
                .fillMaxWidth()
                .liquidGlassSurface(cornerRadius = 20.dp, tint = DoorTreeTheme.barGlassTint)
                .padding(horizontal = 14.dp, vertical = 12.dp),
            horizontalArrangement = Arrangement.spacedBy(12.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Box(
                modifier = Modifier
                    .background(DoorTreeTheme.gradientStart.copy(alpha = 0.18f), CircleShape)
                    .padding(10.dp)
            ) {
                Icon(
                    imageVector = systemIcon(if (tenantDataStore.unreadNotificationCount > 0) "bell.badge.fill" else "bell"),
                    contentDescription = null,
                    tint = DoorTreeTheme.gradientStart
                )
            }
            Column {
                Text(
                    text = when (tenantDataStore.notificationCenterItems.size) {
                        0 -> "Nothing new"
                        1 -> "1 recent notification"
                        else -> "${tenantDataStore.notificationCenterItems.size} recent notifications"
                    },
                    color = DoorTreeTheme.textPrimary
                )
                Text(
                    text = "Payment, lease, and account updates appear here.",
                    color = DoorTreeTheme.textSecondary
                )
            }
        }

        if (tenantDataStore.notificationCenterItems.isEmpty()) {
            SectionPlaceholder(
                systemName = "bell.slash",
                title = "No notifications right now",
                message = "New updates from your property account will appear here."
            )
        } else {
            tenantDataStore.notificationCenterItems.forEach { item ->
                NotificationCenterRow(item = item)
            }
        }
    }
}

@Composable
private fun NotificationCenterRow(item: NotificationCenterItem) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .liquidGlassSurface(
                cornerRadius = 22.dp,
                tint = if (item.isUnread) item.category.iconBackground.copy(alpha = 0.42f) else DoorTreeTheme.barGlassTint
            )
            .padding(14.dp),
        horizontalArrangement = Arrangement.spacedBy(12.dp),
        verticalAlignment = Alignment.Top
    ) {
        Box(
            modifier = Modifier
                .background(item.category.iconBackground, RoundedCornerShape(16.dp))
                .padding(12.dp)
        ) {
            Icon(systemIcon(item.category.icon), contentDescription = null, tint = item.category.iconColor)
        }

        Column(modifier = Modifier.weight(1f), verticalArrangement = Arrangement.spacedBy(6.dp)) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Text(text = item.title, color = DoorTreeTheme.textPrimary, modifier = Modifier.weight(1f))
                Text(text = item.timestamp, color = DoorTreeTheme.textSecondary)
            }
            Text(text = item.message, color = DoorTreeTheme.textSecondary)
            if (item.isUnread) {
                Text(
                    text = "Unread",
                    color = item.category.iconColor,
                    modifier = Modifier
                        .background(item.category.iconBackground.copy(alpha = 0.84f), RoundedCornerShape(999.dp))
                        .padding(horizontal = 10.dp, vertical = 6.dp)
                )
            }
        }
    }
}
