package codewhale.doortreeandroid

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.combinedClickable
import androidx.compose.foundation.ExperimentalFoundationApi
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
import androidx.compose.foundation.text.BasicTextField
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Icon
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import kotlinx.coroutines.launch
import codewhale.doortreeandroid.ui.theme.DoorTreeTheme

@Composable
fun ChatView(tenantDataStore: TenantDataStore) {
    var draftMessage by remember { mutableStateOf("") }
    var sendError by remember { mutableStateOf<String?>(null) }
    var isSending by remember { mutableStateOf(false) }
    var pendingReport by remember { mutableStateOf<ChatMessageItem?>(null) }
    var showReportSuccess by remember { mutableStateOf(false) }
    val coroutineScope = rememberCoroutineScope()
    val scrollState = rememberScrollState()
    val chatContentSignature = remember(tenantDataStore.chatSections) {
        tenantDataStore.chatSections
            .flatMap { section -> section.messages.map { it.id } }
            .joinToString("|")
    }

    LaunchedEffect(chatContentSignature) {
        if (tenantDataStore.chatSections.isNotEmpty()) {
            scrollState.animateScrollTo(scrollState.maxValue)
        }
    }

    RefreshableScreen(
        onRefresh = { tenantDataStore.refresh() },
        modifier = Modifier
            .fillMaxSize()
            .background(DoorTreeTheme.backgroundPrimary)
    ) {
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
                    .verticalScroll(scrollState)
                    .padding(horizontal = DoorTreeTheme.screenHorizontalPadding, vertical = 18.dp),
                verticalArrangement = Arrangement.spacedBy(20.dp)
            ) {
                if (tenantDataStore.chatSections.isEmpty()) {
                    SectionPlaceholder(
                        systemName = "bubble.left.and.exclamationmark.bubble.right",
                        title = L("chat.empty.title"),
                        message = L("chat.empty.message")
                    )
                } else {
                    tenantDataStore.chatSections.forEach { section ->
                        DateSeparator(title = section.title)
                        Column(verticalArrangement = Arrangement.spacedBy(14.dp)) {
                            section.messages.forEach { message ->
                                ChatBubble(
                                    message = message,
                                    managerInitials = tenantDataStore.propertyManagerInitials,
                                    onReport = { selectedMessage ->
                                        pendingReport = selectedMessage
                                    }
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
                Box(
                    modifier = Modifier
                        .weight(1f)
                        .liquidGlassSurface(cornerRadius = 14.dp, interactive = true)
                        .padding(horizontal = 14.dp, vertical = 6.dp)
                ) {
                    BasicTextField(
                        value = draftMessage,
                        onValueChange = {
                            draftMessage = it
                            if (sendError != null) {
                                sendError = null
                            }
                        },
                        textStyle = androidx.compose.ui.text.TextStyle(color = DoorTreeTheme.textPrimary),
                        modifier = Modifier.fillMaxWidth(),
                        decorationBox = { innerTextField ->
                            if (draftMessage.isBlank()) {
                                Text(text = L("chat.message_placeholder"), color = DoorTreeTheme.textSecondary)
                            }
                            innerTextField()
                        }
                    )
                }

                Box(
                    modifier = Modifier
                        .size(48.dp)
                        .then(
                            if (draftMessage.trim().isNotEmpty() && !isSending) {
                                Modifier.background(DoorTreeTheme.primaryGradient, CircleShape)
                            } else {
                                Modifier.background(DoorTreeTheme.backgroundSecondary, CircleShape)
                            }
                        )
                        .clickable(
                            enabled = draftMessage.trim().isNotEmpty() && !isSending
                        ) {
                            val message = draftMessage.trim()
                            if (message.isEmpty()) return@clickable
                            isSending = true
                            sendError = null
                            coroutineScope.launch {
                                runCatching {
                                    tenantDataStore.sendChatMessage(message)
                                }.onSuccess {
                                    draftMessage = ""
                                }.onFailure { error ->
                                    sendError = error.message ?: L("chat.error.send")
                                }
                                isSending = false
                            }
                        },
                    contentAlignment = Alignment.Center
                ) {
                    Icon(systemIcon("arrow.up"), contentDescription = null, tint = DoorTreeTheme.textPrimary)
                }
            }

            if (!sendError.isNullOrBlank()) {
                Text(
                    text = sendError.orEmpty(),
                    color = DoorTreeTheme.destructive,
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = DoorTreeTheme.screenHorizontalPadding)
                )
            }
        }
    }

    pendingReport?.let { message ->
        AlertDialog(
            onDismissRequest = { pendingReport = null },
            title = { Text(L("Report Message")) },
            text = { Text(LF("From %@:\n\"%@\"", tenantDataStore.propertyManagerName, message.text)) },
            confirmButton = {
                TextButton(
                    onClick = {
                        coroutineScope.launch {
                            runCatching {
                                tenantDataStore.reportChatMessage(message)
                            }.onSuccess {
                                pendingReport = null
                                showReportSuccess = true
                            }.onFailure { error ->
                                sendError = error.message ?: L("chat.error.report")
                                pendingReport = null
                            }
                        }
                    }
                ) {
                    Text(L("Report"), color = DoorTreeTheme.destructive)
                }
            },
            dismissButton = {
                TextButton(onClick = { pendingReport = null }) {
                    Text(L("Cancel"))
                }
            }
        )
    }

    if (showReportSuccess) {
        AlertDialog(
            onDismissRequest = { showReportSuccess = false },
            title = { Text(L("Message Reported")) },
            text = { Text(L("Thanks. This message was sent for review.")) },
            confirmButton = {
                TextButton(onClick = { showReportSuccess = false }) {
                    Text(L("common.ok"))
                }
            }
        )
    }
}

@OptIn(ExperimentalFoundationApi::class)
@Composable
private fun ChatBubble(
    message: ChatMessageItem,
    managerInitials: String,
    onReport: (ChatMessageItem) -> Unit
) {
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
                        .combinedClickable(
                            onClick = {},
                            onLongClick = { onReport(message) }
                        )
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
