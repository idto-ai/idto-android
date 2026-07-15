package ai.idto.sdk.internal

import org.json.JSONArray
import org.json.JSONObject

object BridgeJs {

    const val CLOSE_JS: String = "window.IDtoSDK && window.IDtoSDK.close(); true;"

    val POPUP_OPENER_BRIDGE_JS: String = """
(function(){
  function send(type, payload){ try { window.IDtoAndroid.postMessage(JSON.stringify({ type: type, payload: payload })); } catch (e) {} }
  var opener = {
    closed: false,
    focus: function(){}, blur: function(){},
    postMessage: function(data){ send('opener', { data: data, origin: (window.location && window.location.origin) || '' }); }
  };
  try { Object.defineProperty(window, 'opener', { configurable: true, get: function(){ return opener; } }); }
  catch (e) { try { window.opener = opener; } catch (e2) {} }
  var _close = window.close ? window.close.bind(window) : function(){};
  window.close = function(){ send('popupClose', {}); try { _close(); } catch (e) {} };
})(); true;
""".trim()

    fun tokenReplyJs(token: String): String =
        dispatchMessageJs(JSONObject().put("type", GET_TOKEN_RESPONSE).put("token", token).toString())

    fun tokenErrorJs(error: String): String =
        dispatchMessageJs(JSONObject().put("type", GET_TOKEN_RESPONSE).put("error", error).toString())

    fun openerRelayJs(data: Any?, origin: String): String =
        "try{window.dispatchEvent(new MessageEvent('message',{data:${encode(data)},origin:${JSONObject.quote(origin)}}));}catch(e){}; true;"

    fun popupCloseSignalJs(): String =
        "try{var w=window.__IDTO_POPUP__;if(w)w.closed=true;" +
            "window.dispatchEvent(new Event(\"focus\"));" +
            "document.dispatchEvent(new Event(\"visibilitychange\"));}catch(e){}; true;"

    private const val GET_TOKEN_RESPONSE = "idto:getToken:response"

    private fun dispatchMessageJs(dataJson: String): String =
        "try{window.dispatchEvent(new MessageEvent('message',{data:$dataJson}));}catch(e){}; true;"

    private fun encode(value: Any?): String = when (value) {
        null -> "null"
        is JSONObject -> value.toString()
        is JSONArray -> value.toString()
        is Boolean, is Number -> value.toString()
        is String -> JSONObject.quote(value)
        else -> JSONObject.quote(value.toString())
    }
}
