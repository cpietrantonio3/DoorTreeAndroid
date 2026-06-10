package codewhale.doortreeandroid

import android.content.Context
import android.content.Intent
import android.graphics.Bitmap
import android.graphics.Canvas
import android.graphics.Color
import android.graphics.Paint
import android.graphics.RectF
import android.graphics.pdf.PdfDocument
import android.graphics.pdf.PdfRenderer
import android.os.ParcelFileDescriptor
import android.widget.Toast
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ColumnScope
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.Button
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
import androidx.compose.ui.graphics.asImageBitmap
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.window.Dialog
import androidx.compose.ui.window.DialogProperties
import androidx.core.content.FileProvider
import codewhale.doortreeandroid.ui.theme.DoorTreeTheme
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import java.io.File
import java.io.FileOutputStream
import java.time.LocalDate
import java.time.format.DateTimeFormatter
import java.util.Locale

@Composable
fun PaymentHistoryDetailView(
    item: DashboardPaymentHistoryItem,
    onDismiss: () -> Unit
) {
    val context = LocalContext.current
    val scope = rememberCoroutineScope()
    val invoice = (item.source as? DashboardPaymentHistorySource.Invoice)?.invoice
    var previewFile by remember(item.id) { mutableStateOf<File?>(null) }
    var isPreparingPdf by remember(item.id) { mutableStateOf(false) }

    fun openPreview() {
        if (isPreparingPdf) return
        scope.launch {
            isPreparingPdf = true
            try {
                previewFile = withContext(Dispatchers.IO) {
                    DoorTreeReceiptPdfRenderer.render(context, item, invoice)
                }
            } catch (error: Exception) {
                Toast.makeText(
                    context,
                    error.localizedMessage ?: "Unable to open receipt.",
                    Toast.LENGTH_SHORT
                ).show()
            } finally {
                isPreparingPdf = false
            }
        }
    }

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
                    .topSafeAreaPadding()
                    .bottomSafeAreaPadding()
                    .padding(horizontal = DoorTreeTheme.screenHorizontalPadding, vertical = 18.dp),
                verticalArrangement = Arrangement.spacedBy(16.dp)
            ) {
                Row(verticalAlignment = Alignment.Top, horizontalArrangement = Arrangement.spacedBy(14.dp)) {
                    HeaderIconButton(systemName = "chevron.left", onClick = onDismiss)

                    Column(
                        modifier = Modifier.weight(1f),
                        verticalArrangement = Arrangement.spacedBy(4.dp)
                    ) {
                        Text(
                            text = "Payment Receipt",
                            color = DoorTreeTheme.textPrimary,
                            fontWeight = FontWeight.Bold
                        )
                        Text(text = item.title, color = DoorTreeTheme.textSecondary)
                    }

                    StatusBadge(status = item.status)
                }

                Button(
                    onClick = ::openPreview,
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(42.dp)
                ) {
                    Icon(systemIcon("doc.text"), contentDescription = null, modifier = Modifier.size(18.dp))
                    Spacer(modifier = Modifier.size(8.dp))
                    Text(text = if (isPreparingPdf) "Preparing..." else "View PDF")
                }

                PropertyInfoCard(item = item, invoice = invoice)

                if (invoice != null) {
                    ReceiptSection(title = L("maintenance.invoice.bill_to")) {
                        ReceiptDetailRow(label = "Name", value = invoice.recipientName)
                        if (invoice.recipientEmail.isNotBlank()) ReceiptDetailRow(label = L("maintenance.invoice.email"), value = invoice.recipientEmail)
                        if (invoice.recipientAddress.isNotBlank()) ReceiptDetailRow(label = "Address", value = invoice.recipientAddress)
                    }

                    ReceiptSection(title = L("maintenance.invoice.details")) {
                        ReceiptDetailRow(label = L("maintenance.invoice.status"), value = invoice.statusLabel)
                        ReceiptDetailRow(label = L("maintenance.invoice.issue_date"), value = invoice.issueDate)
                        ReceiptDetailRow(label = L("maintenance.invoice.due_date"), value = invoice.dueDate)
                        if (invoice.updatedAt.isNotBlank() && invoice.updatedAt != "-") {
                            ReceiptDetailRow(label = L("maintenance.invoice.updated_at"), value = invoice.updatedAt)
                        }
                    }

                    LineItemsSection(invoice = invoice)

                    ReceiptSection(title = L("maintenance.invoice.summary")) {
                        ReceiptDetailRow(label = L("maintenance.invoice.subtotal"), value = invoice.subtotal)
                        ReceiptDetailRow(label = L("maintenance.invoice.tps"), value = invoice.tpsAmount)
                        ReceiptDetailRow(label = L("maintenance.invoice.tvq"), value = invoice.tvqAmount)
                        ReceiptDetailRow(label = L("maintenance.invoice.total"), value = invoice.total, emphasized = true)
                        ReceiptDetailRow(label = L("maintenance.invoice.balance"), value = invoice.balance)
                    }
                }

                Spacer(modifier = Modifier.height(24.dp))
            }
        }
    }

    previewFile?.let { file ->
        ReceiptPdfPreviewView(
            file = file,
            onDismiss = { previewFile = null }
        )
    }
}

