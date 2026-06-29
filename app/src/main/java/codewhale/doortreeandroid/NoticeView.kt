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
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.SwipeToDismissBox
import androidx.compose.material3.SwipeToDismissBoxValue
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.rememberSwipeToDismissBoxState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.key
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import codewhale.doortreeandroid.ui.theme.DoorTreeTheme

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun NoticeView(
    tenantDataStore: TenantDataStore,
    onClose: () -> Unit
) {
    var selectedDocument by remember { mutableStateOf<DocumentItem?>(null) }
    var noticePendingDelete by remember { mutableStateOf<NoticeItem?>(null) }

    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(DoorTreeTheme.backgroundPrimary)
    ) {
        Column(
            modifier = Modifier
                .fillMaxSize()
                .verticalScroll(rememberScrollState())
                .topSafeAreaPadding()
                .padding(horizontal = DoorTreeTheme.screenHorizontalPadding, vertical = 18.dp),
            verticalArrangement = Arrangement.spacedBy(18.dp)
        ) {
            NoticeHeader(
                unreadCount = tenantDataStore.unreadNoticeCount,
                onClose = onClose
            )
            NoticeSummaryCard(
                totalCount = tenantDataStore.notices.size,
                unreadCount = tenantDataStore.unreadNoticeCount
            )
            NoticeListSection(
                notices = tenantDataStore.notices,
                onOpenNotice = { notice ->
                    if (notice.url != null) {
                        tenantDataStore.markNoticeRead(notice)
                        selectedDocument = notice.documentItem
                    }
                },
                onMarkUnread = tenantDataStore::markNoticeUnread,
                onDelete = { notice -> noticePendingDelete = notice }
            )
        }

        selectedDocument?.let { document ->
            PDFDocumentSheetView(
                document = document,
                onDismiss = { selectedDocument = null }
            )
        }

        noticePendingDelete?.let { notice ->
            AlertDialog(
                onDismissRequest = { noticePendingDelete = null },
                title = { Text("Delete notice?") },
                text = { Text("Are you sure you want to delete this notice? This cannot be undone.") },
                confirmButton = {
                    Button(
                        onClick = {
                            tenantDataStore.deleteNotice(notice)
                            noticePendingDelete = null
                        }
                    ) {
                        Text("Delete")
                    }
                },
                dismissButton = {
                    TextButton(onClick = { noticePendingDelete = null }) {
                        Text("Cancel")
                    }
                },
                containerColor = DoorTreeTheme.backgroundPrimary
            )
        }
    }
}

@Composable
private fun NoticeHeader(
    unreadCount: Int,
    onClose: () -> Unit
) {
    Row(
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(14.dp)
    ) {
        HeaderIconButton(systemName = "chevron.left", onClick = onClose)

        Column(verticalArrangement = Arrangement.spacedBy(3.dp)) {
            Text(
                text = "Notices",
                color = DoorTreeTheme.textPrimary,
                fontWeight = FontWeight.Bold,
                fontSize = 28.sp
            )
            Text(
                text = when (unreadCount) {
                    0 -> "All notices are read"
                    1 -> "1 unread notice"
                    else -> "$unreadCount unread notices"
                },
                color = DoorTreeTheme.textSecondary,
                fontSize = 14.sp
            )
        }
    }
}

@Composable
private fun NoticeSummaryCard(
    totalCount: Int,
    unreadCount: Int
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .glassCard(cornerRadius = 22.dp)
            .padding(16.dp),
        horizontalArrangement = Arrangement.spacedBy(14.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Box(
            modifier = Modifier
                .size(54.dp)
                .clip(RoundedCornerShape(18.dp))
                .background(DoorTreeTheme.paidBackground.copy(alpha = 0.82f)),
            contentAlignment = Alignment.Center
        ) {
            Icon(
                imageVector = systemIcon(if (unreadCount > 0) "bell.badge.fill" else "bell"),
                contentDescription = null,
                tint = DoorTreeTheme.gradientStart
            )
        }

        Column(
            modifier = Modifier.weight(1f),
            verticalArrangement = Arrangement.spacedBy(4.dp)
        ) {
            Text(
                text = when (totalCount) {
                    0 -> "No active notices"
                    1 -> "1 notice on file"
                    else -> "$totalCount notices on file"
                },
                color = DoorTreeTheme.textPrimary,
                fontWeight = FontWeight.SemiBold
            )
            Text(
                text = "Building notices and official tenant PDFs are listed here.",
                color = DoorTreeTheme.textSecondary,
                fontSize = 14.sp
            )
        }
    }
}

