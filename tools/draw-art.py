"""The birds' textures and spawn eggs, painted box by box in each model's own
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
textures/item/budgie_spawn_egg.png and textures/block/perch.png. The icon (art/icon.png) is
drawn by hand, not here.
"""
from pathlib import Path

from PIL import Image

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


# Cockatiel: B body, F face and crest, C cheek patch, W wing bar, T tail, E eye, L feet, K beak.
COCKATIEL = {
	"grey": dict(B="#8d8d93", F="#f0dc5a", C="#f08a3a", W="#f2f2f4", T="#6f6f76", E="#141414", L="#8a8a90", K="#b7a98c", cheek=True, bar=True),
	"lutino": dict(B="#f6eec4", F="#f4e07a", C="#f08a3a", W="#fffbe8", T="#f1e6b0", E="#c43b3b", L="#e8a8b0", K="#d8c7a2", cheek=True, bar=False),
	"whiteface": dict(B="#8d8d93", F="#f4f4f6", C="#8d8d93", W="#f2f2f4", T="#6f6f76", E="#141414", L="#8a8a90", K="#b7a98c", cheek=False, bar=True),
}


def cockatiel(name, p):
	"""The cockatiel's texture in CockatielModel's layout (64x32): head 3x4x3 at 0,0; cap 3x1x3 at 14,0;
	crest 1x4x2 at 26,0; beak top 1x1x1 at 34,0; beak tip at 38,0; body 4x6x4 at 0,12; wing 1x5x4 at
	18,12; tail 2x7x1 at 30,12; leg 1x3x1 at 40,12."""
	img = Image.new("RGBA", (64, 32), (0, 0, 0, 0))

	head = Box(img, 0, 0, 3, 4, 3)
	head.fill(p["F"])
	# The face is yellow (or white) down to the chin; the eye sits high on each side, the cheek patch below and behind it.
	for side in ("left", "right"):
		head.paint(side, lambda x, y, w, h: p["E"] if (x, y) == (1, 0) else (p["C"] if p["cheek"] and (x, y) in ((1, 1), (2, 1), (1, 2), (2, 2)) else p["F"]))
	head.paint("back", lambda x, y, w, h: p["B"] if y >= 2 else p["F"])

	cap = Box(img, 14, 0, 3, 1, 3)
	cap.fill(p["F"])

	crest = Box(img, 26, 0, 1, 4, 2)
	crest.fill(p["F"])
	crest.paint("back", lambda x, y, w, h: shade(p["F"], 0.9))

	Box(img, 34, 0, 1, 1, 1).fill(p["K"])
	Box(img, 38, 0, 1, 1, 1).fill(shade(p["K"], 0.85))

	body = Box(img, 0, 12, 4, 6, 4)
	body.fill(p["B"])
	# The breast is a touch lighter than the back.
	body.paint("front", lambda x, y, w, h: shade(p["B"], 1.06))

	wing = Box(img, 18, 12, 1, 5, 4)
	wing.fill(shade(p["B"], 0.92))
	# The white bar along the folded wing's leading edge on the grey birds.
	for side in ("left", "right"):
		wing.paint(side, lambda x, y, w, h: p["W"] if p["bar"] and x == 0 and 1 <= y <= 3 else shade(p["B"], 0.92 if y < 4 else 0.8))

	tail = Box(img, 30, 12, 2, 7, 1)
	tail.fill(p["T"])

	Box(img, 40, 12, 1, 3, 1).fill(p["L"])

	(TEX / "entity/cockatiel").mkdir(parents=True, exist_ok=True)
	img.save(TEX / "entity/cockatiel" / f"cockatiel_{name}.png")


# Lovebird: B body, F face, R rump (the blue patch above the tail), T tail, E eye, L feet, K beak, Y breast.
LOVEBIRD = {
	"peach": dict(B="#6cc24a", F="#f29a6a", Y="#9edb6e", R="#4a8fd8", T="#3f9a3a", E="#141414", L="#9a9aa0", K="#e8d6a8"),
	"fischer": dict(B="#5fbf4a", F="#f0772c", Y="#e8d04a", R="#3f6fd0", T="#3f9a3a", E="#141414", L="#9a9aa0", K="#d8402a"),
	"blue": dict(B="#5c9ee0", F="#f4f6f8", Y="#9cc6ee", R="#3f6fd0", T="#3a74b8", E="#141414", L="#9a9aa0", K="#e8d6a8"),
}


