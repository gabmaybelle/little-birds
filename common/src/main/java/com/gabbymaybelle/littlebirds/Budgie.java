package com.gabbymaybelle.littlebirds;

import net.minecraft.core.BlockPos;
import net.minecraft.network.syncher.EntityDataAccessor;
import net.minecraft.network.syncher.EntityDataSerializers;
import net.minecraft.network.syncher.SynchedEntityData;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.sounds.SoundEvent;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.tags.BlockTags;
import net.minecraft.tags.ItemTags;
import net.minecraft.util.Mth;
import net.minecraft.util.RandomSource;
import net.minecraft.world.DifficultyInstance;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.damagesource.DamageSource;
import net.minecraft.world.entity.AgeableMob;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.EntityEvent;
import net.minecraft.world.entity.EntityReference;
import net.minecraft.world.entity.EntitySpawnReason;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.SpawnGroupData;
import net.minecraft.world.entity.TamableAnimal;
import net.minecraft.world.entity.ai.attributes.AttributeSupplier;
import net.minecraft.world.entity.ai.attributes.Attributes;
import net.minecraft.world.entity.ai.control.FlyingMoveControl;
import net.minecraft.world.entity.ai.goal.FloatGoal;
import net.minecraft.world.entity.ai.goal.FollowOwnerGoal;
import net.minecraft.world.entity.ai.goal.Goal;
import net.minecraft.world.entity.ai.goal.LookAtPlayerGoal;
import net.minecraft.world.entity.ai.goal.SitWhenOrderedToGoal;
import net.minecraft.world.entity.ai.goal.WaterAvoidingRandomFlyingGoal;
import net.minecraft.world.entity.ai.navigation.FlyingPathNavigation;
import net.minecraft.world.entity.ai.navigation.PathNavigation;
import net.minecraft.world.entity.animal.Animal;
import net.minecraft.world.entity.animal.parrot.ShoulderRidingEntity;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.LevelAccessor;
import net.minecraft.world.level.ServerLevelAccessor;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.pathfinder.PathType;
import net.minecraft.world.level.portal.TeleportTransition;
import net.minecraft.world.level.storage.ValueInput;
import net.minecraft.world.level.storage.ValueOutput;
import net.minecraft.world.phys.Vec3;
import org.jspecify.annotations.Nullable;

/**
 * A budgie: a small pet bird that comes in four colours. It is built on vanilla's shoulder-riding
 * tamable base (what the parrot uses), so flying, taming with seeds, sitting and following its
 * owner are vanilla's; what this mod adds, release by release, is a home (a perch), a greeting
 * when its owner comes back, and a shoulder it only rides when asked.
 *
 * <p>So far: it flies, is tamed with seeds (one try in three), sits when told, follows its owner,
 * and never jumps onto a shoulder by itself. A tamed bird makes the nearest perch its home
 * ({@link FindPerchGoal}): it keeps near it, goes back to sit on it ({@link RoostGoal}) every
 * night and whenever its owner is away, and stays home rather than following its owner out.
 * Break the perch and it is an ordinary pet again. Its voice is the parrot's, pitched up, until
 * it has its own sounds.
 */
public class Budgie extends ShoulderRidingEntity {
	/** The four colours, in the order of {@link #COLOURS}; a wild budgie picks one at random. */
	public enum Colour {
		GREEN, BLUE, YELLOW, WHITE;

		public static final Colour[] COLOURS = values();

		public static Colour byId(int id) {
			return COLOURS[Mth.clamp(id, 0, COLOURS.length - 1)];
		}
	}

	private static final EntityDataAccessor<Integer> DATA_COLOUR = SynchedEntityData.defineId(Budgie.class, EntityDataSerializers.INT);
	private static final EntityDataAccessor<Boolean> DATA_PERCHED = SynchedEntityData.defineId(Budgie.class, EntityDataSerializers.BOOLEAN);
	private static final EntityDataAccessor<Boolean> DATA_ON_SHOULDER = SynchedEntityData.defineId(Budgie.class, EntityDataSerializers.BOOLEAN);

