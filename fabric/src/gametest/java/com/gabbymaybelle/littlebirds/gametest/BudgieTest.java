package com.gabbymaybelle.littlebirds.gametest;

import com.gabbymaybelle.littlebirds.Birds;
import com.gabbymaybelle.littlebirds.Budgie;
import com.gabbymaybelle.littlebirds.Settings;
import java.util.ArrayList;
import java.util.List;
import net.fabricmc.fabric.api.client.gametest.v1.FabricClientGameTest;
import net.fabricmc.fabric.api.client.gametest.v1.context.ClientGameTestContext;
import net.fabricmc.fabric.api.client.gametest.v1.context.TestServerContext;
import net.fabricmc.fabric.api.client.gametest.v1.context.TestSingleplayerContext;
import net.fabricmc.loader.api.FabricLoader;
import net.minecraft.core.BlockPos;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.core.registries.Registries;
import net.minecraft.resources.ResourceKey;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.attribute.EnvironmentAttributes;
import net.minecraft.world.entity.EntitySpawnReason;
import net.minecraft.world.entity.MobCategory;
import net.minecraft.world.entity.SpawnPlacements;
import net.minecraft.world.entity.ai.attributes.Attributes;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.item.SpawnEggItem;
import net.minecraft.world.level.biome.Biome;
import net.minecraft.world.level.biome.Biomes;
import net.minecraft.world.level.biome.MobSpawnSettings;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.phys.AABB;

/**
 * The in-game checks: the mod loads with the mods players are likely to run alongside; a budgie
 * can be put in the world, has its attributes, keeps its colour and is drawn; seeds tame it and
 * its owner can sit it down; and wild budgies spawn where and only when they should. Each later
 * feature gets its own check here (the perch, the greeting, the shoulder).
 */
public class BudgieTest implements FabricClientGameTest {
	private final List<String> failures = new ArrayList<>();

	@Override
	public void runTest(ClientGameTestContext ctx) {
		loaded();
		try (TestSingleplayerContext world = ctx.worldBuilder().create()) {
			world.getConnection().waitForChunksRender();
			TestServerContext server = world.getServer();
			server.runCommand("gamemode creative @a");
			server.runCommand("time set day");
			server.runCommand("weather clear");
			BlockPos home = server.computeOnServer(s -> player(s).blockPosition());
			spawn(ctx, server, home);
			taming(ctx, server, home);
			spawnRules(server, home.offset(6, 0, 6));
			biomes(server);
		}
		if (!failures.isEmpty()) {
			throw new AssertionError(failures.size() + " check(s) failed:\n - " + String.join("\n - ", failures));
		}
	}

	/** The compat mods asked for (-PexpectMods, comma-separated ids) are really loaded, so a run without them can't pass for them. */
	private void loaded() {
		String expected = System.getProperty("little_birds.expectMods", "");
		for (String id : expected.split(",")) {
			if (!id.isBlank()) {
				check(FabricLoader.getInstance().isModLoaded(id.trim()), "the compat mod " + id.trim() + " is loaded");
			}
		}
		check(BuiltInRegistries.ENTITY_TYPE.containsKey(Birds.BUDGIE_ID), "the budgie is registered");
		check(Birds.BUDGIE_SPAWN_EGG.get() instanceof SpawnEggItem, "the budgie spawn egg is registered");
	}

