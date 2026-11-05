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
  `feat/direct-source-early-lookup` on `kernexshadow/BitChord`, head `599cef84`, based on upstream `v1.9.1`), cherry-picked.
  A "Find next song early" setting under Direct Source Stream: once the playing track has run 10 s without a seek or skip
  and is within 10 min of its end, the next track's addon answer is looked up and pinned; its bytes still wait for the
  usual 25 s window.
- **Files:** `data/settings/AppSettings.kt`, `playback/{AudioCache,DirectSourceGate,PlaybackService,StreamChoice}.kt`,
  `ui/screens/SettingsSheet.kt`, `res/values*/strings.xml` (two strings in every locale), `StreamChoiceTest.kt`.
- **Commits:** `0244b308`, cherry-pick of `599cef84` (its message ends with "cherry picked from commit …").
- **Remove:** revert the cherry-pick.
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
  Same added and removed lines as the PR; two conflicts were resolved because `v1.9` has no addon quality picking (added
  in `v1.9.1`): `AddonClient.kt` keeps `v1.9`'s log line without the quality, and `AddonSourceTest.kt` does not import
  `AddonQualityOption`.
- **Files:** `data/sources/{AddonSource,RetryHints}.kt`, `data/sources/addon/{AddonClient,AddonModels}.kt`,
  `data/sources/module/SharedCalls.kt`, `playback/QualityUpgrade.kt` (also edited by K-08), tests `AddonSourceTest.kt`,
  `QualityUpgradeRetryTest.kt`, `SharedCallsTest.kt`.
- **Commits:** cherry-pick of `df256c4a`, with the conflict resolution above.
- **Remove:** revert the cherry-pick.
- **Upstream status:** upstream PR #1523 (open).
- **Watch:** as K-06. When KBitChord merges `v1.9.1` or later, the `AddonClient.kt` and `AddonSourceTest.kt` lines will
  conflict again: take upstream's quality-aware version plus the PR's `keep` predicate. Check with
  `:app:testDevDebugUnitTest --tests 'com.music.bitchord.AddonSourceTest' --tests 'com.music.bitchord.QualityUpgradeRetryTest' --tests 'com.music.bitchord.SharedCallsTest'`.
