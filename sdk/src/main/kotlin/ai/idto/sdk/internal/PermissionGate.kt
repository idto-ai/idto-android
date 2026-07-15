package ai.idto.sdk.internal

import android.Manifest
import android.app.Activity
import android.app.AlertDialog
import android.content.Context
import android.content.Intent
import android.content.pm.PackageManager
import android.net.Uri
import android.provider.Settings
import androidx.core.content.ContextCompat

class PermissionGate(
    private val activity: Activity,
    private val rationale: (String) -> Boolean = { activity.shouldShowRequestPermissionRationale(it) },
    private val held: (String) -> Boolean = {
        ContextCompat.checkSelfPermission(activity, it) == PackageManager.PERMISSION_GRANTED
    },
) {

    private val permanentlyDenied = mutableSetOf<String>()
    private var settingsDialogShown = false

    fun neededPermissions(): List<String> =
        computeNeeded(declaredManaged(activity), MANAGED.filter(held))

    fun onPermissionResult(result: Map<String, Boolean>) {
        result.forEach { (permission, granted) ->
            if (granted) {
                permanentlyDenied.remove(permission)
            } else if (!rationale(permission)) {
                permanentlyDenied.add(permission)
            }
        }
    }

    fun refresh() {
        permanentlyDenied.retainAll { !held(it) }
    }

    fun isPermanentlyDenied(permission: String): Boolean = permission in permanentlyDenied

    fun handleUnheldCaptureResources(permissions: List<String>): Boolean {
        val target = permissions.firstOrNull { it in permanentlyDenied && !held(it) } ?: return false
        if (settingsDialogShown) return false
        settingsDialogShown = true
        activity.runOnUiThread { showSettingsDialog() }
        return true
    }

    private fun showSettingsDialog() {
        AlertDialog.Builder(activity)
            .setTitle("Permission required")
            .setMessage("Camera or microphone access is turned off. Enable it in Settings to continue.")
            .setPositiveButton("Open Settings") { _, _ -> openAppSettings() }
            .setNegativeButton("Cancel", null)
            .show()
    }

    private fun openAppSettings() {
        val intent = Intent(
            Settings.ACTION_APPLICATION_DETAILS_SETTINGS,
            Uri.fromParts("package", activity.packageName, null),
        )
        activity.startActivity(intent)
    }

    companion object {
        val MANAGED = listOf(Manifest.permission.CAMERA, Manifest.permission.RECORD_AUDIO)

        fun computeNeeded(declared: Collection<String>, granted: Collection<String>): List<String> =
            MANAGED.filter { it in declared && it !in granted }

        fun declaredManaged(context: Context): List<String> {
            val declared = try {
                context.packageManager
                    .getPackageInfo(context.packageName, PackageManager.GET_PERMISSIONS)
                    .requestedPermissions
                    ?.toSet()
                    ?: emptySet()
            } catch (e: Exception) {
                emptySet()
            }
            return MANAGED.filter { it in declared }
        }
    }
}
