package ai.idto.sdk

import ai.idto.sdk.internal.DownloadInterceptorJs
import ai.idto.sdk.internal.Shell
import ai.idto.sdk.internal.WireConfig
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class ShellTest {

    private val config = IDtoConfig.Builder("ct-123", "wf-456").build()
    private val injection = WireConfig.buildConfigInjection(config)

    private fun html(env: IDtoEnv? = IDtoEnv.PRODUCTION, debug: Boolean = false) =
        Shell.buildShellHtml(env, injection, debug)

    @Test fun cdnUrlProduction() =
        assertTrue(html(IDtoEnv.PRODUCTION).contains("https://idto-sdk-bucket.s3.ap-south-1.amazonaws.com/sdk/prod/idto.js"))

    @Test fun cdnUrlDevelopment() =
        assertTrue(html(IDtoEnv.DEVELOPMENT).contains("https://idto-sdk-bucket.s3.ap-south-1.amazonaws.com/sdk/dev/idto.js"))

    @Test fun cdnUrlNullDefaultsToProd() =
        assertTrue(html(null).contains("/sdk/prod/idto.js"))

    @Test fun scriptUrlForEnvHelper() {
        assertEquals("https://idto-sdk-bucket.s3.ap-south-1.amazonaws.com/sdk/prod/idto.js", Shell.scriptUrlForEnv(IDtoEnv.PRODUCTION))
        assertEquals("https://idto-sdk-bucket.s3.ap-south-1.amazonaws.com/sdk/dev/idto.js", Shell.scriptUrlForEnv(IDtoEnv.DEVELOPMENT))
        assertEquals("https://idto-sdk-bucket.s3.ap-south-1.amazonaws.com/sdk/prod/idto.js", Shell.scriptUrlForEnv(null))
    }

    @Test fun downloadInterceptorBeforeLoader() {
        val h = html()
        assertTrue(h.contains("__IDTO_DL_HOOKED__"))
        assertTrue(h.indexOf("__IDTO_DL_HOOKED__") < h.indexOf("document.createElement('script')"))
    }

    @Test fun downloadInterceptorInjectedFirst() {
        val h = html()
        assertTrue(h.indexOf(DownloadInterceptorJs.build()) < h.indexOf("__IDTO_ANDROID_CONFIG__"))
    }

    @Test fun configInjectionPresentAndBeforeLoader() {
        val h = html()
        assertTrue(h.contains(injection))
        assertTrue(h.indexOf(injection) < h.indexOf("document.createElement('script')"))
    }

    @Test fun postTargetsAndroidBridgeWithTypePayloadShape() {
        val h = html()
        assertTrue(h.contains("window.IDtoAndroid.postMessage"))
        assertTrue(h.contains("JSON.stringify({ type: type, payload: payload })"))
    }

    @Test fun queueAndFlushGuardPresent() {
        val h = html()
        assertTrue(h.contains("window.IDtoAndroid && window.IDtoAndroid.postMessage"))
        assertTrue(h.contains(".push("))
    }

    @Test fun windowOpenShimPostsOpenAndInstallsPopupStub() {
        val h = html()
        assertTrue(h.contains("window.open = function"))
        assertTrue(h.contains("post('open'"))
        assertTrue(h.contains("window.__IDTO_POPUP__"))
        assertTrue(h.contains("closed: false"))
        assertTrue(h.contains("close: function"))
        assertTrue(h.contains("focus: function"))
        assertTrue(h.contains("blur: function"))
        assertTrue(h.contains("postMessage: function"))
    }

    @Test fun debugTrueWrapsConsole() {
        val h = html(debug = true)
        assertTrue(h.contains("console.log = function"))
        assertTrue(h.contains("console.error = function"))
        assertTrue(h.contains("window.onerror = function"))
    }

    @Test fun debugFalseNoConsoleWrapping() {
        val h = html(debug = false)
        assertFalse(h.contains("console.log = function"))
        assertFalse(h.contains("console.error = function"))
    }

    @Test fun loaderOnErrorPostsNetworkError() {
        val h = html()
        assertTrue(h.contains("s.onerror = function"))
        assertTrue(h.contains("'network_error'"))
    }

    @Test fun loadedButNoOpenPostsUnknownError() {
        val h = html()
        assertTrue(h.contains("typeof window.IDtoSDK.open === 'function'"))
        assertTrue(h.contains("'unknown_error'"))
    }

    @Test fun openWidgetCatchPostsUnknownErrorAndLog() {
        val h = html()
        assertTrue(h.contains("open() threw"))
    }

    @Test fun openWidgetAttachesFiveCallbacksAndPostsReady() {
        val h = html()
        assertTrue(h.contains("cfg.onWorkflowComplete = function"))
        assertTrue(h.contains("cfg.onStepComplete = function"))
        assertTrue(h.contains("cfg.onError = function"))
        assertTrue(h.contains("cfg.onAbandon = function"))
        assertTrue(h.contains("cfg.onClose = function"))
        assertTrue(h.contains("post('ready')"))
        assertTrue(h.contains("window.__IDTO_ANDROID_CONFIG__ || {}"))
    }

    @Test fun getTokenAttachedBeforeOpen() {
        val h = html()
        assertTrue(h.contains("cfg.getToken"))
        assertTrue(h.indexOf("cfg.getToken") < h.indexOf("window.IDtoSDK.open(cfg)"))
    }

    @Test fun getTokenPromiseWiring() {
        val h = html()
        assertTrue(h.contains("new Promise"))
        assertTrue(h.contains("post('idto:getToken')"))
        assertTrue(h.contains("idto:getToken:response"))
        assertTrue(h.contains("addEventListener('message'"))
        assertTrue(h.contains("removeEventListener('message'"))
        assertTrue(h.contains("35000"))
    }

    @Test fun getTokenExposedAsGlobalHook() =
        assertTrue(html().contains("window.__IDTO_ANDROID_GET_TOKEN__"))

    @Test fun viewportMetaExact() {
        assertTrue(html().contains("<meta name=\"viewport\" content=\"width=device-width, initial-scale=1, maximum-scale=1, user-scalable=no, interactive-widget=resizes-content\" />"))
    }

    @Test fun safeAreaVarsZeroed() =
        assertTrue(html().contains(":root{--sat:0px;--sab:0px;--sal:0px;--sar:0px;}"))

    @Test fun singleDocumentNoExternalResourcesBesidesCdn() {
        val h = html()
        assertTrue(h.trimStart().startsWith("<!doctype html>"))
        assertFalse(h.contains("<link"))
        assertEquals(1, Regex("https://").findAll(h).count())
    }
}
