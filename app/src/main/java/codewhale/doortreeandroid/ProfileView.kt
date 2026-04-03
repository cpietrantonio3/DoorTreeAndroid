@file:OptIn(ExperimentalMaterial3Api::class)

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
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.Switch
import androidx.compose.material3.SwitchDefaults
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import codewhale.doortreeandroid.ui.theme.DoorTreeTheme

        @Composable
        fun ProfileView(
            authSession: AuthSessionStore,
            tenantDataStore: TenantDataStore,
            onSignOut: () -> Unit
        ) {
            var faceIdEnabled by remember { mutableStateOf(DoorTreeSampleData.notificationPreferences.faceID) }
            var showingChangePassword by remember { mutableStateOf(false) }

            Column(
                modifier = Modifier
                    .fillMaxSize()
                    .background(DoorTreeTheme.backgroundPrimary)
                    .verticalScroll(rememberScrollState())
                    .topSafeAreaPadding()
                    .padding(horizontal = DoorTreeTheme.screenHorizontalPadding, vertical = 18.dp),
                verticalArrangement = Arrangement.spacedBy(20.dp)
            ) {
                Text(
                    text = L("profile.title"),
                    color = DoorTreeTheme.textPrimary,
                    fontWeight = FontWeight.Bold,
                    fontSize = 28.sp
                )

                Column(horizontalAlignment = Alignment.CenterHorizontally, verticalArrangement = Arrangement.spacedBy(14.dp), modifier = Modifier.fillMaxWidth()) {
                    Box {
                        Box(
                            modifier = Modifier
                                .size(72.dp)
                                .background(DoorTreeTheme.backgroundSecondary, CircleShape),
                            contentAlignment = Alignment.Center
                        ) {
                            Text(text = tenantDataStore.tenantProfile.initials, color = DoorTreeTheme.accentForeground)
                        }
                        Box(
                            modifier = Modifier
                                .align(Alignment.BottomEnd)
                                .background(DoorTreeTheme.primaryGradient, CircleShape)
                                .padding(6.dp)
                        ) {
                            Icon(systemIcon("pencil"), contentDescription = null, tint = DoorTreeTheme.accentForeground)
                        }
                    }

                    Column(horizontalAlignment = Alignment.CenterHorizontally, verticalArrangement = Arrangement.spacedBy(6.dp)) {
                        Text(text = tenantDataStore.tenantProfile.name, color = DoorTreeTheme.textPrimary)
                        Text(text = tenantDataStore.tenantProfile.email, color = DoorTreeTheme.textSecondary)
                    }
                }

                Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
                    Text(text = L("profile.my_property"), color = DoorTreeTheme.textPrimary)
                    Column(
                        modifier = Modifier
                            .fillMaxWidth()
                            .glassCard(cornerRadius = 20.dp)
                            .padding(horizontal = 16.dp, vertical = 6.dp),
                        verticalArrangement = Arrangement.spacedBy(12.dp)
                    ) {
                        ProfileInfoRow(label = L("profile.address"), value = profileAddress(tenantDataStore.propertyInfo))
                        ProfileInfoRow(label = L("profile.unit"), value = tenantDataStore.propertyInfo.subtitle)
                    }
                }

                Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
                    Text(text = L("profile.notifications"), color = DoorTreeTheme.textSecondary)
                    Column(
                        modifier = Modifier
                            .fillMaxWidth()
                            .glassCard(cornerRadius = 20.dp)
                            .padding(horizontal = 16.dp, vertical = 6.dp),
                        verticalArrangement = Arrangement.spacedBy(0.dp)
                    ) {
                        ProfileToggleRow(
                            title = L("profile.payment_reminders"),
                            icon = "bell",
                            isOn = tenantDataStore.notificationPreferences.paymentReminders,
                            onToggle = { tenantDataStore.updateNotificationSetting(NotificationSettingKey.PaymentReminders, it) }
                        )
                        ProfileToggleRow(
                            title = L("profile.maintenance_updates"),
                            icon = "bell",
                            isOn = tenantDataStore.notificationPreferences.maintenanceUpdates,
                            onToggle = { tenantDataStore.updateNotificationSetting(NotificationSettingKey.MaintenanceUpdates, it) }
                        )
                        ProfileToggleRow(
                            title = L("profile.messages"),
                            icon = "bell",
                            isOn = tenantDataStore.notificationPreferences.messages,
                            onToggle = { tenantDataStore.updateNotificationSetting(NotificationSettingKey.Messages, it) }
                        )
                    }
                }

                Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
                    Text(text = L("profile.security"), color = DoorTreeTheme.textSecondary)
                    Column(
                        modifier = Modifier
                            .fillMaxWidth()
                            .glassCard(cornerRadius = 20.dp)
                            .padding(horizontal = 16.dp, vertical = 6.dp),
                        verticalArrangement = Arrangement.spacedBy(0.dp)
                    ) {
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .clickable { showingChangePassword = true }
                                .padding(vertical = 16.dp),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            ProfileRowLabel(icon = "lock.shield", title = L("profile.change_password"))
                            Spacer(modifier = Modifier.weight(1f))
                            Icon(systemIcon("chevron.right"), contentDescription = null, tint = DoorTreeTheme.textSecondary)
                        }
                        ProfileToggleRow(
                            title = L("profile.face_id"),
                            icon = "faceid",
                            isOn = faceIdEnabled,
                            onToggle = { faceIdEnabled = it }
                        )
                    }
                }

                Text(
                    text = L("profile.sign_out"),
                    color = DoorTreeTheme.destructive,
                    modifier = Modifier
                        .fillMaxWidth()
                        .clickable(onClick = onSignOut)
                        .padding(vertical = 18.dp),
                    textAlign = androidx.compose.ui.text.style.TextAlign.Center
                )
            }

            if (showingChangePassword) {
                FullHeightModalBottomSheet(onDismissRequest = { showingChangePassword = false }) {
                    ChangePasswordSheetView(
                        authSession = authSession,
                        prefilledEmail = tenantDataStore.tenantProfile.email,
                        onDismiss = { showingChangePassword = false }
                    )
                }
            }
        }

        @Composable
        private fun ProfileInfoRow(label: String, value: String) {
            Column(verticalArrangement = Arrangement.spacedBy(6.dp), modifier = Modifier.fillMaxWidth()) {
                Text(text = label, color = DoorTreeTheme.textSecondary)
                Text(text = value, color = DoorTreeTheme.textPrimary)
            }
        }

        @Composable
        private fun ProfileRowLabel(icon: String, title: String) {
            Row(horizontalArrangement = Arrangement.spacedBy(12.dp), verticalAlignment = Alignment.CenterVertically) {
                Icon(systemIcon(icon), contentDescription = null, tint = DoorTreeTheme.textSecondary)
                Text(text = title, color = DoorTreeTheme.textPrimary)
            }
        }

        @Composable
        private fun ProfileToggleRow(
            title: String,
            icon: String,
            isOn: Boolean,
            onToggle: (Boolean) -> Unit
        ) {
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(vertical = 14.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                ProfileRowLabel(icon = icon, title = title)
                Spacer(modifier = Modifier.weight(1f))
                Switch(
                    checked = isOn,
                    onCheckedChange = onToggle,
                    colors = SwitchDefaults.colors(
                        checkedThumbColor = DoorTreeTheme.gradientStart,
                        checkedTrackColor = DoorTreeTheme.paidBackground,
                        uncheckedThumbColor = DoorTreeTheme.textSecondary,
                        uncheckedTrackColor = DoorTreeTheme.pendingBackground
                    )
                )
            }
        }

        private fun profileAddress(propertyInfo: PropertyInfo): String {
            return when {
                propertyInfo.address.isBlank() -> propertyInfo.city
                propertyInfo.city.isBlank() -> propertyInfo.address
                else -> "${propertyInfo.address}\n${propertyInfo.city}"
            }
        }
