# AGENTS.md

Guidance for working in this repo (Skytte — an Android shooting-log app). This is the single
source of truth for all coding agents; `CLAUDE.md` just imports it. Keep this file current as
conventions change.

## Documentation: keep it in sync (important)

After **every** change, check whether docs need updating and update them in the same change:
- `README.md` — features, tech stack, and the project-structure tree. Update it whenever you
  add/move/remove a user-facing feature or a notable file/package.
- `.github/CI.md` — only when the CI workflow, its secrets/variables, or distribution change.
- This `AGENTS.md` — when conventions, commands, or workflows change.

If a change doesn't affect any doc, that's fine — but verify, don't assume.

## Project overview

- Android app, **Kotlin + Jetpack Compose (Material 3)**, MVVM (ViewModel + Kotlin `Flow`).
- Persistence: **Cloud Firestore** is the source of truth (offline-first — its on-device cache
  works without a connection and syncs in the background). **Firebase Auth** (Google Sign-In via
  Credential Manager) gates the app; all data lives under `/users/{uid}/…`. Navigation: Navigation
  Compose. Serialization: kotlinx.serialization (JSON backup export/import).
- **Firebase config:** the build needs `app/google-services.json` (git-ignored; injected in CI via
  the `GOOGLE_SERVICES_JSON` secret). In the console, **Firestore** must be enabled and **Google**
  enabled as an Auth provider (which generates `default_web_client_id`, used by `AuthManager`).
  Security rules are in `firestore.rules` (deployed via the Firebase CLI; `firebase.json`).
- **The UI is entirely in Swedish.** All user-facing strings go in
  `app/src/main/res/values/strings.xml` (Swedish). Money is SEK, formatted with
  `Locale.forLanguageTag("sv-SE")` (comma decimals, e.g. `3,50 kr`).
- Toolchain: JDK 17, AGP 8.13.2, Gradle 8.13, `compileSdk/minSdk/targetSdk = 36`,
  `applicationId = se.mindphaser.skytte`. Versions are centralized in
  `gradle/libs.versions.toml`.

## Build / run / verify

```bash
./gradlew :app:installDebug        # build + install (use absolute path if cwd resets)
./gradlew :app:assembleRelease     # signed release (needs RELEASE_KEYSTORE_* env; see .github/CI.md)
```

- Emulator used during development: `emulator-5554`. ADB lives at
  `~/Library/Android/sdk/platform-tools/adb` (not on PATH).
- There are **no automated tests**; verify changes by running the app and observing behavior.
- Screenshots: `adb -s emulator-5554 exec-out screencap -p > /tmp/x.png`, then downscale with
  `sips -Z 760 /tmp/x.png --out /tmp/x_s.png` before reading.
- **Manual text entry on the emulator is unreliable** — a "Try out your stylus" popup intercepts
  `adb input text`. Data now lives in Firestore (no local SQLite to edit), so seed deterministic
  data via the app itself, the **Firestore console**, or a Firestore emulator — or import a JSON
  backup file (Settings → Importera). The old `sqlite3`/`run-as` seeding trick no longer applies.

## Versioning

Set in `app/build.gradle.kts` from the build environment — don't hand-edit per build:
- `versionCode` = `GITHUB_RUN_NUMBER` (monotonic in CI), or `1` locally.
- `versionName` = `"<base> (<run>-<shortSha>)"`, e.g. `1.0 (42-abc1234)`; locally `1.0 (dev-<sha>)`.
- Bump the `versionBase` constant by hand for real releases.
- `BuildConfig.BUILD_DATE` (UTC) is shown with the version on the Settings "about" line.

## Architecture conventions

- **Screens** live in `ui/<feature>/` as `XxxScreen.kt` + `XxxViewModel.kt`. ViewModels expose
  state as `Flow`/`StateFlow` and use a `companion object { val Factory = viewModelFactory { ... } }`
  with the `repositories()` helper in `ui/ViewModelFactory.kt`.
- **Data access:** `SkytteApp` holds a per-user `Repositories` (`data/repo/Repositories.kt`),
  rebuilt whenever the signed-in uid changes and exposed as a `StateFlow<Repositories?>` (null when
  signed out). Each repository wraps a Firestore collection: reads are `observeAll()` flows backed
  by snapshot listeners (so the offline cache emits immediately); **writes (`save`/`delete`) are
  fire-and-forget** — Firestore updates the local cache at once and the returned `Task` only
  completes on server ack, so never `.await()` a write (it would hang offline). Entity ids are
  Firestore document ids (`String`); a blank id means "not yet persisted". `SessionWithRefs` is
  joined in-memory by `combine`-ing the sessions/weapons/ammo flows (Firestore has no joins).
- **Auth gate:** `MainActivity` shows `SignInScreen` when signed out, a spinner while repositories
  build, then `SkytteAppRoot`. ViewModels never see the uid — they only touch repositories.
- **Shared top bar:** `ui/SkytteTopBar.kt` — tinted bar with title + a settings gear, used by
  every main tab. The root `Scaffold` in `ui/SkytteAppRoot.kt` sets `contentWindowInsets =
  WindowInsets(0)` + `consumeWindowInsets(padding)` so the per-screen top bars own the status-bar
  inset (don't reintroduce a double inset).
- **Dashboard** (`ui/dashboard/`) computes all stats in-memory in `DashboardViewModel.buildStats`
  from `SessionRepository.observeAll()`; monthly charts reuse `MonthlyChartCard` (`MonthBucket`).
- **Cost** is centralized in `SessionWithRefs.totalCost()` (`data/Session.kt`) — reused by the
  session card and the dashboard. Reuse it; don't recompute the formula inline.

## Firestore schema evolution (no migrations)

Firestore is schemaless, so there's no Room-style migration step. To evolve the model:
1. Add the new field to the domain class (`data/Session.kt` etc.) and to the `toMap()`/`toXxx()`
   mappers in `data/repo/Repositories.kt`.
2. Make it **nullable or give it a default** and **default-handle the missing case** in the reader
   (old documents won't have the field — e.g. `getDouble("x")`, `getBoolean("x") ?: false`).
3. No backfill is required; documents gain the field next time they're written.

`LocalDate` is stored as an **epoch-day `Long`** (`date`); keep that representation (a future web
client depends on it). One-time import of legacy on-device data lives in
`data/migration/LegacyDbMigration.kt` (reads the old `skytte.db` via raw SQLite, once per uid).

## Backup format

`data/Backup.kt` holds versioned DTOs with a `from()` entity→DTO mapper (current `version = 3`;
ids are `String`). The export `Json` uses `encodeDefaults = true` so every field (incl. nulls) is
written. New entity fields **must** be added to the matching DTO + `from()` + the importer, or they
won't round-trip. Import (`BackupImporter.kt`) is a **merge that assigns fresh ids** and remaps
session→weapon/ammo references, so importing never clobbers existing docs; nothing is deleted.
