package app.cineva.engine.core.engine
import app.cineva.engine.core.provider.SourceProvider
import java.util.concurrent.ConcurrentHashMap
class ProviderRegistry {
    private val providers = ConcurrentHashMap<String, SourceProvider>()
    fun register(provider: SourceProvider) {
        val id = provider.descriptor.id
        check(providers.putIfAbsent(id, provider) == null) { "Provider '$id' is already registered." }
    }
    fun unregister(providerId: String): Boolean = providers.remove(providerId) != null
    fun enabledProviders(): List<SourceProvider> = providers.values.filter { it.descriptor.enabledByDefault }.sortedBy { it.descriptor.id }
    fun allProviders(): List<SourceProvider> = providers.values.sortedBy { it.descriptor.id }
}
