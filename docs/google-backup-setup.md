# Optional Google Drive backup setup

The app remains usable offline. No Google Cloud project or Android OAuth client has been configured for this implementation, and live authorization/upload/restore has **not** been tested. The account panel, authorization flow, archive merge and Drive storage are wired in the app. No embedded client secret, refresh token, web client ID or `google-services.json` is required by this native AuthorizationClient flow.

## Configure Google Cloud

1. Create or select your own Google Cloud project and enable **Google Drive API** in APIs & Services → Library.
2. Configure Google Auth Platform branding, audience and data access (OAuth consent screen). Use the appropriate audience; for an external app in Testing, add each tester's Google account under Test users. Supply the required support/contact and privacy information. Follow the console's publication/verification requirements before distributing broadly.
3. Create an OAuth client with application type **Android**, package **`com.stikr.app`**, and the SHA-1 of the certificate signing the installed APK. For the repository's current release signing certificate:

   ```text
   7E:04:43:D1:0D:24:91:1A:A2:02:EE:91:74:FC:F5:DA:7A:27:78:47
   ```

   This was derived with `keytool` from `keystore/release.p12`, alias `stikr`. Recheck after any key change. To inspect it without putting the keystore password in shell history, run the following and enter the existing signing password from the local build configuration when prompted:

   ```sh
   keytool -list -v -keystore keystore/release.p12 -storetype PKCS12 -alias stikr
   ```

4. Configure requested scopes: `https://www.googleapis.com/auth/drive.appdata`, `openid`, and `email`. The latter two obtain a real verified email label from Google's userinfo endpoint; no profile/photo scope or general Drive access is requested. Drive app data is hidden application storage, distinct from Android's system backup folder.
5. Install an APK signed with that certificate on a device with current Google Play services and a signed-in Google account. Sideloading does not bypass OAuth client registration, consent, audience/test-user restrictions or organization policies. Debug APKs use a different certificate and need their own Android OAuth registration. If distributed with Play App Signing, register the **app signing** certificate as well, rather than only the upload key.

## How backup works

Own `GoogleBackupController(context, repository)` alongside the ViewModel; call methods on the main thread and call `close()` when its owner is cleared. Collect `state`. Existing required fields are preserved; the additive `restoreAvailable: Boolean?` means unknown/check failed (`null`), remote archive exists (`true`), or automatic backup may be enabled (`false`). `accountLabel` is the verified email from OAuth userinfo, never a fabricated label. Only that label and last successful backup timestamp persist; automatic backup deliberately defaults off on each controller creation.

Use Activity Result `StartIntentSenderForResult` for `onResolution`:

```kotlin
val launcher = registerForActivityResult(
    ActivityResultContracts.StartIntentSenderForResult()
) { result ->
    controller.finishAuthorization(
        if (result.resultCode == Activity.RESULT_OK) result.data else null
    )
}
val resolve: (PendingIntent) -> Unit = { pending ->
    launcher.launch(IntentSenderRequest.Builder(pending.intentSender).build())
}
// Button actions:
controller.connect(resolve)
controller.backupNow(resolve)
controller.restore(resolve) { /* refresh repository-backed UI */ }
```

If launching the intent sender fails, call `finishAuthorization(null)` to clear pending/busy state. Never launch consent on startup or from a library observer. Connect checks remote presence by downloading to a temporary cache file, then deletes it; it does **not** import or upload. Offer Restore first when `restoreAvailable == true`; explain that explicit Back up now replaces the remote snapshot with the current local library. Automatic backup cannot be enabled while remote presence is unknown or an existing backup is awaiting the user's decision. Restore merges using `importBackup` and never clears the local library first.

Send a stable library content fingerprint to `libraryChanged`; its first emission establishes a baseline. Later changes are debounced for 1.5 seconds, with uploads only when connected and opted in. These are in-process backups, not a WorkManager guarantee when the app is closed. The background path never launches consent: it disables automatic backup and reports “Reconnect needed” if resolution is required. Every action obtains a fresh Google-cached token via `authorize`; tokens are never persisted or logged. HTTP 401 clears the token cache and asks for an **explicit retry**, avoiding automatic upload replay or background consent. The store should expose HTTP failures with `HTTP 401` or `status: 401` markers (including wrapped causes); there is no typed HTTP error in the agreed store API.

Disconnect cancels pending authorization/debounce and in-flight operation, waits for cancellation/IO completion, then invokes the official `revokeAccess(RevokeAccessRequest)` with the verified account and scopes. It clears local preferences and tokens even if revocation fails, and reports local-only disconnect honestly. Revocation removes all scopes granted to this app for that account, per Google's API contract. It does not delete the remote archive or local packs.

## Validation and limits

Automated tests cover first-emission suppression, repeat fingerprints, safe state defaults and HTTP 401 recognition. Live testing still needs your Cloud project. On a configured device, check: canceled consent, missing/outdated Play services, wrong signing SHA-1/developer error, offline operation, backup/restore round trip with stickers, thumbnails and editor data, reinstall → Connect → Restore, automatic opt-in, revoked/expired credentials, and disconnect during upload. Confirm old remote data survives Connect and automatic backup cannot overwrite it before a user decision.

## Official references

- [Android authorization guide and dependency (22.0.0)](https://developer.android.com/identity/authorization)
- [AuthorizationResult scope URI strings](https://developers.google.com/android/reference/com/google/android/gms/auth/api/identity/AuthorizationResult)
- [AuthorizationClient: authorize, clearToken and revokeAccess](https://developers.google.com/android/reference/com/google/android/gms/auth/api/identity/AuthorizationClient)
- [RevokeAccessRequest builder](https://developers.google.com/android/reference/com/google/android/gms/auth/api/identity/RevokeAccessRequest.Builder)
- [Drive application data storage and scope](https://developers.google.com/workspace/drive/api/guides/appdata)
- [Android OAuth client authentication and signing certificates](https://developers.google.com/android/guides/client-auth)
- [Configure the OAuth consent screen](https://developers.google.com/workspace/guides/configure-oauth-consent)
