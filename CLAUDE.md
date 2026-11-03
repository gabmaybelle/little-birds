# Little Birds

Small pet birds for Minecraft 26.3 (namespace `little_birds`), budgies first, that live on a
perch in your house: a tamed bird makes the nearest perch its home, chirps when its owner comes
back, naps there at dusk, and rides a shoulder only when asked. Fabric and NeoForge jars from one
codebase (`common` holds everything that doesn't care about the loader; `fabric` and `neoforge`
are thin entrypoints and a `Platform` each).

The feel: slow-paced, peaceful, cute. It must never become a species encyclopedia, a breeding sim
or a utility pet. Every new feature is tested against those three words before it's built.

`PROGRESS.md` says what's done, what's broken and what's next, with a row per session: read it
first, and add this session's row before ending.

## This mod's job

**Little Birds**: small pet birds (budgies first) that live on a perch in your house. Anything
else (new mobs that aren't house pets, decoration, farming) belongs elsewhere: ask Gabby where.

## How it's built

- The budgie extends vanilla's shoulder-riding tamable base (what the parrot is built on), so
  flying, seed taming, sitting and owner-following are vanilla's. The mod writes only the perch,
  the greeting, the dusk state and the models. No GeckoLib, no Kotlin.
- Registration goes through `platform/Platform`: Fabric registers at once, NeoForge queues for
  its registry events. Attributes have their own hook (`registerAttributes`).
- Nothing runs every tick on the server for the mod's own sake: bird behaviour lives in goals that
  only run while the bird itself ticks.
- The renderer currently borrows vanilla's parrot model with budgie textures; the budgie's own
  model is the next step.

## Testing

- `./gradlew :fabric:runClientGameTest`: a real game plays through `fabric/src/gametest` (opens a
  window). `./gradlew :neoforge:runGameTestServer`: vanilla's test server runs
  `neoforge/src/gametest` (no window). Both take `-PcompatMods=<folder>` and `-PexpectMods=<ids>`;
  `tools/fetch-test-mods.js` fetches the compat mods (Vibrant Parrots, Cozy Core, Cosy Critters).
- Every new check needs a proof run: the feature switched off, the check fails, the code put back.
- CI (`.github/workflows/build.yml`) builds the jars and runs both game tests, alone and with the
  compat mods, on every push that touches code.

## Rules for public files

Commit messages, README, PROGRESS.md and release notes say what changed and what the tests
counted; they never name local tools, folders or instances, and never credit anyone but Gabby.
Canadian English in docs and messages; in-game text follows Minecraft's own wording.
