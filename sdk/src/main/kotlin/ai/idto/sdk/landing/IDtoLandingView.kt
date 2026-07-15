package ai.idto.sdk.landing

import android.content.Context
import android.graphics.Color
import android.graphics.Typeface
import android.graphics.drawable.GradientDrawable
import android.os.Handler
import android.os.Looper
import android.util.AttributeSet
import android.util.TypedValue
import android.view.Gravity
import android.view.View
import android.view.ViewGroup.LayoutParams.MATCH_PARENT
import android.view.ViewGroup.LayoutParams.WRAP_CONTENT
import android.widget.FrameLayout
import android.widget.ImageView
import android.widget.LinearLayout
import android.widget.ProgressBar
import android.widget.TextView
import androidx.core.view.ViewCompat
import androidx.core.view.WindowInsetsCompat
import androidx.core.widget.TextViewCompat
import ai.idto.sdk.IDto
import ai.idto.sdk.IDtoAbandonData
import ai.idto.sdk.IDtoColors
import ai.idto.sdk.IDtoConfig
import ai.idto.sdk.IDtoErrorData
import ai.idto.sdk.IDtoEventListener
import ai.idto.sdk.IDtoStepCompleteData
import ai.idto.sdk.IDtoTokenCallback
import ai.idto.sdk.IDtoTokenProvider
import ai.idto.sdk.IDtoWorkflowCompleteData