@Composable
private fun PropertyInfoCard(item: DashboardPaymentHistoryItem, invoice: PendingInvoiceItem?) {
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .glassCard(cornerRadius = 24.dp)
            .padding(18.dp),
        verticalArrangement = Arrangement.spacedBy(14.dp)
    ) {
        Text(
            text = invoice?.propertyName?.takeIf { it.isNotBlank() } ?: item.title,
            color = DoorTreeTheme.textPrimary,
            fontWeight = FontWeight.Bold
        )

        Row(horizontalArrangement = Arrangement.spacedBy(12.dp)) {
            ReceiptMetric(label = "Paid", value = item.amount, modifier = Modifier.weight(1f))
            ReceiptMetric(label = "Date", value = item.date, modifier = Modifier.weight(1f))
        }
    }
}

@Composable
private fun ReceiptMetric(label: String, value: String, modifier: Modifier = Modifier) {
    Column(
        modifier = modifier
            .liquidGlassSurface(cornerRadius = 18.dp, tint = DoorTreeTheme.barGlassTint)
            .padding(14.dp),
        verticalArrangement = Arrangement.spacedBy(6.dp)
    ) {
        Text(text = label, color = DoorTreeTheme.textSecondary)
        Text(text = value, color = DoorTreeTheme.textPrimary, fontWeight = FontWeight.SemiBold)
    }
}

@Composable
private fun ReceiptSection(title: String, content: @Composable ColumnScope.() -> Unit) {
    Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
        Text(text = title, color = DoorTreeTheme.textPrimary, fontWeight = FontWeight.Bold)
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .glassCard(cornerRadius = 22.dp)
                .padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp),
            content = content
        )
    }
}

@Composable
private fun ReceiptDetailRow(label: String, value: String, emphasized: Boolean = false) {
    Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.spacedBy(12.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Text(text = label, color = DoorTreeTheme.textSecondary)
        Spacer(modifier = Modifier.weight(1f))
        Text(
            text = value.ifBlank { "-" },
            color = DoorTreeTheme.textPrimary,
            fontWeight = if (emphasized) FontWeight.SemiBold else FontWeight.Normal,
            textAlign = TextAlign.End
        )
    }
}

@Composable
private fun LineItemsSection(invoice: PendingInvoiceItem) {
    Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
        Text(text = L("maintenance.invoice.line_items"), color = DoorTreeTheme.textPrimary, fontWeight = FontWeight.Bold)
        if (invoice.lineItems.isEmpty()) {
            SectionPlaceholder(
                systemName = "doc.text",
                title = L("maintenance.invoice.line_items"),
                message = L("maintenance.invoice.no_line_items")
            )
        } else {
            Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
                invoice.lineItems.forEach { lineItem ->
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .glassCard(cornerRadius = 18.dp)
                            .padding(14.dp),
                        horizontalArrangement = Arrangement.spacedBy(12.dp)
                    ) {
                        Icon(systemIcon("doc.fill"), contentDescription = null, tint = DoorTreeTheme.leaseAccent)
                        Column(modifier = Modifier.weight(1f), verticalArrangement = Arrangement.spacedBy(8.dp)) {
                            Text(text = lineItem.description, color = DoorTreeTheme.textPrimary)
                            Text(
                                text = "${L("maintenance.invoice.qty")} ${lineItem.quantity}  |  ${L("maintenance.invoice.unit_price")} ${lineItem.unitPrice}",
                                color = DoorTreeTheme.textSecondary
                            )
                        }
                        Text(text = lineItem.amount, color = DoorTreeTheme.textPrimary, fontWeight = FontWeight.SemiBold)
                    }
                }
            }
        }
    }
}

