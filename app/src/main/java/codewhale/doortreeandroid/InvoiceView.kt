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
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.foundation.rememberScrollState
import androidx.compose.material3.Icon
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import codewhale.doortreeandroid.ui.theme.DoorTreeTheme
import java.time.LocalDate

@Composable
fun InvoiceView(
    invoice: PendingInvoiceItem,
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
            verticalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            Row(verticalAlignment = Alignment.Top, horizontalArrangement = Arrangement.spacedBy(14.dp)) {
                HeaderIconButton(systemName = "chevron.left", onClick = onClose)
                Column(modifier = Modifier.weight(1f), verticalArrangement = Arrangement.spacedBy(4.dp)) {
                    Text(
                        text = L("maintenance.invoice.title"),
                        color = DoorTreeTheme.textPrimary,
                        fontWeight = FontWeight.Bold,
                        fontSize = 30.sp
                    )
                    Text(text = invoice.invoiceNumber, color = DoorTreeTheme.textSecondary)
                }
                InvoiceStatusPill(
                    label = invoiceStatusLabel(invoice),
                    foreground = invoiceStatusForeground(invoice),
                    background = invoiceStatusBackground(invoice)
                )
            }

            GradientButton(title = L("payments.pay_now"), onClick = {})
            InvoiceHeroCard(invoice = invoice)
            InvoiceLineItemsSection(invoice = invoice)
            InvoiceSummarySection(invoice = invoice)
            InvoiceRecipientSection(invoice = invoice)
            InvoiceDetailsSection(invoice = invoice)

            if (invoice.notes.isNotBlank() || invoice.terms.isNotBlank()) {
                InvoiceNotesSection(invoice = invoice)
            }
        }
    }
}

@Composable
private fun InvoiceHeroCard(invoice: PendingInvoiceItem) {
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .glassCard(cornerRadius = 24.dp)
            .padding(18.dp),
        verticalArrangement = Arrangement.spacedBy(16.dp)
    ) {
        Column(verticalArrangement = Arrangement.spacedBy(6.dp)) {
            Text(
                text = invoice.propertyName.ifBlank { invoice.recipientName.ifBlank { invoice.invoiceNumber } },
                color = DoorTreeTheme.textPrimary
            )
            Text(
                text = invoice.recipientAddress.ifBlank { invoice.recipientEmail },
                color = DoorTreeTheme.textSecondary
            )
        }

        Row(horizontalArrangement = Arrangement.spacedBy(12.dp)) {
            InvoiceMetricCard(
                modifier = Modifier.weight(1f),
                label = L("maintenance.invoice.amount_due"),
                value = invoice.balance,
                accent = true
            )
            InvoiceMetricCard(
                modifier = Modifier.weight(1f),
                label = L("maintenance.invoice.total"),
                value = invoice.total
            )
        }

        Row(horizontalArrangement = Arrangement.spacedBy(12.dp)) {
            InvoiceMetricCard(
                modifier = Modifier.weight(1f),
                label = L("maintenance.invoice.issue_date"),
                value = invoice.issueDate
            )
            InvoiceMetricCard(
                modifier = Modifier.weight(1f),
                label = L("maintenance.invoice.due_date"),
                value = invoice.dueDate
            )
        }
    }
}

@Composable
private fun InvoiceMetricCard(
    modifier: Modifier = Modifier,
    label: String,
    value: String,
    accent: Boolean = false
) {
    Column(
        modifier = modifier
            .liquidGlassSurface(
                cornerRadius = 18.dp,
                tint = if (accent) DoorTreeTheme.destructive.copy(alpha = 0.14f) else DoorTreeTheme.barGlassTint
            )
            .padding(14.dp),
        verticalArrangement = Arrangement.spacedBy(6.dp)
    ) {
        Text(text = label, color = DoorTreeTheme.textSecondary)
        Text(
            text = value,
            color = if (accent) DoorTreeTheme.destructive else DoorTreeTheme.textPrimary,
            fontWeight = FontWeight.SemiBold
        )
    }
}

