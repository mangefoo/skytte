#!/usr/bin/env python3
"""Upload a file to Google Drive using a service-account key.

Reads configuration from environment variables:
  GDRIVE_SA_KEY_FILE  path to the service-account JSON key
  GDRIVE_FOLDER_ID    target Drive folder id (shared with the service account)
  APK_PATH            local path of the file to upload
  DRIVE_FILENAME      name to give the file in Drive (defaults to APK_PATH's basename)

supportsAllDrives=True so this works with Shared Drives (recommended for personal
accounts, where a service account otherwise has no storage quota).
"""
import os
import sys

from google.oauth2 import service_account
from googleapiclient.discovery import build
from googleapiclient.http import MediaFileUpload

SCOPES = ["https://www.googleapis.com/auth/drive.file"]


def main() -> int:
    key_file = os.environ["GDRIVE_SA_KEY_FILE"]
    folder_id = os.environ["GDRIVE_FOLDER_ID"]
    apk_path = os.environ["APK_PATH"]
    name = os.environ.get("DRIVE_FILENAME") or os.path.basename(apk_path)

    if not os.path.isfile(apk_path):
        print(f"::error::APK not found at {apk_path}", file=sys.stderr)
        return 1

    creds = service_account.Credentials.from_service_account_file(key_file, scopes=SCOPES)
    service = build("drive", "v3", credentials=creds, cache_discovery=False)

    metadata = {"name": name, "parents": [folder_id]}
    media = MediaFileUpload(apk_path, mimetype="application/vnd.android.package-archive", resumable=True)

    created = (
        service.files()
        .create(body=metadata, media_body=media, fields="id,name,webViewLink", supportsAllDrives=True)
        .execute()
    )
    print(f"Uploaded '{created['name']}' (id={created['id']})")
    if created.get("webViewLink"):
        print(f"Link: {created['webViewLink']}")
    return 0


if __name__ == "__main__":
    sys.exit(main())
