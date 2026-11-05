package com.gabbymaybelle.littlebirds;

import java.util.EnumSet;
import java.util.List;
import net.minecraft.core.BlockPos;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.sounds.SoundEvent;
import net.minecraft.util.Mth;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.ai.attributes.AttributeSupplier;
import net.minecraft.world.entity.ai.goal.Goal;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.storage.ValueInput;
import net.minecraft.world.level.storage.ValueOutput;
import org.jspecify.annotations.Nullable;

/**
 * A zebra finch: the smallest little bird, with a red beak, in three colours, that likes
 * company. A finch keeps near the nearest other finch ({@link KeepCompanyGoal}), two may share
 * one perch and sit on it side by side, and by day a tamed finch sings a short phrase now and
 * then; a finch that hears another sing answers it a moment later, so two or three keep up a
 * little back-and-forth. A millet spray gets a phrase too. It keeps to its perch and never rides
 * a shoulder. Everything else it does as a pet is {@link LittleBird}'s.
 */
public class ZebraFinch extends LittleBird {
	/** The three colours, in the order of {@link #COLOURS}; a wild finch picks one at random. */
	public enum Colour {
		/** The wild grey: orange cheeks, a tear stripe under the eye, chestnut flanks, a barred tail. */
		GREY,
		/** Fawn: the same bird in warm brown. */
		FAWN,
		/** White all over, with the red beak. */
		WHITE;

		public static final Colour[] COLOURS = values();

		public static Colour byId(int id) {
			return COLOURS[Mth.clamp(id, 0, COLOURS.length - 1)];
		}
	}

	/** How far a finch looks for company, and how far from it a finch drifts before flying back. */
	public static final double COMPANY_REACH = 8.0;
	public static final double COMPANY_RANGE = 4.0;
	/** How far a finch's phrase carries to other finches. */
	public static final double HEARING = 8.0;
	/** How long a tamed finch waits between phrases of its own, in ticks: 20 to 60 seconds. */
	public static final int SONG_WAIT_MIN = 400;
	public static final int SONG_WAIT_SPAN = 800;
	/** How soon a finch answers one it heard, and how long after a phrase it stays quiet. */
	public static final int ANSWER_MIN = 20;
	public static final int ANSWER_SPAN = 20;
	public static final int QUIET_AFTER = 100;
	/** How far either side of the bar's middle each of two finches sits. */
	private static final double SHARED_SEAT = 0.2;

	/** Ticks until its next phrase of its own; set after each one. */
	private int nextSong = -1;
	/** Ticks until it answers a finch it heard; 0 when it isn't about to. */
	private int answerIn;
	/** Ticks after a phrase during which it won't answer. */
	private int quiet;
	/** Set while a phrase is an answer, so an answer calls for no answers of its own. */
	private boolean answering;

	public ZebraFinch(EntityType<? extends ZebraFinch> type, Level level) {
		super(type, level);
	}

