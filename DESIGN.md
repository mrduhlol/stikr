---
name: stikr
description: Little collections of photos and words, ready for WhatsApp.
colors:
  background: "#0C0E0D"
  surface: "#20251F"
  foreground: "#EEEDE7"
  muted: "#B8BFB0"
  primary: "#303B29"
  on-primary: "#E1EBCD"
  violet: "#765384"
  olive: "#647342"
  card-default: "#E5D6C8"
  card-ink: "#211B21"
typography:
  heading:
    fontFamily: "Android sans-serif"
    fontSize: "30sp"
    fontWeight: 600
    lineHeight: 1.13
  body:
    fontFamily: "Android sans-serif"
    fontSize: "14sp"
    fontWeight: 400
    lineHeight: 1.5
rounded:
  pack: "0dp"
  control: "15dp"
  dialog: "23dp"
spacing:
  gutter: "20dp"
  grid: "12dp"
  small: "8dp"
components:
  primary:
    backgroundColor: "{colors.primary}"
    textColor: "{colors.on-primary}"
    rounded: "{rounded.control}"
---

## Overview
Native Android Operate surface. The approved mockup replaces the older
neo-brutalist print world. Preserve the approved black/lime S logo with its large folded corner and collectible
card details. Reference: docs/mockups/pack-refresh-preview.html.

## Colors
Near-black graphite with restrained forest and olive gradients across every screen.
Controls use dark inset fills, quiet olive edges, cream text and native switches.
Avoid bright lime action blocks and white outlines. Color the sharp pack frame and
LVL tab from the thumbnail; use exactly the same tint on both. Card text is dark.
The WhatsApp slider has a subtle liquid front tied to drag progress, with reduced-motion support.

## Typography
Use Android sans for interface text. Headings are sentence case, semibold.
Offer 12 offline fonts: Sans, Serif, Mono, Anton, Caveat, Pacifico, Oswald, Bebas Neue, Lobster, Playfair Display, Orbitron and Righteous.
Movable sticker text spans 8–320 pixels, with pinch scaling and free two-finger rotation. Cover text reaches 400 pixels on the 1080 × 1440 output.
Pack titles use large Oswald type with the author directly below.
Creation controls have prominent labels and short secondary explanations.

## Layout
Two pack columns. Creation actions precede Your packs. Single stickers have a
horizontal row, then a thin divider, then normal packs. No bottom navigation,
filters or column/settings control. The header has a 52dp approved S logo, a sound toggle and a folder entry for local backup export and restore. A round creation action appears only after
the creation row scrolls away. Pack detail owns the WhatsApp slider at bottom.

## Elevation & Depth
Use subtle control borders and diffuse modal scrims. Blur background content
for single-sticker confirmation. Card artwork carries the visual interest.

## Shapes
Pack cards have sharp corners. Their 3:4 portrait cover exports at 1080 by 1440.
The LVL/progress tab is flush with the outer top-left edge, with a diagonal cut.
Other controls are softly rounded; slide track and handle are pills/circles.

## Components
Pack metadata: 25 diagonal segments proportionally track the 30-sticker capacity, extending toward the cut above a larger LVL serial; CHECKPOINT MMM YYYY, middle dot,
persistent numeric ID, sticker count out of 30, title, and a boxed centered
WhatsApp vector. Three-dot menu: Add thumbnail, Rename Title, Rename author.
Singles long-press: Add to WhatsApp, Delete with confirmation. Export requires
a full slide; cancellation returns the handle. Only confirmed WhatsApp success
triggers a compact card rising from below with a bottom-hinged unfolding entrance.
Keep Pack added above and a clear success check below. Use no particle burst.
Reduced motion shows the final state immediately.

## Editing
All text controls appear below Your text: font, size, colour, transparent or custom
background, bold, italic, underline, strikethrough, alignment, rotation and centring.
Multiple text layers keep their transforms when saved and reopened. Outline defaults
to off. Pack edit applies a common look to every sticker using stored originals.
The single-sticker editor saves to the shared Stikr pack before requesting WhatsApp
import, and also offers Save only. Blank new pack names use one of 150 offline words.
The slider grows its handle, adds light drag ticks and refracted water, and dims the
submerged part of its label. Reduced motion suppresses continuous water animation.

## Do's and Don'ts
Keep exact card/tab color alignment. Use real vector arrows and WhatsApp mark.
Do not restore settings, filters, bottom navigation, launch-from-handle animation,
cartwheel rotation, or the Yay wording. Do not celebrate cancelled/failed imports.
