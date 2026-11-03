"""Writes the NeoForge game tests' structure template: an empty 7 x 5 x 7 room, so a test that
needs a few blocks of space (a perch and a bird flying to it) keeps everything inside the test's
own bounds, which the framework keeps loaded and ticking. Vanilla's own "empty" template is one
block. The file is plain NBT (gzip), written here without a library.

    py tools/make-test-room.py
"""
import gzip
import struct
from pathlib import Path

OUT = Path(__file__).resolve().parent.parent / "neoforge/src/gametest/resources/data/little_birds_gametest/structure/room.nbt"
SIZE = (7, 5, 7)
DATA_VERSION = 5023  # the version vanilla's 26.3 templates carry


def tag_name(name):
	raw = name.encode()
	return struct.pack(">H", len(raw)) + raw


def tag_int(name, value):
	return b"\x03" + tag_name(name) + struct.pack(">i", value)


def tag_string(name, value):
	raw = value.encode()
	return b"\x08" + tag_name(name) + struct.pack(">H", len(raw)) + raw


def tag_list(name, kind, payloads):
	return b"\x09" + tag_name(name) + bytes([kind]) + struct.pack(">i", len(payloads)) + b"".join(payloads)


def compound_payload(*tags):
	return b"".join(tags) + b"\x00"


def ints(values):
	return [struct.pack(">i", v) for v in values]


root = (
	b"\x0a" + tag_name("")
	+ tag_list("size", 3, ints(SIZE))
	+ tag_list("entities", 10, [])
	+ tag_list("blocks", 10, [compound_payload(tag_list("pos", 3, ints((0, 0, 0))), tag_int("state", 0))])
	+ tag_list("palette", 10, [compound_payload(tag_string("id", "minecraft:air"))])
	+ tag_int("DataVersion", DATA_VERSION)
	+ b"\x00"
)

OUT.parent.mkdir(parents=True, exist_ok=True)
with gzip.open(OUT, "wb") as f:
	f.write(root)
print("wrote", OUT)
