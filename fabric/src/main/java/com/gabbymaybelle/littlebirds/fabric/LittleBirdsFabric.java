package com.gabbymaybelle.littlebirds.fabric;

import com.gabbymaybelle.littlebirds.LittleBirds;
import com.gabbymaybelle.littlebirds.Shoulder;
import net.fabricmc.api.ModInitializer;
import net.fabricmc.fabric.api.entity.event.v1.ServerEntityLevelChangeEvents;
import net.fabricmc.fabric.api.event.player.UseBlockCallback;
import net.minecraft.world.InteractionResult;

/** The Fabric entrypoint: everything happens in {@code common}; this wires the one player event it needs. */
public class LittleBirdsFabric implements ModInitializer {
	@Override
	public void onInitialize() {
		LittleBirds.init();
		// A sneak-use on a block with an empty hand sets a shoulder bird down there.
		UseBlockCallback.EVENT.register((player, level, hand, hit) ->
				Shoulder.trySetDown(player, level, hit.getBlockPos(), hit.getDirection()) ? InteractionResult.SUCCESS : InteractionResult.PASS);
		// A player who changes worlds takes the bird on their shoulder along.
		ServerEntityLevelChangeEvents.AFTER_PLAYER_CHANGE_LEVEL.register((player, origin, destination) -> Shoulder.followAcrossWorlds(player, origin));
	}
}
