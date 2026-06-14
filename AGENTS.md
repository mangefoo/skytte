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
- Persistence: **Room** (`skytte.db`). Navigation: Navigation Compose. Serialization:
  kotlinx.serialization (JSON backup export/import).
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
  `adb input text`. To get deterministic data, seed the DB directly:
  pull `databases/skytte.db` via `run-as`, edit with local `sqlite3`, `cp` it back via
  `run-as` (push through `/data/local/tmp` first), and clear `*-wal`/`*-shm`.

## Architecture conventions

- **Screens** live in `ui/<feature>/` as `XxxScreen.kt` + `XxxViewModel.kt`. ViewModels expose
  state as `Flow`/`StateFlow` and use a `companion object { val Factory = viewModelFactory { ... } }`
  with the `database()` helper in `ui/ViewModelFactory.kt`.
- **Shared top bar:** `ui/SkytteTopBar.kt` — tinted bar with title + a settings gear, used by
  every main tab. The root `Scaffold` in `ui/SkytteAppRoot.kt` sets `contentWindowInsets =
  WindowInsets(0)` + `consumeWindowInsets(padding)` so the per-screen top bars own the status-bar
  inset (don't reintroduce a double inset).
- **Dashboard** (`ui/dashboard/`) computes all stats in-memory in `DashboardViewModel.buildStats`
  from `SessionDao.observeAll()`; monthly charts reuse `MonthlyChartCard` (`MonthBucket`).
- **Cost** is centralized in `SessionWithRefs.totalCost()` (`data/Session.kt`) — reused by the
  session card and the dashboard. Reuse it; don't recompute the formula inline.

## Room migrations (do not skip)

Real user data exists on devices. When you change an `@Entity`:
1. Bump `version` in `data/AppDatabase.kt`.
2. Add a `Migration(old, new)` with the `ALTER TABLE …` and register it in `.addMigrations(...)`.
3. **Never** use destructive migration. Verify by launching over the previous DB without a crash.

History: v1→v2 added `ammunition.costPerRound`; v2→v3 added `sessions.fee` + `feeIncludesAmmo`.

## Backup format

`data/Backup.kt` holds versioned DTOs (`from()` entity→DTO, `toEntity()` DTO→entity). The export
`Json` uses `encodeDefaults = true` so every field (incl. nulls) is written. New entity fields
**must** be added to the matching DTO + both mappers, or they won't round-trip. Import
(`BackupImporter.kt`) is **merge** (upsert by id; nothing deleted).