	/** How far from the perch its owner can be and still count as home; farther, and the bird stays rather than follows. */
	public static final double HOME_RANGE = 16.0;
	/** How far from its perch a bird wanders on its own. */
	private static final int WANDER_RANGE = 8;

	/** The perch this bird calls home, if it has one. */
	private @Nullable BlockPos perch;
	/** Set when its owner calls it to its perch (uses the perch): the roost goal goes at once. */
	private boolean called;

	/** Where it sits on its owner: this far up from the feet and out to the right of the body's middle. */
	public static final double SHOULDER_UP = 1.2;
	public static final double SHOULDER_OUT = 0.42;

	/** How near its owner has to come to count as home (blocks). */
	public static final double GREETING_REACH = 8.0;
	/** How long its owner has to have been away for a return to earn a greeting: an hour of the day clock. */
	public static final long GREETING_AFTER = 1000L;
	/** The entity event that tells clients to play the greeting bob. */
	private static final byte GREET_EVENT = 69;
	/** The day-clock time its owner was last within reach; -1 until it has an owner. */
	private long lastSawOwner = -1L;
	/** How many greetings it has given since it was loaded (for the tests). */
	public int greetings;
	/** Client side: ticks left of the greeting bob. */
	public int greetTicks;

	// The flap animation, as vanilla's parrot keeps it: the renderer reads these.
	public float flap;
	public float flapSpeed;
	public float oFlapSpeed;
	public float oFlap;
	private float flapping = 1.0F;

	public Budgie(EntityType<? extends Budgie> type, Level level) {
		super(type, level);
		this.moveControl = new FlyingMoveControl<>(this, 10, false);
		this.setPathfindingMalus(PathType.FIRE_IN_NEIGHBOR, -1.0F);
		this.setPathfindingMalus(PathType.FIRE, -1.0F);
		this.setPathfindingMalus(PathType.COCOA, -1.0F);
	}

	public static AttributeSupplier.Builder createAttributes() {
		return Animal.createAnimalAttributes()
				.add(Attributes.MAX_HEALTH, 5.0)
				.add(Attributes.FLYING_SPEED, 0.4F)
				.add(Attributes.MOVEMENT_SPEED, 0.2F);
	}

	@Override
	public @Nullable SpawnGroupData finalizeSpawn(ServerLevelAccessor level, DifficultyInstance difficulty, EntitySpawnReason reason, @Nullable SpawnGroupData groupData) {
		this.setColour(Colour.COLOURS[level.getRandom().nextInt(Colour.COLOURS.length)]);
		if (groupData == null) {
			groupData = new AgeableMob.AgeableMobGroupData(false);
		}
		return super.finalizeSpawn(level, difficulty, reason, groupData);
	}

	@Override
	protected boolean canBeABaby() {
		return false;
	}

	@Override
	protected void registerGoals() {
		this.goalSelector.addGoal(0, new TamableAnimal.TamableAnimalPanicGoal(1.25));
		this.goalSelector.addGoal(0, new FloatGoal(this));
		this.goalSelector.addGoal(1, new LookAtPlayerGoal(this, Player.class, 8.0F));
		this.goalSelector.addGoal(1, new RoostGoal(this));
		this.goalSelector.addGoal(2, new SitWhenOrderedToGoal(this));
		this.goalSelector.addGoal(2, new StayHomeFollowOwnerGoal(this));
		this.goalSelector.addGoal(2, new WaterAvoidingRandomFlyingGoal(this, 1.0));
		this.goalSelector.addGoal(3, new FindPerchGoal(this));
		// No LandOnOwnersShoulderGoal: a budgie rides a shoulder only when asked (a later release).
	}

	/** Vanilla's follow, except that a bird at home stays there when its owner goes out. */
	private static class StayHomeFollowOwnerGoal extends FollowOwnerGoal {
		private final Budgie bird;

