package ai.idto.sdk.internal

import android.content.Context
import android.util.Base64
import java.io.File

object ReportFiles {

    private const val DEFAULT_FILENAME = "report.pdf"
    private const val REPORTS_DIR = "idto_reports"
    private const val MAX_NAME_LENGTH = 120
    private val ILLEGAL = Regex("[\\x00-\\x1f<>:\"|?*]")
    private val DOT_RUNS = Regex("\\.{2,}")
    private val LEADING_DOTS = Regex("^\\.+")
    private val HAS_EXTENSION = Regex("\\.[a-z0-9]{1,8}$", RegexOption.IGNORE_CASE)

    fun sanitizeFilename(name: String?): String {
        val base = (name ?: "")
            .split('/', '\\')
            .last()
            .replace(ILLEGAL, "")
            .replace(DOT_RUNS, ".")
            .replace(LEADING_DOTS, "")
            .trim()
        if (base.isEmpty()) return DEFAULT_FILENAME
        val named = if (HAS_EXTENSION.containsMatchIn(base)) base else "$base.pdf"
        return boundLength(named)
    }

    private fun boundLength(name: String): String {
        if (name.length <= MAX_NAME_LENGTH) return name
        val dot = name.lastIndexOf('.')
        if (dot > 0 && name.length - dot <= 9) {
            val ext = name.substring(dot)
            return name.substring(0, MAX_NAME_LENGTH - ext.length) + ext
        }
        return name.substring(0, MAX_NAME_LENGTH)
    }

    fun decodeBase64(data: String): ByteArray = Base64.decode(data, Base64.DEFAULT)

    fun stageReport(
        context: Context,
        base64: String,
        filename: String?,
        maxBytes: Long = Defaults.MAX_DL_BYTES,
    ): File? {
        val bytes = try { decodeBase64(base64) } catch (e: Exception) { return null }
        if (bytes.isEmpty() || bytes.size.toLong() > maxBytes) return null
        return try {
            val dir = File(context.cacheDir, REPORTS_DIR).apply { mkdirs() }
            File(dir, sanitizeFilename(filename)).apply { writeBytes(bytes) }
        } catch (e: Exception) {
            null
        }
    }

    fun deleteQuietly(file: File?) {
        try {
            if (file != null && file.exists()) file.delete()
        } catch (e: Exception) {
        }
    }
}
