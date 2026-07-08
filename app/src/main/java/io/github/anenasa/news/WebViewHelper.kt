package io.github.anenasa.news

import android.annotation.SuppressLint
import android.view.View
import android.webkit.WebView
import android.webkit.WebViewClient

@SuppressLint("SetJavaScriptEnabled")
class WebViewHelper(val webView: WebView) {

    init {
        WebView.setWebContentsDebuggingEnabled(true)
        webView.settings.javaScriptEnabled = true
        webView.settings.domStorageEnabled = true
        webView.settings.mediaPlaybackRequiresUserGesture = false
        webView.settings.useWideViewPort = true
        webView.settings.loadWithOverviewMode = true
        webView.settings.textZoom = 100
        webView.webViewClient = WebViewClient()
    }

    fun loadUrl(url: String) {
        webView.loadUrl(url)
        webView.visibility = View.VISIBLE
    }

    fun stop() {
        webView.visibility = View.INVISIBLE
        webView.loadUrl("about:blank")
    }
}
