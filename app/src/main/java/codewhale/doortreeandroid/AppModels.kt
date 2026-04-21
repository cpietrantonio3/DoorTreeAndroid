package codewhale.doortreeandroid

import androidx.compose.ui.graphics.Color
import codewhale.doortreeandroid.ui.theme.DoorTreeTheme
import java.time.LocalDate
import java.time.format.DateTimeFormatter
import java.time.format.FormatStyle
import java.util.Locale
import java.util.UUID

data class AuthUser(
    val uid: String,
    val email: String
)

data class TenantProfile(
    val name: String,
    val email: String,
    val phone: String,
    val initials: String
)

data class PropertyInfo(
    val name: String,
    val unit: String,
    val address: String,
    val city: String
) {
    val subtitle: String
        get() = LF("property.subtitle", name, unit)
}

data class LeaseDetails(
    val startDate: String,
    val endDate: String,
    val monthlyRent: String,
    val unitLabel: String,
    val renewalNotice: String
)

data class RentStripeAutopayDetails(
    val lastAttemptAt: String?,
    val lastChargeId: String?,
    val lastError: String?,
    val lastPaymentIntentId: String?,
    val lastProcessedAt: String?,
    val lastStatus: String?,
    val methodType: String?
)

data class RentStripeDetails(
    val autopay: RentStripeAutopayDetails?,
    val isActive: Boolean,
    val paymentLinkId: String,
    val paymentLinkUrl: String
)

data class TenantRentPaymentState(
    val createdAt: String,
    val lastAutopayAt: String?,
    val lastAutopayChargeId: String?,
    val lastAutopayPaymentIntentId: String?,
    val lastAutopayStatus: String?,
    val lastAutopaySucceededAt: String?,
    val lastError: String?,
    val lastSetupAt: String?,
    val lastSetupError: String?,
    val paymentMethodBrand: String?,
    val paymentMethodLabel: String?,
    val paymentMethodLast4: String?,
    val paymentMethodType: String?,
    val pendingSetupCheckoutSessionId: String?,
    val pendingSetupMethodType: String?,
    val selectedMethodType: String,
    val status: String,
    val stripeCustomerId: String?,
    val stripeMandateId: String?,
    val stripePaymentMethodId: String?,
    val stripeSetupIntentId: String?,
    val updatedAt: String
) {
    val currentPreferenceTitle: String
        get() = when {
            pendingSetupMethodType == "card" -> L("payments.preference.title.card_setup_in_progress")
            pendingSetupMethodType == "acss_debit" -> L("payments.preference.title.bank_setup_in_progress")
            selectedMethodType == "card" && status == "active" -> L("payments.preference.title.card_active")
            selectedMethodType == "acss_debit" && status == "verification_pending" -> L("payments.preference.title.bank_verification_pending")
            selectedMethodType == "acss_debit" && status == "active" -> L("payments.preference.title.bank_active")
            else -> L("payments.preference.title.manual")
        }

    val currentPreferenceSubtitle: String
        get() = when {
            pendingSetupMethodType == "card" -> L("payments.preference.subtitle.card_setup_in_progress")
            pendingSetupMethodType == "acss_debit" -> L("payments.preference.subtitle.bank_setup_in_progress")
            status == "verification_pending" -> L("payments.preference.subtitle.bank_verification_pending")
            status == "active" && !paymentMethodLabel.isNullOrBlank() -> LF("payments.preference.subtitle.active_with_label", paymentMethodLabel)
            else -> L("payments.preference.subtitle.manual")
        }

    val hasSavedCardStripeProfile: Boolean
        get() = paymentMethodType == "card" && hasStoredStripeProfile

    val hasSavedBankStripeProfile: Boolean
        get() = paymentMethodType == "acss_debit" && hasStoredStripeProfile

    val isCardAutopayActive: Boolean
        get() = selectedMethodType == "card" && status == "active"

    val isBankAutopayActive: Boolean
        get() = selectedMethodType == "acss_debit" && status == "active"

    val isBankAutopayVerificationPending: Boolean
        get() = selectedMethodType == "acss_debit" && status == "verification_pending"

    val hasStoredStripeProfile: Boolean
        get() = !stripeCustomerId.isNullOrBlank() ||
            !stripePaymentMethodId.isNullOrBlank() ||
            !stripeSetupIntentId.isNullOrBlank() ||
            !stripeMandateId.isNullOrBlank()

    companion object {
        val Empty = TenantRentPaymentState(
            createdAt = "",
            lastAutopayAt = null,
            lastAutopayChargeId = null,
            lastAutopayPaymentIntentId = null,
            lastAutopayStatus = null,
            lastAutopaySucceededAt = null,
            lastError = null,
            lastSetupAt = null,
            lastSetupError = null,
            paymentMethodBrand = null,
            paymentMethodLabel = null,
            paymentMethodLast4 = null,
            paymentMethodType = null,
            pendingSetupCheckoutSessionId = null,
            pendingSetupMethodType = null,
            selectedMethodType = "manual",
            status = "manual",
            stripeCustomerId = null,
            stripeMandateId = null,
            stripePaymentMethodId = null,
            stripeSetupIntentId = null,
            updatedAt = ""
        )
    }
}

