package codewhale.doortreeandroid

import android.annotation.SuppressLint
import android.app.Activity
import android.content.Context
import android.content.Intent
import android.content.ContextWrapper
import android.content.pm.ActivityInfo
import android.graphics.Bitmap
import android.graphics.Color
import android.graphics.Paint
import android.graphics.Path
import android.os.CancellationSignal
import android.os.ParcelFileDescriptor
import android.print.PageRange
import android.print.PrintAttributes
import android.print.PrintDocumentAdapter
import android.print.PrintDocumentInfo
import android.print.PrintManager
import android.view.ViewGroup
import android.webkit.WebChromeClient
import android.webkit.WebResourceRequest
import android.webkit.WebView
import android.webkit.WebViewClient
import android.widget.Toast
import androidx.core.content.FileProvider
import androidx.core.view.WindowCompat
import androidx.core.view.WindowInsetsCompat
import androidx.core.view.WindowInsetsControllerCompat
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.gestures.detectDragGestures
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.safeDrawingPadding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Button
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateListOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.layout.onSizeChanged
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.IntSize
import androidx.compose.ui.viewinterop.AndroidView
import androidx.compose.ui.window.Dialog
import androidx.compose.ui.window.DialogProperties
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import codewhale.doortreeandroid.ui.theme.DoorTreeTheme
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import java.io.File
import java.io.FileOutputStream
import java.net.URL
import java.net.URLEncoder
import java.nio.charset.StandardCharsets
import java.time.LocalDate
import java.time.format.DateTimeFormatter
import java.time.format.FormatStyle
import java.util.Locale

