package com.gabbymaybelle.littlebirds;

import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.util.RandomSource;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.context.BlockPlaceContext;
import net.minecraft.world.level.BlockGetter;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.LevelReader;
import net.minecraft.world.level.ScheduledTickAccess;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.HorizontalDirectionalBlock;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.block.state.StateDefinition;
import net.minecraft.world.phys.AABB;
import net.minecraft.world.phys.BlockHitResult;
import net.minecraft.world.phys.Vec3;
import net.minecraft.world.phys.shapes.CollisionContext;
import org.jspecify.annotations.Nullable;
import net.minecraft.world.phys.shapes.Shapes;
import net.minecraft.world.phys.shapes.VoxelShape;

/**
 * A bird's perch: a small wooden base, a post, and a bar across the top for a bird to sit on. It
 * faces the player who placed it, so the bar runs across their view and a bird on it sits side
 * on; the bird itself turns to face the way the perch faces. It needs a block under it, like a
 * candle. It has no block entity: the birds remember where their perch is, not the other way round.
 */
public class PerchBlock extends HorizontalDirectionalBlock {
	/** How far above the block's bottom a bird's feet sit: the top of the bar. */
	public static final double SEAT_HEIGHT = 12.0 / 16.0;
	/** How far away a budgie can be called from (blocks). */
	public static final double CALL_REACH = 16.0;

	private static final VoxelShape BASE_AND_POST = Shapes.or(Block.column(6.0, 0.0, 2.0), Block.column(2.0, 2.0, 10.0));
	/** The bar runs along x when the perch faces north or south, along z when it faces east or west. */
	private static final VoxelShape ALONG_X = Shapes.or(BASE_AND_POST, Block.box(2.0, 10.0, 7.0, 14.0, 12.0, 9.0));
	private static final VoxelShape ALONG_Z = Shapes.or(BASE_AND_POST, Block.box(7.0, 10.0, 2.0, 9.0, 12.0, 14.0));

	public PerchBlock(Properties properties) {
		super(properties);
		this.registerDefaultState(this.stateDefinition.any().setValue(FACING, Direction.NORTH));
	}

	/** Where a bird sits on the perch at {@code pos}: the middle of the bar. */
	public static Vec3 seat(BlockPos pos) {
		return Vec3.atBottomCenterOf(pos).add(0.0, SEAT_HEIGHT, 0.0);
	}

	/** The way a bird on this perch faces: the way the perch faces (towards whoever placed it). */
	public static float seatYaw(BlockState state) {
		return state.getValue(FACING).toYRot();
	}

	/**
	 * Using the perch with an empty hand: a budgie on the player's shoulder is set down on it, or
	 * else the player's nearest budgie is called to it (it becomes that bird's home). A sneak-use
	 * is handled before this by {@link Shoulder#trySetDown}.
	 */
	@Override
	protected InteractionResult useWithoutItem(BlockState state, Level level, BlockPos pos, Player player, BlockHitResult hit) {
		if (!player.getMainHandItem().isEmpty()) {
			return InteractionResult.PASS;
		}
		if (level.isClientSide()) {
			return InteractionResult.SUCCESS;
		}
		LittleBird bird = Shoulder.riding(player);
		if (bird != null) {
			Vec3 seat = seat(pos);
			bird.snapTo(seat.x, seat.y, seat.z, bird.getYRot(), 0.0F);
			bird.callTo(pos);
			return InteractionResult.SUCCESS;
		}
		bird = nearestBird(level, pos, player);
		if (bird == null) {
			return InteractionResult.PASS;
		}
		bird.callTo(pos);
		return InteractionResult.SUCCESS;
	}

	/** The player's nearest tamed budgie within reach of the perch that isn't already sitting on it. */
	private static @Nullable LittleBird nearestBird(Level level, BlockPos pos, Player player) {
		AABB around = new AABB(pos).inflate(CALL_REACH);
		LittleBird nearest = null;
		double best = Double.MAX_VALUE;
		for (LittleBird bird : level.getEntitiesOfClass(LittleBird.class, around, b -> b.isTame() && b.isOwnedBy(player) && !b.isOnShoulder())) {
			if (bird.isPerched() && pos.equals(bird.getPerch())) {
				continue;
			}
			double distance = bird.distanceToSqr(Vec3.atCenterOf(pos));
			if (distance < best) {
				best = distance;
				nearest = bird;
			}
		}
		return nearest;
	}

	@Override
	protected VoxelShape getShape(BlockState state, BlockGetter level, BlockPos pos, CollisionContext context) {
		return state.getValue(FACING).getAxis() == Direction.Axis.Z ? ALONG_X : ALONG_Z;
	}

	@Override
	public BlockState getStateForPlacement(BlockPlaceContext context) {
		return this.defaultBlockState().setValue(FACING, context.getHorizontalDirection().getOpposite());
	}

	@Override
	protected boolean canSurvive(BlockState state, LevelReader level, BlockPos pos) {
		return Block.canSupportCenter(level, pos.below(), Direction.UP);
	}

	@Override
	protected BlockState updateShape(BlockState state, LevelReader level, ScheduledTickAccess ticks, BlockPos pos, Direction directionToNeighbour, BlockPos neighbourPos, BlockState neighbourState, RandomSource random) {
		return directionToNeighbour == Direction.DOWN && !state.canSurvive(level, pos)
				? Blocks.AIR.defaultBlockState()
				: super.updateShape(state, level, ticks, pos, directionToNeighbour, neighbourPos, neighbourState, random);
	}

	@Override
	protected void createBlockStateDefinition(StateDefinition.Builder<Block, BlockState> builder) {
		builder.add(FACING);
	}
}
