package com.xziggsx.horseloyalty.client;

import com.xziggsx.horseloyalty.HorseLoyaltyMod;
import com.xziggsx.horseloyalty.client.feedback.GaitEffects;
import com.xziggsx.horseloyalty.client.feedback.LoyaltyToasts;
import com.xziggsx.horseloyalty.client.feedback.RidingHud;
import com.xziggsx.horseloyalty.config.HorseLoyaltyConfig;
import com.xziggsx.horseloyalty.config.RidingRules;
import net.fabricmc.api.ClientModInitializer;
import net.fabricmc.fabric.api.client.networking.v1.ClientPlayConnectionEvents;
import net.fabricmc.fabric.api.client.networking.v1.ClientPlayNetworking;

/**
 * Client entrypoint. Runs on the client only.
 * <p>
 * Registers the visual feedback in {@code client/feedback}: the riding HUD, gait particles and
 * loyalty toasts. The other client features hook in through mixins
 * (see {@code horseloyalty.client.mixins.json}):
 * riding controls in {@code AbstractHorseRidingMixin}, the loyalty label in the horse
 * inventory in {@code AbstractMountInventoryScreenMixin}, and the dev panel in {@code HudMixin}.
 */
public class HorseLoyaltyClient implements ClientModInitializer {
	@Override
	public void onInitializeClient() {
		RidingHud.register();
		GaitEffects.register();
		LoyaltyToasts.register();
		// Ride by the server's rules while connected to it, and by our own config again after.
		ClientPlayNetworking.registerGlobalReceiver(RidingRules.Payload.TYPE,
				(payload, _) -> HorseLoyaltyConfig.useServerRidingRules(payload.rules()));
		ClientPlayConnectionEvents.DISCONNECT.register((_, _) -> HorseLoyaltyConfig.useServerRidingRules(null));
		HorseLoyaltyMod.LOGGER.info("Horse Loyalty client loaded");
	}
}
