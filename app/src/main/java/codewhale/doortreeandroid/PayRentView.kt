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
    var managementPrompt by remember { mutableStateOf<SavedPaymentManagementPrompt?>(null) }

    suspend fun continuePaymentFlow(
        kind: PaymentMethodItem.Kind,
        managementMode: Boolean = false
    ) {
        paymentFlowMessage = null
        isStartingPaymentFlow = true

        try {
            val url = tenantDataStore.startRentPaymentFlow(kind, managementMode)
            isStartingPaymentFlow = false

            if (!url.isNullOrBlank()) {
                val selectedMethod = paymentMethods.firstOrNull { it.kind == kind }
                checkoutRequest = HostedCheckoutRequest(
                    url = url,
                    title = selectedMethod?.title ?: L("tab.payments")
                )
            } else {
                val refreshedRentPayment = tenantDataStore.currentRentPayment
                paymentFlowMessage = when (kind) {
                    PaymentMethodItem.Kind.ManualMonthly -> null
                    PaymentMethodItem.Kind.AutopayCard -> L("payments.message.saved_card_activated")
                    PaymentMethodItem.Kind.AutopayBank ->
                        if (refreshedRentPayment.isBankAutopayVerificationPending) {
                            L("payments.message.saved_bank_verification_pending")
                        } else {
                            L("payments.message.saved_bank_activated")
                        }
                }
            }
        } catch (error: Throwable) {
            isStartingPaymentFlow = false
            paymentFlowMessage = error.message
        }
    }

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
                    val prompt = managementPromptFor(tenantDataStore, selectedMethod.kind)
                    if (prompt != null) {
                        paymentFlowMessage = null
                        managementPrompt = prompt
                    } else {
                        scope.launch {
                            continuePaymentFlow(selectedMethod.kind)
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

    managementPrompt?.let { prompt ->
        AlertDialog(
            onDismissRequest = { managementPrompt = null },
            title = {
                Text(text = prompt.title)
            },
            text = {
                Text(text = prompt.message)
            },
            confirmButton = {
                TextButton(
                    onClick = {
                        managementPrompt = null
                        scope.launch {
                            continuePaymentFlow(prompt.kind, managementMode = true)
                        }
                    }
                ) {
                    Text(text = prompt.confirmTitle)
                }
            },
            dismissButton = {
                TextButton(onClick = { managementPrompt = null }) {
                    Text(text = prompt.cancelTitle)
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

private data class SavedPaymentManagementPrompt(
    val cancelTitle: String,
    val confirmTitle: String,
    val kind: PaymentMethodItem.Kind,
    val message: String,
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
        return L("payments.action.working")
    }

    val selectedMethod = tenantDataStore.paymentMethods.firstOrNull { it.id == selectedMethodId }
    val currentRentPayment = tenantDataStore.currentRentPayment
    val isPendingCardSetup = currentRentPayment.pendingSetupMethodType == "card" && !currentRentPayment.hasSavedCardStripeProfile
    val isPendingBankSetup = currentRentPayment.pendingSetupMethodType == "acss_debit" && !currentRentPayment.hasSavedBankStripeProfile

    return when (selectedMethod?.kind) {
        PaymentMethodItem.Kind.ManualMonthly -> L("payments.action.continue_to_stripe")
        PaymentMethodItem.Kind.AutopayCard -> {
            if (isPendingCardSetup) {
                L("payments.action.continue_card_setup")
            } else if (currentRentPayment.isCardAutopayActive) {
                L("payments.action.manage_saved_card")
            } else if (currentRentPayment.hasSavedCardStripeProfile) {
                L("payments.action.use_saved_card")
            } else {
                L("payments.action.setup_card")
            }
        }

        PaymentMethodItem.Kind.AutopayBank -> {
            if (isPendingBankSetup) {
                L("payments.action.continue_bank_setup")
            } else if (currentRentPayment.isBankAutopayActive || currentRentPayment.isBankAutopayVerificationPending) {
                L("payments.action.manage_saved_bank")
            } else if (currentRentPayment.hasSavedBankStripeProfile) {
                L("payments.action.use_saved_bank")
            } else {
                L("payments.action.setup_bank")
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
    val isPendingCardSetup = currentRentPayment.pendingSetupMethodType == "card" && !currentRentPayment.hasSavedCardStripeProfile
    val isPendingBankSetup = currentRentPayment.pendingSetupMethodType == "acss_debit" && !currentRentPayment.hasSavedBankStripeProfile
    return when (selectedMethod.kind) {
        PaymentMethodItem.Kind.ManualMonthly -> when {
            tenantDataStore.currentRentEntry?.isAutopayProcessing == true ->
                L("payments.error.autopay_processing")

            selectedHostedCheckoutUrl(tenantDataStore, selectedMethodId).isNullOrBlank() ->
                L("payments.error.link_preparing")

            else -> null
        }

        PaymentMethodItem.Kind.AutopayCard -> {
            if (isPendingCardSetup) {
                L("payments.method.card.subtitle.setup_pending")
            } else if (currentRentPayment.isCardAutopayActive) {
                if (!currentRentPayment.paymentMethodLabel.isNullOrBlank()) {
                    LF("payments.message.card.active_with_label", currentRentPayment.paymentMethodLabel)
                } else {
                    L("payments.message.card.active_default")
                }
            } else if (currentRentPayment.hasSavedCardStripeProfile) {
                if (!currentRentPayment.paymentMethodLabel.isNullOrBlank()) {
                    LF("payments.message.card.saved_with_label", currentRentPayment.paymentMethodLabel)
                } else {
                    L("payments.message.card.saved_default")
                }
            } else if (tenantDataStore.hasCardRentPaymentManagement) {
                L("payments.message.card.management_available")
            } else {
                null
            }
        }

        PaymentMethodItem.Kind.AutopayBank -> when {
            isPendingBankSetup ->
                L("payments.method.bank.subtitle.setup_pending")

            currentRentPayment.isBankAutopayVerificationPending ->
                L("payments.method.bank.subtitle.verification_pending")

            currentRentPayment.isBankAutopayActive ->
                if (!currentRentPayment.paymentMethodLabel.isNullOrBlank()) {
                    LF("payments.message.bank.active_with_label", currentRentPayment.paymentMethodLabel)
                } else {
                    L("payments.message.bank.active_default")
                }

            currentRentPayment.hasSavedBankStripeProfile ->
                if (!currentRentPayment.paymentMethodLabel.isNullOrBlank()) {
                    LF("payments.message.bank.saved_with_label", currentRentPayment.paymentMethodLabel)
                } else {
                    L("payments.message.bank.saved_default")
                }

            tenantDataStore.hasBankRentPaymentManagement ->
                L("payments.message.bank.management_available")

            else -> null
        }
    }
}

private fun managementPromptFor(
    tenantDataStore: TenantDataStore,
    kind: PaymentMethodItem.Kind
): SavedPaymentManagementPrompt? {
    val currentRentPayment = tenantDataStore.currentRentPayment

    return when (kind) {
        PaymentMethodItem.Kind.ManualMonthly -> null
        PaymentMethodItem.Kind.AutopayCard -> {
            if (!currentRentPayment.isCardAutopayActive) {
                return null
            }

            val message = if (!currentRentPayment.paymentMethodLabel.isNullOrBlank()) {
                LF("payments.alert.saved_card.message_with_label", currentRentPayment.paymentMethodLabel)
            } else {
                L("payments.alert.saved_card.message_default")
            }

            SavedPaymentManagementPrompt(
                cancelTitle = L("payments.alert.saved_card.cancel"),
                confirmTitle = L("payments.alert.saved_card.confirm"),
                kind = kind,
                message = message,
                title = L("payments.alert.saved_card.title")
            )
        }

        PaymentMethodItem.Kind.AutopayBank -> {
            if (!currentRentPayment.isBankAutopayActive && !currentRentPayment.isBankAutopayVerificationPending) {
                return null
            }

            val message = if (!currentRentPayment.paymentMethodLabel.isNullOrBlank()) {
                LF("payments.alert.saved_bank.message_with_label", currentRentPayment.paymentMethodLabel)
            } else {
                L("payments.alert.saved_bank.message_default")
            }

            SavedPaymentManagementPrompt(
                cancelTitle = L("payments.alert.saved_bank.cancel"),
                confirmTitle = L("payments.alert.saved_bank.confirm"),
                kind = kind,
                message = message,
                title = L("payments.alert.saved_bank.title")
            )
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
