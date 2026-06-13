# CI: Release APK → Google Drive

The [`Release APK`](workflows/release-apk.yml) workflow builds a **signed release APK** and
uploads it to Google Drive. It runs on every push to `main` and can also be started manually
from **Actions → Release APK → Run workflow**.

What it does, in order:

1. Builds with JDK 17 + Android SDK 36 and runs `./gradlew assembleRelease`.
2. Signs the APK with your release keystore (provided via secrets).
3. Attaches the APK to the run as a workflow artifact (`release-apk`).
4. Uploads the APK to a Google Drive folder via a service account, named
   `skytte-1.0-<run-number>-<short-sha>.apk`.

The workflow does nothing useful until the secrets below exist.

---

## Required repository secrets

Add these under **Settings → Secrets and variables → Actions → New repository secret**.

| Secret | What it is |
| --- | --- |
| `RELEASE_KEYSTORE_BASE64` | Your release keystore, base64-encoded (see below) |
| `RELEASE_KEYSTORE_PASSWORD` | The keystore (store) password |
| `RELEASE_KEY_ALIAS` | The key alias inside the keystore (e.g. `skytte`) |
| `RELEASE_KEY_PASSWORD` | The key password (often the same as the store password) |
| `GDRIVE_SA_KEY` | The Google service-account JSON key, pasted as-is |
| `GDRIVE_FOLDER_ID` | The ID of the Drive folder to upload into |

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

## 2. Create the Google service account

1. Go to the [Google Cloud Console](https://console.cloud.google.com/) and create (or pick) a project.
2. **APIs & Services → Library →** enable the **Google Drive API**.
3. **APIs & Services → Credentials → Create credentials → Service account.** Give it a name
   and create it (no roles needed).
4. Open the new service account → **Keys → Add key → Create new key → JSON.** A `.json` file
   downloads.
5. Paste the **entire contents** of that JSON file into the `GDRIVE_SA_KEY` secret.

Note the service account's email address (looks like
`name@project-id.iam.gserviceaccount.com`) — you need it in the next step.

---

## 3. Choose the Drive folder and share it

1. In Google Drive, create (or pick) the destination folder.
2. **Share** that folder with the service account's email, giving it **Editor** access.
3. Open the folder and copy its ID from the URL — it's the part after `/folders/`:
   `https://drive.google.com/drive/folders/`**`1AbCdEfGhIjKlMnOpQrStUvWxYz`**
4. Put that ID into the `GDRIVE_FOLDER_ID` secret.

### ⚠️ Personal Gmail vs. Workspace

A service account has **no storage quota on a personal (consumer) Google account**. If the
folder lives in your personal *My Drive*, the upload can fail with
`Service Accounts do not have storage quota`, because the uploaded file would be *owned* by
the service account.

This setup works reliably when the folder is in a **Shared Drive** (Google Workspace): create
a Shared Drive, add the service account as a member, and use a folder inside it. The workflow
already passes `supportsAllDrives=true`.

If you only have a personal account and hit the quota error, switch the upload to **rclone**
with a user OAuth token (uploads as you, owned by you) — ask and the workflow can be adjusted.

---

## Running it

- **Automatic:** push to `main`.
- **Manual:** Actions → **Release APK** → **Run workflow**.

After a green run you'll find the APK both as the `release-apk` artifact on the run page and in
your chosen Drive folder.

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
