package ai.idto.sdk

import android.content.Context
import android.graphics.Color
import android.os.Looper
import android.widget.LinearLayout
import android.widget.TextView
import ai.idto.sdk.landing.IDtoLandingConfig
import ai.idto.sdk.landing.IDtoLandingCopyOverride
import ai.idto.sdk.landing.IDtoLandingCtaOverride
import ai.idto.sdk.landing.IDtoLandingListener
import ai.idto.sdk.landing.IDtoLandingView
import ai.idto.sdk.landing.LandingDefaults
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
class IDtoLandingViewRoboTest {

    private val context: Context = RuntimeEnvironment.getApplication()

    private class FakeProvider : IDtoTokenProvider {
        var calls = 0
        var callback: IDtoTokenCallback? = null
        override fun getToken(callback: IDtoTokenCallback) {
            calls++
            this.callback = callback
        }
    }

    private class CapturingOpener {
        var count = 0
        var config: IDtoConfig? = null
        var listener: IDtoEventListener? = null
        var tokenProvider: IDtoTokenProvider? = null
        val fn: (Context, IDtoConfig, IDtoEventListener, IDtoTokenProvider?) -> Unit =
            { _, c, l, tp -> count++; config = c; listener = l; tokenProvider = tp }
    }

    private fun view(config: IDtoLandingConfig, opener: CapturingOpener = CapturingOpener()): Pair<IDtoLandingView, CapturingOpener> {
        val v = IDtoLandingView(context)
        v.opener = opener.fn
        v.configure(config)
        return v to opener
    }

    private fun cfg(
        provider: IDtoTokenProvider = FakeProvider(),
        brandColor: String? = null,
        copy: IDtoLandingCopyOverride? = null,
        logoUrl: String? = null,
        businessName: String? = null,
    ) = IDtoLandingConfig.Builder("wf-1", provider)
        .apply {
            brandColor?.let { brandColor(it) }
            copy?.let { copy(it) }
            logoUrl?.let { logoUrl(it) }
            businessName?.let { businessName(it) }
        }
        .build()

    private fun idle() = shadowOf(Looper.getMainLooper()).idle()

    @Test
    fun `renders default copy hero steps cta trust footer`() {
        val (v, _) = view(cfg())
        val d = LandingDefaults.DEFAULT_COPY
        assertEquals(d.heroTitle, v.heroTitleView.text.toString())
        assertEquals("A quick, secure KYC check — done in under a minute.", v.subtitleView.text.toString())
        assertEquals(android.view.View.GONE, v.noteView.visibility)
        assertEquals(3, v.stepsContainer.childCount)
        val firstBadge = (v.stepsContainer.getChildAt(0) as LinearLayout).getChildAt(0) as TextView
        assertEquals("1", firstBadge.text.toString())
        assertTrue(v.ctaLabelView.text.toString().contains(d.cta.idle))
        assertEquals(android.view.View.GONE, v.trustContainer.visibility)
        assertEquals(d.footer, v.footerView.text.toString())
    }

    @Test
    fun `business name interpolated into subtitle`() {
        val (v, _) = view(cfg(businessName = "DriveX"))
        assertEquals(
            "A quick, secure DriveX KYC check — done in under a minute.",
            v.subtitleView.text.toString(),
        )
    }

    @Test
    fun `cta arrow uses single space`() {
        val (v, _) = view(cfg())
        assertEquals("Start verification →", v.ctaLabelView.text.toString())
    }

    @Test
    fun `custom brand color drives cta background and contrast text`() {
        val (v, _) = view(cfg(brandColor = "#7C22C4"))
        assertEquals(Color.parseColor("#7C22C4"), v.brandColorInt)
        assertEquals(Color.parseColor(LandingDefaults.contrastText("#7C22C4")), v.ctaTextColorInt)
        assertEquals(v.brandColorInt, v.heroTitleView.currentTextColor)
        assertEquals(v.ctaTextColorInt, v.ctaLabelView.currentTextColor)
    }

    @Test
    fun `custom copy override applied`() {
        val override = IDtoLandingCopyOverride(
            heroTitle = "Verify with CredResolve",
            cta = IDtoLandingCtaOverride(idle = "Begin"),
        )
        val (v, _) = view(cfg(copy = override))
        assertEquals("Verify with CredResolve", v.heroTitleView.text.toString())
        assertTrue(v.ctaLabelView.text.toString().contains("Begin"))
    }

    @Test
    fun `cta tap calls provider then opens sheet with palette and logo`() {
        val provider = FakeProvider()
        val (v, opener) = view(cfg(provider = provider, brandColor = "#7C22C4", logoUrl = "data:image/png;base64,AAA"))
        v.ctaButton.performClick()
        assertEquals(1, provider.calls)
        assertEquals(0, opener.count)
        provider.callback!!.onToken("client-token-xyz")
        idle()
        assertEquals(1, opener.count)
        val opened = opener.config!!
        assertEquals("client-token-xyz", opened.clientToken)
        assertEquals(IDtoDisplayMode.BOTTOM_SHEET, opened.displayMode)
        assertEquals("90%", opened.bottomSheet!!.minHeight)
        assertEquals("data:image/png;base64,AAA", opened.logo)
        assertEquals("#7C22C4", opened.colors!!.primary)
        assertNotNull(opener.tokenProvider)
    }

    @Test
    fun `provider failure returns to idle label and does not open`() {
        val provider = FakeProvider()
        val (v, opener) = view(cfg(provider = provider))
        v.ctaButton.performClick()
        provider.callback!!.onError(RuntimeException("boom"))
        idle()
        assertEquals(0, opener.count)
        assertTrue(v.ctaLabelView.text.toString().contains(LandingDefaults.DEFAULT_COPY.cta.idle))
    }

    @Test
    fun `double tap during loading calls provider once`() {
        val provider = FakeProvider()
        val (v, _) = view(cfg(provider = provider))
        v.ctaButton.performClick()
        v.ctaButton.performClick()
        assertEquals(1, provider.calls)
    }

    @Test
    fun `listener relays complete close abandon`() {
        var completed = false
        var dismissed = false
        var abandoned = false
        val provider = FakeProvider()
        val listener = object : IDtoLandingListener {
            override fun onComplete(data: IDtoWorkflowCompleteData) { completed = true }
            override fun onDismiss() { dismissed = true }
            override fun onAbandon(data: IDtoAbandonData) { abandoned = true }
        }
        val (v, opener) = view(cfg(provider = provider))
        v.setListener(listener)
        v.ctaButton.performClick()
        provider.callback!!.onToken("t")
        idle()
        val relay = opener.listener!!
        relay.onWorkflowComplete(IDtoWorkflowCompleteData.fromJson(null))
        assertTrue(completed)
        assertTrue(v.ctaLabelView.text.toString().contains(LandingDefaults.DEFAULT_COPY.cta.done))
        relay.onClose()
        assertTrue(dismissed)
        relay.onAbandon(IDtoAbandonData.fromJson(null))
        assertTrue(abandoned)
    }

    @Test
    fun `no logo hides logo view`() {
        val (v, _) = view(cfg())
        assertEquals(android.view.View.GONE, v.logoView.visibility)
    }
}
