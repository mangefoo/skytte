# CI: Release APK → Firebase App Distribution

The [`Release APK`](workflows/release-apk.yml) workflow builds a **signed release APK** and
distributes it via **Firebase App Distribution**. It runs on every push to `main` and can also
be started manually from **Actions → Release APK → Run workflow**.

What it does, in order:

1. Builds with JDK 17 + Android SDK 36 and runs `./gradlew assembleRelease`.
2. Signs the APK with your release keystore (provided via secrets).
3. Attaches the APK to the run as a workflow artifact (`release-apk`).
4. Uploads the APK to Firebase App Distribution (via the Firebase CLI) and distributes it to your
   tester group. Testers get an email and install through the Firebase *App Tester* app.

The workflow does nothing useful until the secrets/variables below exist.

---

## Required secrets and variables

These live in an **environment** named `Release`, which the workflow references via
`environment: Release` on its job. Create it under **Settings → Environments → New
environment** (name it exactly `Release`). Add the sensitive values as **environment secrets**
and the non-sensitive ones as **environment variables** (a Variable is read with `vars.*`, a
Secret with `secrets.*` — they are *not* interchangeable). Optionally restrict the
environment's deployment branches to `main` and/or require a reviewer.

> The job only sees these because it declares `environment: Release`. Without that line (or if
> the environment name doesn't match) the references resolve to empty strings at runtime —
> which surfaces as a corrupt keystore (`Tag number over 30 is not supported`) or an empty
> alias (`No key with alias '' found`). Also make sure each value is in the right section:
> something added as a **Variable** is invisible to `secrets.*`, and vice versa.

Secrets (sensitive — masked in logs):

| Secret | What it is |
| --- | --- |
| `RELEASE_KEYSTORE_BASE64` | Your release keystore, base64-encoded (see below) |
| `RELEASE_KEYSTORE_PASSWORD` | The keystore (store) password |
| `RELEASE_KEY_PASSWORD` | The key password (often the same as the store password) |
| `FIREBASE_SERVICE_ACCOUNT` | The Firebase service-account JSON key, pasted as-is |

Variables (not sensitive — stored/displayed in plaintext):

| Variable | What it is |
| --- | --- |
| `RELEASE_KEY_ALIAS` | The key alias inside the keystore (e.g. `skytte`) |
| `FIREBASE_APP_ID` | The Firebase Android **App ID** (`1:NNN:android:XXX`) |
| `FIREBASE_GROUPS` | The tester group alias to distribute to (e.g. `testers`) |

---

## 1. Create the signing keystore

Generate a keystore (keep this file safe and **never commit it** — `.gitignore` already
excludes `*.keystore`/`*.jks`). Use the same value for the store and key passwords if you
want to keep it simple.

```bash
keytool -genkeypair -v \
  -keystore release.keystore \
  -alias skytte \
  -keyalg RSA -keysize 2048 -validity 10000
```

Then base64-encode it **to a single line** and put the result in `RELEASE_KEYSTORE_BASE64`.
Encode into a file and copy from the file — do **not** select wrapped base64 out of the
terminal, as dropped/added characters corrupt the keystore (`Tag number over 30 is not
supported` at build time):

```bash
# Encode to one line (works on macOS and Linux):
openssl base64 -A -in release.keystore -out keystore.b64

# Verify it round-trips to a readable keystore BEFORE pasting:
base64 -d keystore.b64 > /tmp/check.keystore
keytool -list -keystore /tmp/check.keystore      # should list your 'skytte' alias

# Copy the file contents into the RELEASE_KEYSTORE_BASE64 secret:
#   macOS:  pbcopy < keystore.b64
#   Linux:  xclip -selection clipboard < keystore.b64   (or open keystore.b64 and copy all)
```

Set the other three secrets to match what you entered above:
`RELEASE_KEYSTORE_PASSWORD`, `RELEASE_KEY_ALIAS` (e.g. `skytte`), `RELEASE_KEY_PASSWORD`.

> Keep `release.keystore` and its passwords backed up somewhere safe. If you lose them you
> cannot ship an update that the same users can install over the existing app.

---

## 2. Set up the Firebase app

1. At [console.firebase.google.com](https://console.firebase.google.com/) create (or choose) a
   Firebase project.
2. Add an **Android app** with package name `se.mindphaser.skytte`. You do **not** need to add
   `google-services.json` or any SDK — distribution only needs the App ID.
3. Copy the **App ID** from **Project settings → Your apps** (format `1:NNNNNN:android:XXXX`) and
   put it in the `FIREBASE_APP_ID` variable.

## 3. Enable App Distribution and a tester group

1. **Release & Monitor → App Distribution → Get started.**
2. Create a **tester group** (e.g. `testers`) and add your own email as a tester.
3. Put the group's **alias** in the `FIREBASE_GROUPS` variable (e.g. `testers`).

## 4. Create the Firebase service account

The service account lives in the Google Cloud Console
([console.cloud.google.com](https://console.cloud.google.com/), same project as Firebase) →
**IAM & Admin → Service Accounts**.

1. Create a service account (or pick an existing one).
2. Grant it the **Firebase App Distribution Admin** role.
3. Open it in console.cloud.google.com Service Accounts → **Keys → Add key → Create new key 
   → JSON**, then copy the downloaded file's entire
   contents (the whole `{ … }` object — e.g. `pbcopy < key.json`) into the
   `FIREBASE_SERVICE_ACCOUNT` secret. The workflow points the Firebase CLI at it via
   `GOOGLE_APPLICATION_CREDENTIALS`. Treat the file as a credential and delete it afterwards.

---

## Running it

- **Automatic:** push to `main`.
- **Manual:** Actions → **Release APK** → **Run workflow**.

After a green run the build appears in the Firebase console under **App Distribution**, your
testers get an email/notification (install via the Firebase *App Tester* app), and the APK is
also attached to the workflow run as the `release-apk` artifact.

## Local release build (optional)

You can reproduce the signed build locally by pointing the same env vars at a keystore file:

```bash
RELEASE_KEYSTORE_PATH=/path/to/release.keystore \
RELEASE_KEYSTORE_PASSWORD=… \
RELEASE_KEY_ALIAS=skytte \
RELEASE_KEY_PASSWORD=… \
./gradlew assembleRelease
# output: app/build/outputs/apk/release/app-release.apk
```

Without these env vars the release build is left unsigned; debug builds are unaffected.
