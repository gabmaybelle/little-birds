# Little Birds

Make your cottage cozier with a bird or two.

Minecraft's pets follow you around or sit where you left them. None of them live in the house.
Little Birds adds little pet birds, budgies first and cockatiels, lovebirds and more to follow,
that make a perch their own: they chatter when you come in, nap at dusk, ride on your shoulder
when you ask, and are glad when you come home. A companion for the cottage, not a creature to
collect.

**Three birds so far:** the budgie in four colours, the cockatiel in three with a crest that
lifts when it whistles hello, and the lovebird in three, which lives in a pair that shares one
perch. All are tamed with seeds. A tamed bird makes a perch its home, where it sits by day and
sleeps at night, and stays when you go out; it chirps and bobs when you come back after a while
away; it rides your shoulder only when asked (sneak-use it), through jumps, falls and portals;
use a perch to call your bird to it. Wild birds live in savannas, budgies on jungle edges too,
with a settings file to turn that off. More birds to come.

Minecraft 26.3, Fabric and NeoForge. Made by Gabby. The birds' voices are cut from public-domain
(CC0) recordings of real birds on Freesound: budgies by much_username, tomattka and squashy555,
a cockatiel by Tairblenn, lovebirds by 5ro4, bowlingballout and xv920.

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
