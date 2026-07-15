package ai.idto.sdk.internal

import ai.idto.sdk.IDtoBottomSheet
import ai.idto.sdk.IDtoDisplayMode
import kotlin.math.max
import kotlin.math.roundToInt

sealed class Presentation {
    object FullScreen : Presentation()
    data class Sheet(val height: Any) : Presentation()
}

fun resolvePresentation(displayMode: IDtoDisplayMode?, bottomSheet: IDtoBottomSheet?): Presentation {
    if (displayMode != IDtoDisplayMode.BOTTOM_SHEET) return Presentation.FullScreen
    return Presentation.Sheet(bottomSheet?.minHeight ?: Defaults.SHEET_HEIGHT)
}

fun heightToPx(spec: Any?, parentPx: Int, density: Float): Int {
    when (spec) {
        is Int -> return (spec * density).roundToInt()
        is String -> {
            val trimmed = spec.trim()
            if (trimmed.endsWith("%")) {
                trimmed.dropLast(1).toDoubleOrNull()?.let { return (parentPx * it / 100.0).roundToInt() }
            } else {
                trimmed.toDoubleOrNull()?.let { return (it * density).roundToInt() }
            }
        }
    }
    return defaultHeightPx(parentPx)
}

fun fullScreenPadding(barsLeft: Int, barsTop: Int, barsRight: Int, barsBottom: Int, imeBottom: Int): IntArray =
    intArrayOf(barsLeft, barsTop, barsRight, max(barsBottom, imeBottom))

private fun defaultHeightPx(parentPx: Int): Int {
    val pct = Defaults.SHEET_HEIGHT.trimEnd('%').toDouble()
    return (parentPx * pct / 100.0).roundToInt()
}