@Composable
private fun InvoiceLineItemsSection(invoice: PendingInvoiceItem) {
    Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
        Text(text = L("maintenance.invoice.line_items"), color = DoorTreeTheme.textPrimary)

        if (invoice.lineItems.isEmpty()) {
            SectionPlaceholder(
                systemName = "doc.text",
                title = L("maintenance.invoice.line_items"),
                message = L("maintenance.invoice.no_line_items")
            )
        } else {
            invoice.lineItems.forEach { item ->
                InvoiceLineItemRow(item = item)
            }
        }
    }
}

@Composable
private fun InvoiceLineItemRow(item: InvoiceLineItem) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .glassCard(cornerRadius = 18.dp)
            .padding(14.dp),
        horizontalArrangement = Arrangement.spacedBy(12.dp),
        verticalAlignment = Alignment.Top
    ) {
        Icon(systemIcon("doc.fill"), contentDescription = null, tint = DoorTreeTheme.leaseAccent)

        Column(modifier = Modifier.weight(1f), verticalArrangement = Arrangement.spacedBy(8.dp)) {
            Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                Text(text = item.description, color = DoorTreeTheme.textPrimary, modifier = Modifier.weight(1f))
                if (item.taxable) {
                    Text(
                        text = L("maintenance.invoice.taxable"),
                        color = DoorTreeTheme.textSecondary,
                        modifier = Modifier
                            .background(DoorTreeTheme.backgroundSecondary.copy(alpha = 0.65f), RoundedCornerShape(999.dp))
                            .padding(horizontal = 10.dp, vertical = 6.dp)
                    )
                }
            }

            if (item.category.isNotBlank()) {
                Text(text = item.category, color = DoorTreeTheme.textSecondary)
            }

            Row(verticalAlignment = Alignment.CenterVertically) {
                Text(
                    text = "${L("maintenance.invoice.qty")} ${item.quantity}  |  ${L("maintenance.invoice.unit_price")} ${item.unitPrice}",
                    color = DoorTreeTheme.textSecondary,
                    modifier = Modifier.weight(1f)
                )
                Text(text = item.amount, color = DoorTreeTheme.textPrimary, fontWeight = FontWeight.SemiBold)
            }
        }
    }
}

@Composable
private fun InvoiceSummarySection(invoice: PendingInvoiceItem) {
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .glassCard(cornerRadius = 22.dp)
            .padding(16.dp),
        verticalArrangement = Arrangement.spacedBy(12.dp)
    ) {
        Text(text = L("maintenance.invoice.summary"), color = DoorTreeTheme.textPrimary)
        InvoiceDetailRow(label = L("maintenance.invoice.subtotal"), value = invoice.subtotal)
        InvoiceDetailRow(label = L("maintenance.invoice.tps"), value = invoice.tpsAmount)
        InvoiceDetailRow(label = L("maintenance.invoice.tvq"), value = invoice.tvqAmount)
        InvoiceDetailRow(label = L("maintenance.invoice.total"), value = invoice.total)
        InvoiceDetailRow(
            label = L("maintenance.invoice.balance"),
            value = invoice.balance,
            valueColor = DoorTreeTheme.destructive,
            emphasize = true
        )
    }
}

@Composable
private fun InvoiceRecipientSection(invoice: PendingInvoiceItem) {
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .glassCard(cornerRadius = 22.dp)
            .padding(16.dp),
        verticalArrangement = Arrangement.spacedBy(12.dp)
    ) {
        Text(text = L("maintenance.invoice.bill_to"), color = DoorTreeTheme.textPrimary)
        Text(text = invoice.recipientName, color = DoorTreeTheme.textPrimary)

        if (invoice.recipientAddress.isNotBlank()) {
            Text(text = invoice.recipientAddress, color = DoorTreeTheme.textSecondary)
        }
        if (invoice.recipientEmail.isNotBlank()) {
            InvoiceDetailRow(label = L("maintenance.invoice.email"), value = invoice.recipientEmail)
        }
        if (invoice.recipientNumber.isNotBlank()) {
            InvoiceDetailRow(label = L("maintenance.invoice.phone"), value = invoice.recipientNumber)
        }
    }
}

