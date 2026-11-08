# Security model
- HTTPS required by default.
- Provider host allowlist with DNS label-boundary matching.
- Redirect targets are re-validated.
- DNS results resolving to loopback, link-local, site-local/private, multicast or IPv4 carrier-grade NAT ranges are rejected.
- Request timeouts, bounded redirects and bounded response sizes.
- No logging of cookies, authorization headers or signed URLs.
Known limitations: not complete DNS-rebinding protection; HLS/DASH manifests and every segment URL are not parsed yet; subtitle-specific headers and encrypted persistent cache are future work. URL validation alone does not prove nested manifests or segments are safe.
