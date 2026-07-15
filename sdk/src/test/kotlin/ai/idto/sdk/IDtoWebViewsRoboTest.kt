package ai.idto.sdk

import android.Manifest
import android.net.Uri
import android.webkit.PermissionRequest
import android.webkit.WebResourceRequest
import android.webkit.WebSettings
import ai.idto.sdk.internal.IDtoBridge
import ai.idto.sdk.internal.IDtoWebChromeClient
import ai.idto.sdk.internal.IDtoWebViewClient
import ai.idto.sdk.internal.IDtoWebViews
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.RuntimeEnvironment
import org.robolectric.Shadows.shadowOf

@RunWith(RobolectricTestRunner::class)
class IDtoWebViewsRoboTest {

    private val context = RuntimeEnvironment.getApplication()

    private fun webView() = IDtoWebViews.create(
        context,
        IDtoBridge { },
        IDtoWebViewClient({ true }, { false }, { }, { }, { true }),
        IDtoWebChromeClient({ false }),
    )

    @Test
    fun `settings applied per plan`() {
        val s = webView().settings
        assertTrue(s.javaScriptEnabled)
        assertTrue(s.domStorageEnabled)
        assertFalse(s.mediaPlaybackRequiresUserGesture)
        assertFalse(s.javaScriptCanOpenWindowsAutomatically)
        assertFalse(s.allowFileAccess)
        assertFalse(s.allowContentAccess)
        assertEquals(WebSettings.MIXED_CONTENT_NEVER_ALLOW, s.mixedContentMode)
    }

    @Test
    fun `bridge attached under IDtoAndroid name`() {
        val wv = webView()
        assertNotNull(shadowOf(wv).getJavascriptInterface(IDtoWebViews.BRIDGE_NAME))
    }

    @Test
    fun `allowed url loads`() {
        val client = IDtoWebViewClient({ true }, { false }, { }, { }, { true })
        assertFalse(client.shouldOverrideUrlLoading(webView(), request("https://prod.idto.ai/x")))
    }

    @Test
    fun `blocked url is cancelled`() {
        val client = IDtoWebViewClient({ false }, { false }, { }, { }, { true })
        assertTrue(client.shouldOverrideUrlLoading(webView(), request("https://evil.com")))
    }

    @Test
    fun `digilocker main frame routes to popup handler`() {
        var routed: String? = null
        val client = IDtoWebViewClient({ true }, { true }, { routed = it }, { }, { true })
        assertTrue(client.shouldOverrideUrlLoading(webView(), request("https://digilocker.idto.ai/x")))
        assertEquals("https://digilocker.idto.ai/x", routed)
    }

    @Test
    fun `pre ready main frame error reports init error`() {
        var reported = false
        val client = IDtoWebViewClient({ true }, { false }, { }, { reported = true }, { false })
        client.onReceivedError(webView(), request("https://prod.idto.ai", mainFrame = true), null)
        assertTrue(reported)
    }

    @Test
    fun `subframe or post ready error ignored`() {
        var reported = false
        val subframe = IDtoWebViewClient({ true }, { false }, { }, { reported = true }, { false })
        subframe.onReceivedError(webView(), request("https://prod.idto.ai", mainFrame = false), null)
        assertFalse(reported)

        val postReady = IDtoWebViewClient({ true }, { false }, { }, { reported = true }, { true })
        postReady.onReceivedError(webView(), request("https://prod.idto.ai", mainFrame = true), null)
        assertFalse(reported)
    }

    @Test
    fun `renderer gone invokes handler and is consumed`() {
        var gone = false
        val client = IDtoWebViewClient({ true }, { false }, { }, { }, { true }, { gone = true })
        val handled = client.onRenderProcessGone(webView(), FakeRenderGone())
        assertTrue(handled)
        assertTrue(gone)
    }

    @Test
    fun `video capture granted only when camera held`() {
        assertEquals(
            listOf(PermissionRequest.RESOURCE_VIDEO_CAPTURE),
            grantedFor(arrayOf(PermissionRequest.RESOURCE_VIDEO_CAPTURE)) { it == Manifest.permission.CAMERA },
        )
        assertTrue(deniedFor(arrayOf(PermissionRequest.RESOURCE_VIDEO_CAPTURE)) { false })
    }

    @Test
    fun `audio capture granted only when mic held`() {
        assertEquals(
            listOf(PermissionRequest.RESOURCE_AUDIO_CAPTURE),
            grantedFor(arrayOf(PermissionRequest.RESOURCE_AUDIO_CAPTURE)) { it == Manifest.permission.RECORD_AUDIO },
        )
    }

    @Test
    fun `unrelated resource granted and nothing held denies capture`() {
        assertEquals(
            listOf(PermissionRequest.RESOURCE_PROTECTED_MEDIA_ID),
            grantedFor(arrayOf(PermissionRequest.RESOURCE_PROTECTED_MEDIA_ID)) { false },
        )
        assertTrue(
            deniedFor(
                arrayOf(PermissionRequest.RESOURCE_VIDEO_CAPTURE, PermissionRequest.RESOURCE_AUDIO_CAPTURE),
            ) { false },
        )
    }

    private fun grantedFor(resources: Array<String>, held: (String) -> Boolean): List<String> {
        val req = FakePermissionRequest(resources)
        IDtoWebChromeClient(held).onPermissionRequest(req)
        return req.granted?.toList() ?: emptyList()
    }

    private fun deniedFor(resources: Array<String>, held: (String) -> Boolean): Boolean {
        val req = FakePermissionRequest(resources)
        IDtoWebChromeClient(held).onPermissionRequest(req)
        return req.denied
    }

    private fun request(url: String, mainFrame: Boolean = true): WebResourceRequest =
        object : WebResourceRequest {
            override fun getUrl(): Uri = Uri.parse(url)
            override fun isForMainFrame(): Boolean = mainFrame
            override fun isRedirect(): Boolean = false
            override fun hasGesture(): Boolean = false
            override fun getMethod(): String = "GET"
            override fun getRequestHeaders(): Map<String, String> = emptyMap()
        }

    private class FakeRenderGone : android.webkit.RenderProcessGoneDetail() {
        override fun didCrash(): Boolean = true
        override fun rendererPriorityAtExit(): Int = 0
    }

    private class FakePermissionRequest(private val res: Array<String>) : PermissionRequest() {
        var granted: Array<out String>? = null
        var denied: Boolean = false
        override fun getOrigin(): Uri = Uri.parse("https://prod.idto.ai")
        override fun getResources(): Array<String> = res
        override fun grant(resources: Array<out String>) { granted = resources }
        override fun deny() { denied = true }
    }
}
