package com.xziggsx.horseloyalty.personality;

import com.xziggsx.horseloyalty.core.feature.Feature;
import com.xziggsx.horseloyalty.core.feature.FeatureContext;

/** Personalities: steady, brave, skittish, curious and lazy horses, see {@link Personality}. */
public final class PersonalityFeature implements Feature {
	public static final String ID = "personality";

	@Override
	public String id() {
		return ID;
	}

	@Override
	public void registerData() {
		Personality.init();
	}

	@Override
	public void wire(FeatureContext context) {
	}
}
