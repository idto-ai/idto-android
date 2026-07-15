package ai.idto.sdk.internal

import android.content.Context
import android.graphics.Bitmap
import android.graphics.Color
import android.os.Handler
import android.util.TypedValue
import android.view.Gravity
import android.view.View
import android.view.ViewGroup
import android.view.ViewGroup.LayoutParams.MATCH_PARENT
import android.view.ViewGroup.LayoutParams.WRAP_CONTENT
import android.webkit.RenderProcessGoneDetail
import android.webkit.WebResourceRequest
import android.webkit.WebView
import android.webkit.WebViewClient
import android.widget.FrameLayout
import android.widget.LinearLayout
import android.widget.ProgressBar
import android.widget.TextView
import androidx.webkit.WebViewCompat
import androidx.webkit.WebViewFeature

class DigiLockerPopup(
    context: Context,
    url: String,
    isAllowed: (String) -> Boolean,
    isHeld: (String) -> Boolean,
    onUnheldCapture: (List<String>) -> Unit,
    private val onOpener: (Any?, String) -> Unit,
    private val onPopupClose: () -> Unit,
    mainHandler: Handler,
) {

    private val progress = ProgressBar(context)
    val webView: WebView
    val contentView: View

    init {
        val bridge = IDtoBridge { raw -> mainHandler.post { onPopupRaw(raw) } }
        val client = object : WebViewClient() {
            override fun shouldOverrideUrlLoading(view: WebView?, request: WebResourceRequest?): Boolean {
                val u = request?.url?.toString() ?: return true
                return !isAllowed(u)
            }

            override fun onPageStarted(view: WebView?, url: String?, favicon: Bitmap?) {
                progress.visibility = View.VISIBLE
                if (!supportsDocumentStart()) view?.evaluateJavascript(BridgeJs.POPUP_OPENER_BRIDGE_JS, null)
            }

            override fun onPageFinished(view: WebView?, url: String?) {
                progress.visibility = View.GONE
            }

            override fun onRenderProcessGone(view: WebView?, detail: RenderProcessGoneDetail?): Boolean {
                onPopupClose()
                return true
            }
        }
        webView = IDtoWebViews.create(context, bridge, client, IDtoWebChromeClient(isHeld, onUnheldCapture))
        injectOpenerBridge()
        contentView = buildContent(context)
        webView.loadUrl(url)
    }

    fun destroy() {
        (webView.parent as? ViewGroup)?.removeView(webView)
        webView.destroy()
    }

    private fun onPopupRaw(raw: String) {
        when (val message = BridgeMessages.parsePopup(raw)) {
            is PopupMessage.Opener -> onOpener(message.data, message.origin)
            PopupMessage.Close -> onPopupClose()
            null -> Unit
        }
    }

    private fun injectOpenerBridge() {
        if (supportsDocumentStart()) {
            WebViewCompat.addDocumentStartJavaScript(webView, BridgeJs.POPUP_OPENER_BRIDGE_JS, setOf("*"))
        }
    }

    private fun supportsDocumentStart(): Boolean =
        WebViewFeature.isFeatureSupported(WebViewFeature.DOCUMENT_START_SCRIPT)

    private fun buildContent(context: Context): View {
        val root = FrameLayout(context).apply { setBackgroundColor(Color.WHITE) }
        val column = LinearLayout(context).apply { orientation = LinearLayout.VERTICAL }
        column.addView(buildHeader(context), LinearLayout.LayoutParams(MATCH_PARENT, WRAP_CONTENT))
        column.addView(webView, LinearLayout.LayoutParams(MATCH_PARENT, 0, 1f))
        root.addView(column, FrameLayout.LayoutParams(MATCH_PARENT, MATCH_PARENT))
        root.addView(progress, FrameLayout.LayoutParams(WRAP_CONTENT, WRAP_CONTENT, Gravity.CENTER))
        return root
    }

    private fun buildHeader(context: Context): View {
        val pad = dp(context, 16)
        val bar = LinearLayout(context).apply {
            orientation = LinearLayout.HORIZONTAL
            gravity = Gravity.CENTER_VERTICAL
            setPadding(pad, dp(context, 12), pad, dp(context, 12))
        }
        val title = TextView(context).apply {
            text = HEADER_TITLE
            setTextColor(Color.parseColor("#111111"))
            setTextSize(TypedValue.COMPLEX_UNIT_SP, 16f)
        }
        val close = TextView(context).apply {
            text = CLOSE_LABEL
            setTextColor(Color.parseColor("#2563EB"))
            setTextSize(TypedValue.COMPLEX_UNIT_SP, 16f)
            setPadding(dp(context, 8), 0, 0, 0)
            setOnClickListener { onPopupClose() }
        }
        bar.addView(title, LinearLayout.LayoutParams(0, WRAP_CONTENT, 1f))
        bar.addView(close, LinearLayout.LayoutParams(WRAP_CONTENT, WRAP_CONTENT))
        return bar
    }

    private fun dp(context: Context, value: Int): Int =
        (value * context.resources.displayMetrics.density).toInt()

    private companion object {
        const val HEADER_TITLE = "DigiLocker"
        const val CLOSE_LABEL = "Close"
    }
}