	public static AttributeSupplier.Builder createAttributes() {
		return birdAttributes(3.0);
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

	/** A finch keeps to its perch: it never rides a shoulder. */
	@Override
	public boolean ridesShoulders() {
		return false;
	}

	@Override
	public SoundEvent songSound() {
		return Sounds.ZEBRA_FINCH_SONG.get();
	}

	/** A phrase is short: a second or two. */
	@Override
	protected int songTicks() {
		return 30;
	}

	/** A treat gets a phrase (and the finches around it an answer). */
	@Override
	protected void enjoyTreat() {
		this.sing();
	}

	@Override
	protected void registerGoals() {
		super.registerGoals();
		this.goalSelector.addGoal(2, new KeepCompanyGoal(this));
	}

	/** Any two finches may share a perch. */
	@Override
	public boolean sharesPerchWith(LittleBird other) {
		return other instanceof ZebraFinch;
	}

	/** One other finch may share its perch; a third looks elsewhere. */
	@Override
	public int maxPerchMates() {
		return 1;
	}

	/** Two on one perch sit either side of the middle: the one whose id sorts first takes the left. */
	@Override
	public double seatOffset() {
		ZebraFinch mate = this.perchMate();
		if (mate == null) {
			return 0.0;
		}
		return this.getUUID().compareTo(mate.getUUID()) < 0 ? -SHARED_SEAT : SHARED_SEAT;
	}

	/** The other finch that calls this finch's perch home, if there is one nearby. */
	public @Nullable ZebraFinch perchMate() {
		BlockPos perch = this.getPerch();
		if (perch == null) {
			return null;
		}
		List<ZebraFinch> mates = this.level().getEntitiesOfClass(ZebraFinch.class, this.getBoundingBox().inflate(FindPerchGoal.REACH * 2),
				other -> other != this && other.isAlive() && perch.equals(other.getPerch()));
		return mates.isEmpty() ? null : mates.getFirst();
	}

	/** Sings a phrase; unless this is an answer, the finches within hearing answer in a moment. */
	@Override
	public void sing() {
		super.sing();
		this.quiet = QUIET_AFTER;
		this.nextSong = SONG_WAIT_MIN + this.random.nextInt(SONG_WAIT_SPAN);
		if (!this.answering) {
			for (ZebraFinch other : this.level().getEntitiesOfClass(ZebraFinch.class, this.getBoundingBox().inflate(HEARING),
					o -> o != this && o.isAlive() && o.quiet == 0 && o.answerIn == 0)) {
				other.answerIn = ANSWER_MIN + this.random.nextInt(ANSWER_SPAN);
			}
		}
	}

	/** Whether it is in a state to sing: awake, on its feet or its seat, by day. */
	private boolean canSing(ServerLevel level) {
		return !this.isBaby() && !this.isFlying() && !this.isRoosting() && !this.isOnShoulder() && !level.isDarkOutside();
	}

	@Override
	protected void customServerAiStep(ServerLevel level) {
		super.customServerAiStep(level);
		if (this.quiet > 0) {
			this.quiet--;
		}
		if (this.answerIn > 0 && --this.answerIn == 0 && this.canSing(level)) {
			this.answering = true;
			this.sing();
			this.answering = false;
		}
		if (this.nextSong < 0) {
			this.nextSong = SONG_WAIT_MIN + this.random.nextInt(SONG_WAIT_SPAN);
		}
		if (--this.nextSong <= 0) {
			if (this.isTame() && this.canSing(level)) {
				this.sing();
			} else {
				this.nextSong = SONG_WAIT_MIN;
			}
		}
	}

	@Override
	protected void addAdditionalSaveData(ValueOutput output) {
		super.addAdditionalSaveData(output);
		output.putInt("NextSong", this.nextSong);
	}

	@Override
	protected void readAdditionalSaveData(ValueInput input) {
		super.readAdditionalSaveData(input);
		this.nextSong = input.getIntOr("NextSong", -1);
	}

	@Override
	public SoundEvent chatterSound() {
		return Sounds.ZEBRA_FINCH_CHATTER.get();
	}

	@Override
	public SoundEvent greetingSound() {
		return Sounds.ZEBRA_FINCH_GREETING.get();
	}

	@Override
	public SoundEvent settleSound() {
		return Sounds.ZEBRA_FINCH_SETTLE.get();
	}

	/** A finch that has drifted more than a few blocks from the nearest other finch flies back to it. */
	static class KeepCompanyGoal extends Goal {
		private final ZebraFinch bird;
		private @Nullable ZebraFinch company;

		KeepCompanyGoal(ZebraFinch bird) {
			this.bird = bird;
			this.setFlags(EnumSet.of(Flag.MOVE));
		}

		@Override
		public boolean canUse() {
			if (this.bird.isPerched() || this.bird.isOrderedToSit() || this.bird.getRandom().nextInt(reducedTickDelay(20)) != 0) {
				return false;
			}
			List<ZebraFinch> others = this.bird.level().getEntitiesOfClass(ZebraFinch.class, this.bird.getBoundingBox().inflate(COMPANY_REACH),
					other -> other != this.bird && other.isAlive() && !other.isOnShoulder());
			this.company = others.stream().min((a, b) -> Double.compare(a.distanceToSqr(this.bird), b.distanceToSqr(this.bird))).orElse(null);
			return this.company != null && this.bird.distanceToSqr(this.company) > COMPANY_RANGE * COMPANY_RANGE;
		}

		@Override
		public boolean canContinueToUse() {
			return this.company != null && this.company.isAlive() && !this.bird.getNavigation().isDone()
					&& this.bird.distanceToSqr(this.company) > 2.0 * 2.0;
		}

		@Override
		public void start() {
			if (this.company != null) {
				this.bird.getNavigation().moveTo(this.company, 1.0);
			}
		}

		@Override
		public void stop() {
			this.bird.getNavigation().stop();
		}
	}
}
