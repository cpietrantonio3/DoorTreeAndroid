@file:OptIn(ExperimentalMaterial3Api::class)

package codewhale.doortreeandroid

import android.content.Context
import android.content.Intent
import android.net.Uri
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import codewhale.doortreeandroid.ui.theme.DoorTreeTheme

@Composable
fun HomeView(
    tenantDataStore: TenantDataStore,
    onSelectAction: (QuickActionRoute) -> Unit,
    onOpenRequest: (MaintenanceRequestItem) -> Unit,
    onSelectProfile: () -> Unit = {}
) {
    val context = LocalContext.current
    var showingMaintenanceRequest by remember { mutableStateOf(false) }
    var showingMaintenanceChoice by remember { mutableStateOf(false) }
    var showingNotificationCenter by remember { mutableStateOf(false) }

    RefreshableScreen(
        onRefresh = { tenantDataStore.refresh() },
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
            verticalArrangement = Arrangement.spacedBy(14.dp)
        ) {
            Box(modifier = Modifier.fillMaxWidth()) {
                DoorTreeLogoLockup(
                    width = 75.dp,
                    modifier = Modifier.align(Alignment.Center)
                )
                Box(modifier = Modifier.align(Alignment.CenterEnd)) {
                    Box(
                        modifier = Modifier.clickable { showingNotificationCenter = true }
                    ) {
                        HeaderIconButton(systemName = "bell", onClick = { showingNotificationCenter = true })
                        if (tenantDataStore.unreadNotificationCount > 0) {
                            Box(
                                modifier = Modifier
                                    .align(Alignment.TopEnd)
                                    .padding(end = 4.dp, top = 4.dp)
                                    .size(9.dp)
                                    .background(DoorTreeTheme.destructive, CircleShape)
                            )
                        }
                    }
                }
            }

            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .glassCard(cornerRadius = 20.dp)
                    .padding(16.dp),
                horizontalArrangement = Arrangement.spacedBy(14.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                Column(modifier = Modifier.weight(1f), verticalArrangement = Arrangement.spacedBy(6.dp)) {
                    Text(
                        text = LF("home.greeting.format", greetingLabel(), firstName(tenantDataStore.tenantProfile.name)),
                        color = DoorTreeTheme.textPrimary
                    )
                    Text(text = tenantDataStore.propertyInfo.subtitle, color = DoorTreeTheme.textSecondary)
                }
                Box(
                    modifier = Modifier
                        .semantics { contentDescription = L("tab.profile") }
                        .clickable { onSelectProfile() }
                ) {
                    AvatarCircle(initials = tenantDataStore.tenantProfile.initials, size = 46.dp)
                }
            }

            HomeQuickActionsGrid(
                quickActions = tenantDataStore.quickActions,
                unreadDocumentCount = tenantDataStore.unreadDocumentCount,
                unreadChatCount = tenantDataStore.unreadChatCount,
                unreadNotificationCount = tenantDataStore.unreadNotificationCount,
                onSelectAction = { route ->
                    if (route == QuickActionRoute.Requests) {
                        showingMaintenanceChoice = true
                    } else {
                        onSelectAction(route)
                    }
                }
            )

            HomePaymentHistorySection(tenantDataStore = tenantDataStore)
            HomeRequestsSection(
                tenantDataStore = tenantDataStore,
                onNewRequest = { showingMaintenanceRequest = true },
                onOpenRequest = onOpenRequest
            )
        }
    }

    if (showingMaintenanceRequest) {
        FullHeightModalBottomSheet(onDismissRequest = { showingMaintenanceRequest = false }) {
            MaintenanceRequestSheetView(
                tenantDataStore = tenantDataStore,
                onDismiss = { showingMaintenanceRequest = false }
            )
        }
    }

    if (showingMaintenanceChoice) {
        MaintenanceActionChoiceDialog(
            onDismiss = { showingMaintenanceChoice = false },
            onCall = {
                showingMaintenanceChoice = false
                openMaintenancePhoneDialer(context)
            },
            onSubmit = {
                showingMaintenanceChoice = false
                showingMaintenanceRequest = true
            }
        )
    }

    if (showingNotificationCenter) {
        FullHeightModalBottomSheet(onDismissRequest = { showingNotificationCenter = false }) {
            NotificationCenterSheetView(
                tenantDataStore = tenantDataStore,
                onDismiss = { showingNotificationCenter = false }
            )
        }
    }
}

