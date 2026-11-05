package com.gabbymaybelle.littlebirds;

import java.util.EnumSet;
import java.util.List;
import java.util.UUID;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.sounds.SoundEvent;
import net.minecraft.util.Mth;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.ai.attributes.AttributeSupplier;
import net.minecraft.world.entity.ai.goal.Goal;
import net.minecraft.world.entity.animal.Animal;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.storage.ValueInput;
import net.minecraft.world.level.storage.ValueOutput;
import net.minecraft.world.phys.AABB;
import org.jspecify.annotations.Nullable;

/**
 * A lovebird: a small, round little bird in three colours that lives in a pair. Two lovebirds
 * without partners that meet pair up for good ({@link PairUpGoal}); a pair keeps together
 * ({@link KeepTogetherGoal}), may call the same perch home and sits on it side by side, one
 * either side of the middle of the bar. Everything else it does as a pet is {@link LittleBird}'s.
 */
public class Lovebird extends LittleBird {
	/** The three colours, in the order of {@link #COLOURS}; a wild lovebird picks one at random. */
	public enum Colour {
		/** Peach-faced: green, a peach face, a blue rump. */
		PEACH,
		/** Fischer's: green with an orange face, a yellow breast and a red beak. */
		FISCHER,
		/** The blue peach-faced: blue body, a white face, a grey crown. */
		BLUE;

		public static final Colour[] COLOURS = values();

		public static Colour byId(int id) {
			return COLOURS[Mth.clamp(id, 0, COLOURS.length - 1)];
		}
	}

	/** How far a lone lovebird looks for another, and how far apart a pair drifts before one flies back. */
	public static final double PAIR_REACH = 8.0;
	public static final double TOGETHER_RANGE = 4.0;
	/** How far either side of the bar's middle each of a pair sits. */
	private static final double PAIR_SEAT = 0.2;

	/** The partner, by id; resolved against the world when asked. */
	private @Nullable UUID partnerId;

	public Lovebird(EntityType<? extends Lovebird> type, Level level) {
		super(type, level);
	}

	public static AttributeSupplier.Builder createAttributes() {
		return birdAttributes(5.0);
	}

	@Override
	public int variantCount() {
		return Colour.COLOURS.length;
	}

	public Colour getColour() {
		return Colour.byId(this.getVariant());
	}

	public void setColour(Colour colour) {
		this.setVariant(colour.ordinal());
	}

	@Override
	protected void registerGoals() {
		super.registerGoals();
		this.goalSelector.addGoal(2, new KeepTogetherGoal(this));
		this.goalSelector.addGoal(3, new PairUpGoal(this));
	}

	/** The partner, if it has one and it is here (loaded and alive); a dead or lost partner ends the pair. */
	public @Nullable Lovebird getPartner() {
		if (this.partnerId == null || !(this.level() instanceof ServerLevel level)) {
			return null;
		}
		Entity entity = level.getEntity(this.partnerId);
		if (entity instanceof Lovebird partner && partner.isAlive()) {
			return partner;
		}
		if (entity != null) {
			// Found but no good (dead): the pair is over.
			this.partnerId = null;
		}
		return null;
	}

	public boolean hasPartner() {
		return this.partnerId != null;
	}

	/** Makes two lovebirds a pair, both ways. */
	public void pairWith(Lovebird other) {
		this.partnerId = other.getUUID();
		other.partnerId = this.getUUID();
	}

	/** A pair sits either side of the middle: the one whose id sorts first takes the left. */
	@Override
	public double seatOffset() {
		if (this.partnerId == null) {
			return 0.0;
		}
		return this.getUUID().compareTo(this.partnerId) < 0 ? -PAIR_SEAT : PAIR_SEAT;
	}

	@Override
	public boolean sharesPerchWith(LittleBird other) {
		return this.partnerId != null && this.partnerId.equals(other.getUUID());
	}

	/** Its partner may share its perch. */
	@Override
	public int maxPerchMates() {
		return 1;
	}

	@Override
	protected void addAdditionalSaveData(ValueOutput output) {
		super.addAdditionalSaveData(output);
		if (this.partnerId != null) {
			output.putString("Partner", this.partnerId.toString());
		}
	}

	@Override
	protected void readAdditionalSaveData(ValueInput input) {
		super.readAdditionalSaveData(input);
		String partner = input.getStringOr("Partner", "");
		this.partnerId = partner.isEmpty() ? null : UUID.fromString(partner);
	}

	/** A lovebird breeds only with its partner. */
	@Override
	public boolean canMate(Animal partner) {
		return partner == this.getPartner() && super.canMate(partner);
	}

	@Override
	public SoundEvent chatterSound() {
		return Sounds.LOVEBIRD_CHATTER.get();
	}

	@Override
	public SoundEvent greetingSound() {
		return Sounds.LOVEBIRD_GREETING.get();
	}

	@Override
	public SoundEvent settleSound() {
		return Sounds.LOVEBIRD_SETTLE.get();
	}

	/** A lovebird without a partner looks around every few seconds for another on its own, and they pair for good. */
	static class PairUpGoal extends Goal {
		private final Lovebird bird;
		private @Nullable Lovebird found;

		PairUpGoal(Lovebird bird) {
			this.bird = bird;
		}

		@Override
		public boolean canUse() {
			if (this.bird.hasPartner() || this.bird.isBaby() || this.bird.getRandom().nextInt(reducedTickDelay(60)) != 0) {
				return false;
			}
			// Grown birds only: a chick pairs once it has grown up.
			List<Lovebird> alone = this.bird.level().getEntitiesOfClass(Lovebird.class, this.bird.getBoundingBox().inflate(PAIR_REACH),
					other -> other != this.bird && other.isAlive() && !other.hasPartner() && !other.isBaby());
			this.found = alone.isEmpty() ? null : alone.stream().min((a, b) -> Double.compare(a.distanceToSqr(this.bird), b.distanceToSqr(this.bird))).get();
			return this.found != null;
		}

		@Override
		public boolean canContinueToUse() {
			return false;
		}

		@Override
		public void start() {
			if (this.found != null && !this.found.hasPartner()) {
				this.bird.pairWith(this.found);
			}
		}
	}

	/** A lovebird that has drifted more than a few blocks from its partner flies back to it. */
	static class KeepTogetherGoal extends Goal {
		private final Lovebird bird;
		private @Nullable Lovebird partner;

		KeepTogetherGoal(Lovebird bird) {
			this.bird = bird;
			this.setFlags(EnumSet.of(Flag.MOVE));
		}

		@Override
		public boolean canUse() {
			if (this.bird.isPerched() || this.bird.isOnShoulder() || this.bird.isOrderedToSit()) {
				return false;
			}
			this.partner = this.bird.getPartner();
			return this.partner != null && !this.partner.isOnShoulder() && this.bird.distanceToSqr(this.partner) > TOGETHER_RANGE * TOGETHER_RANGE;
		}

		@Override
		public boolean canContinueToUse() {
			return this.partner != null && this.partner.isAlive() && !this.bird.getNavigation().isDone()
					&& this.bird.distanceToSqr(this.partner) > 2.0 * 2.0;
		}

		@Override
		public void start() {
			if (this.partner != null) {
				this.bird.getNavigation().moveTo(this.partner, 1.0);
			}
		}

		@Override
		public void stop() {
			this.bird.getNavigation().stop();
		}
	}

	/** The partner's box, for tests and goals: null if none here. */
	public @Nullable AABB partnerBox() {
		Lovebird partner = this.getPartner();
		return partner == null ? null : partner.getBoundingBox();
	}
}