data class TenantStripeConnectAssociationState(
    val accountId: String?,
    val bankPaymentMethodBrand: String?,
    val bankPaymentMethodLabel: String?,
    val bankPaymentMethodLast4: String?,
    val bankStatus: String?,
    val bankStripeMandateId: String?,
    val bankStripePaymentMethodId: String?,
    val bankStripeSetupIntentId: String?,
    val associated: Boolean,
    val cardPaymentMethodBrand: String?,
    val cardPaymentMethodLabel: String?,
    val cardPaymentMethodLast4: String?,
    val cardStatus: String?,
    val cardStripePaymentMethodId: String?,
    val cardStripeSetupIntentId: String?,
    val creditCardActive: Boolean,
    val debitActive: Boolean,
    val landlordUserId: String?,
    val linkedAt: String?,
    val status: String,
    val stripeCustomerId: String?,
    val stripeMandateId: String?,
    val stripePaymentMethodId: String?,
    val stripeSetupIntentId: String?,
    val tenantUserId: String?,
    val type: String?,
    val updatedAt: String?
) {
    companion object {
        val Empty = TenantStripeConnectAssociationState(
            accountId = null,
            bankPaymentMethodBrand = null,
            bankPaymentMethodLabel = null,
            bankPaymentMethodLast4 = null,
            bankStatus = null,
            bankStripeMandateId = null,
            bankStripePaymentMethodId = null,
            bankStripeSetupIntentId = null,
            associated = false,
            cardPaymentMethodBrand = null,
            cardPaymentMethodLabel = null,
            cardPaymentMethodLast4 = null,
            cardStatus = null,
            cardStripePaymentMethodId = null,
            cardStripeSetupIntentId = null,
            creditCardActive = false,
            debitActive = false,
            landlordUserId = null,
            linkedAt = null,
            status = "manual",
            stripeCustomerId = null,
            stripeMandateId = null,
            stripePaymentMethodId = null,
            stripeSetupIntentId = null,
            tenantUserId = null,
            type = null,
            updatedAt = null
        )
    }

    val hasSavedCardStripeProfile: Boolean
        get() = !stripeCustomerId.isNullOrBlank() && !cardStripePaymentMethodId.isNullOrBlank()

    val hasSavedBankStripeProfile: Boolean
        get() = !stripeCustomerId.isNullOrBlank() &&
            !bankStripePaymentMethodId.isNullOrBlank() &&
            (!bankStripeMandateId.isNullOrBlank() || !bankStripeSetupIntentId.isNullOrBlank())

    val isCardAutopayActive: Boolean
        get() = creditCardActive && hasSavedCardStripeProfile

    val isBankAutopayActive: Boolean
        get() = debitActive && bankStatus == "active" && hasSavedBankStripeProfile

    val isBankAutopayVerificationPending: Boolean
        get() = debitActive && bankStatus == "verification_pending" && hasSavedBankStripeProfile

    val hasConnectedCustomerProfile: Boolean
        get() = associated ||
            !bankStripeMandateId.isNullOrBlank() ||
            !bankStripePaymentMethodId.isNullOrBlank() ||
            !bankStripeSetupIntentId.isNullOrBlank() ||
            !cardStripePaymentMethodId.isNullOrBlank() ||
            !cardStripeSetupIntentId.isNullOrBlank() ||
            !stripeCustomerId.isNullOrBlank() ||
            !stripePaymentMethodId.isNullOrBlank() ||
            !stripeSetupIntentId.isNullOrBlank() ||
            !stripeMandateId.isNullOrBlank()
}

