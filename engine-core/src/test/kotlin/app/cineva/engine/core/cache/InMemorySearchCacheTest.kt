package app.cineva.engine.core.cache
import app.cineva.engine.core.model.StreamCandidate
import kotlin.test.Test
import kotlin.test.assertNotNull
import kotlin.test.assertNull
class InMemorySearchCacheTest {
    @Test fun expiresEntries() {
        val cache=InMemorySearchCache()
        val c=StreamCandidate("id","provider","Provider","https://cdn.example.org/video.mp4")
        cache.put("k",listOf(c),100,1000)
        assertNotNull(cache.get("k",1050))
        assertNull(cache.get("k",1100))
    }
}
