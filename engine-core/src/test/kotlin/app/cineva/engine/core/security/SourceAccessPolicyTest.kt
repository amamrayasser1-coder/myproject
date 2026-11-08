package app.cineva.engine.core.security
import app.cineva.engine.core.model.ProviderDescriptor
import kotlin.test.Test
import kotlin.test.assertFalse
import kotlin.test.assertTrue
class SourceAccessPolicyTest {
    private val policy = SourceAccessPolicy()
    private val provider = ProviderDescriptor("demo-provider", "Demo", setOf("media.example.org"))
    @Test fun allowsExactHost() { assertTrue(policy.isAllowed("https://media.example.org/a.m3u8", provider)) }
    @Test fun allowsSubdomain() { assertTrue(policy.isAllowed("https://cdn.media.example.org/a.mp4", provider)) }
    @Test fun rejectsLookalike() { assertFalse(policy.isAllowed("https://media.example.org.attacker.test/a.mp4", provider)) }
    @Test fun rejectsHttp() { assertFalse(policy.isAllowed("http://media.example.org/a.mp4", provider)) }
    @Test fun rejectsLocalAndIp() {
        assertFalse(policy.isAllowed("https://localhost/a", provider))
        assertFalse(policy.isAllowed("https://127.0.0.1/a", provider))
    }
}
