package ai.idto.sdk.landing

import ai.idto.sdk.IDtoColors
import kotlin.math.pow

data class IDtoLandingCta(
    val idle: String,
    val loading: String,
    val open: String,
    val done: String,
)

data class IDtoLandingCopy(
    val heroTitle: String,
    val heroSubtitle: String,
    val heroNote: String,
    val steps: List<String>,
    val cta: IDtoLandingCta,
    val trust: List<String>,
    val footer: String,
)

data class IDtoLandingCtaOverride(
    val idle: String? = null,
    val loading: String? = null,
    val open: String? = null,
    val done: String? = null,
)

data class IDtoLandingCopyOverride(
    val heroTitle: String? = null,
    val heroSubtitle: String? = null,
    val heroNote: String? = null,
    val steps: List<String>? = null,
    val cta: IDtoLandingCtaOverride? = null,
    val trust: List<String>? = null,
    val footer: String? = null,
)

object LandingDefaults {

    const val IDTO_BRAND = "#0019FF"
    const val BUSINESS_TOKEN = "{business}"

    val DEFAULT_COPY = IDtoLandingCopy(
        heroTitle = "Verify your identity",
        heroSubtitle = "A quick, secure {business}KYC check — done in under a minute.",
        heroNote = "",
        steps = listOf("Confirm your details", "Capture your documents", "Instant result"),
        cta = IDtoLandingCta(
            idle = "Start verification",
            loading = "Starting…",
            open = "In progress…",
            done = "Verify again",
        ),
        trust = emptyList(),
        footer = "Powered by IDto · idto.ai",
    )

    fun interpolateBusiness(text: String, businessName: String?): String =
        text.replace(BUSINESS_TOKEN, businessName?.trim()?.takeIf { it.isNotEmpty() }?.let { "$it " } ?: "")

    fun mergeCopy(override: IDtoLandingCopyOverride?): IDtoLandingCopy {
        if (override == null) return DEFAULT_COPY
        val base = DEFAULT_COPY
        return IDtoLandingCopy(
            heroTitle = override.heroTitle ?: base.heroTitle,
            heroSubtitle = override.heroSubtitle ?: base.heroSubtitle,
            heroNote = override.heroNote ?: base.heroNote,
            steps = override.steps ?: base.steps,
            cta = mergeCta(base.cta, override.cta),
            trust = override.trust ?: base.trust,
            footer = override.footer ?: base.footer,
        )
    }

    private fun mergeCta(base: IDtoLandingCta, override: IDtoLandingCtaOverride?): IDtoLandingCta {
        if (override == null) return base
        return IDtoLandingCta(
            idle = override.idle ?: base.idle,
            loading = override.loading ?: base.loading,
            open = override.open ?: base.open,
            done = override.done ?: base.done,
        )
    }

    fun contrastText(bg: String): String {
        val hex = bg.replace("#", "")
        val full = if (hex.length == 3) hex.map { "$it$it" }.joinToString("") else hex
        val r = full.substring(0, 2).toInt(16) / 255.0
        val g = full.substring(2, 4).toInt(16) / 255.0
        val b = full.substring(4, 6).toInt(16) / 255.0
        val luminance = 0.2126 * lin(r) + 0.7152 * lin(g) + 0.0722 * lin(b)
        return if (luminance > 0.5) "#000000" else "#ffffff"
    }

    private fun lin(c: Double): Double = if (c <= 0.03928) c / 12.92 else ((c + 0.055) / 1.055).pow(2.4)

    fun paletteFromBrand(brand: String, override: IDtoColors?): IDtoColors =
        IDtoColors.Builder()
            .primary(override?.primary ?: brand)
            .background(override?.background ?: "#ffffff")
            .text(override?.text ?: "#18181b")
            .text2(override?.text2 ?: "#71717a")
            .border(override?.border ?: "#e5e5e5")
            .secondary(override?.secondary ?: "#18181b")
            .buttonTextColorPrimary(override?.buttonTextColorPrimary ?: contrastText(brand))
            .buttonTextColorSecondary(override?.buttonTextColorSecondary ?: "#ffffff")
            .build()
}
