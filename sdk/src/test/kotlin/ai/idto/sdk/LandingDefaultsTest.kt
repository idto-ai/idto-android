package ai.idto.sdk

import ai.idto.sdk.landing.IDtoLandingCopyOverride
import ai.idto.sdk.landing.IDtoLandingCtaOverride
import ai.idto.sdk.landing.LandingDefaults
import org.junit.Assert.assertEquals
import org.junit.Test

class LandingDefaultsTest {

    @Test fun brandConstant() = assertEquals("#0019FF", LandingDefaults.IDTO_BRAND)

    @Test fun defaultCopyExactStrings() {
        val c = LandingDefaults.DEFAULT_COPY
        assertEquals("Verify your identity", c.heroTitle)
        assertEquals("A quick, secure {business}KYC check — done in under a minute.", c.heroSubtitle)
        assertEquals("", c.heroNote)
        assertEquals(listOf("Confirm your details", "Capture your documents", "Instant result"), c.steps)
        assertEquals("Start verification", c.cta.idle)
        assertEquals("Starting…", c.cta.loading)
        assertEquals("In progress…", c.cta.open)
        assertEquals("Verify again", c.cta.done)
        assertEquals(emptyList<String>(), c.trust)
        assertEquals("Powered by IDto · idto.ai", c.footer)
    }

    @Test fun interpolateBusinessInsertsNameWithTrailingSpace() {
        assertEquals(
            "A quick, secure DriveX KYC check — done in under a minute.",
            LandingDefaults.interpolateBusiness(LandingDefaults.DEFAULT_COPY.heroSubtitle, "DriveX"),
        )
    }

    @Test fun interpolateBusinessDropsTokenWhenBlank() {
        assertEquals(
            "A quick, secure KYC check — done in under a minute.",
            LandingDefaults.interpolateBusiness(LandingDefaults.DEFAULT_COPY.heroSubtitle, null),
        )
        assertEquals(
            "A quick, secure KYC check — done in under a minute.",
            LandingDefaults.interpolateBusiness(LandingDefaults.DEFAULT_COPY.heroSubtitle, "   "),
        )
    }

    @Test fun mergeCopyNullReturnsDefaults() {
        assertEquals(LandingDefaults.DEFAULT_COPY, LandingDefaults.mergeCopy(null))
    }

    @Test fun mergeCopyPartialCtaKeepsOthers() {
        val merged = LandingDefaults.mergeCopy(
            IDtoLandingCopyOverride(cta = IDtoLandingCtaOverride(idle = "Go")),
        )
        assertEquals("Go", merged.cta.idle)
        assertEquals("Starting…", merged.cta.loading)
        assertEquals("In progress…", merged.cta.open)
        assertEquals("Verify again", merged.cta.done)
    }

    @Test fun mergeCopyTopLevelOverrideWins() {
        val merged = LandingDefaults.mergeCopy(IDtoLandingCopyOverride(heroTitle = "Custom"))
        assertEquals("Custom", merged.heroTitle)
        assertEquals("", merged.heroNote)
    }

    @Test fun contrastTextWhiteOnBrand() = assertEquals("#ffffff", LandingDefaults.contrastText("#0019FF"))
    @Test fun contrastTextWhiteOnPurple() = assertEquals("#ffffff", LandingDefaults.contrastText("#7C22C4"))
    @Test fun contrastTextBlackOnYellow() = assertEquals("#000000", LandingDefaults.contrastText("#FFEB3B"))
    @Test fun contrastTextBlackOnWhite() = assertEquals("#000000", LandingDefaults.contrastText("#FFFFFF"))
    @Test fun contrastTextThreeDigitHex() = assertEquals("#000000", LandingDefaults.contrastText("#fff"))
    @Test fun contrastTextMissingHash() = assertEquals("#ffffff", LandingDefaults.contrastText("0019FF"))

    @Test fun paletteFromBrandDefaults() {
        val p = LandingDefaults.paletteFromBrand("#0019FF", null)
        assertEquals("#0019FF", p.primary)
        assertEquals("#ffffff", p.background)
        assertEquals("#18181b", p.text)
        assertEquals("#71717a", p.text2)
        assertEquals("#e5e5e5", p.border)
        assertEquals("#18181b", p.secondary)
        assertEquals("#ffffff", p.buttonTextColorPrimary)
        assertEquals("#ffffff", p.buttonTextColorSecondary)
    }

    @Test fun paletteFromBrandButtonTextFollowsContrast() {
        val p = LandingDefaults.paletteFromBrand("#FFEB3B", null)
        assertEquals("#000000", p.buttonTextColorPrimary)
    }

    @Test fun paletteExplicitColorsOverridePerField() {
        val override = IDtoColors.Builder().background("#111111").text("#eeeeee").build()
        val p = LandingDefaults.paletteFromBrand("#0019FF", override)
        assertEquals("#111111", p.background)
        assertEquals("#eeeeee", p.text)
        assertEquals("#0019FF", p.primary)
        assertEquals("#71717a", p.text2)
    }
}
