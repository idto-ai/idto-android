package ai.idto.sdk.internal

import android.Manifest
import android.annotation.SuppressLint
import android.content.Context
import android.webkit.JavascriptInterface
import android.webkit.PermissionRequest
import android.webkit.RenderProcessGoneDetail
import android.webkit.WebChromeClient
import android.webkit.WebResourceError
import android.webkit.WebResourceRequest
import android.webkit.WebResourceResponse
import android.webkit.WebSettings
import android.webkit.WebView
import android.webkit.WebViewClient

object IDtoWebViews {

    const val BRIDGE_NAME = "IDtoAndroid"

    @SuppressLint("SetJavaScriptEnabled")
    @Suppress("DEPRECATION")
    fun create(
        context: Context,
        bridge: IDtoBridge,
        client: WebViewClient,
        chromeClient: WebChromeClient,
    ): WebView {
        val webView = WebView(context)
        webView.settings.apply {
            javaScriptEnabled = true
            domStorageEnabled = true
            databaseEnabled = true
            mediaPlaybackRequiresUserGesture = false
            javaScriptCanOpenWindowsAutomatically = false
            setSupportMultipleWindows(false)
            allowFileAccess = false
            allowContentAccess = false
            mixedContentMode = WebSettings.MIXED_CONTENT_NEVER_ALLOW
        }
        webView.addJavascriptInterface(bridge, BRIDGE_NAME)
        webView.webViewClient = client
        webView.webChromeClient = chromeClient
        return webView
    }
}

class IDtoBridge(private val onRaw: (String) -> Unit) {
    @JavascriptInterface
    fun postMessage(raw: String) {
        onRaw(raw)
    }
}

class IDtoWebViewClient(
    private val isAllowed: (String) -> Boolean,
    private val isDigiLocker: (String) -> Boolean,
    private val onDigiLocker: (String) -> Unit,
    private val onPreReadyError: () -> Unit,
    private val isSettled: () -> Boolean,
    private val onRenderGone: () -> Unit = {},
) : WebViewClient() {

    override fun shouldOverrideUrlLoading(view: WebView?, request: WebResourceRequest?): Boolean {
        val url = request?.url?.toString() ?: return true
        if (isDigiLocker(url) && isAllowed(url)) {
            onDigiLocker(url)
            return true
        }
        return !isAllowed(url)
    }

    override fun onReceivedError(view: WebView?, request: WebResourceRequest?, error: WebResourceError?) {
        reportIfPreReadyMainFrame(request)
    }

    override fun onReceivedHttpError(
        view: WebView?,
        request: WebResourceRequest?,
        errorResponse: WebResourceResponse?,
    ) {
        reportIfPreReadyMainFrame(request)
    }

    override fun onRenderProcessGone(view: WebView?, detail: RenderProcessGoneDetail?): Boolean {
        onRenderGone()
        return true
    }

    private fun reportIfPreReadyMainFrame(request: WebResourceRequest?) {
        if (request?.isForMainFrame == true && !isSettled()) {
            onPreReadyError()
        }
    }
}

class IDtoWebChromeClient(
    private val isHeld: (String) -> Boolean,
    private val onUnheldCapture: (List<String>) -> Unit = {},
) : WebChromeClient() {

    override fun onPermissionRequest(request: PermissionRequest) {
        val grant = mutableListOf<String>()
        val unheld = mutableListOf<String>()
        for (resource in request.resources) {
            when (resource) {
                PermissionRequest.RESOURCE_VIDEO_CAPTURE ->
                    if (isHeld(Manifest.permission.CAMERA)) grant.add(resource) else unheld.add(Manifest.permission.CAMERA)
                PermissionRequest.RESOURCE_AUDIO_CAPTURE ->
                    if (isHeld(Manifest.permission.RECORD_AUDIO)) grant.add(resource) else unheld.add(Manifest.permission.RECORD_AUDIO)
                else -> grant.add(resource)
            }
        }
        if (unheld.isNotEmpty()) onUnheldCapture(unheld)
        if (grant.isNotEmpty()) request.grant(grant.toTypedArray()) else request.deny()
    }
}
