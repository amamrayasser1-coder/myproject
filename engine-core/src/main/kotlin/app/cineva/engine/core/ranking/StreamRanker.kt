package app.cineva.engine.core.ranking
import app.cineva.engine.core.model.MediaRequest
import app.cineva.engine.core.model.StreamCandidate
import app.cineva.engine.core.model.VideoQuality
import java.util.Locale
class StreamRanker {
    fun rank(candidates: List<StreamCandidate>, request: MediaRequest, nowEpochMillis: Long = System.currentTimeMillis()): List<StreamCandidate> {
        val languages = request.preferredLanguages.mapIndexed { i, s -> s.lowercase(Locale.ROOT) to i }.toMap()
        return candidates.distinctBy { it.url.substringBefore('#') }
            .filter { it.expiresAtEpochMillis == null || it.expiresAtEpochMillis > nowEpochMillis + 5_000 }
            .sortedWith(compareByDescending<StreamCandidate> { qualityScore(it.quality, request.preferredQuality) }
                .thenByDescending { subtitleScore(it, languages) }
                .thenByDescending { it.reliability }
                .thenBy { it.expiresAtEpochMillis ?: Long.MAX_VALUE }
                .thenBy { it.sourceName.lowercase(Locale.ROOT) })
    }
    private fun qualityScore(actual: VideoQuality, preferred: VideoQuality?): Int {
        if (actual == VideoQuality.UNKNOWN) return 0
        if (preferred == null || preferred == VideoQuality.UNKNOWN) return actual.height
        val distance = kotlin.math.abs(actual.height - preferred.height)
        return 10_000 - distance + if (actual.height <= preferred.height) 100 else 0
    }
    private fun subtitleScore(candidate: StreamCandidate, languages: Map<String, Int>): Int {
        if (candidate.subtitles.isEmpty()) return 0
        if (languages.isEmpty()) return 1
        val best = candidate.subtitles.mapNotNull { it.language?.lowercase(Locale.ROOT)?.let(languages::get) }.minOrNull()
        return if (best == null) 1 else 100 - best
    }
}
