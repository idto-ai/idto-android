package ai.idto.sdk.internal

import ai.idto.sdk.IDtoEnv

object Shell {

    private const val PROD_SCRIPT = "https://idto-sdk-bucket.s3.ap-south-1.amazonaws.com/sdk/prod/idto.js"
    private const val DEV_SCRIPT = "https://idto-sdk-bucket.s3.ap-south-1.amazonaws.com/sdk/dev/idto.js"

    fun scriptUrlForEnv(env: IDtoEnv?): String = when (env) {
        IDtoEnv.DEVELOPMENT -> DEV_SCRIPT
        else -> PROD_SCRIPT
    }

    fun buildShellHtml(env: IDtoEnv?, configInjection: String, debug: Boolean): String {
        val scriptUrl = scriptUrlForEnv(env)
        return """<!doctype html>
<html>
<head>
<meta charset="utf-8" />
<meta name="viewport" content="width=device-width, initial-scale=1, maximum-scale=1, user-scalable=no, interactive-widget=resizes-content" />
<style>html,body{margin:0;padding:0;height:100%;background:transparent;}
:root{--sat:0px;--sab:0px;--sal:0px;--sar:0px;}</style>
</head>
<body>
<script>
${DownloadInterceptorJs.build()}
</script>
<script>
$configInjection
</script>
<script>
(function () {
  var __idtoQueue = [];
  function __idtoDeliver(msg) {
    if (window.IDtoAndroid && window.IDtoAndroid.postMessage) { window.IDtoAndroid.postMessage(msg); return true; }
    return false;
  }
  function __idtoFlush() { while (__idtoQueue.length && __idtoDeliver(__idtoQueue[0])) { __idtoQueue.shift(); } }
  function post(type, payload) {
    var msg = JSON.stringify({ type: type, payload: payload });
    if (!__idtoDeliver(msg)) { __idtoQueue.push(msg); }
  }
  setInterval(__idtoFlush, 50);

  window.open = function (url) {
    try { if (url) post('open', { url: String(url) }); } catch (e) {}
    var stub = {
      closed: false,
      close: function () { stub.closed = true; },
      focus: function () {},
      blur: function () {},
      postMessage: function () {}
    };
    window.__IDTO_POPUP__ = stub;
    return stub;
  };
${debugBlock(debug)}
  function makeGetToken() {
    return function () {
      return new Promise(function (resolve, reject) {
        var settled = false;
        function cleanup() { window.removeEventListener('message', onMsg); }
        var timer = setTimeout(function () {
          if (settled) return;
          settled = true;
          cleanup();
          reject(new Error('token_timeout'));
        }, 35000);
        function onMsg(ev) {
          var data;
          try { data = typeof ev.data === 'string' ? JSON.parse(ev.data) : ev.data; } catch (e) { return; }
          if (!data || data.type !== 'idto:getToken:response') return;
          if (settled) return;
          settled = true;
          clearTimeout(timer);
          cleanup();
          if (data.error || !data.token) { reject(new Error(data.error || 'token_refresh_failed')); }
          else { resolve(data.token); }
        }
        window.addEventListener('message', onMsg);
        post('idto:getToken');
      });
    };
  }
  var __getToken = makeGetToken();
  window.__IDTO_ANDROID_GET_TOKEN__ = __getToken;

  function openWidget() {
    var cfg = window.__IDTO_ANDROID_CONFIG__ || {};
    cfg.getToken = __getToken;
    cfg.onWorkflowComplete = function (data) { post('workflowComplete', data); };
    cfg.onStepComplete = function (data) { post('stepComplete', data); };
    cfg.onError = function (data) { post('error', data); };
    cfg.onAbandon = function (data) { post('abandon', data); };
    cfg.onClose = function () { post('close'); };
    try {
      window.IDtoSDK.open(cfg);
      post('ready');
    } catch (e) {
      post('error', { step: 'init', error: 'unknown_error', session_token: '' });
      post('log', 'open() threw: ' + (e && e.message));
    }
  }

  var s = document.createElement('script');
  s.src = "$scriptUrl";
  s.onload = function () {
    if (window.IDtoSDK && typeof window.IDtoSDK.open === 'function') {
      openWidget();
    } else {
      post('error', { step: 'init', error: 'unknown_error', session_token: '' });
    }
  };
  s.onerror = function () {
    post('error', { step: 'init', error: 'network_error', session_token: '' });
  };
  document.body.appendChild(s);
})();
</script>
</body>
</html>"""
    }

    private fun debugBlock(debug: Boolean): String = if (!debug) "" else """
  var _log = console.log, _err = console.error;
  console.log = function () { try { post('log', Array.prototype.slice.call(arguments).join(' ')); } catch (e) {} _log.apply(console, arguments); };
  console.error = function () { try { post('log', Array.prototype.slice.call(arguments).join(' ')); } catch (e) {} _err.apply(console, arguments); };
  window.onerror = function (msg, src, line, col) { post('log', 'window.onerror: ' + msg + ' @' + line + ':' + col); };
"""
}
