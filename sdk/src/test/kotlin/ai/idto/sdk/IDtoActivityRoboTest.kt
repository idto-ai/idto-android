package ai.idto.sdk

import android.app.Activity
import android.content.Intent
import android.os.Looper
import android.view.View
import android.view.ViewGroup
import android.webkit.WebView
import ai.idto.sdk.internal.IDtoActivity
import ai.idto.sdk.internal.IDtoBridge
import ai.idto.sdk.internal.IDtoWebViews
import ai.idto.sdk.internal.SessionRegistry
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertNull
import org.junit.Assert.assertSame
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.Robolectric
import org.robolectric.RobolectricTestRunner
import org.robolectric.RuntimeEnvironment
import org.robolectric.Shadows.shadowOf
import org.robolectric.android.controller.ActivityController
import java.time.Duration

@RunWith(RobolectricTestRunner::class)
class IDtoActivityRoboTest {

    private val app = RuntimeEnvironment.getApplication()
    private val camera = android.Manifest.permission.CAMERA
    private val mic = android.Manifest.permission.RECORD_AUDIO
    private var controller: ActivityController<IDtoActivity>? = null

    @Before
    fun setUp() {
        SessionRegistry.clear()
        IDtoTestHooks.reset()
        shadowOf(app).grantPermissions(camera, mic)
        val field = androidx.core.content.FileProvider::class.java.getDeclaredField("sCache")
        field.isAccessible = true
        (field.get(null) as MutableMap<*, *>).clear()
    }

    @After
    fun tearDown() {
        controller?.close()
        SessionRegistry.clear()
    }

    private fun config(build: IDtoConfig.Builder.() -> Unit = {}) =
        IDtoConfig.Builder("token", "wft").apply(build).build()

    private fun launch(
        config: IDtoConfig = config(),
        listener: IDtoEventListener,
        provider: IDtoTokenProvider? = null,
    ): IDtoActivity {
        val entry = SessionRegistry.register(config, listener, provider)!!
        val intent = Intent(app, IDtoActivity::class.java).putExtra(IDtoActivity.EXTRA_SESSION_ID, entry.id)
        val c = Robolectric.buildActivity(IDtoActivity::class.java, intent).setup()
        controller = c
        return c.get()
    }

    private fun idle() = shadowOf(Looper.getMainLooper()).idle()
    private fun idleFor(ms: Long) = shadowOf(Looper.getMainLooper()).idleFor(Duration.ofMillis(ms))

    private fun webViewOf(a: Activity): WebView =
        firstWebView(a.findViewById(android.R.id.content))!!

    private fun firstWebView(v: View): WebView? {
        if (v is WebView) return v
        if (v is ViewGroup) for (i in 0 until v.childCount) firstWebView(v.getChildAt(i))?.let { return it }
        return null
    }

    private fun bridgeOf(a: Activity): IDtoBridge =
        shadowOf(webViewOf(a)).getJavascriptInterface(IDtoWebViews.BRIDGE_NAME) as IDtoBridge

    private fun send(a: Activity, raw: String) {
        bridgeOf(a).postMessage(raw)
        idle()
    }

    @Test
    fun `absent session finishes immediately with no webview`() {
        val intent = Intent(app, IDtoActivity::class.java).putExtra(IDtoActivity.EXTRA_SESSION_ID, "missing")
        val c = Robolectric.buildActivity(IDtoActivity::class.java, intent).setup()
        controller = c
        assertTrue(c.get().isFinishing)
        assertNull(firstWebView(c.get().findViewById(android.R.id.content)))
    }

    @Test
    fun `ready disarms timeout and session stays alive`() {
        val l = RecordingListener()
        val a = launch(listener = l)
        send(a, """{"type":"ready"}""")
        idleFor(60000)
        assertFalse(a.isFinishing)
        assertEquals(0, l.errors.size)
        assertEquals(0, l.closeCount)
    }

    @Test
    fun `ready timeout surfaces init network error then closes`() {
        val l = RecordingListener()
        val a = launch(config { readyTimeoutMs(5000) }, l)
        idleFor(5000)
        assertEquals(1, l.errors.size)
        assertEquals("init", l.errors[0].step)
        assertEquals("network_error", l.errors[0].error)
        assertEquals(1, l.closeCount)
        assertTrue(a.isFinishing)
    }

    @Test
    fun `messages dispatch to listener on the main thread`() {
        val l = RecordingListener()
        val a = launch(listener = l)
        send(a, """{"type":"stepComplete","payload":{"step":"pan","session_token":"t"}}""")
        assertEquals("pan", l.steps[0].step)
        assertSame(Looper.getMainLooper().thread, l.threads[0])
    }

