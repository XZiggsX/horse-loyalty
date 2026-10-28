package com.xziggsx.horseloyalty.hunger;

import com.xziggsx.horseloyalty.core.feature.Feature;
import com.xziggsx.horseloyalty.core.feature.FeatureContext;
import com.xziggsx.horseloyalty.loyalty.Hunger;
import java.util.Set;

/** Hunger: tamed horses get hungry, graze, and lose loyalty when neglected. Needs loyalty. */
public final class HungerFeature implements Feature {
	public static final String ID = "hunger";

	@Override
	public String id() {
		return ID;
	}

	@Override
	public Set<String> requires() {
		return Set.of("loyalty");
	}

	@Override
	public void registerData() {
		Hunger.init();
	}

	@Override
	public void wire(FeatureContext context) {
	}
}