@Composable
private fun InvoiceDetailsSection(invoice: PendingInvoiceItem) {
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .glassCard(cornerRadius = 22.dp)
            .padding(16.dp),
        verticalArrangement = Arrangement.spacedBy(12.dp)
    ) {
        Text(text = L("maintenance.invoice.details"), color = DoorTreeTheme.textPrimary)
        InvoiceDetailRow(label = L("maintenance.invoice.status"), value = invoiceStatusLabel(invoice))
        if (invoice.createdAt != "-") {
            InvoiceDetailRow(label = L("maintenance.invoice.created_at"), value = invoice.createdAt)
        }
        if (invoice.updatedAt != "-") {
            InvoiceDetailRow(label = L("maintenance.invoice.updated_at"), value = invoice.updatedAt)
        }
    }
}

@Composable
private fun InvoiceNotesSection(invoice: PendingInvoiceItem) {
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .glassCard(cornerRadius = 22.dp)
            .padding(16.dp),
        verticalArrangement = Arrangement.spacedBy(14.dp)
    ) {
        if (invoice.notes.isNotBlank()) {
            Column(verticalArrangement = Arrangement.spacedBy(6.dp)) {
                Text(text = L("maintenance.invoice.notes"), color = DoorTreeTheme.textPrimary)
                Text(text = invoice.notes, color = DoorTreeTheme.textSecondary)
            }
        }

        if (invoice.terms.isNotBlank()) {
            Column(verticalArrangement = Arrangement.spacedBy(6.dp)) {
                Text(text = L("maintenance.invoice.terms"), color = DoorTreeTheme.textPrimary)
                Text(text = invoice.terms, color = DoorTreeTheme.textSecondary)
            }
        }
    }
}

@Composable
private fun InvoiceDetailRow(
    label: String,
    value: String,
    valueColor: androidx.compose.ui.graphics.Color = DoorTreeTheme.textPrimary,
    emphasize: Boolean = false
) {
    Row(verticalAlignment = Alignment.CenterVertically) {
        Text(text = label, color = DoorTreeTheme.textSecondary)
        Spacer(modifier = Modifier.weight(1f))
        Text(
            text = value,
            color = valueColor,
            fontWeight = if (emphasize) FontWeight.SemiBold else FontWeight.Normal
        )
    }
}

@Composable
private fun InvoiceStatusPill(
    label: String,
    foreground: Color,
    background: Color
) {
    Text(
        text = label,
        color = foreground,
        modifier = Modifier
            .background(background, RoundedCornerShape(999.dp))
            .padding(horizontal = 12.dp, vertical = 8.dp)
    )
}

internal fun invoiceIsOverdue(invoice: PendingInvoiceItem): Boolean {
    val dueDate = invoice.sortDate ?: return false
    return dueDate.isBefore(LocalDate.now())
}

internal fun invoiceStatusLabel(invoice: PendingInvoiceItem): String {
    return if (invoiceIsOverdue(invoice)) L("status.overdue") else invoice.statusLabel
}

internal fun invoiceStatusForeground(invoice: PendingInvoiceItem): Color {
    return if (invoiceIsOverdue(invoice)) DoorTreeTheme.destructive else DoorTreeTheme.dueText
}

internal fun invoiceStatusBackground(invoice: PendingInvoiceItem): Color {
    return if (invoiceIsOverdue(invoice)) {
        DoorTreeTheme.destructive.copy(alpha = 0.14f)
    } else {
        DoorTreeTheme.dueBackground.copy(alpha = 0.76f)
    }
}
