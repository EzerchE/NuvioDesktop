# Draft addon player-position interoperability patch

Base: Nuvio Desktop Dev `37ce07d351fc69e17d05a4640ef78d08deba20e2`, 11 November 2026.

## Scope

This is a locally prepared feature prototype, not an upstream-approved or installed client release. It adds standard Stremio `player` start/pause/stop events with millisecond currentTime/duration. Titles, subtitle languages, providers and episode numbers are not special-cased. It does not change subtitle selection, sorting, translation, audio sync or stream discovery.

Only enabled, matching player-resource addons are offered for explicit session consent. Declining leaves existing subtitle behavior alone. No consent is silently inferred from addon installation. Session state stays in memory. The common runtime call is currently guarded by isDesktop: Android/TV transport identity and native playback acceptance remain untested.

The event policy waits for a ready snapshot, known duration and applied initial seek; loading pauses extrapolation. Seek and state changes send events without periodic HTTP polling. Changed files/episodes stop the old identity first. Invalid snapshots or non-1x speed clear older state instead of claiming a reliable extrapolated position.

The worker holds at most one pending packet, coalesces bursts, caps fanout at eight consented addons, bounds each packet to two seconds and does not retry or log private URLs. Requests preserve installed addon transport/configuration; payloads do not contain media URLs, filenames, subtitle text or credentials. Existing profile checks suppress queued work after a profile change. End/disposal sends a best-effort stop.

Desktop libmpv downloads external subtitles with the libmpv User-Agent in the installed-client observation. Event requests use that same transport identity, without changing video HTTP headers or relaxing USS client isolation. This needs confirmation in a full patched-client playback test, especially with alternate desktop engines.

## Verification actually performed

- The three new production Kotlin files, including the Compose consent dialog, compiled using Kotlin 2.4.21, its Compose plugin and installed desktop dependencies.
- 16 focused JUnit tests passed: resume, initial invalid state, pause/resume/seek, buffering, identity changes, stop, rate limitation, generic IDs, bounded fanout, profile mismatch, queued-work suppression, burst coalescing, provider failure isolation, and once-only post-consent refresh after a successful current delivery. Refused, failed, superseded, unrelated and embedded-track cases do not trigger reload.
- The compiled Kotlin event serializer contacted a loopback-only fixture using the actual USS 2.13.69 PlaybackPositions/parser/SRT insertion module. A simulated twelve-second preparation advance changed resumed position from 185 to 197 seconds; the compound notice occupied 198.5 to 208.5 seconds and retained dialogue. Paused seek remained fixed and stop cleared the position. This is HTTP/module evidence, not native renderer evidence.
- The proposed #1060 companion regression and its six existing focused parser/presentation tests passed: seven total. Two same-English-language tracks kept distinct labels, incoming order, IDs, URLs, language and addon name. The old label worktree was not modified.

Focused reproduction uses the existing local JRE/compiler and app jars only, with no AI/provider requests, cloud writes or credentials. The verification script accepts explicit toolchain and installed-app paths. Its focused compile does not compile all of Dev or replace the full application build.

## Remaining acceptance and upstream policy

- Full Dev application/native playback with this patch has not run. No Nuvio installer was changed and no perfect-sync claim is made.
- A subtitle loaded before consent is refreshed once, only when it is the selected consented addon and a current player event has been successfully delivered. Native permission/first-download ordering and the rendered ten-second interval still require full patched-client acceptance.
- Session permission copy is currently English. It needs upstream resource localization and product review before release. Persisted per-addon consent is deliberately not added without that approval.
- A stopped client/network failure cannot acknowledge rendering. Cached subtitles, buffering and missing events still limit server-side estimation.
- CONTRIBUTING.md requires maintainer approval for new behavior/features. This local patch is therefore not being presented as an approved bugfix or bundled into the existing label/filename PRs.

## Related reports

- https://github.com/NuvioMedia/NuvioDesktop/pull/1059 : blank filename/hash fallback, separate from player events.
- https://github.com/NuvioMedia/NuvioDesktop/issues/1070 : metadata report closed by automation; author already requested reopening.
- https://github.com/NuvioMedia/NuvioDesktop/pull/1060 : preserve addon-provided label/name/title, separate from player events.
- https://github.com/NuvioMedia/NuvioDesktop/issues/1072 : explicit subtitle labels.
- https://github.com/NuvioMedia/NuvioDesktop/pull/1060#issuecomment-6772582146 : synthetic same-language ordering case. This is corroborating feedback from a contributor, not maintainer approval. LabelOrderRegressionTest.kt is a separately tested companion proposal for that PR.
- https://github.com/Stremio/stremio-addon-sdk/blob/master/docs/api/requests/definePlayerHandler.md : event wire protocol; the event response is ignored, not a native overlay API.

## Related USS release emails

Live health reports 2.13.69. Oracle run 42995583660 succeeded on attempt two after the first checkpoint readback refused replacement. Older failed run 42901526211 also refused at checkpoint readback. Those notifications do not establish that the successful production deployment failed. No guard was bypassed.

## Full application build evidence

The local full Dev build has not passed. Initial failures were missing WebView2 native inputs, an absent blank local.properties input, and duplicate MSVC runtime copies. The official 0.1.35-alpha MSI was SHA256-verified and extracted without installing it. All 45 JNI method descriptors matched the current Kotlin native interface; this validates that reuse boundary only, not the new application.

A two-process full Kotlin compilation failed with Java heap exhaustion. A bounded single-process retry was stopped when the 8 GB workstation approached memory exhaustion. No main application or user configuration was replaced. The user authorized pushing only this feature branch to their public fork for standard Windows 16 GB runner verification. The workflow has read-only permissions, no secrets, no signing, no release publication, and no artifact/cache upload. CI success, if obtained, is compilation/test evidence rather than native playback evidence.
