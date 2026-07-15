package ai.idto.sdk

import org.json.JSONArray
import org.json.JSONObject

class IDtoStepCompleteData(
    val step: String,
    val result: JSONObject,
    val accumulatedData: JSONObject,
    val sessionToken: String,
    val creditsDeducted: Double,
    val balanceRemaining: Double,
) {
    companion object {
        fun fromJson(payload: JSONObject?): IDtoStepCompleteData {
            val p = payload ?: JSONObject()
            return IDtoStepCompleteData(
                step = p.optString("step", ""),
                result = p.optJSONObject("result") ?: JSONObject(),
                accumulatedData = p.optJSONObject("accumulated_data") ?: JSONObject(),
                sessionToken = p.optString("session_token", ""),
                creditsDeducted = p.optDouble("credits_deducted", 0.0),
                balanceRemaining = p.optDouble("balance_remaining", 0.0),
            )
        }
    }
}

class IDtoWorkflowCompleteData(
    val allSteps: JSONArray,
    val accumulatedData: JSONObject,
    val sessionToken: String,
) {
    companion object {
        fun fromJson(payload: JSONObject?): IDtoWorkflowCompleteData {
            val p = payload ?: JSONObject()
            return IDtoWorkflowCompleteData(
                allSteps = p.optJSONArray("all_steps") ?: JSONArray(),
                accumulatedData = p.optJSONObject("accumulated_data") ?: JSONObject(),
                sessionToken = p.optString("session_token", ""),
            )
        }
    }
}

class IDtoAbandonData(
    val atStep: String,
    val reason: String,
    val sessionToken: String,
) {
    companion object {
        fun fromJson(payload: JSONObject?): IDtoAbandonData {
            val p = payload ?: JSONObject()
            return IDtoAbandonData(
                atStep = p.optString("at_step", ""),
                reason = p.optString("reason", ""),
                sessionToken = p.optString("session_token", ""),
            )
        }
    }
}

class IDtoErrorData(
    val step: String,
    val error: String,
    val sessionToken: String,
) {
    companion object {
        fun fromJson(payload: JSONObject?): IDtoErrorData {
            val p = payload ?: JSONObject()
            return IDtoErrorData(
                step = p.optString("step", ""),
                error = p.optString("error", ""),
                sessionToken = p.optString("session_token", ""),
            )
        }
    }
}

interface IDtoEventListener {
    fun onStepComplete(data: IDtoStepCompleteData) {}
    fun onWorkflowComplete(data: IDtoWorkflowCompleteData) {}
    fun onAbandon(data: IDtoAbandonData) {}
    fun onError(data: IDtoErrorData) {}
    fun onClose() {}
}

fun interface IDtoTokenProvider {
    fun getToken(callback: IDtoTokenCallback)
}

interface IDtoTokenCallback {
    fun onToken(token: String)
    fun onError(error: Throwable)
}
