# Remaining production work
The first engine foundation and manifest inspection are implemented. Remaining tasks require integration into Cineva and authorized source-specific fixtures:
1. Wire Cineva TMDB IDs and its existing UI/player flow.
2. Add permitted provider adapters with fixture-based tests and terms/permission review.
3. Resolve relative DASH BaseURL inheritance and template expansion against each representation; this inspector only validates explicit static references.
4. Fetch and recursively inspect nested HLS/DASH playlists and every segment before playback.
5. Add subtitle-specific request headers and subtitle DataSource.
6. Add encrypted Room cache and secure provider settings.
7. Add resumable, verified downloads for direct files and HLS/DASH offline packages where licensed.
8. Add Android instrumentation tests on API 26+ devices for playback, subtitles, headers, buffering and downloads.
9. Run dependency vulnerability scans and document supported provider health metrics.
