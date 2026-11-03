package com.gabbymaybelle.littlebirds.fabric;

import com.gabbymaybelle.littlebirds.LittleBirds;
import net.fabricmc.api.ModInitializer;

/** The Fabric entrypoint: everything happens in {@code common}. */
public class LittleBirdsFabric implements ModInitializer {
	@Override
	public void onInitialize() {
		LittleBirds.init();
	}
}
