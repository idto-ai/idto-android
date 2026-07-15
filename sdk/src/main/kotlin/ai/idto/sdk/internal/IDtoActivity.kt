package ai.idto.sdk.internal

import android.content.Intent
import android.graphics.Color
import android.os.Bundle
import android.os.Handler
import android.os.Looper
import android.view.ViewGroup
import android.webkit.WebView
import android.widget.FrameLayout
import androidx.activity.ComponentActivity
import androidx.activity.OnBackPressedCallback
import androidx.activity.result.ActivityResult
import androidx.activity.result.ActivityResultLauncher
import androidx.activity.result.contract.ActivityResultContracts.RequestMultiplePermissions
import androidx.activity.result.contract.ActivityResultContracts.StartActivityForResult
import ai.idto.sdk.IDtoErrorData
import ai.idto.sdk.IDtoTestHooks
import ai.idto.sdk.IDtoTokenCallback
import java.io.File
import java.lang.ref.WeakReference
import java.util.concurrent.atomic.AtomicBoolean

class IDtoActivity : ComponentActivity() {

    private val mainHandler = Handler(Looper.getMainLooper())
    private var entry: SessionEntry? = null
    private var container: FrameLayout? = null
    private var webView: WebView? = null
    private var sheet: SheetLayout? = null
    private var popup: DigiLockerPopup? = null
    private var popupSheet: SheetLayout? = null
    private var presentation: Presentation = Presentation.FullScreen
    private var pendingReportFile: File? = null
    private lateinit var gate: PermissionGate
    private lateinit var permissionLauncher: ActivityResultLauncher<Array<String>>
    private lateinit var shareLauncher: ActivityResultLauncher<Intent>

    private var settled = false
    private var closed = false
    private val readyTimeoutRunnable = Runnable { onReadyTimeout() }
    private val closeFallbackRunnable = Runnable { teardown() }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        entry = SessionRegistry.get(intent.getStringExtra(EXTRA_SESSION_ID))
        val session = entry
        if (session == null) {
            finish()
            return
        }
        activeRef = WeakReference(this)

        val root = FrameLayout(this).apply { setBackgroundColor(Color.TRANSPARENT) }
        container = root
        setContentView(root)

        onBackPressedDispatcher.addCallback(this, object : OnBackPressedCallback(true) {
            override fun handleOnBackPressed() {
                if (popup != null) closePopup() else requestClose()
            }
        })

        gate = PermissionGate(this)
        permissionLauncher = registerForActivityResult(RequestMultiplePermissions()) { result ->
            gate.onPermissionResult(result)
            proceed(session)
        }
        shareLauncher = registerForActivityResult(StartActivityForResult()) { _: ActivityResult ->
            ReportFiles.deleteQuietly(pendingReportFile)
            pendingReportFile = null
        }

