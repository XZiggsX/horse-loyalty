package com.xziggsx.horseloyalty.foals;

import com.xziggsx.horseloyalty.core.config.SettingsSection;
import com.xziggsx.horseloyalty.core.event.HorseEvents;
import com.xziggsx.horseloyalty.core.feature.Feature;
import com.xziggsx.horseloyalty.core.feature.FeatureContext;

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
	public SettingsSection settings() {
		return new FoalSettings();
	}

	@Override
	public void registerData() {
		Family.init();
	}

	@Override
	public void wire(FeatureContext context) {
		context.on(HorseEvents.BRED, HorseEvents.FAMILY, Family::onBorn);
		// Ahead of vanilla's FollowParentGoal (4) and random strolling (6).
		context.goal(3, FollowChosenParentGoal::new);
		context.goal(3, FollowBreederGoal::new);
	}
}
