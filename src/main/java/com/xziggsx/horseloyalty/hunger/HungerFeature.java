package com.xziggsx.horseloyalty.hunger;

import com.xziggsx.horseloyalty.core.config.SettingsSection;
import com.xziggsx.horseloyalty.core.event.HorseEvents;
import com.xziggsx.horseloyalty.core.feature.Feature;
import com.xziggsx.horseloyalty.core.feature.FeatureContext;
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
	public SettingsSection settings() {
		return new HungerSettings();
	}

	@Override
	public void registerData() {
		Hunger.init();
	}

	@Override
	public void wire(FeatureContext context) {
		context.on(HorseEvents.SERVER_SECOND, Hunger::tick);
		// A hungry horse takes any treat, and every meal resets its hunger.
		context.on(HorseEvents.WANTS_TREAT, (horse, player, stack) -> Hunger.isHungry(horse));
		context.on(HorseEvents.ATE, (horse, player, stack) -> Hunger.feed(horse));
		HungerCommands.register(context);
	}
}