@Composable
private fun NoticeListSection(
    notices: List<NoticeItem>,
    onOpenNotice: (NoticeItem) -> Unit,
    onMarkUnread: (NoticeItem) -> Unit,
    onDelete: (NoticeItem) -> Unit
) {
    Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
        Text(
            text = "Tenant Notices",
            color = DoorTreeTheme.textPrimary,
            fontWeight = FontWeight.Bold,
            fontSize = 20.sp
        )

        if (notices.isEmpty()) {
            SectionPlaceholder(
                systemName = "bell.slash",
                title = "No notices yet",
                message = "New notices from your property manager will appear here."
            )
        } else {
            Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
                notices.forEach { notice ->
                    key(notice.id) {
                        NoticeRow(
                            notice = notice,
                            onOpen = { onOpenNotice(notice) },
                            onMarkUnread = { onMarkUnread(notice) },
                            onDelete = { onDelete(notice) }
                        )
                    }
                }
            }
        }
    }
}

@Composable
@OptIn(ExperimentalMaterial3Api::class)
private fun NoticeRow(
    notice: NoticeItem,
    onOpen: () -> Unit,
    onMarkUnread: () -> Unit,
    onDelete: () -> Unit
) {
    val dismissState = rememberSwipeToDismissBoxState(
        confirmValueChange = { value ->
            when (value) {
                SwipeToDismissBoxValue.StartToEnd -> {
                    if (!notice.isUnread) {
                        onMarkUnread()
                    }
                    false
                }
                SwipeToDismissBoxValue.EndToStart -> {
                    onDelete()
                    false
                }
                SwipeToDismissBoxValue.Settled -> false
            }
        }
    )
    val backgroundAlpha by animateFloatAsState(
        targetValue = if (
            dismissState.currentValue != SwipeToDismissBoxValue.Settled ||
            dismissState.targetValue != SwipeToDismissBoxValue.Settled
        ) 1f else 0f,
        label = "noticeSwipeBackgroundAlpha"
    )

    LaunchedEffect(notice.id, notice.isUnread) {
        dismissState.reset()
    }

    SwipeToDismissBox(
        state = dismissState,
        enableDismissFromStartToEnd = !notice.isUnread,
        enableDismissFromEndToStart = true,
        backgroundContent = {
            NoticeSwipeBackground(
                value = dismissState.dismissDirection,
                alpha = backgroundAlpha
            )
        }
    ) {
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .liquidGlassSurface(
                    cornerRadius = 22.dp,
                    tint = if (notice.isUnread) DoorTreeTheme.paidBackground.copy(alpha = 0.35f) else DoorTreeTheme.barGlassTint
                )
                .then(if (notice.url != null) Modifier.clickable(onClick = onOpen) else Modifier)
                .padding(14.dp)
        ) {
            Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
                Row(horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                    Box(
                        modifier = Modifier
                            .size(48.dp)
                            .clip(RoundedCornerShape(16.dp))
                            .background(DoorTreeTheme.paidBackground.copy(alpha = 0.82f)),
                        contentAlignment = Alignment.Center
                    ) {
                        Icon(
                            imageVector = systemIcon("bell.badge.fill"),
                            contentDescription = null,
                            tint = DoorTreeTheme.gradientStart
                        )
                    }

                    Column(
                        modifier = Modifier.weight(1f),
                        verticalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        Row(verticalAlignment = Alignment.Top) {
                            Text(
                                text = notice.displayTitle,
                                color = DoorTreeTheme.textPrimary,
                                fontWeight = FontWeight.SemiBold,
                                maxLines = 2,
                                overflow = TextOverflow.Ellipsis,
                                modifier = Modifier.weight(1f)
                            )
                            if (notice.isUnread) {
                                NoticeUnreadBadge()
                            }
                        }

                        NoticeDetails(notice = notice)
                    }
                }
            }

        }
    }
}

