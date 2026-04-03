package codewhale.doortreeandroid

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.Icon
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import codewhale.doortreeandroid.ui.theme.DoorTreeTheme

@Composable
fun ChatView(tenantDataStore: TenantDataStore) {
    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(DoorTreeTheme.backgroundPrimary)
            .topSafeAreaPadding()
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .background(DoorTreeTheme.barGlassTint)
                .padding(horizontal = DoorTreeTheme.screenHorizontalPadding, vertical = 12.dp),
            horizontalArrangement = Arrangement.spacedBy(12.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Box(
                modifier = Modifier
                    .size(42.dp)
                    .background(DoorTreeTheme.backgroundSecondary, CircleShape),
                contentAlignment = Alignment.Center
            ) {
                Text(text = tenantDataStore.propertyManagerInitials, color = DoorTreeTheme.textPrimary)
            }
            Column {
                Text(text = tenantDataStore.propertyManagerName, color = DoorTreeTheme.textPrimary)
                Text(text = L("chat.manager_role"), color = DoorTreeTheme.textSecondary)
            }
        }

        Column(
            modifier = Modifier
                .weight(1f)
                .fillMaxWidth()
                .verticalScroll(rememberScrollState())
                .padding(horizontal = DoorTreeTheme.screenHorizontalPadding, vertical = 18.dp),
            verticalArrangement = Arrangement.spacedBy(20.dp)
        ) {
            if (tenantDataStore.chatSections.isEmpty()) {
                SectionPlaceholder(
                    systemName = "bubble.left.and.exclamationmark.bubble.right",
                    title = "No messages yet",
                    message = "Conversations with your property manager will appear here once chat data is connected."
                )
            } else {
                tenantDataStore.chatSections.forEach { section ->
                    DateSeparator(title = section.title)
                    Column(verticalArrangement = Arrangement.spacedBy(14.dp)) {
                        section.messages.forEach { message ->
                            ChatBubble(
                                message = message,
                                managerInitials = tenantDataStore.propertyManagerInitials
                            )
                        }
                    }
                }
            }
        }

        Row(
            modifier = Modifier
                .fillMaxWidth()
                .background(DoorTreeTheme.barGlassTint)
                .padding(horizontal = DoorTreeTheme.screenHorizontalPadding, vertical = 12.dp),
            horizontalArrangement = Arrangement.spacedBy(10.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Row(
                modifier = Modifier
                    .weight(1f)
                    .liquidGlassSurface(cornerRadius = 14.dp, interactive = true)
                    .padding(horizontal = 14.dp, vertical = 14.dp),
                horizontalArrangement = Arrangement.spacedBy(10.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                Icon(systemIcon("paperclip"), contentDescription = null, tint = DoorTreeTheme.textSecondary)
                Text(text = L("chat.message_placeholder"), color = DoorTreeTheme.textSecondary)
            }

            Box(
                modifier = Modifier
                    .size(48.dp)
                    .background(DoorTreeTheme.primaryGradient, CircleShape),
                contentAlignment = Alignment.Center
            ) {
                Icon(systemIcon("arrow.up"), contentDescription = null, tint = DoorTreeTheme.textPrimary)
            }
        }
    }
}

@Composable
private fun ChatBubble(message: ChatMessageItem, managerInitials: String) {
    if (message.sender == ChatParticipant.Tenant) {
        Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.End) {
            Column(horizontalAlignment = Alignment.End, verticalArrangement = Arrangement.spacedBy(8.dp)) {
                Text(
                    text = message.text,
                    color = DoorTreeTheme.textPrimary,
                    modifier = Modifier
                        .liquidGlassSurface(cornerRadius = 22.dp, tint = DoorTreeTheme.gradientStart.copy(alpha = 0.92f))
                        .padding(horizontal = 16.dp, vertical = 14.dp)
                )
                Text(text = message.timestamp, color = DoorTreeTheme.textSecondary)
            }
        }
    } else {
        Row(horizontalArrangement = Arrangement.spacedBy(12.dp), verticalAlignment = Alignment.Bottom) {
            Box(
                modifier = Modifier
                    .size(30.dp)
                    .background(DoorTreeTheme.backgroundSecondary, CircleShape),
                contentAlignment = Alignment.Center
            ) {
                Text(text = managerInitials, color = DoorTreeTheme.textPrimary)
            }
            Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                Text(
                    text = message.text,
                    color = DoorTreeTheme.textPrimary,
                    modifier = Modifier
                        .liquidGlassSurface(cornerRadius = 22.dp)
                        .padding(horizontal = 16.dp, vertical = 14.dp)
                )
                Text(text = message.timestamp, color = DoorTreeTheme.textSecondary)
            }
        }
    }
}

@Composable
private fun DateSeparator(title: String) {
    Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(12.dp)) {
        Box(modifier = Modifier.weight(1f).height(1.dp).background(DoorTreeTheme.cardBorder))
        Text(text = title, color = DoorTreeTheme.textSecondary)
        Box(modifier = Modifier.weight(1f).height(1.dp).background(DoorTreeTheme.cardBorder))
    }
}
