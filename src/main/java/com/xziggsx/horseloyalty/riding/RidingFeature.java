package com.xziggsx.horseloyalty.riding;

import com.xziggsx.horseloyalty.core.feature.Feature;
import com.xziggsx.horseloyalty.core.feature.FeatureContext;

/**
 * Riding: gaits, boat-style steering, sidestepping, backing up, gait drops, gait raise and bucking,
 * and the gallop push. The controls run on the rider's client (see the riding client package); the
 * common side keeps the gait each rider reports, see {@link GaitSync}.
 */
public final class RidingFeature implements Feature {
	public static final String ID = "riding";

	@Override
	public String id() {
		return ID;
	}

	@Override
	public void registerData() {
		GaitSync.registerData();
	}

	@Override
	public void wire(FeatureContext context) {
		GaitSync.wire(context);
	}
}
