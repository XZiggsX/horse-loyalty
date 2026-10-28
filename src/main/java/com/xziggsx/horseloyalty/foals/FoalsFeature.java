package com.xziggsx.horseloyalty.foals;

import com.xziggsx.horseloyalty.core.feature.Feature;
import com.xziggsx.horseloyalty.core.feature.FeatureContext;
import com.xziggsx.horseloyalty.loyalty.Family;

/**
 * Foals growing up: bred foals are born tamed with a head start, follow a chosen parent and their
 * breeder, and stroll less. The home for later growing-up features.
 */
public final class FoalsFeature implements Feature {
	public static final String ID = "foals";

	@Override
	public String id() {
		return ID;
	}

	@Override
	public void registerData() {
		Family.init();
	}

	@Override
	public void wire(FeatureContext context) {
	}
}
