# Android integration
Requirements: JDK 17, Android SDK API 35, minSdk 26 for the Media3 adapter.
Include engine-core, engine-network and engine-media3 in Cineva's Gradle settings. Merge plugin and dependency declarations with Cineva's existing version catalog; do not replace its build configuration blindly.
Create ProviderRegistry, register authorized SourceProvider implementations, construct ScraperEngine, and call search(MediaRequest) using Cineva's existing TMDB title, year and ID.
Pass the selected StreamCandidate to Media3PlaybackAdapter.toMediaItem and toMediaSourceFactory. Create a new source factory for each selected stream.
The adapter maps WebVTT, SubRip, SSA/ASS and TTML to Media3 subtitle configurations. Subtitle-specific headers differing from stream headers need a dedicated subtitle DataSource and are not implemented in this first adapter.
A download is complete only after transfer completion, file existence, byte-count verification and successful reopen.
