package com.gabbymaybelle.littlebirds.gametest.neoforge;

import com.gabbymaybelle.littlebirds.Birds;
import java.util.List;
import com.gabbymaybelle.littlebirds.Budgie;
import com.gabbymaybelle.littlebirds.Canary;
import com.gabbymaybelle.littlebirds.Cockatiel;
import com.gabbymaybelle.littlebirds.Foods;
import com.gabbymaybelle.littlebirds.Lovebird;
import com.gabbymaybelle.littlebirds.Perch;
import com.gabbymaybelle.littlebirds.PerchBlock;
import com.gabbymaybelle.littlebirds.Settings;
import com.gabbymaybelle.littlebirds.Sounds;
import com.gabbymaybelle.littlebirds.ZebraFinch;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.core.registries.Registries;
import net.minecraft.gametest.framework.GameTestHelper;
import net.minecraft.resources.ResourceKey;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.util.Mth;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.attribute.EnvironmentAttributes;
import net.minecraft.world.entity.EntitySpawnReason;
import net.minecraft.world.entity.EntityType;
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
import net.minecraft.world.phys.AABB;
import net.minecraft.world.phys.BlockHitResult;
import net.minecraft.world.phys.Vec3;
import net.neoforged.fml.ModList;
import net.neoforged.neoforge.common.NeoForge;
import net.neoforged.neoforge.event.entity.player.PlayerInteractEvent;

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

	/**
	 * At night a tamed budgie claims the perch near it, flies to it, settles on the middle of the
	 * bar facing the perch's way and sleeps; with the perch broken it has no home and gets up.
	 */
	@SuppressWarnings("removal") // makeMockServerPlayerInLevel: a tamed bird needs an owner
	static void roosts(GameTestHelper helper) {
		ServerLevel level = helper.getLevel();
		// The 7x5x7 room: the perch towards one wall, the bird three blocks from it at the same height.
		BlockPos perch = helper.absolutePos(new BlockPos(5, 1, 3));
		helper.setTime(18000);
		level.setBlockAndUpdate(perch.below(), Blocks.OAK_PLANKS.defaultBlockState());
		level.setBlockAndUpdate(perch, Perch.BLOCK.get().defaultBlockState().setValue(PerchBlock.FACING, Direction.WEST));
		level.setBlockAndUpdate(perch.above(), Blocks.AIR.defaultBlockState());
		level.setBlockAndUpdate(perch.above(2), Blocks.AIR.defaultBlockState());
		ServerPlayer player = helper.makeMockServerPlayerInLevel();
		Budgie bird = Birds.BUDGIE.get().create(level, EntitySpawnReason.COMMAND);
		bird.snapTo(perch.getX() - 2.5, perch.getY(), perch.getZ() + 0.5, 0, 0);
		bird.tame(player);
		level.addFreshEntity(bird);
		int[] phase = {0};
		int[] ticks = {0};
		Runnable tidy = () -> {
			bird.discard();
			level.getServer().getPlayerList().remove(player);
			level.setBlockAndUpdate(perch, Blocks.AIR.defaultBlockState());
			level.setBlockAndUpdate(perch.below(), Blocks.AIR.defaultBlockState());
		};
		helper.onEachTick(() -> {
			ticks[0]++;
			if (ticks[0] % 200 == 0) {
				System.out.println("[little_birds_gametest] perch: tick " + ticks[0] + ", phase " + phase[0] + ", home " + bird.getPerch() + ", perched " + bird.isPerched() + ", at " + bird.position());
			}
			switch (phase[0]) {
				case 0 -> {
					if (perch.equals(bird.getPerch())) {
						System.out.println("[little_birds_gametest] perch: claimed after " + ticks[0] + " ticks");
						phase[0] = 1;
					}
				}
				case 1 -> {
					if (bird.isPerched()) {
						double off = bird.position().distanceTo(PerchBlock.seat(perch));
						float yaw = Mth.wrapDegrees(bird.getYRot() - Direction.WEST.toYRot());
						System.out.println("[little_birds_gametest] perch: perched after " + ticks[0] + " ticks, off the seat by " + String.format("%.2f", off) + ", facing off by " + yaw + ", roosting " + bird.isRoosting());
						try {
							helper.assertTrue(off < 0.3, "the budgie should sit on the middle of the bar (off by " + off + ")");
							helper.assertTrue(Math.abs(yaw) < 5.0F, "the budgie should face the way the perch faces (off by " + yaw + ")");
							helper.assertTrue(bird.isRoosting(), "at night on its perch the budgie should be asleep");
						} catch (RuntimeException e) {
							tidy.run();
							throw e;
						}
						level.setBlockAndUpdate(perch, Blocks.AIR.defaultBlockState());
						phase[0] = 2;
					}
				}
				case 2 -> {
					if (!bird.hasPerch() && !bird.isPerched()) {
						System.out.println("[little_birds_gametest] perch: broken, the budgie has no home and is up, after " + ticks[0] + " ticks");
						tidy.run();
						phase[0] = 3;
						helper.succeed();
					}
				}
				default -> {
				}
			}
		});
	}

	/**
	 * The greeting: a sitting budgie says nothing while its owner is away; when the owner comes
	 * back within reach after an hour of the day clock, it says hello once; a quick in-and-out
	 * earns nothing more. Awake it chatters.
	 */
	@SuppressWarnings("removal") // makeMockServerPlayerInLevel: the greeting needs an owner
	static void greets(GameTestHelper helper) {
		ServerLevel level = helper.getLevel();
		BlockPos at = helper.absolutePos(new BlockPos(3, 1, 3));
		BlockPos away = helper.absolutePos(new BlockPos(40, 1, 3));
		ServerPlayer player = helper.makeMockServerPlayerInLevel();
		player.setPos(at.getX() + 0.5, at.getY(), at.getZ() + 0.5);
		Budgie bird = Birds.BUDGIE.get().create(level, EntitySpawnReason.COMMAND);
		bird.snapTo(at.getX() + 1.5, at.getY(), at.getZ() + 0.5, 0, 0);
		bird.tame(player);
		bird.setOrderedToSit(true);
		level.addFreshEntity(bird);
		Runnable tidy = () -> {
			bird.discard();
			level.getServer().getPlayerList().remove(player);
		};
		helper.runAfterDelay(15, () -> {
			try {
				helper.assertTrue(bird.greetings == 0, "a budgie just tamed should have nothing to say yet");
				helper.assertTrue(bird.getAmbientSound() == Sounds.BUDGIE_CHATTER.get(), "awake, the budgie should chatter");
				player.setPos(away.getX() + 0.5, away.getY(), away.getZ() + 0.5);
				helper.setTime(level.getOverworldClockTime() + 1200);
			} catch (RuntimeException e) {
				tidy.run();
				throw e;
			}
		});
		helper.runAfterDelay(30, () -> {
			try {
				helper.assertTrue(bird.greetings == 0, "while its owner is away it should stay quiet");
				player.setPos(at.getX() + 0.5, at.getY(), at.getZ() + 0.5);
			} catch (RuntimeException e) {
				tidy.run();
				throw e;
			}
		});
		helper.runAfterDelay(60, () -> {
			try {
				helper.assertTrue(bird.greetings == 1, "when its owner comes back after an hour away, the budgie should say hello (greetings: " + bird.greetings + ")");
				player.setPos(away.getX() + 0.5, away.getY(), away.getZ() + 0.5);
			} catch (RuntimeException e) {
				tidy.run();
				throw e;
			}
		});
		helper.runAfterDelay(75, () -> player.setPos(at.getX() + 0.5, at.getY(), at.getZ() + 0.5));
		helper.runAfterDelay(105, () -> {
			try {
				System.out.println("[little_birds_gametest] greeting: " + bird.greetings + " greeting(s) after one long absence and one quick in-and-out");
				helper.assertTrue(bird.greetings == 1, "a quick in-and-out should earn no second greeting (greetings: " + bird.greetings + ")");
			} finally {
				tidy.run();
			}
			helper.succeed();
		});
	}

	/**
	 * The shoulder: a sneak-use with an empty hand puts a tamed budgie on its owner's shoulder at
	 * shoulder height, where it stays through a fall; a second sneak-use sets it down, as does a
	 * sneak-use on a block (it hops down onto it) or a use of a perch (it sits on the perch and
	 * makes it home). Using a perch with no bird on the shoulder calls the nearest budgie to it.
	 */
	@SuppressWarnings("removal") // makeMockServerPlayerInLevel: a shoulder needs a player
	static void shoulder(GameTestHelper helper) {
		ServerLevel level = helper.getLevel();
		BlockPos at = helper.absolutePos(new BlockPos(2, 1, 3));
		BlockPos step = helper.absolutePos(new BlockPos(3, 0, 1));
		BlockPos perch = helper.absolutePos(new BlockPos(5, 1, 3));
		level.setBlockAndUpdate(step, Blocks.OAK_PLANKS.defaultBlockState());
		level.setBlockAndUpdate(perch.below(), Blocks.OAK_PLANKS.defaultBlockState());
		level.setBlockAndUpdate(perch, Perch.BLOCK.get().defaultBlockState().setValue(PerchBlock.FACING, Direction.WEST));
		ServerPlayer player = helper.makeMockServerPlayerInLevel();
		player.setPos(at.getX() + 0.5, at.getY(), at.getZ() + 0.5);
		player.setItemInHand(InteractionHand.MAIN_HAND, ItemStack.EMPTY);
		player.setShiftKeyDown(true);
		Budgie bird = Birds.BUDGIE.get().create(level, EntitySpawnReason.COMMAND);
		bird.snapTo(at.getX() + 1.5, at.getY(), at.getZ() + 0.5, 0, 0);
		bird.tame(player);
		level.addFreshEntity(bird);
		Budgie[] called = new Budgie[1];
		Runnable tidy = () -> {
			bird.discard();
			if (called[0] != null) {
				called[0].discard();
			}
			level.getServer().getPlayerList().remove(player);
			level.setBlockAndUpdate(step, Blocks.AIR.defaultBlockState());
			level.setBlockAndUpdate(perch, Blocks.AIR.defaultBlockState());
			level.setBlockAndUpdate(perch.below(), Blocks.AIR.defaultBlockState());
		};
		java.util.function.Consumer<Runnable> step1 = r -> {
			try {
				r.run();
			} catch (RuntimeException e) {
				tidy.run();
				throw e;
			}
		};
		helper.runAfterDelay(5, () -> step1.accept(() -> bird.mobInteract(player, InteractionHand.MAIN_HAND)));
		helper.runAfterDelay(10, () -> step1.accept(() -> {
			helper.assertTrue(bird.isOnShoulder(), "a sneak-use with an empty hand should put the budgie on its owner's shoulder");
			double up = bird.getY() - player.getY();
			double out = Math.hypot(bird.getX() - player.getX(), bird.getZ() - player.getZ());
			System.out.println("[little_birds_gametest] shoulder: riding, up " + String.format("%.2f", up) + ", out " + String.format("%.2f", out));
			helper.assertTrue(up > 1.1 && up < 1.6 && out > 0.2 && out < 0.6, "it should sit at shoulder height, out to one side (up " + up + ", out " + out + ")");
			player.setPos(at.getX() + 0.5, at.getY() + 6, at.getZ() + 0.5);
		}));
		helper.runAfterDelay(50, () -> step1.accept(() -> {
			helper.assertTrue(bird.isOnShoulder(), "it should stay on through a fall");
			bird.mobInteract(player, InteractionHand.MAIN_HAND);
		}));
		helper.runAfterDelay(55, () -> step1.accept(() -> {
			helper.assertFalse(bird.isOnShoulder(), "a second sneak-use should set it down");
			bird.mobInteract(player, InteractionHand.MAIN_HAND);
		}));
		helper.runAfterDelay(60, () -> step1.accept(() -> {
			helper.assertTrue(bird.isOnShoulder(), "it should get back on for the block check");
			PlayerInteractEvent.RightClickBlock event = new PlayerInteractEvent.RightClickBlock(player, InteractionHand.MAIN_HAND, step,
					new BlockHitResult(Vec3.atCenterOf(step).add(0, 0.5, 0), Direction.UP, step, false));
			NeoForge.EVENT_BUS.post(event);
			helper.assertTrue(event.isCanceled(), "a sneak-use on a block should be taken by the mod");
		}));
		helper.runAfterDelay(65, () -> step1.accept(() -> {
			double off = bird.position().distanceTo(Vec3.atBottomCenterOf(step.above()));
			System.out.println("[little_birds_gametest] shoulder: set down " + String.format("%.2f", off) + " from the block");
			helper.assertTrue(!bird.isOnShoulder() && off < 1.5, "a sneak-use on a block should set it down on that block (off by " + off + ")");
			bird.mobInteract(player, InteractionHand.MAIN_HAND);
		}));
		int[] settled = {-1};
		helper.runAfterDelay(70, () -> step1.accept(() -> {
			helper.assertTrue(bird.isOnShoulder(), "it should get back on for the perch check");
			player.setShiftKeyDown(false);
			level.getBlockState(perch).useWithoutItem(level, player, new BlockHitResult(Vec3.atCenterOf(perch), Direction.UP, perch, false));
			helper.assertTrue(!bird.isOnShoulder() && perch.equals(bird.getPerch()), "using the perch with a bird on the shoulder should set it down there, as its home");
			settled[0] = 0;
		}));
		// The per-tick watch is registered now (adding it from inside a later step trips the framework), and waits for the perch step.
		helper.onEachTick(() -> {
			if (settled[0] < 0) {
				return;
			}
			if (settled[0] == 0 && bird.isPerched()) {
				settled[0] = 1;
				System.out.println("[little_birds_gametest] shoulder: settled on the perch");
				bird.discard();
				Budgie b = Birds.BUDGIE.get().create(level, EntitySpawnReason.COMMAND);
				b.snapTo(perch.getX() - 3.5, perch.getY(), perch.getZ() + 0.5, 0, 0);
				b.tame(player);
				level.addFreshEntity(b);
				called[0] = b;
			} else if (settled[0] == 1 && called[0] != null && called[0].tickCount > 5) {
				settled[0] = 2;
				level.getBlockState(perch).useWithoutItem(level, player, new BlockHitResult(Vec3.atCenterOf(perch), Direction.UP, perch, false));
				step1.accept(() -> helper.assertTrue(perch.equals(called[0].getPerch()), "using the perch should call the nearest budgie: the perch becomes its home"));
			} else if (settled[0] == 2 && called[0].isPerched()) {
				settled[0] = 3;
				System.out.println("[little_birds_gametest] shoulder: the called budgie settled on the perch");
				tidy.run();
				helper.succeed();
			}
		});
	}

	/** A cockatiel lives with 6 health, keeps its colour, is tamed with seeds, and the savanna lists it at weight 8. */
	@SuppressWarnings("removal") // makeMockServerPlayerInLevel: taming needs a player
	static void cockatiel(GameTestHelper helper) {
		ServerLevel level = helper.getLevel();
		BlockPos at = helper.absolutePos(new BlockPos(0, 2, 0));
		helper.assertTrue(BuiltInRegistries.ENTITY_TYPE.containsKey(Birds.COCKATIEL_ID), "the cockatiel should be registered");
		helper.assertTrue(Birds.COCKATIEL_SPAWN_EGG.get() instanceof SpawnEggItem, "the cockatiel spawn egg should be registered");
		ServerPlayer player = helper.makeMockServerPlayerInLevel();
		Cockatiel bird = Birds.COCKATIEL.get().create(level, EntitySpawnReason.COMMAND);
		try {
			player.setGameMode(GameType.CREATIVE);
			player.setItemInHand(InteractionHand.MAIN_HAND, new ItemStack(Items.WHEAT_SEEDS, 64));
			bird.snapTo(at.getX() + 0.5, at.getY(), at.getZ() + 0.5, 0, 0);
			bird.setColour(Cockatiel.Colour.WHITEFACE);
			bird.setNoAi(true);
			level.addFreshEntity(bird);
			double health = bird.getAttributeValue(Attributes.MAX_HEALTH);
			helper.assertTrue(health == 6.0, "the cockatiel should have 6 health (got " + health + ")");
			helper.assertTrue(bird.getColour() == Cockatiel.Colour.WHITEFACE, "the cockatiel should keep its colour (got " + bird.getColour() + ")");
			int tries = 0;
			while (tries < 60 && !bird.isTame()) {
				tries++;
				bird.mobInteract(player, InteractionHand.MAIN_HAND);
			}
			int savanna = weightOf(level, Biomes.SAVANNA, Birds.COCKATIEL.get());
			System.out.println("[little_birds_gametest] cockatiel: health " + health + ", tame " + bird.isTame() + " after " + tries + " tries, savanna weight " + savanna);
			helper.assertTrue(bird.isTame(), "seeds should tame a cockatiel (" + tries + " tries)");
			helper.assertTrue(savanna == 8, "savannas should spawn cockatiels at weight 8 (got " + savanna + ")");
		} finally {
			bird.discard();
			level.getServer().getPlayerList().remove(player);
		}
		helper.succeed();
	}

	/** Two lovebirds put down together pair up, both tamed they call one perch home and sit on it either side of the middle; savannas spawn them in twos. */
	@SuppressWarnings("removal") // makeMockServerPlayerInLevel: taming needs a player
	static void lovebirds(GameTestHelper helper) {
		ServerLevel level = helper.getLevel();
		BlockPos perch = helper.absolutePos(new BlockPos(5, 1, 3));
		BlockPos at = helper.absolutePos(new BlockPos(2, 1, 3));
		helper.setTime(18000);
		level.setBlockAndUpdate(perch.below(), Blocks.OAK_PLANKS.defaultBlockState());
		level.setBlockAndUpdate(perch, Perch.BLOCK.get().defaultBlockState().setValue(PerchBlock.FACING, Direction.WEST));
		ServerPlayer player = helper.makeMockServerPlayerInLevel();
		Lovebird a = Birds.LOVEBIRD.get().create(level, EntitySpawnReason.COMMAND);
		Lovebird b = Birds.LOVEBIRD.get().create(level, EntitySpawnReason.COMMAND);
		a.snapTo(at.getX() + 0.5, at.getY(), at.getZ() + 0.5, 0, 0);
		b.snapTo(at.getX() + 1.5, at.getY(), at.getZ() + 0.5, 0, 0);
		a.setColour(Lovebird.Colour.BLUE);
		a.tame(player);
		b.tame(player);
		level.addFreshEntity(a);
		level.addFreshEntity(b);
		int savanna = weightOf(level, Biomes.SAVANNA, Birds.LOVEBIRD.get());
		int[] phase = {0};
		int[] ticks = {0};
		Runnable tidy = () -> {
			a.discard();
			b.discard();
			level.getServer().getPlayerList().remove(player);
			level.setBlockAndUpdate(perch, Blocks.AIR.defaultBlockState());
			level.setBlockAndUpdate(perch.below(), Blocks.AIR.defaultBlockState());
		};
		helper.onEachTick(() -> {
			ticks[0]++;
			if (ticks[0] % 200 == 0) {
				System.out.println("[little_birds_gametest] lovebirds: tick " + ticks[0] + ", phase " + phase[0] + ", paired " + (a.getPartner() == b) + ", perches " + a.getPerch() + " / " + b.getPerch() + ", perched " + a.isPerched() + "/" + b.isPerched());
			}
			switch (phase[0]) {
				case 0 -> {
					if (a.getPartner() == b && b.getPartner() == a) {
						System.out.println("[little_birds_gametest] lovebirds: paired after " + ticks[0] + " ticks");
						phase[0] = 1;
					}
				}
				case 1 -> {
					if (perch.equals(a.getPerch()) && perch.equals(b.getPerch())) {
						System.out.println("[little_birds_gametest] lovebirds: both call the perch home after " + ticks[0] + " ticks");
						phase[0] = 2;
					}
				}
				case 2 -> {
					if (a.isPerched() && b.isPerched()) {
						double apart = a.position().distanceTo(b.position());
						System.out.println("[little_birds_gametest] lovebirds: both perched after " + ticks[0] + " ticks, apart " + String.format("%.2f", apart) + ", savanna weight " + savanna + ", colour " + a.getColour());
						try {
							helper.assertTrue(apart > 0.3 && apart < 0.5, "a pair should sit either side of the middle (apart " + apart + ")");
							helper.assertTrue(a.getColour() == Lovebird.Colour.BLUE, "a lovebird should keep its colour (got " + a.getColour() + ")");
							helper.assertTrue(savanna == 6, "savannas should spawn lovebirds at weight 6 (got " + savanna + ")");
						} finally {
							tidy.run();
						}
						phase[0] = 3;
						helper.succeed();
					}
				}
				default -> {
				}
			}
		});
	}

	/** Two tamed budgies fed seeds fall in love and make a small chick in one parent's colour that belongs to the same owner. */
	@SuppressWarnings("removal") // makeMockServerPlayerInLevel: breeding needs an owner
	static void breeding(GameTestHelper helper) {
		ServerLevel level = helper.getLevel();
		BlockPos at = helper.absolutePos(new BlockPos(3, 1, 3));
		ServerPlayer player = helper.makeMockServerPlayerInLevel();
		player.setPos(at.getX() + 0.5, at.getY(), at.getZ() + 0.5);
		player.setItemInHand(InteractionHand.MAIN_HAND, new ItemStack(Items.WHEAT_SEEDS, 64));
		Budgie a = Birds.BUDGIE.get().create(level, EntitySpawnReason.COMMAND);
		Budgie b = Birds.BUDGIE.get().create(level, EntitySpawnReason.COMMAND);
		a.snapTo(at.getX() + 0.5, at.getY(), at.getZ() + 1.5, 0, 0);
		b.snapTo(at.getX() + 1.5, at.getY(), at.getZ() + 1.5, 0, 0);
		a.setColour(Budgie.Colour.YELLOW);
		b.setColour(Budgie.Colour.WHITE);
		a.tame(player);
		b.tame(player);
		level.addFreshEntity(a);
		level.addFreshEntity(b);
		int[] ticks = {0};
		boolean[] done = {false};
		Budgie[] born = {null};
		// Only this test's birds: tests run side by side, and a wider sweep once took another test's bird.
		Runnable tidy = () -> {
			a.discard();
			b.discard();
			if (born[0] != null) {
				born[0].discard();
			}
			level.getServer().getPlayerList().remove(player);
		};
		helper.runAfterDelay(5, () -> {
			a.mobInteract(player, InteractionHand.MAIN_HAND);
			b.mobInteract(player, InteractionHand.MAIN_HAND);
			try {
				helper.assertTrue(a.isInLove() && b.isInLove(), "seeds should put two tamed budgies in love");
			} catch (RuntimeException e) {
				tidy.run();
				throw e;
			}
		});
		helper.onEachTick(() -> {
			ticks[0]++;
			if (done[0]) {
				return;
			}
			List<Budgie> chicks = level.getEntitiesOfClass(Budgie.class, new AABB(at).inflate(6), c -> c.isBaby() && c.isOwnedBy(player));
			if (!chicks.isEmpty()) {
				done[0] = true;
				Budgie chick = chicks.getFirst();
				born[0] = chick;
				System.out.println("[little_birds_gametest] breeding: chick after " + ticks[0] + " ticks, colour " + chick.getColour() + ", tame " + chick.isTame() + ", scale " + chick.getAgeScale());
				try {
					helper.assertTrue(chick.getColour() == Budgie.Colour.YELLOW || chick.getColour() == Budgie.Colour.WHITE, "the chick should have a parent's colour (got " + chick.getColour() + ")");
					helper.assertTrue(chick.isTame() && chick.isOwnedBy(player), "the chick should belong to its parents' owner");
					helper.assertTrue(chick.getAgeScale() < 1.0F, "the chick should be small");
				} finally {
					tidy.run();
				}
				helper.succeed();
			}
		});
	}

	/** A paired pair of lovebirds fed together makes a chick; a third lovebird fed with one of them makes nothing. */
	@SuppressWarnings("removal") // makeMockServerPlayerInLevel: breeding needs an owner
	static void lovebirdChick(GameTestHelper helper) {
		ServerLevel level = helper.getLevel();
		BlockPos at = helper.absolutePos(new BlockPos(3, 1, 3));
		ServerPlayer player = helper.makeMockServerPlayerInLevel();
		player.setPos(at.getX() + 0.5, at.getY(), at.getZ() + 0.5);
		player.setItemInHand(InteractionHand.MAIN_HAND, new ItemStack(Items.WHEAT_SEEDS, 64));
		Lovebird a = Birds.LOVEBIRD.get().create(level, EntitySpawnReason.COMMAND);
		Lovebird b = Birds.LOVEBIRD.get().create(level, EntitySpawnReason.COMMAND);
		Lovebird c = Birds.LOVEBIRD.get().create(level, EntitySpawnReason.COMMAND);
		a.snapTo(at.getX() + 0.5, at.getY(), at.getZ() + 1.5, 0, 0);
		b.snapTo(at.getX() + 1.5, at.getY(), at.getZ() + 1.5, 0, 0);
		c.snapTo(at.getX() + 2.5, at.getY(), at.getZ() + 1.5, 0, 0);
		for (Lovebird bird : List.of(a, b, c)) {
			bird.tame(player);
			level.addFreshEntity(bird);
		}
		a.pairWith(b);
		Lovebird[] born = {null};
		int[] ticks = {0};
		int[] phase = {0};
		Runnable tidy = () -> {
			a.discard();
			b.discard();
			c.discard();
			if (born[0] != null) {
				born[0].discard();
			}
			level.getServer().getPlayerList().remove(player);
		};
		java.util.function.Supplier<Lovebird> chick = () -> {
			List<Lovebird> chicks = level.getEntitiesOfClass(Lovebird.class, new AABB(at).inflate(6), x -> x.isBaby() && x.isOwnedBy(player));
			return chicks.isEmpty() ? null : chicks.getFirst();
		};
		helper.runAfterDelay(5, () -> {
			a.mobInteract(player, InteractionHand.MAIN_HAND);
			c.mobInteract(player, InteractionHand.MAIN_HAND);
		});
		helper.runAfterDelay(305, () -> {
			try {
				helper.assertTrue(chick.get() == null, "a lovebird fed with a bird that isn't its partner should make no chick");
			} catch (RuntimeException e) {
				tidy.run();
				throw e;
			}
			c.discard();
			phase[0] = 1;
		});
		helper.onEachTick(() -> {
			ticks[0]++;
			if (phase[0] == 1 && !a.isInLove()) {
				a.mobInteract(player, InteractionHand.MAIN_HAND);
				b.mobInteract(player, InteractionHand.MAIN_HAND);
				System.out.println("[little_birds_gametest] lovebird chick: pair fed at tick " + ticks[0] + ", in love " + a.isInLove() + "/" + b.isInLove());
				phase[0] = 2;
			} else if (phase[0] == 2) {
				Lovebird found = chick.get();
				if (found != null) {
					born[0] = found;
					System.out.println("[little_birds_gametest] lovebird chick: born at tick " + ticks[0] + ", colour " + found.getColour());
					tidy.run();
					phase[0] = 3;
					helper.succeed();
				}
			}
		});
	}

	/** Millet spray tames at once and is refused by a full bird; a carrot heals a tamed bird; cocoa beans are refused and kept; a treat hurries a chick along. */
	@SuppressWarnings("removal") // makeMockServerPlayerInLevel: feeding needs a player
	static void eating(GameTestHelper helper) {
		ServerLevel level = helper.getLevel();
		BlockPos at = helper.absolutePos(new BlockPos(0, 2, 0));
		helper.assertTrue(BuiltInRegistries.ITEM.containsKey(Foods.MILLET_SPRAY_ID), "the millet spray should be registered");
		ServerPlayer player = helper.makeMockServerPlayerInLevel();
		Budgie bird = Birds.BUDGIE.get().create(level, EntitySpawnReason.COMMAND);
		Budgie chick = Birds.BUDGIE.get().create(level, EntitySpawnReason.COMMAND);
		try {
			player.setGameMode(GameType.SURVIVAL);
			bird.snapTo(at.getX() + 0.5, at.getY(), at.getZ() + 0.5, 0, 0);
			bird.setNoAi(true);
			level.addFreshEntity(bird);
			player.setItemInHand(InteractionHand.MAIN_HAND, new ItemStack(Foods.MILLET_SPRAY.get(), 2));
			bird.mobInteract(player, InteractionHand.MAIN_HAND);
			helper.assertTrue(bird.isTame() && player.getMainHandItem().getCount() == 1, "millet spray should tame a wild budgie on the first try");
			bird.mobInteract(player, InteractionHand.MAIN_HAND);
			helper.assertTrue(player.getMainHandItem().getCount() == 1 && bird.treats == 1, "a full bird should take no second treat");
			bird.setHealth(3.0F);
			player.setItemInHand(InteractionHand.MAIN_HAND, new ItemStack(Items.CARROT, 2));
			bird.mobInteract(player, InteractionHand.MAIN_HAND);
			helper.assertTrue(bird.getHealth() == 4.0F && player.getMainHandItem().getCount() == 1, "a carrot should heal a tamed budgie a little");
			player.setItemInHand(InteractionHand.MAIN_HAND, new ItemStack(Items.COCOA_BEANS, 2));
			bird.mobInteract(player, InteractionHand.MAIN_HAND);
			helper.assertTrue(bird.refusals == 1 && player.getMainHandItem().getCount() == 2, "cocoa beans should be refused and kept");
			chick.snapTo(at.getX() + 1.5, at.getY(), at.getZ() + 0.5, 0, 0);
			chick.setNoAi(true);
			chick.tame(player);
			chick.setBaby(true);
			level.addFreshEntity(chick);
			int before = chick.getAge();
			player.setItemInHand(InteractionHand.MAIN_HAND, new ItemStack(Foods.MILLET_SPRAY.get(), 1));
			chick.mobInteract(player, InteractionHand.MAIN_HAND);
			System.out.println("[little_birds_gametest] eating: tamed by millet, treats " + bird.treats + ", snacks " + bird.snacks + ", refusals " + bird.refusals + ", chick age " + before + " to " + chick.getAge());
			helper.assertTrue(chick.getAge() > before, "a treat should hurry a chick along");
		} finally {
			bird.discard();
			chick.discard();
			level.getServer().getPlayerList().remove(player);
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
		return weightOf(level, biome, Birds.BUDGIE.get());
	}

	/**
	 * The canary: registered with its egg, 4 health, keeps its colour, seeds tame it, a sneak-use
	 * leaves it on the ground; with the clock set to the next morning (this test runs in a batch
	 * of its own) a tamed one sings once and a millet spray gets a song; flower forests list it at
	 * weight 10 and savannas not at all.
	 */
	@SuppressWarnings("removal") // makeMockServerPlayerInLevel: taming needs a player
	static void canary(GameTestHelper helper) {
		ServerLevel level = helper.getLevel();
		BlockPos at = helper.absolutePos(new BlockPos(2, 1, 3));
		helper.assertTrue(BuiltInRegistries.ENTITY_TYPE.containsKey(Birds.CANARY_ID), "the canary should be registered");
		helper.assertTrue(Birds.CANARY_SPAWN_EGG.get() instanceof SpawnEggItem, "the canary spawn egg should be registered");
		ServerPlayer player = helper.makeMockServerPlayerInLevel();
		Canary bird = Birds.CANARY.get().create(level, EntitySpawnReason.COMMAND);
		Runnable tidy = () -> {
			bird.discard();
			level.getServer().getPlayerList().remove(player);
		};
		int flower;
		int savanna;
		try {
			player.setGameMode(GameType.CREATIVE);
			player.setPos(at.getX() + 0.5, at.getY(), at.getZ() + 0.5);
			player.setItemInHand(InteractionHand.MAIN_HAND, new ItemStack(Items.WHEAT_SEEDS, 64));
			bird.snapTo(at.getX() + 1.5, at.getY(), at.getZ() + 0.5, 0, 0);
			bird.setColour(Canary.Colour.WHITE);
			bird.setNoAi(true);
			level.addFreshEntity(bird);
			double health = bird.getAttributeValue(Attributes.MAX_HEALTH);
			helper.assertTrue(health == 4.0, "the canary should have 4 health (got " + health + ")");
			helper.assertTrue(bird.getColour() == Canary.Colour.WHITE, "the canary should keep its colour (got " + bird.getColour() + ")");
			int tries = 0;
			while (tries < 60 && !bird.isTame()) {
				tries++;
				bird.mobInteract(player, InteractionHand.MAIN_HAND);
			}
			helper.assertTrue(bird.isTame(), "seeds should tame a canary (" + tries + " tries)");
			player.setItemInHand(InteractionHand.MAIN_HAND, ItemStack.EMPTY);
			player.setShiftKeyDown(true);
			bird.mobInteract(player, InteractionHand.MAIN_HAND);
			player.setShiftKeyDown(false);
			helper.assertTrue(!bird.isOnShoulder() && !bird.isOrderedToSit(), "a sneak-use should leave a canary where it is");
			long clock = level.getOverworldClockTime();
			helper.setTime((Math.floorDiv(clock, 24000L) + 1) * 24000L + 1000L);
			clock = level.getOverworldClockTime();
			helper.assertTrue(Canary.isMorning(clock), "the clock should read morning (clock " + clock + ")");
			flower = weightOf(level, Biomes.FLOWER_FOREST, Birds.CANARY.get());
			savanna = weightOf(level, Biomes.SAVANNA, Birds.CANARY.get());
			bird.setNoAi(false);
			bird.setOrderedToSit(true);
		} catch (RuntimeException e) {
			tidy.run();
			throw e;
		}
		int[] ticks = {0};
		boolean[] done = {false};
		helper.onEachTick(() -> {
			ticks[0]++;
			if (done[0]) {
				return;
			}
			if (bird.songs < 1) {
				if (ticks[0] % 100 == 0) {
					System.out.println("[little_birds_gametest] canary: tick " + ticks[0] + ", no song yet, on ground " + bird.onGround());
				}
				return;
			}
			done[0] = true;
			try {
				helper.assertTrue(bird.songs == 1, "a canary should sing once in the morning (songs " + bird.songs + ")");
				player.setItemInHand(InteractionHand.MAIN_HAND, new ItemStack(Foods.MILLET_SPRAY.get(), 1));
				bird.mobInteract(player, InteractionHand.MAIN_HAND);
				System.out.println("[little_birds_gametest] canary: sang after " + ticks[0] + " ticks, songs after a treat " + bird.songs + ", flower forest weight " + flower + ", savanna weight " + savanna);
				helper.assertTrue(bird.songs == 2, "a millet spray should get a song (songs " + bird.songs + ")");
				helper.assertTrue(flower == 10, "flower forests should spawn canaries at weight 10 (got " + flower + ")");
				helper.assertTrue(savanna == 0, "savannas should not spawn canaries (got " + savanna + ")");
			} finally {
				tidy.run();
			}
			helper.succeed();
		});
	}

	/**
	 * Zebra finches: registered with the egg, 3 health, keeps its colour, seeds tame it, a sneak-use
	 * leaves it on the ground; a second finch flies over to keep company; a phrase from one is
	 * answered by the other and the answer by nothing; called to one perch, both sit on it side by
	 * side; plains list it at weight 6 and savannas not at all. Runs by day, in the morning batch.
	 */
	@SuppressWarnings("removal") // makeMockServerPlayerInLevel: taming needs a player
	static void zebraFinches(GameTestHelper helper) {
		ServerLevel level = helper.getLevel();
		BlockPos at = helper.absolutePos(new BlockPos(1, 1, 3));
		BlockPos perch = helper.absolutePos(new BlockPos(5, 1, 3));
		helper.assertTrue(BuiltInRegistries.ENTITY_TYPE.containsKey(Birds.ZEBRA_FINCH_ID), "the zebra finch should be registered");
		helper.assertTrue(Birds.ZEBRA_FINCH_SPAWN_EGG.get() instanceof SpawnEggItem, "the zebra finch spawn egg should be registered");
		level.setBlockAndUpdate(perch.below(), Blocks.OAK_PLANKS.defaultBlockState());
		level.setBlockAndUpdate(perch, Perch.BLOCK.get().defaultBlockState().setValue(PerchBlock.FACING, Direction.WEST));
		ServerPlayer player = helper.makeMockServerPlayerInLevel();
		ZebraFinch a = Birds.ZEBRA_FINCH.get().create(level, EntitySpawnReason.COMMAND);
		ZebraFinch b = Birds.ZEBRA_FINCH.get().create(level, EntitySpawnReason.COMMAND);
		Runnable tidy = () -> {
			a.discard();
			b.discard();
			level.getServer().getPlayerList().remove(player);
			level.setBlockAndUpdate(perch, Blocks.AIR.defaultBlockState());
			level.setBlockAndUpdate(perch.below(), Blocks.AIR.defaultBlockState());
		};
		int plains;
		int savanna;
		try {
			if (level.isDarkOutside()) {
				helper.setTime((Math.floorDiv(level.getOverworldClockTime(), 24000L) + 1) * 24000L + 1000L);
			}
			player.setGameMode(GameType.CREATIVE);
			player.setPos(at.getX() + 0.5, at.getY(), at.getZ() + 0.5);
			player.setItemInHand(InteractionHand.MAIN_HAND, new ItemStack(Items.WHEAT_SEEDS, 64));
			a.snapTo(at.getX() + 0.5, at.getY(), at.getZ() + 0.5, 0, 0);
			a.setColour(ZebraFinch.Colour.FAWN);
			a.setNoAi(true);
			level.addFreshEntity(a);
			double health = a.getAttributeValue(Attributes.MAX_HEALTH);
			helper.assertTrue(health == 3.0, "the zebra finch should have 3 health (got " + health + ")");
			helper.assertTrue(a.getColour() == ZebraFinch.Colour.FAWN, "the zebra finch should keep its colour (got " + a.getColour() + ")");
			int tries = 0;
			while (tries < 60 && !a.isTame()) {
				tries++;
				a.mobInteract(player, InteractionHand.MAIN_HAND);
			}
			helper.assertTrue(a.isTame(), "seeds should tame a zebra finch (" + tries + " tries)");
			player.setItemInHand(InteractionHand.MAIN_HAND, ItemStack.EMPTY);
			player.setShiftKeyDown(true);
			a.mobInteract(player, InteractionHand.MAIN_HAND);
			player.setShiftKeyDown(false);
			helper.assertTrue(!a.isOnShoulder() && !a.isOrderedToSit(), "a sneak-use should leave a zebra finch where it is");
			a.setNoAi(false);
			a.setOrderedToSit(true);
			b.snapTo(at.getX() + 5.5, at.getY(), at.getZ() + 0.5, 0, 0);
			b.tame(player);
			level.addFreshEntity(b);
			plains = weightOf(level, Biomes.PLAINS, Birds.ZEBRA_FINCH.get());
			savanna = weightOf(level, Biomes.SAVANNA, Birds.ZEBRA_FINCH.get());
		} catch (RuntimeException e) {
			tidy.run();
			throw e;
		}
		int[] phase = {0};
		int[] ticks = {0};
		int[] since = {0};
		helper.onEachTick(() -> {
			ticks[0]++;
			if (ticks[0] % 200 == 0) {
				System.out.println("[little_birds_gametest] zebra finches: tick " + ticks[0] + ", phase " + phase[0] + ", apart " + String.format("%.1f", a.distanceTo(b)) + ", songs " + a.songs + "/" + b.songs + ", perched " + a.isPerched() + "/" + b.isPerched());
			}
			switch (phase[0]) {
				case 0 -> {
					if (a.distanceTo(b) < 3.0) {
						System.out.println("[little_birds_gametest] zebra finches: company kept after " + ticks[0] + " ticks");
						b.setOrderedToSit(true);
						a.sing();
						since[0] = ticks[0];
						phase[0] = 1;
					}
				}
				case 1 -> {
					if (b.songs >= 1) {
						System.out.println("[little_birds_gametest] zebra finches: answered after " + (ticks[0] - since[0]) + " ticks");
						since[0] = ticks[0];
						phase[0] = 2;
					} else if (ticks[0] - since[0] > 80) {
						try {
							helper.fail("a finch should answer a phrase it hears within 80 ticks");
						} finally {
							tidy.run();
						}
					}
				}
				case 2 -> {
					if (ticks[0] - since[0] >= 60) {
						try {
							helper.assertTrue(a.songs == 1 && b.songs == 1, "an answer should get no answer (songs " + a.songs + "/" + b.songs + ")");
						} catch (RuntimeException e) {
							tidy.run();
							throw e;
						}
						a.setOrderedToSit(false);
						b.setOrderedToSit(false);
						a.callTo(perch);
						b.callTo(perch);
						phase[0] = 3;
					}
				}
				case 3 -> {
					if (a.isPerched() && b.isPerched()) {
						double apart = a.position().distanceTo(b.position());
						System.out.println("[little_birds_gametest] zebra finches: both perched after " + ticks[0] + " ticks, apart " + String.format("%.2f", apart) + ", plains weight " + plains + ", savanna weight " + savanna);
						try {
							helper.assertTrue(apart > 0.3 && apart < 0.5, "two finches should sit either side of the middle (apart " + apart + ")");
							helper.assertTrue(plains == 6, "plains should spawn zebra finches at weight 6 (got " + plains + ")");
							helper.assertTrue(savanna == 0, "savannas should not spawn zebra finches (got " + savanna + ")");
						} finally {
							tidy.run();
						}
						phase[0] = 4;
						helper.succeed();
					}
				}
				default -> {
				}
			}
		});
	}

	private static int weightOf(ServerLevel level, ResourceKey<Biome> biome, EntityType<?> type) {
		Biome b = level.registryAccess().lookupOrThrow(Registries.BIOME).getOrThrow(biome).value();
		var entry = b.getAttributes().get(EnvironmentAttributes.NATURAL_MOB_SPAWNS);
		if (entry == null) {
			return 0;
		}
		MobSpawnSettings spawns = (MobSpawnSettings) entry.argument();
		return spawns.getMobsToSpawn(MobCategory.CREATURE).unwrap().stream()
				.filter(w -> w.value().type() == type).mapToInt(w -> w.weight()).sum();
	}
}