@Composable
private fun ReceiptPdfPreviewView(file: File, onDismiss: () -> Unit) {
    val context = LocalContext.current
    var pages by remember(file.absolutePath) { mutableStateOf<List<Bitmap>>(emptyList()) }
    var renderError by remember(file.absolutePath) { mutableStateOf<String?>(null) }

    LaunchedEffect(file.absolutePath) {
        try {
            pages = withContext(Dispatchers.IO) { renderPdfPages(file) }
        } catch (error: Exception) {
            renderError = error.localizedMessage ?: "Unable to preview receipt."
        }
    }

    Dialog(
        onDismissRequest = onDismiss,
        properties = DialogProperties(usePlatformDefaultWidth = false)
    ) {
        Column(
            modifier = Modifier
                .fillMaxSize()
                .background(DoorTreeTheme.backgroundPrimary)
                .topSafeAreaPadding()
                .bottomSafeAreaPadding()
        ) {
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = DoorTreeTheme.screenHorizontalPadding, vertical = 12.dp),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                HeaderIconButton(systemName = "xmark", onClick = onDismiss)
                Text(
                    text = "Payment Receipt",
                    color = DoorTreeTheme.textPrimary,
                    fontWeight = FontWeight.SemiBold,
                    modifier = Modifier.weight(1f)
                )
                Button(onClick = { shareReceiptPdf(context, file) }) {
                    Icon(systemIcon("square.and.arrow.up"), contentDescription = null, modifier = Modifier.size(18.dp))
                    Spacer(modifier = Modifier.size(8.dp))
                    Text(text = L("common.export"))
                }
            }

            Column(
                modifier = Modifier
                    .fillMaxSize()
                    .verticalScroll(rememberScrollState())
                    .padding(horizontal = DoorTreeTheme.screenHorizontalPadding, vertical = 12.dp),
                verticalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                renderError?.let {
                    Text(text = it, color = DoorTreeTheme.destructive)
                }

                pages.forEach { page ->
                    Image(
                        bitmap = page.asImageBitmap(),
                        contentDescription = null,
                        modifier = Modifier
                            .fillMaxWidth()
                            .glassCard(cornerRadius = 12.dp)
                            .padding(2.dp)
                    )
                }

                Spacer(modifier = Modifier.height(24.dp))
            }
        }
    }
}

private fun shareReceiptPdf(context: Context, file: File) {
    val uri = FileProvider.getUriForFile(context, "${BuildConfig.APPLICATION_ID}.fileprovider", file)
    val intent = Intent(Intent.ACTION_SEND).apply {
        type = "application/pdf"
        putExtra(Intent.EXTRA_STREAM, uri)
        addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION)
    }
    context.startActivity(Intent.createChooser(intent, "Share receipt"))
}

private fun renderPdfPages(file: File): List<Bitmap> {
    val descriptor = ParcelFileDescriptor.open(file, ParcelFileDescriptor.MODE_READ_ONLY)
    descriptor.use { pfd ->
        PdfRenderer(pfd).use { renderer ->
            return (0 until renderer.pageCount).map { index ->
                renderer.openPage(index).use { page ->
                    val scale = 2
                    val bitmap = Bitmap.createBitmap(page.width * scale, page.height * scale, Bitmap.Config.ARGB_8888)
                    bitmap.eraseColor(Color.WHITE)
                    page.render(bitmap, null, null, PdfRenderer.Page.RENDER_MODE_FOR_DISPLAY)
                    bitmap
                }
            }
        }
    }
}

