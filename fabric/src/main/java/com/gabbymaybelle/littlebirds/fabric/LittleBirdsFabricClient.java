package com.gabbymaybelle.littlebirds.fabric;

import com.gabbymaybelle.littlebirds.Birds;
import com.gabbymaybelle.littlebirds.client.BudgieModel;
import com.gabbymaybelle.littlebirds.client.CockatielModel;
import com.gabbymaybelle.littlebirds.client.CockatielRenderer;
import com.gabbymaybelle.littlebirds.client.BudgieRenderer;
import net.fabricmc.api.ClientModInitializer;
import net.fabricmc.fabric.api.client.rendering.v1.EntityRendererRegistry;
import net.fabricmc.fabric.api.client.rendering.v1.ModelLayerRegistry;

/** The Fabric client entrypoint: the birds' models and renderers, in {@code common}'s {@code client} package. */
public class LittleBirdsFabricClient implements ClientModInitializer {
	@Override
	public void onInitializeClient() {
		ModelLayerRegistry.registerModelLayer(BudgieModel.LAYER, BudgieModel::createBodyLayer);
		EntityRendererRegistry.register(Birds.BUDGIE.get(), BudgieRenderer::new);
		ModelLayerRegistry.registerModelLayer(CockatielModel.LAYER, CockatielModel::createBodyLayer);
		EntityRendererRegistry.register(Birds.COCKATIEL.get(), CockatielRenderer::new);
	}
}
