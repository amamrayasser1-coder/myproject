package app.cineva.engine.core.model

import java.net.URI
import java.security.MessageDigest
import java.util.Locale

enum class VideoQuality(val height: Int) {
    UNKNOWN(0), SD_480(480), HD_720(720), FULL_HD_1080(1080), UHD_2160(2160)
}
enum class SubtitleFormat(val mimeType: String, val extensions: Set<String>) {
    WEBVTT("text/vtt", setOf("vtt")),
    SUBRIP("application/x-subrip", setOf("srt")),
    SSA("text/x-ssa", setOf("ssa", "ass")),
    TTML("application/ttml+xml", setOf("ttml", "xml")),
    UNKNOWN("application/octet-stream", emptySet());

    companion object {
        fun fromUrlOrMime(url: String, mimeType: String?): SubtitleFormat {
            val mime = mimeType?.substringBefore(';')?.trim()?.lowercase(Locale.ROOT)
            entries.firstOrNull { it.mimeType == mime }?.let { return it }
            val ext = runCatching { URI(url).path.substringAfterLast('.', "").lowercase(Locale.ROOT) }.getOrDefault("")
            return entries.firstOrNull { ext in it.extensions } ?: UNKNOWN
        }
    }
}
data class MediaRequest(
    val title: String, val year: Int? = null, val tmdbId: String? = null,
    val imdbId: String? = null, val season: Int? = null, val episode: Int? = null,
    val preferredLanguages: List<String> = emptyList(),
    val preferredQuality: VideoQuality? = VideoQuality.FULL_HD_1080
) {
    init {
        require(title.isNotBlank())
        require(season == null || season > 0)
        require(episode == null || episode > 0)
        require((season == null) == (episode == null))
    }
    fun cacheKey(providerId: String): String {
        val raw = listOf(providerId, title.trim().lowercase(Locale.ROOT), year, tmdbId, imdbId,
            season, episode, preferredLanguages.map { it.lowercase(Locale.ROOT) }.sorted(), preferredQuality?.name).joinToString("|")
        return MessageDigest.getInstance("SHA-256").digest(raw.toByteArray()).joinToString("") { "%02x".format(it) }
    }
}
data class SubtitleTrack(
    val url: String, val language: String? = null, val label: String? = null,
    val mimeType: String? = null, val isDefault: Boolean = false,
    val headers: Map<String, String> = emptyMap()
) {
    val format: SubtitleFormat get() = SubtitleFormat.fromUrlOrMime(url, mimeType)
}
data class StreamCandidate(
    val id: String, val sourceId: String, val sourceName: String, val url: String,
    val quality: VideoQuality = VideoQuality.UNKNOWN, val mimeType: String? = null,
    val headers: Map<String, String> = emptyMap(), val referer: String? = null,
    val subtitles: List<SubtitleTrack> = emptyList(), val audioLanguages: List<String> = emptyList(),
    val expiresAtEpochMillis: Long? = null, val reliability: Double = 0.5
) {
    init { require(id.isNotBlank() && sourceId.isNotBlank() && url.isNotBlank()); require(reliability in 0.0..1.0) }
}
data class ProviderDescriptor(
    val id: String, val displayName: String, val allowedHosts: Set<String>,
    val enabledByDefault: Boolean = true, val maxResults: Int = 20
) {
    init {
        require(id.matches(Regex("[a-zA-Z0-9._-]{2,64}")))
        require(displayName.isNotBlank() && allowedHosts.isNotEmpty() && maxResults in 1..100)
    }
}
enum class ProviderStatus { SUCCESS, EMPTY, CACHED, DISABLED, CIRCUIT_OPEN, TIMEOUT, FAILED }
data class ProviderOutcome(
    val providerId: String, val status: ProviderStatus, val candidateCount: Int = 0,
    val elapsedMillis: Long = 0, val safeMessage: String? = null
)
data class SearchReport(
    val candidates: List<StreamCandidate>, val providerOutcomes: List<ProviderOutcome>,
    val rejectedCandidateCount: Int, val elapsedMillis: Long
)
