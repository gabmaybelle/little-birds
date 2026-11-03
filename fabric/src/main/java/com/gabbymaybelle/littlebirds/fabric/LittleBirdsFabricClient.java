package com.gabbymaybelle.littlebirds.fabric;

import com.gabbymaybelle.littlebirds.Birds;
import com.gabbymaybelle.littlebirds.client.BudgieRenderer;
import net.fabricmc.api.ClientModInitializer;
import net.fabricmc.fabric.api.client.rendering.v1.EntityRendererRegistry;

/** The Fabric client entrypoint: the birds' renderers, in {@code common}'s {@code client} package. */
public class LittleBirdsFabricClient implements ClientModInitializer {
	@Override
	public void onInitializeClient() {
		EntityRendererRegistry.register(Birds.BUDGIE.get(), BudgieRenderer::new);
	}
}
