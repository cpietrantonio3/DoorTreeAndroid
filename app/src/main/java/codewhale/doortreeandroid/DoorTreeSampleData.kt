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
            title = L("mock.payment_method.credit_card.title"),
            subtitle = L("mock.payment_method.credit_card.subtitle"),
            icon = "creditcard.fill"
        ),
        PaymentMethodItem(
            title = L("mock.payment_method.bank_transfer.title"),
            subtitle = L("mock.payment_method.bank_transfer.subtitle"),
            icon = "building.columns.fill"
        ),
        PaymentMethodItem(
            title = L("mock.payment_method.apple_pay.title"),
            subtitle = L("mock.payment_method.apple_pay.subtitle"),
            icon = "iphone"
        )
    )

    val notificationPreferences = NotificationPreferences.Default
}