		StayHomeFollowOwnerGoal(Budgie bird) {
			super(bird, 1.0, 5.0F, 1.0F);
			this.bird = bird;
		}

		@Override
		public boolean canUse() {
			return !this.bird.staysHome() && super.canUse();
		}

		@Override
		public boolean canContinueToUse() {
			return !this.bird.staysHome() && super.canContinueToUse();
		}
	}

	/** A bird at home doesn't teleport after an owner who has gone out, either. */
	@Override
	public boolean shouldTryTeleportToOwner() {
		return !this.staysHome() && super.shouldTryTeleportToOwner();
	}

	/**
	 * Whether the bird stays home instead of following: it has a perch, it is near it, and its
	 * owner is away (farther than {@link #HOME_RANGE} from the perch, or not here at all).
	 */
	public boolean staysHome() {
		BlockPos perch = this.getPerch();
		if (perch == null || perch.distToCenterSqr(this.position()) >= RoostGoal.FLY_HOME_RANGE * RoostGoal.FLY_HOME_RANGE) {
			return false;
		}
		LivingEntity owner = this.getOwner();
		return owner == null || perch.distToCenterSqr(owner.position()) >= HOME_RANGE * HOME_RANGE;
	}

	/** The bird's perch, or null once it has none or the block is gone (checked as it's asked). */
	public @Nullable BlockPos getPerch() {
		if (this.perch != null && !this.level().getBlockState(this.perch).is(Perch.BLOCK.get())) {
			this.setPerch(null);
		}
		return this.perch;
	}

	public boolean hasPerch() {
		return this.getPerch() != null;
	}

	/** Makes a perch the bird's home (or, with null, takes it away); its wandering keeps within reach of it. */
	public void setPerch(@Nullable BlockPos pos) {
		this.perch = pos == null ? null : pos.immutable();
		if (pos == null) {
			this.setHomeTo(BlockPos.ZERO, -1);
		} else {
			this.setHomeTo(pos, WANDER_RANGE);
		}
	}

	/** Whether the bird is sitting on its perch right now. */
	public boolean isPerched() {
		return this.entityData.get(DATA_PERCHED);
	}

	public void setPerched(boolean perched) {
		this.entityData.set(DATA_PERCHED, perched);
	}

	/** Whether the bird is asleep on its perch: on it, after dark. */
	public boolean isRoosting() {
		return this.isPerched() && this.level().isDarkOutside();
	}

	@Override
	protected PathNavigation createNavigation(Level level) {
		FlyingPathNavigation navigation = new FlyingPathNavigation(this, level);
		navigation.setCanOpenDoors(false);
		navigation.setCanFloat(true);
		return navigation;
	}

	@Override
	public void aiStep() {
		super.aiStep();
		this.calculateFlapping();
		if (this.greetTicks > 0) {
			this.greetTicks--;
		}
		if (this.isOnShoulder()) {
			this.followShoulder();
		}
	}

	@Override
	protected void customServerAiStep(ServerLevel level) {
		super.customServerAiStep(level);
		// On a shoulder nothing moves it but its owner; the look goals still turn its head.
		this.goalSelector.setControlFlag(Goal.Flag.MOVE, !this.isOnShoulder());
		if (this.isOnShoulder() && this.tickCount % 10 == 0 && !(this.getOwner() instanceof Player owner && owner.isAlive() && !owner.isSpectator())) {
			ServerPlayer elsewhere = this.ownerInAnotherWorld(level);
			if (elsewhere != null) {
				// Its owner went through a portal: it goes too, straight onto the shoulder on the other side.
				this.followThroughPortal(elsewhere);
			} else {
				// Its owner has gone (logged out, died): back on its own feet.
				this.setOnShoulder(false);
			}
		}
		if (this.tickCount % 10 == 0) {
			this.watchForOwner(level);
		}
	}

