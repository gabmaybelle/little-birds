package com.gabbymaybelle.littlebirds.gametest.neoforge;

import com.gabbymaybelle.littlebirds.Birds;
import com.gabbymaybelle.littlebirds.Budgie;
import com.gabbymaybelle.littlebirds.Settings;
import net.minecraft.core.BlockPos;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.core.registries.Registries;
import net.minecraft.gametest.framework.GameTestHelper;
import net.minecraft.resources.ResourceKey;
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
import net.minecraft.world.level.GameType;
import net.minecraft.world.level.biome.Biome;
import net.minecraft.world.level.biome.Biomes;
import net.minecraft.world.level.biome.MobSpawnSettings;
import net.minecraft.world.level.block.Blocks;
import net.neoforged.fml.ModList;

/**
 * The checks: the mod and its compat mods loaded; a budgie lives in the world with its attributes
 * and colour; seeds tame it and its owner sits it down; and wild budgies spawn where and only when
 * they should, with the biome modifier's weights.
 */
final class BudgieChecks {
	private BudgieChecks() {
	}

	/** The compat mods asked for (-PexpectMods) are loaded, and the budgie and its egg are registered. */
	static void loaded(GameTestHelper helper) {
		String expected = System.getProperty("little_birds.expectMods", "");
		for (String id : expected.split(",")) {
			if (!id.isBlank()) {
				helper.assertTrue(ModList.get().isLoaded(id.trim()), "the compat mod " + id.trim() + " should be loaded");
			}
		}
		helper.assertTrue(BuiltInRegistries.ENTITY_TYPE.containsKey(Birds.BUDGIE_ID), "the budgie should be registered");
		helper.assertTrue(Birds.BUDGIE_SPAWN_EGG.get() instanceof SpawnEggItem, "the budgie spawn egg should be registered");
		System.out.println("[little_birds_gametest] loaded: budgie and egg registered; compat mods " + (expected.isBlank() ? "none" : expected));
		helper.succeed();
	}

	/** A budgie put in the world is alive 40 ticks later, has 5 health, keeps its colour, and starts wild. */
	static void lives(GameTestHelper helper) {
		ServerLevel level = helper.getLevel();
		BlockPos at = helper.absolutePos(new BlockPos(0, 2, 0));
		Budgie bird = Birds.BUDGIE.get().create(level, EntitySpawnReason.COMMAND);
		helper.assertTrue(bird != null, "a budgie should be creatable");
		bird.snapTo(at.getX() + 0.5, at.getY(), at.getZ() + 0.5, 0, 0);
		bird.setColour(Budgie.Colour.YELLOW);
		level.addFreshEntity(bird);
		helper.runAfterDelay(40, () -> {
			try {
				double health = bird.getAttributeValue(Attributes.MAX_HEALTH);
				System.out.println("[little_birds_gametest] budgie: alive " + bird.isAlive() + ", health " + health + ", colour " + bird.getColour() + ", tame " + bird.isTame());
				helper.assertTrue(bird.isAlive(), "the budgie should be alive after 40 ticks");
				helper.assertTrue(health == 5.0, "the budgie should have 5 health (got " + health + ")");
				helper.assertTrue(bird.getColour() == Budgie.Colour.YELLOW, "the budgie should keep its colour (got " + bird.getColour() + ")");
				helper.assertFalse(bird.isTame(), "a fresh budgie should be wild");
			} finally {
				bird.discard();
			}
			helper.succeed();
		});
	}

	/** Seeds tame a wild budgie within a few tries, it belongs to the feeder, and a use on the ground sits it down and stands it up. */
	@SuppressWarnings("removal") // makeMockServerPlayerInLevel: taming needs a player
	static void tames(GameTestHelper helper) {
		ServerLevel level = helper.getLevel();
		BlockPos at = helper.absolutePos(new BlockPos(0, 2, 0));
		ServerPlayer player = helper.makeMockServerPlayerInLevel();
		Budgie bird = Birds.BUDGIE.get().create(level, EntitySpawnReason.COMMAND);
		Runnable tidy = () -> {
			bird.discard();
			level.getServer().getPlayerList().remove(player);
		};
		try {
			player.setGameMode(GameType.CREATIVE);
			player.setItemInHand(InteractionHand.MAIN_HAND, new ItemStack(Items.WHEAT_SEEDS, 64));
			bird.snapTo(at.getX() + 0.5, at.getY(), at.getZ() + 0.5, 0, 0);
			bird.setNoAi(true);
			level.addFreshEntity(bird);
			int tries = 0;
			while (tries < 60 && !bird.isTame()) {
				tries++;
				bird.mobInteract(player, InteractionHand.MAIN_HAND);
			}
			System.out.println("[little_birds_gametest] taming: tame " + bird.isTame() + " after " + tries + " tries, owner ok " + bird.isOwnedBy(player));
			helper.assertTrue(bird.isTame(), "seeds should tame a budgie (" + tries + " tries)");
			helper.assertTrue(bird.isOwnedBy(player), "the tamed budgie should belong to the player who fed it");
			player.setItemInHand(InteractionHand.MAIN_HAND, ItemStack.EMPTY);
			bird.setOnGround(true);
			bird.mobInteract(player, InteractionHand.MAIN_HAND);
			helper.assertTrue(bird.isOrderedToSit(), "using a tamed budgie on the ground should sit it down");
			bird.mobInteract(player, InteractionHand.MAIN_HAND);
			helper.assertFalse(bird.isOrderedToSit(), "using it again should stand it up");
		} finally {
			tidy.run();
		}
		helper.succeed();
	}

