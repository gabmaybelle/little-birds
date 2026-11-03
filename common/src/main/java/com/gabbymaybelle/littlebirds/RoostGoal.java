package com.gabbymaybelle.littlebirds;

import java.util.EnumSet;
import net.minecraft.core.BlockPos;
import net.minecraft.world.entity.ai.goal.Goal;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.phys.Vec3;

/**
 * A bird with a perch goes back to it: every night, whenever its owner is away, and now and then
 * in the day for a sit. It flies to the air above the bar, settles onto it, turns to face the
 * way the perch faces, and stays put until morning (or until its owner is back and it has sat a
 * while). The perch gone, or the bird told to sit elsewhere, ends it.
 */
public class RoostGoal extends Goal {
	/** A bird farther than this from its perch doesn't try to fly home; it acts like any other pet until it's near again. */
	public static final double FLY_HOME_RANGE = 32.0;
	/** The chance, each tick, that a bird at home in daylight goes to sit on its perch for a while. */
	private static final int DAYTIME_VISIT_CHANCE = 600;
	/** How long it keeps trying to land before giving up for now. */
	private static final int GIVE_UP_TICKS = 600;

	private final Budgie bird;
	private int dwell;
	private int flying;
	private boolean landed;

	public RoostGoal(Budgie bird) {
		this.bird = bird;
		this.setFlags(EnumSet.of(Flag.MOVE));
	}

	@Override
	public boolean canUse() {
		BlockPos perch = this.bird.getPerch();
		if (perch == null || !this.free() || !this.nearHome(perch)) {
			return false;
		}
		return this.bird.level().isDarkOutside() || this.bird.staysHome() || this.bird.getRandom().nextInt(DAYTIME_VISIT_CHANCE) == 0;
	}

	@Override
	public boolean canContinueToUse() {
		BlockPos perch = this.bird.getPerch();
		if (perch == null || !this.free() || !this.nearHome(perch)) {
			return false;
		}
		if (!this.landed && this.flying > GIVE_UP_TICKS) {
			return false;
		}
		return this.bird.level().isDarkOutside() || this.bird.staysHome() || this.dwell > 0;
	}

	private boolean free() {
		return this.bird.isTame() && !this.bird.isPassenger() && !this.bird.isOrderedToSit();
	}

	private boolean nearHome(BlockPos perch) {
		return perch.distToCenterSqr(this.bird.position()) < FLY_HOME_RANGE * FLY_HOME_RANGE;
	}

	@Override
	public boolean requiresUpdateEveryTick() {
		return true;
	}

	@Override
	public void start() {
		this.dwell = 400 + this.bird.getRandom().nextInt(800);
		this.flying = 0;
		this.landed = false;
		this.flyHome();
	}

	@Override
	public void tick() {
		BlockPos perch = this.bird.getPerch();
		if (perch == null) {
			return;
		}
		Vec3 seat = PerchBlock.seat(perch);
		Vec3 here = this.bird.position();
		double dx = seat.x - here.x, dz = seat.z - here.z, dy = here.y - seat.y;
		double across = dx * dx + dz * dz;
		if (this.landed) {
			if (across > 0.25 || Math.abs(dy) > 0.5) {
				// Pushed off: back up and settle again.
				this.takeOff();
				this.flyHome();
				return;
			}
			// Sitting still on the bar: held there, feet down, whatever nudges it.
			this.bird.getNavigation().stop();
			this.bird.setPos(seat.x, seat.y, seat.z);
			this.bird.setDeltaMovement(Vec3.ZERO);
			this.bird.setOnGround(true);
			if (!this.bird.level().isDarkOutside()) {
				this.dwell--;
			}
			return;
		}
		this.flying++;
		if (across < 1.0 && dy >= -0.1 && dy < 1.5) {
			// Over the bar: leave the path, glide down to the seat, and settle once the feet reach it.
			this.bird.getNavigation().stop();
			this.bird.getMoveControl().setWantedPosition(seat.x, seat.y, seat.z, 0.6);
			if (this.bird.onGround() || dy < 0.12) {
				this.settle(perch);
			}
		} else if (across < 1.0 && dy < -0.1) {
			// Beside or under the bar (on the base, say): a hop up, and the path above the perch takes it from there.
			if (this.bird.onGround()) {
				this.bird.setDeltaMovement(this.bird.getDeltaMovement().add(0.0, 0.4, 0.0));
			}
			if (this.bird.getNavigation().isDone()) {
				this.flyHome();
			}
		} else if (this.bird.getNavigation().isDone() && this.flying % 40 == 0) {
			this.flyHome();
		}
	}

	@Override
	public void stop() {
		this.takeOff();
		this.bird.getNavigation().stop();
	}

	private void flyHome() {
		BlockPos perch = this.bird.getPerch();
		if (perch != null) {
			// Aim for the air a block and a half above the bar, so the bird comes down onto it rather than at it.
			this.bird.getNavigation().moveTo(perch.getX() + 0.5, perch.getY() + 1.5, perch.getZ() + 0.5, 1.0);
		}
	}

	private void settle(BlockPos perch) {
		this.landed = true;
		Vec3 seat = PerchBlock.seat(perch);
		this.bird.setPos(seat.x, seat.y, seat.z);
		this.bird.setOnGround(true);
		BlockState state = this.bird.level().getBlockState(perch);
		if (state.getBlock() instanceof PerchBlock) {
			float yaw = PerchBlock.seatYaw(state);
			this.bird.setYRot(yaw);
			this.bird.setYBodyRot(yaw);
			this.bird.setYHeadRot(yaw);
		}
		this.bird.setDeltaMovement(Vec3.ZERO);
		this.bird.setPerched(true);
		this.bird.setInSittingPose(true);
	}

	private void takeOff() {
		if (this.landed) {
			this.landed = false;
			this.bird.setPerched(false);
			this.bird.setInSittingPose(this.bird.isOrderedToSit());
		}
	}
}
