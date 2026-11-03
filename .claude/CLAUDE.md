# KBitChord

KBitChord is a personal build of [kushagrasinghx/BitChord](https://github.com/kushagrasinghx/BitChord), the Android music
app, published at [kernexshadow/kbitchord](https://github.com/kernexshadow/kbitchord). It follows upstream releases
automatically. It differs from upstream in branding, application IDs, the in-app updater source, an extra revision number
for its own builds, and upstream pull requests it carries before they are released. Everything KBitChord adds is listed in
[KBITCHORD_CHANGES.md](../KBITCHORD_CHANGES.md).

BitChord is GPLv3, so the repository stays public: every published APK must have its source available.

## Repositories and branches

| Remote | Repository | Role |
|---|---|---|
| `origin` | `kernexshadow/kbitchord` | This repo. `main` is the only branch and the release branch. |
| `upstream` | `kushagrasinghx/BitChord` | Source of truth. Releases are tags (`v1.9`, `v1.8.1`, …) on its `main`. |

- `main` = an upstream **release tag** + the KBitChord commits on top. Only release tags are merged, never an upstream
  branch head, so every KBitChord release matches an exact upstream version.
- Contributions to BitChord itself are made from the separate checkout `C:\Users\tfout\source\repos\bitchord` and the
  `kernexshadow/BitChord` fork, on branches based on upstream's active development branch (see its `CONTRIBUTING.md`).
  Never open PRs from this repo, and never let KBitChord commits reach an upstream PR branch.
- A KBitChord feature that should also go upstream: develop it in the bitchord checkout, open the upstream PR there, then
  cherry-pick it into KBitChord `main` (`git cherry-pick -x`) and record it in KBITCHORD_CHANGES.md with the PR number.

## Modules

- The phone app (`app`) and the Android TV app (`tv`) are built and released.
- The watch app (`wearos`) is not part of KBitChord yet. It keeps upstream's application IDs, and KBitChord builds pass
  `-Pbitchord.bundleWear=false`, so the phone APK does not carry a watch APK. To add it later: give `wearos` the phone's IDs
  (`com.kbitchord.app` / `com.kbitchord.dev`, required for Data Layer pairing), drop the flag from the workflow, brand it,
  and add a K-entry.

## Versions and releases

| Build | Release tag | APK `versionName` | GitHub release |
|---|---|---|---|
| Automatic, upstream `v1.9` | `v1.9+k` | `1.9+k` | latest (prerelease if upstream's is) |
| Manual, revision 1 of that version | `v1.9+k.1` | `1.9+k.1` | same |
| Upstream `v1.9.1` | `v1.9.1+k`, then `+k.1`, `+k.2` | without the `v` | same |

- `+k` is semver build metadata. The updater (`AppUpdateChecker.isNewer`) strips the tag's `v` and reads `+k.N` as the last
  tie-breaker, so the order is `1.8.1+k < 1.8.1+k.1 < 1.9+k < 1.9+k.1 < 1.9.1+k`. Any other build metadata is ignored.
- `versionName` in the repo stays upstream's: `bitchordVersionName` in the root `build.gradle.kts` (phone and watch) and
  `versionName` in `tv/build.gradle.kts` (TV). The workflow stamps the release version into both at build time and never
  commits it, because the updater compares the installed `versionName` with release tags.
- `versionCode` stays upstream's. Revisions reuse it, which Android allows for updates.
- Release assets: `KBitChord-v1.9-k.1.apk` (universal) and `KBitChord-v1.9-k.1_<abi>.apk` for the phone,
  `KBitChordTV-…` for TV. `+` becomes `-` in file names, because GitHub rewrites it. The updater takes the assets whose
  names start with `UPDATE_APK_PREFIX` (`KBitChord-` or `KBitChordTV-`), the universal one first.

## Sync and release workflow

`.github/workflows/kbitchord-sync-release.yml`:

- **Automatic**, every 6 hours (cron `37 */6 * * *`, UTC): finds the newest non-draft upstream release. If `<tag>+k`
  already exists it does nothing. Otherwise it merges the upstream tag into `main`, pushes, builds, and publishes.
- **Manual**: Actions → "Sync upstream and release" → Run workflow, or
  `gh workflow run kbitchord-sync-release.yml -R kernexshadow/kbitchord -f manual=true`. It builds the current `main` as
  the next `+k.N` for the upstream version in `build.gradle.kts`, and the release notes list commits since the previous
  KBitChord release. `-f tag=<upstream tag>` (without `manual`) builds a specific upstream release.
- Merge conflicts: the job fails with an error annotation and GitHub emails the owner. Resolve locally (see below).
- It disables every other workflow in the repo. Inherited upstream workflows must not run here (`contributors-image.yml`
  would even commit to `main`).
- It runs the updater unit tests, then `:app:assembleProdRelease :tv:assembleProdRelease -Pbitchord.bundleWear=false`,
  fails if an APK is unsigned, and uploads the universal and per-ABI APKs (arm64-v8a, armeabi-v7a, x86_64) of both apps.
- Every `gh` call passes `-R`/an explicit repo: with an `upstream` remote present, `gh` defaults to kushagrasinghx.
- To rebuild a version that already has a release, delete that release and its tag first, then run the workflow again.

Secrets, referenced by name in the workflow:

| Secret | Content |
|---|---|
| `SYNC_PAT` | Fine-grained token, this repo only, Contents + Workflows read/write. Does the checkout and push, because `GITHUB_TOKEN` cannot push upstream changes to `.github/workflows`. |
| `KBITCHORD_KEYSTORE_BASE64` | The release keystore, base64. The same key as knuvio (PKCS12, one password for store and key). |
| `KBITCHORD_KEY_ALIAS`, `KBITCHORD_KEYSTORE_PASSWORD` | Its alias and password. |
| `KBITCHORD_LOCAL_PROPERTIES_BASE64` | Optional `local.properties`: `LASTFM_API_KEY`, `LASTFM_SECRET`, `LISTEN_TOGETHER_SERVER`. Without it Last.fm scrobbling is off and Listen Together starts with upstream's server. |

## Resolving an upstream merge conflict

```bash
git fetch upstream --tags
git merge v1.9.1                   # the upstream release tag
# resolve: KBITCHORD_CHANGES.md says why each KBitChord change exists and which files it touches
git push origin main
gh workflow run kbitchord-sync-release.yml -R kernexshadow/kbitchord   # publishes v1.9.1+k
```

## Rules for KBitChord changes

1. **Prefer overlays to edits.** Resources go in the flavor source sets `app/src/prod/res`, `app/src/dev/res`,
   `tv/src/prod/res` and `tv/src/dev/res`, which override `main` and never conflict with upstream. The TV app builds from
   `app/src/main` but not from the phone's flavor folders, so the TV needs its own overlays. Edit upstream files only when
   there is no other way, and keep the diff minimal.
2. **Record every change** in KBITCHORD_CHANGES.md in the same commit: what, why, files, how to remove, upstream status.
3. Commit messages start with `kbitchord:`. Code, comments and docs are in English.
4. When upstream adds a locale whose strings define `app_name`, rerun the branding script, or that locale shows "BitChord".
   A flavor's `resValue` in a build script outranks that flavor's `res` folder, so a new `resValue("string", "app_name", …)`
   upstream needs the same one-line edit as the existing ones (K-01). Check the names with
   `aapt2 dump badging <apk> | grep label`.
5. When upstream renames or moves the launcher icon (`mipmap-anydpi/ic_launcher*.xml`) or the TV banner
   (`tv/src/main/res/drawable*/tv_banner.png`), update the script: an overlay only replaces a resource with the same name
   and qualifiers.

## Branding assets

`kbitchord-branding/` holds the K artwork (`source/k-mark.png`, the same as knuvio's) and the script that generates every
KBitChord image and name (Python 3 with Pillow, NumPy, SciPy):

```bash
python kbitchord-branding/make_branding.py kbitchord-branding/source/k-mark.png <preview-dir>
```

It writes the adaptive launcher icon (K on navy, the K's alpha as the monochrome layer), the TV banner and `app_name` in
every locale to the four overlay folders, and preview images to `<preview-dir>`; check them before committing.

## Local builds

```bash
ANDROID_HOME="$LOCALAPPDATA/Android/Sdk" ./gradlew :app:assembleProdRelease :tv:assembleProdRelease -Pbitchord.bundleWear=false
ANDROID_HOME="$LOCALAPPDATA/Android/Sdk" ./gradlew :app:testDevDebugUnitTest
```

- Same toolchain as BitChord. `keystore.properties` and `local.properties` in the repo root are gitignored. Locally
  `keystore.properties` points at the Android debug keystore; the real key is used only in CI. A local release build
  therefore cannot update a KBitChord installed from GitHub, or the reverse.
- Application IDs: phone `com.kbitchord.app` ("KBitChord"), dev `com.kbitchord.dev` ("KBitChord Dev"); TV
  `com.kbitchord.app.tv` ("KBitChord TV"), dev `com.kbitchord.dev.tv` ("KBitChord TV Dev"). The `benchmark` build type adds
  `.benchmark`. All of them install next to the official BitChord apps.
- Upstream's `verifyDevInstall` task (after `installDevDebug`) still compiles `com.dev.bitchord`, upstream's dev app.
  Harmless; run `adb shell cmd package compile -m verify -f com.kbitchord.dev` for KBitChord's.
