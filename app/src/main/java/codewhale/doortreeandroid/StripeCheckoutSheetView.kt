package codewhale.doortreeandroid

import android.annotation.SuppressLint
import android.graphics.Bitmap
import android.view.ViewGroup
import android.webkit.WebChromeClient
import android.webkit.WebResourceRequest
import android.webkit.WebView
import android.webkit.WebViewClient
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Icon
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.viewinterop.AndroidView
import androidx.compose.ui.window.Dialog
import androidx.compose.ui.window.DialogProperties
import androidx.compose.foundation.layout.size
import androidx.compose.ui.unit.dp
import codewhale.doortreeandroid.ui.theme.DoorTreeTheme

@Composable
fun StripeCheckoutSheetView(
    url: String,
    onDismiss: () -> Unit
) {
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
                modifier = Modifier.fillMaxSize()
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
                        text = L("payments.pay_now"),
                        color = DoorTreeTheme.textPrimary
                    )
                }

                StripeCheckoutWebView(
                    url = url,
                    modifier = Modifier.weight(1f)
                )
            }
        }
    }
}

@SuppressLint("SetJavaScriptEnabled")
@Composable
private fun StripeCheckoutWebView(
    url: String,
    modifier: Modifier = Modifier
) {
    var isLoading by remember(url) { mutableStateOf(true) }
    var webView by remember { mutableStateOf<WebView?>(null) }

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
                    loadUrl(url)
                    webView = this
                }
            },
            update = { view ->
                if (view.url != url) {
                    view.loadUrl(url)
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
