package ai.idto.sdk

import android.Manifest
import android.app.Activity
import android.content.DialogInterface
import android.os.Looper
import android.provider.Settings
import ai.idto.sdk.internal.PermissionGate
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.Robolectric
import org.robolectric.RobolectricTestRunner
import org.robolectric.Shadows.shadowOf
import org.robolectric.shadows.ShadowAlertDialog

@RunWith(RobolectricTestRunner::class)
class PermissionGateRoboTest {

    private val camera = Manifest.permission.CAMERA
    private val mic = Manifest.permission.RECORD_AUDIO

    private fun activity(): Activity =
        Robolectric.buildActivity(Activity::class.java).setup().get()

    private fun gate(
        activity: Activity,
        rationale: (String) -> Boolean = { false },
        held: (String) -> Boolean = { false },
    ) = PermissionGate(activity, rationale, held)

    @Test
    fun `needed is declared intersect managed`() {
        assertEquals(listOf(camera), PermissionGate.computeNeeded(listOf(camera), emptyList()))
        assertEquals(emptyList<String>(), PermissionGate.computeNeeded(listOf(camera), listOf(camera)))
        assertEquals(listOf(camera, mic), PermissionGate.computeNeeded(listOf(camera, mic), emptyList()))
    }

    @Test
    fun `all granted needs nothing`() {
        val g = gate(activity(), held = { true })
        assertTrue(g.neededPermissions().isEmpty())
    }

    @Test
    fun `denied still proceeds and is not flagged when rationale available`() {
        val g = gate(activity(), rationale = { true }, held = { false })
        g.onPermissionResult(mapOf(camera to false))
        assertFalse(g.isPermanentlyDenied(camera))
        assertFalse(g.handleUnheldCaptureResources(listOf(camera)))
        assertNull(ShadowAlertDialog.getLatestAlertDialog())
    }

    @Test
    fun `permanent denial is flagged`() {
        val g = gate(activity(), rationale = { false }, held = { false })
        g.onPermissionResult(mapOf(camera to false))
        assertTrue(g.isPermanentlyDenied(camera))
    }

    @Test
    fun `unheld permanently denied permission shows settings dialog once`() {
        val a = activity()
        val g = gate(a, rationale = { false }, held = { false })
        g.onPermissionResult(mapOf(camera to false))

        assertTrue(g.handleUnheldCaptureResources(listOf(camera)))
        val dialog = ShadowAlertDialog.getLatestAlertDialog()
        assertNotNull(dialog)

        dialog!!.getButton(DialogInterface.BUTTON_POSITIVE).performClick()
        shadowOf(Looper.getMainLooper()).idle()
        val started = shadowOf(a).nextStartedActivity
        assertEquals(Settings.ACTION_APPLICATION_DETAILS_SETTINGS, started.action)
        assertEquals("package", started.data?.scheme)

        assertFalse(g.handleUnheldCaptureResources(listOf(camera)))
    }

    @Test
    fun `refresh clears permanent denial once granted`() {
        var granted = false
        val g = gate(activity(), rationale = { false }, held = { granted })
        g.onPermissionResult(mapOf(camera to false))
        assertTrue(g.isPermanentlyDenied(camera))
        granted = true
        g.refresh()
        assertFalse(g.isPermanentlyDenied(camera))
    }
}
