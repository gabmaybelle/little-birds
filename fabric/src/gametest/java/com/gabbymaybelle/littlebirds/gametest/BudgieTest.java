package com.gabbymaybelle.littlebirds.gametest;

import com.gabbymaybelle.littlebirds.Birds;
import com.gabbymaybelle.littlebirds.Budgie;
import com.gabbymaybelle.littlebirds.Cockatiel;
import com.gabbymaybelle.littlebirds.Perch;
import com.gabbymaybelle.littlebirds.PerchBlock;
import com.gabbymaybelle.littlebirds.Settings;
import com.gabbymaybelle.littlebirds.Sounds;
import java.util.ArrayList;
import java.util.List;
import net.fabricmc.fabric.api.client.gametest.v1.FabricClientGameTest;
import net.fabricmc.fabric.api.client.gametest.v1.context.ClientGameTestContext;
import net.fabricmc.fabric.api.client.gametest.v1.context.TestServerContext;
import net.fabricmc.fabric.api.client.gametest.v1.context.TestSingleplayerContext;
import net.fabricmc.fabric.api.event.player.UseBlockCallback;
import net.fabricmc.loader.api.FabricLoader;
import net.minecraft.client.CameraType;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.core.registries.Registries;
import net.minecraft.resources.ResourceKey;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.util.Mth;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.InteractionResult;
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
import net.minecraft.world.phys.BlockHitResult;
import net.minecraft.world.phys.Vec3;

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
		sounds(ctx);
		try (TestSingleplayerContext world = ctx.worldBuilder().create()) {
			world.getConnection().waitForChunksRender();
			TestServerContext server = world.getServer();
			server.runCommand("gamemode creative @a");
			server.runCommand("time set day");
			server.runCommand("weather clear");
			// Keep the screenshots clean: no command chatter, and the taming advancement granted now rather than popping up later.
			server.runCommand("gamerule sendCommandFeedback false");
			server.runCommand("gamerule announceAdvancements false");
			server.runCommand("gamerule showDeathMessages false");
			server.runCommand("advancement grant @a only minecraft:husbandry/tame_an_animal");
			BlockPos home = server.computeOnServer(s -> player(s).blockPosition());
			spawn(ctx, server, home);
			taming(ctx, server, home);
			perch(ctx, server, home);
			greeting(ctx, server, home);
			shoulder(ctx, server, home);
			cockatiel(ctx, server, home);
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

	/**
	 * The budgie's three sound events each have real files behind them on the client: the sound
	 * manager drops an entry whose file is missing (leaving the event empty), so this picks a
	 * sound from each event and looks its file up in the resources.
	 */
	private void sounds(ClientGameTestContext ctx) {
		for (var sound : List.of(Sounds.BUDGIE_CHATTER, Sounds.BUDGIE_GREETING, Sounds.BUDGIE_SETTLE,
				Sounds.COCKATIEL_CHATTER, Sounds.COCKATIEL_GREETING, Sounds.COCKATIEL_SETTLE)) {
			var id = sound.get().location();
			String found = ctx.computeOnClient(mc -> {
				var event = mc.getSoundManager().getSoundEvent(id);
				if (event == null || event.getWeight() == 0) {
					return "no sounds listed";
				}
				var pick = event.getSound(net.minecraft.util.RandomSource.create());
				return mc.getResourceManager().getResource(pick.getPath()).isPresent() ? "ok " + pick.getPath().getPath() : "missing file " + pick.getPath();
			});
			check(found.startsWith("ok "), "the sound " + id.getPath() + " has files to play (" + found + ")");
		}
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
	 * A tamed budgie makes the perch near it its home and stays there when its owner goes out; at
	 * night it flies to the perch, settles on the bar facing the way the perch faces and sleeps;
	 * in the morning it gets up; and with the perch broken it has no home again.
	 */
	private void perch(ClientGameTestContext ctx, TestServerContext server, BlockPos home) {
		BlockPos perch = home.offset(3, 0, -3);
		Budgie bird = server.computeOnServer(s -> {
			ServerLevel level = s.overworld();
			level.setBlockAndUpdate(perch.below(), Blocks.OAK_PLANKS.defaultBlockState());
			level.setBlockAndUpdate(perch, Perch.BLOCK.get().defaultBlockState().setValue(PerchBlock.FACING, Direction.WEST));
			level.setBlockAndUpdate(perch.above(), Blocks.AIR.defaultBlockState());
			level.setBlockAndUpdate(perch.above(2), Blocks.AIR.defaultBlockState());
			Budgie b = Birds.BUDGIE.get().create(level, EntitySpawnReason.COMMAND);
			b.snapTo(home.getX() + 1.5, home.getY(), home.getZ() + 0.5, 0, 0);
			b.setColour(Budgie.Colour.GREEN);
			b.tame(player(s));
			level.addFreshEntity(b);
			return b;
		});
		check(server.computeOnServer(s -> s.overworld().getBlockState(perch).getBlock() instanceof PerchBlock), "the perch block can be placed");
		check(waitFor(ctx, server, 600, s -> perch.equals(bird.getPerch())), "a tamed budgie makes the perch near it its home");
		// With its owner 30 blocks away the bird stays home; with the owner back it doesn't.
		server.runOnServer(s -> player(s).teleportTo(s.overworld(), home.getX() + 30.5, home.getY(), home.getZ() + 0.5, java.util.Set.of(), 0.0F, 0.0F, false));
		ctx.waitTicks(5);
		check(server.computeOnServer(s -> bird.staysHome()), "with its owner away, the budgie stays home");
		server.runOnServer(s -> player(s).teleportTo(s.overworld(), home.getX() + 0.5, home.getY(), home.getZ() + 0.5, java.util.Set.of(), 0.0F, 0.0F, false));
		ctx.waitTicks(5);
		check(server.computeOnServer(s -> !bird.staysHome()), "with its owner back, the budgie is free to follow again");
		server.runCommand("time set night");
		boolean perched = waitFor(ctx, server, 1200, s -> bird.isPerched());
		check(perched, "at night the budgie goes to sit on its perch");
		if (perched) {
			Vec3 seat = PerchBlock.seat(perch);
			double off = server.computeOnServer(s -> bird.position().distanceTo(seat));
			check(off < 0.3, "the budgie sits on the middle of the bar (off by " + String.format("%.2f", off) + ")");
			float yaw = server.computeOnServer(s -> Mth.wrapDegrees(bird.getYRot() - Direction.WEST.toYRot()));
			check(Math.abs(yaw) < 5.0F, "the budgie faces the way the perch faces (off by " + yaw + " degrees)");
			check(server.computeOnServer(s -> bird.isRoosting()), "at night, on its perch, the budgie is asleep");
			check(server.computeOnServer(s -> bird.getAmbientSound() == null), "asleep, it is quiet");
			// A look at it side on, from two blocks south of the perch, so the sleeping pose shows.
			server.runOnServer(s -> player(s).teleportTo(s.overworld(), perch.getX() + 0.5, perch.getY(), perch.getZ() + 2.5, java.util.Set.of(), 180.0F, 10.0F, false));
			ctx.waitTicks(10);
			ctx.takeScreenshot("budgie_perch");
			server.runOnServer(s -> player(s).teleportTo(s.overworld(), home.getX() + 0.5, home.getY(), home.getZ() + 0.5, java.util.Set.of(), 0.0F, 0.0F, false));
		}
		server.runCommand("time set day");
		check(waitFor(ctx, server, 1600, s -> !bird.isPerched()), "in the morning the budgie gets up");
		server.runOnServer(s -> s.overworld().setBlockAndUpdate(perch, Blocks.AIR.defaultBlockState()));
		check(server.computeOnServer(s -> !bird.hasPerch()), "with the perch broken, the budgie has no home");
		server.runCommand("kill @e[type=little_birds:budgie]");
		server.runOnServer(s -> s.overworld().setBlockAndUpdate(perch.below(), Blocks.AIR.defaultBlockState()));
	}

	/**
	 * The greeting: a sitting budgie says nothing while its owner is away; when the owner comes
	 * back within reach after an hour of the day clock, it says hello once; a quick in-and-out
	 * earns nothing more. Asleep on its perch it is quiet; awake it chatters.
	 */
	private void greeting(ClientGameTestContext ctx, TestServerContext server, BlockPos home) {
		Budgie bird = server.computeOnServer(s -> {
			ServerLevel level = s.overworld();
			Budgie b = Birds.BUDGIE.get().create(level, EntitySpawnReason.COMMAND);
			b.snapTo(home.getX() + 1.5, home.getY(), home.getZ() + 1.5, 0, 0);
			b.tame(player(s));
			b.setOrderedToSit(true);
			level.addFreshEntity(b);
			return b;
		});
		ctx.waitTicks(15);
		check(bird.greetings == 0, "a budgie just tamed has nothing to say yet");
		check(server.computeOnServer(s -> bird.getAmbientSound() == Sounds.BUDGIE_CHATTER.get()), "awake, the budgie chatters");
		teleport(server, home.offset(40, 0, 0));
		server.runCommand("time add 1200");
		ctx.waitTicks(15);
		check(bird.greetings == 0, "while its owner is away it stays quiet");
		teleport(server, home);
		ctx.waitTicks(30);
		check(bird.greetings == 1, "when its owner comes back after an hour away, the budgie says hello (greetings: " + bird.greetings + ")");
		teleport(server, home.offset(40, 0, 0));
		ctx.waitTicks(15);
		teleport(server, home);
		ctx.waitTicks(30);
		check(bird.greetings == 1, "a quick in-and-out earns no second greeting (greetings: " + bird.greetings + ")");
		server.runCommand("kill @e[type=little_birds:budgie]");
	}

	/**
	 * The shoulder: a sneak-use with an empty hand puts a tamed budgie on its owner's shoulder,
	 * where it sits at shoulder height and stays through a fall; a second sneak-use sets it down,
	 * as does a sneak-use on a block (it hops down onto it) or a use of a perch (it sits on the
	 * perch). Using a perch with no bird on the shoulder calls the nearest budgie to it.
	 */
	private void shoulder(ClientGameTestContext ctx, TestServerContext server, BlockPos home) {
		BlockPos perch = home.offset(-3, 0, 3);
		Budgie first = server.computeOnServer(s -> {
			ServerLevel level = s.overworld();
			level.setBlockAndUpdate(perch.below(), Blocks.OAK_PLANKS.defaultBlockState());
			level.setBlockAndUpdate(perch, Perch.BLOCK.get().defaultBlockState().setValue(PerchBlock.FACING, Direction.NORTH));
			level.setBlockAndUpdate(perch.above(), Blocks.AIR.defaultBlockState());
			Budgie b = Birds.BUDGIE.get().create(level, EntitySpawnReason.COMMAND);
			b.snapTo(home.getX() + 1.5, home.getY(), home.getZ() + 0.5, 0, 0);
			b.tame(player(s));
			level.addFreshEntity(b);
			player(s).setItemInHand(InteractionHand.MAIN_HAND, ItemStack.EMPTY);
			player(s).setShiftKeyDown(true);
			return b;
		});
		ctx.waitTicks(5);
		server.runOnServer(s -> first.mobInteract(player(s), InteractionHand.MAIN_HAND));
		ctx.waitTicks(5);
		check(first.isOnShoulder(), "a sneak-use with an empty hand puts the budgie on its owner's shoulder");
		double up = server.computeOnServer(s -> first.getY() - player(s).getY());
		double out = server.computeOnServer(s -> Math.hypot(first.getX() - player(s).getX(), first.getZ() - player(s).getZ()));
		check(up > 1.1 && up < 1.6 && out > 0.2 && out < 0.6, "it sits at shoulder height, out to one side (up " + String.format("%.2f", up) + ", out " + String.format("%.2f", out) + ")");
		// A look at it from the front, in third person (on your own shoulder it isn't drawn in first person).
		ctx.runOnClient(mc -> mc.options.setCameraType(CameraType.THIRD_PERSON_FRONT));
		ctx.waitTicks(10);
		ctx.takeScreenshot("budgie_shoulder");
		ctx.runOnClient(mc -> mc.options.setCameraType(CameraType.FIRST_PERSON));
		teleport(server, home.above(6));
		ctx.waitTicks(40);
		check(first.isOnShoulder(), "it stays on through a fall");
		// Its owner goes to the Nether: the first turns up on the shoulder there, and comes back the same way.
		// (The Nether generates on the way, which can stall the server for seconds, so these wait as long as it takes.)
		server.runCommand("execute in minecraft:the_nether run tp @a 8.5 80.0 8.5");
		java.util.function.Function<MinecraftServer, Budgie> inNether = s -> {
			ServerLevel nether = s.getLevel(net.minecraft.world.level.Level.NETHER);
			List<Budgie> birds = nether.getEntitiesOfClass(Budgie.class, player(s).getBoundingBox().inflate(3.0), b -> b.isOnShoulder());
			return birds.isEmpty() ? null : birds.getFirst();
		};
		check(waitFor(ctx, server, 1200, s -> player(s).level().dimension() == net.minecraft.world.level.Level.NETHER && inNether.apply(s) != null),
				"through a portal to the Nether, the budgie comes along on the shoulder");
		teleport(server, home);
		java.util.function.Function<MinecraftServer, Budgie> inOverworld = s -> {
			List<Budgie> birds = s.overworld().getEntitiesOfClass(Budgie.class, player(s).getBoundingBox().inflate(3.0), b -> b.isOnShoulder());
			return birds.isEmpty() ? null : birds.getFirst();
		};
		check(waitFor(ctx, server, 1200, s -> player(s).level().dimension() == net.minecraft.world.level.Level.OVERWORLD && inOverworld.apply(s) != null),
				"and back to the Overworld with its owner");
		Budgie back = server.computeOnServer(inOverworld::apply);
		Budgie bird = back != null ? back : first;
		server.runOnServer(s -> bird.mobInteract(player(s), InteractionHand.MAIN_HAND));
		ctx.waitTicks(5);
		check(!bird.isOnShoulder(), "a second sneak-use sets it down");
		// Back on, then a sneak-use on the block two steps away: it hops down onto that block.
		BlockPos step = home.offset(2, -1, 0);
		server.runOnServer(s -> bird.mobInteract(player(s), InteractionHand.MAIN_HAND));
		ctx.waitTicks(5);
		InteractionResult result = server.computeOnServer(s -> UseBlockCallback.EVENT.invoker().interact(player(s), s.overworld(), InteractionHand.MAIN_HAND,
				new BlockHitResult(Vec3.atCenterOf(step).add(0, 0.5, 0), Direction.UP, step, false)));
		ctx.waitTicks(5);
		check(result == InteractionResult.SUCCESS && !bird.isOnShoulder() && bird.position().distanceTo(Vec3.atBottomCenterOf(step.above())) < 1.5,
				"a sneak-use on a block sets it down on that block");
		// Back on, then a use of the perch: it sits on the perch and the perch is its home.
		server.runOnServer(s -> bird.mobInteract(player(s), InteractionHand.MAIN_HAND));
		check(waitFor(ctx, server, 100, s -> bird.isOnShoulder()), "it gets back on for the perch check");
		server.runOnServer(s -> player(s).setShiftKeyDown(false));
		usePerch(server, perch);
		ctx.waitTicks(5);
		check(!bird.isOnShoulder() && perch.equals(server.computeOnServer(s -> bird.getPerch())), "using the perch with a bird on the shoulder sets it down there, and the perch is its home");
		check(waitFor(ctx, server, 300, s -> bird.isPerched()), "it settles on the perch");
		server.runCommand("kill @e[type=little_birds:budgie]");
		ctx.waitTicks(5);
		// A fresh bird six blocks away, no home yet: using the perch calls it over.
		Budgie called = server.computeOnServer(s -> {
			ServerLevel level = s.overworld();
			Budgie b = Birds.BUDGIE.get().create(level, EntitySpawnReason.COMMAND);
			b.snapTo(perch.getX() + 6.5, perch.getY(), perch.getZ() + 0.5, 0, 0);
			b.tame(player(s));
			level.addFreshEntity(b);
			return b;
		});
		ctx.waitTicks(5);
		usePerch(server, perch);
		ctx.waitTicks(2);
		check(perch.equals(server.computeOnServer(s -> called.getPerch())), "using the perch calls the nearest budgie: the perch is its home");
		check(waitFor(ctx, server, 600, s -> called.isPerched()), "and it flies over and settles on it");
		server.runCommand("kill @e[type=little_birds:budgie]");
		server.runOnServer(s -> {
			s.overworld().setBlockAndUpdate(perch, Blocks.AIR.defaultBlockState());
			s.overworld().setBlockAndUpdate(perch.below(), Blocks.AIR.defaultBlockState());
		});
	}

	/**
	 * The cockatiel: it is registered with its egg, lives, keeps its colour, is tamed with seeds like
	 * a budgie, greets its owner (the shared bird code), and is listed in savannas; a still one is
	 * posed beside a budgie and a parrot for the size screenshot.
	 */
	private void cockatiel(ClientGameTestContext ctx, TestServerContext server, BlockPos home) {
		check(BuiltInRegistries.ENTITY_TYPE.containsKey(Birds.COCKATIEL_ID), "the cockatiel is registered");
		check(Birds.COCKATIEL_SPAWN_EGG.get() instanceof SpawnEggItem, "the cockatiel spawn egg is registered");
		Cockatiel bird = server.computeOnServer(s -> {
			ServerLevel level = s.overworld();
			Cockatiel b = Birds.COCKATIEL.get().create(level, EntitySpawnReason.COMMAND);
			b.snapTo(home.getX() + 1.5, home.getY(), home.getZ() + 1.5, 0, 0);
			b.setColour(Cockatiel.Colour.LUTINO);
			b.setNoAi(true);
			level.addFreshEntity(b);
			player(s).setItemInHand(InteractionHand.MAIN_HAND, new ItemStack(Items.WHEAT_SEEDS, 64));
			return b;
		});
		ctx.waitTicks(20);
		check(bird.isAlive(), "a cockatiel put in the world is alive");
		double health = server.computeOnServer(s -> bird.getAttributeValue(Attributes.MAX_HEALTH));
		check(health == 6.0, "the cockatiel has 6 health (got " + health + ")");
		check(bird.getColour() == Cockatiel.Colour.LUTINO, "the cockatiel kept its colour (got " + bird.getColour() + ")");
		int tries = 0;
		while (tries < 60 && !bird.isTame()) {
			tries++;
			server.runOnServer(s -> bird.mobInteract(player(s), InteractionHand.MAIN_HAND));
			ctx.waitTicks(1);
		}
		check(bird.isTame(), "seeds tame a cockatiel (after " + tries + " tries)");
		server.runOnServer(s -> player(s).setItemInHand(InteractionHand.MAIN_HAND, ItemStack.EMPTY));
		// The greeting comes from the shared bird code: away an hour, back, one whistle.
		server.runOnServer(s -> {
			bird.setNoAi(false);
			bird.setOrderedToSit(true);
		});
		teleport(server, home.offset(40, 0, 0));
		server.runCommand("time add 1200");
		ctx.waitTicks(15);
		teleport(server, home);
		ctx.waitTicks(30);
		check(bird.greetings == 1, "back after an hour away, the cockatiel whistles hello (greetings: " + bird.greetings + ")");
		int savanna = server.computeOnServer(s -> {
			Biome b = s.registryAccess().lookupOrThrow(Registries.BIOME).getOrThrow(Biomes.SAVANNA).value();
			var entry = b.getAttributes().get(EnvironmentAttributes.NATURAL_MOB_SPAWNS);
			MobSpawnSettings spawns = (MobSpawnSettings) entry.argument();
			return spawns.getMobsToSpawn(MobCategory.CREATURE).unwrap().stream()
					.filter(w -> w.value().type() == Birds.COCKATIEL.get()).mapToInt(w -> w.weight()).sum();
		});
		check(savanna == 8, "savannas spawn cockatiels at weight 8 (got " + savanna + ")");
		// Side on, for size: a still grey cockatiel between a still budgie and a vanilla parrot.
		server.runCommand("kill @e[type=little_birds:cockatiel]");
		ctx.waitTicks(80);
		server.runOnServer(s -> {
			ServerLevel level = s.overworld();
			Cockatiel c = Birds.COCKATIEL.get().create(level, EntitySpawnReason.COMMAND);
			c.snapTo(home.getX() + 3.5, home.getY(), home.getZ() + 0.5, 0, 0);
			c.setColour(Cockatiel.Colour.GREY);
			c.setNoAi(true);
			c.setOnGround(true);
			level.addFreshEntity(c);
			Budgie b = Birds.BUDGIE.get().create(level, EntitySpawnReason.COMMAND);
			b.snapTo(home.getX() + 3.5, home.getY(), home.getZ() - 1.0, 0, 0);
			b.setColour(Budgie.Colour.GREEN);
			b.setNoAi(true);
			b.setOnGround(true);
			level.addFreshEntity(b);
			s.getCommands().performPrefixedCommand(s.createCommandSourceStack().withSuppressedOutput(),
					"summon minecraft:parrot " + (home.getX() + 3.5) + " " + home.getY() + " " + (home.getZ() + 2.0) + " {NoAI:1b}");
			player(s).teleportTo(level, home.getX() + 0.5, home.getY(), home.getZ() + 0.5, java.util.Set.of(), -90.0F, 20.0F, false);
		});
		ctx.waitTicks(10);
		ctx.takeScreenshot("cockatiel");
		server.runCommand("kill @e[type=little_birds:cockatiel]");
		server.runCommand("kill @e[type=little_birds:budgie]");
		server.runCommand("kill @e[type=minecraft:parrot]");
	}

	private static void usePerch(TestServerContext server, BlockPos perch) {
		server.runOnServer(s -> {
			ServerLevel level = s.overworld();
			level.getBlockState(perch).useWithoutItem(level, player(s), new BlockHitResult(Vec3.atCenterOf(perch), Direction.UP, perch, false));
		});
	}

	private static void teleport(TestServerContext server, BlockPos to) {
		server.runOnServer(s -> player(s).teleportTo(s.overworld(), to.getX() + 0.5, to.getY(), to.getZ() + 0.5, java.util.Set.of(), 0.0F, 0.0F, false));
	}

	/** Waits, in steps of ten ticks, until the condition holds on the server or the ticks run out. */
	private static boolean waitFor(ClientGameTestContext ctx, TestServerContext server, int maxTicks, java.util.function.Predicate<MinecraftServer> condition) {
		for (int waited = 0; waited < maxTicks; waited += 10) {
			if (server.computeOnServer(condition::test)) {
				return true;
			}
			ctx.waitTicks(10);
		}
		return server.computeOnServer(condition::test);
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
