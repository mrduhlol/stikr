# Stikr website design

The website is a download and product demonstration page. It has its own web
layout and does not change the native Android design system in the root DESIGN.md.

## Built design

- Black page background, graphite/forest controls and cream text. Lime is reserved
  for branding, links and download actions. The 60/30/10 direction is an attention
  hierarchy, not a measured pixel quota. The later pure-black background brief wins.
- Self-hosted Anton display headings and system body text. Desktop content is capped
  at 1140px. Mobile keeps 18px gutters, puts downloads before screenshots, and uses
  a horizontal screenshot gallery with scroll snapping.
- A product-led split hero, three native UI captures, flat feature rows, an embedded
  demo, a five-step guide, FAQs and a final download action. No invented reviews,
  popularity counts, awards or performance claims.
- Parent demo controls select the trusted same-origin iframe's screen by postMessage.
  The child validates origin, parent identity, message type and permitted screen.
  The frame has an explicit scroll cue and a standalone link.
- Mobile has a download action after the hero action leaves view. Browser back/forward
  restoration reconnects its observer.

## Artwork and motion

ASCII Magic's Jellyfish piece and toolkit are vendored under assets/ascii with the
upstream MIT license. Source: https://github.com/kailashsr38/ascii-magic-art.
The renderer excludes the library's water and particle palette keys. Jellyfish cells
use only ` .:-=+*#%@` and equal RGB channels, drawn on black. It fits the character
plane to preserve silhouettes without stretching. The uploaded source video is not
used in this library-based version.

The background canvas ignores pointer events and stays below content. Drawing is
capped at 12fps on narrow screens and 18fps on desktop, with an 8fps fallback after
slow frames. Device pixel ratio is capped at 1.5. Hidden tabs pause; reduced-motion
preferences produce a still. Listeners and animation loops clean up on pagehide.

## Raster provenance

See docs/site/screenshot-captures.md for native capture paths and WebP conversion.
These are Compose/Robolectric captures with sample content, not physical-device
captures. All three retain complete frames and total 85,872 bytes.
The site logo is an unchanged copy of docs/brand/stikr-pebble.png. Demo JPGs are
unchanged copies of user-supplied docs/mockups/assets images. Fonts and their license
files are local. No generative images or copied reference-site assets were used.

## Product and demo boundaries

Browser edits stay local. Photo input accepts JPG, PNG and WebP up to 8MB, with a
24-megapixel limit. Images decode before saves, stale photo requests cancel, and blob
URLs are revoked on replacement/cleanup. PNG export is browser-local. WhatsApp import
and Android cut-out are labelled Android-only. Packs in the demo are examples.
Installation, local ZIP backup, restoration, 12 fonts, outlines off by default and
3:4 covers follow current Android source behavior. The consumer guides were updated.

## Finish review

Reviewed against Impeccable and Unslop. Production references: linear.app,
raycast.com and culturedcode.com/things. Their layout principles informed hierarchy;
none of their assets, marketing claims or code were copied.

Luna reviewed paths, responsive structure, iframe messaging and reduced motion.
Findings addressed: neutral demo author, proper pressed-state semantics, visible
scroll cue, and standalone access. Its proposed literal palette-ratio adjustment
was declined because the later pure-black requirement is explicit.

Main page renders passed at 390/1440px and final 320/1440px, without horizontal
overflow. Pixel checks found visible ASCII and zero non-grey canvas pixels. The demo
passed 320px interactions and 390px rendering, including 12 fonts, pinch scaling,
photo selection, Canvas PNG save, parent messaging, origin rejection and URL cleanup.
Node syntax and static path scans passed. Extended 728px demo/headless integration
captures exceeded the preview service's 20-second load limit, so full desktop demo
visual validation remains a known limit. Its source review passed.

Verdict: ready for the public download page, with the extended desktop demo capture
limit recorded. GitHub Pages deployment and public page/demo responses are checked
separately at release. The native APK is unchanged by these website files.