    @Test
    fun `workflowComplete is non terminal and close ends the session once`() {
        val l = RecordingListener()
        val a = launch(listener = l)
        val id = intentSessionId(a)
        send(a, """{"type":"workflowComplete","payload":{"session_token":"t"}}""")
        assertEquals(1, l.workflowCount)
        assertFalse(a.isFinishing)
        send(a, """{"type":"close"}""")
        assertEquals(1, l.closeCount)
        assertTrue(a.isFinishing)
        assertNull(SessionRegistry.get(id))
        assertFalse(IDto.isOpen())
    }

    @Test
    fun `back press injects close js and falls back after 800ms`() {
        val l = RecordingListener()
        val a = launch(listener = l)
        val wv = webViewOf(a)
        a.onBackPressedDispatcher.onBackPressed()
        assertEquals(BridgeJsClose, shadowOf(wv).lastEvaluatedJavascript)
        assertFalse(a.isFinishing)
        idleFor(800)
        assertTrue(a.isFinishing)
        assertEquals(1, l.closeCount)
    }

    @Test
    fun `terminal error tears down non terminal error keeps activity`() {
        val l1 = RecordingListener()
        val a1 = launch(listener = l1)
        send(a1, """{"type":"error","payload":{"step":"init","error":"network_error"}}""")
        assertEquals(1, l1.errors.size)
        assertTrue(a1.isFinishing)
        assertEquals(1, l1.closeCount)

        controller?.close()
        SessionRegistry.clear()

        val l2 = RecordingListener()
        val a2 = launch(listener = l2)
        send(a2, """{"type":"error","payload":{"step":"face","error":"session_expired"}}""")
        assertEquals(1, l2.errors.size)
        assertFalse(a2.isFinishing)
    }

    @Test
    fun `token request with provider replies with token`() {
        val l = RecordingListener()
        val a = launch(listener = l, provider = { cb -> cb.onToken("abc123") })
        send(a, """{"type":"ready"}""")
        send(a, """{"type":"idto:getToken"}""")
        val js = shadowOf(webViewOf(a)).lastEvaluatedJavascript
        assertTrue(js.contains("idto:getToken:response"))
        assertTrue(js.contains("abc123"))
    }

    @Test
    fun `token request provider error replies token_refresh_failed`() {
        val l = RecordingListener()
        val a = launch(listener = l, provider = { cb -> cb.onError(RuntimeException("boom")) })
        send(a, """{"type":"ready"}""")
        send(a, """{"type":"idto:getToken"}""")
        assertTrue(shadowOf(webViewOf(a)).lastEvaluatedJavascript.contains("token_refresh_failed"))
    }

    @Test
    fun `token request without provider replies no_token_provider`() {
        val l = RecordingListener()
        val a = launch(listener = l, provider = null)
        send(a, """{"type":"ready"}""")
        send(a, """{"type":"idto:getToken"}""")
        assertTrue(shadowOf(webViewOf(a)).lastEvaluatedJavascript.contains("no_token_provider"))
    }

    @Test
    fun `token watchdog replies token_refresh_failed after 30s`() {
        val l = RecordingListener()
        val a = launch(listener = l, provider = { })
        send(a, """{"type":"ready"}""")
        send(a, """{"type":"idto:getToken"}""")
        idleFor(30000)
        assertTrue(shadowOf(webViewOf(a)).lastEvaluatedJavascript.contains("token_refresh_failed"))
    }

    @Test
    fun `renderer crash surfaces error and closes the session`() {
        val l = RecordingListener()
        val a = launch(listener = l)
        send(a, """{"type":"ready"}""")
        val wv = webViewOf(a)
        val client = wv.webViewClient as ai.idto.sdk.internal.IDtoWebViewClient
        val handled = client.onRenderProcessGone(wv, renderGone())
        idle()
        assertTrue(handled)
        assertEquals(1, l.errors.size)
        assertEquals("init", l.errors[0].step)
        assertTrue(a.isFinishing)
        assertEquals(1, l.closeCount)
    }

    @Test
    fun `dispatch after close is dropped`() {
        val l = RecordingListener()
        val a = launch(listener = l)
        send(a, """{"type":"close"}""")
        assertEquals(1, l.closeCount)
        send(a, """{"type":"stepComplete","payload":{"step":"pan","session_token":"t"}}""")
        assertEquals(0, l.steps.size)
    }

