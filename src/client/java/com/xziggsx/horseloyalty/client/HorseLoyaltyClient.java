package com.xziggsx.horseloyalty.client;

import com.xziggsx.horseloyalty.HorseLoyaltyMod;
import com.xziggsx.horseloyalty.core.client.SettingsSyncClient;
import com.xziggsx.horseloyalty.core.feature.Features;
import com.xziggsx.horseloyalty.feedback.client.GaitEffects;
import com.xziggsx.horseloyalty.feedback.client.RidingHud;
import com.xziggsx.horseloyalty.loyalty.client.LoyaltyClient;
import com.xziggsx.horseloyalty.riding.RidingFeature;
import com.xziggsx.horseloyalty.riding.client.RidingClient;
import net.fabricmc.api.ClientModInitializer;

/**
 * Client entrypoint. Runs on the client only, after the common entrypoint has registered and
 * wired every feature. Starts the client side of each feature that has one; the rest hooks in
 * through each feature's client mixins (riding controls, the loyalty label, the horse info tab and
 * the dev panel).
 */
public class HorseLoyaltyClient implements ClientModInitializer {
	@Override
	public void onInitializeClient() {
		RidingClient.init(Features.context(RidingFeature.ID));
		LoyaltyClient.init();
		RidingHud.register();
		GaitEffects.register();
		// Follow the server's switches and synced settings while connected to it.
		SettingsSyncClient.init();
		HorseLoyaltyMod.LOGGER.info("Horse Loyalty client loaded");
	}
}
