# Little Birds

Make your cottage cozier with a bird or two.

Minecraft's pets follow you around or sit where you left them. None of them live in the house.
Little Birds adds little pet birds, budgies first and cockatiels, lovebirds and more to follow,
that make a perch their own: they chatter when you come in, nap at dusk, ride on your shoulder
when you ask, and are glad when you come home. A companion for the cottage, not a creature to
collect.

**What's in 0.1.0:** the budgie in four colours, tamed with seeds; a perch it makes its home,
where it sits by day and sleeps at night, and stays when you go out; a chirp and a bob when you
come back after a while away; a shoulder it rides only when asked (sneak-use it), through jumps,
falls and portals; use a perch to call your bird to it. Wild budgies live in savannas and on
jungle edges, with a settings file to turn that off. More birds to come.

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
