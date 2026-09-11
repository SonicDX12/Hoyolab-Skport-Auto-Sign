package com.autosign.app.ui.components

import android.util.Log
import android.webkit.ConsoleMessage
import android.webkit.CookieManager
import android.webkit.WebChromeClient
import android.webkit.WebResourceError
import android.webkit.WebResourceRequest
import android.webkit.WebResourceResponse
import android.webkit.WebView
import android.webkit.WebViewClient
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Close
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.viewinterop.AndroidView
import androidx.compose.ui.window.Dialog
import androidx.compose.ui.window.DialogProperties
import kotlinx.coroutines.delay

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun TokenLoginWebView(
    loginUrl: String,
    onCookiesUpdated: (rawCookieHeader: String) -> Unit,
    onDismiss: () -> Unit,
    userAgentOverride: String? = null,
    // Some cookies (e.g. SkPort's ACCOUNT_TOKEN) are scoped to a Path other than "/", so
    // CookieManager only returns them for a URL under that path — not for the page actually
    // being viewed. When set, this URL is polled in addition to the page's own current URL.
    cookieLookupUrl: String? = null
) {
    Dialog(onDismissRequest = onDismiss, properties = DialogProperties(usePlatformDefaultWidth = false)) {
        Scaffold(
            topBar = {
                TopAppBar(
                    title = { Text("Log in") },
                    navigationIcon = {
                        IconButton(onClick = onDismiss) { Icon(Icons.Default.Close, contentDescription = "Close") }
                    }
                )
            }
        ) { padding ->
            val context = LocalContext.current
            var currentUrl by remember { mutableStateOf(loginUrl) }
            val webView = remember {
                WebView(context).apply {
                    settings.javaScriptEnabled = true
                    settings.domStorageEnabled = true
                    settings.useWideViewPort = true
                    settings.loadWithOverviewMode = true
                    // Some login SPAs serve a reduced/blank shell when they detect the "; wv)"
                    // WebView marker in the default UA — present as a normal browser instead.
                    settings.userAgentString = userAgentOverride ?: settings.userAgentString.replace("; wv", "")
                    CookieManager.getInstance().setAcceptCookie(true)
                    CookieManager.getInstance().setAcceptThirdPartyCookies(this, true)
                    webViewClient = object : WebViewClient() {
                        override fun onPageFinished(view: WebView, url: String) {
                            currentUrl = url
                            CookieManager.getInstance().getCookie(url)?.let(onCookiesUpdated)
                        }
                        override fun onReceivedError(view: WebView, request: WebResourceRequest, error: WebResourceError) {
                            Log.w("AutoSignWebView", "onReceivedError url=${request.url} code=${error.errorCode} desc=${error.description}")
                        }
                        override fun onReceivedHttpError(view: WebView, request: WebResourceRequest, errorResponse: WebResourceResponse) {
                            Log.w("AutoSignWebView", "onReceivedHttpError url=${request.url} status=${errorResponse.statusCode}")
                        }
                    }
                    webChromeClient = object : WebChromeClient() {
                        override fun onConsoleMessage(message: ConsoleMessage): Boolean {
                            Log.d("AutoSignWebView", "console[${message.messageLevel()}] ${message.message()}")
                            return true
                        }
                    }
                    loadUrl(loginUrl)
                }
            }
            // Many login pages finish authenticating via client-side JS/XHR without a further
            // full-page navigation, so onPageFinished never fires again after login — poll instead.
            LaunchedEffect(Unit) {
                while (true) {
                    delay(1000)
                    CookieManager.getInstance().getCookie(currentUrl)?.let(onCookiesUpdated)
                    cookieLookupUrl?.let { url -> CookieManager.getInstance().getCookie(url)?.let(onCookiesUpdated) }
                }
            }
            // Rotation/back-press mid-login must not leak the WebView or leave a live callback registered.
            DisposableEffect(Unit) {
                onDispose {
                    webView.stopLoading()
                    webView.webViewClient = WebViewClient()
                    webView.webChromeClient = null
                    webView.destroy()
                }
            }
            AndroidView(factory = { webView }, modifier = Modifier.padding(padding).fillMaxSize())
        }
    }
}
