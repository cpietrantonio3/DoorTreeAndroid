package codewhale.doortreeandroid

import android.widget.Toast
import androidx.compose.foundation.clickable
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalClipboardManager
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.AnnotatedString
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.window.Dialog
import androidx.compose.ui.window.DialogProperties
import codewhale.doortreeandroid.ui.theme.DoorTreeTheme

@Composable
fun InteracTransferSheetView(
    details: InteracTransferDetails,
    onDismiss: () -> Unit
) {
    val clipboard = LocalClipboardManager.current
    val context = LocalContext.current

    Dialog(
        onDismissRequest = onDismiss,
        properties = DialogProperties(usePlatformDefaultWidth = false)
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
                    .padding(bottom = 24.dp)
            ) {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .topSafeAreaPadding()
                        .padding(horizontal = DoorTreeTheme.screenHorizontalPadding, vertical = 16.dp),
                    horizontalArrangement = Arrangement.spacedBy(14.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    HeaderIconButton(systemName = "xmark", onClick = onDismiss)
                    Text(
                        text = "One-Time Bank Transfer",
                        color = DoorTreeTheme.textPrimary,
                        fontSize = 28.sp,
                        fontWeight = FontWeight.Bold
                    )
                }

                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = DoorTreeTheme.screenHorizontalPadding),
                    verticalArrangement = Arrangement.spacedBy(18.dp)
                ) {
                    Text(
                        text = "Use the landlord's transfer details below for a one-off wire transfer or e-Transfer.",
                        color = DoorTreeTheme.textSecondary
                    )

                    TransferDetailCard(
                        title = "Recipient Email",
                        value = details.recipientEmail,
                        actionTitle = "Copy Email"
                    ) {
                        clipboard.setText(AnnotatedString(details.recipientEmail))
                        Toast.makeText(context, "Recipient email copied", Toast.LENGTH_SHORT).show()
                    }

                    TransferDetailCard(
                        title = "Recipient Name",
                        value = details.recipientName
                    )

                    TransferDetailCard(
                        title = "Amount",
                        value = details.amount,
                        actionTitle = "Copy Amount"
                    ) {
                        clipboard.setText(AnnotatedString(details.amount))
                        Toast.makeText(context, "Amount copied", Toast.LENGTH_SHORT).show()
                    }

                    TransferDetailCard(
                        title = "Due Date",
                        value = details.dueDate
                    )

                    TransferDetailCard(
                        title = "Reference",
                        value = details.reference,
                        actionTitle = "Copy Reference"
                    ) {
                        clipboard.setText(AnnotatedString(details.reference))
                        Toast.makeText(context, "Reference copied", Toast.LENGTH_SHORT).show()
                    }

                    Column(
                        modifier = Modifier
                            .fillMaxWidth()
                            .liquidGlassSurface(
                                cornerRadius = 18.dp,
                                tint = if (details.autodepositEnabled) {
                                    DoorTreeTheme.paidBackground.copy(alpha = 0.28f)
                                } else {
                                    DoorTreeTheme.dueBackground.copy(alpha = 0.32f)
                                }
                            )
                            .padding(16.dp),
                        verticalArrangement = Arrangement.spacedBy(10.dp)
                    ) {
                        Text(
                            text = if (details.autodepositEnabled) "Autodeposit enabled" else "Autodeposit not enabled",
                            color = DoorTreeTheme.textPrimary,
                            fontWeight = FontWeight.SemiBold
                        )
                        Text(
                            text = if (details.autodepositEnabled) {
                                "Your transfer should deposit automatically when sent to this recipient."
                            } else {
                                "The landlord has not marked Autodeposit as enabled yet, so the transfer may require a security question."
                            },
                            color = DoorTreeTheme.textSecondary
                        )
                    }
                }
            }
        }
    }
}

@Composable
private fun TransferDetailCard(
    title: String,
    value: String,
    actionTitle: String? = null,
    onAction: (() -> Unit)? = null
) {
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .liquidGlassSurface(cornerRadius = 18.dp, interactive = onAction != null)
            .padding(16.dp),
        verticalArrangement = Arrangement.spacedBy(12.dp)
    ) {
        Text(
            text = title,
            color = DoorTreeTheme.textSecondary,
            fontSize = 12.sp,
            fontWeight = FontWeight.SemiBold
        )
        Text(
            text = value,
            color = DoorTreeTheme.textPrimary,
            fontWeight = FontWeight.SemiBold
        )
        if (actionTitle != null && onAction != null) {
            Text(
                text = actionTitle,
                color = DoorTreeTheme.gradientStart,
                fontWeight = FontWeight.SemiBold,
                modifier = Modifier
                    .padding(top = 2.dp)
                    .clickable(onClick = onAction)
            )
        }
    }
}
