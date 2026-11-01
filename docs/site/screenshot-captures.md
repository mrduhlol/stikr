# App screen captures

These images show the native Android Compose interface rendered by Stikr's
Robolectric screen tests. They are app UI captures with sample content, not
photographs or screenshots from a physical device. The website's browser demo
is a separate simulation.

| Website asset | Native capture source | Output | Bytes |
| --- | --- | --- | ---: |
| `assets/app-home.webp` | `app/build/ui-renders/home-packs-light.png` | 400 × 880 | 30,614 |
| `assets/app-pack.webp` | `app/build/ui-renders/approved-pack-native.png` | 400 × 880 | 33,002 |
| `assets/app-editor.webp` | `app/build/ui-renders/editor-dark.png` | 400 × 880 | 22,256 |

The source captures are 1200 × 2640 PNG files from the November 1, 2026 test
run. The home capture's historical filename says `light`, but its rendered
interface uses the current graphite theme. The pack capture includes the cover
photo supplied by the user and the enlarged title and LVL treatment. The home
and editor use sample fixture stickers.

Preparation only resized the complete images with Lanczos resampling and encoded
them as WebP at quality 92. It did not crop, generate, recolour, rearrange, or
replace any UI or image content. The three assets total 85,872 bytes.
