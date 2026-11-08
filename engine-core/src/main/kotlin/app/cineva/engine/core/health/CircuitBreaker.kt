package app.cineva.engine.core.health
enum class CircuitState { CLOSED, OPEN, HALF_OPEN }
class CircuitBreaker(private val failureThreshold: Int = 3, private val openDurationMillis: Long = 30_000) {
    init { require(failureThreshold > 0 && openDurationMillis > 0) }
    private val lock = Any()
    private var state = CircuitState.CLOSED
    private var failures = 0
    private var openedAt = 0L
    private var probeInFlight = false
    fun tryAcquire(now: Long = System.currentTimeMillis()): Boolean = synchronized(lock) {
        when (state) {
            CircuitState.CLOSED -> true
            CircuitState.OPEN -> if (now - openedAt < openDurationMillis) false else {
                state = CircuitState.HALF_OPEN; probeInFlight = true; true
            }
            CircuitState.HALF_OPEN -> if (probeInFlight) false else { probeInFlight = true; true }
        }
    }
    fun recordSuccess() = synchronized(lock) { state = CircuitState.CLOSED; failures = 0; probeInFlight = false }
    fun recordFailure(now: Long = System.currentTimeMillis()) = synchronized(lock) {
        failures++; probeInFlight = false
        if (state == CircuitState.HALF_OPEN || failures >= failureThreshold) { state = CircuitState.OPEN; openedAt = now }
    }
    fun currentState(): CircuitState = synchronized(lock) { state }
}
