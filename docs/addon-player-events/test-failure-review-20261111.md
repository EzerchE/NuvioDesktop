# Desktop failure review, 11 November 2026

Scope: player-event prototype on Dev base `37ce07d351fc69e17d05a4640ef78d08deba20e2`. No personal profile, keys, playback links or user logs are included.

## Fresh comparison

Run: https://github.com/EzerchE/NuvioDesktop/actions/runs/43011337221

- Unchanged base: 50 tests in the six implicated suites, seven failures.
- Patched application: 1,899 full-suite tests, five failures, one skip, 1,893 passes. Application, Windows bridge and isolated distributable compiled.
- The two cache failures reproduced before the fix and passed afterward. All five remaining failures also reproduced on the unchanged base. The two other historical failures did not reproduce on either comparison run; they remain intermittent, not proven fixed.
- The workflow intentionally exported a draft test bundle even with recorded test failures. Its successful export status does not mean the full test suite was green.

| Historical failures | Count | Evidence and disposition |
| --- | --- | --- |
| DesktopCacheTest jar and file resource cleanup | 2 | Confirmed Windows file locks: the unchanged base could not delete second.jar or lib.bin because another process still held them. Resource header reads opened file streams, and JarURLConnection caching retained JarFiles. The patch disables URL connection caching, closes the metadata JarFile and file stream, and disables caching for the subsequent resource read. Both original regressions pass in the patched full suite. |
| LinuxUpdateInstallTest | 3 | Confirmed platform-specific expectation mismatch. The test expects `/tmp/updates/...`; File.absolutePath on the Windows runner is `D:\tmp\updates\...`. The replacement-script substring assertion has the same issue. These tests need host-independent fixture expectations; this is not evidence of movie/subtitle misbehavior. |
| SettingsNavigationTest | 1 | The test simulates Android via `ios=false` but the production policy additionally reads the real `isDesktop` value. Buffer settings are deliberately hidden on Desktop. A future test-only correction should explicitly model platform input or assert the host's policy; do not alter Desktop behavior merely to make the Android assumption pass. |
| WatchProgressIdentityTest | 1 | The legacy completion fixture uses a one-second duration and expects 94% to count as completed. WatchingPolicies deliberately treats durations below 121 seconds as placeholder/error clips. Use a realistic-duration fixture to test legacy identity/completion preservation; do not weaken the placeholder guard. |
| PluginRuntimeDesktopTest concurrent QuickJS execution | 1 | Previously failed with QuickJsException; passed on both the unchanged-base and patched comparison. Exact transient cause remains unproven. No plugin-runtime behavior was changed. |
| CollectionCardRemoteImageTest extensionless animation | 1 | Previously timed out waiting for a rendered color; passed on both comparison runs. Exact rendering phase/cause remains unproven. No image decoder or UI timeout was relaxed. |

The five host/fixture expectation failures were not silently excluded or called fixed. Any follow-up correction should be separate from the requested new player-event behavior. Existing source-label and filename PRs remain separate.

## Bundle boundary

Follow-up: the five deterministic fixture failures have now been corrected without changing production behavior. Sixty-six targeted tests passed locally, including these suites and the new network/refresh checks. A fresh complete suite is required for the combined feature; the earlier comparison is not being rebuilt on every push. See [native interoperability evidence](native-autosync-review-20261111.md).

Only an own-fork, unpublished draft release was authorized for native acceptance. The unsigned app is extracted into an isolated directory, using separate APPDATA, LOCALAPPDATA and user.home values. The installed Nuvio app and its original configuration are not replaced. Never upload the local test profile or its logs wholesale. The bundle is a test build, not an official Nuvio release.

## Remaining native acceptance

Record actual video decoding, initial subtitle arrival before activity permission, explicit user consent, one successful current event acknowledgement, one selected-addon reload, matching transport identity and the rendered note interval. Protocol or unit evidence alone cannot establish native rendering. Real-video testing uses a public Blender movie and explicitly synthetic control subtitles, with no AI or account-specific provider calls; this does not test translation quality or audio synchronization.
