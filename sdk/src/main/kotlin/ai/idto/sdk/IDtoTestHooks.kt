package ai.idto.sdk

import android.os.Handler
import android.os.Looper
import android.webkit.ValueCallback
import android.webkit.WebView
import androidx.annotation.RestrictTo
import java.lang.ref.WeakReference
import java.util.Collections

@RestrictTo(RestrictTo.Scope.LIBRARY_GROUP)
object IDtoTestHooks {

    @Volatile
    var lastEvent: String? = null
        private set

    private const val MAX_LOG_ENTRIES = 500
    private val logBuffer = Collections.synchronizedList(mutableListOf<String>())
    private var webViewRef: WeakReference<WebView>? = null
    private val mainHandler = Handler(Looper.getMainLooper())

    val logs: List<String>
        get() = synchronized(logBuffer) { logBuffer.toList() }

    fun evaluateJs(js: String, resultCallback: ValueCallback<String>?) {
        mainHandler.post {
            val webView = webViewRef?.get()
            if (webView == null) {
                resultCallback?.onReceiveValue(null)
            } else {
                webView.evaluateJavascript(js, resultCallback)
            }
        }
    }

    fun recordEvent(type: String) {
        lastEvent = type
    }

    fun recordLog(message: String) {
        synchronized(logBuffer) {
            logBuffer.add(message)
            while (logBuffer.size > MAX_LOG_ENTRIES) logBuffer.removeAt(0)
        }
    }

    fun bindWebView(webView: WebView) {
        webViewRef = WeakReference(webView)
    }

    fun unbindWebView() {
        webViewRef = null
    }

    fun reset() {
        lastEvent = null
        synchronized(logBuffer) { logBuffer.clear() }
        webViewRef = null
    }
}