	/** A budgie put down near the player is alive 40 ticks later, has the budgie's health, and keeps the colour it was given. */
	private void spawn(ClientGameTestContext ctx, TestServerContext server, BlockPos home) {
		server.runOnServer(s -> {
			ServerLevel level = s.overworld();
			Budgie bird = Birds.BUDGIE.get().create(level, EntitySpawnReason.COMMAND);
			bird.snapTo(home.getX() + 2.5, home.getY() + 1, home.getZ() + 0.5, 0, 0);
			bird.setColour(Budgie.Colour.BLUE);
			level.addFreshEntity(bird);
		});
		ctx.waitTicks(40);
		List<Budgie> birds = server.computeOnServer(s -> s.overworld().getEntitiesOfClass(Budgie.class, new AABB(home).inflate(16)));
		check(birds.size() == 1, "one budgie is in the world after 40 ticks (found " + birds.size() + ")");
		if (!birds.isEmpty()) {
			Budgie bird = birds.getFirst();
			check(bird.isAlive(), "the budgie is alive");
			double health = server.computeOnServer(s -> bird.getAttributeValue(Attributes.MAX_HEALTH));
			check(health == 5.0, "the budgie has 5 health (got " + health + ")");
			check(bird.getColour() == Budgie.Colour.BLUE, "the budgie kept its colour (got " + bird.getColour() + ")");
			check(!bird.isTame(), "a fresh budgie is wild");
		}
		// Look at the bird from a couple of blocks away for the screenshot, hotbar and all.
		server.runOnServer(s -> {
			ServerPlayer p = player(s);
			p.teleportTo(s.overworld(), home.getX() + 0.5, home.getY(), home.getZ() + 0.5, java.util.Set.of(), -90.0F, 20.0F, false);
		});
		// A vanilla parrot beside it, for scale.
		// A fresh, still budgie three blocks out, side on to the camera so the whole bird shows, with
		// a vanilla parrot beside it for scale; the first bird has wandered by now.
		server.runCommand("kill @e[type=little_birds:budgie]");
		ctx.waitTicks(80);
		server.runOnServer(s -> {
			ServerLevel level = s.overworld();
			Budgie b = Birds.BUDGIE.get().create(level, EntitySpawnReason.COMMAND);
			b.snapTo(home.getX() + 3.5, home.getY(), home.getZ() + 0.5, 0, 0);
			b.setColour(Budgie.Colour.GREEN);
			b.setNoAi(true);
			b.setOnGround(true);
			b.setYBodyRot(0.0F);
			b.setYHeadRot(0.0F);
			level.addFreshEntity(b);
			s.getCommands().performPrefixedCommand(s.createCommandSourceStack().withSuppressedOutput(),
					"summon minecraft:parrot " + (home.getX() + 3.5) + " " + home.getY() + " " + (home.getZ() + 2.0) + " {NoAI:1b}");
		});
		ctx.waitTicks(10);
		ctx.takeScreenshot("budgie");
		server.runCommand("kill @e[type=little_birds:budgie]");
		server.runCommand("kill @e[type=minecraft:parrot]");
	}

	/**
	 * Seeds tame a wild budgie (one try in three, so a handful of tries), a tamed one belongs to
	 * the player who fed it, and using it again on the ground sits it down and stands it up.
	 */
	private void taming(ClientGameTestContext ctx, TestServerContext server, BlockPos home) {
		Budgie bird = server.computeOnServer(s -> {
			ServerLevel level = s.overworld();
			Budgie b = Birds.BUDGIE.get().create(level, EntitySpawnReason.COMMAND);
			b.snapTo(home.getX() + 1.5, home.getY(), home.getZ() + 1.5, 0, 0);
			b.setNoAi(true);
			level.addFreshEntity(b);
			player(s).setItemInHand(InteractionHand.MAIN_HAND, new ItemStack(Items.WHEAT_SEEDS, 64));
			return b;
		});
		ctx.waitTicks(5);
		int tries = 0;
		while (tries < 60 && !bird.isTame()) {
			tries++;
			server.runOnServer(s -> bird.mobInteract(player(s), InteractionHand.MAIN_HAND));
			ctx.waitTicks(1);
		}
		int fed = tries;
		check(bird.isTame(), "seeds tame a budgie (after " + fed + " tries)");
		check(server.computeOnServer(s -> bird.isOwnedBy(player(s))), "the tamed budgie belongs to the player who fed it");
		// On the ground (no AI, so it stays put; without AI it never lands by itself, so it is set down), a use sits it down and the next stands it up.
		boolean onGround = server.computeOnServer(s -> {
			bird.setOnGround(true);
			return !bird.isFlying();
		});
		check(onGround, "the budgie is on the ground for the sit check");
		server.runOnServer(s -> player(s).setItemInHand(InteractionHand.MAIN_HAND, ItemStack.EMPTY));
		server.runOnServer(s -> bird.mobInteract(player(s), InteractionHand.MAIN_HAND));
		ctx.waitTicks(2);
		check(bird.isOrderedToSit(), "using a tamed budgie sits it down");
		server.runOnServer(s -> bird.mobInteract(player(s), InteractionHand.MAIN_HAND));
		ctx.waitTicks(2);
		check(!bird.isOrderedToSit(), "using it again stands it up");
		server.runCommand("kill @e[type=little_birds:budgie]");
	}

