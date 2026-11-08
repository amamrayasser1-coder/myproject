package app.cineva.engine.core.cache
import app.cineva.engine.core.model.StreamCandidate
import java.util.concurrent.ConcurrentHashMap
interface SearchCache {
    fun get(key: String, nowEpochMillis: Long = System.currentTimeMillis()): List<StreamCandidate>?
    fun put(key: String, value: List<StreamCandidate>, ttlMillis: Long, nowEpochMillis: Long = System.currentTimeMillis())
    fun invalidateAll()
}
class InMemorySearchCache(private val maxEntries: Int = 500) : SearchCache {
    private data class Entry(val value: List<StreamCandidate>, val expiresAt: Long, val touchedAt: Long)
    private val entries = ConcurrentHashMap<String, Entry>()
    init { require(maxEntries > 0) }
    override fun get(key: String, nowEpochMillis: Long): List<StreamCandidate>? {
        val entry = entries[key] ?: return null
        if (entry.expiresAt <= nowEpochMillis) { entries.remove(key, entry); return null }
        entries[key] = entry.copy(touchedAt = nowEpochMillis)
        return entry.value
    }
    override fun put(key: String, value: List<StreamCandidate>, ttlMillis: Long, nowEpochMillis: Long) {
        require(ttlMillis > 0)
        entries[key] = Entry(value.toList(), nowEpochMillis + ttlMillis, nowEpochMillis)
        if (entries.size > maxEntries) entries.entries.sortedBy { it.value.touchedAt }
            .take((entries.size - maxEntries).coerceAtLeast(1)).forEach { entries.remove(it.key, it.value) }
    }
    override fun invalidateAll() { entries.clear() }
}
