@file:OptIn(ExperimentalMaterial3Api::class)

package codewhale.doortreeandroid

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.material3.Badge
import androidx.compose.material3.BadgedBox
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.NavigationBar
import androidx.compose.material3.NavigationBarItem
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import codewhale.doortreeandroid.ui.theme.DoorTreeTheme

private enum class DoorTreeTab(val titleKey: String, val icon: String) {
    Home("tab.home", "house.fill"),
    Payments("tab.payments", "creditcard.fill"),
    Requests("tab.requests", "wrench.and.screwdriver.fill"),
    Chat("tab.chat", "bubble.left.fill"),
    Profile("tab.profile", "person.crop.circle.fill")
}

@Composable
fun ContentView(
    authSession: AuthSessionStore,
    tenantDataStore: TenantDataStore
) {
    var selectedTab by remember { mutableStateOf(DoorTreeTab.Home) }
    var paymentViewMode by remember { mutableStateOf(PaymentViewMode.Rent) }
    var showingLease by remember { mutableStateOf(false) }
    var showingMaintenanceRequest by remember { mutableStateOf(false) }
    var selectedInvoice by remember { mutableStateOf<PendingInvoiceItem?>(null) }
    var selectedRequest by remember { mutableStateOf<MaintenanceRequestItem?>(null) }
    val chatBadgeCount = if (selectedTab == DoorTreeTab.Chat) 0 else tenantDataStore.unreadChatCount
    val pendingInvoiceBadgeCount = tenantDataStore.pendingInvoices.size
    val duePaymentBadgeCount = tenantDataStore.duePaymentCount

    LaunchedEffect(selectedTab) {
        tenantDataStore.setChatOpen(selectedTab == DoorTreeTab.Chat)
    }

    Box(modifier = Modifier.fillMaxSize().background(DoorTreeTheme.backgroundPrimary)) {
        Column(modifier = Modifier.fillMaxSize()) {
            Box(modifier = Modifier.weight(1f)) {
                when (selectedTab) {
                    DoorTreeTab.Home -> HomeView(
                        tenantDataStore = tenantDataStore,
                        onOpenRequest = { request -> selectedRequest = request },
                        onSelectProfile = { selectedTab = DoorTreeTab.Profile },
                        onSelectAction = { route ->
                            when (route) {
                                QuickActionRoute.Payments -> {
                                    paymentViewMode = PaymentViewMode.Rent
                                    selectedTab = DoorTreeTab.Payments
                                }
                                QuickActionRoute.Parking -> {
                                    paymentViewMode = PaymentViewMode.Parking
                                    selectedTab = DoorTreeTab.Payments
                                }
                                QuickActionRoute.Requests -> showingMaintenanceRequest = true
                                QuickActionRoute.Chat -> selectedTab = DoorTreeTab.Chat
                                QuickActionRoute.Lease -> showingLease = true
                            }
                        }
                    )
                    DoorTreeTab.Payments -> PayRentView(
                        tenantDataStore = tenantDataStore,
                        mode = paymentViewMode,
                        onModeChange = { paymentViewMode = it }
                    )
                    DoorTreeTab.Requests -> MaintenanceView(
                        tenantDataStore = tenantDataStore,
                        onNewRequest = { showingMaintenanceRequest = true },
                        onOpenInvoice = { invoice -> selectedInvoice = invoice },
                        onOpenRequest = { request -> selectedRequest = request }
                    )
                    DoorTreeTab.Chat -> ChatView(tenantDataStore = tenantDataStore)
                    DoorTreeTab.Profile -> ProfileView(
                        authSession = authSession,
                        tenantDataStore = tenantDataStore,
                        onSignOut = authSession::signOut
                    )
                }
            }

            NavigationBar(containerColor = DoorTreeTheme.tabBarOverlay) {
                DoorTreeTab.entries.forEach { tab ->
                    val badgeCount = when (tab) {
                        DoorTreeTab.Payments -> duePaymentBadgeCount
                        DoorTreeTab.Requests -> pendingInvoiceBadgeCount
                        DoorTreeTab.Chat -> chatBadgeCount
                        else -> 0
                    }

                    NavigationBarItem(
                        selected = selectedTab == tab,
                        onClick = { selectedTab = tab },
                        icon = {
                            if (badgeCount > 0) {
                                BadgedBox(
                                    badge = {
                                        Badge {
                                            Text(text = badgeCount.coerceAtMost(99).toString())
                                        }
                                    }
                                ) {
                                    Icon(
                                        imageVector = systemIcon(tab.icon),
                                        contentDescription = null,
                                        tint = if (selectedTab == tab) DoorTreeTheme.gradientStart else DoorTreeTheme.textSecondary
                                    )
                                }
                            } else {
                                Icon(
                                    imageVector = systemIcon(tab.icon),
                                    contentDescription = null,
                                    tint = if (selectedTab == tab) DoorTreeTheme.gradientStart else DoorTreeTheme.textSecondary
                                )
                            }
                        },
                        label = { Text(L(tab.titleKey)) }
                    )
                }
            }
        }

        if (showingLease) {
            LeaseView(
                tenantDataStore = tenantDataStore,
                onClose = { showingLease = false }
            )
        }

        selectedInvoice?.let { invoice ->
            InvoiceView(
                invoice = invoice,
                tenantDataStore = tenantDataStore,
                onClose = { selectedInvoice = null }
            )
        }

        selectedRequest?.let { request ->
            MaintenanceDetailView(
                request = request,
                onClose = { selectedRequest = null }
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
}