@Composable
private fun MaintenanceActionChoiceDialog(
    onDismiss: () -> Unit,
    onCall: () -> Unit,
    onSubmit: () -> Unit
) {
    AlertDialog(
        onDismissRequest = onDismiss,
        icon = {
            Icon(
                imageVector = systemIcon("wrench.and.screwdriver.fill"),
                contentDescription = null,
                tint = DoorTreeTheme.gradientStart
            )
        },
        title = {
            Text(
                text = L("maintenance.choice.title"),
                color = DoorTreeTheme.textPrimary,
                textAlign = TextAlign.Center,
                modifier = Modifier.fillMaxWidth()
            )
        },
        text = {
            Column(verticalArrangement = Arrangement.spacedBy(14.dp)) {
                Text(
                    text = L("maintenance.choice.message"),
                    color = DoorTreeTheme.textSecondary,
                    textAlign = TextAlign.Center,
                    modifier = Modifier.fillMaxWidth()
                )
                GradientButton(
                    title = L("maintenance.choice.submit"),
                    icon = "wrench.and.screwdriver.fill",
                    onClick = onSubmit
                )
                Button(
                    onClick = onCall,
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Icon(systemIcon("phone"), contentDescription = null)
                    Text(text = L("maintenance.choice.call"))
                }
            }
        },
        confirmButton = {
            TextButton(onClick = onDismiss) {
                Text(text = L("cancel"))
            }
        },
        containerColor = DoorTreeTheme.backgroundPrimary
    )
}

private fun openMaintenancePhoneDialer(context: Context) {
    val phoneIntent = Intent(Intent.ACTION_DIAL, Uri.parse("tel:+14388003102"))
    runCatching { context.startActivity(phoneIntent) }
}

@Composable
private fun HomeQuickActionsGrid(
    quickActions: List<QuickActionItem>,
    unreadDocumentCount: Int,
    unreadChatCount: Int,
    unreadNotificationCount: Int,
    onSelectAction: (QuickActionRoute) -> Unit
) {
    Column(verticalArrangement = Arrangement.spacedBy(DoorTreeTheme.cardSpacing)) {
        quickActions.chunked(2).forEach { rowItems ->
            Row(horizontalArrangement = Arrangement.spacedBy(DoorTreeTheme.cardSpacing)) {
                rowItems.forEach { item ->
                    Box(modifier = Modifier.weight(1f)) {
                        QuickActionCard(
                            item = item,
                            notificationCount = notificationCountFor(
                                item = item,
                                unreadDocumentCount = unreadDocumentCount,
                                unreadChatCount = unreadChatCount,
                                unreadNotificationCount = unreadNotificationCount
                            ),
                            height = 128.dp
                        ) {
                            onSelectAction(item.route)
                        }
                    }
                }

                if (rowItems.size == 1) {
                    Box(modifier = Modifier.weight(1f))
                }
            }
        }
    }
}

private fun notificationCountFor(
    item: QuickActionItem,
    unreadDocumentCount: Int,
    unreadChatCount: Int,
    unreadNotificationCount: Int
): Int = when (item.route) {
    QuickActionRoute.Chat -> unreadChatCount
    QuickActionRoute.Lease -> unreadDocumentCount
    QuickActionRoute.Notices -> unreadNotificationCount
    else -> 0
}

