# Skytte

An Android app for logging shooting sessions and keeping an inventory of weapons
and ammunition. The UI is in Swedish.

## Features

- **Skjuttillfällen (Sessions)** — log a session with date, location, weapon,
  ammunition, round count, and shooting type.
- **Vapen (Weapons)** — maintain a list of weapons (name, caliber, notes).
- **Ammunition** — maintain a list of ammunition (name, caliber, notes).
- **Export & import** — back up all data (sessions, weapons, ammunition) to a JSON
  file via the system share sheet, and import a backup JSON (e.g. from Downloads)
  to merge it back in. Both live on the **Inställningar** (Settings) screen.

## Tech stack

- **Language:** Kotlin 2.1.0
- **UI:** Jetpack Compose (Material 3)
- **Navigation:** Navigation Compose
- **Persistence:** Room (SQLite), database file `skytte.db`
- **Architecture:** MVVM (ViewModel + Kotlin Flow)
- **Serialization:** kotlinx.serialization (JSON export/import)
- **Build:** Gradle 8.13 with Android Gradle Plugin 8.13.2

## Requirements

- **JDK 17** (the project targets Java 17 / `jvmTarget = "17"`)
- **Android SDK** with API level 36 (`compileSdk` / `minSdk` / `targetSdk = 36`)
- Android Studio is recommended but not required; the Gradle wrapper handles the
  Gradle install.

Make sure `ANDROID_HOME` (or `ANDROID_SDK_ROOT`) points at your SDK, or provide a
`local.sdk.dir` via a `local.properties` file in the project root:

```properties
sdk.dir=/Users/<you>/Library/Android/sdk
```

## Building

Use the Gradle wrapper — it downloads the correct Gradle version automatically.

```bash
# Build the debug APK
./gradlew :app:assembleDebug

# Build a release APK
./gradlew :app:assembleRelease

# Install the debug build on a connected device/emulator
./gradlew :app:installDebug

# Clean
./gradlew clean
```

On Windows use `gradlew.bat` instead of `./gradlew`.

The debug APK is written to `app/build/outputs/apk/debug/`.

## Running

Open the project in Android Studio and run the `app` configuration on an
emulator or a device running Android API 36+, or build and install from the
command line with `./gradlew :app:installDebug`.

## Project structure

```
app/src/main/java/se/mindphaser/skytte/
├── MainActivity.kt          # Entry point
├── SkytteApp.kt             # Application class (holds the database)
├── data/                    # Room entities, DAOs, database, JSON backup/export/import
│   ├── AppDatabase.kt
│   ├── Session.kt / Weapon.kt / Ammunition.kt
│   ├── SessionDao.kt / WeaponDao.kt / AmmunitionDao.kt
│   ├── Backup.kt            # Serializable backup DTOs (+ entity mappers)
│   ├── BackupExporter.kt    # Builds JSON, writes file, shares via FileProvider
│   └── BackupImporter.kt    # Reads a backup JSON and merges it into the database
└── ui/                      # Jetpack Compose screens + ViewModels
    ├── SkytteAppRoot.kt     # Bottom-nav + navigation graph
    ├── dashboard/ sessions/ weapons/ ammunition/ settings/
    └── theme/
```

## Versioning

Dependency and plugin versions are centralized in
[`gradle/libs.versions.toml`](gradle/libs.versions.toml).

> **Gradle 9 note:** This project is pinned to Gradle 8.13 because AGP 8.13.2
> does not support Gradle 9. Moving to Gradle 9 requires upgrading to AGP 9.x,
> which is a major release with breaking changes.