@Composable
private fun NoticeUnreadBadge() {
    Row(
        modifier = Modifier
            .clip(RoundedCornerShape(999.dp))
            .background(DoorTreeTheme.paidBackground.copy(alpha = 0.82f))
            .padding(horizontal = 9.dp, vertical = 5.dp),
        horizontalArrangement = Arrangement.spacedBy(5.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Box(
            modifier = Modifier
                .size(7.dp)
                .clip(RoundedCornerShape(999.dp))
                .background(DoorTreeTheme.gradientStart)
        )
        Text(
            text = "Unread",
            color = DoorTreeTheme.gradientStart,
            fontSize = 12.sp,
            fontWeight = FontWeight.SemiBold,
            maxLines = 1
        )
    }
}

@Composable
@OptIn(ExperimentalMaterial3Api::class)
private fun NoticeSwipeBackground(
    value: SwipeToDismissBoxValue,
    alpha: Float
) {
    val isDelete = value == SwipeToDismissBoxValue.EndToStart
    val isUnread = value == SwipeToDismissBoxValue.StartToEnd

    Box(
        modifier = Modifier
            .fillMaxWidth()
            .alpha(alpha)
            .clip(RoundedCornerShape(22.dp))
            .background(
                when {
                    isDelete -> DoorTreeTheme.destructive
                    isUnread -> DoorTreeTheme.gradientStart
                    else -> Color.Transparent
                }
            )
            .padding(horizontal = 22.dp),
        contentAlignment = when {
            isDelete -> Alignment.CenterEnd
            else -> Alignment.CenterStart
        }
    ) {
        if (isDelete || isUnread) {
            Row(horizontalArrangement = Arrangement.spacedBy(8.dp), verticalAlignment = Alignment.CenterVertically) {
                Icon(
                    imageVector = systemIcon(if (isDelete) "xmark" else "bell.badge.fill"),
                    contentDescription = null,
                    tint = Color.White
                )
                Text(
                    text = if (isDelete) "Delete" else "Unread",
                    color = Color.White,
                    fontWeight = FontWeight.SemiBold
                )
            }
        }
    }
}

@Composable
private fun NoticeDetails(notice: NoticeItem) {
    Column(verticalArrangement = Arrangement.spacedBy(5.dp)) {
        Row(horizontalArrangement = Arrangement.spacedBy(6.dp), verticalAlignment = Alignment.CenterVertically) {
            Icon(
                imageVector = systemIcon("calendar"),
                contentDescription = null,
                tint = DoorTreeTheme.textSecondary,
                modifier = Modifier.size(14.dp)
            )
            Text(
                text = "Event date ${notice.displayDate}",
                color = DoorTreeTheme.textSecondary,
                fontSize = 14.sp,
                fontWeight = FontWeight.SemiBold,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis
            )
        }

        Text(
            text = "${notice.propertyName.ifBlank { "Property -" }} • ${notice.unitLabel}",
            color = DoorTreeTheme.textSecondary,
            fontSize = 14.sp,
            maxLines = 1,
            overflow = TextOverflow.Ellipsis
        )

        Text(
            text = if (notice.sentAt == "-") "Sent date unavailable" else "Sent ${notice.sentAt}",
            color = DoorTreeTheme.textSecondary,
            fontSize = 12.sp,
            maxLines = 1,
            overflow = TextOverflow.Ellipsis
        )
    }
}
