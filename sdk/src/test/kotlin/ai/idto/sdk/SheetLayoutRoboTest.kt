package ai.idto.sdk

import android.graphics.Color
import android.graphics.drawable.ColorDrawable
import android.os.Looper
import android.view.View
import android.view.View.MeasureSpec
import android.webkit.WebView
import androidx.core.graphics.Insets
import ai.idto.sdk.internal.Presentation
import ai.idto.sdk.internal.SheetLayout
import com.google.android.material.shape.AbsoluteCornerSize
import com.google.android.material.shape.MaterialShapeDrawable
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNull
import org.junit.Assert.assertSame
import org.junit.Assert.assertTrue
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.RuntimeEnvironment
import org.robolectric.Shadows.shadowOf
import java.time.Duration

@RunWith(RobolectricTestRunner::class)
class SheetLayoutRoboTest {

    private val context = RuntimeEnvironment.getApplication()
    private val density = context.resources.displayMetrics.density

    private fun sheet(presentation: Presentation, onClose: () -> Unit = {}) =
        SheetLayout(context, presentation, onClose)

    private fun laidOut(layout: SheetLayout, w: Int = 1000, h: Int = 2000): SheetLayout {
        layout.measure(MeasureSpec.makeMeasureSpec(w, MeasureSpec.EXACTLY), MeasureSpec.makeMeasureSpec(h, MeasureSpec.EXACTLY))
        layout.layout(0, 0, w, h)
        return layout
    }

    private fun idleFor(ms: Long) = shadowOf(Looper.getMainLooper()).idleFor(Duration.ofMillis(ms))

    @Test
    fun `sheet card height resolves ninety percent`() {
        val s = laidOut(sheet(Presentation.Sheet("90%")))
        assertEquals(1800, s.card.layoutParams.height)
    }

    @Test
    fun `sheet card height resolves custom percent`() {
        val s = laidOut(sheet(Presentation.Sheet("60%")))
        assertEquals(1200, s.card.layoutParams.height)
    }

    @Test
    fun `backdrop is black at 0_55 alpha`() {
        val s = sheet(Presentation.Sheet("90%"))
        val color = (s.backdrop!!.background as ColorDrawable).color
        assertEquals(Color.argb(140, 0, 0, 0), color)
    }

    @Test
    fun `card corner radius is 24dp top only`() {
        val s = sheet(Presentation.Sheet("90%"))
        val model = (s.card.background as MaterialShapeDrawable).shapeAppearanceModel
        assertEquals(24f * density, (model.topLeftCornerSize as AbsoluteCornerSize).cornerSize)
        assertEquals(24f * density, (model.topRightCornerSize as AbsoluteCornerSize).cornerSize)
        assertEquals(0f, (model.bottomLeftCornerSize as AbsoluteCornerSize).cornerSize)
        assertEquals(0f, (model.bottomRightCornerSize as AbsoluteCornerSize).cornerSize)
    }

    @Test
    fun `full screen has no backdrop and card fills parent`() {
        val s = laidOut(sheet(Presentation.FullScreen))
        assertNull(s.backdrop)
        assertEquals(2000, s.card.height)
        assertEquals(1000, s.card.width)
    }

    @Test
    fun `full screen padded by system bars top and bottom`() {
        val s = sheet(Presentation.FullScreen)
        s.applyInsets(Insets.of(5, 60, 7, 40), Insets.of(0, 0, 0, 0))
        assertEquals(5, s.card.paddingLeft)
        assertEquals(60, s.card.paddingTop)
        assertEquals(7, s.card.paddingRight)
        assertEquals(40, s.card.paddingBottom)
    }

    @Test
    fun `full screen bottom padding takes ime when visible`() {
        val s = sheet(Presentation.FullScreen)
        s.applyInsets(Insets.of(0, 60, 0, 40), Insets.of(0, 0, 0, 900))
        assertEquals(900, s.card.paddingBottom)
        assertEquals(60, s.card.paddingTop)
    }

    @Test
    fun `sheet card bottom inset composes max not sum and no top inset`() {
        val s = sheet(Presentation.Sheet("90%"))
        s.applyInsets(Insets.of(0, 80, 0, 500), Insets.of(0, 0, 0, 300))
        assertEquals(0, s.card.paddingTop)
        assertEquals(500, s.card.paddingBottom)
    }

    @Test
    fun `backdrop tap requests close and does not remove card`() {
        var closes = 0
        val s = sheet(Presentation.Sheet("90%")) { closes++ }
        s.backdrop!!.performClick()
        assertEquals(1, closes)
        assertSame(s, s.card.parent)
    }

    @Test
    fun `webview is the same instance through enter and inset change`() {
        val s = sheet(Presentation.Sheet("90%"))
        val web = WebView(context)
        s.mountContent(web)
        s.animateIn()
        idleFor(280)
        s.applyInsets(Insets.of(0, 0, 0, 40), Insets.of(0, 0, 0, 0))
        assertSame(web, findWebView(s))
        assertSame(s.card, web.parent)
    }

    @Test
    fun `exit keeps content mounted until animation end`() {
        val s = sheet(Presentation.Sheet("90%"))
        val web = WebView(context)
        s.mountContent(web)
        var ended = false
        s.animateOut { ended = true }
        assertFalse(ended)
        assertSame(s.card, web.parent)
        idleFor(200)
        assertTrue(ended)
    }

    @Test
    fun `full screen exit invokes end immediately`() {
        val s = sheet(Presentation.FullScreen)
        var ended = false
        s.animateOut { ended = true }
        assertTrue(ended)
    }

    private fun findWebView(v: View): WebView? {
        if (v is WebView) return v
        if (v is android.view.ViewGroup) for (i in 0 until v.childCount) findWebView(v.getChildAt(i))?.let { return it }
        return null
    }
}
