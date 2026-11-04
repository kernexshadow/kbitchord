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
