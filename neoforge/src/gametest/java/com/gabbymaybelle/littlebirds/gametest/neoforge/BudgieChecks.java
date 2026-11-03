package com.gabbymaybelle.littlebirds.gametest.neoforge;

import com.gabbymaybelle.littlebirds.Birds;
import com.gabbymaybelle.littlebirds.Budgie;
import net.minecraft.core.BlockPos;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.gametest.framework.GameTestHelper;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.entity.EntitySpawnReason;
import net.minecraft.world.entity.ai.attributes.Attributes;
import net.minecraft.world.item.SpawnEggItem;
import net.neoforged.fml.ModList;

/** The first checks: the mod and its compat mods loaded, and a budgie lives in the world with its attributes and colour. */
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
}
