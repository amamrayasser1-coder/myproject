package app.cineva.engine.core.security
import app.cineva.engine.core.model.ProviderDescriptor
import java.net.IDN
import java.net.URI
import java.util.Locale
class SourceAccessPolicy(private val allowHttpForLocalDevelopment: Boolean = false) {
    fun isAllowed(url: String, descriptor: ProviderDescriptor): Boolean {
        val uri = runCatching { URI(url) }.getOrNull() ?: return false
        val scheme = uri.scheme?.lowercase(Locale.ROOT) ?: return false
        if (scheme != "https" && !(allowHttpForLocalDevelopment && scheme == "http")) return false
        if (uri.userInfo != null || uri.host.isNullOrBlank() || uri.rawFragment != null) return false
        val host = runCatching { IDN.toASCII(uri.host.trimEnd('.')).lowercase(Locale.ROOT) }.getOrNull() ?: return false
        if (host.contains(':') || IPV4.matches(host)) return false
        if (host == "localhost" || host.endsWith(".localhost") || host.endsWith(".local") || host.endsWith(".internal")) return false
        return descriptor.allowedHosts.mapNotNull {
            runCatching { IDN.toASCII(it.trim().trimEnd('.')).lowercase(Locale.ROOT) }.getOrNull()
        }.any { host == it || host.endsWith(".$it") }
    }
    fun requireAllowed(url: String, descriptor: ProviderDescriptor) {
        require(isAllowed(url, descriptor)) { "URL is outside the provider's HTTPS host allowlist." }
    }
    private companion object { val IPV4 = Regex("^(?:\\d{1,3}\\.){3}\\d{1,3}$") }
}