data class RentInteracDetails(
    val isActive: Boolean,
    val requestId: String,
    val requestUrl: String,
    val currency: String,
    val collectibleAmount: Double?,
    val status: String,
    val completedAt: String?
)

data class InteracRecipientSettings(
    val email: String,
    val displayName: String,
    val autodepositEnabled: Boolean,
    val isEnabled: Boolean
)

data class InteracTransferDetails(
    val id: String,
    val recipientEmail: String,
    val recipientName: String,
    val amount: String,
    val dueDate: String,
    val reference: String,
    val autodepositEnabled: Boolean
)

data class RentLedgerEntry(
    val id: String,
    val dueDate: String,
    val dueDateDisplay: String,
    val amountValue: Double?,
    val amount: String,
    val balanceValue: Double?,
    val balance: String,
    val statusLabel: String,
    val statusStyle: StatusBadgeStyle,
    val propertyName: String,
    val propertyManager: String,
    val leaseStart: String,
    val leaseEnd: String,
    val tenantName: String,
    val tenantEmail: String,
    val tenantUid: String,
    val unitNumber: String,
    val interac: RentInteracDetails?,
    val stripe: RentStripeDetails?,
    val sortDate: LocalDate?
) {
    val isPaid: Boolean
        get() = statusStyle == StatusBadgeStyle.Paid ||
            statusStyle == StatusBadgeStyle.Completed ||
            ((balanceValue ?: amountValue ?: 0.0) <= 0.0 && (balanceValue != null || amountValue != null))

    val isAutopayProcessing: Boolean
        get() = stripe?.autopay?.lastStatus == "processing" && stripe.autopay.lastChargeId == id

    fun hostedCheckoutUrl(kind: PaymentMethodItem.Kind): String? = when (kind) {
        PaymentMethodItem.Kind.ManualMonthly ->
            stripe?.takeIf { it.isActive && it.paymentLinkUrl.isNotBlank() }?.paymentLinkUrl

        PaymentMethodItem.Kind.AutopayCard,
        PaymentMethodItem.Kind.AutopayBank -> null
    }
}

enum class StatusBadgeStyle(val localizationKey: String) {
    Due("status.due"),
    Paid("status.paid"),
    InProgress("status.in_progress"),
    Completed("status.completed"),
    Pending("status.pending");

    val localizedLabel: String
        get() = L(localizationKey)
}

data class PaymentItem(
    val id: String = UUID.randomUUID().toString(),
    val month: String,
    val date: String,
    val amount: String,
    val status: StatusBadgeStyle
)

data class MaintenanceRequestItem(
    val id: String = UUID.randomUUID().toString(),
    val title: String,
    val category: String,
    val submittedDate: String,
    val submittedDateShort: String,
    val status: StatusBadgeStyle,
    val issue: String,
    val details: String,
    val priority: String,
    val preferredDate: String,
    val assignedTo: String,
    val propertyName: String,
    val unit: String,
    val tenantName: String,
    val internalNotes: String,
    val costEstimate: String,
    val createdAt: String,
    val updatedAt: String,
    val photos: List<String>,
    val sortDate: LocalDate? = null
)

