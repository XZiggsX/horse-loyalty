package com.xziggsx.horseloyalty.summoning;

import com.xziggsx.horseloyalty.core.config.SettingsSection;
import com.xziggsx.horseloyalty.core.feature.Feature;
import com.xziggsx.horseloyalty.core.feature.FeatureContext;

/** Summoning: bind a goat horn to a horse and blow it to call the horse. */
public final class SummoningFeature implements Feature {
	public static final String ID = "summoning";

	@Override
	public String id() {
		return ID;
	}

	@Override
	public SettingsSection settings() {
		return new SummoningSettings();
	}

	@Override
	public void registerData() {
		HornBinding.init();
		HornCall.init();
	}

	@Override
	public void wire(FeatureContext context) {
		HornBinding.wire(context);
		// Level with panicking; only runs for a horse that was called.
		context.goal(1, ComeWhenCalledGoal::new);
		SummoningCommands.register(context);
	}
}
