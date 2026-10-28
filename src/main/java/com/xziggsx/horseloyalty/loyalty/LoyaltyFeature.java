package com.xziggsx.horseloyalty.loyalty;

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
	public void registerData() {
		Loyalty.init();
		LoyaltyChangePayload.init();
	}

	@Override
	public void wire(FeatureContext context) {
		context.provide(Bond.KEY, new LoyaltyBond());
	}
}
