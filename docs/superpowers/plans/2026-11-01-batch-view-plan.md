# Batch pack-view plan

Turn the batch **done** screen into the sketch the partner drew: one page per
pack — a big **thumbnail** (the pack's first sticker), a `— NAME —` band, a
3-column grid of that pack's stickers, an **Edit** pencil top-right — with
**swiping** to move between the packs a batch created, and an action sheet when
a sticker is tapped. Add an explicit **Remove background** toggle to the batch
dialog, default OFF.

## Why

The done screen is text-only rows today; the user wanted to see the actual
stickers, flip through the packs, and jump into editing straight from the batch
view. Background removal in a batch is currently implicit (always off); the
partner wants it visible and switchable, off by default.

## Goals / non-goals

- Goals: rich done view (thumbnail, name, grid, edit), swipe between packs,
  sticker tap → Edit / Share / Delete options, batch dialog toggle for
  background removal (default OFF), processing counters keep working.
- Non-goals: no changes to the WhatsApp export contract, no new dependencies,
  no per-sticker editing mid-batch (stickers land with the default style),
  no changes to the editor or pack screens beyond call-site wiring.

## Global constraints

- UI is built from the in-repo Neo kit (`ui/neo/Neo.kt`, `ui/neo/NeoFrame.kt`,
  `ui/neo/Neo.kt` composables) and the theme tokens; match the pack screen's
  visual language (NeoCard tiles, paper/ink palette).
- Throughput: `androidx.compose.foundation`'s `HorizontalPager` (already in the
  compose BOM — no new dependency).
- Unit tests are Robolectric (`testDebugUnitTest` is the CI gate; release-variant
  UI tests are known-flaky and out of scope).
- Code style: comments explain why, composables private to their file, no
  magic strings duplicated across files (reuse `PackRepository` constants).
- Batch data flow: `BatchState.created: List<StickerPack>` already carries each
  pack's `id`, `name`, `stickers` (file names). Sticker files live under
  `context.filesDir/packs/<packId>/<fileName>` (the same layout
  `PackScreen`'s `StickerImage` tiles read).
- The single-sticker editor's cut-out toggle already defaults to OFF; nothing
  changes there.

## Interfaces agreed up front

- `MainViewModel.startBatch(title: String, author: String, uris: List<Uri>, removeBackground: Boolean = false)`
  — Task 1 owns this; the old 3-argument call site keeps compiling via the
  default parameter until Task 2 wires the dialog.
- `BatchState.removeBackground: Boolean = false` — Task 1 adds the field;
  Task 2 may read it for the processing status line.
- `CreateBatchDialog(onDismiss: () -> Unit, onConfirm: (title: String, author: String, removeBackground: Boolean) -> Unit)`

## Task 1: Batch remove-background plumbing + tests

In `app/src/main/java/com/stikr/app/ui/MainViewModel.kt`:

- Add the `removeBackground: Boolean = false` parameter to `startBatch`.
- Thread it into `runBatch(...)` and pass it to `process(source, cutout = removeBackground)`
  (currently hard-coded `cutout = false` at the runBatch site).
- Add `removeBackground: Boolean = false` to `BatchState`; set it in `startBatch`
  when publishing PROCESSING, so the screen can show a cutting status.
- Nothing else moves: pack naming/counts, counters, cancellation semantics,
  and source-photo saving stay exactly as they are.

Tests (TDD): extend the existing MainViewModel batch tests — a run started with
`removeBackground = true` cuts out photos (the injectable cutter receives them;
a spied/stub cutter that returns a transformed bitmap proves `cutout` flows
through — the fallback counter stays 0 when the cutter succeeds and increments
when it returns null), and a run with the default keeps calling the cutter zero
times. Follow how the existing batch tests inject a fake cutter.

## Task 2: The batch done view (sketch) + swipe + sticker sheet + dialog toggle

In `app/src/main/java/com/stikr/app/ui/BatchScreen.kt` and the call sites in
`app/src/main/java/com/stikr/app/ui/StikrApp.kt`:

- DONE phase becomes a `HorizontalPager` over `state.created` (one page per
  pack). If exactly one pack exists, the pager still holds one page; no
  swipe affordance needed.
- Page layout (top to bottom), matching the sketch and the Neo kit:
  1. Big thumbnail: the pack's first sticker rendered through `StickerImage`
     (file `filesDir/packs/<packId>/<stickers.first()>`; placeholder/empty art
     if the pack somehow has none).
  2. The pack name as a band under the thumbnail (sketch's `— [Name] —`).
  3. A 3-column grid of that pack's stickers — reuse the tile look of
     `PackScreen`'s `StickerTile` (NeoCard + `StickerImage`), build a local
     private composable; a column-based manual grid is fine, `LazyVerticalGrid`
     inside a pager needs explicit height handling — prefer whatever is
     simplest and scrolls.
- **Edit pencil** at the top-right of the thumbnail: opens that pack on the
  full pack screen (reuse the existing `onOpenPack` callback).
- **Page indicator** so swiping is discoverable when `created.size > 1` (e.g. a
  small `2/3` label or dots above the name band;pick what fits the Neo kit).
- Tap a sticker → options sheet (NeoDialog-based, matching `PackScreen`'s
  sticker sheet): **Edit sticker** (primary), **Share**, **Delete** (danger,
  confirm). Wire via new callbacks consumed in `StikrApp.kt`:
  `onEditSticker(packId, fileName)` → `viewModel.editSticker(packId, fileName)`
  + navigate to EDITOR (same wiring the pack screen uses), `onShareSticker` →
  `viewModel.shareSticker`, `onDeleteSticker` → confirm dialog →
  `viewModel.deleteSticker`. Delete removes from the shown list by refreshing
  `state.created` through the view model's pack stream or by locally dropping
  the entry — choose the one that keeps DONE state truthful.
- The keep "N packs created" + summary headline, the `Done` button, and the
  PROCESSING screen. In PROCESSING, when `state.removeBackground` is true,
  update the status line to "Cutting out the subject…" in place of
  "Making stickers…".
- `CreateBatchDialog` gains a **Remove background** toggle
  (default OFF) and its `onConfirm` passes `removeBackground` through to
  `startBatch` from `StikrApp.kt` (4-argument call).

No unit tests required for this task; Task 3 owns render coverage.

## Task 3: Render coverage, full suite, verified build

- Extend `app/src/test/java/com/stikr/app/ui/ScreenRenderTest.kt` (or create a
  focused batch-screen render test following that file's capture pattern) with:
  a DONE capture with two packs, and a capture of the sticker sheet state if
  feasible within the existing harness patterns.
- Run the full `./gradlew testDebugUnitTest` — everything green.
- Run `./gradlew assembleRelease assembleDebug`, confirm the mapping/outputs,
  and copy both APKs to `build/stikr.apk` and `build/stikr-debug.apk`.
- Fix only mechanical failures discovered here (typos, missing imports,
  resource names); anything structural goes back to the controller as findings.
