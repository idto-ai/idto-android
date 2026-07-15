package ai.idto.sdk.internal

import ai.idto.sdk.IDtoConfig
import ai.idto.sdk.IDtoEventListener
import ai.idto.sdk.IDtoTokenProvider
import java.util.UUID
import java.util.concurrent.ConcurrentHashMap

class SessionEntry(
    val id: String,
    val config: IDtoConfig,
    val listener: IDtoEventListener,
    val tokenProvider: IDtoTokenProvider?,
)

object SessionRegistry {

    private val entries = ConcurrentHashMap<String, SessionEntry>()

    @Synchronized
    fun register(
        config: IDtoConfig,
        listener: IDtoEventListener,
        tokenProvider: IDtoTokenProvider?,
    ): SessionEntry? {
        if (entries.isNotEmpty()) return null
        val entry = SessionEntry(UUID.randomUUID().toString(), config, listener, tokenProvider)
        entries[entry.id] = entry
        return entry
    }

    fun get(id: String?): SessionEntry? = id?.let { entries[it] }

    fun remove(id: String?) {
        id?.let { entries.remove(it) }
    }

    fun isActive(): Boolean = entries.isNotEmpty()

    fun clear() {
        entries.clear()
    }
}
