package ai.idto.sdk

import ai.idto.sdk.internal.BridgeJs
import org.json.JSONObject
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class BridgeJsTest {

    @Test fun closeJsExactByteMatch() =
        assertEquals("window.IDtoSDK && window.IDtoSDK.close(); true;", BridgeJs.CLOSE_JS)

    @Test fun tokenReplyDispatchesResponseWithToken() {
        val js = BridgeJs.tokenReplyJs("tkn-1")
        assertTrue(js.contains("new MessageEvent('message'"))
        val expected = JSONObject().put("type", "idto:getToken:response").put("token", "tkn-1").toString()
        assertTrue(js.contains(expected))
        assertTrue(js.trimEnd().endsWith("; true;"))
    }

    @Test fun tokenReplyEscapesQuotesAndBackslashes() {
        val nasty = "a\"b\\c"
        val js = BridgeJs.tokenReplyJs(nasty)
        val expected = JSONObject().put("type", "idto:getToken:response").put("token", nasty).toString()
        assertTrue(js.contains(expected))
    }

    @Test fun tokenErrorVariantsHaveErrorKeyNoTokenKey() {
        val np = BridgeJs.tokenErrorJs("no_token_provider")
        val rf = BridgeJs.tokenErrorJs("token_refresh_failed")
        assertTrue(np.contains("\"error\":\"no_token_provider\""))
        assertTrue(rf.contains("\"error\":\"token_refresh_failed\""))
        assertFalse(np.contains("\"token\""))
        assertFalse(rf.contains("\"token\""))
        assertTrue(np.contains("idto:getToken:response"))
    }

    @Test fun openerRelayReDispatchesDataAndOrigin() {
        val js = BridgeJs.openerRelayJs("payload-data", "https://digilocker.idto.ai")
        assertTrue(js.contains("new MessageEvent('message'"))
        assertTrue(js.contains(JSONObject.quote("payload-data")))
        assertTrue(js.contains(JSONObject.quote("https://digilocker.idto.ai")))
    }

    @Test fun openerRelayEscapesScriptAndQuotes() {
        val nasty = "</script>\"q\""
        val js = BridgeJs.openerRelayJs(nasty, "")
        assertTrue(js.contains(JSONObject.quote(nasty)))
        assertFalse(js.contains("</script>"))
    }

    @Test fun openerRelayNullDataSerializesAsNull() {
        val js = BridgeJs.openerRelayJs(null, "o")
        assertTrue(js.contains("data:null"))
    }

    @Test fun popupCloseSignalSetsClosedAndDispatchesEvents() {
        val js = BridgeJs.popupCloseSignalJs()
        assertTrue(js.contains("window.__IDTO_POPUP__"))
        assertTrue(js.contains("closed = true") || js.contains("closed=true"))
        assertTrue(js.contains("new Event(\"focus\")"))
        assertTrue(js.contains("visibilitychange"))
    }

    @Test fun popupOpenerBridgeShimsOpenerAndClose() {
        val js = BridgeJs.POPUP_OPENER_BRIDGE_JS
        assertTrue(js.contains("window.IDtoAndroid.postMessage"))
        assertTrue(js.contains("'opener'"))
        assertTrue(js.contains("window.opener"))
        assertTrue(js.contains("window.close = function"))
        assertTrue(js.contains("'popupClose'"))
        assertFalse(js.contains("ReactNativeWebView"))
    }
}
