package ai.idto.sdk

import ai.idto.sdk.internal.BridgeMessage
import ai.idto.sdk.internal.BridgeMessages
import org.json.JSONObject
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test

class BridgeMessagesTest {

    private fun parse(raw: String) = BridgeMessages.parse(raw)

    private fun msg(type: String, payload: Any? = null): String {
        val o = JSONObject().put("type", type)
        if (payload != null) o.put("payload", payload)
        return o.toString()
    }

    @Test fun invalidJsonIsNull() = assertNull(parse("not json {"))

    @Test fun missingTypeIsNull() = assertNull(parse("""{"payload":{}}"""))

    @Test fun nonStringTypeIsNull() = assertNull(parse("""{"type":42}"""))

    @Test fun unknownTypeIsNull() = assertNull(parse(msg("somethingElse")))

    @Test fun nonObjectIsNull() = assertNull(parse("""["ready"]"""))

    @Test fun readyParsesWithoutPayload() =
        assertTrue(parse(msg("ready")) is BridgeMessage.Ready)

    @Test fun closeParses() =
        assertTrue(parse(msg("close")) is BridgeMessage.Close)

    @Test fun logParsesStringPayload() {
        val m = parse(msg("log", "hello world")) as BridgeMessage.Log
        assertEquals("hello world", m.message)
    }

    @Test fun openParsesUrl() {
        val m = parse(msg("open", JSONObject().put("url", "https://digilocker.idto.ai/x"))) as BridgeMessage.Open
        assertEquals("https://digilocker.idto.ai/x", m.url)
    }

    @Test fun downloadParsesAllThreeFields() {
        val payload = JSONObject().put("filename", "r.pdf").put("mime", "application/pdf").put("base64", "AAAA")
        val m = parse(msg("download", payload)) as BridgeMessage.Download
        assertEquals("r.pdf", m.filename)
        assertEquals("application/pdf", m.mime)
        assertEquals("AAAA", m.base64)
    }

    @Test fun stepCompleteDecodesAllSixFieldsAsDouble() {
        val payload = JSONObject()
            .put("step", "pan")
            .put("result", JSONObject().put("ok", true))
            .put("accumulated_data", JSONObject().put("a", 1))
            .put("session_token", "st")
            .put("credits_deducted", 2.5)
            .put("balance_remaining", 97.5)
        val m = parse(msg("stepComplete", payload)) as BridgeMessage.StepComplete
        assertEquals("pan", m.data.step)
        assertEquals("st", m.data.sessionToken)
        assertEquals(2.5, m.data.creditsDeducted, 0.0)
        assertEquals(97.5, m.data.balanceRemaining, 0.0)
        assertTrue(m.data.result.getBoolean("ok"))
        assertEquals(1, m.data.accumulatedData.getInt("a"))
    }

    @Test fun abandonDecodesSnakeCaseKeys() {
        val payload = JSONObject().put("at_step", "aadhaar").put("reason", "user_exit").put("session_token", "st")
        val m = parse(msg("abandon", payload)) as BridgeMessage.Abandon
        assertEquals("aadhaar", m.data.atStep)
        assertEquals("user_exit", m.data.reason)
        assertEquals("st", m.data.sessionToken)
    }

    @Test fun errorDecodesWithEmptySessionTokenTolerated() {
        val payload = JSONObject().put("step", "init").put("error", "network_error")
        val m = parse(msg("error", payload)) as BridgeMessage.Error
        assertEquals("init", m.data.step)
        assertEquals("network_error", m.data.error)
        assertEquals("", m.data.sessionToken)
    }

    @Test fun workflowCompleteDecodesArrayAndData() {
        val payload = JSONObject()
            .put("all_steps", org.json.JSONArray().put("pan").put("aadhaar"))
            .put("accumulated_data", JSONObject().put("k", "v"))
            .put("session_token", "st")
        val m = parse(msg("workflowComplete", payload)) as BridgeMessage.WorkflowComplete
        assertEquals(2, m.data.allSteps.length())
        assertEquals("v", m.data.accumulatedData.getString("k"))
        assertEquals("st", m.data.sessionToken)
    }

    @Test fun missingPayloadFieldsDefaultSafely() {
        val m = parse(msg("stepComplete")) as BridgeMessage.StepComplete
        assertEquals("", m.data.step)
        assertEquals(0.0, m.data.creditsDeducted, 0.0)
        assertEquals(0, m.data.result.length())
    }

    @Test fun getTokenIsNotABridgeMessage() =
        assertNull(parse(msg("idto:getToken")))

    @Test fun isTokenRequestDetectsColonType() {
        assertTrue(BridgeMessages.isTokenRequest(msg("idto:getToken")))
        assertTrue(BridgeMessages.isTokenRequest("""  { "type" : "idto:getToken" }  """))
        assertFalse(BridgeMessages.isTokenRequest(msg("ready")))
        assertFalse(BridgeMessages.isTokenRequest("not json"))
    }

    @Test fun closeIsTerminal() =
        assertTrue((parse(msg("close")) as BridgeMessage).isTerminal())

    @Test fun workflowCompleteIsNotTerminal() {
        val payload = JSONObject().put("all_steps", org.json.JSONArray())
        assertFalse((parse(msg("workflowComplete", payload)) as BridgeMessage).isTerminal())
    }

    @Test fun errorTerminalOnlyForInitOrInsufficientCredits() {
        fun err(step: String, error: String): Boolean {
            val p = JSONObject().put("step", step).put("error", error)
            return (parse(msg("error", p)) as BridgeMessage).isTerminal()
        }
        assertTrue(err("init", "network_error"))
        assertTrue(err("pan", "insufficient_credits"))
        assertFalse(err("aadhaar", "session_expired"))
    }

    @Test fun readyAndLogAreNotTerminal() {
        assertFalse((parse(msg("ready")) as BridgeMessage).isTerminal())
        assertFalse((parse(msg("log", "x")) as BridgeMessage).isTerminal())
    }
}
