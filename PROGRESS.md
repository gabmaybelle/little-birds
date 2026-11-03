# Progress

What's done, what's broken, what's next. Newest entry first. Each session adds a row before it
ends, saying what changed, what the game test counted, what the proof run was (the feature off,
which checks fail, code put back), what was learned, and what was deliberately not done.

## Where things stand

- **Done:** the budgie: its own model and animation (flapping in the air, little steps and a head
  bob on the ground, settling down to sit), textures with the real markings in four colours, a
  spawn egg with its own art; tamed with seeds, sits when told, follows its owner; wild budgies in
  savannas and jungles in small flocks, with a settings file to turn them off or make them rarer.
  Game tests on both loaders cover all of it, alone and alongside Vibrant Parrots, Cozy Core and
  Cosy Critters; CI runs them on every push.
- **Placeholder:** the icon (a stand-in from the art script) and the budgie's voice (the
  parrot's, pitched up) until the real ones exist. The textures are drawn by `tools/draw-art.py`;
  hand-painted ones can replace them in the same layout.
- **Not yet:** the perch, the greeting and dusk state, shoulder on request, the budgie's own sounds.

## Next (0.1.0, locked)

1. The perch block; perch-as-home goal; sleeping on it at night; the perch test.
2. The greeting on return and the dusk state, with sounds; shoulder on request (sneak-use on,
   sneak-use off, never jumps off on its own); their tests.
3. Release prep: the icon, a hand play of the release jar, the listing.

New ideas during the build go to 0.2.0 or later, never into 0.1.0.

## Sessions

| Date | What happened |
|---|---|
| 2026-11-03 | Session 2: the budgie's own model, built the way vanilla builds its parrot rather than as a stack of cubes (a body pitched forward, a small head with a skull cap for the round forehead, a two-piece hooked beak, thin wings lying along the body, a flat tail plate, short legs; about three quarters of a parrot) with flap, step, head-bob and sitting animation; textures painted box by box from bird references (yellow forehead and face, scalloping as soft narrow bars from the nape over the back and wings, violet cheek patches with throat spots, cobalt tail; lutino and albino with no bars, red eyes and pink feet), shaded per face like vanilla; spawn egg art. The first cut was three plain boxes with flat paint and a gap under the head; Gabby's calls on the way: less blocky, like the parrot; a solid face; bars not a checker; then narrower and lighter bars. The screenshot step now poses a still budgie beside a vanilla parrot for scale; natural spawning in savannas (weight 15) and jungles (10) in flocks of 2 to 4, on grass or leaves in daylight, with `config/little_birds.properties` (`natural_spawning`, `spawn_chance`); mob registration moved into one `MobSpec` so Fabric builds the spawn rule into the entity type and NeoForge uses its events and biome modifier files. New checks: seeds tame a budgie (2 tries this run) and it belongs to the feeder; a use on the ground sits it and the next stands it; the spawn rule passes on grass, fails on stone, with spawning off and with chance 0, then passes again; savanna, jungle and sparse jungle list the budgie at the mod's weights and plains don't. Fabric: 25 of 25 checks with the compat mods and every check alone; NeoForge: 5 of 5 tests alone and with the compat mods. The settings checks are their own proof (each rule is tested both ways). Found on the way: NeoForge 26.3's `add_spawns` wants vanilla's `count` object, not `minCount`/`maxCount`; a bird without AI never lands, so the sit check sets it down first. |
| 2026-11-03 | Session 1: the repo from the multiloader template (`common` / `fabric` / `neoforge`), the budgie registered with attributes and a spawn egg, a renderer on the parrot's model with placeholder textures, and a game test on each loader (the mod and the compat mods load; a budgie put in the world is alive 40 ticks later, has 5 health, keeps its colour and starts wild). The NeoForge test passed alone and with Vibrant Parrots, Puzzles Lib and Cozy Core loaded (2 of 2 checks each). Proof run: asked for a mod that isn't there, the loaded check failed as it should. The Fabric client test passed in a real game, alone (7 of 7 checks) and with Vibrant Parrots, Puzzles Lib, Forge Config API Port, Cosy Critters and Cozy Core loaded (11 of 11); Puzzles Lib's newest build needs Fabric API 0.163 or later, so the project now builds against 0.165.0. CI runs both tests alone and with the compat mods; the first run on GitHub passed all five jobs. |
