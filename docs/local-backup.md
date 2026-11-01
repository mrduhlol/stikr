# Local backup and restore

Stikr uses local backup files only. There is no account connection, Google Cloud setup, subscription or automatic cloud upload.

## Save a backup

1. Open Stikr and tap the folder icon at the top right.
2. Choose **Save backup file**.
3. In Android's file picker, choose **Downloads** or another folder on your device, then save the ZIP.
4. Wait for **Backup saved** before leaving or uninstalling.

The archive includes pack identities, names/authors, sticker images, private original photos, edit settings, thumbnails and tray images. It does not include account credentials. Backups are manual snapshots: save another file after making changes. Files remain under your control; Stikr does not upload them.

## Restore after reinstalling

1. Reinstall Stikr.
2. Tap the folder icon → **Restore from file**.
3. Select your saved ZIP and confirm **Restore**.

Missing packs are added. Matching pack IDs use the higher content version; local data wins ties. Existing collections are not cleared first. The entire archive is validated before merging, and corrupt or unsafe archives are rejected.

Keep the ZIP outside Stikr's private app folders. Uninstalling removes the working library and app-private files; a backup in Downloads can be selected after reinstalling. Android's automatic app backup is disabled. Stikr asks the system picker for local files only.

## Limits

The current format allows ZIPs up to 512 MiB, up to 1 GiB expanded, with bounded entry counts and file sizes. There is no background backup schedule. Save a new backup explicitly when you want to preserve your latest work.
