package com.gabbymaybelle.littlebirds.gametest;

import com.gabbymaybelle.littlebirds.Birds;
import com.gabbymaybelle.littlebirds.Budgie;
import java.util.ArrayList;
import java.util.List;
import net.fabricmc.fabric.api.client.gametest.v1.FabricClientGameTest;
import net.fabricmc.fabric.api.client.gametest.v1.context.ClientGameTestContext;
import net.fabricmc.fabric.api.client.gametest.v1.context.TestServerContext;
import net.fabricmc.fabric.api.client.gametest.v1.context.TestSingleplayerContext;
import net.fabricmc.loader.api.FabricLoader;
import net.minecraft.core.BlockPos;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.entity.EntitySpawnReason;
import net.minecraft.world.entity.ai.attributes.Attributes;
import net.minecraft.world.item.SpawnEggItem;
import net.minecraft.world.phys.AABB;

/**
 * The first in-game check: the mod loads with the mods players are likely to run alongside, a
 * budgie can be put in the world, has its attributes, keeps its colour, and is drawn. Each later
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
		ctx.takeScreenshot("budgie");
		server.runCommand("kill @e[type=little_birds:budgie]");
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
