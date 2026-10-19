# Contributing to stikr

Read [PRODUCT.md](PRODUCT.md) before proposing a change. stikr makes static
stickers on Android, processes photos on device, and imports packs into WhatsApp.
Keep the name lowercase and use the confirmed capabilities in product copy.
Never invent installs, reviews, user counts, an app-store listing, or shipped features.

## Report a bug

[Open an issue](https://github.com/mrduhlol/stikr/issues) with your Android version,
stikr version, steps to reproduce, expected result, and actual result. For cut-out
or export failures, include whether Google Play services and WhatsApp or WhatsApp
Business are available. Use a non-sensitive sample image if an image is needed.

## Set up the app

Use JDK 17 and Android SDK platform 34 with build tools 34.0.0, matching CI.
Clone the repository, then run these commands at its root.

```bash
git clone https://github.com/mrduhlol/stikr.git
cd stikr
./gradlew :app:assembleDebug
adb install app/build/outputs/apk/debug/app-debug.apk
```

The install command needs a connected Android 8.0+ phone with USB debugging enabled.
You can also open the project in Android Studio and press **Run**.
Debug and release signatures differ. Uninstalling a build to switch signatures
removes its local packs.

## Submit a change

1. For a new feature or a change in behaviour, open an issue describing the problem and proposed scope.
2. Make a focused change on a branch. Keep unrelated formatting out of the diff.
3. Run the relevant checks below. Add a regression test when fixing behaviour that can be tested.
4. Open a pull request describing the problem, the resulting behaviour, and your validation. State anything you could not verify.

For UI changes, follow the [design system](https://mrduhlol.github.io/stikr/design.html).
Keep pastel text dark, borders visible, and shadows hard. Include light and dark
renders in the pull request. Preserve the fallback to a full photo when cut-out
is unavailable and image sharing when WhatsApp is unavailable.

## Verify the app

Run the unit tests with Robolectric.

```bash
./gradlew testDebugUnitTest
```

Sample sticker renders appear in `app/build/sticker-renders/`. To render whole
screens, including dialogs, run the screen test.

```bash
./gradlew testDebugUnitTest --tests "com.stikr.app.ui.ScreenRenderTest"
```

Review `app/build/ui-renders/` in light and dark. Update the relevant committed
images in `docs/theme/` when the UI changes. Keep the site's copies at
`docs/site/assets/screens-light.png` and `docs/site/assets/screens-dark.png`
in sync. For changes to cut-out or WhatsApp export, describe checks on a real
phone as well as automated tests.

## Edit the docs

Edit the plain HTML pages and shared `style.css` in `docs/site/`.
Keep the site self-contained, with local images and fonts. No build step,
JavaScript framework, third-party CDN, or external font service is needed.
Anton is bundled with its [OFL license](licenses/anton-OFL.txt), also copied into
the deployed site at `docs/site/licenses/anton-OFL.txt`.

```bash
python3 -m http.server 8000 --directory docs/site
```

Open `http://localhost:8000`. Check every page at desktop and phone widths,
light and dark modes, keyboard focus, and relative links. The Pages workflow
publishes `docs/site/` on pushes to `main`. The repository's
Pages source must be set to **GitHub Actions**.

## Release flow

Maintainers bump `versionName` and push a `v*` tag. The existing
[APK workflow](.github/workflows/build-apk.yml) signs with the persistent
`keystore/release.p12` and attaches `stikr.v<version>.apk` and `stikr.apk` to the
GitHub Release. Preserve that signing identity so releases can update installed
copies. CI debug artifacts use throwaway keys and are for smoke testing.
