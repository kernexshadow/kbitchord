# KBitChord changes

Everything KBitChord adds on top of [kushagrasinghx/BitChord](https://github.com/kushagrasinghx/BitChord). Each entry says
why the change exists, which files it touches, and how to remove it. Read this first when an upstream merge conflicts.

Files under `app/src/prod/res`, `app/src/dev/res`, `tv/src/prod/res`, `tv/src/dev/res`, `kbitchord-branding/`, the
KBitChord workflow and KBitChord-only tests are KBitChord-only and cannot conflict. **Upstream files that KBitChord edits**
are the ones to watch:

| Upstream file | Changed by |
|---|---|
| `app/src/dev/AndroidManifest.xml`, `tv/src/dev/AndroidManifest.xml` | [K-01](#k-01-branding-launcher-icon-tv-banner-app-name) |
| `app/build.gradle.kts`, `tv/build.gradle.kts` | [K-01](#k-01-branding-launcher-icon-tv-banner-app-name), [K-02](#k-02-application-ids-and-updater-source) |
| `app/src/main/java/com/music/bitchord/data/AppUpdateChecker.kt` | [K-02](#k-02-application-ids-and-updater-source), [K-05](#k-05-kbitchord-revisions-in-the-updater) |
| Playback and settings files and strings listed in K-06 | [K-06](#k-06-find-next-song-early-upstream-pr-1439) |
| Matching, playback and addon files listed in K-07, K-08, K-09 | [K-07](#k-07-shared-release-before-a-runtime-vouches-for-another-artist-upstream-pr-1456), [K-08](#k-08-keep-a-mid-song-lossless-upgrade-and-replay-it-from-disk-upstream-pr-1475), [K-09](#k-09-ask-an-addon-again-when-it-says-not-yet-upstream-pr-1523) |
| `BitChordApplication.kt`, `ui/screens/SettingsSheet.kt`, `playback/PlaybackService.kt`, `playback/DirectSourceGate.kt`, `sharedUi/…/ui/player/PlayerControls.kt` | [K-10](#k-10-start-on-youtube-if-the-addon-is-slow) |
| `data/sources/addon/AddonClient.kt`, `data/sources/AddonSource.kt`, `BitChordApplication.kt` | [K-11](#k-11-addon-manifest-off-the-critical-path) |
| `playback/AudioCache.kt` (key factory, `dropForeignEntries`, new `kb*` functions), `playback/PlaybackService.kt` (the offer calls) | [K-13](#k-13-the-offered-lossless-copy-for-the-next-play) |

Upstream status: **KBitChord only** = not meant for upstream; **upstream PR #N** = proposed; **in upstream since X** = the
change can be dropped from KBitChord after merging X.

---

## K-01 Branding: launcher icon, TV banner, app name

- **Why:** KBitChord must be recognisable next to the official BitChord on the same phone or TV.
- **What:**
  - K launcher icon, adaptive: the K on navy, with the K's alpha as the monochrome (themed icon) layer. Also the round icon
    and the "Default" choice in Settings → App icon.
  - K TV banner.
  - App name "KBitChord" in every locale; "KBitChord TV" for the TV app; the dev flavors show "KBitChord Dev" and
    "KBitChord TV Dev".
  - Not branded: the alternative launcher icons in Settings → App icon (glass, neon, monochrome), the in-app BitChord mark
    (`ic_logo`, TV home header, Replay cards), the notification icon, and "BitChord" in UI text. They stay upstream's.
- **Files:** `app/src/{prod,dev}/res/**` and `tv/src/{prod,dev}/res/**` (`mipmap-anydpi/ic_launcher*.xml`,
  `drawable-nodpi/kbitchord_launcher_foreground.png`, `values/kbitchord_launcher.xml`, `values*/strings_kbitchord.xml`,
  TV `drawable*/tv_banner.png`), `kbitchord-branding/**` (source artwork and generator script). Upstream files edited, one
  line each, where upstream sets the name outside the resources: the application label in `app/src/dev/AndroidManifest.xml`
  and `tv/src/dev/AndroidManifest.xml`, and the `resValue("string", "app_name", …)` lines in `app/build.gradle.kts` (dev)
  and `tv/build.gradle.kts` (dev and prod). A flavor's `resValue` outranks that flavor's `res` folder, so the overlay alone
  would leave upstream's name in the default locale.
- **Commits:** `e7bae167`, `18416bf3`.
- **Remove:** revert both commits; the upstream resources in `app/src/main/res` and `tv/src/main/res` take over.
- **Upstream status:** KBitChord only.
- **Watch:** a new upstream locale with `app_name` needs the script rerun. An overlay only replaces an upstream resource
  with the same name and qualifiers: if upstream moves or renames the launcher icon or TV banner, update the script.

## K-02 Application IDs and updater source

- **Why:** KBitChord installs next to the official apps, is signed with its own key, and must update from KBitChord
  releases.
- **What:** phone `com.kbitchord.app`, dev `com.kbitchord.dev`; TV `com.kbitchord.app.tv`, dev `com.kbitchord.dev.tv`
  (was `com.music.bitchord`, `com.dev.bitchord`, `com.music.bitchord.tv`, `com.dev.bitchord.tv`). The updater reads
  `kernexshadow/kbitchord`'s latest release, and takes the assets named `KBitChord-*` (phone) or `KBitChordTV-*` (TV).
  The namespace and Kotlin packages stay `com.music.bitchord`. The watch app keeps upstream's IDs (see `.claude/CLAUDE.md`).
- **Files:** `app/build.gradle.kts` and `tv/build.gradle.kts` (3 lines each: two `applicationId`, `UPDATE_APK_PREFIX`),
  `AppUpdateChecker.kt` (`LATEST_RELEASE_URL`).
- **Commits:** `d61f69bd`.
- **Remove:** revert the commit. Installed KBitChord apps would then be replaced by, or conflict with, the official apps.
- **Upstream status:** KBitChord only. A configurable repo for the updater (a Gradle property with upstream's as default)
  could be proposed upstream to shrink this diff.

## K-03 Upstream sync and release workflow

- **Why:** follow upstream releases without manual work, and publish signed APKs.
- **What:** `.github/workflows/kbitchord-sync-release.yml`: scheduled merge of upstream release tags, manual `+k.N`
  revision builds, versionName stamping (phone and TV), disabling inherited workflows, signed phone and TV release builds
  without the watch app, a check that every APK carries the KBitChord certificate, and a GitHub release. Details in
  `.claude/CLAUDE.md`.
- **Files:** `.github/workflows/kbitchord-sync-release.yml`.
- **Commits:** `cb5d86bd` and "kbitchord: check the certificate of every release APK".
- **Remove:** delete the workflow; releases then have to be built by hand.
- **Upstream status:** KBitChord only.

## K-04 Project notes

- **Why:** keep how KBitChord works, and why each change exists, next to the code.
- **Files:** `.claude/CLAUDE.md` (force-added: upstream's `.gitignore` ignores `.claude/`), `KBITCHORD_CHANGES.md`.
- **Upstream status:** KBitChord only.

## K-05 KBitChord revisions in the updater

- **Why:** manual KBitChord builds of the same upstream version (`+k.1`, `+k.2`, …) must be offered as updates, while any
  newer upstream version still wins. Upstream's parser read `1.9+k` as `1.0`, because `9+k` is not a number.
- **What:** `parseVersion` splits off `+` build metadata and reads `+k` / `+k.N` as a revision (0 / N); `isNewer`
  compares it only when everything else is equal, so `1.9+k < 1.9+k.1 < 1.9.1+k`. Other build metadata is ignored, and
  upstream's beta rule (`1.9-beta2 < 1.9`) is unchanged. `isNewer` is `internal` for the test.
- **Files:** `AppUpdateChecker.kt` (upstream file), `app/src/test/java/com/music/bitchord/data/KBitChordRevisionTest.kt`
  (KBitChord-only tests).
- **Commits:** `2c4d7257`.
- **Remove:** revert the commit and stop publishing `+k.N` builds (the workflow's manual mode depends on it).
- **Upstream status:** KBitChord only.
- **Watch:** upstream edits to `parseVersion`/`isNewer` will conflict. Keep the revision as the last tie-breaker and run
  `:app:testDevDebugUnitTest --tests 'com.music.bitchord.data.KBitChordRevisionTest'`.

## K-06 Find next song early (upstream PR #1439)

- **Why:** under Direct Source Stream, skipping to the next track waits for the addon lookup. Looking the next track up
  early makes skips fast.
- **What:** [kushagrasinghx/BitChord#1439](https://github.com/kushagrasinghx/BitChord/pull/1439) by kernexshadow (branch
  `feat/direct-source-early-lookup` on `kernexshadow/BitChord`, head `0d6007da`, based on upstream `v1.9.1`), cherry-picked.
  A "Find next song early" setting under Direct Source Stream: once the playing track has run 3 s without a seek or skip
  and is within 10 min of its end, the next track's addon answer is looked up and pinned; its bytes still wait for the
  usual 25 s window. The PR first waited 10 s (`EARLY_LOOKUP_AFTER_MS` in `DirectSourceGate`).
- **Files:** `data/settings/AppSettings.kt`, `playback/{AudioCache,DirectSourceGate,PlaybackService,StreamChoice}.kt`,
  `ui/screens/SettingsSheet.kt`, `res/values*/strings.xml` (two strings in every locale), `StreamChoiceTest.kt`.
- **Commits:** `0244b308`, cherry-pick of `599cef84` (its message ends with "cherry picked from commit …"), and the
  conflict resolution in "Merge upstream v1.9.1": v1.9.1 passes YouTube's length to the addon search
  (`directStream(target, lengthOf = …)`), which now sits in the PR's `lookupFor`, next to its `lookAhead`. Then
  `3df40382`, cherry-pick of `0d6007da`: the 3 s wait.
- **Remove:** revert both cherry-picks, then put v1.9.1's `lengthOf` back into `DirectSourceGate.lookup`.
- **Upstream status:** upstream PR #1439 (open).
- **Watch:** if the PR gets more commits, cherry-pick them too. When an upstream release contains the PR, merging it may
  conflict in the files above: take upstream's version and delete this entry. If the PR is closed without merging, revert.
  Check with `:app:testDevDebugUnitTest --tests 'com.music.bitchord.StreamChoiceTest'`.

## K-07 Shared release before a runtime vouches for another artist (upstream PR #1456)

- **Why:** the track matcher accepted a candidate by a different artist when the two runtimes agreed within 2 s, so a
  common title could swap in the wrong song (Mat Kearney's "As Long As I Live" for Catherine Russell's).
- **What:** [kushagrasinghx/BitChord#1456](https://github.com/kushagrasinghx/BitChord/pull/1456) by kernexshadow (branch
  `fix/different-artist-match` on `kernexshadow/BitChord`, head `3d6ba98f`, based on upstream `v1.9.1`), cherry-picked
  unchanged. The runtime exception for another artist now also needs the release to agree: both albums named and equal,
  or one side's title naming the other's album in full. Otherwise the track stays on YouTube.
- **Files:** `data/sources/TrackMatcher.kt`, `SourcesTest.kt`.
- **Commits:** cherry-pick of `3d6ba98f` (its message ends with "cherry picked from commit …").
- **Remove:** revert the cherry-pick.
- **Upstream status:** upstream PR #1456 (open).
- **Watch:** as K-06. Check with `:app:testDevDebugUnitTest --tests 'com.music.bitchord.SourcesTest'`.

## K-08 Keep a mid-song lossless upgrade and replay it from disk (upstream PR #1475)

- **Why:** a track that started on YouTube and swapped up to FLAC mid-song paid for the upgrade on every play: the
  upgraded copy was never complete and was purged once the queue moved on.
- **What:** [kushagrasinghx/BitChord#1475](https://github.com/kushagrasinghx/BitChord/pull/1475) by kernexshadow (branch
  `feat/keep-lossless-upgrade` on `kernexshadow/BitChord`, head `2d5fc31d`, based on upstream `v1.9.1`), cherry-picked
  unchanged. After the swap the missing parts of the upgraded rendition are fetched from its stream URL; once whole it is
  kept, and the next play of the track starts from it in FLAC with no second look and no swap.
- **Files:** `MainActivity.kt`, `playback/{AudioCache,PlaybackService,QualityUpgrade}.kt` (`AudioCache` and
  `PlaybackService` also edited by K-06, `QualityUpgrade` by K-09), new `KeptUpgradeTest.kt`.
- **Commits:** cherry-pick of `2d5fc31d`.
- **Remove:** revert the cherry-pick (after K-09 if both go: they share `QualityUpgrade.kt`).
- **Upstream status:** upstream PR #1475 (open).
- **Watch:** as K-06. Check with `:app:testDevDebugUnitTest --tests 'com.music.bitchord.KeptUpgradeTest'`.

## K-09 Ask an addon again when it says "not yet" (upstream PR #1523)

- **Why:** an addon whose debrid service is still downloading can answer "not yet" with `retryAfter` instead of holding
  the request open. Playback then starts on YouTube at once and switches to the lossless copy when the addon has it.
- **What:** [kushagrasinghx/BitChord#1523](https://github.com/kushagrasinghx/BitChord/pull/1523) by kernexshadow (branch
  `feat/upgrade-when-addon-ready` on `kernexshadow/BitChord`, head `df256c4a`, based on upstream `v1.9.1`), cherry-picked.
  Same added and removed lines as the PR. Cherry-picked onto `v1.9`, which has no addon quality picking, so it needed two
  conflict resolutions; the merge of `v1.9.1` brought those lines back to the PR's own version.
- **Files:** `data/sources/{AddonSource,RetryHints}.kt`, `data/sources/addon/{AddonClient,AddonModels}.kt`,
  `data/sources/module/SharedCalls.kt`, `playback/QualityUpgrade.kt` (also edited by K-08), tests `AddonSourceTest.kt`,
  `QualityUpgradeRetryTest.kt`, `SharedCallsTest.kt`.
- **Commits:** cherry-pick of `df256c4a`, and the conflict resolution in "Merge upstream v1.9.1" (`AddonClient.kt`:
  v1.9.1's log line with the quality plus the PR's `keep`; `AddonSourceTest.kt`: v1.9.1's imports plus `AddonStream`).
- **Remove:** revert the cherry-pick.
- **Upstream status:** upstream PR #1523 (open).
- **Watch:** as K-06. Check with
  `:app:testDevDebugUnitTest --tests 'com.music.bitchord.AddonSourceTest' --tests 'com.music.bitchord.QualityUpgradeRetryTest' --tests 'com.music.bitchord.SharedCallsTest'`.

## K-10 Start on YouTube if the addon is slow

- **Why:** under Direct Source Stream a track whose addon answer isn't ready waits up to 15 s in silence before falling
  back to YouTube, and then swaps the addon copy in mid-song with a short cut. The user wanted playback to start at once
  and never be cut unless they ask.
- **What:** two settings under Direct Source Stream (Settings → Audio quality, phone only): "Start on YouTube if the
  addon is slow" (off by default; first released in v1.9.1+k.2 as "Don't wait for the addon") and "Wait for the addon",
  0–10 s, 4 s by default (3 s in v1.9.1+k.3 and +k.4). Against Nebula an answer took 2.6–3.7 s, so 3 s sent half the
  tracks measured to YouTube. Skipping the pre-play size check instead was tried and dropped: the answer came ~0.8 s
  sooner but the first note did not, because that check is also the first request to the file's server and the player
  then paid it.
  - While the track is only *next* inside the window, the addon is waited on as before (up to 15 s, unheard). Once it has
    to start (a tap, a skip, or a transition that came first), YouTube's URL is resolved beside the lookup and the addon
    gets the "Wait for the addon" time to answer; after it, YouTube plays. 0 s starts on whichever answers first, which is
    nearly always YouTube (about 0.3 s against seconds for an addon). An answer already pinned by "Find next song early"
    plays from the addon as usual.
  - The wait counts from when that track's lookup began, not from the tap (`DirectSourceGate.waitLeftMs`): a lookup
    started early ("Find next song early", or read-ahead inside the window) that still has no answer when the listener
    skips to the track goes straight to YouTube.
  - A YouTube start keeps looking. A better copy found for the playing track is not swapped in: the player shows
    "FLAC available" (or "Hi-Res", "320 kbps", "Dolby Atmos") as a pill where the quality badge goes, shown whether or not
    stats for nerds is on. Tapping it, or "Upgrade quality" in the player menu, swaps it in through the usual audition.
    Leaving it alone plays YouTube to the end; the offer goes away with the track.
  - Not on TV: the TV settings don't show it and the TV player has no button, so the TV app behaves as before.
- **Files:** new `data/settings/KBitChordSettings.kt` (own preferences file `kbitchord_settings`),
  `ui/screens/KBitChordSettingsRows.kt` (the slider sub-row),
  `playback/HeldUpgrade.kt`, `shared/…/data/UpgradeOffers.kt`, `res/values{,-pt}/strings_kbitchord_playback.xml`,
  `sharedUi/…/composeResources/values{,-pt}/strings_kbitchord.xml`, test `KBitChordUpgradeOfferTest.kt`. Upstream files
  edited, each marked `KBitChord (K-10)`: `BitChordApplication.kt` (init, one line), `ui/screens/SettingsSheet.kt` (the
  two rows), `playback/DirectSourceGate.kt` (`awaitPlaying`), `playback/PlaybackService.kt` (`resolveDirect` branch,
  `resolveDirectWithoutWaiting`, `acceptUpgradeOffer`, the two `swapIn` calls in `lookForBetterCopy`,
  `upgradeQualityNow`, `driveDirectWindow`, the collector in `onCreate`), `sharedUi/…/ui/player/PlayerControls.kt`
  (`PlaybackQualityLabel`, `UpgradeOfferButton`).
- **Tested:** on the phone emulator with a local mock addon whose `/stream` answered after 12 s: the track started on
  YouTube at once, "FLAC available" appeared when the answer came, and a tap swapped it in (676 ms seam). With the 3 s
  wait: a mock answering its stream in 1 s played from the addon from the first note (2.6 s after the tap, search and
  checks included); one answering in 6 s went to YouTube at 3.0 s and offered the FLAC when it came.
- **Remove:** revert the commit.
- **Upstream status:** upstream PR #1592 (open; settings in `AppSettings`, strings in every locale, independent of #1439).
- **Watch:** upstream changes to `resolveDirect`, `lookForBetterCopy`, `swapIn` or the quality badge. Check with
  `:app:testDevDebugUnitTest --tests 'com.music.bitchord.KBitChord*'`.

## K-11 Addon manifest off the critical path

- **Why:** every addon search and stream request reads the addon's manifest first (its settings and whether it asks
  for `bitchordAuth`). The client keeps a manifest for 10 minutes and then fetches it again in front of the next
  request, so the first track after the app starts, and one track every ten minutes after that, waited for a manifest
  round trip: 0.66 s measured against Nebula, which on its own pushed a 3.2 s lookup past K-10's 3 s wait.
- **What:**
  - `AddonClient.manifestForRequest()`: the last manifest read answers at once; an expired one is refreshed in the
    background for the next request. Used by `settingsFor` and `AddonSource.refreshPolicy` (the request paths). The
    Sources screen and the add-addon test still read a fresh one through `manifest()`.
  - `KBitChordWarmup.warmAddonManifests()`: reads every enabled addon's manifest when the app starts.
  - An addon that turns `bitchordAuth` on is still answered: its `401` asks for credentials, which the client sends.
- **Files:** new `data/sources/KBitChordWarmup.kt`. Upstream files edited, each marked `KBitChord (K-11)`:
  `data/sources/addon/AddonClient.kt` (`lastManifest`, `manifestForRequest`, two lines in `manifest`, `settingsFor`),
  `data/sources/AddonSource.kt` (`refreshPolicy`), `BitChordApplication.kt` (one line after startup).
- **Tested:** `AddonSourceTest`, `AddonAuthTest`, `SourcesTest` pass. On the phone emulator with Nebula: the manifest was
  read at startup, and the first tracks played after it read no manifest before their search.
- **Remove:** revert the commit.
- **Upstream status:** upstream PR #1590 (open; warm-up in `SourceRegistry.warmAddonManifests`).
- **Watch:** upstream changes to `AddonClient.manifest`, `settingsFor` or `AddonSource.admitted`/`refreshPolicy`.

## K-13 The offered lossless copy, for the next play

- **Why:** with K-10, a track that started on YouTube and was offered a FLAC the listener didn't take started on its
  cached YouTube copy again the next time, and offered the FLAC again. The app already knew the addon had it.
- **What:** when K-10's offer is made and its track stops playing:
  - **On Wi-Fi** (an unmetered connection) the offered copy is downloaded whole in the background, into the rendition
    an upgrade swaps to (`#hifi`), while the track keeps playing YouTube. A tap on the offer swaps to it from disk. Once
    the track has stopped playing it is marked kept, so the next play starts on it, and the YouTube copy is dropped
    (kept when Automix's analysis still reads it). Measured: a 26.8 MB FLAC whole in 13 s; the next play started on it
    in 0.98 s, with no request to YouTube or the addon.
  - **Otherwise** (mobile data, or the download never came whole) the YouTube copy's cache entry is tagged as having a
    lossless copy at the addon. The next play then opens a separate entry (`#kblossless`), so it goes to the resolver
    and K-10 waits for the addon instead of starting on the copy on disk. Offline, `completeSibling` still hands back
    the YouTube copy. Measured after an app restart on mobile data: the addon answered in 2.8 s and the track played
    FLAC from the first note.
  - The tag lives in the YouTube entry's own content metadata, so it goes with that entry (eviction, Clear song cache).
- **Files:** `playback/HeldUpgrade.kt` (offers remember their queue item and cache entry; the download and the settling),
  upstream files edited, each marked `KBitChord (K-13)`: `playback/AudioCache.kt` (one line in the key factory, one in
  `dropForeignEntries`, and `kbKeepOfferedCopy`, `kbSettleOffer`, `kbLosslessKey`), `playback/PlaybackService.kt` (the
  item URI passed to `HeldUpgrade.offer`).
- **Tested:** on the phone emulator with Nebula, both paths above. Not tested: offline replay of a tagged track, a tap
  on the offer while the download is running, a physical device.
- **Remove:** revert the commit.
- **Upstream status:** KBitChord only; builds on K-10 (upstream PR #1592).
- **Watch:** upstream changes to `AudioCache`'s key factory, `keptUpgradeFor`, `completeSibling`, `keepsUpgrade`,
  `dropForeignEntries` or `swapIn`'s `alreadyFilled`.