@Composable
fun PDFDocumentSheetView(
    document: DocumentItem,
    tenantName: String = "",
    onRenewalDecision: suspend (String, Bitmap?) -> Unit = { _, _ -> },
    onDismiss: () -> Unit
) {
    val context = LocalContext.current
    val scope = rememberCoroutineScope()
    var isPreparingDocument by remember(document.id) { mutableStateOf(false) }
    var isSubmittingDecision by remember(document.id) { mutableStateOf(false) }
    var showingSignatureCapture by rememberSaveable(document.id) { mutableStateOf(false) }
    var showingDecisionConfirmation by rememberSaveable(document.id) { mutableStateOf(false) }
    var pendingRenewalStatus by rememberSaveable(document.id) { mutableStateOf("accept") }

    fun confirmRenewalDecision(status: String) {
        pendingRenewalStatus = status
        showingDecisionConfirmation = true
    }

    fun submitRenewalDecision(status: String, signatureBitmap: Bitmap?) {
        if (isSubmittingDecision) return
        scope.launch {
            isSubmittingDecision = true
            try {
                onRenewalDecision(status, signatureBitmap)
                onDismiss()
            } catch (error: Exception) {
                Toast.makeText(
                    context,
                    error.localizedMessage ?: "Unable to update this renewal notice right now.",
                    Toast.LENGTH_SHORT
                ).show()
            } finally {
                isSubmittingDecision = false
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
            Column(modifier = Modifier.fillMaxSize()) {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .topSafeAreaPadding()
                        .padding(horizontal = DoorTreeTheme.screenHorizontalPadding, vertical = 16.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    HeaderIconButton(systemName = "xmark", onClick = onDismiss)
                    Text(
                        text = document.filename,
                        color = DoorTreeTheme.textPrimary,
                        modifier = Modifier
                            .weight(1f)
                            .padding(horizontal = 14.dp)
                    )
                    val url = document.url
                    if (url == null) {
                        Spacer(modifier = Modifier.size(46.dp))
                    } else if (isPreparingDocument) {
                        Box(
                            modifier = Modifier.size(46.dp),
                            contentAlignment = Alignment.Center
                        ) {
                            CircularProgressIndicator(
                                modifier = Modifier.size(24.dp),
                                color = DoorTreeTheme.gradientStart
                            )
                        }
                    } else {
                        HeaderIconButton(
                            systemName = "printer",
                            onClick = {
                                scope.launch {
                                    isPreparingDocument = true
                                    try {
                                        val pdfFile = cachedPDFFile(context, document, url)
                                        printPDF(context, document.filename, pdfFile)
                                    } catch (_: Exception) {
                                        Toast.makeText(context, "Unable to print this document right now.", Toast.LENGTH_SHORT).show()
                                    } finally {
                                        isPreparingDocument = false
                                    }
                                }
                            }
                        )
                        HeaderIconButton(
                            systemName = "square.and.arrow.up",
                            onClick = {
                                scope.launch {
                                    isPreparingDocument = true
                                    try {
                                        val pdfFile = cachedPDFFile(context, document, url)
                                        sharePDF(context, document.filename, pdfFile)
                                    } catch (_: Exception) {
                                        Toast.makeText(context, "Unable to share this document right now.", Toast.LENGTH_SHORT).show()
                                    } finally {
                                        isPreparingDocument = false
                                    }
                                }
                            }
                        )
                    }
                }

                if (document.requiresRenewalAction) {
                    RenewalDecisionBar(
                        isSubmitting = isSubmittingDecision,
                        onRefuse = { confirmRenewalDecision("refuse") },
                        onNotRenewing = { confirmRenewalDecision("notRenewing") },
                        onAccept = { confirmRenewalDecision("accept") }
                    )
                }

                val url = document.url
                if (url == null) {
                    Box(modifier = Modifier.padding(DoorTreeTheme.screenHorizontalPadding)) {
                        SectionPlaceholder(
                            systemName = "doc.text",
                            title = "Document unavailable",
                            message = "This document could not be opened right now."
                        )
                    }
                } else {
                    PDFWebView(
                        url = url,
                        modifier = Modifier.weight(1f)
                    )
                }
            }
        }
    }

    if (showingDecisionConfirmation) {
        AlertDialog(
            onDismissRequest = { showingDecisionConfirmation = false },
            containerColor = DoorTreeTheme.backgroundPrimary,
            titleContentColor = DoorTreeTheme.textPrimary,
            textContentColor = DoorTreeTheme.textSecondary,
            title = { Text(L("Are you sure?")) },
            text = { Text(L("You will be asked to sign before sending your reply to the landlord.")) },
            confirmButton = {
                TextButton(
                    onClick = {
                        showingDecisionConfirmation = false
                        showingSignatureCapture = true
                    }
                ) {
                    Text(
                        text = renewalDecisionConfirmationTitle(pendingRenewalStatus),
                        color = DoorTreeTheme.gradientStart
                    )
                }
            },
            dismissButton = {
                TextButton(onClick = { showingDecisionConfirmation = false }) {
                    Text(L("Cancel"), color = DoorTreeTheme.textSecondary)
                }
            }
        )
    }

    if (showingSignatureCapture) {
        RenewalSignatureCaptureView(
            tenantName = tenantName,
            onCancel = { showingSignatureCapture = false },
            onComplete = { signatureBitmap ->
                showingSignatureCapture = false
                submitRenewalDecision(pendingRenewalStatus, signatureBitmap)
            }
        )
    }
}

@Composable
private fun RenewalDecisionBar(
    isSubmitting: Boolean,
    onRefuse: () -> Unit,
    onNotRenewing: () -> Unit,
    onAccept: () -> Unit
) {
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .background(DoorTreeTheme.barGlassTint)
            .padding(horizontal = DoorTreeTheme.screenHorizontalPadding, vertical = 8.dp),
        verticalArrangement = Arrangement.spacedBy(8.dp)
    ) {
        Column(verticalArrangement = Arrangement.spacedBy(4.dp)) {
            Text(
                text = L("Action Required"),
                color = DoorTreeTheme.destructive,
                fontWeight = FontWeight.SemiBold,
                fontSize = 14.sp,
                maxLines = 1,
                modifier = Modifier
                    .background(DoorTreeTheme.destructive.copy(alpha = 0.14f), RoundedCornerShape(999.dp))
                    .padding(horizontal = 9.dp, vertical = 4.dp)
            )
            Text(
                text = L("Choose how you want to respond to this renewal notice."),
                color = DoorTreeTheme.textSecondary,
                fontSize = 15.sp,
                lineHeight = 18.sp,
                maxLines = 2,
                overflow = TextOverflow.Ellipsis
            )
        }

        Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
            OutlinedButton(
                onClick = onRefuse,
                enabled = !isSubmitting,
                modifier = Modifier
                    .weight(1f)
                    .height(42.dp),
                contentPadding = PaddingValues(horizontal = 4.dp, vertical = 0.dp)
            ) {
                Text(
                    text = L("Refuse"),
                    color = DoorTreeTheme.destructive,
                    fontSize = 13.sp,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis
                )
            }
            OutlinedButton(
                onClick = onNotRenewing,
                enabled = !isSubmitting,
                modifier = Modifier
                    .weight(1f)
                    .height(42.dp),
                contentPadding = PaddingValues(horizontal = 4.dp, vertical = 0.dp)
            ) {
                Text(
                    text = L("Not Renewing"),
                    color = DoorTreeTheme.destructive,
                    fontSize = 13.sp,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis
                )
            }
            Button(
                onClick = onAccept,
                enabled = !isSubmitting,
                modifier = Modifier
                    .weight(1f)
                    .height(42.dp),
                contentPadding = PaddingValues(horizontal = 4.dp, vertical = 0.dp)
            ) {
                if (isSubmitting) {
                    CircularProgressIndicator(
                        modifier = Modifier.size(18.dp),
                        color = DoorTreeTheme.textPrimary
                    )
                } else {
                    Text(
                        text = L("Accept"),
                        fontSize = 13.sp,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis
                    )
                }
            }
        }
    }
}

