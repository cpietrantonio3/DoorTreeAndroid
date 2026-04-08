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
import androidx.compose.material3.Icon
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import codewhale.doortreeandroid.ui.theme.DoorTreeTheme
import java.time.format.DateTimeFormatter
import java.util.Locale
import kotlinx.coroutines.launch

@Composable
fun PayRentView(tenantDataStore: TenantDataStore) {
    val paymentMethods = tenantDataStore.paymentMethods
    val scope = rememberCoroutineScope()
    var selectedMethodId by remember { mutableStateOf<String?>(null) }
    var checkoutRequest by remember { mutableStateOf<HostedCheckoutRequest?>(null) }
    var isStartingPaymentFlow by remember { mutableStateOf(false) }
    var paymentFlowMessage by remember { mutableStateOf<String?>(null) }

    LaunchedEffect(paymentMethods) {
        if (selectedMethodId == null || paymentMethods.none { it.id == selectedMethodId }) {
            selectedMethodId = paymentMethods.firstOrNull()?.id
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

            PayRentHeroCard(tenantDataStore)

            Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
                Text(
                    text = L("payments.method_section"),
                    color = DoorTreeTheme.textSecondary,
                    fontWeight = FontWeight.SemiBold
                )

                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .liquidGlassSurface(
                            cornerRadius = 16.dp,
                            tint = DoorTreeTheme.gradientStart.copy(alpha = 0.12f)
                        )
                        .padding(14.dp),
                    verticalArrangement = Arrangement.spacedBy(4.dp)
                ) {
                    Text(
                        text = tenantDataStore.currentRentPayment.currentPreferenceTitle,
                        color = DoorTreeTheme.textPrimary,
                        fontWeight = FontWeight.SemiBold
                    )
                    Text(
                        text = tenantDataStore.currentRentPayment.currentPreferenceSubtitle,
                        color = DoorTreeTheme.textSecondary
                    )
                }

                paymentMethods.forEach { method ->
                    PaymentMethodRow(
                        method = method,
                        isSelected = selectedMethodId == method.id,
                        onClick = {
                            selectedMethodId = method.id
                            paymentFlowMessage = null
                        }
                    )
                }
            }

            GradientButton(
                title = primaryActionTitle(
                    tenantDataStore = tenantDataStore,
                    selectedMethodId = selectedMethodId,
                    isStartingPaymentFlow = isStartingPaymentFlow
                ),
                enabled = canStartPaymentFlow(tenantDataStore, selectedMethodId) && !isStartingPaymentFlow,
                onClick = {
                    val selectedMethod = paymentMethods.firstOrNull { it.id == selectedMethodId } ?: return@GradientButton
                    scope.launch {
                        paymentFlowMessage = null
                        isStartingPaymentFlow = true

                        try {
                            val url = tenantDataStore.startRentPaymentFlow(selectedMethod.kind)
                            isStartingPaymentFlow = false

                            if (!url.isNullOrBlank()) {
                                checkoutRequest = HostedCheckoutRequest(
                                    url = url,
                                    title = selectedMethod.title
                                )
                            } else {
                                paymentFlowMessage = when (selectedMethod.kind) {
                                    PaymentMethodItem.Kind.ManualMonthly -> null
                                    PaymentMethodItem.Kind.AutopayCard -> "Card autopay is already active for your rent."
                                    PaymentMethodItem.Kind.AutopayBank -> "Bank autopay is already active for your rent."
                                }
                            }
                        } catch (error: Throwable) {
                            isStartingPaymentFlow = false
                            paymentFlowMessage = error.message
                        }
                    }
                }
            )

            paymentStatusMessage(
                tenantDataStore = tenantDataStore,
                selectedMethodId = selectedMethodId,
                paymentFlowMessage = paymentFlowMessage
            )?.let { message ->
                Text(
                    text = message,
                    color = DoorTreeTheme.textSecondary
                )
            }

            Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
                Text(
                    text = L("payments.schedule_section"),
                    color = DoorTreeTheme.textSecondary,
                    fontWeight = FontWeight.SemiBold
                )
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
                Text(
                    text = L("payments.history_section"),
                    color = DoorTreeTheme.textSecondary,
                    fontWeight = FontWeight.SemiBold
                )
                if (tenantDataStore.completedPayments.isEmpty()) {
                    SectionPlaceholder(
                        systemName = "clock.arrow.circlepath",
                        title = "No completed payments yet",
                        message = "Past rent payments will appear here after they are synced."
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
            onDismiss = {
                checkoutRequest = null
                scope.launch {
                    tenantDataStore.refresh()
                }
            }
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
            text = currentRentEntry?.sortDate?.format(DateTimeFormatter.ofPattern("LLLL yyyy", Locale.getDefault()))
                ?: L("payments.hero.title"),
            color = DoorTreeTheme.textPrimary,
            fontWeight = FontWeight.SemiBold
        )
        Text(
            text = lease.monthlyRent,
            color = DoorTreeTheme.textPrimary,
            fontWeight = FontWeight.Bold,
            fontSize = 32.sp
        )
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

private fun selectedHostedCheckoutUrl(
    tenantDataStore: TenantDataStore,
    selectedMethodId: String?
): String? {
    val selectedMethod = tenantDataStore.paymentMethods.firstOrNull { it.id == selectedMethodId } ?: return null
    if (selectedMethod.kind != PaymentMethodItem.Kind.ManualMonthly) {
        return null
    }
    return tenantDataStore.hostedCheckoutUrl(PaymentMethodItem.Kind.ManualMonthly)?.takeIf { it.isNotBlank() }
}

private fun canStartPaymentFlow(
    tenantDataStore: TenantDataStore,
    selectedMethodId: String?
): Boolean {
    val selectedMethod = tenantDataStore.paymentMethods.firstOrNull { it.id == selectedMethodId } ?: return false
    return when (selectedMethod.kind) {
        PaymentMethodItem.Kind.ManualMonthly ->
            selectedHostedCheckoutUrl(tenantDataStore, selectedMethodId) != null &&
                tenantDataStore.currentRentEntry?.isAutopayProcessing != true

        PaymentMethodItem.Kind.AutopayCard,
        PaymentMethodItem.Kind.AutopayBank -> true
    }
}

private fun primaryActionTitle(
    tenantDataStore: TenantDataStore,
    selectedMethodId: String?,
    isStartingPaymentFlow: Boolean
): String {
    if (isStartingPaymentFlow) {
        return "Working..."
    }

    val selectedMethod = tenantDataStore.paymentMethods.firstOrNull { it.id == selectedMethodId }
    val currentRentPayment = tenantDataStore.currentRentPayment
    val hasStripeManagement = tenantDataStore.stripeConnectAssociation.associated

    return when (selectedMethod?.kind) {
        PaymentMethodItem.Kind.ManualMonthly -> "Continue to Stripe"
        PaymentMethodItem.Kind.AutopayCard -> {
            if (currentRentPayment.pendingSetupMethodType == "card") {
                "Continue Card Setup"
            } else if (hasStripeManagement) {
                "Manage Card Autopay"
            } else {
                "Set Up Card Autopay"
            }
        }

        PaymentMethodItem.Kind.AutopayBank -> {
            if (currentRentPayment.pendingSetupMethodType == "acss_debit") {
                "Continue Bank Setup"
            } else if (hasStripeManagement) {
                "Manage Bank Autopay"
            } else {
                "Set Up Bank Autopay"
            }
        }

        null -> L("payments.pay_now")
    }
}

private fun paymentStatusMessage(
    tenantDataStore: TenantDataStore,
    selectedMethodId: String?,
    paymentFlowMessage: String?
): String? {
    if (!paymentFlowMessage.isNullOrBlank()) {
        return paymentFlowMessage
    }

    val currentRentPayment = tenantDataStore.currentRentPayment
    currentRentPayment.lastSetupError?.takeIf { it.isNotBlank() }?.let { return it }
    currentRentPayment.lastError?.takeIf { it.isNotBlank() }?.let { return it }

    val selectedMethod = tenantDataStore.paymentMethods.firstOrNull { it.id == selectedMethodId } ?: return null
    return when (selectedMethod.kind) {
        PaymentMethodItem.Kind.ManualMonthly -> when {
            tenantDataStore.currentRentEntry?.isAutopayProcessing == true ->
                "An automatic payment is already processing for the current rent charge."

            selectedHostedCheckoutUrl(tenantDataStore, selectedMethodId).isNullOrBlank() ->
                "Stripe is still preparing the rent payment link."

            else -> null
        }

        PaymentMethodItem.Kind.AutopayCard -> {
            if (currentRentPayment.pendingSetupMethodType == "card") {
                "Stripe is waiting for you to finish the hosted card setup."
            } else {
                null
            }
        }

        PaymentMethodItem.Kind.AutopayBank -> when {
            currentRentPayment.pendingSetupMethodType == "acss_debit" ->
                "Stripe is waiting for you to finish the hosted bank setup."

            currentRentPayment.selectedMethodType == "acss_debit" &&
                currentRentPayment.status == "verification_pending" ->
                "Stripe may still need bank verification before automatic PAD payments become active."

            else -> null
        }
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
                .background(
                    color = if (isSelected) DoorTreeTheme.gradientStart else Color.Transparent,
                    shape = RoundedCornerShape(999.dp)
                )
                .size(18.dp)
                .padding(3.dp)
        ) {
            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .background(
                        color = if (isSelected) DoorTreeTheme.gradientStart else Color.Transparent,
                        shape = RoundedCornerShape(999.dp)
                    )
                    .padding(6.dp)
            )
        }
    }
}
