package app.cineva.engine.core.engine
import app.cineva.engine.core.cache.InMemorySearchCache
import app.cineva.engine.core.cache.SearchCache
import app.cineva.engine.core.health.CircuitBreaker
import app.cineva.engine.core.model.*
import app.cineva.engine.core.provider.SourceProvider
import app.cineva.engine.core.ranking.StreamRanker
import app.cineva.engine.core.security.SourceAccessPolicy
import kotlinx.coroutines.*
import kotlinx.coroutines.sync.Semaphore
import kotlinx.coroutines.sync.withPermit
import java.util.concurrent.ConcurrentHashMap

class ScraperEngine(
    private val registry: ProviderRegistry,
    private val accessPolicy: SourceAccessPolicy = SourceAccessPolicy(),
    private val ranker: StreamRanker = StreamRanker(),
    private val cache: SearchCache = InMemorySearchCache(),
    private val config: EngineConfig = EngineConfig(),
    private val breakerFactory: () -> CircuitBreaker = { CircuitBreaker() }
) {
    private val breakers = ConcurrentHashMap<String, CircuitBreaker>()
    suspend fun search(request: MediaRequest): SearchReport {
        val started = System.currentTimeMillis()
        val providers = registry.enabledProviders()
        val semaphore = Semaphore(config.maxConcurrentProviders)
        val results = withContext(config.dispatcher) {
            coroutineScope { providers.map { p -> async { semaphore.withPermit { query(p, request) } } }.awaitAll() }
        }
        val raw = results.flatMap { it.candidates }
        val valid = raw.filter { valid(it, providers) }
        return SearchReport(ranker.rank(valid, request).take(config.maxCandidates),
            results.map { it.outcome }, raw.size - valid.size, System.currentTimeMillis() - started)
    }
    private suspend fun query(provider: SourceProvider, request: MediaRequest): ProviderResult {
        val descriptor = provider.descriptor
        val started = System.currentTimeMillis()
        val breaker = breakers.computeIfAbsent(descriptor.id) { breakerFactory() }
        if (!breaker.tryAcquire()) return ProviderResult(emptyList(), ProviderOutcome(
            descriptor.id, ProviderStatus.CIRCUIT_OPEN, elapsedMillis = System.currentTimeMillis() - started))
        val key = request.cacheKey(descriptor.id)
        cache.get(key)?.let { cached ->
            val safe = cached.filter { valid(it, listOf(provider)) }
            breaker.recordSuccess()
            return ProviderResult(safe, ProviderOutcome(descriptor.id, ProviderStatus.CACHED, safe.size,
                System.currentTimeMillis() - started))
        }
        return try {
            val found = withTimeout(config.providerTimeoutMillis) { provider.search(request) }.take(descriptor.maxResults)
            val accepted = found.filter { it.sourceId == descriptor.id &&
                accessPolicy.isAllowed(it.url, descriptor) &&
                it.subtitles.all { sub -> accessPolicy.isAllowed(sub.url, descriptor) } }
            breaker.recordSuccess()
            if (accepted.isNotEmpty()) cache.put(key, accepted, config.cacheTtlMillis)
            ProviderResult(accepted, ProviderOutcome(descriptor.id,
                if (accepted.isEmpty()) ProviderStatus.EMPTY else ProviderStatus.SUCCESS,
                accepted.size, System.currentTimeMillis() - started))
        } catch (timeout: TimeoutCancellationException) {
            breaker.recordFailure()
            ProviderResult(emptyList(), ProviderOutcome(descriptor.id, ProviderStatus.TIMEOUT,
                elapsedMillis = System.currentTimeMillis() - started))
        } catch (cancelled: CancellationException) { throw cancelled
        } catch (_: Exception) {
            breaker.recordFailure()
            ProviderResult(emptyList(), ProviderOutcome(descriptor.id, ProviderStatus.FAILED,
                elapsedMillis = System.currentTimeMillis() - started,
                safeMessage = "Provider request failed; sensitive details were not logged."))
        }
    }
    private fun valid(candidate: StreamCandidate, providers: List<SourceProvider>): Boolean {
        val provider = providers.firstOrNull { it.descriptor.id == candidate.sourceId } ?: return false
        val fresh = candidate.expiresAtEpochMillis == null || candidate.expiresAtEpochMillis > System.currentTimeMillis() + 5_000
        return fresh && accessPolicy.isAllowed(candidate.url, provider.descriptor) &&
            candidate.subtitles.all { accessPolicy.isAllowed(it.url, provider.descriptor) }
    }
    private data class ProviderResult(val candidates: List<StreamCandidate>, val outcome: ProviderOutcome)
}
