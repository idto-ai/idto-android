package ai.idto.sdk

import ai.idto.sdk.internal.ReportShare
import android.content.Intent
import androidx.core.content.FileProvider
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.RuntimeEnvironment
import java.io.File

@RunWith(RobolectricTestRunner::class)
class ReportShareRoboTest {

    private val context = RuntimeEnvironment.getApplication()

    @Before
    fun resetProviderCache() {
        val field = FileProvider::class.java.getDeclaredField("sCache")
        field.isAccessible = true
        (field.get(null) as MutableMap<*, *>).clear()
    }

    private fun staged(): File =
        File(context.cacheDir, "idto_reports").apply { mkdirs() }.let { File(it, "report.pdf").apply { writeBytes(byteArrayOf(1, 2, 3)) } }

    @Test
    fun `send intent is action send with grant flag and provider uri`() {
        val send = ReportShare.buildSendIntent(ReportShare.uriFor(context, staged()), "application/pdf")
        assertEquals(Intent.ACTION_SEND, send.action)
        assertTrue(send.flags and Intent.FLAG_GRANT_READ_URI_PERMISSION != 0)
        val uri = send.getParcelableExtra<android.net.Uri>(Intent.EXTRA_STREAM)!!
        assertEquals("${context.packageName}.idto.fileprovider", uri.authority)
    }

    @Test
    fun `send intent type comes from message mime`() {
        val send = ReportShare.buildSendIntent(ReportShare.uriFor(context, staged()), "image/png")
        assertEquals("image/png", send.type)
    }

    @Test
    fun `empty mime defaults to application pdf`() {
        val send = ReportShare.buildSendIntent(ReportShare.uriFor(context, staged()), "")
        assertEquals("application/pdf", send.type)
    }

    @Test
    fun `chooser wraps the send intent`() {
        val chooser = ReportShare.buildChooser(context, staged(), "application/pdf", "myreport.pdf")
        assertEquals(Intent.ACTION_CHOOSER, chooser.action)
        val inner = chooser.getParcelableExtra<Intent>(Intent.EXTRA_INTENT)
        assertNotNull(inner)
        assertEquals(Intent.ACTION_SEND, inner!!.action)
    }

    @Test
    fun `chooser uses sanitized filename as title`() {
        val chooser = ReportShare.buildChooser(context, staged(), "application/pdf", "myreport.pdf")
        assertEquals("myreport.pdf", chooser.getCharSequenceExtra(Intent.EXTRA_TITLE))
    }
}