data class PendingInvoiceItem(
    val id: String = UUID.randomUUID().toString(),
    val invoiceNumber: String,
    val propertyName: String,
    val recipientName: String,
    val recipientAddress: String,
    val recipientEmail: String,
    val recipientNumber: String,
    val issueDate: String,
    val dueDate: String,
    val createdAt: String,
    val updatedAt: String,
    val statusLabel: String,
    val notes: String,
    val terms: String,
    val subtotal: String,
    val tpsAmount: String,
    val tvqAmount: String,
    val total: String,
    val balance: String,
    val lineItems: List<InvoiceLineItem>,
    val stripe: PendingInvoiceStripeDetails? = null,
    val sortDate: LocalDate? = null
) {
    val hostedCheckoutUrl: String?
        get() = stripe
            ?.takeIf { it.isActive && it.paymentLinkUrl.isNotBlank() }
            ?.paymentLinkUrl
}

data class PendingInvoiceStripeDetails(
    val isActive: Boolean,
    val checkoutSessionId: String,
    val currency: String,
    val paymentLinkUrl: String
)

data class InvoiceLineItem(
    val id: String = UUID.randomUUID().toString(),
    val description: String,
    val category: String,
    val quantity: String,
    val unitPrice: String,
    val amount: String,
    val taxable: Boolean
)

enum class QuickActionRoute {
    Payments,
    Requests,
    Chat,
    Lease
}

data class QuickActionItem(
    val id: String = UUID.randomUUID().toString(),
    val title: String,
    val subtitle: String,
    val icon: String,
    val iconColor: Color,
    val iconBackground: Color,
    val route: QuickActionRoute
)

data class PaymentMethodItem(
    val id: String = UUID.randomUUID().toString(),
    val title: String,
    val subtitle: String,
    val icon: String,
    val kind: Kind
) {
    enum class Kind {
        ManualMonthly,
        AutopayCard,
        AutopayBank
    }
}

enum class MaintenanceCategory(
    val defaultTitle: String,
    val localizationKey: String,
    val icon: String
) {
    Plumbing("Plumbing", "maintenance.category.plumbing", "drop.fill"),
    Electrical("Electrical", "maintenance.category.electrical", "bolt.fill"),
    Hvac("HVAC", "maintenance.category.hvac", "fan.fill"),
    Heating("Heating", "maintenance.category.heating", "flame.fill"),
    Cooling("Cooling", "maintenance.category.cooling", "snowflake"),
    ApplianceRepair("Appliance Repair", "maintenance.category.appliance_repair", "washer.fill"),
    GeneralRepair("General Repair", "maintenance.category.general_repair", "hammer.fill"),
    Carpentry("Carpentry", "maintenance.category.carpentry", "square.split.diagonal.2x2.fill"),
    Painting("Painting", "maintenance.category.painting", "paintbrush.fill"),
    Flooring("Flooring", "maintenance.category.flooring", "square.grid.3x3.fill"),
    Locksmith("Locksmith", "maintenance.category.locksmith", "lock.fill"),
    PestControl("Pest Control", "maintenance.category.pest_control", "ladybug.fill"),
    Cleaning("Cleaning", "maintenance.category.cleaning", "sparkles"),
    Roofing("Roofing", "maintenance.category.roofing", "house.fill"),
    Exterior("Exterior", "maintenance.category.exterior", "building.2.fill"),
    Landscaping("Landscaping", "maintenance.category.landscaping", "leaf.fill"),
    SnowRemoval("Snow Removal", "maintenance.category.snow_removal", "snowflake.circle.fill"),
    Drywall("Drywall", "maintenance.category.drywall", "rectangle.split.3x1.fill"),
    WaterDamage("Water Damage", "maintenance.category.water_damage", "drop.triangle.fill"),
    Mold("Mold", "maintenance.category.mold", "aqi.medium"),
    Inspection("Inspection", "maintenance.category.inspection", "checklist"),
    PreventiveMaintenance("Preventive Maintenance", "maintenance.category.preventive_maintenance", "wrench.and.screwdriver.fill"),
    Emergency("Emergency", "maintenance.category.emergency", "exclamationmark.triangle.fill"),
    Other("Other", "maintenance.category.other", "ellipsis.circle.fill");

    val localizedTitle: String
        get() = L(localizationKey)
}

