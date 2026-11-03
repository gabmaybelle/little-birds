package com.gabbymaybelle.littlebirds;

import net.minecraft.core.BlockPos;
import net.minecraft.world.entity.ai.goal.Goal;
import net.minecraft.world.phys.AABB;
import org.jspecify.annotations.Nullable;

/**
 * A tamed bird without a home looks around now and then for a perch to make its own: the nearest
 * one within reach that no other budgie has claimed. It takes nothing from the other goals (no
 * flags), so it runs alongside whatever the bird is doing.
 */
public class FindPerchGoal extends Goal {
	/** How far, in blocks, a bird looks for a perch. */
	public static final int REACH = 8;
	private static final int REACH_UP = 4;

	private final Budgie bird;
	private @Nullable BlockPos found;

	public FindPerchGoal(Budgie bird) {
		this.bird = bird;
	}

	@Override
	public boolean canUse() {
		if (!this.bird.isTame() || this.bird.hasPerch() || this.bird.getRandom().nextInt(reducedTickDelay(100)) != 0) {
			return false;
		}
		this.found = this.nearestFreePerch();
		return this.found != null;
	}

	@Override
	public boolean canContinueToUse() {
		return false;
	}

	@Override
	public void start() {
		if (this.found != null) {
			this.bird.setPerch(this.found);
		}
	}

	private @Nullable BlockPos nearestFreePerch() {
		BlockPos at = this.bird.blockPosition();
		BlockPos nearest = null;
		double best = Double.MAX_VALUE;
		for (BlockPos pos : BlockPos.betweenClosed(at.offset(-REACH, -REACH_UP, -REACH), at.offset(REACH, REACH_UP, REACH))) {
			if (this.bird.level().getBlockState(pos).is(Perch.BLOCK.get())) {
				double distance = pos.distToCenterSqr(this.bird.position());
				if (distance < best && !this.claimedByAnother(pos)) {
					best = distance;
					nearest = pos.immutable();
				}
			}
		}
		return nearest;
	}

	/** Lovebirds will share a perch one day; for now one budgie, one perch. */
	private boolean claimedByAnother(BlockPos perch) {
		AABB around = new AABB(perch).inflate(FindPerchGoal.REACH * 2);
		return !this.bird.level().getEntitiesOfClass(Budgie.class, around, other -> other != this.bird && perch.equals(other.getPerch())).isEmpty();
	}
}