private object DoorTreeReceiptPdfRenderer {
    private const val pageWidth = 612
    private const val pageHeight = 792
    private const val margin = 46f
    private const val slate900 = 0xFF0F172A.toInt()
    private const val slate700 = 0xFF334155.toInt()
    private const val slate600 = 0xFF475569.toInt()
    private const val slate500 = 0xFF64748B.toInt()
    private const val slate300 = 0xFFCBD5E1.toInt()
    private const val slate200 = 0xFFE2E8F0.toInt()
    private const val slate100 = 0xFFF1F5F9.toInt()
    private const val slate50 = 0xFFF8FAFC.toInt()
    private const val doorTreeGreen = 0xFF2ECC8A.toInt()

    fun render(context: Context, item: DashboardPaymentHistoryItem, invoice: PendingInvoiceItem?): File {
        val directory = File(context.cacheDir, "shared_pdfs").apply { mkdirs() }
        val file = File(directory, "DoorTree-Receipt-${sanitize(item.id)}.pdf")
        val document = PdfDocument()
        var page = document.startPage(PdfDocument.PageInfo.Builder(pageWidth, pageHeight, 1).create())
        var canvas = page.canvas

        drawHeader(canvas, item, invoice)
        val panelY = margin + 158f
        drawInfoPanels(canvas, item, invoice, panelY)

        var y = panelY + 112f + 28f
        drawTableHeader(canvas, y)
        y += 38f

        receiptRows(item, invoice).forEach { row ->
            val rowHeight = maxOf(28f, measureTextHeight(row.description, 240f, 10.5f) + 12f)
            if (y + rowHeight > pageHeight - 190f) {
                drawFooter(canvas)
                document.finishPage(page)
                page = document.startPage(PdfDocument.PageInfo.Builder(pageWidth, pageHeight, document.pages.size + 1).create())
                canvas = page.canvas
                y = margin
                drawTableHeader(canvas, y)
                y += 38f
            }
            drawLineItemRow(canvas, row, y, rowHeight)
            y += rowHeight
        }

        drawNotesAndSummary(canvas, document, page, item, invoice, y)
        document.writeTo(FileOutputStream(file))
        document.close()
        return file
    }

    private fun drawNotesAndSummary(
        canvas: Canvas,
        document: PdfDocument,
        page: PdfDocument.Page,
        item: DashboardPaymentHistoryItem,
        invoice: PendingInvoiceItem?,
        tableY: Float
    ) {
        val notes = invoice?.notes?.trim().orEmpty()
        val notesHeight = if (notes.isBlank()) 0f else measureTextHeight(notes, 228f, 10.5f) + 42f
        val summaryWidth = 210f
        val summaryRows = invoice?.let {
            1 + (if (isZeroAmount(it.tpsAmount)) 0 else 1) + (if (isZeroAmount(it.tvqAmount)) 0 else 1)
        } ?: 0
        val summaryHeight = if (invoice == null) 78f else 96f + summaryRows * 22f
        val summaryX = pageWidth - margin - summaryWidth
        var footerY = tableY + 24f
        var activeCanvas = canvas
        var activePage = page

        if (footerY + maxOf(summaryHeight, notesHeight) > pageHeight - 96f) {
            drawFooter(activeCanvas)
            document.finishPage(activePage)
            activePage = document.startPage(PdfDocument.PageInfo.Builder(pageWidth, pageHeight, document.pages.size + 1).create())
            activeCanvas = activePage.canvas
            footerY = margin
        }

        if (notes.isNotBlank()) {
            val rect = RectF(margin, footerY, margin + 260f, footerY + notesHeight)
            activeCanvas.drawRoundRect(rect, 16f, 16f, fill(slate50))
            drawText(activeCanvas, "Notes", rect.left + 16f, rect.top + 22f, 11f, slate900, bold = true)
            drawWrappedText(activeCanvas, notes, rect.left + 16f, rect.top + 42f, 228f, 13f, 10.5f, slate600)
        }

        activeCanvas.drawRoundRect(RectF(summaryX, footerY, summaryX + summaryWidth, footerY + summaryHeight), 18f, 18f, fill(slate50))
        if (invoice != null) {
            val rows = buildList {
                add("Subtotal" to invoice.subtotal)
                if (!isZeroAmount(invoice.tpsAmount)) add("TPS (5%)" to invoice.tpsAmount)
                if (!isZeroAmount(invoice.tvqAmount)) add("TVQ (9.975%)" to invoice.tvqAmount)
            }
            rows.forEachIndexed { index, row ->
                drawSummaryRow(activeCanvas, row.first, row.second, summaryX + 18f, footerY + 24f + index * 22f, summaryWidth - 36f)
            }
            val dividerY = footerY + 24f + rows.size * 22f + 2f
            drawLine(activeCanvas, summaryX + 18f, dividerY, summaryX + summaryWidth - 18f, dividerY, slate300)
            drawSummaryRow(activeCanvas, "Total", invoice.total, summaryX + 18f, dividerY + 22f, summaryWidth - 36f, true)
            drawSummaryRow(activeCanvas, "Balance", invoice.balance, summaryX + 18f, dividerY + 46f, summaryWidth - 36f)
        } else {
            drawSummaryRow(activeCanvas, "Paid", item.amount, summaryX + 18f, footerY + 28f, summaryWidth - 36f, true)
            drawSummaryRow(activeCanvas, "Status", item.status.localizedLabel, summaryX + 18f, footerY + 52f, summaryWidth - 36f)
        }

        drawFooter(activeCanvas)
        document.finishPage(activePage)
    }

