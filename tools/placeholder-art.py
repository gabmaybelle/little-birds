"""Placeholder art for the first builds: four budgie textures on the parrot's UV layout, a spawn
egg and a jar icon. Plain shapes in flat colours with a darker shade, so the bird is readable in
the game until the real pixel art replaces them.

    py tools/placeholder-art.py

Writes into common/src/main/resources/assets/little_birds/textures/ and art/icon.png.
"""
from pathlib import Path

from PIL import Image, ImageDraw

ROOT = Path(__file__).resolve().parent.parent
TEX = ROOT / "common/src/main/resources/assets/little_birds/textures"

# base, shade, cheek (the budgie's cheek spots), beak
COLOURS = {
    "green": ((108, 196, 72), (62, 140, 44), (72, 92, 200), (214, 170, 110)),
    "blue": ((96, 150, 230), (54, 98, 180), (60, 60, 120), (214, 170, 110)),
    "yellow": ((240, 220, 80), (190, 160, 40), (120, 110, 200), (230, 180, 110)),
    "white": ((236, 240, 244), (180, 190, 200), (150, 150, 220), (230, 180, 110)),
}


def box(draw, x, y, w, h, d, base, shade):
    """One cube of the parrot layout at tex offset (x, y): top/bottom strip, then the four sides."""
    # Top and bottom.
    draw.rectangle((x + d, y, x + d + w - 1, y + d - 1), fill=base)
    draw.rectangle((x + d + w, y, x + d + w + w - 1, y + d - 1), fill=shade)
    # Sides: left, front, right, back.
    draw.rectangle((x, y + d, x + d - 1, y + d + h - 1), fill=shade)
    draw.rectangle((x + d, y + d, x + d + w - 1, y + d + h - 1), fill=base)
    draw.rectangle((x + d + w, y + d, x + d + w + d - 1, y + d + h - 1), fill=shade)
    draw.rectangle((x + d + w + d, y + d, x + d + w + d + w - 1, y + d + h - 1), fill=shade)


def budgie(name, base, shade, cheek, beak):
    img = Image.new("RGBA", (32, 32), (0, 0, 0, 0))
    d = ImageDraw.Draw(img)
    box(d, 2, 8, 3, 6, 3, base, shade)        # body
    box(d, 22, 1, 3, 4, 1, shade, shade)      # tail
    box(d, 19, 8, 1, 5, 3, shade, shade)      # wings (both use this box)
    box(d, 2, 2, 2, 3, 2, base, shade)        # head
    box(d, 10, 0, 2, 1, 4, base, shade)       # head2 (the brow)
    box(d, 11, 7, 1, 2, 1, beak, beak)        # beak
    box(d, 16, 7, 1, 2, 1, beak, beak)        # beak2
    d.rectangle((14, 18, 17, 21), fill=(120, 100, 90))   # legs
    d.rectangle((2, 18, 6, 23), fill=base)               # feather (a flat strip)
    # Eyes and cheek spots on the head's front face (head box at 2,2: front face x 4..5, y 4..6).
    d.point((4, 4), fill=(20, 20, 20))
    d.point((5, 4), fill=(20, 20, 20))
    d.point((4, 6), fill=cheek)
    d.point((5, 6), fill=cheek)
    img.save(TEX / "entity/budgie" / f"budgie_{name}.png")


def egg():
    img = Image.new("RGBA", (16, 16), (0, 0, 0, 0))
    d = ImageDraw.Draw(img)
    d.ellipse((4, 2, 11, 13), fill=(108, 196, 72), outline=(62, 140, 44))
    for x, y in ((6, 5), (9, 8), (7, 10)):
        d.point((x, y), fill=(240, 220, 80))
    img.save(TEX / "item/budgie_spawn_egg.png")


def icon():
    """A green budgie on a perch, as a 32-px sprite scaled to 512 on transparency."""
    img = Image.new("RGBA", (32, 32), (0, 0, 0, 0))
    d = ImageDraw.Draw(img)
    d.rectangle((6, 25, 25, 26), fill=(150, 110, 70))          # perch
    d.rectangle((15, 27, 16, 31), fill=(120, 90, 55))          # its post
    d.ellipse((10, 10, 21, 25), fill=(108, 196, 72))           # body
    d.ellipse((12, 5, 20, 13), fill=(240, 220, 80))            # head
    d.rectangle((19, 9, 21, 10), fill=(230, 180, 110))         # beak
    d.point((17, 8), fill=(20, 20, 20))                        # eye
    d.rectangle((13, 16, 15, 23), fill=(62, 140, 44))          # wing
    d.rectangle((9, 20, 10, 27), fill=(72, 92, 200))           # tail
    big = img.resize((512, 512), Image.NEAREST)
    (ROOT / "art").mkdir(exist_ok=True)
    big.save(ROOT / "art/icon.png")


if __name__ == "__main__":
    (TEX / "entity/budgie").mkdir(parents=True, exist_ok=True)
    (TEX / "item").mkdir(parents=True, exist_ok=True)
    for name, colours in COLOURS.items():
        budgie(name, *colours)
    egg()
    icon()
    print("placeholder art written")