    @Test
    fun `open reports error when the host cannot start the activity`() {
        SessionRegistry.clear()
        val l = RecordingListener()
        IDto.open(ThrowingContext(app), config(), l)
        assertEquals(1, l.errors.size)
        assertEquals("init", l.errors[0].step)
        assertFalse(IDto.isOpen())
    }

    private fun renderGone(): android.webkit.RenderProcessGoneDetail =
        object : android.webkit.RenderProcessGoneDetail() {
            override fun didCrash(): Boolean = true
            override fun rendererPriorityAtExit(): Int = 0
        }

    private class ThrowingContext(base: android.content.Context) : android.content.ContextWrapper(base) {
        override fun startActivity(intent: Intent?) {
            throw android.content.ActivityNotFoundException("no activity")
        }
    }

    @Test
    fun `destroy releases the webview`() {
        val l = RecordingListener()
        val a = launch(listener = l)
        val wv = webViewOf(a)
        controller?.destroy()
        assertTrue(shadowOf(wv).wasDestroyCalled())
    }

    @Test
    fun `open from application context adds new task flag`() {
        SessionRegistry.clear()
        IDto.open(app, config(), RecordingListener())
        val started = shadowOf(app).nextStartedActivity
        assertTrue(started.flags and Intent.FLAG_ACTIVITY_NEW_TASK != 0)
    }

    @Test
    fun `open from activity context omits new task flag`() {
        SessionRegistry.clear()
        val host = Robolectric.buildActivity(Activity::class.java).setup().get()
        IDto.open(host, config(), RecordingListener())
        val started = shadowOf(host).nextStartedActivity
        assertEquals(0, started.flags and Intent.FLAG_ACTIVITY_NEW_TASK)
    }

    @Test
    fun `second open while active reports session_active`() {
        SessionRegistry.register(config(), RecordingListener(), null)
        val l = RecordingListener()
        IDto.open(app, config(), l)
        assertEquals("init", l.errors[0].step)
        assertEquals("session_active", l.errors[0].error)
        assertNull(shadowOf(app).nextStartedActivity)
    }

    @Test
    fun `IDto close injects close js and clears open state`() {
        val l = RecordingListener()
        val a = launch(listener = l)
        val wv = webViewOf(a)
        IDto.close()
        idle()
        assertEquals(BridgeJsClose, shadowOf(wv).lastEvaluatedJavascript)
        send(a, """{"type":"close"}""")
        assertFalse(IDto.isOpen())
    }

    @Test
    fun `open message presents popup only for allow listed url`() {
        val a = launch(listener = RecordingListener())
        send(a, """{"type":"open","payload":{"url":"https://digilocker.idto.ai/oauth"}}""")
        assertNotNull(popupWebView(a))
    }

    @Test
    fun `open message with blocked url opens no popup`() {
        val a = launch(listener = RecordingListener())
        send(a, """{"type":"open","payload":{"url":"https://evil.com/oauth"}}""")
        assertNull(popupWebView(a))
    }

    @Test
    fun `popup header title is always DigiLocker`() {
        val a = launch(listener = RecordingListener())
        send(a, """{"type":"open","payload":{"url":"https://digilocker.idto.ai/oauth"}}""")
        assertNotNull(firstTextEquals(a.findViewById(android.R.id.content), "DigiLocker"))
    }

    @Test
    fun `opener relays into main webview and popup stays mounted`() {
        val a = launch(listener = RecordingListener())
        send(a, """{"type":"open","payload":{"url":"https://digilocker.idto.ai/oauth"}}""")
        val popup = popupWebView(a)!!
        popupBridgeOf(popup).postMessage("""{"type":"opener","payload":{"data":{"code":"xyz"},"origin":"https://demo.idto.ai"}}""")
        idle()
        val mainJs = shadowOf(webViewOf(a)).lastEvaluatedJavascript
        assertTrue(mainJs.contains("MessageEvent"))
        assertTrue(mainJs.contains("xyz"))
        assertNotNull(popupWebView(a))
        assertFalse(shadowOf(popup).wasDestroyCalled())
    }

    @Test
    fun `back press dismisses popup first then closes session`() {
        val a = launch(listener = RecordingListener())
        send(a, """{"type":"open","payload":{"url":"https://digilocker.idto.ai/oauth"}}""")
        val popup = popupWebView(a)!!
        a.onBackPressedDispatcher.onBackPressed()
        idleFor(200)
        assertTrue(shadowOf(popup).wasDestroyCalled())
        assertNull(popupWebView(a))
        assertFalse(a.isFinishing)

        a.onBackPressedDispatcher.onBackPressed()
        assertEquals(BridgeJsClose, shadowOf(webViewOf(a)).lastEvaluatedJavascript)
    }

