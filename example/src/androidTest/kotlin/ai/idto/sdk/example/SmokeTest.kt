package ai.idto.sdk.example

import android.Manifest
import android.annotation.SuppressLint
import android.webkit.ValueCallback
import androidx.test.core.app.ActivityScenario
import androidx.test.ext.junit.runners.AndroidJUnit4
import androidx.test.platform.app.InstrumentationRegistry
import androidx.test.rule.GrantPermissionRule
import androidx.test.uiautomator.By
import androidx.test.uiautomator.UiDevice
import androidx.test.uiautomator.Until
import ai.idto.sdk.IDto
import ai.idto.sdk.IDtoTestHooks
import org.junit.After
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertTrue
import org.junit.Assume.assumeTrue
import org.junit.Before
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import java.io.File
import java.util.concurrent.CountDownLatch
import java.util.concurrent.TimeUnit

@RunWith(AndroidJUnit4::class)
@SuppressLint("RestrictedApi")
class SmokeTest {

    @get:Rule
    val permissions: GrantPermissionRule =
        GrantPermissionRule.grant(Manifest.permission.CAMERA, Manifest.permission.RECORD_AUDIO)

    private val device = UiDevice.getInstance(InstrumentationRegistry.getInstrumentation())
    private var scenario: ActivityScenario<MainActivity>? = null

    @Before
    fun setUp() {
        IDtoTestHooks.reset()
    }

    @After
    fun tearDown() {
        scenario?.close()
    }

    @Test
    fun liveSmokeReachesReadyAndClosesCleanly() {
        assumeTrue(
            "demo credentials not set — see example/README",
            DevCredentials.isConfigured,
        )
        scenario = ActivityScenario.launch(MainActivity::class.java)

        assertTrue(
            "landing hero not visible",
            device.wait(Until.hasObject(By.textContains(HERO_TITLE)), 10_000),
        )
        assertTrue(
            "landing CTA not visible",
            device.wait(Until.hasObject(By.textContains(CTA_IDLE)), 5_000),
        )

        screenshot("01-landing.png")

        val cta = device.findObject(By.textContains(CTA_IDLE))
        assertNotNull("CTA object not found", cta)
        cta.click()

        assertTrue(
            "CTA never left the idle state after tap (onStart did not fire)",
            device.wait(Until.gone(By.textContains(CTA_IDLE)), 12_000),
        )

        assertTrue(
            "WebView never attached within 25s",
            device.wait(Until.hasObject(By.clazz(WEBVIEW_CLASS)), 25_000),
        )
        Thread.sleep(2_000)
        screenshot("02-sheet-webview.png")

        val ready = pollFor(45_000) { IDtoTestHooks.lastEvent == "ready" }
        if (!ready) {
            throw AssertionError(
                "ready never fired within 45s. lastEvent=${IDtoTestHooks.lastEvent}. logs=\n" +
                    IDtoTestHooks.logs.joinToString("\n"),
            )
        }

        assertTrue(
            "init error dispatched instead of ready",
            IDtoTestHooks.lastEvent != "error",
        )
        assertTrue(
            "WebView detached after ready",
            device.hasObject(By.clazz(WEBVIEW_CLASS)),
        )

        val probeMarker = "probe:true"
        val probeJs =
            "window.__IDTO_ANDROID_GET_TOKEN__().then(function(t){" +
                "window.IDtoAndroid.postMessage(JSON.stringify({type:'log'," +
                "payload:'probe:' + (typeof t === 'string' && t.length > 0)}))})" +
                ".catch(function(e){window.IDtoAndroid.postMessage(JSON.stringify({type:'log'," +
                "payload:'probe:err:' + e}))})"
        val probeStarted = CountDownLatch(1)
        IDtoTestHooks.evaluateJs(probeJs, ValueCallback { probeStarted.countDown() })
        probeStarted.await(10, TimeUnit.SECONDS)

        val probed = pollFor(30_000) { IDtoTestHooks.logs.any { it.contains(probeMarker) } }
        if (!probed) {
            throw AssertionError(
                "token-refresh probe never returned $probeMarker within 30s. logs=\n" +
                    IDtoTestHooks.logs.joinToString("\n"),
            )
        }

        screenshot("03-ready-probed.png")

        device.pressBack()

        val closed = pollFor(5_000) { !IDto.isOpen() }
        Thread.sleep(1_000)
        screenshot("04-after-back.png")
        assertTrue(
            "close teardown did not complete within 5s (IDto.isOpen still true)",
            closed,
        )
    }

    private fun screenshot(name: String) {
        runCatching {
            val dir = File("/sdcard/Android/media/ai.idto.sdk.example/additional_test_output")
            dir.mkdirs()
            device.takeScreenshot(File(dir, name))
        }
    }

    private fun pollFor(timeoutMs: Long, condition: () -> Boolean): Boolean {
        val deadline = System.currentTimeMillis() + timeoutMs
        while (System.currentTimeMillis() < deadline) {
            if (condition()) return true
            Thread.sleep(200)
        }
        return condition()
    }

    private companion object {
        const val HERO_TITLE = "Verify your identity"
        const val CTA_IDLE = "Start verification"
        const val WEBVIEW_CLASS = "android.webkit.WebView"
    }
}
