package com.gabbymaybelle.littlebirds.fabric;

import com.gabbymaybelle.littlebirds.Birds;
import com.gabbymaybelle.littlebirds.client.BudgieModel;
import com.gabbymaybelle.littlebirds.client.CanaryModel;
import com.gabbymaybelle.littlebirds.client.CanaryRenderer;
import com.gabbymaybelle.littlebirds.client.CockatielModel;
import com.gabbymaybelle.littlebirds.client.CockatielRenderer;
import com.gabbymaybelle.littlebirds.client.LovebirdModel;
import com.gabbymaybelle.littlebirds.client.LovebirdRenderer;
import com.gabbymaybelle.littlebirds.client.BudgieRenderer;
import com.gabbymaybelle.littlebirds.client.ZebraFinchModel;
import com.gabbymaybelle.littlebirds.client.ZebraFinchRenderer;
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
		ModelLayerRegistry.registerModelLayer(LovebirdModel.LAYER, LovebirdModel::createBodyLayer);
		EntityRendererRegistry.register(Birds.LOVEBIRD.get(), LovebirdRenderer::new);
		ModelLayerRegistry.registerModelLayer(CanaryModel.LAYER, CanaryModel::createBodyLayer);
		EntityRendererRegistry.register(Birds.CANARY.get(), CanaryRenderer::new);
		ModelLayerRegistry.registerModelLayer(ZebraFinchModel.LAYER, ZebraFinchModel::createBodyLayer);
		EntityRendererRegistry.register(Birds.ZEBRA_FINCH.get(), ZebraFinchRenderer::new);
	}
}
