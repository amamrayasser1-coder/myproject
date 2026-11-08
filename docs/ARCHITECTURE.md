# Architecture
TMDB metadata -> MediaRequest -> ProviderRegistry -> ScraperEngine -> providers -> URL policy -> StreamRanker -> SearchReport -> Media3 adapter -> Cineva player.
engine-core contains contracts, models, bounded parallel orchestration, cache, circuit breaker and ranking.
engine-network contains bounded HTTP transport and configurable HTML extraction.
engine-media3 contains Android playback and external subtitle configuration.
Providers are isolated; errors from one provider should not break other results. Redirect targets are checked again and private/local DNS results are rejected. Network requests have timeouts, redirect limits and response-size limits. A dedicated data-source factory prevents request-header leakage between selected sources.
The initial HTML adapter extracts direct media links from configured selectors on authorized sites only. HLS/DASH manifests and segments still need production validation.
