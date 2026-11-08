# Cineva Scraper Engine

Independent Kotlin media-source engine foundation for Cineva.

## Modules
- engine-core: provider contracts, media models, concurrency, cache, circuit breaker, validation and ranking.
- engine-network: bounded OkHttp transport and authorized HTML selector adapter.
- engine-media3: AndroidX Media3 adapter for stream headers and external subtitles.

## Features
- Bounded parallel provider search with timeouts and cancellation.
- Per-provider circuit breakers and TTL cache.
- HTTPS host allowlists, redirect re-validation, DNS filtering for local/private addresses, response-size limits and timeouts.
- Normalized quality, MIME type, headers, referer, expiry and subtitle metadata.
- Result deduplication and quality/language/reliability ranking.
- Media3 subtitle metadata for WebVTT, SubRip, SSA/ASS and TTML.

## Build
Requires JDK 17, Android SDK API 35 and Gradle 8.11.1 or compatible.
Run `gradle :engine-core:test` and `gradle build`.

This is a starter engine, not a claim of superiority to mature upstream projects. Core Kotlin sources were smoke-compiled locally; full Gradle/Android build and device playback tests remain to be run in a configured Android SDK environment.

Only connect sources you are authorized to access and use. No DRM removal, CAPTCHA solving, paywall bypass, credential harvesting or anti-bot evasion is included. HLS/DASH manifests and segments need additional production validation.
