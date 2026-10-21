# Product

<!-- impeccable:product-schema 1 -->

## Platform

android

## Users

Android users who want stickers made from their own photos — a pet, a friend, a
screenshot — without a computer, an account, or a paid cut-out tool. Single
confirmed audience until testing says otherwise.

## Product Purpose

stikr turns a photo into a sticker on the phone itself: the subject is cut out
on device, given a white sticker outline and an optional caption, saved into
packs, and imported into WhatsApp as a real sticker pack or shared as an image.
Success means a finished sticker in under a minute, with nothing leaving the
phone.

## Positioning

No account, no sign-in, no watermark, no upload: the only dependency is the
phone. Neighbouring tools are web apps that upload photos or apps that stop at
saving a PNG; stikr finishes the job by delivering an importable WhatsApp pack.

## Operating Context

Native Android app, Kotlin + Jetpack Compose (Material 3), Android 8.0 and
newer. Photos come from the system photo picker, so no storage permission is
asked. Packs live as plain files in the app's private storage. Export to
WhatsApp (and WhatsApp Business) goes through the standard sticker-pack
ContentProvider contract, which stikr implements. Distribution: signed release
APKs sideloaded from GitHub Releases, starting with v0.1; releases are built and
signed in CI with the persistent repo keystore whenever a `v*` tag is pushed.

## Capabilities and Constraints

Confirmed: photo picker; on-device auto cut-out (off by default) using ML Kit subject
segmentation (model fetched by Google Play services on first use; falls back to
the full photo when unavailable, with the toggle exposed); outline in white or
ink with adjustable thickness; optional caption in four colours (cream, ink,
butter, cherry), top or bottom; packs with create, rename, delete; add and
delete stickers; import up to 30 photos at once into a pack; edit and replace
existing stickers with private originals and saved edit settings; export a pack to WhatsApp (3–30 stickers per pack) from a
pinned Add to WhatsApp bar at the bottom of the pack screen; share a
single sticker as a PNG through the share sheet; persistent System, Light, Dark,
and AMOLED appearance from the top-right Settings button; a restrained home
library with actual sticker previews and accurate readiness counts; a three-page first-run tour shown once on the first
launch and replayable from the home screen; batches that turn as many picked
photos as the user likes into automatic packs of at most 30, with a title and
an author set once and a run that can be stopped mid-way, keeping nothing;
per-pack author metadata, passed to WhatsApp as the pack publisher; an
enforced 30-sticker cap per pack, where a full pack disables saving and
offers Save into a new pack.

Constraints: static stickers only, no animation; cut-out requires Google Play
services, everything else works fully offline; no cloud, no accounts, no
analytics, no telemetry; nothing is ever uploaded.

Not shipped yet: tray-icon picker, multiple subjects, caption fonts, emoji
suggestions per sticker.

## Brand Commitments

Name stikr (lowercase). Voice line: "Photos in, stickers out." Visual anchor:
a sticker book — cream paper (#F1EDE7), ink outlines (#191715), hard offset
shadows, pastel fills (butter #F8DFA0, cyan #9EDDE6, green #9CE4A4, pink
#F3CDDA) and one loud red accent (#AE403E); Anton for display type. App mark:
a white sticker "S" with a peeled corner on purple (#723AF4) — the launcher
icon (`docs/brand/logo.png`); it lives outside the paper palette on purpose,
since it has to stand alone on a home screen. Source of truth:
github.com/mrduhlol/stikr.

## Evidence on Hand

- README.md (features, how to build and run)
- app/src/main (the whole app: renderer, cut-out, repository, export)
- app/src/test renders visual samples to app/build/sticker-renders/ for review
  without a device
- GitHub Releases — v0.1 is the first signed release (stikr.apk)

Absence future work must not fabricate: no installs, no reviews, no user counts,
no real app-store listing, no photos of real people.

## Product Principles

1. One photo to one finished sticker in under a minute.
2. Nothing leaves the phone — no uploads, no accounts, no tracking.
3. Fail soft: whatever is unavailable (cut-out, WhatsApp) degrades to something
   that still works (the full photo, a share).
4. A pack that says it will import into WhatsApp does import into WhatsApp.
