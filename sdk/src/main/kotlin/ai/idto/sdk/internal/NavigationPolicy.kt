package ai.idto.sdk.internal

import ai.idto.sdk.IDtoEnv
import java.net.URI

object NavigationPolicy {

    private const val CDN_HOST = "idto-sdk-bucket.s3.ap-south-1.amazonaws.com"
    private val ALLOWED_DOMAINS = listOf("idto.ai", "digilocker.gov.in", "meripehchaan.gov.in")
    private val SCHEME = Regex("^([a-zA-Z][a-zA-Z0-9+.-]*):")

    private const val PROD_ORIGIN = "https://prod.idto.ai"
    private const val DEV_ORIGIN = "https://dev.idto.ai"

    private data class Parsed(val scheme: String, val host: String)

    private fun parseUrl(url: String): Parsed? {
        val scheme = SCHEME.find(url)?.groupValues?.get(1)?.lowercase() ?: return null
        if (scheme == "about" || scheme == "data" || scheme == "blob") return Parsed(scheme, "")
        return try {
            Parsed(scheme, URI(url).host?.lowercase() ?: "")
        } catch (e: Exception) {
            null
        }
    }

    private fun isHostOrSubdomain(host: String, domain: String): Boolean =
        host == domain || host.endsWith(".$domain")

    fun isAllowedNavigation(url: String, baseUrl: String? = null, extra: List<String>? = null): Boolean {
        val parsed = parseUrl(url) ?: return false
        val (scheme, host) = parsed
        if (scheme == "about" || scheme == "data" || scheme == "blob") return true
        if (scheme != "http" && scheme != "https") return false
        if (host.isEmpty()) return false
        if (host == CDN_HOST) return true
        if (ALLOWED_DOMAINS.any { isHostOrSubdomain(host, it) }) return true
        val overrideHost = baseUrl?.let { parseUrl(it)?.host }
        if (!overrideHost.isNullOrEmpty() && overrideHost == host) return true
        return (extra ?: emptyList()).any { it.lowercase() == host }
    }

    fun isDigiLockerUrl(url: String): Boolean {
        val parsed = parseUrl(url) ?: return false
        if (parsed.scheme != "http" && parsed.scheme != "https") return false
        val host = parsed.host
        if (host == "digilocker.idto.ai") return true
        return isHostOrSubdomain(host, "digilocker.gov.in") || isHostOrSubdomain(host, "meripehchaan.gov.in")
    }

    fun apiBaseForEnv(env: IDtoEnv?): String = when (env) {
        IDtoEnv.DEVELOPMENT -> DEV_ORIGIN
        else -> PROD_ORIGIN
    }

    fun webViewOrigin(env: IDtoEnv?, baseUrl: String?): String {
        if (baseUrl != null) {
            try {
                val u = URI(baseUrl)
                val scheme = u.scheme?.lowercase()
                val host = u.host
                if ((scheme == "https" || scheme == "http") && !host.isNullOrEmpty()) {
                    val port = if (u.port != -1) ":${u.port}" else ""
                    return "$scheme://$host$port"
                }
            } catch (e: Exception) {
            }
        }
        return apiBaseForEnv(env)
    }
}
