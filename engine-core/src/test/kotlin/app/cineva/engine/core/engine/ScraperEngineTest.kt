package app.cineva.engine.core.engine
import app.cineva.engine.core.model.*
import app.cineva.engine.core.provider.SourceProvider
import kotlinx.coroutines.runBlocking
import kotlin.test.Test
import kotlin.test.assertEquals
class ScraperEngineTest {
    @Test fun searchesProviderAndReturnsAllowedCandidate()=runBlocking {
        val registry=ProviderRegistry()
        registry.register(object:SourceProvider {
            override val descriptor=ProviderDescriptor("demo-one","Demo",setOf("cdn.example.org"))
            override suspend fun search(request:MediaRequest)=listOf(
                StreamCandidate("stream","demo-one","Demo","https://cdn.example.org/master.m3u8",VideoQuality.FULL_HD_1080))
        })
        val report=ScraperEngine(registry).search(MediaRequest("Demo movie"))
        assertEquals(1,report.candidates.size)
        assertEquals(0,report.rejectedCandidateCount)
    }
}
