package io.github.anenasa.news

import android.annotation.SuppressLint
import android.os.SystemClock
import android.util.Log
import android.view.MotionEvent
import android.view.View
import android.webkit.WebView
import android.webkit.WebViewClient
import androidx.lifecycle.LifecycleOwner
import androidx.lifecycle.coroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import kotlinx.coroutines.suspendCancellableCoroutine
import kotlin.coroutines.resume
import kotlin.time.Duration.Companion.milliseconds

@SuppressLint("SetJavaScriptEnabled")
class WebViewHelper(webView: WebView, lifecycleOwner: LifecycleOwner) {
    var onPageFinishedExecuted = false
    private val scope = lifecycleOwner.lifecycle.coroutineScope
    private var webAutomationJob: Job? = null
    var scripts: List<String> = emptyList()

    init {
        WebView.setWebContentsDebuggingEnabled(true)
        webView.settings.javaScriptEnabled = true
        webView.settings.domStorageEnabled = true
        webView.settings.mediaPlaybackRequiresUserGesture = false
        webView.settings.useWideViewPort = true
        webView.settings.loadWithOverviewMode = true
        webView.settings.textZoom = 100
        webView.webViewClient = object : WebViewClient() {
            override fun onPageFinished(view: WebView?, url: String?) {
                super.onPageFinished(view, url)
                if (onPageFinishedExecuted) return
                onPageFinishedExecuted = true
                webAutomationJob?.cancel()
                webAutomationJob = scope.launch(Dispatchers.Main) {
                    try {
                        view?.runScript(scripts)
                    } catch (e: Exception) {
                        Log.e(TAG, "runScript failed", e)
                    }
                }
            }
        }
    }

    fun loadUrl(webView: WebView, url: String, scripts: List<String>) {
        onPageFinishedExecuted = false
        webAutomationJob?.cancel()
        this.scripts = scripts
        webView.loadUrl(url)
        webView.visibility = View.VISIBLE
    }

    fun stop(webView: WebView) {
        webView.visibility = View.INVISIBLE
        webAutomationJob?.cancel()
        webView.loadUrl("about:blank")
    }

    companion object {
        const val TAG = "WebViewHelper"
    }
}

suspend fun WebView.runScript(scripts: List<String>) {
    // FIXME: clear scripts when loading new page
    var index = 0
    while (index < scripts.size) {
        val parts = scripts[index++].split(',', limit=3)
        delay(parts[1].toLong().milliseconds)
        if (parts[0] == "tap") {
            val (x, y) = parts[2].split(',', limit=2)
            simulateTap(x.toInt(), y.toInt())
        } else if (parts[0] == "js") {
            val result = awaitEvaluateJavascript(parts[2])
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