	/**
	 * The shoulder, on both sides every tick: the bird is held at its owner's right shoulder,
	 * facing the way the owner faces. It isn't a passenger (the game won't let anything ride a
	 * player), so it saves and loads like any bird; the client places it the same way, which keeps
	 * it on the shoulder between the server's updates.
	 */
	private void followShoulder() {
		if (!(this.getOwner() instanceof Player owner)) {
			return;
		}
		float yaw = owner.yBodyRot;
		float rad = yaw * Mth.DEG_TO_RAD;
		double x = owner.getX() - Mth.cos(rad) * SHOULDER_OUT;
		double z = owner.getZ() - Mth.sin(rad) * SHOULDER_OUT;
		this.setPos(x, owner.getY() + SHOULDER_UP, z);
		this.setDeltaMovement(Vec3.ZERO);
		this.setYRot(yaw);
		this.setYBodyRot(yaw);
		this.setYHeadRot(yaw);
		this.setOnGround(true);
	}

	/** The owner, if online, alive and in another world than the bird. */
	private @Nullable ServerPlayer ownerInAnotherWorld(ServerLevel level) {
		EntityReference<LivingEntity> reference = this.getOwnerReference();
		if (reference == null) {
			return null;
		}
		ServerPlayer owner = level.getServer().getPlayerList().getPlayer(reference.getUUID());
		return owner != null && owner.isAlive() && !owner.isSpectator() && owner.level() != level ? owner : null;
	}

	/** On a shoulder it doesn't take portals by itself (it would arrive before its owner); it goes when its owner has gone. */
	@Override
	public boolean canUsePortal(boolean ignorePassenger) {
		return !this.isOnShoulder() && super.canUsePortal(ignorePassenger);
	}

	/** Its owner is in another world: the bird goes there too, straight onto the shoulder. */
	public void followThroughPortal(ServerPlayer owner) {
		ServerLevel there = (ServerLevel) owner.level();
		float yaw = owner.yBodyRot;
		float rad = yaw * Mth.DEG_TO_RAD;
		Vec3 seat = new Vec3(owner.getX() - Mth.cos(rad) * SHOULDER_OUT, owner.getY() + SHOULDER_UP, owner.getZ() - Mth.sin(rad) * SHOULDER_OUT);
		this.teleport(new TeleportTransition(there, seat, Vec3.ZERO, yaw, 0.0F, TeleportTransition.DO_NOTHING));
	}

	/** Whether the bird is on its owner's shoulder. */
	public boolean isOnShoulder() {
		return this.entityData.get(DATA_ON_SHOULDER);
	}

	/** Puts the bird on its owner's shoulder, or sets it down where it is. */
	public void setOnShoulder(boolean on) {
		this.entityData.set(DATA_ON_SHOULDER, on);
		this.noPhysics = on;
		this.setNoGravity(on);
		if (on) {
			this.getNavigation().stop();
			this.setOrderedToSit(false);
			this.setPerched(false);
			this.setInSittingPose(true);
		} else {
			this.setInSittingPose(this.isOrderedToSit());
		}
	}

	/**
	 * The greeting: the bird keeps the day-clock time its owner was last within reach. When the
	 * owner comes back within reach after an hour or more away, it chirps hello once and bobs.
	 * A bird on its owner's shoulder is never away.
	 */
	private void watchForOwner(ServerLevel level) {
		if (!this.isTame()) {
			return;
		}
		LivingEntity owner = this.getOwner();
		long now = level.getOverworldClockTime();
		if (owner == null || (!this.isOnShoulder() && this.distanceToSqr(owner) > GREETING_REACH * GREETING_REACH)) {
			if (this.lastSawOwner > now) {
				this.lastSawOwner = now; // the clock was turned back
			}
			return;
		}
		if (this.lastSawOwner >= 0 && now - this.lastSawOwner >= GREETING_AFTER) {
			this.greet();
		}
		this.lastSawOwner = now;
	}

	private void greet() {
		this.greetings++;
		this.playSound(Sounds.BUDGIE_GREETING.get(), 1.0F, this.getVoicePitch());
		this.level().broadcastEntityEvent(this, GREET_EVENT);
	}

