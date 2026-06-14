# Skytte

An Android app for logging shooting sessions and keeping an inventory of weapons
and ammunition. The UI is in Swedish.

## Features

- **Skjuttillfällen (Sessions)** — log a session with date, location, weapon,
  ammunition, round count, and shooting type.
- **Vapen (Weapons)** — maintain a list of weapons (name, caliber, notes).
- **Ammunition** — maintain a list of ammunition (name, caliber, notes).
- **Cloud sync** — data is stored in **Cloud Firestore** under your Google account, so it
  syncs across your devices and is backed up off-device. The app is **offline-first**: it
  works fully without a connection (e.g. at the range) and syncs when back online. Sign in
  with Google on first launch; sign out from **Inställningar** (Settings).
- **Export & import** — back up all data (sessions, weapons, ammunition) to a JSON
  file via the system share sheet, and import a backup JSON (e.g. from Downloads)
  to merge it back in. Both live on the **Inställningar** (Settings) screen, which
  is organized into **Utseende** (appearance), **Data**, and **Om Skytte** (about)
  sections.

## Tech stack

- **Language:** Kotlin 2.1.0
- **UI:** Jetpack Compose (Material 3)
- **Navigation:** Navigation Compose
- **Persistence:** Cloud Firestore (offline-first; the local cache is the on-device store)
- **Auth:** Firebase Auth with Google Sign-In (via Credential Manager)
- **Architecture:** MVVM (ViewModel + Kotlin Flow over Firestore snapshot listeners)
- **Serialization:** kotlinx.serialization (JSON export/import)
- **Build:** Gradle 8.13 with Android Gradle Plugin 8.13.2

> **Firebase setup:** the app needs `app/google-services.json` (not committed) and, in the
> Firebase console, **Firestore** enabled and **Google** enabled as an Auth provider. See
> [`.github/CI.md`](.github/CI.md) for the build-time `GOOGLE_SERVICES_JSON` secret.

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
├── MainActivity.kt          # Entry point + auth gate
├── SkytteApp.kt             # Application class (Firestore, AuthManager, per-user Repositories)
├── auth/
│   └── AuthManager.kt       # Firebase Auth + Google Sign-In (Credential Manager)
├── data/                    # Domain models, Firestore repositories, JSON backup/export/import
│   ├── Session.kt / Weapon.kt / Ammunition.kt   # plain data classes (String ids)
│   ├── repo/Repositories.kt # Firestore-backed repositories (snapshot listeners → Flow)
│   ├── migration/LegacyDbMigration.kt           # one-time import of old local skytte.db
│   ├── Backup.kt            # Serializable backup DTOs
│   ├── BackupExporter.kt    # Builds JSON, writes file, shares via FileProvider
│   └── BackupImporter.kt    # Reads a backup JSON and merges it into Firestore
└── ui/                      # Jetpack Compose screens + ViewModels
    ├── SkytteAppRoot.kt     # Bottom-nav + navigation graph
    ├── auth/SignInScreen.kt # Sign-in gate UI
    ├── dashboard/ sessions/ weapons/ ammunition/ settings/
    └── theme/
```

Firestore rules live in [`firestore.rules`](firestore.rules) (deployed via the Firebase CLI;
see [`firebase.json`](firebase.json)).

## Versioning

Dependency and plugin versions are centralized in
[`gradle/libs.versions.toml`](gradle/libs.versions.toml).

> **Gradle 9 note:** This project is pinned to Gradle 8.13 because AGP 8.13.2
> does not support Gradle 9. Moving to Gradle 9 requires upgrading to AGP 9.x,
> which is a major release with breaking changes.