	/**
	 * Wild budgies may appear on grass in daylight, not on stone, and never while natural spawning
	 * is off or the chance is 0. Spawn eggs don't go through this rule.
	 */
	private void spawnRules(TestServerContext server, BlockPos at) {
		server.runOnServer(s -> {
			ServerLevel level = s.overworld();
			level.setBlockAndUpdate(at.below(), Blocks.GRASS_BLOCK.defaultBlockState());
			level.setBlockAndUpdate(at, Blocks.AIR.defaultBlockState());
			level.setBlockAndUpdate(at.above(), Blocks.AIR.defaultBlockState());
			level.setBlockAndUpdate(at.offset(2, -1, 0), Blocks.STONE.defaultBlockState());
			level.setBlockAndUpdate(at.offset(2, 0, 0), Blocks.AIR.defaultBlockState());
		});
		check(rule(server, at), "a wild budgie may spawn on grass in daylight");
		check(!rule(server, at.offset(2, 0, 0)), "a wild budgie may not spawn on stone");
		Settings.naturalSpawning = false;
		check(!rule(server, at), "with natural_spawning off, no wild budgie spawns");
		Settings.naturalSpawning = true;
		Settings.spawnChance = 0;
		check(!rule(server, at), "with spawn_chance 0, no wild budgie spawns");
		Settings.spawnChance = 100;
		check(rule(server, at), "with the settings back, a wild budgie may spawn again");
	}

	private static boolean rule(TestServerContext server, BlockPos at) {
		return server.computeOnServer(s -> SpawnPlacements.checkSpawnRules(Birds.BUDGIE.get(), s.overworld(), EntitySpawnReason.NATURAL, at, s.overworld().getRandom()));
	}

	/** The savanna and the jungle list the budgie among their creatures, with the mod's weights; the plains don't. */
	private void biomes(TestServerContext server) {
		check(weight(server, Biomes.SAVANNA) == 15, "savannas spawn budgies at weight 15 (got " + weight(server, Biomes.SAVANNA) + ")");
		check(weight(server, Biomes.JUNGLE) == 10, "jungles spawn budgies at weight 10 (got " + weight(server, Biomes.JUNGLE) + ")");
		check(weight(server, Biomes.SPARSE_JUNGLE) == 10, "sparse jungles spawn budgies too");
		check(weight(server, Biomes.PLAINS) == 0, "plains don't spawn budgies");
	}

	/** The budgie's spawn weight in a biome, 0 if it isn't listed. */
	private static int weight(TestServerContext server, ResourceKey<Biome> biome) {
		return server.computeOnServer(s -> {
			Biome b = s.registryAccess().lookupOrThrow(Registries.BIOME).getOrThrow(biome).value();
			var entry = b.getAttributes().get(EnvironmentAttributes.NATURAL_MOB_SPAWNS);
			if (entry == null) {
				return 0;
			}
			MobSpawnSettings spawns = (MobSpawnSettings) entry.argument();
			return spawns.getMobsToSpawn(MobCategory.CREATURE).unwrap().stream()
					.filter(w -> w.value().type() == Birds.BUDGIE.get()).mapToInt(w -> w.weight()).sum();
		});
	}

	private static ServerPlayer player(MinecraftServer server) {
		return server.getPlayerList().getPlayers().getFirst();
	}

	private void check(boolean ok, String what) {
		System.out.println("[little_birds_gametest] " + (ok ? "ok   " : "FAIL ") + what);
		if (!ok) {
			failures.add(what);
		}
	}
}
