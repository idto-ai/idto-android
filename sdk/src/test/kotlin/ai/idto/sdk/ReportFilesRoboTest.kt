package ai.idto.sdk

import ai.idto.sdk.internal.ReportFiles
import android.util.Base64
import org.junit.Assert.assertArrayEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.RuntimeEnvironment
import java.io.File

@RunWith(RobolectricTestRunner::class)
class ReportFilesRoboTest {

    private val context = RuntimeEnvironment.getApplication()

    private fun b64(text: String) = Base64.encodeToString(text.toByteArray(), Base64.NO_WRAP)

    @Test
    fun `stages base64 into reports dir and round trips`() {
        val file = ReportFiles.stageReport(context, b64("hello world"), "statement.pdf")!!
        assertTrue(file.exists())
        assertTrue(file.parentFile!!.name == "idto_reports")
        assertArrayEquals("hello world".toByteArray(), file.readBytes())
        assertTrue(file.name == "statement.pdf")
    }

    @Test
    fun `invalid base64 returns null and leaves no file`() {
        val before = reportsFiles()
        assertNull(ReportFiles.stageReport(context, "@@@not base64@@@", "x.pdf"))
        assertEquals(before, reportsFiles())
    }

    @Test
    fun `oversized payload rejected before write`() {
        val file = ReportFiles.stageReport(context, b64("abcdef"), "big.pdf", maxBytes = 3)
        assertNull(file)
    }

    @Test
    fun `delete quietly removes staged file`() {
        val file = ReportFiles.stageReport(context, b64("bytes"), "gone.pdf")!!
        assertTrue(file.exists())
        ReportFiles.deleteQuietly(file)
        assertFalse(file.exists())
    }

    @Test
    fun `delete of missing file is silent`() {
        val missing = File(context.cacheDir, "idto_reports/none.pdf")
        ReportFiles.deleteQuietly(missing)
        ReportFiles.deleteQuietly(null)
    }

    @Test
    fun `untrusted filename is sanitized on stage`() {
        val file = ReportFiles.stageReport(context, b64("x"), "../../etc/passwd")!!
        assertTrue(file.name == "passwd.pdf")
        assertTrue(file.parentFile!!.name == "idto_reports")
    }

    private fun reportsFiles(): Set<String> =
        File(context.cacheDir, "idto_reports").listFiles()?.map { it.name }?.toSet() ?: emptySet()

    private fun assertEquals(a: Any?, b: Any?) = org.junit.Assert.assertEquals(a, b)
}
