package com.xziggsx.horseloyalty.summoning;

import com.xziggsx.horseloyalty.core.feature.Feature;
import com.xziggsx.horseloyalty.core.feature.FeatureContext;
import com.xziggsx.horseloyalty.horn.HornBinding;
import com.xziggsx.horseloyalty.horn.HornCall;

/** Summoning: bind a goat horn to a horse and blow it to call the horse. */
public final class SummoningFeature implements Feature {
	public static final String ID = "summoning";

	@Override
	public String id() {
		return ID;
	}

	@Override
	public void registerData() {
		HornBinding.init();
		HornCall.init();
	}

	@Override
	public void wire(FeatureContext context) {
		HornBinding.wire(context);
	}
}