private fun renewalDecisionConfirmationTitle(status: String): String {
    return when (status) {
        "accept" -> L("Accept")
        "refuse" -> L("Refuse")
        "notRenewing" -> L("Not Renewing")
        else -> L("common.continue")
    }
}

@Composable
private fun RenewalSignatureCaptureView(
    tenantName: String,
    onCancel: () -> Unit,
    onComplete: (Bitmap) -> Unit
) {
    val context = LocalContext.current
    val lines = remember { mutableStateListOf<List<Offset>>() }
    var currentLine by remember { mutableStateOf<List<Offset>>(emptyList()) }
    var canvasSize by remember { mutableStateOf(IntSize.Zero) }
    val hasSignature = lines.isNotEmpty() || currentLine.isNotEmpty()

    DisposableEffect(Unit) {
        val activity = context.findActivity()
        val previousOrientation = activity?.requestedOrientation
        val window = activity?.window
        val insetsController = window?.let { WindowInsetsControllerCompat(it, it.decorView) }
        window?.let { WindowCompat.setDecorFitsSystemWindows(it, false) }
        insetsController?.hide(WindowInsetsCompat.Type.systemBars())
        insetsController?.systemBarsBehavior = WindowInsetsControllerCompat.BEHAVIOR_SHOW_TRANSIENT_BARS_BY_SWIPE
        activity?.requestedOrientation = ActivityInfo.SCREEN_ORIENTATION_LANDSCAPE
        onDispose {
            if (previousOrientation != null) {
                activity.requestedOrientation = previousOrientation
            }
            if (window != null) {
                WindowCompat.setDecorFitsSystemWindows(window, true)
            }
            insetsController?.show(WindowInsetsCompat.Type.systemBars())
        }
    }

    Dialog(
        onDismissRequest = onCancel,
        properties = DialogProperties(
            usePlatformDefaultWidth = false,
            decorFitsSystemWindows = false
        )
    ) {
        Column(
            modifier = Modifier
                .fillMaxSize()
                .background(DoorTreeTheme.backgroundPrimary)
                .safeDrawingPadding()
                .padding(horizontal = 18.dp, vertical = 10.dp),
            verticalArrangement = Arrangement.spacedBy(10.dp)
        ) {
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .height(48.dp),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(16.dp)
            ) {
                Column(modifier = Modifier.weight(1f), verticalArrangement = Arrangement.spacedBy(2.dp)) {
                    Text(
                        text = L("Sign Renewal Notice"),
                        color = DoorTreeTheme.textPrimary,
                        fontWeight = FontWeight.Bold,
                        fontSize = 20.sp,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis
                    )
                    val displayName = tenantName.trim().ifBlank { "Tenant" }
                    Text(
                        text = "$displayName - ${LocalDate.now().format(DateTimeFormatter.ofLocalizedDate(FormatStyle.MEDIUM).withLocale(Locale.getDefault()))}",
                        color = DoorTreeTheme.textSecondary,
                        fontSize = 13.sp,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis
                    )
                }
                OutlinedButton(
                    onClick = onCancel,
                    modifier = Modifier.height(40.dp),
                    contentPadding = PaddingValues(horizontal = 14.dp, vertical = 0.dp)
                ) {
                    Text(L("Cancel"), maxLines = 1)
                }
            }

            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .weight(1f)
                    .background(DoorTreeTheme.barGlassTint, RoundedCornerShape(16.dp))
                    .padding(1.dp)
            ) {
                Canvas(
                    modifier = Modifier
                        .fillMaxSize()
                        .onSizeChanged { canvasSize = it }
                        .pointerInput(Unit) {
                            detectDragGestures(
                                onDragStart = { offset ->
                                    currentLine = listOf(offset)
                                },
                                onDrag = { change, _ ->
                                    currentLine = currentLine + change.position
                                },
                                onDragEnd = {
                                    if (currentLine.isNotEmpty()) {
                                        lines.add(currentLine)
                                    }
                                    currentLine = emptyList()
                                },
                                onDragCancel = {
                                    currentLine = emptyList()
                                }
                            )
                        }
                ) {
                    val strokeWidth = 3.dp.toPx()
                    (lines + listOf(currentLine)).forEach { line ->
                        line.zipWithNext().forEach { (start, end) ->
                            drawLine(
                                color = androidx.compose.ui.graphics.Color.Black,
                                start = start,
                                end = end,
                                strokeWidth = strokeWidth,
                                cap = StrokeCap.Round
                            )
                        }
                    }
                }

                Text(
                    text = L("Sign here"),
                    color = DoorTreeTheme.textSecondary,
                    modifier = Modifier
                        .align(Alignment.BottomStart)
                        .padding(16.dp)
                )
            }

            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .height(44.dp),
                horizontalArrangement = Arrangement.spacedBy(12.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                OutlinedButton(
                    onClick = {
                        lines.clear()
                        currentLine = emptyList()
                    },
                    enabled = hasSignature,
                    modifier = Modifier.height(42.dp),
                    contentPadding = PaddingValues(horizontal = 18.dp, vertical = 0.dp)
                ) {
                    Text(L("Clear"))
                }
                Spacer(modifier = Modifier.weight(1f))
                Button(
                    onClick = {
                        onComplete(signatureBitmap(lines.toList() + listOf(currentLine), canvasSize))
                    },
                    enabled = hasSignature,
                    modifier = Modifier.height(42.dp),
                    contentPadding = PaddingValues(horizontal = 18.dp, vertical = 0.dp)
                ) {
                    Text(L("Use Signature"))
                }
            }
        }
    }
}