def lovebird(name, p):
	"""The lovebird's texture in LovebirdModel's layout (32x32): head 3x3x3 at 0,0; beak top 1x1x1 at 12,0;
	beak tip at 16,0; body 3x5x3 at 0,7; wing 1x4x3 at 12,7; tail 2x3x1 at 20,7; leg 1x2x1 at 12,14."""
	img = Image.new("RGBA", (32, 32), (0, 0, 0, 0))

	head = Box(img, 0, 0, 3, 3, 3)
	head.fill(p["B"])
	# The face colour covers the front and the front half of each side; the eye sits high on the side.
	head.paint("front", lambda x, y, w, h: p["F"])
	for side in ("left", "right"):
		head.paint(side, lambda x, y, w, h: p["E"] if (x, y) == (1, 0) else (p["F"] if x <= 1 else p["B"]))
	head.paint("top", lambda x, y, w, h: p["F"] if y == 0 else p["B"])

	Box(img, 12, 0, 1, 1, 1).fill(p["K"])
	Box(img, 16, 0, 1, 1, 1).fill(shade(p["K"], 0.85))

	body = Box(img, 0, 7, 3, 5, 3)
	body.fill(p["B"])
	# A lighter breast, and the rump patch low on the back.
	body.paint("front", lambda x, y, w, h: p["Y"] if y < 3 else p["B"])
	body.paint("back", lambda x, y, w, h: p["R"] if y >= 3 else p["B"])

	wing = Box(img, 12, 7, 1, 4, 3)
	wing.fill(shade(p["B"], 0.9))
	for side in ("left", "right"):
		wing.paint(side, lambda x, y, w, h: shade(p["B"], 0.9 if y < 3 else 0.78))

	tail = Box(img, 20, 7, 2, 3, 1)
	tail.fill(p["T"])

	Box(img, 12, 14, 1, 2, 1).fill(p["L"])

	(TEX / "entity/lovebird").mkdir(parents=True, exist_ok=True)
	img.save(TEX / "entity/lovebird" / f"lovebird_{name}.png")


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


def egg(name, pal):
	img = Image.new("RGBA", (16, 16), (0, 0, 0, 0))
	for y, row in enumerate(EGG.strip("\n").split("\n")):
		for x, ch in enumerate(row):
			if ch != ".":
				img.putpixel((x, y), rgb(pal[ch]))
	img.save(TEX / "item" / f"{name}_spawn_egg.png")


MILLET = """
................
..............o.
.............oYo
............oYyo
..........ooYyo.
.........oYYyo..
........oYyyo...
.......oYyoYo...
......oYyooYo...
.....oYyo.oyo...
....oyyo..oo....
...oBbo.........
..oBbo..........
.oBbo...........
.obo............
.oo.............
"""


def millet():
	"""The millet spray item: a drooping sprig of golden grain on a brown stalk, in the house's outlined pixel style."""
	pal = {"o": "#4a3a1e", "Y": "#f0d060", "y": "#c9a83a", "B": "#9a7a3a", "b": "#6e5426"}
	img = Image.new("RGBA", (16, 16), (0, 0, 0, 0))
	for y, row in enumerate(MILLET.strip("\n").split("\n")):
		for x, ch in enumerate(row):
			if ch != ".":
				img.putpixel((x, y), rgb(pal[ch]))
	img.save(TEX / "item/millet_spray.png")


def perch():
	"""The perch block's texture, in the layout models/block/perch.json reads: the base's top (0,0 to 6,6) and
	sides (0,6 to 6,8) in oak plank; the post's sides (6,0 to 8,8); the bar's top, underside and sides
	(0,8 to 12,14, a peeled stick with a string wrap near each end) and its ends (12,8 to 14,10)."""
	plank, plank_dark, plank_line = "#b8945f", "#9a7a4a", "#6e5433"
	post, post_dark = "#a88654", "#8a6c42"
	stick, stick_dark, stick_light = "#d2b283", "#a98b5f", "#e6cc9e"
	string, string_dark = "#eadcc0", "#bfae8c"
	img = Image.new("RGBA", (16, 16), (0, 0, 0, 0))
	px = img.putpixel
	for y in range(6):
		for x in range(6):
			px((x, y), rgb(plank_line if y in (2, 5) else plank_dark if (x + y) % 5 == 0 else plank))
	for y in range(6, 8):
		for x in range(6):
			px((x, y), rgb(plank_dark if y == 7 else plank))
	for y in range(8):
		for x in range(6, 8):
			px((x, y), rgb(post_dark if x == 7 or y % 4 == 3 else post))
	for y in range(8, 14):
		for x in range(12):
			wrapped = x in (1, 2, 9, 10)
			if y < 10:
				colour = string if wrapped else stick_light if y == 8 else stick
			elif y < 12:
				colour = string_dark if wrapped else stick_dark
			else:
				colour = (string if y == 12 else string_dark) if wrapped else (stick if y == 12 else stick_dark)
			px((x, y), rgb(colour))
	for y in range(8, 10):
		for x in range(12, 14):
			px((x, y), rgb(stick_dark if (x + y) % 2 else stick))
	(TEX / "block").mkdir(parents=True, exist_ok=True)
	img.save(TEX / "block/perch.png")


if __name__ == "__main__":
	(TEX / "entity/budgie").mkdir(parents=True, exist_ok=True)
	(TEX / "item").mkdir(parents=True, exist_ok=True)
	for name, palette in PALETTES.items():
		budgie(name, palette)
	for name, palette in COCKATIEL.items():
		cockatiel(name, palette)
	egg("budgie", {"o": "#2e2119", "B": "#7fcf3a", "b": "#4f9a2a", "Y": "#f4e45a"})
	for name, palette in LOVEBIRD.items():
		lovebird(name, palette)
	egg("cockatiel", {"o": "#2e2119", "B": "#8d8d93", "b": "#6f6f76", "Y": "#f0dc5a"})
	egg("lovebird", {"o": "#2e2119", "B": "#6cc24a", "b": "#3f9a3a", "Y": "#f29a6a"})
	perch()
	millet()
	print("art written")
