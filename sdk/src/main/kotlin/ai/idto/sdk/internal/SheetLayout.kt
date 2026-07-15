package ai.idto.sdk.internal

import android.animation.Animator
import android.animation.AnimatorListenerAdapter
import android.animation.ObjectAnimator
import android.content.Context
import android.content.res.ColorStateList
import android.graphics.Color
import android.graphics.drawable.ColorDrawable
import android.view.Gravity
import android.view.View
import android.view.ViewGroup.LayoutParams.MATCH_PARENT
import android.view.ViewGroup.LayoutParams.WRAP_CONTENT
import android.view.animation.PathInterpolator
import android.widget.FrameLayout
import androidx.core.graphics.Insets
import androidx.core.view.ViewCompat
import androidx.core.view.WindowInsetsCompat
import com.google.android.material.shape.CornerFamily
import com.google.android.material.shape.MaterialShapeDrawable
import com.google.android.material.shape.ShapeAppearanceModel
import kotlin.math.max
import kotlin.math.roundToInt

class SheetLayout(
    context: Context,
    val presentation: Presentation,
    private val onCloseRequest: () -> Unit,
) : FrameLayout(context) {

    private val isSheet = presentation is Presentation.Sheet
    val card: FrameLayout = FrameLayout(context)
    val backdrop: View? = if (isSheet) buildBackdrop() else null

    init {
        backdrop?.let { addView(it, LayoutParams(MATCH_PARENT, MATCH_PARENT)) }
        addView(card, buildCardParams())
        card.background = if (isSheet) buildSheetBackground() else ColorDrawable(Color.WHITE)
        ViewCompat.setOnApplyWindowInsetsListener(this) { _, insets ->
            applyInsets(insets.getInsets(WindowInsetsCompat.Type.systemBars()), insets.getInsets(WindowInsetsCompat.Type.ime()))
            insets
        }
    }

    fun mountContent(view: View) {
        if (view.parent !== card) {
            (view.parent as? FrameLayout)?.removeView(view)
            card.addView(view, LayoutParams(MATCH_PARENT, MATCH_PARENT))
        }
    }

    fun applyInsets(bars: Insets, ime: Insets) {
        val bottom = max(bars.bottom, ime.bottom)
        if (isSheet) {
            card.setPadding(0, 0, 0, bottom)
        } else {
            val p = fullScreenPadding(bars.left, bars.top, bars.right, bars.bottom, ime.bottom)
            card.setPadding(p[0], p[1], p[2], p[3])
        }
    }

    fun animateIn() {
        if (!isSheet) return
        card.translationY = travel()
        doOnCardSized {
            backdrop?.let {
                it.alpha = 0f
                ObjectAnimator.ofFloat(it, View.ALPHA, 0f, 1f).apply {
                    duration = Defaults.ANIM_IN_MS
                    interpolator = EASE_OUT
                    start()
                }
            }
            ObjectAnimator.ofFloat(card, View.TRANSLATION_Y, card.translationY, 0f).apply {
                duration = Defaults.ANIM_IN_MS
                interpolator = EASE_OUT
                start()
            }
        }
    }

    private fun doOnCardSized(action: () -> Unit) {
        if (card.height > 0) {
            action()
            return
        }
        card.addOnLayoutChangeListener(object : View.OnLayoutChangeListener {
            override fun onLayoutChange(
                v: View, l: Int, t: Int, r: Int, b: Int,
                ol: Int, ot: Int, or2: Int, ob: Int,
            ) {
                if (card.height <= 0) return
                card.removeOnLayoutChangeListener(this)
                action()
            }
        })
    }

    fun animateOut(onEnd: () -> Unit) {
        if (!isSheet) {
            onEnd()
            return
        }
        backdrop?.let {
            ObjectAnimator.ofFloat(it, View.ALPHA, it.alpha, 0f).apply {
                duration = Defaults.ANIM_OUT_MS
                interpolator = EASE_IN
                start()
            }
        }
        ObjectAnimator.ofFloat(card, View.TRANSLATION_Y, card.translationY, travel()).apply {
            duration = Defaults.ANIM_OUT_MS
            interpolator = EASE_IN
            addListener(object : AnimatorListenerAdapter() {
                override fun onAnimationEnd(animation: Animator) = onEnd()
            })
            start()
        }
    }

    override fun onMeasure(widthMeasureSpec: Int, heightMeasureSpec: Int) {
        if (presentation is Presentation.Sheet) {
            val available = MeasureSpec.getSize(heightMeasureSpec)
            if (available > 0) {
                val newH = heightToPx(presentation.height, available, resources.displayMetrics.density)
                if (card.layoutParams.height != newH) card.layoutParams.height = newH
            }
        }
        super.onMeasure(widthMeasureSpec, heightMeasureSpec)
    }

    private fun travel(): Float = resources.displayMetrics.heightPixels.toFloat()

    private fun buildBackdrop(): View = View(context).apply {
        background = ColorDrawable(BACKDROP_COLOR)
        setOnClickListener { onCloseRequest() }
    }

    private fun buildCardParams(): LayoutParams =
        if (isSheet) LayoutParams(MATCH_PARENT, WRAP_CONTENT, Gravity.BOTTOM)
        else LayoutParams(MATCH_PARENT, MATCH_PARENT)

    private fun buildSheetBackground(): MaterialShapeDrawable {
        val radius = Defaults.CORNER_RADIUS_DP * resources.displayMetrics.density
        val shape = ShapeAppearanceModel.builder()
            .setTopLeftCorner(CornerFamily.ROUNDED, radius)
            .setTopRightCorner(CornerFamily.ROUNDED, radius)
            .build()
        return MaterialShapeDrawable(shape).apply { fillColor = ColorStateList.valueOf(Color.WHITE) }
    }

    private companion object {
        val BACKDROP_COLOR = Color.argb((Defaults.BACKDROP_ALPHA * 255).roundToInt(), 0, 0, 0)
        val EASE_OUT = PathInterpolator(0.33f, 1f, 0.68f, 1f)
        val EASE_IN = PathInterpolator(0.32f, 0f, 0.67f, 0f)
    }
}
