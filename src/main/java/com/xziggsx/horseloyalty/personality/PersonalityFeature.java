package com.xziggsx.horseloyalty.personality;

import com.xziggsx.horseloyalty.core.feature.Feature;
import com.xziggsx.horseloyalty.core.feature.FeatureContext;
import com.xziggsx.horseloyalty.personality.api.PersonalityView;
import com.xziggsx.horseloyalty.riding.api.RideModifiers;

/** Personalities: steady, brave, skittish, curious and lazy horses, see {@link Personalities}. */
public final class PersonalityFeature implements Feature {
	public static final String ID = "personality";

	@Override
	public String id() {
		return ID;
	}

	@Override
	public void registerData() {
		Personalities.init();
	}

	@Override
	public void wire(FeatureContext context) {
		context.provide(PersonalityView.KEY, new HorsePersonalityView());
		context.contribute(RideModifiers.ALL, new PersonalityRideModifier());
	}
}
