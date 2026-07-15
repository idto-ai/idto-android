package ai.idto.sdk

import android.app.Activity
import android.content.Context
import android.content.Intent
import ai.idto.sdk.internal.IDtoActivity
import ai.idto.sdk.internal.SessionRegistry

object IDto {

    @JvmStatic
    @JvmOverloads
    fun open(
        context: Context,
        config: IDtoConfig,
        listener: IDtoEventListener,
        tokenProvider: IDtoTokenProvider? = null,
    ) {
        val entry = SessionRegistry.register(config, listener, tokenProvider)
        if (entry == null) {
            listener.onError(IDtoErrorData("init", "session_active", ""))
            return
        }
        val intent = Intent(context, IDtoActivity::class.java)
            .putExtra(IDtoActivity.EXTRA_SESSION_ID, entry.id)
        if (context !is Activity) {
            intent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
        }
        try {
            context.startActivity(intent)
        } catch (e: Throwable) {
            SessionRegistry.remove(entry.id)
            listener.onError(IDtoErrorData("init", "unknown_error", ""))
        }
    }

    @JvmStatic
    fun close() {
        IDtoActivity.requestCloseActive()
    }

    @JvmStatic
    fun isOpen(): Boolean = SessionRegistry.isActive()
}
