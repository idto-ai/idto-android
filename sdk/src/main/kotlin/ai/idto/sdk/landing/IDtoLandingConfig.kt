package ai.idto.sdk.landing

import android.graphics.drawable.Drawable
import ai.idto.sdk.IDtoAadhaarConfig
import ai.idto.sdk.IDtoAbandonData
import ai.idto.sdk.IDtoBottomSheet
import ai.idto.sdk.IDtoColors
import ai.idto.sdk.IDtoConfig
import ai.idto.sdk.IDtoDisplayMode
import ai.idto.sdk.IDtoEnv
import ai.idto.sdk.IDtoErrorData
import ai.idto.sdk.IDtoFaceMatchConfig
import ai.idto.sdk.IDtoLanguage
import ai.idto.sdk.IDtoNameMatchConfig
import ai.idto.sdk.IDtoPanConfig
import ai.idto.sdk.IDtoStepCompleteData
import ai.idto.sdk.IDtoTheme
import ai.idto.sdk.IDtoTokenProvider
import ai.idto.sdk.IDtoWorkflowCompleteData
import ai.idto.sdk.internal.Defaults
import org.json.JSONObject

interface IDtoLandingListener {
    fun onComplete(data: IDtoWorkflowCompleteData) {}
    fun onDismiss() {}
    fun onAbandon(data: IDtoAbandonData) {}
    fun onError(data: IDtoErrorData) {}
    fun onStepComplete(data: IDtoStepCompleteData) {}
}

