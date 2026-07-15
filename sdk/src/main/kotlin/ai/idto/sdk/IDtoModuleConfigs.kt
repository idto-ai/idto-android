package ai.idto.sdk

import org.json.JSONArray
import org.json.JSONObject

enum class IDtoLivenessPolicy(val wire: String) {
    FAIL_OPEN("fail_open"),
    FAIL_CLOSED("fail_closed"),
    NEEDS_REVIEW("needs_review"),
}

enum class IDtoDecisionMode(val wire: String) {
    ALL("all"),
    ANY("any"),
}

enum class IDtoComparePair(val wire: String) {
    AADHAAR_VS_PAN("aadhaar_vs_pan"),
    AADHAAR_VS_DL("aadhaar_vs_dl"),
    AADHAAR_VS_BANK("aadhaar_vs_bank"),
    PAN_VS_BANK("pan_vs_bank"),
}

class IDtoColors private constructor(
    val background: String?,
    val text: String?,
    val text2: String?,
    val border: String?,
    val primary: String?,
    val secondary: String?,
    val buttonTextColorPrimary: String?,
    val buttonTextColorSecondary: String?,
) {
    fun toJson(): JSONObject = JSONObject().apply {
        putOpt("background", background)
        putOpt("text", text)
        putOpt("text2", text2)
        putOpt("border", border)
        putOpt("primary", primary)
        putOpt("secondary", secondary)
        putOpt("buttonTextColor_primary", buttonTextColorPrimary)
        putOpt("buttonTextColor_secondary", buttonTextColorSecondary)
    }

    class Builder {
        private var background: String? = null
        private var text: String? = null
        private var text2: String? = null
        private var border: String? = null
        private var primary: String? = null
        private var secondary: String? = null
        private var buttonTextColorPrimary: String? = null
        private var buttonTextColorSecondary: String? = null

        fun background(v: String) = apply { background = v }
        fun text(v: String) = apply { text = v }
        fun text2(v: String) = apply { text2 = v }
        fun border(v: String) = apply { border = v }
        fun primary(v: String) = apply { primary = v }
        fun secondary(v: String) = apply { secondary = v }
        fun buttonTextColorPrimary(v: String) = apply { buttonTextColorPrimary = v }
        fun buttonTextColorSecondary(v: String) = apply { buttonTextColorSecondary = v }
        fun build() = IDtoColors(
            background, text, text2, border, primary, secondary,
            buttonTextColorPrimary, buttonTextColorSecondary,
        )
    }
}

class IDtoAadhaarConfig private constructor(
    val digilockerMaxFailures: Int?,
    val okycEnabled: Boolean?,
) {
    fun toJson(): JSONObject = JSONObject().apply {
        putOpt("digilockerMaxFailures", digilockerMaxFailures)
        putOpt("okycEnabled", okycEnabled)
    }

    class Builder {
        private var digilockerMaxFailures: Int? = null
        private var okycEnabled: Boolean? = null
        fun digilockerMaxFailures(v: Int) = apply { digilockerMaxFailures = v }
        fun okycEnabled(v: Boolean) = apply { okycEnabled = v }
        fun build() = IDtoAadhaarConfig(digilockerMaxFailures, okycEnabled)
    }
}

class IDtoFaceMatchConfig private constructor(
    val skipLiveness: Boolean?,
    val threshold: Int?,
    val livenessFailurePolicy: IDtoLivenessPolicy?,
) {
    fun toJson(): JSONObject = JSONObject().apply {
        putOpt("skipLiveness", skipLiveness)
        putOpt("threshold", threshold)
        putOpt("livenessFailurePolicy", livenessFailurePolicy?.wire)
    }

    class Builder {
        private var skipLiveness: Boolean? = null
        private var threshold: Int? = null
        private var livenessFailurePolicy: IDtoLivenessPolicy? = null
        fun skipLiveness(v: Boolean) = apply { skipLiveness = v }
        fun threshold(v: Int) = apply { threshold = v }
        fun livenessFailurePolicy(v: IDtoLivenessPolicy) = apply { livenessFailurePolicy = v }
        fun build() = IDtoFaceMatchConfig(skipLiveness, threshold, livenessFailurePolicy)
    }
}

class IDtoPanConfig private constructor(
    val skipContextScreen: Boolean?,
) {
    fun toJson(): JSONObject = JSONObject().apply {
        putOpt("skipContextScreen", skipContextScreen)
    }

    class Builder {
        private var skipContextScreen: Boolean? = null
        fun skipContextScreen(v: Boolean) = apply { skipContextScreen = v }
        fun build() = IDtoPanConfig(skipContextScreen)
    }
}

class IDtoNameMatchConfig private constructor(
    val threshold: Int?,
    val decisionMode: IDtoDecisionMode?,
    val extraHonorifics: List<String>?,
    val aliases: Map<String, String>?,
    val comparePairs: List<IDtoComparePair>?,
) {
    fun toJson(): JSONObject = JSONObject().apply {
        putOpt("threshold", threshold)
        putOpt("decision_mode", decisionMode?.wire)
        extraHonorifics?.let { put("extra_honorifics", JSONArray(it)) }
        aliases?.let { put("aliases", JSONObject(it)) }
        comparePairs?.let { put("compare_pairs", JSONArray(it.map(IDtoComparePair::wire))) }
    }

    class Builder {
        private var threshold: Int? = null
        private var decisionMode: IDtoDecisionMode? = null
        private var extraHonorifics: List<String>? = null
        private var aliases: Map<String, String>? = null
        private var comparePairs: List<IDtoComparePair>? = null
        fun threshold(v: Int) = apply { threshold = v }
        fun decisionMode(v: IDtoDecisionMode) = apply { decisionMode = v }
        fun extraHonorifics(v: List<String>) = apply { extraHonorifics = v }
        fun aliases(v: Map<String, String>) = apply { aliases = v }
        fun comparePairs(v: List<IDtoComparePair>) = apply { comparePairs = v }
        fun build() = IDtoNameMatchConfig(threshold, decisionMode, extraHonorifics, aliases, comparePairs)
    }
}

class IDtoBottomSheet private constructor(
    val minHeight: Any?,
    val maxHeight: Any?,
) {
    class Builder {
        private var minHeight: Any? = null
        private var maxHeight: Any? = null
        fun minHeight(v: String) = apply { minHeight = v }
        fun minHeight(v: Int) = apply { minHeight = v }
        fun maxHeight(v: String) = apply { maxHeight = v }
        fun maxHeight(v: Int) = apply { maxHeight = v }
        fun build() = IDtoBottomSheet(minHeight, maxHeight)
    }
}
