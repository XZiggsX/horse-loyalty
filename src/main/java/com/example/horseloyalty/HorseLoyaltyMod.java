package com.example.horseloyalty;

import com.example.horseloyalty.config.HorseLoyaltyConfig;
import com.example.horseloyalty.loyalty.Family;
import com.example.horseloyalty.loyalty.Hunger;
import com.example.horseloyalty.loyalty.Loyalty;
import net.fabricmc.api.ModInitializer;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * Common entrypoint: runs on dedicated servers and on the client (whose built-in server
 * runs singleplayer worlds). Everything in the main source set is loaded on both sides;
 * the game logic in it (loyalty, hunger, feeding) only does work on the server side.
 * Client-only code (riding controls, screens, HUD) is in the client source set.
 */
public class HorseLoyaltyMod implements ModInitializer {
	public static final String MOD_ID = "horseloyalty";
	public static final Logger LOGGER = LoggerFactory.getLogger(MOD_ID);

	@Override
	public void onInitialize() {
		// Config first: the attachments and all game logic read their numbers from it.
		HorseLoyaltyConfig.load();
		// Registers the data saved on horses (Fabric Data Attachment API).
		Loyalty.init();
		Hunger.init();
		Family.init();
		LOGGER.info("Horse Loyalty loaded");
	}
}
