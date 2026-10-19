"""Builds docs/theme/screens-{light,dark}.png from the ScreenRenderTest output.

Run from the repository root after
  ./gradlew testDebugUnitTest --tests "com.stikr.app.ui.ScreenRenderTest"
then copy both images to docs/site/assets/. Needs Pillow.

Each image is 1552x820: home (empty), home (packs), a WhatsApp-ready pack and
the editor, whole screens scaled to 820px tall, split by ink rules.
"""
from PIL import Image, ImageDraw

SOURCE = "app/build/ui-renders"
WIDTH, HEIGHT, GAP, RULE = 1552, 820, 20, 4
THEMES = {
    "light": ("#F1EDE7", "#191715"),
    "dark": ("#13110D", "#F1EDE7"),
}
SCREENS = ["home-empty", "home-packs", "pack-whatsapp", "editor"]

for theme, (paper, ink) in THEMES.items():
    screen_width = (WIDTH - GAP * (len(SCREENS) - 1)) // len(SCREENS)
    sheet = Image.new("RGB", (WIDTH, HEIGHT), paper)
    draw = ImageDraw.Draw(sheet)
    x = 0
    for index, screen in enumerate(SCREENS):
        render = Image.open(f"{SOURCE}/{screen}-{theme}.png").convert("RGB")
        sheet.paste(render.resize((screen_width, HEIGHT), Image.LANCZOS), (x, 0))
        x += screen_width
        if index < len(SCREENS) - 1:
            centre = x + GAP // 2
            draw.rectangle((centre - RULE // 2, 0, centre + RULE // 2 - 1, HEIGHT), fill=ink)
            x += GAP
    sheet.save(f"docs/theme/screens-{theme}.png", optimize=True)