class IDtoLandingConfig private constructor(
    val workflowTemplateId: String,
    val tokenProvider: IDtoTokenProvider,
    val copy: IDtoLandingCopyOverride?,
    val brandColor: String?,
    val logo: Drawable?,
    val logoWidthDp: Int?,
    val logoHeightDp: Int?,
    val logoUrl: String?,
    val colors: IDtoColors?,
    val businessName: String?,
    val phone: String?,
    val merchantUserId: String?,
    val startFresh: Boolean?,
    val preVerified: Map<String, Boolean>?,
    val skippable: List<String>?,
    val sessionToken: String?,
    val referenceName: String?,
    val accumulatedData: JSONObject?,
    val aadhaarConfig: IDtoAadhaarConfig?,
    val faceMatchReferenceImage: String?,
    val faceMatchConfig: IDtoFaceMatchConfig?,
    val panConfig: IDtoPanConfig?,
    val nameMatchConfig: IDtoNameMatchConfig?,
    val language: IDtoLanguage?,
    val theme: IDtoTheme?,
    val env: IDtoEnv?,
    val baseUrl: String?,
    val bottomSheet: IDtoBottomSheet?,
    val allowedHosts: List<String>?,
    val debug: Boolean,
    val readyTimeoutMs: Long,
) {
    fun buildConfig(clientToken: String, palette: IDtoColors): IDtoConfig {
        val builder = IDtoConfig.Builder(clientToken, workflowTemplateId)
            .displayMode(IDtoDisplayMode.BOTTOM_SHEET)
            .bottomSheet(bottomSheet ?: IDtoBottomSheet.Builder().minHeight(Defaults.SHEET_HEIGHT).build())
            .colors(palette)
            .debug(debug)
            .readyTimeoutMs(readyTimeoutMs)
        logoUrl?.let { builder.logo(it) }
        businessName?.let { builder.businessName(it) }
        phone?.let { builder.phone(it) }
        merchantUserId?.let { builder.merchantUserId(it) }
        startFresh?.let { builder.startFresh(it) }
        preVerified?.let { builder.preVerified(it) }
        skippable?.let { builder.skippable(it) }
        sessionToken?.let { builder.sessionToken(it) }
        referenceName?.let { builder.referenceName(it) }
        accumulatedData?.let { builder.accumulatedData(it) }
        aadhaarConfig?.let { builder.aadhaarConfig(it) }
        faceMatchReferenceImage?.let { builder.faceMatchReferenceImage(it) }
        faceMatchConfig?.let { builder.faceMatchConfig(it) }
        panConfig?.let { builder.panConfig(it) }
        nameMatchConfig?.let { builder.nameMatchConfig(it) }
        language?.let { builder.language(it) }
        theme?.let { builder.theme(it) }
        env?.let { builder.env(it) }
        baseUrl?.let { builder.baseUrl(it) }
        allowedHosts?.let { builder.allowedHosts(it) }
        return builder.build()
    }

    class Builder(private val workflowTemplateId: String, private val tokenProvider: IDtoTokenProvider) {
        private var copy: IDtoLandingCopyOverride? = null
        private var brandColor: String? = null
        private var logo: Drawable? = null
        private var logoWidthDp: Int? = null
        private var logoHeightDp: Int? = null
        private var logoUrl: String? = null
        private var colors: IDtoColors? = null
        private var businessName: String? = null
        private var phone: String? = null
        private var merchantUserId: String? = null
        private var startFresh: Boolean? = null
        private var preVerified: Map<String, Boolean>? = null
        private var skippable: List<String>? = null
        private var sessionToken: String? = null
        private var referenceName: String? = null
        private var accumulatedData: JSONObject? = null
        private var aadhaarConfig: IDtoAadhaarConfig? = null
        private var faceMatchReferenceImage: String? = null
        private var faceMatchConfig: IDtoFaceMatchConfig? = null
        private var panConfig: IDtoPanConfig? = null
        private var nameMatchConfig: IDtoNameMatchConfig? = null
        private var language: IDtoLanguage? = null
        private var theme: IDtoTheme? = null
        private var env: IDtoEnv? = null
        private var baseUrl: String? = null
        private var bottomSheet: IDtoBottomSheet? = null
        private var allowedHosts: List<String>? = null
        private var debug: Boolean = false
        private var readyTimeoutMs: Long = Defaults.READY_TIMEOUT_MS

        fun copy(v: IDtoLandingCopyOverride) = apply { copy = v }
        fun brandColor(v: String) = apply { brandColor = v }
        fun logo(v: Drawable) = apply { logo = v }
        fun logoSizeDp(width: Int, height: Int) = apply { logoWidthDp = width; logoHeightDp = height }
        fun logoUrl(v: String) = apply { logoUrl = v }
        fun colors(v: IDtoColors) = apply { colors = v }
        fun businessName(v: String) = apply { businessName = v }
        fun phone(v: String) = apply { phone = v }
        fun merchantUserId(v: String) = apply { merchantUserId = v }
        fun startFresh(v: Boolean) = apply { startFresh = v }
        fun preVerified(v: Map<String, Boolean>) = apply { preVerified = v }
        fun skippable(v: List<String>) = apply { skippable = v }
        fun sessionToken(v: String) = apply { sessionToken = v }
        fun referenceName(v: String) = apply { referenceName = v }
        fun accumulatedData(v: JSONObject) = apply { accumulatedData = v }
        fun aadhaarConfig(v: IDtoAadhaarConfig) = apply { aadhaarConfig = v }
        fun faceMatchReferenceImage(v: String) = apply { faceMatchReferenceImage = v }
        fun faceMatchConfig(v: IDtoFaceMatchConfig) = apply { faceMatchConfig = v }
        fun panConfig(v: IDtoPanConfig) = apply { panConfig = v }
        fun nameMatchConfig(v: IDtoNameMatchConfig) = apply { nameMatchConfig = v }
        fun language(v: IDtoLanguage) = apply { language = v }
        fun theme(v: IDtoTheme) = apply { theme = v }
        fun env(v: IDtoEnv) = apply { env = v }
        fun baseUrl(v: String) = apply { baseUrl = v }
        fun bottomSheet(v: IDtoBottomSheet) = apply { bottomSheet = v }
        fun allowedHosts(v: List<String>) = apply { allowedHosts = v }
        fun debug(v: Boolean) = apply { debug = v }
        fun readyTimeoutMs(v: Long) = apply { readyTimeoutMs = v }

        fun build() = IDtoLandingConfig(
            workflowTemplateId, tokenProvider, copy, brandColor, logo, logoWidthDp, logoHeightDp,
            logoUrl, colors, businessName, phone, merchantUserId, startFresh, preVerified, skippable, sessionToken,
            referenceName, accumulatedData, aadhaarConfig, faceMatchReferenceImage, faceMatchConfig,
            panConfig, nameMatchConfig, language, theme, env, baseUrl, bottomSheet, allowedHosts,
            debug, readyTimeoutMs,
        )
    }
}
