# Little Birds

Make your cottage cozier with a bird or two.

Minecraft's pets follow you around or sit where you left them. None of them live in the house.
Little Birds adds little pet birds, budgies first and cockatiels, lovebirds and more to follow,
that make a perch their own: they chatter when you come in, nap at dusk, ride on your shoulder
when you ask, and are glad when you come home. A companion for the cottage, not a creature to
collect.

**In progress.** The first release will have the budgie in four colours (tamed with seeds), a
perch it calls home, a chirp when you come back and a shoulder it only rides when asked.

Minecraft 26.3, Fabric and NeoForge. Made by Gabby. The budgie's voice is cut from public-domain
(CC0) recordings of real budgies on Freesound by much_username, tomattka and squashy555.

## Building

```
./gradlew build
```

The jars land in `fabric/build/libs/` and `neoforge/build/libs/`. The in-game tests:

```
./gradlew :fabric:runClientGameTest
./gradlew :neoforge:runGameTestServer
```

The Fabric one opens a real game. Both take `-PcompatMods=<folder>` to load the mods in that folder
too (`node tools/fetch-test-mods.js fabric|neoforge <folder>` fetches the ones the mod is tested with).

## Licence

MIT.