	/** Wild budgies may appear on grass in daylight, not on stone, and never while natural spawning is off or the chance is 0. */
	static void spawnRules(GameTestHelper helper) {
		ServerLevel level = helper.getLevel();
		BlockPos grass = helper.absolutePos(new BlockPos(0, 2, 0));
		BlockPos stone = helper.absolutePos(new BlockPos(0, 2, 2));
		level.setBlockAndUpdate(grass.below(), Blocks.GRASS_BLOCK.defaultBlockState());
		level.setBlockAndUpdate(stone.below(), Blocks.STONE.defaultBlockState());
		try {
			helper.assertTrue(rule(level, grass), "a wild budgie should be allowed on grass in daylight");
			helper.assertFalse(rule(level, stone), "a wild budgie should not be allowed on stone");
			Settings.naturalSpawning = false;
			boolean off = rule(level, grass);
			Settings.naturalSpawning = true;
			Settings.spawnChance = 0;
			boolean never = rule(level, grass);
			Settings.spawnChance = 100;
			System.out.println("[little_birds_gametest] spawn rules: grass ok, stone no, spawning off " + off + ", chance 0 " + never);
			helper.assertFalse(off, "with natural_spawning off, no wild budgie should spawn");
			helper.assertFalse(never, "with spawn_chance 0, no wild budgie should spawn");
			helper.assertTrue(rule(level, grass), "with the settings back, a wild budgie should be allowed again");
		} finally {
			Settings.naturalSpawning = true;
			Settings.spawnChance = 100;
			level.setBlockAndUpdate(grass.below(), Blocks.AIR.defaultBlockState());
			level.setBlockAndUpdate(stone.below(), Blocks.AIR.defaultBlockState());
		}
		helper.succeed();
	}

	private static boolean rule(ServerLevel level, BlockPos at) {
		return SpawnPlacements.checkSpawnRules(Birds.BUDGIE.get(), level, EntitySpawnReason.NATURAL, at, level.getRandom());
	}

	/** The biome modifier files give the savanna weight 15 and the jungles weight 10, and leave the plains alone. */
	static void biomes(GameTestHelper helper) {
		ServerLevel level = helper.getLevel();
		int savanna = weight(level, Biomes.SAVANNA), jungle = weight(level, Biomes.JUNGLE), sparse = weight(level, Biomes.SPARSE_JUNGLE), plains = weight(level, Biomes.PLAINS);
		System.out.println("[little_birds_gametest] biome weights: savanna " + savanna + ", jungle " + jungle + ", sparse jungle " + sparse + ", plains " + plains);
		helper.assertTrue(savanna == 15, "savannas should spawn budgies at weight 15 (got " + savanna + ")");
		helper.assertTrue(jungle == 10, "jungles should spawn budgies at weight 10 (got " + jungle + ")");
		helper.assertTrue(sparse == 10, "sparse jungles should spawn budgies too (got " + sparse + ")");
		helper.assertTrue(plains == 0, "plains should not spawn budgies (got " + plains + ")");
		helper.succeed();
	}

	private static int weight(ServerLevel level, ResourceKey<Biome> biome) {
		Biome b = level.registryAccess().lookupOrThrow(Registries.BIOME).getOrThrow(biome).value();
		var entry = b.getAttributes().get(EnvironmentAttributes.NATURAL_MOB_SPAWNS);
		if (entry == null) {
			return 0;
		}
		MobSpawnSettings spawns = (MobSpawnSettings) entry.argument();
		return spawns.getMobsToSpawn(MobCategory.CREATURE).unwrap().stream()
				.filter(w -> w.value().type() == Birds.BUDGIE.get()).mapToInt(w -> w.weight()).sum();
	}
}
