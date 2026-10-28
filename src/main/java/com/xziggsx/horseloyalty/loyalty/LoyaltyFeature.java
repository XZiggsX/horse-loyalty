package com.xziggsx.horseloyalty.loyalty;

import com.xziggsx.horseloyalty.core.config.SettingsSection;
import com.xziggsx.horseloyalty.core.event.HorseEvents;
import com.xziggsx.horseloyalty.core.feature.Feature;
import com.xziggsx.horseloyalty.core.feature.FeatureContext;
import com.xziggsx.horseloyalty.loyalty.api.Bond;

/** Loyalty: levels 0 to 5 per rider, earned by feeding and riding, see {@link Loyalty}. */
public final class LoyaltyFeature implements Feature {
	public static final String ID = "loyalty";

	@Override
	public String id() {
		return ID;
	}

	@Override
	public SettingsSection settings() {
		return new LoyaltySettings();
	}

	@Override
	public void registerData() {
		Loyalty.init();
		LoyaltyChangePayload.init();
	}

	@Override
	public void wire(FeatureContext context) {
		context.provide(Bond.KEY, new LoyaltyBond());
		// A tamed horse at full health also takes a treat once the feeder's cooldown has passed.
		context.on(HorseEvents.WANTS_TREAT, (horse, player, stack) -> Loyalty.canCountFeed(horse, player));
		context.on(HorseEvents.ATE, (horse, player, stack) -> {
			if (Loyalty.canCountFeed(horse, player)) {
				Loyalty.recordFeed(horse, player);
			}
		});
		context.on(HorseEvents.SERVER_TICK, RideTicks::tick);
		// The rider's own loyalty status above the hotbar.
		context.on(HorseEvents.MOUNTED, (horse, player) -> player.sendOverlayMessage(Loyalty.describe(horse, player)));
	}
}
