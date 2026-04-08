package codewhale.doortreeandroid

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
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
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import codewhale.doortreeandroid.ui.theme.DoorTreeTheme
import java.time.format.DateTimeFormatter

@Composable
fun PayRentView(tenantDataStore: TenantDataStore) {
    var selectedMethodId by remember(tenantDataStore.paymentMethods) {
        mutableStateOf(tenantDataStore.paymentMethods.firstOrNull()?.id)
    }
    var checkoutRequest by remember { mutableStateOf<HostedCheckoutRequest?>(null) }

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
            verticalArrangement = Arrangement.spacedBy(18.dp)
        ) {
            Text(
                text = L("tab.payments"),
                color = DoorTreeTheme.textPrimary,
                fontWeight = FontWeight.Bold,
                fontSize = 28.sp
            )
            PayRentHeroCard(tenantDataStore = tenantDataStore)

            Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
                Text(text = L("payments.method_section"), color = DoorTreeTheme.textSecondary)
                tenantDataStore.paymentMethods.forEach { method ->
                    PaymentMethodRow(
                        method = method,
                        isSelected = selectedMethodId == method.id,
                        onClick = { selectedMethodId = method.id }
                    )
                }
            }

            GradientButton(
                title = L("payments.pay_now"),
                enabled = canStartPaymentFlow(tenantDataStore, selectedMethodId),
                onClick = {
                    when (tenantDataStore.paymentMethods.firstOrNull { it.id == selectedMethodId }?.kind) {
                        PaymentMethodItem.Kind.OnlinePayment -> {
                            val request = hostedCheckoutRequest(tenantDataStore, selectedMethodId)
                            if (request != null) {
                                checkoutRequest = request
                            }
                        }

                        PaymentMethodItem.Kind.BankTransfer -> {
                            Unit
                        }

                        null -> Unit
                    }
                }
            )
            checkoutUnavailableMessage(tenantDataStore, selectedMethodId)?.let { message ->
                Text(
                    text = message,
                    color = DoorTreeTheme.textSecondary
                )
            }

            Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
                Text(text = L("payments.schedule_section"), color = DoorTreeTheme.textSecondary)
                val entries = tenantDataStore.rentScheduleEntries
                if (entries.isEmpty()) {
                    SectionPlaceholder(
                        systemName = "calendar.badge.clock",
                        title = L("payments.schedule.empty_title"),
                        message = L("payments.schedule.empty_message")
                    )
                } else {
                    entries.forEach { entry ->
                        RentScheduleRow(entry = entry)
                    }
                }
            }

            Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
                Text(text = L("payments.history_section"), color = DoorTreeTheme.textSecondary)
                if (tenantDataStore.completedPayments.isEmpty()) {
                    SectionPlaceholder(
                        systemName = "clock.arrow.circlepath",
                        title = "No completed payments yet",
                        message = "Past rent payments will appear here once payments are recorded."
                    )
                } else {
                    tenantDataStore.completedPayments.forEach { payment ->
                        PaymentRow(
                            payment = payment,
                            badgeForegroundOverride = if (payment.status == StatusBadgeStyle.Paid) DoorTreeTheme.paidText else null,
                            badgeBackgroundOverride = if (payment.status == StatusBadgeStyle.Paid) DoorTreeTheme.paidBackground else null
                        )
                    }
                }
            }
        }
    }

    checkoutRequest?.let { request ->
        HostedCheckoutSheetView(
            url = request.url,
            title = request.title,
            onDismiss = { checkoutRequest = null }
        )
    }
}