    private fun drawHeader(canvas: Canvas, item: DashboardPaymentHistoryItem, invoice: PendingInvoiceItem?) {
        canvas.drawRoundRect(RectF(margin, margin, pageWidth - margin, margin + 138f), 20f, 20f, fill(slate50))
        drawText(canvas, "DoorTree", margin + 22f, margin + 46f, 28f, slate900, bold = true)
        drawText(canvas, "Property management receipt", margin + 22f, margin + 72f, 10.5f, slate600)
        drawWrappedText(canvas, invoice?.propertyName?.takeIf { it.isNotBlank() } ?: item.title, margin + 22f, margin + 91f, 250f, 13f, 10.5f, slate600)
        drawText(canvas, "Generated by DoorTree", margin + 22f, margin + 126f, 10.5f, slate600)

        val cardWidth = 162f
        val cardHeight = 74f
        val cardX = pageWidth - margin - cardWidth - 20f
        val cardY = margin + 18f
        canvas.drawRoundRect(RectF(cardX, cardY, cardX + cardWidth, cardY + cardHeight), 18f, 18f, fill(slate900))
        drawText(canvas, "Amount paid", cardX + 16f, cardY + 25f, 9.5f, slate300)
        drawText(canvas, item.amount, cardX + 16f, cardY + 51f, 18f, Color.WHITE, bold = true)
        canvas.drawRoundRect(RectF(cardX + 16f, cardY + 54f, cardX + 94f, cardY + 72f), 8f, 8f, fill(doorTreeGreen))
        drawCenteredText(canvas, item.status.localizedLabel, cardX + 55f, cardY + 67f, 8.5f, slate900, bold = true)
    }

