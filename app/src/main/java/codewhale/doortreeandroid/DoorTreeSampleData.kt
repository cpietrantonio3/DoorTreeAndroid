package codewhale.doortreeandroid

import codewhale.doortreeandroid.ui.theme.DoorTreeTheme

object DoorTreeSampleData {
    val quickActions = listOf(
        QuickActionItem(
            title = L("mock.quick.pay_rent.title"),
            subtitle = L("mock.quick.pay_rent.subtitle"),
            icon = "creditcard.fill",
            iconColor = DoorTreeTheme.gradientStart,
            iconBackground = DoorTreeTheme.paidBackground.copy(alpha = 0.45f),
            route = QuickActionRoute.Payments
        ),
        QuickActionItem(
            title = L("mock.quick.maintenance.title"),
            subtitle = L("mock.quick.maintenance.subtitle"),
            icon = "wrench.and.screwdriver.fill",
            iconColor = DoorTreeTheme.dueText,
            iconBackground = DoorTreeTheme.dueBackground.copy(alpha = 0.75f),
            route = QuickActionRoute.Requests
        ),
        QuickActionItem(
            title = L("mock.quick.chat.title"),
            subtitle = L("mock.quick.chat.subtitle"),
            icon = "bubble.left.and.bubble.right.fill",
            iconColor = DoorTreeTheme.chatAccent,
            iconBackground = DoorTreeTheme.chatAccentBackground.copy(alpha = 0.75f),
            route = QuickActionRoute.Chat
        ),
        QuickActionItem(
            title = L("mock.quick.lease.title"),
            subtitle = L("mock.quick.lease.subtitle"),
            icon = "doc.text.fill",
            iconColor = DoorTreeTheme.leaseAccent,
            iconBackground = DoorTreeTheme.leaseAccentBackground.copy(alpha = 0.75f),
            route = QuickActionRoute.Lease
        )
    )

    val paymentMethods = listOf(
        PaymentMethodItem(
            title = "Manual monthly pay",
            subtitle = "Open Stripe and pay manually each month when rent is due.",
            icon = "creditcard.fill",
            kind = PaymentMethodItem.Kind.ManualMonthly
        ),
        PaymentMethodItem(
            title = "Automatic card pay",
            subtitle = "Save a credit card once for automatic monthly rent payments.",
            icon = "creditcard.circle.fill",
            kind = PaymentMethodItem.Kind.AutopayCard
        ),
        PaymentMethodItem(
            title = "Automatic bank debit",
            subtitle = "Save bank details once for automatic monthly PAD rent payments.",
            icon = "building.columns.fill",
            kind = PaymentMethodItem.Kind.AutopayBank
        )
    )

    val notificationPreferences = NotificationPreferences.Default
}
