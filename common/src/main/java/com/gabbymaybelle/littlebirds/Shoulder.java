package com.gabbymaybelle.littlebirds;

import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.level.entity.EntityTypeTest;
import net.minecraft.world.level.Level;
import net.minecraft.world.phys.Vec3;
import org.jspecify.annotations.Nullable;

/**
 * A budgie on a shoulder. Vanilla's shoulder slots only carry a parrot's colour, and nothing may
 * ride a player, so a budgie on a shoulder is a state of its own: the bird is held at its owner's
 * shoulder every tick ({@link LittleBird#isOnShoulder}). It gets on when its owner sneak-uses it with
 * an empty hand, and gets off the same way, or when its owner sneak-uses a block with an empty
 * hand (it hops down onto that block; a perch becomes its seat), or uses a perch. It never gets
 * off on its own for a jump or a fall, and it follows its owner through portals; only its owner
 * going away for good (logging out, dying) sets it down where it is.
 */
public final class Shoulder {
	private Shoulder() {
	}

	/** The budgie on this player's shoulder, if one is. */
	public static @Nullable LittleBird riding(Player player) {
		for (LittleBird bird : player.level().getEntitiesOfClass(LittleBird.class, player.getBoundingBox().inflate(3.0), b -> b.isOnShoulder() && b.isOwnedBy(player))) {
			return bird;
		}
		return null;
	}

	/**
	 * The player has just changed worlds (a portal, the End's exit): the budgie on their shoulder
	 * comes too. This runs from the loader's player-changed-world event, because the bird's own
	 * chunk stops ticking the moment its owner leaves and it would never notice on its own.
	 */
	public static void followAcrossWorlds(ServerPlayer player, ServerLevel origin) {
		if (origin == player.level()) {
			return;
		}
		for (LittleBird bird : origin.getEntities(EntityTypeTest.forClass(LittleBird.class), b -> b.isOnShoulder() && b.isOwnedBy(player))) {
			bird.followThroughPortal(player);
		}
	}

	/**
	 * A sneak-use on a block with an empty hand: the budgie on the player's shoulder hops down onto
	 * it (onto the seat if it's a perch, which becomes its home). True when a bird was set down,
	 * so the loader can stop the use there. Does nothing on the client but say whether it would.
	 */
	public static boolean trySetDown(Player player, Level level, BlockPos pos, Direction face) {
		if (!player.isShiftKeyDown() || !player.getMainHandItem().isEmpty()) {
			return false;
		}
		LittleBird bird = riding(player);
		if (bird == null) {
			return false;
		}
		if (!level.isClientSide()) {
			bird.setOnShoulder(false);
			if (level.getBlockState(pos).getBlock() instanceof PerchBlock) {
				Vec3 seat = PerchBlock.seat(pos);
				bird.snapTo(seat.x, seat.y, seat.z, bird.getYRot(), 0.0F);
				bird.callTo(pos);
			} else {
				BlockPos beside = pos.relative(face);
				bird.snapTo(beside.getX() + 0.5, beside.getY(), beside.getZ() + 0.5, bird.getYRot(), 0.0F);
			}
		}
		return true;
	}
}
