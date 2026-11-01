# stikr

**Photos in, stickers out.**

Turn a photo into a sticker on your phone. Cut out the subject, add an outline
and a caption, then import a pack into WhatsApp or share a sticker anywhere.
No account or watermark. Save and restore local backup files to keep your collections.

![Android 8.0+](docs/site/assets/badge-android.svg)
![On device](docs/site/assets/badge-local.svg)
![No account](docs/site/assets/badge-free.svg)

[![Download stikr APK](https://img.shields.io/badge/Download-stikr.apk-723AF4?style=for-the-badge&logo=android&logoColor=white)](https://github.com/mrduhlol/stikr/releases/download/app/stikr.apk)
· **[READ THE DOCS ↗](https://mrduhlol.github.io/stikr/)**

Free, for Android 8.0 and newer. **One app, one download: `stikr.apk`.**

This is the normal app for everyday use: create and edit sticker packs, remove
backgrounds, share stickers, and add packs to WhatsApp. The same download is
updated whenever changes on `main` pass the tests and build successfully. Install
it over your existing normal APK to keep your packs; no version selection needed.

Debug APKs are for local development and troubleshooting and are not distributed
here. They have the same sticker features, with debugging enabled and extra
tooling. If you previously installed a debug build, Android may require you to
uninstall it before installing the normal app because the signing keys differ.
Uninstalling deletes local packs. A completed local backup saved outside the app can be restored after reinstalling.

---

## **What you get**

| Your photo | Your sticker |
| :--- | :--- |
| **Cut out the subject** | Auto cut-out is off by default. Turn it on in the editor; ML Kit subject segmentation runs on device. Google Play services fetches the model once. If cut-out is unavailable, keep the full photo. |
| **Add the sticker outline** | White or ink, with adjustable thickness. |
| **Say something** | Optional caption in cream, ink, butter, or cherry, at the top or bottom. |
| **Take the tour** | Three short pages on the first launch: photos in, stickers out; pick, cut, dress up; packs, ready for WhatsApp. The tour introduces editing and WhatsApp import. |
| **Keep a pack** | Create, rename, and delete packs. Select up to 30 photos together, then tap a sticker and choose **Edit sticker** to change it in place. Add and delete stickers. A pack holds at most 30 stickers, WhatsApp's limit, so a full pack offers **Save into a new pack**. |
| **Make a batch** | Pick as many photos as you like. stikr makes a sticker from each one and splits the run into packs of at most 30, with a title and an author you set once. |
| **Send it to WhatsApp** | Import a pack of 3–30 stickers through the standard sticker-pack contract. Works with WhatsApp and WhatsApp Business. |
| **Share one anywhere** | Send a sticker as a PNG through Android's normal share sheet. |
| **Keep it local** | No required sign-in, analytics, or telemetry. Editing works offline; cut-out requires Google Play services. Backups are local ZIP files saved with Android’s file picker; Stikr does not upload them. |
| **Keep your collections** | The folder icon opens **Save backup file** and **Restore from file**. See the [local backup guide](docs/local-backup.md). No account, Cloud registration or subscription is needed. |

Static stickers only. A tray-icon picker, multiple subjects,
and emoji suggestions are not shipped. Photos come from the system photo picker;
stikr does not ask for storage permission.

## **Install**

1. Tap **[DOWNLOAD APK](https://github.com/mrduhlol/stikr/releases/download/app/stikr.apk)** on your phone.
2. Open the downloaded file.
3. Allow **Install unknown apps** when Android asks.
4. Tap **Install**.

Every release uses the same signing key, `keystore/release.p12`, so the next
release installs over the one you have. If you installed a debug build,
uninstall it first. Android rejects updates with a different signature.
Uninstalling removes local packs. Save a local backup to Downloads first if you want to restore the library after reinstalling.

[Installation and update details](https://mrduhlol.github.io/stikr/install.html)

## **Make a sticker**

1. Open **stikr** and tap **Edit a single photo**, or create a pack first.
2. Pick a photo. The full photo is kept by default; turn on **Auto cut-out** if you want to remove its background.
3. Adjust the **outline**, add a **caption** if you want, and tap
   **Add to “*pack name*”**.
4. With 3–30 stickers in a pack, tap **Add to WhatsApp** at the bottom of the
   pack screen to import it. To send one image, tap the sticker and choose
   **Share**.
5. For a pile of photos, tap **Select photos** on home, set a **title** and an
   **author**, and pick as many photos as you like. stikr makes a sticker from
   each one and splits the run into packs of at most 30. You can stop a run
   half-way — then nothing is kept. A full pack in the editor offers **Save
   into a new pack**.

Inside a pack, **Select photos** adds up to its remaining 30-sticker capacity in one selection. Tap any sticker and choose **Edit sticker**, then **Save changes** to replace it. New stickers keep private originals and edit settings for later changes; older stickers without originals reopen their saved image.

The cut-out model downloads on first use through Google Play services.
When cut-out is unavailable, stikr falls back to the full photo with the toggle exposed.
A batch that fills more than one pack names them after the title —
“Beach trip 1”, “Beach trip 2” — and the author is passed to WhatsApp as the
pack's publisher.

[Full how-to](https://mrduhlol.github.io/stikr/use.html) ·
[Privacy details](https://mrduhlol.github.io/stikr/privacy.html)

## **Your little collections**

The app uses near-black graphite backgrounds with restrained forest gradients,
dark olive controls, and a two-column collection of sharp pack cards. Each card uses
its thumbnail's colors, a flush striped LVL tab, its creation checkpoint and
persistent numeric ID. The approved black/lime S with the large folded corner is the launcher icon.

**Create a sticker pack** selects photos together. **Create a sticker** opens the
single-photo editor and saves to the shared **Stikr** pack. Singles appear in
their own row; long-press one to delete it or open the WhatsApp slider. Each
new shared pack starts with two starter stickers, removed automatically after
three of your own are saved. Full packs continue into Stikr 2, Stikr 3, and so on.
Repeated imports update the same pack identity.

Open a pack and tap its thumbnail or use **⋮ → Add thumbnail** to choose a photo
or text cover. Photo covers support drag and zoom in a 4:5 crop and save at
1080 × 1350 pixels. Text covers include fonts, size, bold/italic, background,
and text color. The same menu edits the title and author.

Sticker outlines default to **3 px**. **Bleeding bend** rounds photo corners
before drawing the outline and defaults off. Caption controls include font,
size up to 170, bold, background, and color. Cover text reaches 400, with Sans, Serif, Mono, Anton, Caveat, Pacifico and Oswald available offline. A full slide requests WhatsApp import; a
compact card rises and unfolds with a success check only after WhatsApp confirms success.
Reduced-motion settings show the final success state immediately. Short click, slide,
and success sounds follow the top-right mute toggle and the phone’s silent mode.

The approved [interactive design reference](docs/mockups/dark-onboarding-preview.html)
and [design tokens](DESIGN.md) document the intended layout and behavior.

## **Build it**

Kotlin + Jetpack Compose with Material 3. One module, minSdk 26.
Use JDK 17 and Android SDK platform 34 with build tools 34.0.0, as in CI.

```bash
./gradlew :app:assembleDebug
# With a phone connected and USB debugging enabled:
adb install app/build/outputs/apk/debug/app-debug.apk
```

Or open the project in Android Studio and press **Run**.

| Source under `app/src/main/java/com/stikr/app/` | Job |
| :--- | :--- |
| `render/StickerRenderer.kt` | Draws a 512×512 sticker: dilated silhouette for the white or ink outline, subject on top, caption last. |
| `render/StickerCodec.kt` | Encodes WebP, reducing quality until the file fits WhatsApp's limits of 100 KB per sticker and 50 KB per tray icon. |
| `editor/SubjectCutout.kt` | Wraps ML Kit segmentation in a suspend function. Returns null when cut-out is unavailable. |
| `data/PackRepository.kt` | Stores packs as files and `pack.json` in private app storage. No database. Caches parsed `pack.json` until the file changes. |
| `export/StickerPackContentProvider.kt` | Implements WhatsApp's sticker provider contract: `metadata`, `stickers/<pack>`, and `stickers_asset/<pack>/<file>`, readable only with `com.whatsapp.sticker.READ`. |
| `ui/MainViewModel.kt` | Editor and pack state. Slider drags are conflated into one preview render at a time, reusing the outlined subject layer when only the caption changes. |
| `ui/StickerImage.kt` | Decodes sticker thumbnails off the main thread at their displayed size, into a shared 8 MB memory cache. |

### Test and inspect renders

```bash
./gradlew testDebugUnitTest
./gradlew testDebugUnitTest --tests "com.stikr.app.ui.ScreenRenderTest"
```

Robolectric renders sample stickers to `app/build/sticker-renders/`.
The screen test renders whole screens in light and dark, including dialogs,
empty, loading, saving and exporting states, to `app/build/ui-renders/`.
`python3 docs/theme/make-screens.py` (needs Pillow) turns those renders into the
committed `docs/theme/screens-*.png`; copy them to `docs/site/assets/` too.

### Build speed and startup

`gradle.properties` turns on the configuration cache, the build cache and
parallel execution, so repeat builds skip configuration and reuse task
outputs. CI caches Gradle with `gradle/actions/setup-gradle`, runs the unit
tests on pushes to `main` and release tags, and uploads their reports. Release APKs bundle a baseline
profile for launch and the home screen (`app/src/main/baseline-prof.txt`),
which `androidx.profileinstaller` installs on sideloaded phones. Compose
preview tooling ships in debug builds only.

### App download

Push to `main`. The [`build-apk.yml`](.github/workflows/build-apk.yml) workflow
runs the tests, builds with the persistent `keystore/release.p12` signing key,
and replaces `stikr.apk` on the single **stikr** download page. No version tag
or separate release is needed. Android's internal `versionCode` must increase
for newer builds; keep the signing key unchanged so updates preserve local packs.

[Development guide](https://mrduhlol.github.io/stikr/develop.html)

## **Contributing**

Open an issue with a reproducible bug or a concrete proposal. For code changes,
keep the scope small, run the relevant tests, and include light and dark renders
for UI changes. Read [CONTRIBUTING.md](CONTRIBUTING.md) and [PRODUCT.md](PRODUCT.md)
before adding a feature.

The [docs site](docs/site/index.html) is static HTML and one CSS file, with local
images and Anton. Preview it with `python3 -m http.server 8000 --directory docs/site`.
The [Pages workflow](.github/workflows/pages.yml) deploys it on pushes to `main`. Select **GitHub Actions** as the repository's Pages source.
