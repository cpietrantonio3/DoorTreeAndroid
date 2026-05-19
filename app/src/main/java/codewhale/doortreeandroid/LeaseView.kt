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
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.Icon
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import codewhale.doortreeandroid.ui.theme.DoorTreeTheme

@Composable
fun LeaseView(
    tenantDataStore: TenantDataStore,
    onClose: () -> Unit
) {
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
            Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(14.dp)) {
                HeaderIconButton(systemName = "chevron.left", onClick = onClose)
                Text(
                    text = L("lease.title"),
                    color = DoorTreeTheme.textPrimary,
                    fontWeight = FontWeight.Bold,
                    fontSize = 28.sp
                )
            }

            LeaseSummarySection(tenantDataStore = tenantDataStore)
            LeaseRenewalBanner(tenantDataStore = tenantDataStore)
            LeaseDocumentsSection(tenantDataStore = tenantDataStore)

            Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
                Text(text = L("lease.rent_schedule"), color = DoorTreeTheme.textPrimary)
                val entries = tenantDataStore.rentScheduleEntries
                if (entries.isEmpty()) {
                    SectionPlaceholder(
                        systemName = "calendar.badge.clock",
                        title = L("payments.schedule.empty_title"),
                        message = L("payments.schedule.empty_message")
                    )
                } else {
                    entries.forEach { entry ->
                        RentScheduleRow(entry = entry, style = RentScheduleRowStyle.LeaseDisplay)
                    }
                }
            }
        }
    }
}

@Composable
private fun LeaseSummarySection(tenantDataStore: TenantDataStore) {
    val lease = tenantDataStore.leaseDetails
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .glassCard(cornerRadius = 20.dp)
            .padding(horizontal = 16.dp, vertical = 6.dp),
        verticalArrangement = Arrangement.spacedBy(12.dp)
    ) {
        LeaseSummaryRow(label = L("lease.start_date"), value = lease.startDate)
        LeaseSummaryRow(label = L("lease.end_date"), value = lease.endDate)
        LeaseSummaryRow(label = L("lease.monthly_rent"), value = lease.monthlyRent)
        LeaseSummaryRow(label = L("lease.unit"), value = lease.unitLabel)
    }
}

@Composable
private fun LeaseRenewalBanner(tenantDataStore: TenantDataStore) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .background(DoorTreeTheme.dueBackground.copy(alpha = 0.60f), RoundedCornerShape(16.dp))
            .padding(14.dp),
        horizontalArrangement = Arrangement.spacedBy(12.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Icon(systemIcon("calendar"), contentDescription = null, tint = DoorTreeTheme.dueText)
        Text(text = tenantDataStore.leaseDetails.renewalNotice, color = DoorTreeTheme.dueText)
    }
}

@Composable
private fun LeaseDocumentsSection(tenantDataStore: TenantDataStore) {
    var selectedDocument by remember { mutableStateOf<DocumentItem?>(null) }

    Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
        Text(text = L("lease.documents"), color = DoorTreeTheme.textPrimary)
        if (tenantDataStore.documents.isEmpty()) {
            SectionPlaceholder(
                systemName = "doc.text",
                title = "No lease documents yet",
                message = "Lease documents will appear here when they are linked to this tenant."
            )
        } else {
            val documentsModifier = if (tenantDataStore.documents.size > 4) {
                Modifier
                    .height(358.dp)
                    .verticalScroll(rememberScrollState())
            } else {
                Modifier
            }
            Column(
                modifier = documentsModifier,
                verticalArrangement = Arrangement.spacedBy(10.dp)
            ) {
                tenantDataStore.documents.forEach { document ->
                    LeaseDocumentRow(
                        document = document,
                        onOpen = {
                            tenantDataStore.markDocumentRead(document)
                            selectedDocument = document
                        }
                    )
                }
            }
        }
    }

    selectedDocument?.let { document ->
        PDFDocumentSheetView(
            document = document,
            tenantName = tenantDataStore.tenantProfile.name,
            onRenewalDecision = { status, signatureBitmap ->
                tenantDataStore.recordRenewalDecision(document, status, signatureBitmap)
                tenantDataStore.refresh()
            },
            onDismiss = { selectedDocument = null }
        )
    }
}

@Composable
private fun LeaseDocumentRow(
    document: DocumentItem,
    onOpen: () -> Unit
) {
    Box(
        modifier = Modifier
            .fillMaxWidth()
            .height(82.dp)
            .glassCard(cornerRadius = 16.dp)
            .then(if (document.url != null) Modifier.clickable(onClick = onOpen) else Modifier)
            .padding(14.dp),
    ) {
        Row(
            horizontalArrangement = Arrangement.spacedBy(14.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Icon(systemIcon("doc.fill"), contentDescription = null, tint = DoorTreeTheme.leaseAccent)
            Column(
                modifier = Modifier.weight(1f),
                verticalArrangement = Arrangement.spacedBy(2.dp)
            ) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(8.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text(
                        text = document.filename,
                        color = DoorTreeTheme.textPrimary,
                        maxLines = 1,
                        modifier = Modifier.weight(1f)
                    )
                    if (document.requiresRenewalAction) {
                        Text(
                            text = L("Action Required"),
                            color = DoorTreeTheme.destructive,
                            fontWeight = FontWeight.SemiBold,
                            maxLines = 1,
                            modifier = Modifier
                                .background(
                                    DoorTreeTheme.destructive.copy(alpha = 0.14f),
                                    RoundedCornerShape(999.dp)
                                )
                                .padding(horizontal = 8.dp, vertical = 3.dp)
                        )
                    }
                }
                if (document.subtitle.isNotBlank()) {
                    Text(text = document.subtitle, color = DoorTreeTheme.textSecondary, maxLines = 1)
                }
            }
            Icon(systemIcon("arrow.down.circle.fill"), contentDescription = null, tint = DoorTreeTheme.textSecondary)
        }

        if (document.shouldShowNotificationBadge) {
            NotificationDotBadge(
                modifier = Modifier.align(Alignment.TopEnd)
            )
        }
    }
}

@Composable
private fun LeaseSummaryRow(label: String, value: String) {
    Row(modifier = Modifier.fillMaxWidth(), verticalAlignment = Alignment.CenterVertically) {
        Text(text = label, color = DoorTreeTheme.textSecondary)
        Spacer(modifier = Modifier.weight(1f))
        Text(text = value, color = DoorTreeTheme.textPrimary)
    }
}
