# Native AutoSync interoperability, 11 November 2026

## Scope

Own-fork Desktop test build only. The installed Nuvio application, original profiles, USS settings and API keys were not replaced. All playback used a loopback server, the public Blender Tears of Steel film and its official Dutch and English subtitle files. No translation/provider calls or cloud storage writes were used.

## Results recorded so far

- With AutoSync disabled, the external Dutch SRT loaded and rendered its original dialogue, including the cue at 00:38.
- With AutoSync enabled on the identical reference MKV and external SRT, Nuvio marked the selected addon subtitle as auto-synced. The generated SRT's SHA256 was identical to the source: `1146fe8a924306d680d9b7c82cf652e56e9f1ed28c81142fa477daee04b45b58`. Its timestamps were unchanged. This case did not produce a double timing correction.
- The real compiled timeline retimer also preserved all timestamps with an extra ten-second USS note at 23, 150 and 300 seconds. This algorithm check is separate from native rendering.
- A successfully delivered native player event arrived with the same `libmpv` identity as the native subtitle request. No audio, media URL or filename was included in that event.
- Focused follow-up compilation passed. Sixty-six local JUnit tests passed, covering event/consent boundaries, network gating, manual-delay preservation and the corrected baseline fixtures. This is not a fresh full-suite result.

## Confirmed fixes

1. An unavailable account backend must not prevent independent addon subtitle requests. Actual `NoInternet` still defers them. Authentication and TLS protections are unchanged.
2. The Compose consent alert could be hidden under the heavyweight native player. Desktop now uses an owned native dialog; declining or closing it still grants nothing. Native visibility was observed over the decoded video. Permission acceptance remains a manual user action.
3. A post-consent direct `setSubtitleUri` cancelled the client's AutoSync work. Refresh now keeps the selected subtitle in selected-only AutoSync scope and respects explicit or persisted manual delays.
4. The explicit refresh invalidates only the selected URL/header parsed-body cache entry. An old response from before consent must not overwrite the new notice body. Reference and alternative caches are retained.
5. Desktop subtitle analysis uses the native subtitle user agent unless the source explicitly supplies one. Video headers are untouched.
6. `OkHttpClient.newBuilder()` shared the addon download dispatcher. A loopback regression with all five per-host slots occupied reproduced an event waiting behind downloads; the original transport failed the timing assertion after about nine seconds. A separate dispatcher, bounded to eight event requests, passed the same regression in 0.84 seconds for the whole test. Connection/TLS behavior, the two-second deadline and redirect refusal remain unchanged. This queue-level proof is separate from the final native late-load measurement.

## Limits and remaining acceptance

The late-load/resume note interval and the complete feature CI result must be recorded separately before claiming completion. No audio listening or audio verification was performed by these checks. The public Blender case does not establish that the reported The Middle RCVR release is fixed. Ordinary Nuvio users do not receive this own-fork prototype through a USS server update.

The five deterministic baseline failures received fixture-only corrections: host-independent file paths, the Desktop settings policy and a realistic-duration legacy watch-progress example. The two historical intermittent rendering/QuickJS failures were not declared fixed. The already-recorded unchanged-base build is reused rather than rerun on every feature push.
