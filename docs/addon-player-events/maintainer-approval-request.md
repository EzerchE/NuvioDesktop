# Feature approval request: opt-in standard addon player events on Desktop

Draft only. Not submitted. Please review the final build/playback evidence before posting.

## Problem

Subtitle addons cannot place a short delivery note at a resumed playback position when the client does not emit standard `player` events. A note inside an ordinary SRT file cannot know when it was actually rendered. Subtitles that were already loaded before the user granted activity permission also need a carefully bounded refresh.

## Proposed Desktop scope

- Offer explicit, session-only consent for enabled addons declaring a matching `player` resource. Declining preserves existing behavior.
- Send standard start/pause/stop events, plus the current state on seek, with currentTime/duration in milliseconds. Wait for valid position/duration and applied initial resume seek; never advertise a speculative 0/0 position.
- Scope state to profile and playback identity. Coalesce bursts, bound targets and deadlines, avoid polling/retries, and keep all state transient.
- Preserve the selected subtitle's originating manifest in memory. After a successful current event delivery, refresh only the currently selected, consented addon's subtitle once. Keep selection, video position and manual delay. Do not refresh embedded tracks, unrelated addons, superseded files, refused requests or failed/ambiguous deliveries.
- Do not send signed media URLs, filenames, subtitle text, provider keys or addon credentials to another addon. Existing private addon configuration remains on its own transport.

This is title-, episode- and subtitle-language-independent. Initial integration is Desktop only. Android/TV and other engines need separate transport/renderer acceptance. Desktop event and native subtitle HTTP identities must agree; the prototype uses the observed libmpv identity without changing video headers.

## Why request approval first

This adds an optional client behavior and consent UI. It should not be bundled into a subtitle label bugfix or described as an already-approved regression fix. Would maintainers approve this direction, and prefer session-only consent, persisted per-addon permissions, or a native subtitle-loaded notification surface instead?

## Alternatives and boundaries

Guessing a video position on the addon server is unreliable. Repeating notices across the whole SRT is distracting and inflates subtitle text. A native, bounded plain-text subtitle-loaded notice would be stronger than timing estimates because it knows the real render/load event. No audio access or automatic audio verification is included in this patch.

## Related reports

- #1059 / #1070: missing filename/hash hints, a separate matching issue.
- #1060 / #1072: preserve explicit subtitle labels, a separate picker issue.
- https://github.com/Stremio/stremio-addon-sdk/blob/master/docs/api/requests/definePlayerHandler.md : current standard event contract. Its response is ignored and does not provide an overlay API.

## Evidence to attach

The full Desktop application and Windows native bridge compiled at c65ced3. The full suite ran 1,899 tests with nine failures and one skip; do not describe the workflow as green. The new 16 focused event tests passed, along with a real Desktop HTTP transport loopback probe for cancellation, timeout, redirect refusal and an empty successful response. See README for the exact failing suites and CI link. Permission-before/after-download order, same-file resume, long preparation and actual patched-native rendering must still be reported separately with actual outcomes. Do not replace pending acceptance with fixture screenshots or claim guaranteed sync.

All fixtures must be synthetic, loopback-only and credential-free. Keep permission copy localized before an upstream release. No extra app or paid account is required of addon users.

## Product question

Position events do not expose audio, embedded tracks or file hashes and do not by themselves improve sync or translation. For a short information note, a native selected-subtitle-loaded notice may be more proportionate than broad watch-activity permission. Please advise on this direction before further implementation or rollout.
