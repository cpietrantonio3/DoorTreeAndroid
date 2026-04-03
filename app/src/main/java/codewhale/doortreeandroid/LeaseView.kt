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
import androidx.compose.foundation.shape.RoundedCornerShape
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
                val entries = RentScheduleBuilder.entries(tenantRecord = tenantDataStore.tenantRecord, leaseDetails = tenantDataStore.leaseDetails)
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
    Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
        Text(text = L("lease.documents"), color = DoorTreeTheme.textPrimary)
        if (tenantDataStore.documents.isEmpty()) {
            SectionPlaceholder(
                systemName = "doc.text",
                title = "No lease documents yet",
                message = "Lease documents will appear here when they are linked to this tenant."
            )
        } else {
            tenantDataStore.documents.forEach { document ->
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .glassCard(cornerRadius = 16.dp)
                        .padding(14.dp),
                    horizontalArrangement = Arrangement.spacedBy(14.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Icon(systemIcon("doc.fill"), contentDescription = null, tint = DoorTreeTheme.leaseAccent)
                    Text(text = document.filename, color = DoorTreeTheme.textPrimary, modifier = Modifier.weight(1f))
                    Icon(systemIcon("arrow.down.circle.fill"), contentDescription = null, tint = DoorTreeTheme.textSecondary)
                }
            }
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
