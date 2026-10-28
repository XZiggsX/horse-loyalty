package com.xziggsx.horseloyalty.horseinfo;

import com.xziggsx.horseloyalty.core.feature.Feature;
import com.xziggsx.horseloyalty.core.feature.FeatureContext;

/** Horse info: the Info tab in the horse inventory, built from a {@link HorseProfile}. */
public final class HorseInfoFeature implements Feature {
	public static final String ID = "horseInfo";

	@Override
	public String id() {
		return ID;
	}

	@Override
	public void registerData() {
	}

	@Override
	public void wire(FeatureContext context) {
	}
}