    @Test
    fun `closing popup signals main webview`() {
        val a = launch(listener = RecordingListener())
        send(a, """{"type":"open","payload":{"url":"https://digilocker.idto.ai/oauth"}}""")
        val popup = popupWebView(a)!!
        popupBridgeOf(popup).postMessage("""{"type":"popupClose","payload":{}}""")
        idleFor(200)
        assertTrue(shadowOf(webViewOf(a)).lastEvaluatedJavascript.contains("visibilitychange"))
        assertNull(popupWebView(a))
    }

    @Test
    fun `destroy releases both main and popup webviews`() {
        val a = launch(listener = RecordingListener())
        send(a, """{"type":"open","payload":{"url":"https://digilocker.idto.ai/oauth"}}""")
        val main = webViewOf(a)
        val popup = popupWebView(a)!!
        controller?.destroy()
        assertTrue(shadowOf(main).wasDestroyCalled())
        assertTrue(shadowOf(popup).wasDestroyCalled())
    }

    @Test
    fun `download message stages a report and launches a chooser`() {
        val a = launch(listener = RecordingListener())
        val b64 = android.util.Base64.encodeToString("pdfbytes".toByteArray(), android.util.Base64.NO_WRAP)
        send(a, """{"type":"download","payload":{"filename":"r.pdf","mime":"application/pdf","base64":"$b64"}}""")
        val started = shadowOf(a).nextStartedActivityForResult
        assertNotNull(started)
        assertEquals(Intent.ACTION_CHOOSER, started.intent.action)
    }

    @Test
    fun `popup mirrors sheet shape when session is bottom sheet`() {
        val cfg = config { displayMode(IDtoDisplayMode.BOTTOM_SHEET) }
        val a = launch(cfg, RecordingListener())
        send(a, """{"type":"open","payload":{"url":"https://digilocker.idto.ai/oauth"}}""")
        val sheets = sheetLayouts(a.findViewById(android.R.id.content))
        assertEquals(2, sheets.size)
        assertTrue(sheets.all { it.presentation is ai.idto.sdk.internal.Presentation.Sheet })
    }

    private fun allWebViews(v: View, out: MutableList<WebView>) {
        if (v is WebView) out.add(v)
        if (v is ViewGroup) for (i in 0 until v.childCount) allWebViews(v.getChildAt(i), out)
    }

    private fun popupWebView(a: Activity): WebView? {
        val out = mutableListOf<WebView>()
        allWebViews(a.findViewById(android.R.id.content), out)
        return out.firstOrNull { shadowOf(it).lastLoadedUrl != null }
    }

    private fun popupBridgeOf(wv: WebView): IDtoBridge =
        shadowOf(wv).getJavascriptInterface(IDtoWebViews.BRIDGE_NAME) as IDtoBridge

    private fun sheetLayouts(v: View, out: MutableList<ai.idto.sdk.internal.SheetLayout> = mutableListOf()): List<ai.idto.sdk.internal.SheetLayout> {
        if (v is ai.idto.sdk.internal.SheetLayout) out.add(v)
        if (v is ViewGroup) for (i in 0 until v.childCount) sheetLayouts(v.getChildAt(i), out)
        return out
    }

    private fun firstTextEquals(v: View, text: String): View? {
        if (v is android.widget.TextView && v.text == text) return v
        if (v is ViewGroup) for (i in 0 until v.childCount) firstTextEquals(v.getChildAt(i), text)?.let { return it }
        return null
    }

    private fun intentSessionId(a: Activity): String? =
        a.intent.getStringExtra(IDtoActivity.EXTRA_SESSION_ID)

    private companion object {
        const val BridgeJsClose = "window.IDtoSDK && window.IDtoSDK.close(); true;"
    }

    private class RecordingListener : IDtoEventListener {
        val steps = mutableListOf<IDtoStepCompleteData>()
        val errors = mutableListOf<IDtoErrorData>()
        val threads = mutableListOf<Thread>()
        var workflowCount = 0
        var closeCount = 0

        override fun onStepComplete(data: IDtoStepCompleteData) {
            steps.add(data); threads.add(Thread.currentThread())
        }

        override fun onWorkflowComplete(data: IDtoWorkflowCompleteData) {
            workflowCount++
        }

        override fun onError(data: IDtoErrorData) {
            errors.add(data)
        }

        override fun onClose() {
            closeCount++
        }
    }
}
