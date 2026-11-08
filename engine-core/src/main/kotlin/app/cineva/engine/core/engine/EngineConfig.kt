package app.cineva.engine.core.engine
import kotlinx.coroutines.CoroutineDispatcher
import kotlinx.coroutines.Dispatchers
data class EngineConfig(
    val providerTimeoutMillis: Long = 12_000,
    val maxConcurrentProviders: Int = 5,
    val cacheTtlMillis: Long = 180_000,
    val maxCandidates: Int = 40,
    val dispatcher: CoroutineDispatcher = Dispatchers.IO
) {
    init {
        require(providerTimeoutMillis > 0)
        require(maxConcurrentProviders in 1..20)
        require(cacheTtlMillis > 0)
        require(maxCandidates in 1..200)
    }
}
