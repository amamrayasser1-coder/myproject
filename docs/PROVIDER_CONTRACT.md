# Provider contract
Implement SourceProvider for each source Cineva is permitted to use. Each provider must have a stable ID, declare its authorized HTTPS host allowlist, set StreamCandidate.sourceId to its own ID, normalize quality/MIME/headers/referer/subtitles/expiry, respect source terms and rate limits, and avoid logging secrets.
Do not implement DRM removal, CAPTCHA solving, paywall bypass, credential theft or anti-bot circumvention. Add a CDN host only after confirming it is part of the authorized source.
