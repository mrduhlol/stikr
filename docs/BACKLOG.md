# Stikr backlog

Candidate work, gathered 2026-11-16. Nothing here is committed to being built —
each item needs a quick brainstorm before it becomes a design.

## Confirmed unshipped (from PRODUCT.md)

- **Tray-icon picker** — let the user choose which sticker becomes the 96×96
  WhatsApp tray icon. Today it is generated automatically in
  `StickerRenderer.renderTray` / `PackExporter.writeTray`.
- **Multiple subjects** — ML Kit subject segmentation currently lifts one
  subject. Offer a way to keep more than one in a cut-out.
- **Emoji suggestions per sticker** — the `sticker_emoji` field already exists
  in the ContentProvider contract; suggesting values for it is not built.

## Chores

- **Caption fonts: docs only** — all 12 fonts *are* implemented in
  `StickerTypography` (10 bundled TTFs + system Sans/Serif/Mono). `PRODUCT.md`
  still lists "caption fonts" as not shipped. Update the doc; no code needed.
- **Inspect untracked `tools/`** — the only dirty path in git. Find out what it
  is before it is committed or lost.
- **Test-suite pass** — run the unit/screenshot tests and fix anything broken
  or flaky.

## New feature ideas (suggestions, not committed)

- **Cut-out edge refinement** — add/erase brush over the segmentation mask.
  Auto cut-out is the core promise; letting users fix a bad edge is the single
  biggest quality win.
- **Crop / rotate before cut-out** — frame the subject before segmentation so
  the mask starts from a tighter photo.
- **Reusable style presets** — save a look (font, colour, outline, placement)
  and apply it to other stickers or a whole pack.
- **Reorder stickers in a pack** — drag to arrange; pack order is what users
  see in WhatsApp.
- **Duplicate sticker / duplicate pack** — small, frequently useful.
- **Transparent PNG export** — share the raw cut-out without the white sticker
  outline, alongside the existing share-as-sticker path.
- **Home-screen shortcut / widget** — one tap straight into creation, matching
  the "sticker in under a minute" principle.
- **Accessibility pass** — TalkBack labels and minimum touch targets across
  the creation flow.

## Second wave (suggestions, not committed)

- **Share a photo into Stikr** — `AndroidManifest.xml` only accepts ZIP pack
  imports via `ACTION_SEND`/`VIEW`. Add an `image/*` target so "Share → Stikr"
  from the gallery or camera drops the photo straight into the editor. The
  shortest path from "photos in" to "stickers out."
- **Undo / redo in the editor** — there is no undo for text and outline edits.
  A small history stack would remove the fear of experimenting.
- **Remember my look** — every new sticker starts from `StickerRenderer.Style()`
  defaults. Persist the last-used font, outline, colour and caption position
  so a run of stickers starts where the last one finished.
- **Recent captions** — a chip row of captions used recently, next to the
  caption field.
- **Auto-numbered captions in batches** — a template like `Hello {n}` becomes
  `Hello 1` … `Hello 30` across a batch run; pairs with the existing
  BatchPlanner.
- **Recently deleted / recover** — deleted stickers go to a short-lived tray
  instead of vanishing outright, in keeping with the fail-soft principle.
- **Pack archive** — hide a pack without deleting it or its stickers.

## Third wave (suggestions, not committed)

- **Reaction-word packs from one photo** — pick one photo, choose a set of
  caption presets (YES / NO / OK / LOL …), get a pack of captioned stickers in
  one shot. Very close to the "sticker in under a minute" promise.
- **In-app "How it works / Privacy" screen** — `SettingsScreen` currently has
  only Appearance. A short pane explaining on-device processing and why
  nothing uploads reinforces the core positioning.
- **Per-sticker emoji entry** — `StickerPack` has no emoji field at all, so the
  `sticker_emoji` value WhatsApp displays cannot currently be set. This is a
  prerequisite for the emoji-suggestions item above.
- **Compare original / hold-to-reveal** — temporarily show the uncut photo over
  the cut-out so edge quality is easy to judge. Pairs with edge refinement.
- **Pre-flight check before WhatsApp export** — validate count, file sizes,
  tray icon and WhatsApp availability, with an actionable message if something
  is wrong. Reinforces "a pack that says it will import, does import."
- **Backup indicator / gentle reminder** — show when the last local backup was
  made; backups stay manual and on-device.
- **Optional passphrase-protected backups** — backup ZIPs hold private photos;
  an optional passphrase would make them safe to move around.
- **Zoom + transparency checkerboard in the editor** — makes transparent edges
  and stray pixels visible while editing. Small.
