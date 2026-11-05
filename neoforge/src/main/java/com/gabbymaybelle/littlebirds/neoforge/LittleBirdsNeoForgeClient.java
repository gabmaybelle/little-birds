package com.gabbymaybelle.littlebirds.neoforge;

import com.gabbymaybelle.littlebirds.Birds;
import com.gabbymaybelle.littlebirds.LittleBirds;
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
import net.neoforged.api.distmarker.Dist;
import net.neoforged.bus.api.IEventBus;
import net.neoforged.fml.common.Mod;
import net.neoforged.neoforge.client.event.EntityRenderersEvent;

/** The NeoForge client entrypoint (loaded only on players' computers): the birds' models and renderers. */
@Mod(value = LittleBirds.MOD_ID, dist = Dist.CLIENT)
public class LittleBirdsNeoForgeClient {
	public LittleBirdsNeoForgeClient(IEventBus modBus) {
		modBus.addListener(EntityRenderersEvent.RegisterLayerDefinitions.class, event -> {
			event.registerLayerDefinition(BudgieModel.LAYER, BudgieModel::createBodyLayer);
			event.registerLayerDefinition(CockatielModel.LAYER, CockatielModel::createBodyLayer);
			event.registerLayerDefinition(LovebirdModel.LAYER, LovebirdModel::createBodyLayer);
			event.registerLayerDefinition(CanaryModel.LAYER, CanaryModel::createBodyLayer);
			event.registerLayerDefinition(ZebraFinchModel.LAYER, ZebraFinchModel::createBodyLayer);
		});
		modBus.addListener(EntityRenderersEvent.RegisterRenderers.class, event -> {
			event.registerEntityRenderer(Birds.BUDGIE.get(), BudgieRenderer::new);
			event.registerEntityRenderer(Birds.COCKATIEL.get(), CockatielRenderer::new);
			event.registerEntityRenderer(Birds.LOVEBIRD.get(), LovebirdRenderer::new);
			event.registerEntityRenderer(Birds.CANARY.get(), CanaryRenderer::new);
			event.registerEntityRenderer(Birds.ZEBRA_FINCH.get(), ZebraFinchRenderer::new);
		});
	}
}
