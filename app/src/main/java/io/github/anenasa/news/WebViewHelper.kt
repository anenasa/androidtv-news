package io.github.anenasa.news

import android.annotation.SuppressLint
import android.os.SystemClock
import android.util.Log
import android.view.MotionEvent
import android.view.View
import android.webkit.CookieManager
import android.webkit.WebView
import android.webkit.WebViewClient
import androidx.lifecycle.coroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import kotlinx.coroutines.suspendCancellableCoroutine
import kotlin.coroutines.resume
import kotlin.time.Duration.Companion.milliseconds

@SuppressLint("SetJavaScriptEnabled")
class WebViewHelper {
    var onPageFinishedExecuted = false
    private var webAutomationJob: Job? = null
    var scripts: List<String> = emptyList()

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
                    if (onPageFinishedExecuted) return
                    onPageFinishedExecuted = true
                    webAutomationJob?.cancel()
                    webAutomationJob = scope.launch(Dispatchers.Main) {
                        view?.runScript(scripts)
                        if (view?.url == url && url != "about:blank") {
                            mainActivity.textInfo.text = ""
                        }
                    }
                }
            }
        }
    }

    fun loadUrl(webView: WebView?, url: String, scripts: List<String>) {
        onPageFinishedExecuted = false
        webAutomationJob?.cancel()
        this.scripts = scripts
        webView?.loadUrl(url)
        webView?.visibility = View.VISIBLE
    }

    fun stop(webView: WebView?) {
        webView?.visibility = View.INVISIBLE
        webAutomationJob?.cancel()
        webView?.loadUrl("about:blank")
    }

    companion object {
        const val TAG = "WebViewHelper"
    }
}

suspend fun WebView.runScript(scripts: List<String>) {
    var index = 0
    while (index < scripts.size) {
        val (type, time, content) = scripts[index++].split(',', limit=3)
            .takeIf { it.size == 3 } ?: run {
                Log.e(WebViewHelper.TAG, "runScript failed: size != 3")
                break
        }
        delay((time.toLongOrNull() ?: 0L).milliseconds)
        if (type == "tap") {
            if (!content.contains(',')) {
                Log.e(WebViewHelper.TAG, "runScript failed: no comma in coordinates")
                break
            }
            val (x, y) = content.split(',', limit=2)
            simulateTap(x.toIntOrNull() ?: 50, y.toIntOrNull() ?: 50)
        } else if (type == "js") {
            val result = awaitEvaluateJavascript(content)
            if (result == "\"retry\"") index--
        }
    }
}

suspend fun WebView.awaitEvaluateJavascript(script: String): String? =
    suspendCancellableCoroutine { continuation ->
        this.evaluateJavascript(script) { result ->
            if (continuation.isActive) {
                continuation.resume(result)
            }
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
