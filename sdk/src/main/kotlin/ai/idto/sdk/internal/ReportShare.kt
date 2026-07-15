package ai.idto.sdk.internal

import android.content.Context
import android.content.Intent
import android.net.Uri
import androidx.core.content.FileProvider
import java.io.File

object ReportShare {

    private const val AUTHORITY_SUFFIX = ".idto.fileprovider"
    private const val DEFAULT_MIME = "application/pdf"

    fun authority(context: Context): String = context.packageName + AUTHORITY_SUFFIX

    fun uriFor(context: Context, file: File): Uri =
        FileProvider.getUriForFile(context, authority(context), file)

    fun buildSendIntent(uri: Uri, mime: String): Intent =
        Intent(Intent.ACTION_SEND).apply {
            type = mime.ifEmpty { DEFAULT_MIME }
            putExtra(Intent.EXTRA_STREAM, uri)
            addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION)
        }

    fun buildChooser(context: Context, file: File, mime: String, name: String): Intent =
        Intent.createChooser(buildSendIntent(uriFor(context, file), mime), name)
}