	@Override
	public void handleEntityEvent(@EntityEvent.Value byte id) {
		if (id == GREET_EVENT) {
			this.greetTicks = 30;
		} else {
			super.handleEntityEvent(id);
		}
	}

	@Override
	public void tame(Player player) {
		super.tame(player);
		this.lastSawOwner = this.level().getOverworldClockTime();
	}

	/** Its owner calls it to a perch: that perch is home now, and it goes at once. */
	public void callTo(BlockPos perch) {
		this.setOnShoulder(false);
		this.setPerch(perch);
		this.called = true;
		this.setOrderedToSit(false);
	}

	/** Whether it has been called and not yet gone; asking clears it. */
	boolean consumeCall() {
		boolean was = this.called;
		this.called = false;
		return was;
	}

	private void calculateFlapping() {
		this.oFlap = this.flap;
		this.oFlapSpeed = this.flapSpeed;
		this.flapSpeed = this.flapSpeed + (!this.onGround() && !this.isPassenger() ? 4 : -1) * 0.3F;
		this.flapSpeed = Mth.clamp(this.flapSpeed, 0.0F, 1.0F);
		if (!this.onGround() && this.flapping < 1.0F) {
			this.flapping = 1.0F;
		}
		this.flapping *= 0.9F;
		Vec3 movement = this.getDeltaMovement();
		if (!this.onGround() && movement.y < 0.0) {
			this.setDeltaMovement(movement.multiply(1.0, 0.6, 1.0));
		}
		this.flap = this.flap + this.flapping * 2.0F;
	}

	/** Seeds tame a wild budgie (one try in three); a tamed one sits or stands up when its owner uses it. */
	@Override
	public InteractionResult mobInteract(Player player, InteractionHand hand) {
		ItemStack stack = player.getItemInHand(hand);
		if (!this.isTame() && stack.is(ItemTags.PARROT_FOOD)) {
			this.usePlayerItem(player, hand, stack);
			if (!this.isSilent()) {
				this.level().playSound(null, this.getX(), this.getY(), this.getZ(), SoundEvents.PARROT_EAT, this.getSoundSource(), 1.0F, this.getVoicePitch());
			}
			if (!this.level().isClientSide()) {
				if (this.random.nextInt(3) == 0) {
					this.tame(player);
					this.level().broadcastEntityEvent(this, (byte) 7);
				} else {
					this.level().broadcastEntityEvent(this, (byte) 6);
				}
			}
			return InteractionResult.SUCCESS;
		}
		if (this.isTame() && this.isOwnedBy(player) && player.isShiftKeyDown() && stack.isEmpty()) {
			// Sneak-use: onto the shoulder, or off it.
			if (!this.level().isClientSide()) {
				if (this.isOnShoulder()) {
					this.setOnShoulder(false);
				} else if (Shoulder.riding(player) == null) {
					this.setOnShoulder(true);
				}
			}
			return InteractionResult.SUCCESS;
		}
		if (!this.isFlying() && this.isTame() && this.isOwnedBy(player) && !this.isOnShoulder()) {
			if (!this.level().isClientSide()) {
				this.setOrderedToSit(!this.isOrderedToSit());
			}
			return InteractionResult.SUCCESS;
		}
		return super.mobInteract(player, hand);
	}

	@Override
	public boolean isFood(ItemStack stack) {
		return false;
	}

	/**
	 * Where a wild budgie may appear: on grass or leaves in daylight, like a parrot, and only while
	 * {@code natural_spawning} is on, at the {@code spawn_chance} rate. Spawn eggs and commands
	 * don't come through here.
	 */
	public static boolean checkSpawnRules(EntityType<Budgie> type, LevelAccessor level, EntitySpawnReason reason, BlockPos pos, RandomSource random) {
		if (!Settings.naturalSpawning || random.nextInt(100) >= Settings.spawnChance) {
			return false;
		}
		return level.getBlockState(pos.below()).is(BlockTags.PARROTS_SPAWNABLE_ON) && isBrightEnoughToSpawn(level, pos);
	}

