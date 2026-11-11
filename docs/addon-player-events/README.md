# Draft addon player-position interoperability patch

Base: Nuvio Desktop Dev `37ce07d351fc69e17d05a4640ef78d08deba20e2`, 11 November 2026.

## Scope

This is a locally prepared feature prototype, not an upstream-approved or installed client release. It adds standard Stremio `player` start/pause/stop events with millisecond currentTime/duration. Titles, subtitle languages, providers and episode numbers are not special-cased. It does not change subtitle selection, sorting, translation, audio sync or stream discovery.

Only enabled, matching player-resource addons are offered for explicit session consent. Declining leaves existing subtitle behavior alone. No consent is silently inferred from addon installation. Session state stays in memory. The common runtime call is currently guarded by isDesktop: Android/TV transport identity and native playback acceptance remain untested.

The event policy waits for a ready snapshot, known duration and applied initial seek; loading pauses extrapolation. Seek and state changes send events without periodic HTTP polling. Changed files/episodes stop the old identity first. Invalid snapshots or non-1x speed clear older state instead of claiming a reliable extrapolated position.

The worker holds at most one pending packet, coalesces bursts, caps fanout at eight consented addons, bounds each packet to two seconds and does not retry or log private URLs. Desktop uses a dedicated asynchronous HTTP transport with call/connect/read limits, cancellation wired to Call.cancel(), redirects disabled, no response-body retention, and no connection retry. Other platforms explicitly remain unsupported. Requests preserve installed addon transport/configuration; payloads do not contain media URLs, filenames, subtitle text or credentials. Existing profile checks suppress queued work after a profile change. End/disposal sends a best-effort stop.

Desktop libmpv downloads external subtitles with the libmpv User-Agent in the installed-client observation. Event requests use that same transport identity, without changing video HTTP headers or relaxing USS client isolation. This needs confirmation in a full patched-client playback test, especially with alternate desktop engines.

## Verification actually performed

- The three new production Kotlin files, including the Compose consent dialog, compiled using Kotlin 2.4.21, its Compose plugin and installed desktop dependencies.
- 16 focused JUnit tests passed: resume, initial invalid state, pause/resume/seek, buffering, identity changes, stop, rate limitation, generic IDs, bounded fanout, profile mismatch, queued-work suppression, burst coalescing, provider failure isolation, and once-only post-consent refresh after a successful current delivery. Refused, failed, superseded, unrelated and embedded-track cases do not trigger reload.
- The compiled Kotlin event serializer contacted a loopback-only fixture using the actual USS 2.13.69 PlaybackPositions/parser/SRT insertion module. A simulated twelve-second preparation advance changed resumed position from 185 to 197 seconds; the compound notice occupied 198.5 to 208.5 seconds and retained dialogue. Paused seek remained fixed and stop cleared the position. This is HTTP/module evidence, not native renderer evidence.
- A focused compile of the actual Desktop HTTP transport with a synthetic client binding passed loopback acceptance: empty successful 204, matching transport identity, redirect refusal, a two-second total call limit, and prompt coroutine cancellation. This is not native renderer evidence.
- The proposed #1060 companion regression and its six existing focused parser/presentation tests passed: seven total. Two same-English-language tracks kept distinct labels, incoming order, IDs, URLs, language and addon name. The old label worktree was not modified.

Focused reproduction uses the existing local JRE/compiler and app jars only, with no AI/provider requests, cloud writes or credentials. The verification script accepts explicit toolchain and installed-app paths. Its focused compile does not compile all of Dev or replace the full application build.

## Remaining acceptance and upstream policy

Update: [the native AutoSync follow-up](native-autosync-review-20261111.md) records later same-file playback and 66 passing focused checks. Statements below about pending native playback describe the earlier build stage. The late-load note interval and fresh full-suite outcome are still separate acceptance items.

- Full Dev application and the Windows C++ player bridge compiled in CI. Native movie playback with the patched application has not run. No installed Nuvio application was replaced and no perfect-sync claim is made.
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

## Initial full-build result

Feature commit c65ced3152a11654679edc1e7b5476662880e939 was verified in https://github.com/EzerchE/NuvioDesktop/actions/runs/43008412217 on the standard public Windows runner. buildWindowsPlayerBridge, desktopJar and test compilation completed. The full desktop suite reported 1,899 tests, nine failures and one skip (1,889 passed). The new player-event suite was not among the failures; the same 16 focused tests also passed locally.

The overall workflow is failed, not green. Failures are two DesktopCacheTest cases, CollectionCardRemoteImageTest, PluginRuntimeDesktopTest, SettingsNavigationTest, three LinuxUpdateInstallTest cases and WatchProgressIdentityTest. These occur outside the edited modules, and that initial run did not include a base comparison. The subsequent comparison below now separates reproduced base failures from intermittent failures. Do not silently exclude them or report a clean complete suite. Build outputs were not uploaded or installed; actual patched-native video/consent rendering remains pending.

The initial checkout failure came from an upstream gitlink without a matching .gitmodules URL. The verification workflow now uses a credential-free public checkout. A superseded build was cancelled when the actual HTTP cancellation fix was pushed, rather than running both builds to completion.

## Permission tradeoff

These player events provide position and state, not audio, embedded subtitle text or video filename/hash. They do not independently improve translation or verify sync. For USS their immediate benefit is positioning the info note; basic search, translation and reference alignment must remain usable without this activity permission. The current USS setting is optional and defaults off. A bounded native subtitle-loaded notice is the preferred long-term integration and should be discussed before expanding user permissions merely for a notice.

## Subsequent comparison and isolated bundle

Run https://github.com/EzerchE/NuvioDesktop/actions/runs/43011337221 compared unchanged Dev with feature commit 6deef9c. The base's 50 selected tests failed seven cases. The patched full 1,899-test suite failed five cases, skipped one and passed 1,893. The two Windows cache resource locks reproduced on base and passed after explicit resource closure; all five remaining failures also reproduce on base. The historical image-animation and QuickJS failures did not reproduce in either comparison and remain intermittent.

See [the detailed nine-failure review](test-failure-review-20261111.md). The workflow's success describes build/bundle export, not a green full suite; failure counts are retained explicitly. An own-fork unpublished draft bundle was authorized and exported without Actions artifact/cache storage. Its SHA256 was checked before extraction into an isolated directory. The original installed Nuvio application was not replaced; separate APPDATA, LOCALAPPDATA and user.home directories are used. Native acceptance is still pending guest-mode entry and explicit user activity consent.

USS server release 2.13.70 passed its own Linux/Windows CI and protected Oracle publication independently of this client prototype. Its activity permission remains optional/off by default. This does not ship the prototype to ordinary Nuvio users.
