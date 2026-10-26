package com.xziggsx.horseloyalty.client;

import com.xziggsx.horseloyalty.HorseLoyaltyMod;
import net.fabricmc.api.ClientModInitializer;

/**
 * Client entrypoint. Runs on the client only.
 * <p>
 * Nothing needs registering here yet: the client features hook in through mixins
 * (see {@code horseloyalty.client.mixins.json}):
 * riding controls in {@code AbstractHorseRidingMixin}, the loyalty label in the horse
 * inventory in {@code AbstractMountInventoryScreenMixin}, and the dev panel in {@code HudMixin}.
 */
public class HorseLoyaltyClient implements ClientModInitializer {
	@Override
	public void onInitializeClient() {
		HorseLoyaltyMod.LOGGER.info("Horse Loyalty client loaded");
	}
}