private fun signatureBitmap(lines: List<List<Offset>>, canvasSize: IntSize): Bitmap {
    val width = if (canvasSize.width > 0) canvasSize.width else 800
    val height = if (canvasSize.height > 0) canvasSize.height else 300
    val bitmap = Bitmap.createBitmap(width, height, Bitmap.Config.ARGB_8888)
    val canvas = android.graphics.Canvas(bitmap)
    val paint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
        color = Color.BLACK
        strokeWidth = 3f
        strokeCap = Paint.Cap.ROUND
        strokeJoin = Paint.Join.ROUND
        style = Paint.Style.STROKE
    }

    lines.filter { it.size > 1 }.forEach { line ->
        val path = Path()
        path.moveTo(line.first().x, line.first().y)
        line.drop(1).forEach { point ->
            path.lineTo(point.x, point.y)
        }
        canvas.drawPath(path, paint)
    }

    return bitmap
}

private suspend fun cachedPDFFile(context: Context, document: DocumentItem, url: String): File {
    return withContext(Dispatchers.IO) {
        val cacheDirectory = File(context.cacheDir, "shared_pdfs").apply {
            mkdirs()
        }
        val file = File(cacheDirectory, "${document.id}-${url.hashCode()}-${sanitizedPDFName(document.filename)}")
        if (file.exists() && file.length() > 0) {
            return@withContext file
        }

        file.delete()
        URL(url).openStream().use { input ->
            FileOutputStream(file).use { output ->
                input.copyTo(output)
            }
        }
        file
    }
}

private fun sharePDF(context: Context, filename: String, file: File) {
    val uri = FileProvider.getUriForFile(context, "${context.packageName}.fileprovider", file)
    val shareIntent = Intent(Intent.ACTION_SEND).apply {
        type = "application/pdf"
        putExtra(Intent.EXTRA_STREAM, uri)
        putExtra(Intent.EXTRA_TITLE, filename)
        addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION)
    }

    context.startActivity(Intent.createChooser(shareIntent, "Share document"))
}

private fun printPDF(context: Context, filename: String, file: File) {
    val printManager = context.getSystemService(Context.PRINT_SERVICE) as PrintManager
    printManager.print(
        filename,
        PDFFilePrintDocumentAdapter(file, sanitizedPDFName(filename)),
        PrintAttributes.Builder().build()
    )
}

