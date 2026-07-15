package ai.idto.sdk

import ai.idto.sdk.internal.Defaults
import org.json.JSONObject

enum class IDtoEnv(val wire: String) {
    PRODUCTION("production"),
    DEVELOPMENT("development"),
}

enum class IDtoTheme(val wire: String) {
    LIGHT("light"),
    DARK("dark"),
}

enum class IDtoLanguage(val wire: String) {
    EN("en"),
    HI("hi"),
}

enum class IDtoDisplayMode(val wire: String) {
    FULL_SCREEN("full_screen"),
    BOTTOM_SHEET("bottom_sheet"),
}

class IDtoConfig private constructor(
    val clientToken: String,
    val workflowTemplateId: String,
    val sessionToken: String?,
    val merchantUserId: String?,
    val phone: String?,
    val startFresh: Boolean?,
    val preVerified: Map<String, Boolean>?,
    val accumulatedData: JSONObject?,
    val aadhaarConfig: IDtoAadhaarConfig?,
    val referenceName: String?,
    val nameMatchConfig: IDtoNameMatchConfig?,
    val faceMatchReferenceImage: String?,
    val faceMatchConfig: IDtoFaceMatchConfig?,
    val panConfig: IDtoPanConfig?,
    val businessName: String?,
    val logo: String?,
    val language: IDtoLanguage?,
    val theme: IDtoTheme?,
    val displayMode: IDtoDisplayMode,
    val bottomSheet: IDtoBottomSheet?,
    val env: IDtoEnv?,
    val baseUrl: String?,
    val colors: IDtoColors?,
    val debug: Boolean,
    val readyTimeoutMs: Long,
    val allowedHosts: List<String>?,
) {
    class Builder(private val clientToken: String, private val workflowTemplateId: String) {
        private var sessionToken: String? = null
        private var merchantUserId: String? = null
        private var phone: String? = null
        private var startFresh: Boolean? = null
        private var preVerified: Map<String, Boolean>? = null
        private var accumulatedData: JSONObject? = null
        private var aadhaarConfig: IDtoAadhaarConfig? = null
        private var referenceName: String? = null
        private var nameMatchConfig: IDtoNameMatchConfig? = null
        private var faceMatchReferenceImage: String? = null
        private var faceMatchConfig: IDtoFaceMatchConfig? = null
        private var panConfig: IDtoPanConfig? = null
        private var businessName: String? = null
        private var logo: String? = null
        private var language: IDtoLanguage? = null
        private var theme: IDtoTheme? = null
        private var displayMode: IDtoDisplayMode = IDtoDisplayMode.FULL_SCREEN
        private var bottomSheet: IDtoBottomSheet? = null
        private var env: IDtoEnv? = null
        private var baseUrl: String? = null
        private var colors: IDtoColors? = null
        private var debug: Boolean = false
        private var readyTimeoutMs: Long = Defaults.READY_TIMEOUT_MS
        private var allowedHosts: List<String>? = null

        fun sessionToken(v: String) = apply { sessionToken = v }
        fun merchantUserId(v: String) = apply { merchantUserId = v }
        fun phone(v: String) = apply { phone = v }
        fun startFresh(v: Boolean) = apply { startFresh = v }
        fun preVerified(v: Map<String, Boolean>) = apply { preVerified = v }
        fun accumulatedData(v: JSONObject) = apply { accumulatedData = v }
        fun aadhaarConfig(v: IDtoAadhaarConfig) = apply { aadhaarConfig = v }
        fun referenceName(v: String) = apply { referenceName = v }
        fun nameMatchConfig(v: IDtoNameMatchConfig) = apply { nameMatchConfig = v }
        fun faceMatchReferenceImage(v: String) = apply { faceMatchReferenceImage = v }
        fun faceMatchConfig(v: IDtoFaceMatchConfig) = apply { faceMatchConfig = v }
        fun panConfig(v: IDtoPanConfig) = apply { panConfig = v }
        fun businessName(v: String) = apply { businessName = v }
        fun logo(url: String) = apply { logo = url }
        fun language(v: IDtoLanguage) = apply { language = v }
        fun theme(v: IDtoTheme) = apply { theme = v }
        fun displayMode(v: IDtoDisplayMode) = apply { displayMode = v }
        fun bottomSheet(v: IDtoBottomSheet) = apply { bottomSheet = v }
        fun env(v: IDtoEnv) = apply { env = v }
        fun baseUrl(v: String) = apply { baseUrl = v }
        fun colors(v: IDtoColors) = apply { colors = v }
        fun debug(v: Boolean) = apply { debug = v }
        fun readyTimeoutMs(v: Long) = apply { readyTimeoutMs = v }
        fun allowedHosts(v: List<String>) = apply { allowedHosts = v }

        fun build() = IDtoConfig(
            clientToken, workflowTemplateId, sessionToken, merchantUserId, phone, startFresh,
            preVerified, accumulatedData, aadhaarConfig, referenceName, nameMatchConfig,
            faceMatchReferenceImage, faceMatchConfig, panConfig, businessName, logo, language,
            theme, displayMode, bottomSheet, env, baseUrl, colors, debug, readyTimeoutMs, allowedHosts,
        )
    }
}
