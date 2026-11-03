# Progress

What's done, what's broken, what's next. Newest entry first. Each session adds a row before it
ends, saying what changed, what the game test counted, what the proof run was (the feature off,
which checks fail, code put back), what was learned, and what was deliberately not done.

## Where things stand

- **Done:** the skeleton. A budgie entity in four colours, tamed with seeds, that flies, sits when
  told and follows its owner; a spawn egg; placeholder textures on the parrot's model; game tests on
  both loaders that load the mod alongside Vibrant Parrots, Cozy Core and Cosy Critters and put a
  budgie in the world; CI for all of it.
- **Broken or placeholder:** the budgie wears the parrot's model and voice; the textures and the
  icon are flat placeholders until the real pixel art is drawn.
- **Not yet:** natural spawning, the perch, the greeting and dusk state, shoulder on request, the
  budgie's own sounds.

## Next (0.1.0, locked)

1. The budgie's own model, four real textures, flap and head-bob animation, natural spawning
   (jungle edges and savanna, with a setting) and the spawn egg art; taming and spawning tests.
2. The perch block; perch-as-home goal; sleeping on it at night; the perch test.
3. The greeting on return and the dusk state, with sounds; shoulder on request (sneak-use on,
   sneak-use off, never jumps off on its own); their tests.
4. Release prep: the icon, a hand play of the release jar, the listing.

New ideas during the build go to 0.2.0 or later, never into 0.1.0.

## Sessions

| Date | What happened |
|---|---|
| 2026-11-03 | Session 1: the repo from the multiloader template (`common` / `fabric` / `neoforge`), the budgie registered with attributes and a spawn egg, a renderer on the parrot's model with placeholder textures, and a game test on each loader (the mod and the compat mods load; a budgie put in the world is alive 40 ticks later, has 5 health, keeps its colour and starts wild). The NeoForge test passed alone and with Vibrant Parrots, Puzzles Lib and Cozy Core loaded (2 of 2 checks each). Proof run: asked for a mod that isn't there, the loaded check failed as it should. The Fabric client test passed in a real game, alone (7 of 7 checks) and with Vibrant Parrots, Puzzles Lib, Forge Config API Port, Cosy Critters and Cozy Core loaded (11 of 11); Puzzles Lib's newest build needs Fabric API 0.163 or later, so the project now builds against 0.165.0. CI runs both tests alone and with the compat mods; the first run on GitHub passed all five jobs. |
