package ai.idto.sdk

import android.net.Uri
import android.os.Handler
import android.os.Looper
import android.view.View
import android.view.ViewGroup
import android.webkit.WebResourceRequest
import android.webkit.WebSettings
import android.webkit.WebView
import android.widget.TextView
import ai.idto.sdk.internal.DigiLockerPopup
import ai.idto.sdk.internal.IDtoBridge
import ai.idto.sdk.internal.IDtoWebViews
import ai.idto.sdk.internal.Presentation
import ai.idto.sdk.internal.SheetLayout
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.RuntimeEnvironment
import org.robolectric.Shadows.shadowOf

@RunWith(RobolectricTestRunner::class)
class DigiLockerPopupRoboTest {

    private val context = RuntimeEnvironment.getApplication()

    private fun popup(
        onOpener: (Any?, String) -> Unit = { _, _ -> },
        onClose: () -> Unit = {},
        allowed: (String) -> Boolean = { true },
    ) = DigiLockerPopup(
        context,
        "https://digilocker.idto.ai/x",
        isAllowed = allowed,
        isHeld = { false },
        onUnheldCapture = { },
        onOpener = onOpener,
        onPopupClose = onClose,
        mainHandler = Handler(Looper.getMainLooper()),
    )

    private fun bridgeOf(p: DigiLockerPopup): IDtoBridge =
        shadowOf(p.webView).getJavascriptInterface(IDtoWebViews.BRIDGE_NAME) as IDtoBridge

    private fun send(p: DigiLockerPopup, raw: String) {
        bridgeOf(p).postMessage(raw)
        shadowOf(Looper.getMainLooper()).idle()
    }

    @Test
    fun `popup webview gets same settings as main`() {
        val s = popup().webView.settings
        assertTrue(s.javaScriptEnabled)
        assertTrue(s.domStorageEnabled)
        assertFalse(s.mediaPlaybackRequiresUserGesture)
        assertEquals(WebSettings.MIXED_CONTENT_NEVER_ALLOW, s.mixedContentMode)
    }

    @Test
    fun `header shows DigiLocker title`() {
        val p = popup()
        assertEquals("DigiLocker", firstTextEquals(p.contentView, "DigiLocker")?.text)
    }

    @Test
    fun `opener message relays data and origin and keeps popup mounted`() {
        var data: Any? = null
        var origin: String? = null
        val p = popup(onOpener = { d, o -> data = d; origin = o })
        send(p, """{"type":"opener","payload":{"data":{"code":"abc"},"origin":"https://demo.idto.ai"}}""")
        assertEquals("https://demo.idto.ai", origin)
        assertTrue(data.toString().contains("abc"))
        assertFalse(shadowOf(p.webView).wasDestroyCalled())
    }

    @Test
    fun `popupClose message triggers close`() {
        var closed = 0
        val p = popup(onClose = { closed++ })
        send(p, """{"type":"popupClose","payload":{}}""")
        assertEquals(1, closed)
    }

    @Test
    fun `non allow listed navigation inside popup is blocked`() {
        val p = popup(allowed = { it.startsWith("https://digilocker.idto.ai") })
        val client = shadowOf(p.webView).webViewClient
        assertTrue(client.shouldOverrideUrlLoading(p.webView, request("https://evil.com")))
        assertFalse(client.shouldOverrideUrlLoading(p.webView, request("https://digilocker.idto.ai/y")))
    }

    @Test
    fun `popup content mirrors sheet and full screen shapes`() {
        val p = popup()
        val sheet = SheetLayout(context, Presentation.Sheet("90%")) { }
        sheet.mountContent(p.contentView)
        assertSameParent(sheet.card, p.contentView)

        val q = popup()
        val full = SheetLayout(context, Presentation.FullScreen) { }
        full.mountContent(q.contentView)
        assertSameParent(full.card, q.contentView)
    }

    @Test
    fun `destroy releases popup webview`() {
        val p = popup()
        p.destroy()
        assertTrue(shadowOf(p.webView).wasDestroyCalled())
    }

    private fun assertSameParent(parent: ViewGroup, child: View) =
        org.junit.Assert.assertSame(parent, child.parent)

    private fun request(url: String): WebResourceRequest = object : WebResourceRequest {
        override fun getUrl(): Uri = Uri.parse(url)
        override fun isForMainFrame(): Boolean = true
        override fun isRedirect(): Boolean = false
        override fun hasGesture(): Boolean = false
        override fun getMethod(): String = "GET"
        override fun getRequestHeaders(): Map<String, String> = emptyMap()
    }

    private fun firstTextEquals(v: View, text: String): TextView? {
        if (v is TextView && v.text == text) return v
        if (v is ViewGroup) for (i in 0 until v.childCount) firstTextEquals(v.getChildAt(i), text)?.let { return it }
        return null
    }
}