enum class MaintenancePriority(
    val defaultTitle: String,
    val localizationKey: String
) {
    Low("Low", "maintenance.priority.low"),
    Medium("Medium", "maintenance.priority.medium"),
    High("High", "maintenance.priority.high"),
    Urgent("Urgent", "maintenance.priority.urgent"),
    Emergency("Emergency", "maintenance.priority.emergency");

    val localizedTitle: String
        get() = L(localizationKey)
}

enum class ChatParticipant {
    Tenant,
    Landlord
}

data class ChatMessageItem(
    val id: String,
    val sender: ChatParticipant,
    val text: String,
    val timestamp: String,
    val senderUserId: String = "",
    val sentAtIso8601: String = "",
    val sentTimestamp: Long = 0L,
    val isRead: Boolean = true
)

data class ChatSection(
    val id: String,
    val title: String,
    val messages: List<ChatMessageItem>
)

data class DocumentItem(
    val id: String = UUID.randomUUID().toString(),
    val filename: String,
    val subtitle: String = "",
    val url: String? = null,
    val storageReference: String = "",
    val databasePath: String? = null,
    val read: Boolean = false,
    val isRenewalNotice: Boolean = false,
    val isActionTaken: Boolean = true,
    val status: String = ""
) {
    val requiresRenewalAction: Boolean
        get() = isRenewalNotice && !isActionTaken

    val shouldShowNotificationBadge: Boolean
        get() = !read || requiresRenewalAction

    val markingRead: DocumentItem
        get() = copy(read = true)

    fun markingRenewalActionTaken(status: String, url: String? = null): DocumentItem {
        return copy(
            url = url ?: this.url,
            read = true,
            isActionTaken = true,
            status = status
        )
    }
}

enum class NotificationCenterCategory(
    val icon: String,
    val iconColor: Color,
    val iconBackground: Color
) {
    Payment(
        "creditcard.fill",
        DoorTreeTheme.dueText,
        DoorTreeTheme.dueBackground.copy(alpha = 0.72f)
    ),
    Lease(
        "doc.text.fill",
        DoorTreeTheme.leaseAccent,
        DoorTreeTheme.leaseAccentBackground.copy(alpha = 0.82f)
    ),
    Message(
        "bubble.left.and.bubble.right.fill",
        DoorTreeTheme.chatAccent,
        DoorTreeTheme.chatAccentBackground.copy(alpha = 0.82f)
    ),
    Reminder(
        "bell.badge.fill",
        DoorTreeTheme.gradientStart,
        DoorTreeTheme.paidBackground.copy(alpha = 0.82f)
    )
}

data class NotificationCenterItem(
    val id: String,
    val title: String,
    val message: String,
    val timestamp: String,
    val category: NotificationCenterCategory,
    val isUnread: Boolean
)

enum class NotificationSettingKey(val firebaseKey: String) {
    PaymentReminders("paymentReminders"),
    MaintenanceUpdates("maintenanceUpdates"),
    Messages("messages")
}

data class NotificationPreferences(
    val paymentReminders: Boolean,
    val maintenanceUpdates: Boolean,
    val messages: Boolean,
    val faceID: Boolean
) {
    fun updating(key: NotificationSettingKey, isEnabled: Boolean): NotificationPreferences {
        return when (key) {
            NotificationSettingKey.PaymentReminders -> copy(paymentReminders = isEnabled)
            NotificationSettingKey.MaintenanceUpdates -> copy(maintenanceUpdates = isEnabled)
            NotificationSettingKey.Messages -> copy(messages = isEnabled)
        }
    }

    companion object {
        val Default = NotificationPreferences(
            paymentReminders = true,
            maintenanceUpdates = true,
            messages = true,
            faceID = true
        )
    }
}

data class RentScheduleEntry(
    val dueDate: LocalDate,
    val amount: String,
    val statusLabel: String,
    val accentColor: Color,
    val accentBackground: Color
) {
    val id: String
        get() = dueDate.toString()

    val monthLabel: String
        get() = dueDate.format(DateTimeFormatter.ofPattern("LLLL yyyy", Locale.getDefault()))

    val formattedDueDate: String
        get() = dueDate.format(DateTimeFormatter.ofLocalizedDate(FormatStyle.MEDIUM).withLocale(Locale.getDefault()))
}
