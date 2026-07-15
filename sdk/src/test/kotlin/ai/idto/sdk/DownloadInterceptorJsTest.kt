package ai.idto.sdk

import ai.idto.sdk.internal.DownloadInterceptorJs
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class DownloadInterceptorJsTest {

    private val js = DownloadInterceptorJs.build()

    @Test fun idempotencyGuardCheckedFirst() {
        assertTrue(js.contains("if (window.__IDTO_DL_HOOKED__) return;"))
        assertTrue(js.contains("window.__IDTO_DL_HOOKED__ = true;"))
        assertTrue(js.indexOf("if (window.__IDTO_DL_HOOKED__) return;") < js.indexOf("window.__IDTO_DL_HOOKED__ = true;"))
    }

    @Test fun patchesBothUrlAndWebkitUrl() {
        assertTrue(js.contains("patchURLClass(window.URL);"))
        assertTrue(js.contains("patchURLClass(window.webkitURL);"))
        assertTrue(js.contains("obj instanceof Blob"))
    }

    @Test fun revokeDeferredByFiveThousand() {
        assertTrue(js.contains("setTimeout(function () { try { delete blobs[url]; } catch (e) {} }, 5000)"))
    }

    @Test fun patchesAnchorClickAndCaptureListener() {
        assertTrue(js.contains("HTMLAnchorElement.prototype.click = function"))
        assertTrue(js.contains("document.addEventListener('click', function (ev) {"))
        assertTrue(js.contains("}, true);"))
    }

    @Test fun ceilingIsVerbatimRnForm() {
        assertTrue(js.contains("var MAX_DL_BYTES = 50 * 1024 * 1024;"))
        assertTrue(js.contains("blob.size > MAX_DL_BYTES"))
        assertTrue(js.contains("download skipped: blob too large"))
    }

    @Test fun blobPathUsesRetainedElseFetch() {
        assertTrue(js.contains("if (href.indexOf('blob:') === 0 && blobs[href]) {"))
        assertTrue(js.contains("window.fetch(href).then(function (r) { return r.blob(); })"))
    }

    @Test fun fileReaderDataUrlStripAndDownloadPost() {
        assertTrue(js.contains("reader.readAsDataURL(blob);"))
        assertTrue(js.contains("var comma = s.indexOf(',');"))
        assertTrue(js.contains("post('download', { filename: filename, mime: blob.type || 'application/octet-stream', base64: base64 });"))
    }

    @Test fun filenameFallbackOrder() {
        assertTrue(js.contains("if (dl) return String(dl);"))
        assertTrue(js.contains("return decodeURIComponent(last);"))
        assertTrue(js.contains("return 'report.pdf';"))
    }

    @Test fun postsToAndroidBridgeNotReactNative() {
        assertTrue(js.contains("window.IDtoAndroid.postMessage"))
        assertFalse(js.contains("ReactNativeWebView"))
    }

    @Test fun isEs5NoArrowNoTemplateNoLetConst() {
        assertFalse(Regex("=>").containsMatchIn(js))
        assertFalse(js.contains("`"))
        assertFalse(Regex("\\blet\\b").containsMatchIn(js))
        assertFalse(Regex("\\bconst\\b").containsMatchIn(js))
    }

    @Test fun isSelfInvokingInstaller() {
        assertTrue(js.trimStart().startsWith("(function installDownloadHook()"))
        assertTrue(js.trimEnd().endsWith("})();"))
    }
}
