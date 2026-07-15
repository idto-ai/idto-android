package ai.idto.sdk.internal

import ai.idto.sdk.IDtoAbandonData
import ai.idto.sdk.IDtoErrorData
import ai.idto.sdk.IDtoStepCompleteData
import ai.idto.sdk.IDtoWorkflowCompleteData
import org.json.JSONObject

sealed class BridgeMessage {
    object Ready : BridgeMessage()
    object Close : BridgeMessage()
    data class StepComplete(val data: IDtoStepCompleteData) : BridgeMessage()
    data class WorkflowComplete(val data: IDtoWorkflowCompleteData) : BridgeMessage()
    data class Abandon(val data: IDtoAbandonData) : BridgeMessage()
    data class Error(val data: IDtoErrorData) : BridgeMessage()
    data class Log(val message: String) : BridgeMessage()
    data class Open(val url: String) : BridgeMessage()
    data class Download(val filename: String, val mime: String, val base64: String) : BridgeMessage()

    fun isTerminal(): Boolean = when (this) {
        is Close -> true
        is Error -> data.step == "init" || data.error == "insufficient_credits"
        else -> false
    }
}

sealed class PopupMessage {
    data class Opener(val data: Any?, val origin: String) : PopupMessage()
    object Close : PopupMessage()
}

object BridgeMessages {

    private const val TOKEN_REQUEST_TYPE = "idto:getToken"

    fun parse(raw: String): BridgeMessage? {
        val root = raw.toJsonObjectOrNull() ?: return null
        val type = root.optStringOrNull("type") ?: return null
        val payload = root.optJSONObject("payload")
        return when (type) {
            "ready" -> BridgeMessage.Ready
            "close" -> BridgeMessage.Close
            "stepComplete" -> BridgeMessage.StepComplete(IDtoStepCompleteData.fromJson(payload))
            "workflowComplete" -> BridgeMessage.WorkflowComplete(IDtoWorkflowCompleteData.fromJson(payload))
            "abandon" -> BridgeMessage.Abandon(IDtoAbandonData.fromJson(payload))
            "error" -> BridgeMessage.Error(IDtoErrorData.fromJson(payload))
            "log" -> BridgeMessage.Log(root.optString("payload", ""))
            "open" -> BridgeMessage.Open((payload ?: JSONObject()).optString("url", ""))
            "download" -> {
                val p = payload ?: JSONObject()
                BridgeMessage.Download(
                    filename = p.optString("filename", ""),
                    mime = p.optString("mime", ""),
                    base64 = p.optString("base64", ""),
                )
            }
            else -> null
        }
    }

    fun isTokenRequest(raw: String): Boolean =
        raw.toJsonObjectOrNull()?.optStringOrNull("type") == TOKEN_REQUEST_TYPE

    fun parsePopup(raw: String): PopupMessage? {
        val root = raw.toJsonObjectOrNull() ?: return null
        return when (root.optStringOrNull("type")) {
            "opener" -> {
                val p = root.optJSONObject("payload") ?: JSONObject()
                val data = p.opt("data")
                PopupMessage.Opener(if (data == JSONObject.NULL) null else data, p.optString("origin", ""))
            }
            "popupClose" -> PopupMessage.Close
            else -> null
        }
    }

    private fun String.toJsonObjectOrNull(): JSONObject? =
        try { JSONObject(this) } catch (e: Exception) { null }

    private fun JSONObject.optStringOrNull(key: String): String? =
        if (opt(key) is String) getString(key) else null
}
