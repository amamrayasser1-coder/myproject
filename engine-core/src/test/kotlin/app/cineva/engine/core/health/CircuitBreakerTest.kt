package app.cineva.engine.core.health
import kotlin.test.Test
import kotlin.test.assertFalse
import kotlin.test.assertTrue
class CircuitBreakerTest {
    @Test fun opensAndAllowsOneProbe() {
        val b = CircuitBreaker(1, 100)
        assertTrue(b.tryAcquire(0)); b.recordFailure(1)
        assertFalse(b.tryAcquire(2)); assertTrue(b.tryAcquire(101))
        assertFalse(b.tryAcquire(102)); b.recordSuccess(); assertTrue(b.tryAcquire(103))
    }
}
