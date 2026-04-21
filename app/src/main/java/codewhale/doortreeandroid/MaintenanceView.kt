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
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.Icon
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import codewhale.doortreeandroid.ui.theme.DoorTreeTheme

private const val PendingInvoicesVisibleRows = 4
private val PendingInvoicesMaxHeight = 336.dp

@Composable
fun MaintenanceView(
    tenantDataStore: TenantDataStore,
    onNewRequest: () -> Unit,
    onOpenInvoice: (PendingInvoiceItem) -> Unit,
    onOpenRequest: (MaintenanceRequestItem) -> Unit
) {
    val sortedActiveRequests = tenantDataStore.maintenanceRequests.sortedWith(
        compareBy<MaintenanceRequestItem>({ it.sortDate == null }, { it.sortDate }, { it.id })
    )
    val pendingInvoiceNotificationCount = tenantDataStore.pendingInvoices.size

    RefreshableScreen(
        onRefresh = { tenantDataStore.refresh() },
        modifier = Modifier
            .fillMaxSize()
            .background(DoorTreeTheme.backgroundPrimary)
    ) {
        Column(
            modifier = Modifier
                .fillMaxSize()
                .topSafeAreaPadding()
                .padding(horizontal = DoorTreeTheme.screenHorizontalPadding, vertical = 18.dp),
            verticalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Text(
                    text = L("maintenance.title"),
                    color = DoorTreeTheme.textPrimary,
                    fontWeight = FontWeight.Bold,
                    fontSize = 28.sp
                )
                Spacer(modifier = Modifier.weight(1f))
                Text(
                    text = L("maintenance.new_request"),
                    color = DoorTreeTheme.textPrimary,
                    modifier = Modifier
                        .liquidGlassSurface(cornerRadius = 16.dp, interactive = true, tint = DoorTreeTheme.gradientStart.copy(alpha = 0.18f))
                        .clickable(onClick = onNewRequest)
                        .padding(horizontal = 12.dp, vertical = 8.dp)
                )
            }

            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .glassCard(cornerRadius = 20.dp)
                    .padding(16.dp),
                verticalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    Text(
                        text = L("maintenance.section.pending_invoices"),
                        color = DoorTreeTheme.textSecondary,
                        fontWeight = FontWeight.Bold
                    )
                    if (pendingInvoiceNotificationCount > 0) {
                        NotificationCountBadge(count = pendingInvoiceNotificationCount)
                    }
                }
                if (tenantDataStore.pendingInvoices.isEmpty()) {
                    SectionPlaceholder(
                        systemName = "doc.badge.clock",
                        title = "No pending invoices",
                        message = "Tenant invoices will appear here when they are assigned to you by your landlord."
                    )
                } else {
                    SectionList(
                        itemCount = tenantDataStore.pendingInvoices.size,
                        visibleRows = PendingInvoicesVisibleRows,
                        maxHeight = PendingInvoicesMaxHeight
                    ) {
                        tenantDataStore.pendingInvoices.forEach { invoice ->
                            PendingInvoiceRow(
                                invoice = invoice,
                                onClick = { onOpenInvoice(invoice) }
                            )
                        }
                    }
                }
            }

            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .weight(1f)
                    .glassCard(cornerRadius = 20.dp)
                    .padding(16.dp),
                verticalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                Text(
                    text = L("maintenance.section.active_requests"),
                    color = DoorTreeTheme.textSecondary,
                    fontWeight = FontWeight.Bold
                )
                if (tenantDataStore.maintenanceRequests.isEmpty()) {
                    SectionPlaceholder(
                        systemName = "wrench.and.screwdriver",
                        title = "No active requests",
                        message = "Maintenance requests will appear here after they are created for this tenant."
                    )
                    Spacer(modifier = Modifier.weight(1f))
                } else {
                    Column(
                        modifier = Modifier
                            .fillMaxWidth()
                            .weight(1f)
                            .verticalScroll(rememberScrollState()),
                        verticalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        sortedActiveRequests.forEach { request ->
                            DismissiblePendingRequestRow(
                                request = request,
                                subtitle = LF("requests.submitted_format", request.submittedDate),
                                onClick = { onOpenRequest(request) },
                                onDeleteConfirmed = {
                                    tenantDataStore.deleteMaintenanceRequest(request)
                                }
                            )
                        }
                    }
                }
            }
        }
    }
}

@Composable
private fun PendingInvoiceRow(
    invoice: PendingInvoiceItem,
    onClick: () -> Unit
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .liquidGlassSurface(cornerRadius = 14.dp, interactive = true)
            .clickable(onClick = onClick)
            .padding(14.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(12.dp)
    ) {
        Column(modifier = Modifier.weight(1f), verticalArrangement = Arrangement.spacedBy(4.dp)) {
            Text(
                text = invoice.invoiceNumber.ifBlank { L("maintenance.invoice.title") },
                color = DoorTreeTheme.textPrimary,
                fontWeight = FontWeight.Bold
            )
            Text(
                text = LF("maintenance.invoice.due_format", invoice.dueDate),
                color = DoorTreeTheme.textSecondary
            )
        }
        Text(text = invoice.balance, color = DoorTreeTheme.textPrimary)
        StatusBadge(
            status = StatusBadgeStyle.Pending,
            label = invoiceStatusLabel(invoice),
            foregroundOverride = if (invoiceIsOverdue(invoice)) invoiceStatusForeground(invoice) else null,
            backgroundOverride = if (invoiceIsOverdue(invoice)) invoiceStatusBackground(invoice) else null
        )
    }
}

@Composable
private fun SectionList(
    itemCount: Int,
    visibleRows: Int,
    maxHeight: androidx.compose.ui.unit.Dp,
    content: @Composable () -> Unit
) {
    val scrollModifier = if (itemCount > visibleRows) {
        Modifier
            .fillMaxWidth()
            .heightIn(max = maxHeight)
            .verticalScroll(rememberScrollState())
    } else {
        Modifier.fillMaxWidth()
    }

    Column(
        modifier = scrollModifier,
        verticalArrangement = Arrangement.spacedBy(8.dp)
    ) {
        content()
    }
}
