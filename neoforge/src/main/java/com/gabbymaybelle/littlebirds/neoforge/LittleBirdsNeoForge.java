package com.gabbymaybelle.littlebirds.neoforge;

import com.gabbymaybelle.littlebirds.LittleBirds;
import com.gabbymaybelle.littlebirds.Shoulder;
import com.gabbymaybelle.littlebirds.platform.Services;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.InteractionResult;
import net.neoforged.bus.api.IEventBus;
import net.neoforged.fml.common.Mod;
import net.neoforged.neoforge.common.NeoForge;
import net.neoforged.neoforge.event.BuildCreativeModeTabContentsEvent;
import net.neoforged.neoforge.event.entity.EntityAttributeCreationEvent;
import net.neoforged.neoforge.event.entity.RegisterSpawnPlacementsEvent;
import net.neoforged.neoforge.event.entity.player.PlayerEvent;
import net.neoforged.neoforge.event.entity.player.PlayerInteractEvent;
import net.neoforged.neoforge.registries.RegisterEvent;

/** The NeoForge entrypoint: everything happens in {@code common}; this wires the registry events and the one player event it needs. */
@Mod(LittleBirds.MOD_ID)
public class LittleBirdsNeoForge {
	public LittleBirdsNeoForge(IEventBus modBus) {
		NeoForgePlatform platform = (NeoForgePlatform) Services.PLATFORM;
		// init() queues the registrations; the registry events hand them to NeoForge.
		LittleBirds.init();
		modBus.addListener(RegisterEvent.class, platform::onRegister);
		modBus.addListener(EntityAttributeCreationEvent.class, platform::onAttributes);
		modBus.addListener(RegisterSpawnPlacementsEvent.class, platform::onSpawnPlacements);
		modBus.addListener(BuildCreativeModeTabContentsEvent.class, platform::onCreativeTab);
		// A sneak-use on a block with an empty hand sets a shoulder bird down there.
		NeoForge.EVENT_BUS.addListener(PlayerInteractEvent.RightClickBlock.class, LittleBirdsNeoForge::onUseBlock);
		// A player who changes worlds takes the bird on their shoulder along.
		NeoForge.EVENT_BUS.addListener(PlayerEvent.PlayerChangedDimensionEvent.class, LittleBirdsNeoForge::onChangedWorld);
	}

	static void onChangedWorld(PlayerEvent.PlayerChangedDimensionEvent event) {
		if (event.getEntity() instanceof ServerPlayer player) {
			ServerLevel origin = player.level().getServer().getLevel(event.getFrom());
			if (origin != null) {
				Shoulder.followAcrossWorlds(player, origin);
			}
		}
	}

	static void onUseBlock(PlayerInteractEvent.RightClickBlock event) {
		if (Shoulder.trySetDown(event.getEntity(), event.getLevel(), event.getPos(), event.getFace())) {
			event.setCanceled(true);
			event.setCancellationResult(InteractionResult.SUCCESS);
		}
	}
}
