package com.xziggsx.horseloyalty.dev;

import com.xziggsx.horseloyalty.core.feature.Feature;
import com.xziggsx.horseloyalty.core.feature.FeatureContext;

/**
 * Dev tools: the dev HUD panel and the operator commands that bend the game for testing. The HUD
 * also needs the dev environment or the {@code -Dhorseloyalty.devhud=true} JVM flag.
 */
public final class DevToolsFeature implements Feature {
	public static final String ID = "devTools";

	@Override
	public String id() {
		return ID;
	}

	@Override
	public void registerData() {
	}

	@Override
	public void wire(FeatureContext context) {
	}
}