private fun sanitizedPDFName(name: String): String {
    val baseName = name
        .replace(Regex("[^A-Za-z0-9 _.-]"), "-")
        .trim()
        .ifBlank { "DoorTree Document" }
    return if (baseName.endsWith(".pdf", ignoreCase = true)) baseName else "$baseName.pdf"
}

private tailrec fun Context.findActivity(): Activity? {
    return when (this) {
        is Activity -> this
        is ContextWrapper -> baseContext.findActivity()
        else -> null
    }
}

private class PDFFilePrintDocumentAdapter(
    private val file: File,
    private val filename: String
) : PrintDocumentAdapter() {
    override fun onLayout(
        oldAttributes: PrintAttributes?,
        newAttributes: PrintAttributes?,
        cancellationSignal: CancellationSignal?,
        callback: LayoutResultCallback?,
        extras: android.os.Bundle?
    ) {
        if (cancellationSignal?.isCanceled == true) {
            callback?.onLayoutCancelled()
            return
        }

        val info = PrintDocumentInfo.Builder(filename)
            .setContentType(PrintDocumentInfo.CONTENT_TYPE_DOCUMENT)
            .setPageCount(PrintDocumentInfo.PAGE_COUNT_UNKNOWN)
            .build()
        callback?.onLayoutFinished(info, oldAttributes != newAttributes)
    }

    override fun onWrite(
        pages: Array<out PageRange>?,
        destination: ParcelFileDescriptor?,
        cancellationSignal: CancellationSignal?,
        callback: WriteResultCallback?
    ) {
        if (destination == null) {
            callback?.onWriteFailed("No print destination available.")
            return
        }

        try {
            file.inputStream().use { input ->
                FileOutputStream(destination.fileDescriptor).use { output ->
                    input.copyTo(output)
                }
            }
            if (cancellationSignal?.isCanceled == true) {
                callback?.onWriteCancelled()
            } else {
                callback?.onWriteFinished(arrayOf(PageRange.ALL_PAGES))
            }
        } catch (error: Exception) {
            callback?.onWriteFailed(error.localizedMessage ?: "Unable to print document.")
        }
    }
}

@SuppressLint("SetJavaScriptEnabled")
@Composable
private fun PDFWebView(
    url: String,
    modifier: Modifier = Modifier
) {
    var isLoading by remember(url) { mutableStateOf(true) }
    var webView by remember { mutableStateOf<WebView?>(null) }
    val viewerUrl = remember(url) {
        val encodedUrl = URLEncoder.encode(url, StandardCharsets.UTF_8.toString())
        "https://drive.google.com/viewerng/viewer?embedded=true&url=$encodedUrl"
    }

    DisposableEffect(Unit) {
        onDispose {
            webView?.destroy()
            webView = null
        }
    }

    Box(modifier = modifier.fillMaxSize()) {
        AndroidView(
            modifier = Modifier.fillMaxSize(),
            factory = { context ->
                WebView(context).apply {
                    layoutParams = ViewGroup.LayoutParams(
                        ViewGroup.LayoutParams.MATCH_PARENT,
                        ViewGroup.LayoutParams.MATCH_PARENT
                    )
                    settings.javaScriptEnabled = true
                    settings.domStorageEnabled = true
                    settings.loadsImagesAutomatically = true
                    settings.useWideViewPort = true
                    settings.loadWithOverviewMode = true
                    webChromeClient = WebChromeClient()
                    webViewClient = object : WebViewClient() {
                        override fun onPageStarted(view: WebView?, url: String?, favicon: Bitmap?) {
                            isLoading = true
                        }

                        override fun onPageFinished(view: WebView?, url: String?) {
                            isLoading = false
                        }

                        override fun shouldOverrideUrlLoading(
                            view: WebView?,
                            request: WebResourceRequest?
                        ): Boolean = false
                    }
                    loadUrl(viewerUrl)
                    webView = this
                }
            },
            update = { view ->
                if (view.url != viewerUrl) {
                    view.loadUrl(viewerUrl)
                }
                webView = view
            }
        )

        if (isLoading) {
            CircularProgressIndicator(
                modifier = Modifier
                    .align(Alignment.Center)
                    .size(32.dp),
                color = DoorTreeTheme.gradientStart
            )
        }
    }
}