	@Override
	public boolean canMate(Animal partner) {
		return false;
	}

	@Override
	public @Nullable AgeableMob getBreedOffspring(ServerLevel level, AgeableMob partner) {
		return null;
	}

	@Override
	protected void checkFallDamage(double y, boolean onGround, BlockState state, BlockPos pos) {
	}

	/** Chatter by day; quiet once it has settled on its perch for the night. */
	@Override
	public @Nullable SoundEvent getAmbientSound() {
		return this.isRoosting() ? null : Sounds.BUDGIE_CHATTER.get();
	}

	@Override
	protected SoundEvent getHurtSound(DamageSource source) {
		return SoundEvents.PARROT_HURT;
	}

	@Override
	protected SoundEvent getDeathSound() {
		return SoundEvents.PARROT_DEATH;
	}

	@Override
	protected void playStepSound(BlockPos pos, BlockState state) {
		this.playSound(SoundEvents.PARROT_STEP, 0.15F, 1.0F);
	}

	@Override
	protected boolean isFlapping() {
		return this.flyDist > this.nextFlap;
	}

	private float nextFlap = 1.0F;

	@Override
	protected void onFlap() {
		this.playSound(SoundEvents.PARROT_FLY, 0.15F, 1.0F);
		this.nextFlap = this.flyDist + this.flapSpeed / 2.0F;
	}

	/** Its own recordings play as they are, give or take a little, so no two chirps are quite the same. */
	@Override
	public float getVoicePitch() {
		return (this.random.nextFloat() - this.random.nextFloat()) * 0.1F + 1.0F;
	}

	@Override
	public SoundSource getSoundSource() {
		return SoundSource.NEUTRAL;
	}

	@Override
	public boolean isPushable() {
		return true;
	}

	@Override
	protected void doPush(Entity entity) {
		if (!(entity instanceof Player)) {
			super.doPush(entity);
		}
	}

	@Override
	public boolean hurtServer(ServerLevel level, DamageSource source, float damage) {
		if (this.isInvulnerableTo(level, source)) {
			return false;
		}
		this.setOrderedToSit(false);
		return super.hurtServer(level, source, damage);
	}

	public Colour getColour() {
		return Colour.byId(this.entityData.get(DATA_COLOUR));
	}

	public void setColour(Colour colour) {
		this.entityData.set(DATA_COLOUR, colour.ordinal());
	}

	@Override
	protected void defineSynchedData(SynchedEntityData.Builder data) {
		super.defineSynchedData(data);
		data.define(DATA_COLOUR, Colour.GREEN.ordinal());
		data.define(DATA_PERCHED, false);
		data.define(DATA_ON_SHOULDER, false);
	}

	@Override
	protected void addAdditionalSaveData(ValueOutput output) {
		super.addAdditionalSaveData(output);
		output.putInt("Colour", this.getColour().ordinal());
		if (this.perch != null) {
			output.store("Perch", BlockPos.CODEC, this.perch);
		}
		output.putLong("LastSawOwner", this.lastSawOwner);
		output.putBoolean("OnShoulder", this.isOnShoulder());
	}

	@Override
	protected void readAdditionalSaveData(ValueInput input) {
		super.readAdditionalSaveData(input);
		this.setColour(Colour.byId(input.getIntOr("Colour", 0)));
		this.setPerch(input.read("Perch", BlockPos.CODEC).orElse(null));
		this.lastSawOwner = input.getLongOr("LastSawOwner", -1L);
		if (input.getBooleanOr("OnShoulder", false)) {
			this.setOnShoulder(true);
		}
	}

	public boolean isFlying() {
		return !this.onGround();
	}

	@Override
	protected boolean omnidirectionalAirMover() {
		return true;
	}

	@Override
	protected boolean canFlyToOwner() {
		return true;
	}

	@Override
	public Vec3 getLeashOffset() {
		return new Vec3(0.0, 0.5F * this.getEyeHeight(), this.getBbWidth() * 0.4F);
	}
}