        val needed = gate.neededPermissions()
        if (needed.isEmpty()) proceed(session) else permissionLauncher.launch(needed.toTypedArray())
    }

    private fun proceed(session: SessionEntry) {
        if (webView != null || isFinishing) return
        val bridge = IDtoBridge { raw -> mainHandler.post { onBridgeRaw(raw) } }
        val client = IDtoWebViewClient(
            isAllowed = { url -> NavigationPolicy.isAllowedNavigation(url, session.config.baseUrl, session.config.allowedHosts) },
            isDigiLocker = { url -> NavigationPolicy.isDigiLockerUrl(url) },
            onDigiLocker = { url -> openPopup(url) },
            onPreReadyError = { onReadyTimeout() },
            isSettled = { settled },
            onRenderGone = { onRendererGone() },
        )
        val chrome = IDtoWebChromeClient(
            isHeld = { permission -> isGranted(permission) },
            onUnheldCapture = { unheld -> gate.handleUnheldCaptureResources(unheld) },
        )
        val view = IDtoWebViews.create(this, bridge, client, chrome)
        webView = view
        IDtoTestHooks.bindWebView(view)

        presentation = resolvePresentation(session.config.displayMode, session.config.bottomSheet)
        val main = SheetLayout(this, presentation, ::requestClose)
        sheet = main
        main.mountContent(view)
        container?.addView(main, ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.MATCH_PARENT)
        main.animateIn()

        armReadyTimeout(session)
        val injection = WireConfig.buildConfigInjection(session.config)
        val html = Shell.buildShellHtml(session.config.env, injection, session.config.debug)
        val origin = NavigationPolicy.webViewOrigin(session.config.env, session.config.baseUrl)
        view.loadDataWithBaseURL(origin, html, "text/html", "utf-8", null)
    }

    private fun openPopup(url: String) {
        val session = entry ?: return
        if (!NavigationPolicy.isAllowedNavigation(url, session.config.baseUrl, session.config.allowedHosts)) return
        if (popup != null) return
        val created = DigiLockerPopup(
            context = this,
            url = url,
            isAllowed = { u -> NavigationPolicy.isAllowedNavigation(u, session.config.baseUrl, session.config.allowedHosts) },
            isHeld = { permission -> isGranted(permission) },
            onUnheldCapture = { unheld -> gate.handleUnheldCaptureResources(unheld) },
            onOpener = { data, origin -> injectJs(BridgeJs.openerRelayJs(data, origin)) },
            onPopupClose = ::closePopup,
            mainHandler = mainHandler,
        )
        popup = created
        val ps = SheetLayout(this, presentation, ::closePopup)
        popupSheet = ps
        ps.mountContent(created.contentView)
        container?.addView(ps, ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.MATCH_PARENT)
        ps.animateIn()
    }

    private fun closePopup() {
        val ps = popupSheet ?: return
        val active = popup
        popupSheet = null
        popup = null
        injectJs(BridgeJs.popupCloseSignalJs())
        ps.animateOut {
            container?.removeView(ps)
            active?.destroy()
        }
    }

    private fun handleDownload(message: BridgeMessage.Download) {
        ReportFiles.deleteQuietly(pendingReportFile)
        pendingReportFile = null
        val file = ReportFiles.stageReport(this, message.base64, message.filename) ?: return
        pendingReportFile = file
        try {
            shareLauncher.launch(ReportShare.buildChooser(this, file, message.mime, ReportFiles.sanitizeFilename(message.filename)))
        } catch (e: Throwable) {
            ReportFiles.deleteQuietly(file)
            pendingReportFile = null
        }
    }

    private fun isGranted(permission: String): Boolean =
        checkSelfPermission(permission) == android.content.pm.PackageManager.PERMISSION_GRANTED

    private fun armReadyTimeout(session: SessionEntry) {
        settled = false
        mainHandler.postDelayed(readyTimeoutRunnable, session.config.readyTimeoutMs)
    }

    private fun onReadyTimeout() {
        if (settled) return
        settled = true
        mainHandler.removeCallbacks(readyTimeoutRunnable)
        entry?.listener?.onError(IDtoErrorData("init", "network_error", ""))
        teardown()
    }

    private fun onBridgeRaw(raw: String) {
        if (BridgeMessages.isTokenRequest(raw)) {
            respondToTokenRequest()
            return
        }
        val message = BridgeMessages.parse(raw) ?: return
        if (message !is BridgeMessage.Log) {
            settled = true
            mainHandler.removeCallbacks(readyTimeoutRunnable)
        }
        IDtoTestHooks.recordEvent(typeName(message))
        dispatch(message)
    }

    private fun onRendererGone() {
        if (closed) return
        entry?.listener?.onError(IDtoErrorData("init", "unknown_error", ""))
        teardown()
    }

    private fun dispatch(message: BridgeMessage) {
        if (closed) return
        val listener = entry?.listener ?: return
        when (message) {
            is BridgeMessage.Ready -> Unit
            is BridgeMessage.StepComplete -> listener.onStepComplete(message.data)
            is BridgeMessage.WorkflowComplete -> listener.onWorkflowComplete(message.data)
            is BridgeMessage.Abandon -> listener.onAbandon(message.data)
            is BridgeMessage.Error -> {
                listener.onError(message.data)
                if (message.isTerminal()) teardown()
            }
            is BridgeMessage.Log -> IDtoTestHooks.recordLog(message.message)
            is BridgeMessage.Close -> teardown()
            is BridgeMessage.Open -> openPopup(message.url)
            is BridgeMessage.Download -> handleDownload(message)
        }
    }

    private fun respondToTokenRequest() {
        val provider = entry?.tokenProvider
        if (provider == null) {
            injectJs(BridgeJs.tokenErrorJs("no_token_provider"))
            return
        }
        val replied = AtomicBoolean(false)
        val watchdog = Runnable {
            if (replied.compareAndSet(false, true)) injectJs(BridgeJs.tokenErrorJs("token_refresh_failed"))
        }
        mainHandler.postDelayed(watchdog, TOKEN_WATCHDOG_MS)
        val callback = object : IDtoTokenCallback {
            override fun onToken(token: String) {
                mainHandler.post {
                    if (replied.compareAndSet(false, true)) {
                        mainHandler.removeCallbacks(watchdog)
                        injectJs(BridgeJs.tokenReplyJs(token))
                    }
                }
            }

            override fun onError(error: Throwable) {
                mainHandler.post {
                    if (replied.compareAndSet(false, true)) {
                        mainHandler.removeCallbacks(watchdog)
                        injectJs(BridgeJs.tokenErrorJs("token_refresh_failed"))
                    }
                }
            }
        }
        try {
            provider.getToken(callback)
        } catch (e: Throwable) {
            callback.onError(e)
        }
    }

    private fun requestClose() {
        injectJs(BridgeJs.CLOSE_JS)
        mainHandler.removeCallbacks(closeFallbackRunnable)
        mainHandler.postDelayed(closeFallbackRunnable, Defaults.CLOSE_FALLBACK_MS)
    }

    private fun injectJs(js: String) {
        webView?.evaluateJavascript(js, null)
    }

    private fun teardown() {
        if (isFinishing) return
        mainHandler.removeCallbacks(readyTimeoutRunnable)
        mainHandler.removeCallbacks(closeFallbackRunnable)
        fireClose()
        finish()
    }

    private fun fireClose() {
        if (closed) return
        closed = true
        entry?.listener?.onClose()
        SessionRegistry.remove(entry?.id)
    }

    override fun onDestroy() {
        mainHandler.removeCallbacks(readyTimeoutRunnable)
        mainHandler.removeCallbacks(closeFallbackRunnable)
        IDtoTestHooks.unbindWebView()
        popup?.destroy()
        popup = null
        popupSheet = null
        webView?.let {
            (it.parent as? ViewGroup)?.removeView(it)
            it.destroy()
        }
        webView = null
        ReportFiles.deleteQuietly(pendingReportFile)
        pendingReportFile = null
        fireClose()
        if (activeRef?.get() === this) activeRef = null
        super.onDestroy()
    }

    private fun typeName(message: BridgeMessage): String = when (message) {
        is BridgeMessage.Ready -> "ready"
        is BridgeMessage.StepComplete -> "stepComplete"
        is BridgeMessage.WorkflowComplete -> "workflowComplete"
        is BridgeMessage.Abandon -> "abandon"
        is BridgeMessage.Error -> "error"
        is BridgeMessage.Log -> "log"
        is BridgeMessage.Close -> "close"
        is BridgeMessage.Open -> "open"
        is BridgeMessage.Download -> "download"
    }

    companion object {
        const val EXTRA_SESSION_ID = "ai.idto.sdk.SESSION_ID"
        private const val TOKEN_WATCHDOG_MS = 30000L

        @Volatile
        private var activeRef: WeakReference<IDtoActivity>? = null

        fun requestCloseActive() {
            val activity = activeRef?.get() ?: return
            activity.runOnUiThread { activity.requestClose() }
        }
    }
}
