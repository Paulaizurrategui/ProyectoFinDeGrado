package com.paulaizurrategui.urtriply.ui.screens

import android.annotation.SuppressLint
import android.webkit.WebView
import android.webkit.WebViewClient
import android.webkit.WebSettings
import androidx.activity.compose.BackHandler
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.getValue
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.viewinterop.AndroidView
import androidx.compose.ui.res.stringResource
import com.paulaizurrategui.urtriply.R
import com.paulaizurrategui.urtriply.ui.components.UrTriplyGradientScaffold

@SuppressLint("SetJavaScriptEnabled")
@Composable
fun WebViewScreen(
    url: String,
    onBack: () -> Unit
) {
    var isLoading by remember { mutableStateOf(true) }

    BackHandler(onBack = onBack)

    UrTriplyGradientScaffold(title = androidx.compose.ui.res.stringResource(R.string.webview_title), onBack = onBack) {
        Box(modifier = Modifier.fillMaxSize()) {
            if (url.isBlank()) {
                Text(stringResource(R.string.webview_no_url), modifier = Modifier.align(Alignment.Center))
            } else {
                val finalUrl = remember(url) {
                    if (url.startsWith("http://") || url.startsWith("https://")) url else "https://$url"
                }

                AndroidView(
                    modifier = Modifier.fillMaxSize(),
                    factory = { context ->
                        WebView(context).apply {
                            settings.javaScriptEnabled = true
                            settings.domStorageEnabled = true
                            settings.mixedContentMode = WebSettings.MIXED_CONTENT_COMPATIBILITY_MODE
                            webViewClient = object : WebViewClient() {
                                override fun onPageFinished(view: WebView?, finishedUrl: String?) {
                                    isLoading = false
                                }
                                override fun onReceivedError(view: WebView?, errorCode: Int, description: String?, failingUrl: String?) {
                                    isLoading = false
                                }
                            }
                            loadUrl(finalUrl)
                        }
                    },
                    update = { webView ->
                        if (webView.url != finalUrl) {
                            webView.loadUrl(finalUrl)
                        }
                    }
                )
            }

            if (isLoading) {
                Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                    CircularProgressIndicator(color = MaterialTheme.colorScheme.primary)
                }
            }
        }
    }
}