    private fun drawInfoPanels(canvas: Canvas, item: DashboardPaymentHistoryItem, invoice: PendingInvoiceItem?, y: Float) {
        val gap = 16f
        val panelWidth = (pageWidth - margin * 2f - gap) / 2f
        val panelHeight = 112f
        canvas.drawRoundRect(RectF(margin, y, margin + panelWidth, y + panelHeight), 16f, 16f, fill(slate100))
        canvas.drawRoundRect(RectF(margin + panelWidth + gap, y, margin + panelWidth * 2f + gap, y + panelHeight), 16f, 16f, fill(slate100))

        drawText(canvas, "Bill To", margin + 18f, y + 30f, 12f, slate900, bold = true)
        val billTo = listOfNotNull(
            invoice?.recipientName?.takeIf { it.isNotBlank() },
            invoice?.recipientAddress?.takeIf { it.isNotBlank() },
            invoice?.recipientEmail?.takeIf { it.isNotBlank() },
            invoice?.recipientNumber?.takeIf { it.isNotBlank() }
        ).ifEmpty { listOf("Tenant") }
        drawWrappedLines(canvas, billTo, margin + 18f, y + 52f, panelWidth - 36f, 14f, 10.5f, slate700)

        val detailsX = margin + panelWidth + gap + 18f
        drawText(canvas, "Invoice Details", detailsX, y + 30f, 12f, slate900, bold = true)
        val detailRows = listOf(
            "Invoice #" to (invoice?.invoiceNumber?.takeIf { it.isNotBlank() } ?: item.id),
            "Type" to item.title,
            "Issue Date" to (invoice?.issueDate ?: item.date),
            "Due Date" to (invoice?.dueDate ?: item.date)
        )
        detailRows.forEachIndexed { index, row ->
            val rowY = y + 52f + index * 18f
            drawText(canvas, "${row.first}:", detailsX, rowY, 10.5f, slate700, bold = true)
            drawText(canvas, row.second.ifBlank { "-" }, detailsX + 88f, rowY, 10.5f, slate700)
        }
    }

    private fun drawTableHeader(canvas: Canvas, y: Float) {
        canvas.drawRoundRect(RectF(margin, y, pageWidth - margin, y + 28f), 12f, 12f, fill(slate50))
        drawText(canvas, "Products / Services", margin + 14f, y + 19f, 10.5f, slate900, bold = true)
        drawRightText(canvas, "Qty / Hrs", margin + 332f, y + 19f, 10.5f, slate900, bold = true)
        drawRightText(canvas, "Rate", margin + 424f, y + 19f, 10.5f, slate900, bold = true)
        drawRightText(canvas, "Amount", pageWidth - margin - 14f, y + 19f, 10.5f, slate900, bold = true)
    }

    private fun drawLineItemRow(canvas: Canvas, row: PdfLineItem, y: Float, height: Float) {
        drawLine(canvas, margin, y + height, pageWidth - margin, y + height, slate200)
        drawWrappedText(canvas, row.description, margin + 14f, y + 18f, 240f, 13f, 10.5f, slate700)
        drawRightText(canvas, row.quantity, margin + 332f, y + 18f, 10.5f, slate700)
        drawRightText(canvas, row.rate, margin + 424f, y + 18f, 10.5f, slate700)
        drawRightText(canvas, row.amount, pageWidth - margin - 14f, y + 18f, 10.5f, slate700)
    }

    private fun drawFooter(canvas: Canvas) {
        drawLine(canvas, margin, pageHeight - 64f, pageWidth - margin, pageHeight - 64f, slate200)
        drawText(canvas, "Generated with DoorTree", margin, pageHeight - 44f, 9.5f, slate500)
        drawRightText(canvas, "Receipt generated ${formattedToday()}", pageWidth - margin, pageHeight - 44f, 9.5f, slate500)
    }

    private fun receiptRows(item: DashboardPaymentHistoryItem, invoice: PendingInvoiceItem?): List<PdfLineItem> {
        if (invoice == null) {
            return listOf(PdfLineItem(item.title, "1", item.amount, item.amount))
        }
        if (invoice.lineItems.isEmpty()) {
            return listOf(PdfLineItem(item.title, "1", invoice.total, invoice.total))
        }
        return invoice.lineItems.map { lineItem ->
            PdfLineItem(
                description = if (lineItem.category.isBlank()) lineItem.description else "${lineItem.description} (${lineItem.category})",
                quantity = lineItem.quantity,
                rate = lineItem.unitPrice,
                amount = lineItem.amount
            )
        }
    }

    private fun drawSummaryRow(canvas: Canvas, label: String, value: String, x: Float, y: Float, width: Float, total: Boolean = false) {
        drawText(canvas, label, x, y, if (total) 12f else 10.5f, if (total) slate900 else slate600, bold = total)
        drawRightText(canvas, value.ifBlank { "-" }, x + width, y, if (total) 12f else 10.5f, if (total) slate900 else slate600, bold = total)
    }

