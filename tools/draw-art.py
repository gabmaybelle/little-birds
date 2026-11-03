"""The budgie textures, the spawn egg and a stand-in icon, painted box by box in the model's own
texture layout (BudgieModel.createBodyLayer): head 2x3x2 at 0,0; skull cap 2x1x3 at 8,0; beak
top 1x1x1 at 18,0; beak tip 1x1x1 at 22,0; body 3x5x3 at 0,6; wing 1x4x3 at 12,6; tail 2x5x1 at
20,6; leg 1x2x1 at 12,13; 32x32. Shaded the way vanilla shades its mobs: each face lighter at the
top and darker at the bottom, tops light, undersides dark.

Real markings, checked against bird references: the wild green has a yellow forehead, face and
throat; black scalloping edged in yellow from the nape down the back and over the folded wings;
violet cheek patches with three black throat spots beside each; a cobalt tail; grey-blue feet and
a horn-coloured beak. The blue is the same bird with a white face and white edging. Lutino (all
yellow) and albino (all white) have no black at all: silvery-white cheeks, red eyes, pink feet.

    py tools/draw-art.py

Writes common/src/main/resources/assets/little_birds/textures/entity/budgie/budgie_<colour>.png,
textures/item/budgie_spawn_egg.png and art/icon.png (the icon is a stand-in until the real one).
"""
from pathlib import Path

from PIL import Image, ImageDraw

ROOT = Path(__file__).resolve().parent.parent
TEX = ROOT / "common/src/main/resources/assets/little_birds/textures"


def rgb(h):
	return (int(h[1:3], 16), int(h[3:5], 16), int(h[5:7], 16), 255)


def shade(h, k):
	"""A colour scaled by k (1.0 as is, below darker, above lighter), as a hex string."""
	r, g, b, _ = rgb(h)
	f = lambda c: max(0, min(255, round(c * k)))
	return "#%02x%02x%02x" % (f(r), f(g), f(b))


# F face, B body, K scallop, G scallop edging, C cheek patch, S throat spot, T tail, E eye, L feet
PALETTES = {
	"green": dict(F="#f2e35c", B="#7ccf3c", K="#4a5e34", G="#dcd678", C="#7a6ddc", S="#262d1e", T="#2b5fb0", E="#141414", L="#8a8fa0", barred=True),
	"blue": dict(F="#f2f6fa", B="#5a9fe6", K="#3e4e6a", G="#d8e4f2", C="#6a60c8", S="#262c38", T="#2a4fa8", E="#141414", L="#8a8fa0", barred=True),
	"yellow": dict(F="#fbeea0", B="#f5db5e", K="#f5db5e", G="#f5db5e", C="#f0f0ee", S="#fbeea0", T="#f0d55a", E="#c43b3b", L="#e8a8b0", barred=False),
	"white": dict(F="#ffffff", B="#f3f5f7", K="#f3f5f7", G="#f3f5f7", C="#eeeef2", S="#ffffff", T="#eef1f4", E="#c43b3b", L="#e8a8b0", barred=False),
}
BEAK = "#d8c08a"
# How each kind of face is lit, as vanilla does it: the top bright, sides even, the underside dark.
LIGHT = {"top": 1.12, "front": 1.0, "back": 0.92, "left": 0.96, "right": 0.96, "bottom": 0.72}


class Box:
	"""One cube's six faces in Minecraft's texture layout: top and bottom in a strip above the four sides."""

	def __init__(self, img, u, v, w, h, d):
		self.img, self.u, self.v, self.w, self.h, self.d = img, u, v, w, h, d

	def face(self, name):
		u, v, w, h, d = self.u, self.v, self.w, self.h, self.d
		return {
			"top": (u + d, v, w, d),
			"bottom": (u + d + w, v, w, d),
			"right": (u, v + d, d, h),
			"front": (u + d, v + d, w, h),
			"left": (u + d + w, v + d, d, h),
			"back": (u + 2 * d + w, v + d, w, h),
		}[name]

	def paint(self, name, colour_at):
		"""colour_at(x, y, w, h) -> hex colour for each pixel; the face's light and a top-to-bottom gradient are applied."""
		x0, y0, w, h = self.face(name)
		for y in range(h):
			for x in range(w):
				base = colour_at(x, y, w, h)
				grad = 1.0 if h <= 1 else 1.05 - 0.12 * (y / (h - 1))
				self.img.putpixel((x0 + x, y0 + y), rgb(shade(base, LIGHT[name] * grad)))

	def fill(self, colour, faces=("top", "bottom", "front", "back", "left", "right")):
		for face in faces:
			self.paint(face, lambda x, y, w, h: colour)


