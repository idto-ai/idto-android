package ai.idto.sdk

import org.json.JSONObject
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

class EventModelsTest {

    @Test fun stepCompleteAllSixFields() {
        val payload = JSONObject(
            """{"step":"pan","result":{"ok":true},"accumulated_data":{"a":1},
               "session_token":"st","credits_deducted":2.5,"balance_remaining":97.5}"""
        )
        val d = IDtoStepCompleteData.fromJson(payload)
        assertEquals("pan", d.step)
        assertEquals(true, d.result.getBoolean("ok"))
        assertEquals(1, d.accumulatedData.getInt("a"))
        assertEquals("st", d.sessionToken)
        assertEquals(2.5, d.creditsDeducted, 0.0)
        assertEquals(97.5, d.balanceRemaining, 0.0)
    }

    @Test fun stepCompleteNumbersAreDouble() {
        val d = IDtoStepCompleteData.fromJson(JSONObject("""{"credits_deducted":3,"balance_remaining":10}"""))
        assertEquals(3.0, d.creditsDeducted, 0.0)
        assertEquals(10.0, d.balanceRemaining, 0.0)
    }

    @Test fun stepCompleteMissingFieldsDefaultSafely() {
        val d = IDtoStepCompleteData.fromJson(JSONObject("{}"))
        assertEquals("", d.step)
        assertEquals(0, d.result.length())
        assertEquals(0, d.accumulatedData.length())
        assertEquals("", d.sessionToken)
        assertEquals(0.0, d.creditsDeducted, 0.0)
        assertEquals(0.0, d.balanceRemaining, 0.0)
    }

    @Test fun stepCompleteNullPayloadDefaultsSafely() {
        val d = IDtoStepCompleteData.fromJson(null)
        assertEquals("", d.step)
        assertEquals(0, d.accumulatedData.length())
    }

    @Test fun workflowCompleteAllFields() {
        val payload = JSONObject(
            """{"all_steps":[{"s":"pan"},{"s":"dl"}],"accumulated_data":{"x":9},"session_token":"st"}"""
        )
        val d = IDtoWorkflowCompleteData.fromJson(payload)
        assertEquals(2, d.allSteps.length())
        assertEquals("pan", d.allSteps.getJSONObject(0).getString("s"))
        assertEquals(9, d.accumulatedData.getInt("x"))
        assertEquals("st", d.sessionToken)
    }

    @Test fun workflowCompleteMissingFieldsDefaultSafely() {
        val d = IDtoWorkflowCompleteData.fromJson(JSONObject("{}"))
        assertEquals(0, d.allSteps.length())
        assertEquals(0, d.accumulatedData.length())
        assertEquals("", d.sessionToken)
    }

    @Test fun abandonAllFields() {
        val d = IDtoAbandonData.fromJson(JSONObject("""{"at_step":"pan","reason":"user","session_token":"st"}"""))
        assertEquals("pan", d.atStep)
        assertEquals("user", d.reason)
        assertEquals("st", d.sessionToken)
    }

    @Test fun abandonMissingFieldsDefaultSafely() {
        val d = IDtoAbandonData.fromJson(JSONObject("{}"))
        assertEquals("", d.atStep)
        assertEquals("", d.reason)
        assertEquals("", d.sessionToken)
    }

    @Test fun errorAllFields() {
        val d = IDtoErrorData.fromJson(JSONObject("""{"step":"init","error":"network_error","session_token":"st"}"""))
        assertEquals("init", d.step)
        assertEquals("network_error", d.error)
        assertEquals("st", d.sessionToken)
    }

    @Test fun errorEmptySessionTokenTolerated() {
        val d = IDtoErrorData.fromJson(JSONObject("""{"step":"pan","error":"boom"}"""))
        assertEquals("pan", d.step)
        assertEquals("boom", d.error)
        assertEquals("", d.sessionToken)
    }

    @Test fun listenerHasDefaultMethods() {
        val listener = object : IDtoEventListener {}
        listener.onStepComplete(IDtoStepCompleteData.fromJson(JSONObject("{}")))
        listener.onWorkflowComplete(IDtoWorkflowCompleteData.fromJson(JSONObject("{}")))
        listener.onAbandon(IDtoAbandonData.fromJson(JSONObject("{}")))
        listener.onError(IDtoErrorData.fromJson(JSONObject("{}")))
        listener.onClose()
        assertTrue(true)
    }
}