    private fun fill(color: Int) = Paint(Paint.ANTI_ALIAS_FLAG).apply {
        style = Paint.Style.FILL
        this.color = color
    }

    private fun textPaint(size: Float, color: Int, bold: Boolean = false, align: Paint.Align = Paint.Align.LEFT) =
        Paint(Paint.ANTI_ALIAS_FLAG).apply {
            this.color = color
            textSize = size
            textAlign = align
            typeface = android.graphics.Typeface.create(android.graphics.Typeface.DEFAULT, if (bold) android.graphics.Typeface.BOLD else android.graphics.Typeface.NORMAL)
        }

    private fun drawText(canvas: Canvas, text: String, x: Float, baseline: Float, size: Float, color: Int, bold: Boolean = false) {
        canvas.drawText(text.ifBlank { "-" }, x, baseline, textPaint(size, color, bold))
    }

    private fun drawCenteredText(canvas: Canvas, text: String, centerX: Float, baseline: Float, size: Float, color: Int, bold: Boolean = false) {
        canvas.drawText(text.ifBlank { "-" }, centerX, baseline, textPaint(size, color, bold, Paint.Align.CENTER))
    }

    private fun drawRightText(canvas: Canvas, text: String, x: Float, baseline: Float, size: Float, color: Int, bold: Boolean = false) {
        canvas.drawText(text.ifBlank { "-" }, x, baseline, textPaint(size, color, bold, Paint.Align.RIGHT))
    }

    private fun drawWrappedLines(canvas: Canvas, lines: List<String>, x: Float, y: Float, width: Float, lineHeight: Float, size: Float, color: Int) {
        var cursorY = y
        lines.forEach { line ->
            drawWrappedText(canvas, line, x, cursorY, width, lineHeight, size, color)
            cursorY += maxOf(lineHeight, measureTextHeight(line, width, size))
        }
    }

    private fun drawWrappedText(canvas: Canvas, text: String, x: Float, baseline: Float, width: Float, lineHeight: Float, size: Float, color: Int) {
        val paint = textPaint(size, color)
        var cursorY = baseline
        wrapText(text.ifBlank { "-" }, paint, width).forEach { line ->
            canvas.drawText(line, x, cursorY, paint)
            cursorY += lineHeight
        }
    }

    private fun wrapText(text: String, paint: Paint, width: Float): List<String> {
        val words = text.split(Regex("\\s+")).filter { it.isNotBlank() }
        if (words.isEmpty()) return listOf("-")
        val lines = mutableListOf<String>()
        var current = ""
        words.forEach { word ->
            val candidate = if (current.isBlank()) word else "$current $word"
            if (paint.measureText(candidate) <= width || current.isBlank()) {
                current = candidate
            } else {
                lines += current
                current = word
            }
        }
        if (current.isNotBlank()) lines += current
        return lines
    }

    private fun measureTextHeight(text: String, width: Float, size: Float): Float {
        val paint = textPaint(size, slate700)
        return wrapText(text.ifBlank { "-" }, paint, width).size * 13f
    }

    private fun drawLine(canvas: Canvas, startX: Float, startY: Float, endX: Float, endY: Float, color: Int) {
        canvas.drawLine(startX, startY, endX, endY, Paint(Paint.ANTI_ALIAS_FLAG).apply {
            this.color = color
            strokeWidth = 1f
        })
    }

    private fun isZeroAmount(value: String): Boolean {
        val filtered = value.filter { it.isDigit() || it == '.' || it == '-' }
        return (filtered.toDoubleOrNull() ?: 0.0) == 0.0
    }

    private fun formattedToday(): String =
        LocalDate.now().format(DateTimeFormatter.ofPattern("MMM d, yyyy", Locale.CANADA))

    private fun sanitize(value: String): String =
        value.replace(Regex("[^A-Za-z0-9]+"), "-").trim('-').ifBlank { "receipt" }
}

private data class PdfLineItem(
    val description: String,
    val quantity: String,
    val rate: String,
    val amount: String
)