class IDtoLandingView @JvmOverloads constructor(
    context: Context,
    attrs: AttributeSet? = null,
    defStyleAttr: Int = 0,
) : FrameLayout(context, attrs, defStyleAttr) {

    internal var opener: (Context, IDtoConfig, IDtoEventListener, IDtoTokenProvider?) -> Unit =
        { c, cfg, l, tp -> IDto.open(c, cfg, l, tp) }

    private val mainHandler = Handler(Looper.getMainLooper())
    private var config: IDtoLandingConfig? = null
    private var listener: IDtoLandingListener? = null
    private var status = LandingStatus.IDLE
    private var copy = LandingDefaults.DEFAULT_COPY
    private var palette: IDtoColors = LandingDefaults.paletteFromBrand(LandingDefaults.IDTO_BRAND, null)

    internal var brandColorInt: Int = Color.parseColor(LandingDefaults.IDTO_BRAND)
    internal var ctaTextColorInt: Int = Color.WHITE

    private val content = LinearLayout(context)
    internal val logoView = ImageView(context)
    internal val heroTitleView = TextView(context)
    internal val subtitleView = TextView(context)
    internal val noteView = TextView(context)
    internal val stepsContainer = LinearLayout(context)
    internal val ctaButton = FrameLayout(context)
    internal val ctaLabelView = TextView(context)
    internal val spinner = ProgressBar(context)
    internal val trustContainer = LinearLayout(context)
    internal val footerView = TextView(context)

    private val internalListener = object : IDtoEventListener {
        override fun onStepComplete(data: IDtoStepCompleteData) { listener?.onStepComplete(data) }
        override fun onWorkflowComplete(data: IDtoWorkflowCompleteData) {
            transition(LandingEvent.WORKFLOW_COMPLETE)
            listener?.onComplete(data)
        }
        override fun onAbandon(data: IDtoAbandonData) {
            transition(LandingEvent.ABANDON)
            listener?.onAbandon(data)
        }
        override fun onError(data: IDtoErrorData) { listener?.onError(data) }
        override fun onClose() {
            transition(LandingEvent.CLOSE)
            listener?.onDismiss()
        }
    }

    init {
        buildSkeleton()
        render()
    }

    fun configure(config: IDtoLandingConfig) {
        this.config = config
        copy = LandingDefaults.mergeCopy(config.copy)
        val brand = config.brandColor ?: config.colors?.primary ?: LandingDefaults.IDTO_BRAND
        palette = LandingDefaults.paletteFromBrand(brand, config.colors)
        brandColorInt = Color.parseColor(brand)
        ctaTextColorInt = Color.parseColor(palette.buttonTextColorPrimary ?: "#ffffff")
        render()
    }

    fun setListener(listener: IDtoLandingListener) {
        this.listener = listener
    }

    private fun transition(event: LandingEvent) {
        status = LandingState.next(status, event)
        render()
    }

    private fun onStart() {
        if (status == LandingStatus.LOADING || status == LandingStatus.OPEN) return
        val cfg = config ?: return
        transition(LandingEvent.START)
        cfg.tokenProvider.getToken(object : IDtoTokenCallback {
            override fun onToken(token: String) { mainHandler.post { onTokenReceived(token) } }
            override fun onError(error: Throwable) { mainHandler.post { transition(LandingEvent.TOKEN_ERROR) } }
        })
    }

    private fun onTokenReceived(token: String) {
        if (status != LandingStatus.LOADING) return
        val cfg = config ?: return
        transition(LandingEvent.TOKEN_OK)
        opener(context, cfg.buildConfig(token, palette), internalListener, cfg.tokenProvider)
    }

    private fun render() {
        content.setBackgroundColor(Color.parseColor(palette.background ?: "#ffffff"))
        renderLogo()
        heroTitleView.text = copy.heroTitle
        heroTitleView.setTextColor(brandColorInt)
        bindText(subtitleView, LandingDefaults.interpolateBusiness(copy.heroSubtitle, config?.businessName), Color.parseColor(SUBTITLE_COLOR))
        bindText(noteView, copy.heroNote, Color.parseColor(palette.text2 ?: "#71717a"))
        renderSteps()
        renderCta()
        renderTrust()
        bindText(footerView, copy.footer, Color.parseColor(FOOTER_COLOR))
    }

    private fun renderLogo() {
        val drawable = config?.logo
        if (drawable == null) {
            logoView.visibility = View.GONE
            return
        }
        logoView.visibility = View.VISIBLE
        logoView.setImageDrawable(drawable)
        val w = config?.logoWidthDp?.let { dp(it) } ?: WRAP_CONTENT
        val h = config?.logoHeightDp?.let { dp(it) } ?: dp(30)
        logoView.layoutParams = FrameLayout.LayoutParams(w, h, Gravity.CENTER_VERTICAL).apply { leftMargin = -dp(1) }
    }

    private fun renderSteps() {
        stepsContainer.removeAllViews()
        if (copy.steps.isEmpty()) {
            stepsContainer.visibility = View.GONE
            return
        }
        stepsContainer.visibility = View.VISIBLE
        stepsContainer.background = cardBackground()
        copy.steps.forEachIndexed { index, step ->
            stepsContainer.addView(stepRow(index + 1, step), rowParams(if (index == 0) 0 else dp(18)))
        }
    }

    private fun renderCta() {
        val bg = GradientDrawable().apply {
            cornerRadius = dp(14).toFloat()
            setColor(brandColorInt)
        }
        ctaButton.background = bg
        val busy = status == LandingStatus.LOADING || status == LandingStatus.OPEN
        ctaButton.alpha = if (busy) 0.7f else 1f
        if (status == LandingStatus.LOADING) {
            spinner.visibility = View.VISIBLE
            spinner.indeterminateTintList = android.content.res.ColorStateList.valueOf(ctaTextColorInt)
            ctaLabelView.visibility = View.GONE
        } else {
            spinner.visibility = View.GONE
            ctaLabelView.visibility = View.VISIBLE
            ctaLabelView.text = ctaLabel() + " →"
            ctaLabelView.setTextColor(ctaTextColorInt)
        }
    }

    private fun renderTrust() {
        trustContainer.removeAllViews()
        if (copy.trust.isEmpty()) {
            trustContainer.visibility = View.GONE
            return
        }
        trustContainer.visibility = View.VISIBLE
        copy.trust.forEachIndexed { index, item ->
            if (index > 0) trustContainer.addView(trustLabel("·", Color.parseColor("#9ca3af")))
            trustContainer.addView(trustLabel(item, Color.parseColor("#6b7280")))
        }
    }

    private fun ctaLabel(): String = when (status) {
        LandingStatus.IDLE -> copy.cta.idle
        LandingStatus.LOADING -> copy.cta.loading
        LandingStatus.OPEN -> copy.cta.open
        LandingStatus.DONE -> copy.cta.done
    }

    private fun buildSkeleton() {
        content.orientation = LinearLayout.VERTICAL
        content.setPadding(dp(20), dp(24), dp(20), dp(28))
        addView(content, LayoutParams(MATCH_PARENT, MATCH_PARENT))
        ViewCompat.setOnApplyWindowInsetsListener(this) { _, insets ->
            val top = insets.getInsets(WindowInsetsCompat.Type.statusBars()).top
            content.setPadding(dp(20), top + dp(24), dp(20), dp(28))
            insets
        }

        val header = FrameLayout(context)
        header.addView(logoView, FrameLayout.LayoutParams(WRAP_CONTENT, WRAP_CONTENT, Gravity.CENTER_VERTICAL))
        content.addView(header, LinearLayout.LayoutParams(MATCH_PARENT, dp(48)))

        val topArea = LinearLayout(context).apply {
            orientation = LinearLayout.VERTICAL
            gravity = Gravity.CENTER_VERTICAL
        }
        content.addView(topArea, LinearLayout.LayoutParams(MATCH_PARENT, 0, 1f))

        val hero = LinearLayout(context).apply { orientation = LinearLayout.VERTICAL }
        styleHeroTitle(heroTitleView)
        styleBody(subtitleView, 16f, dp(16))
        styleBody(noteView, 14f, dp(8))
        hero.addView(heroTitleView)
        hero.addView(subtitleView)
        hero.addView(noteView)
        topArea.addView(hero)

        stepsContainer.orientation = LinearLayout.VERTICAL
        stepsContainer.setPadding(dp(20), dp(20), dp(20), dp(20))
        topArea.addView(stepsContainer, LinearLayout.LayoutParams(MATCH_PARENT, WRAP_CONTENT).apply { topMargin = dp(16) })

        buildCta()
        content.addView(ctaButton, LinearLayout.LayoutParams(MATCH_PARENT, WRAP_CONTENT))

        trustContainer.orientation = LinearLayout.HORIZONTAL
        trustContainer.gravity = Gravity.CENTER_VERTICAL
        content.addView(trustContainer, LinearLayout.LayoutParams(WRAP_CONTENT, WRAP_CONTENT).apply { topMargin = dp(20) })

        footerView.setTextSize(TypedValue.COMPLEX_UNIT_SP, 12f)
        footerView.gravity = Gravity.CENTER
        content.addView(footerView, LinearLayout.LayoutParams(MATCH_PARENT, WRAP_CONTENT).apply { topMargin = dp(16) })
    }

    private fun buildCta() {
        ctaButton.minimumHeight = dp(58)
        ctaButton.setPadding(dp(16), dp(18), dp(16), dp(18))
        ctaButton.isClickable = true
        ctaButton.isFocusable = true
        ctaButton.setOnClickListener { onStart() }
        ctaLabelView.setTextSize(TypedValue.COMPLEX_UNIT_SP, 17f)
        ctaLabelView.typeface = Typeface.create("sans-serif-medium", Typeface.NORMAL)
        ctaLabelView.letterSpacing = 0.2f / 17f
        ctaLabelView.gravity = Gravity.CENTER
        ctaButton.addView(ctaLabelView, FrameLayout.LayoutParams(WRAP_CONTENT, WRAP_CONTENT, Gravity.CENTER))
        val spinnerSize = dp(24)
        ctaButton.addView(spinner, FrameLayout.LayoutParams(spinnerSize, spinnerSize, Gravity.CENTER))
        spinner.visibility = View.GONE
    }

    private fun stepRow(index: Int, step: String): LinearLayout {
        val row = LinearLayout(context).apply {
            orientation = LinearLayout.HORIZONTAL
            gravity = Gravity.CENTER_VERTICAL
        }
        val badge = TextView(context).apply {
            text = index.toString()
            setTextColor(ctaTextColorInt)
            setTextSize(TypedValue.COMPLEX_UNIT_SP, 13f)
            typeface = Typeface.DEFAULT_BOLD
            gravity = Gravity.CENTER
            background = GradientDrawable().apply { shape = GradientDrawable.OVAL; setColor(brandColorInt) }
        }
        row.addView(badge, LinearLayout.LayoutParams(dp(26), dp(26)))
        val label = TextView(context).apply {
            text = step
            setTextColor(Color.parseColor(palette.text ?: "#18181b"))
            setTextSize(TypedValue.COMPLEX_UNIT_SP, 15f)
        }
        row.addView(label, LinearLayout.LayoutParams(WRAP_CONTENT, WRAP_CONTENT).apply { marginStart = dp(12) })
        return row
    }

    private fun trustLabel(text: String, color: Int): TextView = TextView(context).apply {
        this.text = text
        setTextColor(color)
        setTextSize(TypedValue.COMPLEX_UNIT_SP, 13f)
        setPadding(dp(4), 0, dp(4), 0)
    }

    private fun cardBackground(): GradientDrawable = GradientDrawable().apply {
        cornerRadius = dp(16).toFloat()
        setColor(Color.TRANSPARENT)
        setStroke(dp(1), Color.parseColor(palette.border ?: "#e5e5e5"))
    }

    private fun rowParams(topMargin: Int) = LinearLayout.LayoutParams(MATCH_PARENT, WRAP_CONTENT).apply { this.topMargin = topMargin }

    private fun styleHeroTitle(view: TextView) {
        view.setTextSize(TypedValue.COMPLEX_UNIT_SP, 32f)
        TextViewCompat.setLineHeight(view, TypedValue.applyDimension(TypedValue.COMPLEX_UNIT_SP, 38f, resources.displayMetrics).toInt())
        view.letterSpacing = -0.5f / 32f
        view.typeface = Typeface.DEFAULT_BOLD
    }

    private fun styleBody(view: TextView, sizeSp: Float, topMargin: Int) {
        view.setTextSize(TypedValue.COMPLEX_UNIT_SP, sizeSp)
        (view.layoutParams as? LinearLayout.LayoutParams)?.topMargin = topMargin
        view.layoutParams = LinearLayout.LayoutParams(WRAP_CONTENT, WRAP_CONTENT).apply { this.topMargin = topMargin }
    }

    private fun bindText(view: TextView, text: String, color: Int) {
        if (text.isBlank()) {
            view.visibility = View.GONE
            return
        }
        view.visibility = View.VISIBLE
        view.text = text
        view.setTextColor(color)
    }

    private fun dp(value: Int): Int =
        (value * resources.displayMetrics.density).toInt()

    private companion object {
        const val SUBTITLE_COLOR = "#3f3f46"
        const val FOOTER_COLOR = "#a1a1aa"
    }
}
