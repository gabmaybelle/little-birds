package com.gabbymaybelle.littlebirds.neoforge;

import com.gabbymaybelle.littlebirds.Birds;
import com.gabbymaybelle.littlebirds.LittleBirds;
import com.gabbymaybelle.littlebirds.client.BudgieRenderer;
import net.neoforged.api.distmarker.Dist;
import net.neoforged.bus.api.IEventBus;
import net.neoforged.fml.common.Mod;
import net.neoforged.neoforge.client.event.EntityRenderersEvent;

/** The NeoForge client entrypoint (loaded only on players' computers): the birds' renderers. */
@Mod(value = LittleBirds.MOD_ID, dist = Dist.CLIENT)
public class LittleBirdsNeoForgeClient {
	public LittleBirdsNeoForgeClient(IEventBus modBus) {
		modBus.addListener(EntityRenderersEvent.RegisterRenderers.class, event -> event.registerEntityRenderer(Birds.BUDGIE.get(), BudgieRenderer::new));
	}
}
