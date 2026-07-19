package io.github.anenasa.news

import android.annotation.SuppressLint
import android.os.SystemClock
import android.view.MotionEvent
import android.view.View
import android.webkit.CookieManager
import android.webkit.JsPromptResult
import android.webkit.WebChromeClient
import android.webkit.WebView
import android.webkit.WebViewClient
import androidx.lifecycle.coroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.launch
import okhttp3.Headers

@SuppressLint("SetJavaScriptEnabled")
class WebViewHelper {
    var onPageFinishedExecuted = false
    private var webAutomationJob: Job? = null
    var scriptOnFinish: String = ""

    fun createWebView(mainActivity: MainActivity): WebView {
        val scope = mainActivity.lifecycle.coroutineScope
        val cookieManager: CookieManager = CookieManager.getInstance()
        cookieManager.removeAllCookie()
        MyApplication.cookieJar.cookies.forEach { cookie ->
            val scheme = if (cookie.secure) "https://" else "http://"
            val domain = cookie.domain.let { if (it.startsWith(".")) it.drop(1) else it }
            cookieManager.setCookie("$scheme$domain${cookie.path}", cookie.toString())
        }
        cookieManager.flush()
        WebView.setWebContentsDebuggingEnabled(true)
        return WebView(mainActivity).apply {
            settings.javaScriptEnabled = true
            settings.domStorageEnabled = true
            settings.mediaPlaybackRequiresUserGesture = false
            settings.useWideViewPort = true
            settings.loadWithOverviewMode = true
            settings.textZoom = 100
            webViewClient = object : WebViewClient() {
                override fun onPageFinished(view: WebView?, url: String?) {
                    super.onPageFinished(view, url)
                    if (view?.url != url || url == "about:blank") return
                    mainActivity.textInfo.text = ""
                    if (onPageFinishedExecuted) return
                    onPageFinishedExecuted = true
                    webAutomationJob?.cancel()
                    webAutomationJob = scope.launch(Dispatchers.Main) {
                        view?.evaluateJavascript(scriptOnFinish, null)
                    }
                }
            }
            webChromeClient = object : WebChromeClient() {
                override fun onJsPrompt(
                    view: WebView?,
                    url: String?,
                    message: String?,
                    defaultValue: String?,
                    result: JsPromptResult?
                ): Boolean {
                    if (message?.startsWith("tap:") == true && message.contains(',')) {
                        result?.confirm()
                        val (x, y) = message.removePrefix("tap:").split(',', limit=2)
                        simulateTap(x.toIntOrNull() ?: 50, y.toIntOrNull() ?: 50)
                        return true
                    } else if (message?.startsWith("fullscreen:") == true) {
                        message.removePrefix("fullscreen:").also { element ->
                            this@apply.evaluateJavascript("""
                                $element.style.position = 'fixed';
                                $element.style.top = '0';
                                $element.style.left = '0';
                                $element.style.zIndex = '999999';
                                $element.style.width = '100%';
                                $element.style.height = '100%';
                                $element.style.objectFit = 'fill';
                                document.body.appendChild($element);
                            """, null)
                        }
                        result?.confirm()
                        return true
                    }
                    return super.onJsPrompt(view, url, message, defaultValue, result)
                }
            }
        }
    }

    fun loadUrl(webView: WebView?, url: String, scriptOnFinish: String, okHttpHeaders: Headers) {
        onPageFinishedExecuted = false
        webAutomationJob?.cancel()
        this.scriptOnFinish = scriptOnFinish
        webView?.settings?.userAgentString = okHttpHeaders["User-Agent"]
        webView?.loadUrl(url, okHttpHeaders.toMap())
        webView?.visibility = View.VISIBLE
    }

    fun stop(webView: WebView?) {
        webView?.visibility = View.INVISIBLE
        webAutomationJob?.cancel()
        webView?.loadUrl("about:blank")
    }
}

fun WebView.simulateTap(percentX: Int, percentY: Int) {
    val time = SystemClock.uptimeMillis()
    val downEvent = MotionEvent.obtain(
        time, time,
        MotionEvent.ACTION_DOWN, this.width * percentX / 100f, this.height * percentY / 100f, 0
    )
    val upEvent = MotionEvent.obtain(
        time, time + 50,
        MotionEvent.ACTION_UP, this.width * percentX / 100f, this.height * percentY / 100f, 0
    )
    dispatchTouchEvent(downEvent)
    dispatchTouchEvent(upEvent)
    downEvent.recycle()
    upEvent.recycle()
}
