package ai.idto.sdk

import ai.idto.sdk.internal.Defaults
import ai.idto.sdk.internal.Presentation
import ai.idto.sdk.internal.resolvePresentation
import ai.idto.sdk.internal.heightToPx
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

class PresentationTest {

    @Test fun defaultNullDisplayModeFullScreen() =
        assertEquals(Presentation.FullScreen, resolvePresentation(null, null))

    @Test fun explicitFullScreenIgnoresBottomSheet() {
        val bs = IDtoBottomSheet.Builder().minHeight("60%").build()
        assertEquals(Presentation.FullScreen, resolvePresentation(IDtoDisplayMode.FULL_SCREEN, bs))
    }

    @Test fun bottomSheetDefaultHeight() {
        val p = resolvePresentation(IDtoDisplayMode.BOTTOM_SHEET, null)
        assertEquals(Presentation.Sheet("90%"), p)
    }

    @Test fun bottomSheetPercentHeight() {
        val bs = IDtoBottomSheet.Builder().minHeight("60%").build()
        assertEquals(Presentation.Sheet("60%"), resolvePresentation(IDtoDisplayMode.BOTTOM_SHEET, bs))
    }

    @Test fun bottomSheetPxHeight() {
        val bs = IDtoBottomSheet.Builder().minHeight(480).build()
        assertEquals(Presentation.Sheet(480), resolvePresentation(IDtoDisplayMode.BOTTOM_SHEET, bs))
    }

    @Test fun heightPercentOfParent() =
        assertEquals(1800, heightToPx("90%", 2000, 1.0f))

    @Test fun heightPercentSixty() =
        assertEquals(1200, heightToPx("60%", 2000, 1.0f))

    @Test fun heightIntDpTimesDensity() =
        assertEquals(960, heightToPx(480, 2000, 2.0f))

    @Test fun heightNumericStringTimesDensity() =
        assertEquals(960, heightToPx("480", 2000, 2.0f))

    @Test fun heightInvalidFallsBackToNinetyPercent() =
        assertEquals(1800, heightToPx("garbage", 2000, 1.0f))

    @Test fun heightNullFallsBackToNinetyPercent() =
        assertEquals(1800, heightToPx(null, 2000, 1.0f))

    @Test fun constantsLocked() {
        assertEquals(0.55f, Defaults.BACKDROP_ALPHA)
        assertEquals(24, Defaults.CORNER_RADIUS_DP)
        assertEquals(280L, Defaults.ANIM_IN_MS)
        assertEquals(200L, Defaults.ANIM_OUT_MS)
        assertEquals("90%", Defaults.SHEET_HEIGHT)
    }
}
