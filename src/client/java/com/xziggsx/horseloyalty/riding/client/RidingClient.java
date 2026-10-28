package com.xziggsx.horseloyalty.riding.client;

import com.xziggsx.horseloyalty.core.debug.DebugSource;
import com.xziggsx.horseloyalty.core.feature.FeatureContext;
import com.xziggsx.horseloyalty.riding.api.RideView;

/**
 * Client side of riding. The controls themselves hook in through {@code HorseRidingMixin};
 * this swaps in the client's {@link RideView}, which knows your own ride, and adds the dev HUD's riding section.
 */
public final class RidingClient {
	private RidingClient() {
	}

	/** Called once from the client entrypoint with riding's context. */
	public static void init(FeatureContext context) {
		context.provide(RideView.KEY, new ClientRideView());
		context.contribute(DebugSource.ALL, new RidingDebug());
	}
}