@Composable
private fun HomePaymentHistorySection(tenantDataStore: TenantDataStore) {
    var selectedPayment by remember { mutableStateOf<DashboardPaymentHistoryItem?>(null) }
    var paymentHistoryPage by remember { mutableStateOf(0) }
    val pageSize = 12
    val visibleRows = 3
    val rowHeight = 80.dp
    val rowSpacing = 10.dp
    val paymentHistory = tenantDataStore.dashboardPaymentHistory
    val pageCount = maxOf(1, (paymentHistory.size + pageSize - 1) / pageSize)
    val currentPage = paymentHistoryPage.coerceIn(0, pageCount - 1)
    val visiblePaymentHistory = paymentHistory
        .drop(currentPage * pageSize)
        .take(pageSize)
    val shouldScrollPaymentHistory = visiblePaymentHistory.size > visibleRows
    val paymentHistoryMaxHeight = rowHeight * visibleRows + rowSpacing * (visibleRows - 1)

    Column(
        modifier = Modifier
            .fillMaxWidth()
            .glassCard(cornerRadius = 22.dp)
            .padding(14.dp),
        verticalArrangement = Arrangement.spacedBy(14.dp)
    ) {
        Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(10.dp)) {
            Icon(systemIcon("clock.arrow.circlepath"), contentDescription = null, tint = DoorTreeTheme.dueText)
            Text(text = L("home.payment_history"), color = DoorTreeTheme.textPrimary)
        }

        if (paymentHistory.isEmpty()) {
            SectionPlaceholder(
                systemName = "creditcard.trianglebadge.exclamationmark",
                title = "No payment history yet",
                message = "Rent, parking, and maintenance invoice payments will appear here once they are recorded."
            )
        } else {
            Column(
                modifier = if (shouldScrollPaymentHistory) {
                    Modifier
                        .heightIn(max = paymentHistoryMaxHeight)
                        .verticalScroll(rememberScrollState())
                } else {
                    Modifier
                },
                verticalArrangement = Arrangement.spacedBy(rowSpacing)
            ) {
                visiblePaymentHistory.forEach { payment ->
                    Box(modifier = Modifier.clickable { selectedPayment = payment }) {
                        PaymentRow(
                            payment = payment.paymentItem,
                            badgeForegroundOverride = if (payment.status == StatusBadgeStyle.Due) DoorTreeTheme.destructive else null,
                            badgeBackgroundOverride = if (payment.status == StatusBadgeStyle.Due) DoorTreeTheme.destructive.copy(alpha = 0.14f) else null
                        )
                    }
                }
            }

            if (pageCount > 1) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(12.dp)
                ) {
                    Button(
                        onClick = { paymentHistoryPage = maxOf(0, currentPage - 1) },
                        enabled = currentPage > 0
                    ) {
                        Icon(systemIcon("chevron.left"), contentDescription = null)
                    }

                    Text(
                        text = "${currentPage + 1} / $pageCount",
                        color = DoorTreeTheme.textSecondary,
                        modifier = Modifier.weight(1f),
                        textAlign = TextAlign.Center
                    )

                    Button(
                        onClick = { paymentHistoryPage = minOf(pageCount - 1, currentPage + 1) },
                        enabled = currentPage < pageCount - 1
                    ) {
                        Icon(systemIcon("chevron.right"), contentDescription = null)
                    }
                }
            }
        }
    }

    selectedPayment?.let { payment ->
        PaymentHistoryDetailView(
            item = payment,
            onDismiss = { selectedPayment = null }
        )
    }
}

@Composable
private fun HomeRequestsSection(
    tenantDataStore: TenantDataStore,
    onNewRequest: () -> Unit,
    onOpenRequest: (MaintenanceRequestItem) -> Unit
) {
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .glassCard(cornerRadius = 22.dp)
            .padding(14.dp),
        verticalArrangement = Arrangement.spacedBy(14.dp)
    ) {
        Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(10.dp)) {
            Icon(systemIcon("wrench.and.screwdriver.fill"), contentDescription = null, tint = DoorTreeTheme.textSecondary)
            Text(text = L("home.my_requests"), color = DoorTreeTheme.textPrimary)
        }

        if (tenantDataStore.maintenanceRequests.isEmpty()) {
            SectionPlaceholder(
                systemName = "wrench.and.screwdriver",
                title = "No maintenance requests yet",
                message = "Requests will appear here after they are saved for this tenant."
            )
        } else {
            Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
                tenantDataStore.maintenanceRequests.forEach { request ->
                    DismissiblePendingRequestRow(
                        request = request,
                        onClick = { onOpenRequest(request) },
                        onDeleteConfirmed = {
                            tenantDataStore.deleteMaintenanceRequest(request)
                        }
                    )
                }
            }
        }

        Text(
            text = L("home.new_maintenance_request"),
            color = DoorTreeTheme.gradientStart,
            modifier = Modifier
                .fillMaxWidth()
                .liquidGlassSurface(cornerRadius = DoorTreeTheme.buttonCornerRadius, interactive = true)
                .clickable(onClick = onNewRequest)
                .padding(vertical = 16.dp),
            textAlign = androidx.compose.ui.text.style.TextAlign.Center
        )
    }
}

private fun greetingLabel(): String {
    val hour = java.time.LocalTime.now().hour
    return when (hour) {
        in 5..11 -> L("home.greeting.morning")
        in 12..17 -> L("home.greeting.afternoon")
        else -> L("home.greeting.evening")
    }
}

private fun firstName(fullName: String): String {
    return fullName.split(" ").firstOrNull().orEmpty().ifBlank { "there" }
}