def budgie(name, p):
	img = Image.new("RGBA", (32, 32), (0, 0, 0, 0))

	def scallops(base, rows=None):
		"""Scallops as solid rows, the way the parrot wing is striped: black, then the light edging, then the base colour."""
		def at(x, y, w, h):
			if not p["barred"] or (rows is not None and y not in rows):
				return base
			return p["K"] if y % 4 == 0 else p["G"] if y % 4 == 1 else base
		return at

	head = Box(img, 0, 0, 2, 3, 3)
	head.fill(p["F"])
	# The throat spots in a row under the chin, and the cheek patch on each side with the eye above it.
	head.paint("front", lambda x, y, w, h: p["S"] if y == 2 else p["F"])
	for side in ("left", "right"):
		head.paint(side, lambda x, y, w, h: p["E"] if (x, y) == (1, 0) else p["C"] if (x, y) in ((1, 1), (2, 1), (2, 2)) else p["S"] if (x, y) == (1, 2) else p["F"])
	head.paint("back", scallops(p["F"]))

	cap = Box(img, 10, 0, 2, 1, 3)
	cap.fill(p["F"])
	# The crown: yellow over the forehead, the first scallops at the back of the skull.
	cap.paint("top", lambda x, y, w, h: scallops(p["F"])(x, y, w, h) if y == h - 1 else p["F"])
	cap.paint("back", scallops(p["F"]))

	Box(img, 20, 0, 1, 1, 1).fill(BEAK)
	Box(img, 24, 0, 1, 1, 1).fill(shade(BEAK, 0.85))

	body = Box(img, 0, 6, 3, 5, 3)
	body.fill(p["B"])
	# The mantle is scalloped; the breast, belly and flanks are plain (the wings cover the rest).
	body.paint("top", scallops(p["B"]))
	body.paint("back", lambda x, y, w, h: scallops(p["B"])(x, y, w, h) if y < 3 else p["B"])

	wing = Box(img, 12, 6, 1, 4, 3)
	wing.fill(shade(p["B"], 0.9))
	# The outside of a folded wing: scalloped coverts, the flight feathers darker at the tip.
	for side in ("left", "right"):
		wing.paint(side, lambda x, y, w, h: scallops(p["B"])(x, y, w, h) if y < 3 else shade(p["B"], 0.8))

	tail = Box(img, 20, 6, 2, 5, 1)
	tail.fill(p["T"])

	Box(img, 12, 13, 1, 2, 1).fill(p["L"])

	img.save(TEX / "entity/budgie" / f"budgie_{name}.png")


EGG = """
................
......oooo......
.....oBBBBo.....
....oBBYBBBo....
...oBBBBBBBBo...
...oBYBBBBYBo...
..oBBBBBYBBBBo..
..oBBBBBBBBBBo..
..oBBYBBBBBBBo..
..oBBBBBBYBBBo..
..obBBBBBBBbbo..
...obbBYBBbbo...
...obbbbbbbbo...
....obbbbbbo....
.....oooooo.....
................
"""


def egg():
	pal = {"o": "#2e2119", "B": "#7fcf3a", "b": "#4f9a2a", "Y": "#f4e45a"}
	img = Image.new("RGBA", (16, 16), (0, 0, 0, 0))
	for y, row in enumerate(EGG.strip("\n").split("\n")):
		for x, ch in enumerate(row):
			if ch != ".":
				img.putpixel((x, y), rgb(pal[ch]))
	img.save(TEX / "item/budgie_spawn_egg.png")


def icon():
	"""A stand-in: a green budgie on a perch, a 32-px sprite scaled to 512 on transparency. The real icon replaces it."""
	img = Image.new("RGBA", (32, 32), (0, 0, 0, 0))
	d = ImageDraw.Draw(img)
	d.rectangle((6, 25, 25, 26), fill=(150, 110, 70))
	d.rectangle((15, 27, 16, 31), fill=(120, 90, 55))
	d.ellipse((10, 10, 21, 25), fill=(127, 207, 58))
	d.ellipse((12, 5, 20, 13), fill=(244, 228, 90))
	d.rectangle((19, 9, 21, 10), fill=(216, 192, 138))
	d.point((17, 8), fill=(20, 20, 20))
	d.rectangle((13, 16, 15, 23), fill=(79, 154, 42))
	d.rectangle((9, 20, 10, 27), fill=(43, 95, 176))
	(ROOT / "art").mkdir(exist_ok=True)
	img.resize((512, 512), Image.NEAREST).save(ROOT / "art/icon.png")


if __name__ == "__main__":
	(TEX / "entity/budgie").mkdir(parents=True, exist_ok=True)
	(TEX / "item").mkdir(parents=True, exist_ok=True)
	for name, palette in PALETTES.items():
		budgie(name, palette)
	egg()
	icon()
	print("art written")