@Composable
private fun PayRentHeroCard(tenantDataStore: TenantDataStore) {
    val lease = tenantDataStore.leaseDetails
    val currentRentEntry = tenantDataStore.nextRentEntry ?: tenantDataStore.currentRentEntry
    val leaseEnded = tenantDataStore.tenantRecord?.leaseEnded == true
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .liquidGlassSurface(cornerRadius = 18.dp, tint = DoorTreeTheme.gradientStart.copy(alpha = 0.18f))
            .padding(16.dp),
        verticalArrangement = Arrangement.spacedBy(12.dp)
    ) {
        Text(
            text = currentRentEntry?.sortDate?.format(DateTimeFormatter.ofPattern("LLLL yyyy", java.util.Locale.getDefault()))
                ?: L("payments.hero.title"),
            color = DoorTreeTheme.textPrimary
        )
        Text(text = lease.monthlyRent, color = DoorTreeTheme.textPrimary)
        Row(verticalAlignment = Alignment.CenterVertically) {
            Text(
                text = currentRentEntry?.dueDateDisplay
                    ?: if (lease.endDate == "-") "Current monthly rent" else "Lease ends ${lease.endDate}",
                color = DoorTreeTheme.textSecondary
            )
            Spacer(modifier = Modifier.weight(1f))
            StatusBadge(
                status = currentRentEntry?.statusStyle ?: if (leaseEnded) StatusBadgeStyle.Completed else StatusBadgeStyle.Due,
                label = currentRentEntry?.statusLabel ?: if (leaseEnded) "Lease ended" else "Current rent"
            )
        }
    }
}

private data class HostedCheckoutRequest(
    val url: String,
    val title: String
)

private fun hostedCheckoutRequest(
    tenantDataStore: TenantDataStore,
    selectedMethodId: String?
): HostedCheckoutRequest? {
    val selectedMethod = tenantDataStore.paymentMethods.firstOrNull { it.id == selectedMethodId } ?: return null
    if (selectedMethod.kind != PaymentMethodItem.Kind.OnlinePayment) {
        return null
    }
    val url = tenantDataStore.hostedCheckoutUrl(selectedMethod.kind)?.takeIf { it.isNotBlank() } ?: return null
    return HostedCheckoutRequest(
        url = url,
        title = selectedMethod.title
    )
}

private fun canStartPaymentFlow(
    tenantDataStore: TenantDataStore,
    selectedMethodId: String?
): Boolean {
    val selectedMethod = tenantDataStore.paymentMethods.firstOrNull { it.id == selectedMethodId } ?: return false
    return when (selectedMethod.kind) {
        PaymentMethodItem.Kind.OnlinePayment -> hostedCheckoutRequest(tenantDataStore, selectedMethodId) != null
        PaymentMethodItem.Kind.BankTransfer -> false
    }
}

private fun checkoutUnavailableMessage(
    tenantDataStore: TenantDataStore,
    selectedMethodId: String?
): String? {
    val selectedMethod = tenantDataStore.paymentMethods.firstOrNull { it.id == selectedMethodId } ?: return null

    return when (selectedMethod.kind) {
        PaymentMethodItem.Kind.OnlinePayment -> null
        PaymentMethodItem.Kind.BankTransfer -> null
    }
}

@Composable
private fun PaymentMethodRow(
    method: PaymentMethodItem,
    isSelected: Boolean,
    onClick: () -> Unit
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .liquidGlassSurface(
                cornerRadius = 16.dp,
                interactive = true,
                tint = if (isSelected) DoorTreeTheme.gradientStart.copy(alpha = 0.18f) else Color.Unspecified
            )
            .clickable(onClick = onClick)
            .padding(14.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(12.dp)
    ) {
        Box(
            modifier = Modifier
                .background(DoorTreeTheme.paidBackground.copy(alpha = 0.45f), RoundedCornerShape(14.dp))
                .padding(14.dp)
        ) {
            Icon(systemIcon(method.icon), contentDescription = null, tint = DoorTreeTheme.gradientStart)
        }

        Column(modifier = Modifier.weight(1f), verticalArrangement = Arrangement.spacedBy(4.dp)) {
            Text(text = method.title, color = DoorTreeTheme.textPrimary)
            Text(text = method.subtitle, color = DoorTreeTheme.textSecondary)
        }

        Box(
            modifier = Modifier
                .border(
                    width = 2.dp,
                    color = if (isSelected) DoorTreeTheme.gradientStart else DoorTreeTheme.textSecondary.copy(alpha = 0.35f),
                    shape = RoundedCornerShape(999.dp)
                )
                .padding(5.dp)
        ) {
            Box(
                modifier = Modifier
                    .background(
                        color = if (isSelected) DoorTreeTheme.gradientStart else Color.Transparent,
                        shape = RoundedCornerShape(999.dp)
                    )
                    .padding(6.dp)
            )
        }
    }
}
