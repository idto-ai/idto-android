package ai.idto.sdk.internal

import ai.idto.sdk.IDtoConfig
import org.json.JSONObject

object WireConfig {

    private const val CONFIG_GLOBAL = "window.__IDTO_ANDROID_CONFIG__"
    private const val DEBUG_GLOBAL = "window.__IDTO_ANDROID_DEBUG__"
    private const val LINE_SEPARATOR = ' '
    private const val PARAGRAPH_SEPARATOR = ' '

    fun toWebConfig(config: IDtoConfig): JSONObject = JSONObject().apply {
        put("client_token", config.clientToken)
        put("workflow_template_id", config.workflowTemplateId)
        putOpt("session_token", config.sessionToken)
        putOpt("merchant_user_id", config.merchantUserId)
        putOpt("phone", config.phone)
        putOpt("start_fresh", config.startFresh)
        config.preVerified?.let { put("pre_verified", JSONObject(it as Map<*, *>)) }
        putOpt("accumulated_data", config.accumulatedData)
        putOpt("aadhaarConfig", config.aadhaarConfig?.toJson())
        putOpt("reference_name", config.referenceName)
        putOpt("nameMatchConfig", config.nameMatchConfig?.toJson())
        putOpt("faceMatchReferenceImage", config.faceMatchReferenceImage)
        putOpt("faceMatchConfig", config.faceMatchConfig?.toJson())
        putOpt("panConfig", config.panConfig?.toJson())
        putOpt("businessName", config.businessName)
        putOpt("logo", config.logo)
        putOpt("language", config.language?.wire)
        putOpt("theme", config.theme?.wire)
        putOpt("env", config.env?.wire)
        putOpt("baseUrl", config.baseUrl)
        putOpt("colors", config.colors?.toJson())
        put("displayMode", "full_screen")
    }

    fun buildConfigInjection(config: IDtoConfig): String {
        val json = htmlSafe(toWebConfig(config).toString())
        return "$CONFIG_GLOBAL = $json; $DEBUG_GLOBAL = ${config.debug};"
    }

    private fun htmlSafe(json: String): String = json
        .replace("<", "\\u003c")
        .replace(LINE_SEPARATOR.toString(), "\\u2028")
        .replace(PARAGRAPH_SEPARATOR.toString(), "\\u2029")
}
