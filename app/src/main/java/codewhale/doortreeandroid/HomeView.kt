@file:OptIn(ExperimentalMaterial3Api::class)

package codewhale.doortreeandroid

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import codewhale.doortreeandroid.ui.theme.DoorTreeTheme

@Composable
fun HomeView(
    tenantDataStore: TenantDataStore,
    onSelectAction: (QuickActionRoute) -> Unit,
    onOpenRequest: (MaintenanceRequestItem) -> Unit
) {
    var showingMaintenanceRequest by remember { mutableStateOf(false) }
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
                AvatarCircle(initials = tenantDataStore.tenantProfile.initials, size = 46.dp)
            }

            Column(verticalArrangement = Arrangement.spacedBy(DoorTreeTheme.cardSpacing)) {
                Row(horizontalArrangement = Arrangement.spacedBy(DoorTreeTheme.cardSpacing)) {
                    tenantDataStore.quickActions.take(2).forEach { item ->
                        Box(modifier = Modifier.weight(1f)) {
                            QuickActionCard(item = item, height = 128.dp) {
                                onSelectAction(item.route)
                            }
                        }
                    }
                }
                Row(horizontalArrangement = Arrangement.spacedBy(DoorTreeTheme.cardSpacing)) {
                    tenantDataStore.quickActions.drop(2).forEach { item ->
                        Box(modifier = Modifier.weight(1f)) {
                            QuickActionCard(item = item, height = 128.dp) {
                                onSelectAction(item.route)
                            }
                        }
                    }
                }
            }

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
private fun HomePaymentHistorySection(tenantDataStore: TenantDataStore) {
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

        if (tenantDataStore.paymentHistory.isEmpty()) {
            SectionPlaceholder(
                systemName = "creditcard.trianglebadge.exclamationmark",
                title = "No payment history yet",
                message = "Rent payments will appear here once payments are recorded."
            )
        } else {
            Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
                tenantDataStore.paymentHistory.forEach { payment ->
                    PaymentRow(
                        payment = payment,
                        titleOverride = "Rent",
                        badgeForegroundOverride = if (payment.status == StatusBadgeStyle.Due) DoorTreeTheme.destructive else null,
                        badgeBackgroundOverride = if (payment.status == StatusBadgeStyle.Due) DoorTreeTheme.destructive.copy(alpha = 0.14f) else null
                    )
                }
            }
        }
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
